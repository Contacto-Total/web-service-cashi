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
 * en que llegó y fecha_retiro el archivo en que ya no vino; mientras está asignado, fecha_retiro va vacía. El banco
 * retira por CJ: el CJ que no viene sale de la tabla dinámica aunque su cliente siga con otros. El cliente al que no
 * le queda ningún CJ queda además INACTIVE y sus contactos de campañas activas dejan de marcarse; uno que llega o
 * vuelve queda ACTIVE. Gestiones, promesas y pagos no se tocan.
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
    public record Deteccion(int clientesRetirados, int cjRetirados, int clientesReingresados, int clientesNuevos,
                            int contactosBloqueados, Set<String> codigosAActivar) {

        public Map<String, Object> resumen(int cjAgregados) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clientesRetirados", clientesRetirados);
            m.put("cjRetirados", cjRetirados);
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
     * Llena con el valor del archivo las columnas indicadas que están vacías (NULL) en CJ que ya estaban en la tabla
     * dinámica; no pisa un valor guardado. columnas[0] es la columna del CJ. Devuelve los CU de las filas completadas,
     * que se vuelven a sincronizar y activar.
     */
    public Set<String> completarVacias(String tabla, List<String> columnas, List<Object[]> filas,
                                       List<String> objetivo, String columnaCu) {
        Set<String> codigos = new LinkedHashSet<>();
        if (objetivo.isEmpty()) {
            return codigos;
        }
        String colCj = columnas.get(0);
        StringBuilder vacia = new StringBuilder();
        for (String c : objetivo) {
            vacia.append(vacia.length() == 0 ? "" : " OR ").append('`').append(c).append("` IS NULL");
        }
        Map<String, String> guardadoPorCj = new HashMap<>();
        jdbcTemplate.query("SELECT `" + colCj + "` FROM `" + tabla + "` WHERE " + vacia,
                rs -> {
                    String guardado = rs.getString(1);
                    String cj = normalizarCj(guardado);
                    if (cj != null) {
                        guardadoPorCj.put(cj, guardado);
                    }
                });
        if (guardadoPorCj.isEmpty()) {
            return codigos;
        }

        int indiceCu = columnas.indexOf(columnaCu);
        int total = 0;
        for (String c : objetivo) {
            int i = columnas.indexOf(c);
            if (i < 0) {
                continue;
            }
            List<Object[]> args = new ArrayList<>();
            Set<String> vistos = new HashSet<>();
            for (Object[] f : filas) {
                String cj = normalizarCj(f[0]);
                if (cj == null || f[i] == null || !guardadoPorCj.containsKey(cj) || !vistos.add(cj)) {
                    continue;
                }
                args.add(new Object[]{f[i], guardadoPorCj.get(cj)});
                if (indiceCu >= 0 && limpio(f[indiceCu]) != null) {
                    codigos.add(limpio(f[indiceCu]));
                }
            }
            if (args.isEmpty()) {
                continue;
            }
            for (int n : jdbcTemplate.batchUpdate("UPDATE `" + tabla + "` SET `" + c + "` = ? WHERE `" + colCj
                    + "` = ? AND `" + c + "` IS NULL", args)) {
                total += Math.max(n, 0);
            }
        }
        logger.info("Carga de {}: {} valores vacíos completados en {} ({} clientes)", tabla, total, objetivo, codigos.size());
        return codigos;
    }

    /**
     * Compara los CJ del archivo con los asignados de la subcartera (filas sin fecha_retiro).
     * Al cliente que no viene le pone fecha_retiro en sus CJ con los datos que tenían, lo borra de la tabla dinámica,
     * lo deja INACTIVE y bloquea sus contactos en campañas activas. Al CJ que no viene de un cliente que sí viene le
     * pone fecha_retiro y lo borra de la tabla dinámica; el cliente sigue ACTIVE. Por cada CJ del archivo sin fila
     * vigente abre una fila con fecha_ingreso; su cliente se activa después de la sincronización.
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
            return new Deteccion(0, 0, 0, 0, 0, Set.of());
        }

        Map<String, List<Long>> vigentesPorCu = new HashMap<>();
        Map<Long, String> cjPorId = new HashMap<>();
        Map<String, String> documentoPorCu = new HashMap<>();
        Map<String, Long> sinDocumento = new HashMap<>();
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
                    } else {
                        sinDocumento.put(normalizarCj(rs.getString("identificador_deuda")), id);
                    }
                    cjsVigentes.add(rs.getString("identificador_deuda"));
                },
                subcarteraId);

        // Filas vigentes que entraron sin documento: lo toman del archivo.
        List<Object[]> documentos = new ArrayList<>();
        for (FilaArchivo f : archivoPorCj.values()) {
            Long id = sinDocumento.get(f.cj());
            if (id != null && f.documento() != null) {
                documentos.add(new Object[]{f.documento(), id});
                documentoPorCu.putIfAbsent(f.cu(), f.documento());
            }
        }
        if (!documentos.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "UPDATE cashi_db.historial_cartera SET documento = ? WHERE id = ? AND documento IS NULL", documentos);
            logger.info("Historial de cartera, subcartera {}: {} CJ vigentes completados con su documento",
                    subcarteraId, documentos.size());
        }

        List<String> retirados = new ArrayList<>(vigentesPorCu.keySet());
        retirados.removeAll(cusArchivo);
        Map<String, List<Long>> sueltosPorCu = new HashMap<>();
        for (Map.Entry<String, List<Long>> e : vigentesPorCu.entrySet()) {
            if (!cusArchivo.contains(e.getKey())) {
                continue;
            }
            for (Long id : e.getValue()) {
                if (!archivoPorCj.containsKey(normalizarCj(cjPorId.get(id)))) {
                    sueltosPorCu.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(id);
                }
            }
        }
        int contactos = retirar(subcarteraId, tabla, col, retirados, vigentesPorCu, cjPorId, documentoPorCu, fecha);
        int cjRetirados = retirarCjs(tabla, col, sueltosPorCu, cjPorId, fecha);
        for (String cu : retirados) {
            cjRetirados += vigentesPorCu.get(cu).size();
        }

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
        logger.info("Historial de cartera, subcartera {} ({}): {} clientes retirados, {} CJ retirados, {} reingresados, "
                        + "{} nuevos, {} CJ con fila nueva, {} contactos bloqueados",
                subcarteraId, fecha, retirados.size(), cjRetirados, reingresados, nuevos, altas.size(), contactos);
        return new Deteccion(retirados.size(), cjRetirados, reingresados, nuevos, contactos, cusQueLlegan);
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
        Map<String, Object[]> datosPorCj = datosEnTabla(tabla, col, retirados);
        List<Object[]> cierres = new ArrayList<>();
        for (String cu : retirados) {
            for (Long id : vigentesPorCu.get(cu)) {
                Object[] d = datosPorCj.getOrDefault(normalizarCj(cjPorId.get(id)), new Object[5]);
                cierres.add(new Object[]{Date.valueOf(fecha), d[0], d[1], d[2], d[3], id});
            }
        }
        cerrar(cierres);

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

    /**
     * Retira los CJ que no vinieron de clientes que sí vinieron: fecha_retiro con los datos que tenían y fuera de la
     * tabla dinámica. El cliente no cambia de estado ni sus contactos, porque le quedan otros CJ asignados.
     */
    private int retirarCjs(String tabla, ColumnasCartera col, Map<String, List<Long>> sueltosPorCu,
                           Map<Long, String> cjPorId, LocalDate fecha) {
        if (sueltosPorCu.isEmpty()) {
            return 0;
        }
        Map<String, Object[]> datosPorCj = datosEnTabla(tabla, col, sueltosPorCu.keySet());
        List<Object[]> cierres = new ArrayList<>();
        List<Object[]> borrados = new ArrayList<>();
        for (List<Long> ids : sueltosPorCu.values()) {
            for (Long id : ids) {
                Object[] d = datosPorCj.getOrDefault(normalizarCj(cjPorId.get(id)), new Object[5]);
                cierres.add(new Object[]{Date.valueOf(fecha), d[0], d[1], d[2], d[3], id});
                if (d[4] != null) {
                    borrados.add(new Object[]{d[4]});
                }
            }
        }
        cerrar(cierres);
        if (!borrados.isEmpty()) {
            jdbcTemplate.batchUpdate("DELETE FROM `" + tabla + "` WHERE `" + col.cj() + "` = ?", borrados);
        }
        return cierres.size();
    }

    /**
     * Producto, moneda, capital, deuda total y el CJ tal como está guardado, por CJ normalizado, de las filas de esos
     * clientes en la tabla dinámica.
     */
    private Map<String, Object[]> datosEnTabla(String tabla, ColumnasCartera col, Collection<String> cus) {
        Map<String, Object[]> datosPorCj = new HashMap<>();
        String select = "SELECT `" + col.cj() + "` AS cj, " + columnaO(col.producto()) + " AS producto, "
                + columnaO(col.moneda()) + " AS moneda, " + columnaO(col.capital()) + " AS capital, "
                + columnaO(col.deudaTotal()) + " AS deuda_total FROM `" + tabla + "` WHERE `" + col.cu() + "` IN (";
        for (List<String> parte : bloques(cus)) {
            jdbcTemplate.query(select + marcadores(parte.size()) + ")",
                    rs -> {
                        String guardado = rs.getString("cj");
                        String cj = normalizarCj(guardado);
                        if (cj != null) {
                            datosPorCj.putIfAbsent(cj, new Object[]{rs.getString("producto"), rs.getString("moneda"),
                                    rs.getBigDecimal("capital"), rs.getBigDecimal("deuda_total"), guardado});
                        }
                    },
                    parte.toArray());
        }
        return datosPorCj;
    }

    /** Cierra las filas del historial: {fecha_retiro, producto, moneda, capital, deuda_total, id}. */
    private void cerrar(List<Object[]> cierres) {
        jdbcTemplate.batchUpdate(
                "UPDATE cashi_db.historial_cartera SET fecha_retiro = ?, producto = COALESCE(?, producto), "
                        + "moneda = COALESCE(?, moneda), capital = COALESCE(?, capital), "
                        + "deuda_total = COALESCE(?, deuda_total) WHERE id = ?",
                cierres);
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
