package com.cashi.customermanagement.domain.model.aggregates;

import com.cashi.customermanagement.domain.model.entities.ContactMethod;
import com.cashi.customermanagement.domain.model.valueobjects.DocumentNumber;
import com.cashi.shared.domain.AggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer Aggregate Root
 */
@Entity
@Table(name = "clientes")
@Getter
@NoArgsConstructor
public class Customer extends AggregateRoot {

    @Column(name = "id_inquilino")
    private Long tenantId;

    @Column(name = "nombre_inquilino")
    private String tenantName;

    @Column(name = "id_cartera")
    private Long portfolioId;

    @Column(name = "nombre_cartera")
    private String portfolioName;

    @Column(name = "id_subcartera")
    private Long subPortfolioId;

    @Column(name = "nombre_subcartera")
    private String subPortfolioName;

    @Column(name = "id_cliente")
    private String customerId;

    @Column(name = "codigo_identificacion", length = 50)
    private String identificationCode;

    @Column(name = "documento", length = 20)
    private String document;

    @Column(name = "nombre_completo")
    private String fullName;

    @Column(name = "primer_nombre")
    private String firstName;

    @Column(name = "segundo_nombre")
    private String secondName;

    @Column(name = "primer_apellido")
    private String firstLastName;

    @Column(name = "segundo_apellido")
    private String secondLastName;

    @Column(name = "fecha_nacimiento")
    private LocalDate birthDate;

    @Column(name = "edad")
    private Integer age;

    @Column(name = "estado_civil")
    private String maritalStatus;

    @Column(name = "ocupacion")
    private String occupation;

    @Column(name = "tipo_cliente")
    private String customerType;

    @Column(name = "direccion")
    private String address;

    @Column(name = "distrito")
    private String district;

    @Column(name = "provincia")
    private String province;

    @Column(name = "departamento")
    private String department;

    @Column(name = "referencia_personal")
    private String personalReference;

    @Column(name = "numero_cuenta_linea_prestamo")
    private String accountNumber;

    // Información de deuda/mora
    @Column(name = "dias_mora")
    private Integer overdueDays;

    @Column(name = "monto_mora")
    private Double overdueAmount;

    @Column(name = "monto_capital")
    private Double principalAmount;

    @Column(name = "estado", length = 20)
    private String status;

    @Column(name = "fecha_importacion")
    private LocalDate importDate;

    @Column(name = "ultimo_acceso")
    private LocalDate lastAccessedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ContactMethod> contactMethods = new ArrayList<>();

    public Customer(Long tenantId, String identificationCode, String document, String fullName,
                   LocalDate birthDate, String status) {
        super();
        this.tenantId = tenantId;
        this.identificationCode = identificationCode;
        this.document = document;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.age = calculateAge(birthDate);
        this.status = status;
        this.importDate = LocalDate.now();
    }

    // Setters for sync service
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }

    public void setSubPortfolioId(Long subPortfolioId) {
        this.subPortfolioId = subPortfolioId;
    }

    public void setSubPortfolioName(String subPortfolioName) {
        this.subPortfolioName = subPortfolioName;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setIdentificationCode(String identificationCode) {
        this.identificationCode = identificationCode;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public void setFirstLastName(String firstLastName) {
        this.firstLastName = firstLastName;
    }

    public void setSecondLastName(String secondLastName) {
        this.secondLastName = secondLastName;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        this.age = calculateAge(birthDate);
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setPersonalReference(String personalReference) {
        this.personalReference = personalReference;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setOverdueDays(Integer overdueDays) {
        this.overdueDays = overdueDays;
    }

    public void setOverdueAmount(Double overdueAmount) {
        this.overdueAmount = overdueAmount;
    }

    public void setPrincipalAmount(Double principalAmount) {
        this.principalAmount = principalAmount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setImportDate(LocalDate importDate) {
        this.importDate = importDate;
    }

    public LocalDate getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void updateLastAccessedAt() {
        this.lastAccessedAt = LocalDate.now();
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) return null;
        return LocalDate.now().getYear() - birthDate.getYear();
    }
}
