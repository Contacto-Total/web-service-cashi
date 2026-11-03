package com.cashi.systemconfiguration.application.internal.commandservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Asignación de la cartera por cliente en las subcarteras de una fila por deuda (identificador de deuda enlazado).
 *
 * cashi_db.historial_cartera guarda una fila por CJ y por cada vez que estuvo asignado: fecha_ingreso es el archivo
 * en que llegó y fecha_retiro el archivo en que su cliente (CU) ya no vino; mientras está asignado, fecha_retiro va
 * vacía. Un cliente que no viene se borra de la tabla dinámica, queda INACTIVE y sus contactos de campañas activas
 * dejan de marcarse; uno que llega o vuelve queda ACTIVE. Gestiones, promesas y pagos no se tocan.
 */
@Service
public class HistorialCarteraService {

    private static final Logger logger = LoggerFactory.getLogger(HistorialCarteraService.class);
    private static final int BLOQUE = 500;

    private final JdbcTemplate jdbcTemplate;

    public HistorialCarteraService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Columnas de la tabla dinámica que usa la detección. producto, moneda, capital y deudaTotal pueden ser null. */
    public record ColumnasCartera(String cj, String cu, String documento,
                                  String producto, String moneda, String capital, String deudaTotal) {}

    /** CJ, CU y documento de una fila del archivo, con las mismas transformaciones que en la tabla dinámica. */
    public record FilaArchivo(String cj, String cu, String documento) {}

    /** CJ insertados en la tabla dinámica y los CU a los que pertenecen. */
    public record Altas(int cjAgregados, Set<String> codigos) {}

