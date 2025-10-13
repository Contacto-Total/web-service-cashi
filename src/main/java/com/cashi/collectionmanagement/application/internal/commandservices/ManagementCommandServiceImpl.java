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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class ManagementCommandServiceImpl implements ManagementCommandService {

    private final ManagementRepository repository;
    private final ObjectMapper objectMapper;

    public ManagementCommandServiceImpl(ManagementRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
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
                String dynamicFieldsJson = objectMapper.writeValueAsString(command.dynamicFields());
                System.out.println("🔧 Campos Dinámicos:");
                System.out.println("   - Cantidad de campos: " + command.dynamicFields().size());
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

        return repository.save(management);
    }
}
