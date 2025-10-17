package com.cashi.customermanagement.domain.model.aggregates;

import com.cashi.customermanagement.domain.model.entities.AccountInfo;
import com.cashi.customermanagement.domain.model.entities.ContactInfo;
import com.cashi.customermanagement.domain.model.entities.DebtInfo;
import com.cashi.customermanagement.domain.model.valueobjects.DocumentNumber;
import com.cashi.shared.domain.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Customer Aggregate Root
 */
@Entity
@Table(name = "clientes")
@Getter
@NoArgsConstructor
public class Customer extends AggregateRoot {

    @Column(name = "id_inquilino", nullable = false)
    private Long tenantId;

    @Column(name = "id_cliente", unique = true, nullable = false, length = 36)
    private String customerId;

    @Column(name = "codigo_documento", length = 50)
    private String documentCode;

    @Column(name = "nombre_completo", nullable = false)
    private String fullName;

    @Embedded
    private DocumentNumber documentNumber;

    @Column(name = "fecha_nacimiento")
    private LocalDate birthDate;

    @Column(name = "edad")
    private Integer age;

    @Column(name = "estado", length = 20)
    private String status;

    @Column(name = "fecha_importacion")
    private LocalDate importDate;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_informacion_contacto")
    private ContactInfo contactInfo;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_informacion_cuenta")
    private AccountInfo accountInfo;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_informacion_deuda")
    private DebtInfo debtInfo;

    public Customer(Long tenantId, String customerId, String documentCode, String fullName,
                   DocumentNumber documentNumber, LocalDate birthDate, String status) {
        super();
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.documentCode = documentCode;
        this.fullName = fullName;
        this.documentNumber = documentNumber;
        this.birthDate = birthDate;
        this.age = calculateAge(birthDate);
        this.status = status;
        this.importDate = LocalDate.now();
    }

    public void updateContactInfo(ContactInfo contactInfo) {
        this.contactInfo = contactInfo;
        updateTimestamp();
    }

    public void updateAccountInfo(AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
        updateTimestamp();
    }

    public void updateDebtInfo(DebtInfo debtInfo) {
        this.debtInfo = debtInfo;
        updateTimestamp();
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) return null;
        return LocalDate.now().getYear() - birthDate.getYear();
    }
}
