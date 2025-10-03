package com.cashi.customermanagement.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer_contact_info")
@Getter
@NoArgsConstructor
public class ContactInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "primary_phone", length = 20)
    private String primaryPhone;

    @Column(name = "alternative_phone", length = 20)
    private String alternativePhone;

    @Column(name = "work_phone", length = 20)
    private String workPhone;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
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