    /** Resumen de la detección; codigosAActivar se pasan a ACTIVE después de sincronizar clientes. */
    public record Deteccion(int clientesRetirados, int clientesReingresados, int clientesNuevos,
                            int contactosBloqueados, Set<String> codigosAActivar) {

        public Map<String, Object> resumen(int cjAgregados) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clientesRetirados", clientesRetirados);
            m.put("clientesReingresados", clientesReingresados);
            m.put("clientesNuevos", clientesNuevos);
            m.put("cjAgregados", cjAgregados);
            m.put("contactosBloqueados", contactosBloqueados);
            return m;
        }
    }

    /**
     * Identificador de deuda listo para comparar: sin espacios ni ceros a la izquierda, porque el banco a veces lo
     * manda con ellos y a veces sin ellos. Null si queda vacío.
     */
    public static String normalizarCj(Object valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.toString().trim().replaceFirst("^0+", "");
        return limpio.isEmpty() ? null : limpio;
    }

    private static String limpio(Object valor) {
        if (valor == null) {
            return null;
        }
        String s = valor.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * Inserta en la tabla dinámica las filas del archivo cuyo CJ no está. columnas[0] es la columna del CJ y cada fila
     * trae sus valores en el mismo orden; los CJ repetidos en el archivo entran una sola vez.
     */
    public Altas insertarCjNuevos(String tabla, List<String> columnas, List<Object[]> filas, String columnaCu) {
        Set<String> existentes = new HashSet<>();
        jdbcTemplate.query("SELECT `" + columnas.get(0) + "` FROM `" + tabla + "`",
                rs -> { String cj = normalizarCj(rs.getString(1)); if (cj != null) existentes.add(cj); });

        int indiceCu = columnas.indexOf(columnaCu);
        List<Object[]> nuevas = new ArrayList<>();
        Set<String> codigos = new LinkedHashSet<>();
        for (Object[] fila : filas) {
            String cj = normalizarCj(fila[0]);
            if (cj == null || !existentes.add(cj)) {
                continue;
            }
            nuevas.add(fila);
            if (indiceCu >= 0 && limpio(fila[indiceCu]) != null) {
                codigos.add(limpio(fila[indiceCu]));
            }
        }
        if (nuevas.isEmpty()) {
            return new Altas(0, codigos);
        }

        StringBuilder cols = new StringBuilder();
        for (String c : columnas) {
            cols.append(cols.length() == 0 ? "" : ", ").append('`').append(c).append('`');
        }
        String sql = "INSERT INTO `" + tabla + "` (" + cols + ") VALUES ("
                + String.join(", ", Collections.nCopies(columnas.size(), "?")) + ")";
        jdbcTemplate.batchUpdate(sql, nuevas);
        logger.info("Carga de {}: {} CJ nuevos insertados ({} clientes)", tabla, nuevas.size(), codigos.size());
        return new Altas(nuevas.size(), codigos);
    }

    /**
     * Compara los CU del archivo con los clientes asignados de la subcartera (filas sin fecha_retiro).
     * Al cliente que no viene le pone fecha_retiro en sus CJ con los datos que tenían, lo borra de la tabla dinámica,
     * lo deja INACTIVE y bloquea sus contactos en campañas activas. Por cada CJ del archivo sin fila vigente abre una
     * fila con fecha_ingreso; su cliente se activa después de la sincronización.
     */
    public Deteccion registrar(Integer subcarteraId, String tabla, ColumnasCartera col,
                               List<FilaArchivo> filas, LocalDate fecha) {
        Map<String, FilaArchivo> archivoPorCj = new LinkedHashMap<>();
        Set<String> cusArchivo = new HashSet<>();
        for (FilaArchivo f : filas) {
            String cj = normalizarCj(f.cj());
            String cu = limpio(f.cu());
            if (cj == null || cu == null) {
                continue;
            }
            archivoPorCj.putIfAbsent(cj, new FilaArchivo(cj, cu, limpio(f.documento())));
            cusArchivo.add(cu);
        }
        if (archivoPorCj.isEmpty()) {
            logger.warn("Historial de cartera, subcartera {}: el archivo no trae CJ ni CU; no se registra nada", subcarteraId);
            return new Deteccion(0, 0, 0, 0, Set.of());
        }

        Map<String, List<Long>> vigentesPorCu = new HashMap<>();
        Map<Long, String> cjPorId = new HashMap<>();
        Map<String, String> documentoPorCu = new HashMap<>();
        Set<String> cjsVigentes = new HashSet<>();
        jdbcTemplate.query(
                "SELECT id, codigo_cliente, documento, identificador_deuda FROM cashi_db.historial_cartera "
                        + "WHERE id_subcartera = ? AND fecha_retiro IS NULL",
                rs -> {
                    long id = rs.getLong("id");
                    String cu = rs.getString("codigo_cliente");
                    if (cu == null) {
                        return;
                    }
                    vigentesPorCu.computeIfAbsent(cu, k -> new ArrayList<>()).add(id);
                    cjPorId.put(id, rs.getString("identificador_deuda"));
                    if (rs.getString("documento") != null) {
                        documentoPorCu.putIfAbsent(cu, rs.getString("documento"));
                    }
                    cjsVigentes.add(rs.getString("identificador_deuda"));
                },
                subcarteraId);

        List<String> retirados = new ArrayList<>(vigentesPorCu.keySet());
        retirados.removeAll(cusArchivo);
        int contactos = retirar(subcarteraId, tabla, col, retirados, vigentesPorCu, cjPorId, documentoPorCu, fecha);

        List<Object[]> altas = new ArrayList<>();
        Set<String> cusQueLlegan = new LinkedHashSet<>();
        for (FilaArchivo f : archivoPorCj.values()) {
            if (cjsVigentes.contains(f.cj())) {
                continue;
            }
            altas.add(new Object[]{subcarteraId, f.cu(), f.documento(), f.cj(), Date.valueOf(fecha)});
            if (!vigentesPorCu.containsKey(f.cu())) {
                cusQueLlegan.add(f.cu());
            }
        }
        Set<String> conHistorial = cusConRetiro(subcarteraId, cusQueLlegan);
        if (!altas.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO cashi_db.historial_cartera "
                            + "(id_subcartera, codigo_cliente, documento, identificador_deuda, fecha_ingreso) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    altas);
        }

        int reingresados = conHistorial.size();
        int nuevos = cusQueLlegan.size() - reingresados;
        logger.info("Historial de cartera, subcartera {} ({}): {} clientes retirados, {} reingresados, {} nuevos, "
                        + "{} CJ con fila nueva, {} contactos bloqueados",
                subcarteraId, fecha, retirados.size(), reingresados, nuevos, altas.size(), contactos);
        return new Deteccion(retirados.size(), reingresados, nuevos, contactos, cusQueLlegan);
    }

    /** Pasa a ACTIVE los clientes de la subcartera con esos códigos; se llama después de sincronizarlos. */
    public int activar(Integer subcarteraId, Collection<String> codigos) {
        int total = 0;
        for (List<String> parte : bloques(codigos)) {
            List<Object> args = new ArrayList<>();
            args.add(subcarteraId);
            args.addAll(parte);
            total += jdbcTemplate.update(
                    "UPDATE cashi_db.clientes SET estado = 'ACTIVE' WHERE id_subcartera = ? AND codigo_identificacion IN ("
                            + marcadores(parte.size()) + ") AND (estado IS NULL OR estado <> 'ACTIVE')",
                    args.toArray());
        }
        return total;
    }

    private int retirar(Integer subcarteraId, String tabla, ColumnasCartera col, List<String> retirados,
                        Map<String, List<Long>> vigentesPorCu, Map<Long, String> cjPorId,
                        Map<String, String> documentoPorCu, LocalDate fecha) {
        if (retirados.isEmpty()) {
            return 0;
        }
        Map<String, Object[]> datosPorCj = new HashMap<>();
        String select = "SELECT `" + col.cj() + "` AS cj, " + columnaO(col.producto()) + " AS producto, "
                + columnaO(col.moneda()) + " AS moneda, " + columnaO(col.capital()) + " AS capital, "
                + columnaO(col.deudaTotal()) + " AS deuda_total FROM `" + tabla + "` WHERE `" + col.cu() + "` IN (";
        for (List<String> parte : bloques(retirados)) {
            jdbcTemplate.query(select + marcadores(parte.size()) + ")",
                    rs -> {
                        String cj = normalizarCj(rs.getString("cj"));
                        if (cj != null) {
                            datosPorCj.putIfAbsent(cj, new Object[]{rs.getString("producto"), rs.getString("moneda"),
                                    rs.getBigDecimal("capital"), rs.getBigDecimal("deuda_total")});
                        }
                    },
                    parte.toArray());
        }

        List<Object[]> cierres = new ArrayList<>();
        for (String cu : retirados) {
            for (Long id : vigentesPorCu.get(cu)) {
                Object[] d = datosPorCj.getOrDefault(cjPorId.get(id), new Object[4]);
                cierres.add(new Object[]{Date.valueOf(fecha), d[0], d[1], d[2], d[3], id});
            }
        }
        jdbcTemplate.batchUpdate(
                "UPDATE cashi_db.historial_cartera SET fecha_retiro = ?, producto = COALESCE(?, producto), "
                        + "moneda = COALESCE(?, moneda), capital = COALESCE(?, capital), "
                        + "deuda_total = COALESCE(?, deuda_total) WHERE id = ?",
                cierres);

        int contactos = 0;
        for (List<String> parte : bloques(retirados)) {
            jdbcTemplate.update("DELETE FROM `" + tabla + "` WHERE `" + col.cu() + "` IN (" + marcadores(parte.size()) + ")",
                    parte.toArray());

            List<Object> args = new ArrayList<>();
            args.add(subcarteraId);
            args.addAll(parte);
            jdbcTemplate.update("UPDATE cashi_db.clientes SET estado = 'INACTIVE' WHERE id_subcartera = ? "
                    + "AND codigo_identificacion IN (" + marcadores(parte.size()) + ")", args.toArray());

            List<String> documentos = parte.stream().map(documentoPorCu::get).filter(d -> d != null).toList();
            if (!documentos.isEmpty()) {
                List<Object> a = new ArrayList<>();
                a.add(subcarteraId);
                a.addAll(documentos);
                // Mismos estados que bloquea la tipificación de fallecido: los que el discador todavía puede marcar.
                contactos += jdbcTemplate.update(
                        "UPDATE cashi_discador_db.contactos ct "
                                + "JOIN cashi_discador_db.campanas k ON k.id = ct.campana_id AND k.status = 'ACTIVE' "
                                + "SET ct.status = 'DO_NOT_CALL', ct.actualizado_en = NOW() "
                                + "WHERE k.id_subcartera = ? AND ct.documento IN (" + marcadores(documentos.size()) + ") "
                                + "AND ct.status IN ('PENDING', 'NO_ANSWER', 'BUSY', 'CONTACTED')",
                        a.toArray());
            }
        }
        return contactos;
    }

    private Set<String> cusConRetiro(Integer subcarteraId, Set<String> cus) {
        Set<String> conRetiro = new HashSet<>();
        for (List<String> parte : bloques(cus)) {
            List<Object> args = new ArrayList<>();
            args.add(subcarteraId);
            args.addAll(parte);
            conRetiro.addAll(jdbcTemplate.queryForList(
                    "SELECT DISTINCT codigo_cliente FROM cashi_db.historial_cartera WHERE id_subcartera = ? "
                            + "AND fecha_retiro IS NOT NULL AND codigo_cliente IN (" + marcadores(parte.size()) + ")",
                    String.class, args.toArray()));
        }
        return conRetiro;
    }

    private static String columnaO(String columna) {
        return columna != null ? "`" + columna + "`" : "NULL";
    }

    private static String marcadores(int n) {
        return String.join(",", Collections.nCopies(n, "?"));
    }

    private static List<List<String>> bloques(Collection<String> valores) {
        List<String> lista = new ArrayList<>(valores);
        List<List<String>> partes = new ArrayList<>();
        for (int i = 0; i < lista.size(); i += BLOQUE) {
            partes.add(lista.subList(i, Math.min(i + BLOQUE, lista.size())));
        }
        return partes;
    }
}
