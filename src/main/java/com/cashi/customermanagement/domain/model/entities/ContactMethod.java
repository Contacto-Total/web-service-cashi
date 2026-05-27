package com.cashi.customermanagement.domain.model.entities;

import com.cashi.customermanagement.domain.model.aggregates.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "metodos_contacto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMethod {

    // Homologado con com.callcenter.model.MetodoContacto (cashi-discador-backend).
    public enum EstadoContactabilidad {
        NUEVO,
        NO_CONTACTADO,
        INVALIDO,
        CONTACTADO,
        CONTACTO_TERCERO,
        CONTACTO_TITULAR,
        INVALIDO_CONFIRMADO
    }

    public enum EstadoOsiptel {
        SIN_VALIDAR,
        PERTENECE,
        NO_PERTENECE
    }

    public enum EstadoWhatsapp {
        SIN_VALIDAR,
        TIENE,
        NO_TIENE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Customer customer;

    /**
     * Tipo de contacto: "telefono" o "email"
     */
    @Column(name = "tipo_contacto", nullable = false, length = 50)
    private String contactType;

    /**
     * Subtipo específico del contacto
     * Para teléfonos: telefono_principal, telefono_secundario, telefono_trabajo, telefono_referencia_1, telefono_referencia_2
     * Para email: email
     */
    @Column(name = "subtipo", nullable = false, length = 100)
    private String subtype;

    /**
     * Valor del contacto (número de teléfono o email)
     */
    @Column(name = "valor", nullable = false, length = 255)
    private String value;

    /**
     * Etiqueta/nombre de la cabecera original del CSV
     */
    @Column(name = "etiqueta", length = 255)
    private String label;

    /**
     * Fecha en que se importó este método de contacto
     */
    @Column(name = "fecha_importacion")
    private LocalDate importDate;

    /**
     * Estado del método de contacto
     */
    @Column(name = "estado", length = 50)
    private String status;

    /**
     * Estado de contactabilidad unificado, derivado de la ultima tipificacion.
     * Valores: NUEVO | NO_CONTACTADO | INVALIDO | CONTACTADO | CONTACTO_TERCERO | CONTACTO_TITULAR | INVALIDO_CONFIRMADO.
     */
    @Column(name = "estado_contactabilidad", length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoContactabilidad estadoContactabilidad = EstadoContactabilidad.NUEVO;

    /**
     * Estado de validacion vs portal Osiptel.
     * Valores: SIN_VALIDAR | PERTENECE | NO_PERTENECE.
     */
    @Column(name = "estado_osiptel", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoOsiptel estadoOsiptel = EstadoOsiptel.SIN_VALIDAR;

    /**
     * Estado de validacion WhatsApp.
     * Valores: SIN_VALIDAR | TIENE | NO_TIENE.
     */
    @Column(name = "estado_whatsapp", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoWhatsapp estadoWhatsapp = EstadoWhatsapp.SIN_VALIDAR;

}
