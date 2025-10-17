package com.cashi.collectionmanagement.application.internal.commandservices;

import com.cashi.collectionmanagement.domain.model.aggregates.Management;
import com.cashi.collectionmanagement.domain.model.commands.*;
import com.cashi.collectionmanagement.domain.model.entities.CallDetail;
import com.cashi.collectionmanagement.domain.model.entities.PaymentDetail;
import com.cashi.collectionmanagement.domain.model.valueobjects.ContactResult;
import com.cashi.collectionmanagement.domain.model.valueobjects.ManagementType;
import com.cashi.collectionmanagement.domain.model.valueobjects.PaymentMethod;
import com.cashi.collectionmanagement.domain.services.ManagementCommandService;
import com.cashi.collectionmanagement.infrastructure.persistence.jpa.repositories.ManagementRepository;
import com.cashi.paymentprocessing.domain.model.aggregates.PaymentSchedule;
import com.cashi.paymentprocessing.domain.model.entities.Installment;
import com.cashi.paymentprocessing.domain.services.InstallmentStatusCommandService;
import com.cashi.paymentprocessing.infrastructure.persistence.jpa.repositories.PaymentScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ManagementCommandServiceImpl implements ManagementCommandService {

    private final ManagementRepository repository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final InstallmentStatusCommandService installmentStatusService;
    private final ObjectMapper objectMapper;

    public ManagementCommandServiceImpl(ManagementRepository repository,
                                       PaymentScheduleRepository paymentScheduleRepository,
                                       InstallmentStatusCommandService installmentStatusService,
                                       ObjectMapper objectMapper) {
        this.repository = repository;
        this.paymentScheduleRepository = paymentScheduleRepository;
        this.installmentStatusService = installmentStatusService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Management handle(CreateManagementCommand command) {
        System.out.println("========================================");
        System.out.println("📝 INICIANDO CREACIÓN DE GESTIÓN");
        System.out.println("========================================");
        System.out.println("📋 Tabla destino: gestiones");
        System.out.println("👤 Cliente ID: " + command.customerId());
        System.out.println("👨‍💼 Asesor ID: " + command.advisorId());
        System.out.println("📢 Campaña ID: " + command.campaignId());

        var management = new Management(
            command.customerId(),
            command.advisorId(),
            command.campaignId()
        );

        // Clasificación: Categoría/grupo al que pertenece la tipificación
        if (command.classificationCode() != null) {
            System.out.println("📁 Clasificación (Categoría):");
            System.out.println("   - Código: " + command.classificationCode());
            System.out.println("   - Descripción: " + command.classificationDescription());
            System.out.println("   - Columnas BD: codigo_clasificacion, descripcion_clasificacion");

            management.setClassification(
                command.classificationCode(),
                command.classificationDescription()
            );
        }

        // Tipificación: Código específico/hoja (último nivel en jerarquía)
        if (command.typificationCode() != null) {
            System.out.println("🏷️  Tipificación (Hoja específica):");
            System.out.println("   - Código: " + command.typificationCode());
            System.out.println("   - Descripción: " + command.typificationDescription());
            System.out.println("   - Requiere Pago: " + command.typificationRequiresPayment());
            System.out.println("   - Requiere Cronograma: " + command.typificationRequiresSchedule());
            System.out.println("   - Columnas BD: codigo_tipificacion, descripcion_tipificacion, tipificacion_requiere_pago, tipificacion_requiere_cronograma");

            management.setTypification(
                command.typificationCode(),
                command.typificationDescription(),
                command.typificationRequiresPayment(),
                command.typificationRequiresSchedule()
            );
        }

        if (command.observations() != null) {
            System.out.println("💬 Observaciones: " + command.observations());
            System.out.println("   - Columna BD: observaciones");
            management.setObservations(command.observations());
        }

        // Serializar campos dinámicos a JSON
        if (command.dynamicFields() != null && !command.dynamicFields().isEmpty()) {
            try {
                // Calcular saldo pendiente automáticamente si viene monto_pagado
                Map<String, Object> enrichedFields = enrichDynamicFieldsWithPendingBalance(
                    command.dynamicFields(),
                    command.customerId()
                );

                String dynamicFieldsJson = objectMapper.writeValueAsString(enrichedFields);
                System.out.println("🔧 Campos Dinámicos:");
                System.out.println("   - Cantidad de campos: " + enrichedFields.size());
                System.out.println("   - Columna BD: campos_dinamicos_json");
                System.out.println("   - JSON guardado:");
                System.out.println(dynamicFieldsJson);

                management.setDynamicFieldsJson(dynamicFieldsJson);
            } catch (Exception e) {
                System.err.println("❌ Error serializando campos dinámicos: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ℹ️  No hay campos dinámicos para guardar");
        }

        System.out.println("----------------------------------------");
        System.out.println("💾 Guardando en base de datos...");
        Management savedManagement = repository.save(management);

        System.out.println("✅ GESTIÓN GUARDADA EXITOSAMENTE");
        System.out.println("   - ID Gestión: " + savedManagement.getManagementId().getManagementId());
        System.out.println("   - Tabla: gestiones");
        System.out.println("   - Fecha: " + savedManagement.getManagementDate());
        System.out.println("========================================");

        // Detectar y guardar cronogramas de pago (campos tipo tabla)
        if (command.dynamicFields() != null && !command.dynamicFields().isEmpty()) {
            processPaymentSchedules(command.dynamicFields(), savedManagement);
        }

        return savedManagement;
    }

    @Override
    public Management handle(UpdateManagementCommand command) {
        var management = repository.findByManagementId_ManagementId(command.managementId())
            .orElseThrow(() -> new IllegalArgumentException("Management not found: " + command.managementId()));

        // Actualizar Clasificación
        if (command.classificationCode() != null) {
            management.setClassification(
                command.classificationCode(),
                command.classificationDescription()
            );
        }

        // Actualizar Tipificación
        if (command.typificationCode() != null) {
            management.setTypification(
                command.typificationCode(),
                command.typificationDescription(),
                command.typificationRequiresPayment(),
                command.typificationRequiresSchedule()
            );
        }

        // Actualizar observaciones
        if (command.observations() != null) {
            management.setObservations(command.observations());
        }

        return repository.save(management);
    }

    @Override
    public Management handle(StartCallCommand command) {
        var management = repository.findByManagementId_ManagementId(command.managementId())
            .orElseThrow(() -> new IllegalArgumentException("Management not found: " + command.managementId()));

        var callDetail = new CallDetail(command.phoneNumber(), command.startTime());
        management.setCallDetail(callDetail);

        return repository.save(management);
    }

    @Override
    public Management handle(EndCallCommand command) {
        var management = repository.findByManagementId_ManagementId(command.managementId())
            .orElseThrow(() -> new IllegalArgumentException("Management not found: " + command.managementId()));

        if (management.getCallDetail() != null) {
            management.getCallDetail().endCall(command.endTime());
        }

        return repository.save(management);
    }

    @Override
    public Management handle(RegisterPaymentCommand command) {
        System.out.println("\n========================================");
        System.out.println("💳 REGISTRANDO PAGO");
        System.out.println("========================================");

        var management = repository.findByManagementId_ManagementId(command.managementId())
            .orElseThrow(() -> new IllegalArgumentException("Management not found: " + command.managementId()));

        var paymentMethod = new PaymentMethod(
            command.paymentMethodType(),
            command.paymentMethodDetails()
        );

        var paymentDetail = new PaymentDetail(
            command.amount(),
            command.scheduledDate(),
            paymentMethod
        );

        if (command.voucherNumber() != null) {
            paymentDetail.setVoucherDetails(command.voucherNumber(), command.bankName());
        }

        management.setPaymentDetail(paymentDetail);
        Management savedManagement = repository.save(management);

        System.out.println("✅ Pago registrado: S/ " + command.amount());
        System.out.println("   - Gestión ID: " + command.managementId());
        System.out.println("   - Cliente ID: " + management.getCustomerId());
        System.out.println("   - Tipificación: " + management.getTypificationCode());

        // Verificar si esta tipificación aplica pagos a cronogramas automáticamente
        String typificationCode = management.getTypificationCode();
        boolean appliesPaymentToSchedule = isPaymentApplicableToSchedule(typificationCode);

        if (appliesPaymentToSchedule) {
            System.out.println("\n🔗 Esta tipificación enlaza pagos con cronogramas pendientes");
            applyPaymentToPendingSchedules(management.getCustomerId(), command.amount(), command.managementId());
        } else {
            System.out.println("\nℹ️  Esta tipificación NO enlaza con cronogramas");
        }

        System.out.println("========================================\n");

        return savedManagement;
    }

    /**
     * Verifica si una tipificación aplica pagos a cronogramas automáticamente
     * Configurable por código de tipificación
     */
    private boolean isPaymentApplicableToSchedule(String typificationCode) {
        if (typificationCode == null) return false;

        // Tipificaciones de Financiera OH que enlazan con cronogramas:
        // PC - Pago Confirmado
        // PT - Pago Total
        // PP - Pago Parcial
        // PPT - Pago por Tercero
        return typificationCode.equals("PC") ||
               typificationCode.equals("PT") ||
               typificationCode.equals("PP") ||
               typificationCode.equals("PPT");
    }

    /**
     * Aplica un pago a cronogramas pendientes del cliente
     * Busca cuotas pendientes en orden y las marca como pagadas
     */
    private void applyPaymentToPendingSchedules(String customerId, BigDecimal paymentAmount, String managementId) {
        System.out.println("🔍 Buscando cronogramas activos del cliente: " + customerId);

        // Buscar cronogramas activos del cliente
        List<PaymentSchedule> activeSchedules = paymentScheduleRepository.findByCustomerIdAndIsActiveTrue(customerId);

        if (activeSchedules.isEmpty()) {
            System.out.println("   ⚠️  No se encontraron cronogramas activos para este cliente");
            return;
        }

        System.out.println("   ✅ " + activeSchedules.size() + " cronograma(s) activo(s) encontrado(s)");

        BigDecimal remainingPayment = paymentAmount;
        int installmentsPaid = 0;

        // Procesar cada cronograma (por orden de fecha de inicio, más antiguos primero)
        activeSchedules.sort((s1, s2) -> s1.getStartDate().compareTo(s2.getStartDate()));

        for (PaymentSchedule schedule : activeSchedules) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break; // Ya no hay más pago por aplicar
            }

            System.out.println("\n   📋 Procesando cronograma: " + schedule.getScheduleId().getScheduleId());
            System.out.println("      - Cuotas totales: " + schedule.getNumberOfInstallments());
            System.out.println("      - Cuotas pagadas: " + schedule.getPaidInstallments());
            System.out.println("      - Cuotas pendientes: " + schedule.getPendingInstallments());

            // Obtener cuotas pendientes en orden
            List<Installment> pendingInstallments = schedule.getInstallments().stream()
                .filter(Installment::isPending)
                .sorted((i1, i2) -> i1.getDueDate().compareTo(i2.getDueDate()))
                .toList();

            if (pendingInstallments.isEmpty()) {
                System.out.println("      ✅ Este cronograma ya está completamente pagado");
                continue;
            }

            // Aplicar pago a cuotas pendientes
            for (Installment installment : pendingInstallments) {
                if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal installmentAmount = installment.getAmount();

                if (remainingPayment.compareTo(installmentAmount) >= 0) {
                    // Pago completo de la cuota
                    installment.markAsPaid(LocalDate.now());
                    remainingPayment = remainingPayment.subtract(installmentAmount);
                    installmentsPaid++;

                    System.out.println("      💰 Cuota #" + installment.getInstallmentNumber() + " PAGADA COMPLETA");
                    System.out.println("         - Monto cuota: S/ " + installmentAmount);
                    System.out.println("         - Pago restante: S/ " + remainingPayment);

                    // Registrar en historial de estados
                    try {
                        installmentStatusService.registerPayment(
                            installment.getId(),
                            managementId,
                            java.time.LocalDateTime.now(),
                            installmentAmount,
                            "Pago aplicado automáticamente desde gestión",
                            "SYSTEM"
                        );
                    } catch (Exception e) {
                        System.err.println("         ⚠️  Error registrando historial de cuota: " + e.getMessage());
                    }
                } else {
                    // Pago parcial de la cuota (por ahora solo registramos en logs)
                    System.out.println("      ⚠️  Cuota #" + installment.getInstallmentNumber() + " - Pago PARCIAL");
                    System.out.println("         - Monto cuota: S/ " + installmentAmount);
                    System.out.println("         - Pago disponible: S/ " + remainingPayment);
                    System.out.println("         - La cuota queda PENDIENTE (pago parcial no soportado aún)");
                    break; // No procesamos más cuotas de este cronograma
                }
            }

            // Guardar cambios del cronograma
            paymentScheduleRepository.save(schedule);
        }

        System.out.println("\n   ✅ Resumen de aplicación de pago:");
        System.out.println("      - Monto pagado: S/ " + paymentAmount);
        System.out.println("      - Cuotas pagadas: " + installmentsPaid);
        System.out.println("      - Sobrante: S/ " + remainingPayment);
    }

    /**
     * Procesa campos dinámicos tipo tabla (cronogramas de pago) y los guarda en tablas separadas
     */
    @SuppressWarnings("unchecked")
    private void processPaymentSchedules(Map<String, Object> dynamicFields, Management management) {
        System.out.println("========================================");
        System.out.println("📊 DETECTANDO CRONOGRAMAS DE PAGO");
        System.out.println("========================================");

        // Buscar campos que sean arrays (potencialmente tablas/cronogramas)
        dynamicFields.forEach((fieldId, fieldValue) -> {
            // Los campos tipo tabla vienen como List<Map<String, Object>>
            if (fieldValue instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
                System.out.println("📋 Campo tipo tabla detectado: " + fieldId);

                // Identificar si es un cronograma de pagos por el fieldId
                if (fieldId.toLowerCase().contains("cronograma")) {
                    System.out.println("💰 Es un CRONOGRAMA DE PAGOS - guardando en tablas separadas");
                    savePaymentSchedule((List<Map<String, Object>>) fieldValue, management);
                } else {
                    System.out.println("ℹ️  Es una tabla genérica - se mantiene en JSON");
                }
            }
        });

        System.out.println("========================================\n");
    }

    /**
     * Guarda el cronograma de pagos en las tablas cronogramas_pago y cuotas
     * ACTUALIZADO: Usa el nuevo constructor que respeta montos personalizados por cuota
     */
    private void savePaymentSchedule(List<Map<String, Object>> scheduleRows, Management management) {
        try {
            System.out.println("💾 Guardando cronograma en tablas normalizadas:");
            System.out.println("   - Tabla cabecera: cronogramas_pago");
            System.out.println("   - Tabla detalle: cuotas");
            System.out.println("   - Número de cuotas: " + scheduleRows.size());

            // Construir lista de datos de cuotas con montos personalizados
            List<PaymentSchedule.InstallmentData> installmentDataList = new java.util.ArrayList<>();
            int installmentNumber = 1;

            for (Map<String, Object> row : scheduleRows) {
                // Extraer monto personalizado de la cuota
                Object montoObj = row.get("monto");
                if (montoObj == null) {
                    System.err.println("⚠️  Cuota #" + installmentNumber + " sin monto, omitiendo");
                    continue;
                }
                BigDecimal amount = new BigDecimal(montoObj.toString());

                // Extraer fecha de vencimiento
                Object fechaObj = row.get("fecha_vencimiento");
                if (fechaObj == null) {
                    fechaObj = row.get("fecha_pago"); // Fallback para compatibilidad
                }
                LocalDate dueDate = (fechaObj instanceof String fechaStr)
                    ? LocalDate.parse(fechaStr)
                    : LocalDate.now().plusMonths(installmentNumber - 1);

                // Agregar a la lista de datos de cuotas
                installmentDataList.add(new PaymentSchedule.InstallmentData(
                    installmentNumber,
                    amount,
                    dueDate
                ));

                System.out.println("   📝 Cuota #" + installmentNumber + ": S/ " + amount + " - Vence: " + dueDate);
                installmentNumber++;
            }

            if (installmentDataList.isEmpty()) {
                System.err.println("❌ No se pudieron extraer cuotas del cronograma");
                return;
            }

            // Crear el PaymentSchedule usando el NUEVO constructor con montos personalizados
            // scheduleType: "FINANCIERA" para convenios con entidad financiera
            // negotiatedAmount: monto total negociado/acordado
            BigDecimal totalAmount = installmentDataList.stream()
                .map(PaymentSchedule.InstallmentData::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            PaymentSchedule paymentSchedule = new PaymentSchedule(
                management.getCustomerId(),
                management.getManagementId().getManagementId(),
                "FINANCIERA", // Tipo de cronograma para convenios
                totalAmount,  // Monto negociado
                installmentDataList // Lista de cuotas con montos personalizados
            );

            // Guardar
            PaymentSchedule savedSchedule = paymentScheduleRepository.save(paymentSchedule);

            System.out.println("✅ Cronograma guardado exitosamente:");
            System.out.println("   - ID Cronograma: " + savedSchedule.getScheduleId().getScheduleId());
            System.out.println("   - Tipo: " + savedSchedule.getScheduleType());
            System.out.println("   - Monto Total: S/ " + totalAmount);
            System.out.println("   - Número de Cuotas: " + installmentDataList.size());
            System.out.println("   - Las cuotas se crearon con MONTOS PERSONALIZADOS del cronograma frontend");

            // Crear registros de historial inicial (PENDIENTE) para cada cuota
            System.out.println("\n📋 Creando registros de historial inicial para cada cuota:");
            List<Installment> installments = savedSchedule.getInstallments();
            for (Installment installment : installments) {
                installmentStatusService.registerInitialStatus(
                    installment.getId(),
                    management.getManagementId().getManagementId(),
                    "SYSTEM" // Por ahora usamos SYSTEM, luego puede ser el usuario actual
                );
            }
            System.out.println("✅ Historial inicial creado para " + installments.size() + " cuotas");

        } catch (Exception e) {
            System.err.println("❌ Error guardando cronograma de pagos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Enriquece los campos dinámicos calculando automáticamente el saldo_pendiente
     * si viene el campo monto_pagado.
     *
     * Fórmula: saldo_pendiente = (suma de cuotas pendientes) - (monto pagado)
     */
    private Map<String, Object> enrichDynamicFieldsWithPendingBalance(
        Map<String, Object> dynamicFields,
        String customerId
    ) {
        // Si no hay monto_pagado, retornar los campos sin cambios
        if (!dynamicFields.containsKey("monto_pagado")) {
            return dynamicFields;
        }

        try {
            Object montoPagadoObj = dynamicFields.get("monto_pagado");
            if (montoPagadoObj == null) {
                return dynamicFields;
            }

            BigDecimal montoPagado = new BigDecimal(montoPagadoObj.toString());

            System.out.println("\n💰 CALCULANDO SALDO PENDIENTE AUTOMÁTICAMENTE");
            System.out.println("   - Monto pagado: S/ " + montoPagado);

            // Obtener cronogramas activos del cliente
            List<PaymentSchedule> activeSchedules = paymentScheduleRepository
                .findByCustomerIdAndIsActiveTrue(customerId);

            if (activeSchedules.isEmpty()) {
                System.out.println("   ⚠️  No hay cronogramas activos, saldo pendiente = 0");
                // Crear un nuevo mapa con el campo saldo_pendiente
                Map<String, Object> enrichedFields = new java.util.HashMap<>(dynamicFields);
                enrichedFields.put("saldo_pendiente", BigDecimal.ZERO);
                return enrichedFields;
            }

            // Calcular suma total de cuotas pendientes
            BigDecimal totalPendiente = BigDecimal.ZERO;

            for (PaymentSchedule schedule : activeSchedules) {
                List<Installment> pendingInstallments = schedule.getInstallments().stream()
                    .filter(Installment::isPending)
                    .toList();

                for (Installment installment : pendingInstallments) {
                    totalPendiente = totalPendiente.add(installment.getAmount());
                }
            }

            System.out.println("   - Total pendiente en cronogramas: S/ " + totalPendiente);

            // Calcular saldo pendiente: Total pendiente - Monto pagado
            BigDecimal saldoPendiente = totalPendiente.subtract(montoPagado);

            // Asegurar que no sea negativo
            if (saldoPendiente.compareTo(BigDecimal.ZERO) < 0) {
                saldoPendiente = BigDecimal.ZERO;
            }

            System.out.println("   - Saldo pendiente calculado: S/ " + saldoPendiente);
            System.out.println("   ✅ Campo saldo_pendiente agregado automáticamente\n");

            // Crear un nuevo mapa con el campo saldo_pendiente
            Map<String, Object> enrichedFields = new java.util.HashMap<>(dynamicFields);
            enrichedFields.put("saldo_pendiente", saldoPendiente);

            return enrichedFields;

        } catch (Exception e) {
            System.err.println("   ❌ Error calculando saldo pendiente: " + e.getMessage());
            e.printStackTrace();
            return dynamicFields; // Retornar los campos originales en caso de error
        }
    }
}
