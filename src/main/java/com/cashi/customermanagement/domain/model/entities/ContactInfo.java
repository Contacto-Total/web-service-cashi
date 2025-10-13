package com.cashi.customermanagement.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "informacion_contacto")
@Getter
@NoArgsConstructor
public class ContactInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telefono_principal", length = 20)
    private String primaryPhone;

    @Column(name = "telefono_alternativo", length = 20)
    private String alternativePhone;

    @Column(name = "telefono_trabajo", length = 20)
    private String workPhone;

    @Column(name = "correo_electronico", length = 100)
    private String email;

    @Column(name = "direccion", columnDefinition = "TEXT")
    private String address;

    public ContactInfo(String primaryPhone, String alternativePhone, String workPhone,
                      String email, String address) {
        this.primaryPhone = primaryPhone;
        this.alternativePhone = alternativePhone;
        this.workPhone = workPhone;
        this.email = email;
        this.address = address;
    }
}
