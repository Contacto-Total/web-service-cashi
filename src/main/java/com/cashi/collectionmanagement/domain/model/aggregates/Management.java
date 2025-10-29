package com.cashi.collectionmanagement.domain.model.aggregates;

import com.cashi.shared.domain.model.entities.Portfolio;
import com.cashi.shared.domain.model.entities.SubPortfolio;
import com.cashi.shared.domain.model.entities.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "gestiones", indexes = {
    @Index(name = "idx_gest_inquilino", columnList = "id_inquilino"),
    @Index(name = "idx_gest_cartera", columnList = "id_cartera"),
    @Index(name = "idx_gest_subcartera", columnList = "id_subcartera"),
    @Index(name = "idx_gest_cliente", columnList = "id_cliente"),
    @Index(name = "idx_gest_asesor", columnList = "id_asesor")
})
@Getter
@NoArgsConstructor
public class Management {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Multi-tenant fields (nullable for backward compatibility)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inquilino")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cartera")
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_subcartera")
    private SubPortfolio subPortfolio;

    @Column(name = "id_cliente")
    private String customerId;

    @Column(name = "id_asesor")
    private String advisorId;

    @Column(name = "telefono", length = 50)
    private String phone;

    // Niveles de categorización jerárquica
    @Column(name = "nivel1_id")
    private Long level1Id;

    @Column(name = "nivel1_nombre", length = 255)
    private String level1Name;

    @Column(name = "nivel2_id")
    private Long level2Id;

    @Column(name = "nivel2_nombre", length = 255)
    private String level2Name;

    @Column(name = "nivel3_id")
    private Long level3Id;

    @Column(name = "nivel3_nombre", length = 255)
    private String level3Name;

    @Column(name = "observaciones", length = 2000)
    private String observations;

    @Column(name = "tipificacion_requiere_pago")
    private Boolean typificationRequiresPayment;

    @Column(name = "tipificacion_requiere_cronograma")
    private Boolean typificationRequiresSchedule;

    // Campos automáticos de fecha y hora de gestión
    @Column(name = "fecha_gestion", nullable = false)
    private LocalDate managementDate;

    @Column(name = "hora_gestion", nullable = false)
    private LocalTime managementTime;

    // Constructor
    public Management(Tenant tenant, Portfolio portfolio, SubPortfolio subPortfolio,
                     String customerId, String advisorId, String phone) {
        this.tenant = tenant;
        this.portfolio = portfolio;
        this.subPortfolio = subPortfolio;
        this.customerId = customerId;
        this.advisorId = advisorId;
        this.phone = phone;
        // Establecer fecha y hora automáticamente
        this.managementDate = LocalDate.now();
        this.managementTime = LocalTime.now();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public void setTypificationRequiresPayment(Boolean typificationRequiresPayment) {
        this.typificationRequiresPayment = typificationRequiresPayment;
    }

    public void setTypificationRequiresSchedule(Boolean typificationRequiresSchedule) {
        this.typificationRequiresSchedule = typificationRequiresSchedule;
    }

    public void setSubPortfolio(SubPortfolio subPortfolio) {
        this.subPortfolio = subPortfolio;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setLevel1(Long id, String name) {
        this.level1Id = id;
        this.level1Name = name;
    }

    public void setLevel2(Long id, String name) {
        this.level2Id = id;
        this.level2Name = name;
    }

    public void setLevel3(Long id, String name) {
        this.level3Id = id;
        this.level3Name = name;
    }

}
