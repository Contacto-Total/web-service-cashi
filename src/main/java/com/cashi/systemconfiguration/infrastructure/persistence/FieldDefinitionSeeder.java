package com.cashi.systemconfiguration.infrastructure.persistence;

import com.cashi.shared.domain.model.entities.FieldDefinition;
import com.cashi.shared.infrastructure.persistence.jpa.repositories.FieldDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeder específico para las definiciones de campos del sistema
 * Carga el catálogo maestro de 113 campos disponibles para mapeo de cabeceras
 * - 38 campos numéricos
 * - 10 datos cliente
 * - 10 datos cuenta
 * - 34 campos de texto
 * - 21 campos de fecha
 */
@Component
@Order(1) // Se ejecuta antes que otros seeders
public class FieldDefinitionSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(FieldDefinitionSeeder.class);

    private final FieldDefinitionRepository fieldDefinitionRepository;

    public FieldDefinitionSeeder(FieldDefinitionRepository fieldDefinitionRepository) {
        this.fieldDefinitionRepository = fieldDefinitionRepository;
    }

    @Override
    public void run(String... args) {
        logger.info("====================================================================");
        logger.info("INICIANDO SEEDING - DEFINICIONES DE CAMPOS DEL SISTEMA (113 CAMPOS)");
        logger.info("====================================================================");

        long camposAntesDeSeeding = fieldDefinitionRepository.count();
        logger.info("Campos existentes antes del seeding: {}", camposAntesDeSeeding);

        seedCamposNumericos();
        seedCamposTexto();
        seedCamposFecha();

        long totalFields = fieldDefinitionRepository.count();
        long camposNuevos = totalFields - camposAntesDeSeeding;

        logger.info("====================================================================");
        if (camposNuevos > 0) {
            logger.info("✓ SEEDING COMPLETADO - {} NUEVOS CAMPOS AGREGADOS", camposNuevos);
        } else {
            logger.info("✓ SEEDING COMPLETADO - TODOS LOS CAMPOS YA EXISTEN");
        }
        logger.info("  Total de campos en BD: {}", totalFields);
        logger.info("====================================================================");
    }

    private void seedCamposNumericos() {
        logger.info("--------------------------------------------------------------------");
        logger.info("Seeding Campos Numéricos y Adicionales (58 campos)...");

        seedField("monto_capital", "Monto Capital",
            "Monto principal adeudado del préstamo",
            "NUMERICO", "decimal(18,2)");

        seedField("monto_interes", "Monto Interés",
            "Intereses acumulados sobre el capital",
            "NUMERICO", "decimal(18,2)");

        seedField("monto_mora_inicial", "Monto Mora Inicial",
            "Intereses moratorios por atraso en pagos iniciales",
            "NUMERICO", "decimal(18,2)");

        seedField("monto_mora", "Monto Mora",
            "Intereses moratorios por atraso en pagos",
            "NUMERICO", "decimal(18,2)");

        seedField("deuda_total_inicial", "Deuda Total Inicial",
            "Deuda total inicial a pagar",
            "NUMERICO", "decimal(18,2)");

        seedField("deuda_total_actual", "Deuda Total Actual",
            "Deuda total actual a pagar",
            "NUMERICO", "decimal(18,2)");

        seedField("monto_minimo_pagar", "Monto Mínimo a Pagar",
            "Monto mínimo requerido para el pago",
            "NUMERICO", "decimal(18,2)");

        seedField("dias_mora", "Días de Mora",
            "Cantidad de días transcurridos en situación de mora",
            "NUMERICO", null);

        seedField("cuotas_totales", "Cuotas Totales",
            "Número total de cuotas del crédito",
            "NUMERICO", null);

        seedField("cuotas_pagadas", "Cuotas Pagadas",
            "Número de cuotas efectivamente pagadas",
            "NUMERICO", null);

        seedField("cuotas_pendientes", "Cuotas Pendientes",
            "Número de cuotas que faltan pagar",
            "NUMERICO", null);

        seedField("saldo_pendiente", "Saldo Pendiente",
            "Saldo total pendiente de pago",
            "NUMERICO", "decimal(18,2)");

        seedField("monto_desembolsado", "Monto Desembolsado",
            "Monto originalmente desembolsado del crédito",
            "NUMERICO", "decimal(18,2)");

        seedField("tasa_interes", "Tasa de Interés",
            "Tasa de interés anual del crédito",
            "NUMERICO", "decimal(5,2)");

        seedField("puntaje_crediticio", "Puntaje Crediticio",
            "Score crediticio del cliente",
            "NUMERICO", null);

        seedField("cantidad_llamadas", "Cantidad de Llamadas",
            "Número de intentos de contacto realizados",
            "NUMERICO", null);

        seedField("porcentaje_cumplimiento", "Porcentaje de Cumplimiento",
            "Porcentaje histórico de cumplimiento de pagos",
            "NUMERICO", "decimal(5,2)");

        seedField("numero_refinanciaciones", "Número de Refinanciaciones",
            "Cantidad de veces que el crédito ha sido refinanciado",
            "NUMERICO", null);

        seedField("monto_promesa_pago", "Monto Promesa de Pago",
            "Monto comprometido por el cliente para pagar",
            "NUMERICO", "decimal(18,2)");

        seedField("monto_gastos_cobranza", "Monto Gastos de Cobranza",
            "Gastos administrativos de cobranza aplicados",
            "NUMERICO", "decimal(18,2)");

        seedField("monto_total_deuda", "Monto Total de Deuda",
            "Suma total de capital + intereses + mora + gastos",
            "NUMERICO", "decimal(18,2)");

        seedField("porcentaje_descuento", "Porcentaje de Descuento",
            "Descuento aplicado sobre la deuda",
            "NUMERICO", "decimal(5,2)");

        seedField("monto_cuota_mensual", "Monto Cuota Mensual",
            "Monto de la cuota mensual del crédito",
            "NUMERICO", "decimal(18,2)");

        seedField("edad", "Edad",
            "Edad del cliente en años",
            "NUMERICO", null);

        seedField("monto_ultimo_pago", "Monto Último Pago",
            "Monto del último pago realizado",
            "NUMERICO", "decimal(18,2)");

        seedField("monto_pagos_total_mes", "Monto Pagos Total del Mes",
            "Monto del pago total en el mes",
            "NUMERICO", "decimal(18,2)");

        seedField("suma_pagos_mes", "Suma Pagos del Mes",
            "Suma total de pagos realizados en el mes",
            "NUMERICO", "decimal(18,2)");

        seedField("saldo_actual_tarjeta", "Saldo Actual Tarjeta",
            "Saldo actual de tarjeta de crédito",
            "NUMERICO", "decimal(18,2)");

        seedField("saldo_actual_linea_prestamo", "Saldo Actual Línea de Préstamo",
            "Saldo actual de línea de préstamo",
            "NUMERICO", "decimal(18,2)");

        seedField("dias_mora_inicial", "Días de Mora Inicial",
            "Días de mora al momento de asignación",
            "NUMERICO", null);

        seedField("monto_capital_inicial", "Monto Capital Inicial",
            "Saldo de capital al momento de asignación",
            "NUMERICO", "decimal(18,2)");

        seedField("monto_actual", "Monto Actual",
            "Saldo actual consolidado",
            "NUMERICO", "decimal(18,2)");

        seedField("promocion_1", "Promoción 1",
            "Campo promocional 1",
            "NUMERICO", "decimal(18,2)");

        seedField("promocion_2", "Promoción 2",
            "Campo promocional 2",
            "NUMERICO", "decimal(18,2)");

        seedField("promocion_3", "Promoción 3",
            "Campo promocional 3",
            "NUMERICO", "decimal(18,2)");

        seedField("promocion_4", "Promoción 4",
            "Campo promocional 4",
            "NUMERICO", "decimal(18,2)");

        seedField("promocion_5", "Promoción 5",
            "Campo promocional 5",
            "NUMERICO", "decimal(18,2)");

        seedField("promocion_6", "Promoción 6",
            "Campo promocional 6",
            "NUMERICO", "decimal(18,2)");

        seedField("promocion_7", "Promoción 7",
            "Campo promocional 7",
            "NUMERICO", "decimal(18,2)");

        seedField("promocion_8", "Promoción 8",
            "Campo promocional 8",
            "NUMERICO", "decimal(18,2)");

        seedField("promocion_9", "Promoción 9",
            "Campo promocional 9",
            "NUMERICO", "decimal(18,2)");

        seedField("promocion_10", "Promoción 10",
            "Campo promocional 10",
            "NUMERICO", "decimal(18,2)");

        // Datos adicionales del cliente
        seedField("dato_cliente_1", "Dato Cliente 1",
            "Campo adicional 1 para datos del cliente",
            "TEXTO", null);

        seedField("dato_cliente_2", "Dato Cliente 2",
            "Campo adicional 2 para datos del cliente",
            "TEXTO", null);

        seedField("dato_cliente_3", "Dato Cliente 3",
            "Campo adicional 3 para datos del cliente",
            "TEXTO", null);

        seedField("dato_cliente_4", "Dato Cliente 4",
            "Campo adicional 4 para datos del cliente",
            "TEXTO", null);

        seedField("dato_cliente_5", "Dato Cliente 5",
            "Campo adicional 5 para datos del cliente",
            "TEXTO", null);

        seedField("dato_cliente_6", "Dato Cliente 6",
            "Campo adicional 6 para datos del cliente",
            "TEXTO", null);

        seedField("dato_cliente_7", "Dato Cliente 7",
            "Campo adicional 7 para datos del cliente",
            "TEXTO", null);

        seedField("dato_cliente_8", "Dato Cliente 8",
            "Campo adicional 8 para datos del cliente",
            "TEXTO", null);

        seedField("dato_cliente_9", "Dato Cliente 9",
            "Campo adicional 9 para datos del cliente",
            "TEXTO", null);

        seedField("dato_cliente_10", "Dato Cliente 10",
            "Campo adicional 10 para datos del cliente",
            "TEXTO", null);

        // Datos adicionales de la cuenta
        seedField("dato_cuenta_1", "Dato Cuenta 1",
            "Campo adicional 1 para datos de la cuenta",
            "TEXTO", null);

        seedField("dato_cuenta_2", "Dato Cuenta 2",
            "Campo adicional 2 para datos de la cuenta",
            "TEXTO", null);

        seedField("dato_cuenta_3", "Dato Cuenta 3",
            "Campo adicional 3 para datos de la cuenta",
            "TEXTO", null);

        seedField("dato_cuenta_4", "Dato Cuenta 4",
            "Campo adicional 4 para datos de la cuenta",
            "TEXTO", null);

        seedField("dato_cuenta_5", "Dato Cuenta 5",
            "Campo adicional 5 para datos de la cuenta",
            "TEXTO", null);

        seedField("dato_cuenta_6", "Dato Cuenta 6",
            "Campo adicional 6 para datos de la cuenta",
            "TEXTO", null);

        seedField("dato_cuenta_7", "Dato Cuenta 7",
            "Campo adicional 7 para datos de la cuenta",
            "TEXTO", null);

        seedField("dato_cuenta_8", "Dato Cuenta 8",
            "Campo adicional 8 para datos de la cuenta",
            "TEXTO", null);

        seedField("dato_cuenta_9", "Dato Cuenta 9",
            "Campo adicional 9 para datos de la cuenta",
            "TEXTO", null);

        seedField("dato_cuenta_10", "Dato Cuenta 10",
            "Campo adicional 10 para datos de la cuenta",
            "TEXTO", null);

        logger.info("✓ Campos Numéricos - 38 campos creados");
        logger.info("✓ Datos Cliente - 10 campos creados");
        logger.info("✓ Datos Cuenta - 10 campos creados");
    }

    private void seedCamposTexto() {
        logger.info("--------------------------------------------------------------------");
        logger.info("Seeding Campos de Texto (34 campos)...");

        seedField("codigo_identificacion", "Código de identificación",
            "Código de identificación del cliente (C****, COD****, etc)",
            "TEXTO", null);

        seedField("documento", "Documento de Identidad",
            "Número de documento del cliente (DNI, CE, RUC)",
            "TEXTO", null);
        
        seedField("primer_nombre", "Primer Nombre",
            "Primer nombre del cliente",
            "TEXTO", null);

        seedField("segundo_nombre", "Segundo Nombre",
            "Segundo nombre del cliente",
            "TEXTO", null);

        seedField("primer_apellido", "Primer Apellido",
            "Primer apellido del cliente",
            "TEXTO", null);

        seedField("segundo_apellido", "Segundo Apellido",
            "Segundo apellido del cliente",
            "TEXTO", null);

        seedField("nombre_completo", "Nombre Completo",
            "Nombre y apellidos completos del cliente",
            "TEXTO", null);

        seedField("direccion", "Dirección",
            "Dirección domiciliaria completa",
            "TEXTO", null);

        seedField("distrito", "Distrito",
            "Distrito de residencia del cliente",
            "TEXTO", null);

        seedField("provincia", "Provincia",
            "Provincia de residencia del cliente",
            "TEXTO", null);

        seedField("departamento", "Departamento",
            "Departamento de residencia del cliente",
            "TEXTO", null);

        seedField("telefono_principal", "Teléfono Principal",
            "Número de teléfono principal de contacto",
            "TEXTO", null);

        seedField("telefono_secundario", "Teléfono Secundario",
            "Número de teléfono alternativo",
            "TEXTO", null);

        seedField("telefono_trabajo", "Teléfono de Trabajo",
            "Número de teléfono laboral",
            "TEXTO", null);

        seedField("email", "Correo Electrónico",
            "Dirección de correo electrónico",
            "TEXTO", null);

         seedField("cartera", "Cartera",
            "Cartera",
            "TEXTO", null);

        seedField("tipo_producto", "Tipo de Producto",
            "Tipo de producto financiero (préstamo, tarjeta, etc.)",
            "TEXTO", null);

        seedField("numero_contrato", "Número de Contrato",
            "Número identificador del contrato o cuenta",
            "TEXTO", null);

        seedField("gestor_asignado", "Gestor Asignado",
            "Nombre del gestor de cobranza asignado",
            "TEXTO", null);

        seedField("entidad_asignada", "Entidad Asignada",
            "Nombre de la entidad de cobranza asignada",
            "TEXTO", null);

        seedField("oficina_origen", "Oficina de Origen",
            "Oficina o agencia donde se originó el crédito",
            "TEXTO", null);

        seedField("estado_cuenta", "Estado de Cuenta",
            "Estado actual de la cuenta (vigente, vencido, castigado)",
            "TEXTO", null);

        seedField("motivo_mora", "Motivo de Mora",
            "Razón reportada del atraso en pagos",
            "TEXTO", null);

        seedField("resultado_gestion", "Resultado de Gestión",
            "Resultado del último contacto de cobranza",
            "TEXTO", null);

        seedField("observaciones", "Observaciones",
            "Notas y comentarios adicionales",
            "TEXTO", null);

        seedField("referencia_personal", "Referencia Personal",
            "Nombre de la persona de referencia",
            "TEXTO", null);

        seedField("ocupacion", "Ocupación",
            "Ocupación o profesión del cliente",
            "TEXTO", null);

        seedField("estado_civil", "Estado Civil",
            "Estado civil del cliente",
            "TEXTO", null);

        seedField("telefono_referencia_1", "Teléfono Referencia 1",
            "Número de teléfono de primera referencia",
            "TEXTO", null);

        seedField("telefono_referencia_2", "Teléfono Referencia 2",
            "Número de teléfono de segunda referencia",
            "TEXTO", null);

        seedField("periodo_asignacion", "Periodo de Asignación",
            "Periodo en que se asignó la cartera",
            "TEXTO", null);

        seedField("rango_mora", "Rango de Mora",
            "Rango o tramo de días de mora",
            "TEXTO", null);

        seedField("numero_cuenta_original", "Número Cuenta Original",
            "Número de cuenta original del cliente",
            "TEXTO", null);

        seedField("flag_dependiente", "Flag Dependencia",
            "Indicador de dependencia",
            "TEXTO", null);

        seedField("flag_linea_prestamo", "Flag Línea de Préstamo",
            "Indicador de línea de préstamo",
            "TEXTO", null);

        seedField("numero_cuenta_linea_prestamo", "Número Cuenta Línea Préstamo",
            "Número de cuenta de línea de préstamo",
            "TEXTO", null);

        seedField("estado_contencion", "Estado de Contención",
            "Estado de contención de la cuenta",
            "TEXTO", null);

        seedField("grupo_mora_inicial", "Grupo de Mora Inicial",
            "Grupo o rango de mora al momento de asignación",
            "TEXTO", null);

        seedField("grupo_mora", "Grupo de Mora",
            "Grupo o rango actual de mora",
            "TEXTO", null);

        seedField("probabilidad_pago", "Probabilidad de Pago",
            "Probabilidad estimada de pago del cliente",
            "TEXTO", null);

        seedField("procede_ajuste", "Procede Ajuste",
            "Indicador si procede ajuste en la cuenta",
            "TEXTO", null);

        seedField("estado_migracion", "Estado Migración",
            "Estado del proceso de migración de datos",
            "TEXTO", null);

        seedField("tipo_cliente", "Tipo de cliente",
            "Tipo de cliente",
            "TEXTO", null);

        seedField("procede_refinanciacion", "Procede Refinanciación",
            "Indicador si procede refinanciación del crédito",
            "TEXTO", null);

        logger.info("✓ Campos de Texto - 34 campos creados");
    }

    private void seedCamposFecha() {
        logger.info("--------------------------------------------------------------------");
        logger.info("Seeding Campos de Fecha (21 campos)...");

        seedField("fecha_vencimiento", "Fecha de Vencimiento",
            "Fecha de vencimiento de la obligación actual",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_emision", "Fecha de Emisión",
            "Fecha de emisión o facturación del documento",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_desembolso", "Fecha de Desembolso",
            "Fecha en que se realizó el desembolso del crédito",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_proximo_vencimiento", "Fecha Próximo Vencimiento",
            "Fecha de vencimiento de la próxima cuota",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_corte", "Fecha de Corte",
            "Fecha de corte del estado de cuenta",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_ingreso_cartera", "Fecha de Ingreso a Cartera",
            "Fecha en que ingresó a la cartera de cobranza",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_primera_cuota", "Fecha Primera Cuota",
            "Fecha de vencimiento de la primera cuota",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_ultima_cuota", "Fecha Última Cuota",
            "Fecha de vencimiento de la última cuota",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_castigo", "Fecha de Castigo",
            "Fecha en que la deuda fue castigada contablemente",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_refinanciacion", "Fecha de Refinanciación",
            "Fecha de la última refinanciación",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_actualizacion_datos", "Fecha Actualización de Datos",
            "Fecha de última actualización de información del cliente",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_carga_sistema", "Fecha de Carga al Sistema",
            "Fecha en que se cargó la información al sistema",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_proxima_gestion", "Fecha Próxima Gestión",
            "Fecha programada para el próximo contacto",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_liberacion_garantia", "Fecha Liberación de Garantía",
            "Fecha en que se liberó la garantía",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_ultimo_pago", "Fecha de Último Pago",
            "Fecha del último pago realizado por el cliente",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_promesa_pago", "Fecha Promesa de Pago",
            "Fecha comprometida por el cliente para realizar el pago",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_nacimiento", "Fecha de Nacimiento",
            "Fecha de nacimiento del cliente",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_inicio_mora", "Fecha Inicio de Mora",
            "Fecha en que comenzó la situación de mora",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_cierre_cuenta", "Fecha de Cierre de Cuenta",
            "Fecha en que se cerró o canceló la cuenta",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_ultimo_contacto", "Fecha de Último Contacto",
            "Fecha del último contacto exitoso con el cliente",
            "FECHA", "dd/MM/yyyy");

        seedField("fecha_activacion", "Fecha de Activación",
            "Fecha en que se activó el producto financiero",
            "FECHA", "dd/MM/yyyy");

        logger.info("✓ Campos de Fecha - 21 campos creados");
    }

    private void seedField(String fieldCode, String fieldName, String description,
                          String dataType, String format) {
        if (!fieldDefinitionRepository.existsByFieldCode(fieldCode)) {
            FieldDefinition field = new FieldDefinition(
                fieldCode, fieldName, description, dataType, format
            );
            fieldDefinitionRepository.save(field);
            logger.info("  ✓ {} - {}", fieldCode, fieldName);
        }
    }
}
