package com.cashi.shared.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * HeaderAlias Entity - Nombres alternativos para cabeceras de configuración
 * Permite que una cabecera sea reconocida por múltiples nombres en los archivos Excel
 */
@Entity
@Table(name = "alias_cabeceras", indexes = {
    @Index(name = "idx_alias_config", columnList = "id_configuracion_cabecera"),
    @Index(name = "idx_alias_nombre", columnList = "alias"),
    @Index(name = "idx_alias_busqueda", columnList = "alias")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class HeaderAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Referencia a la configuración de cabecera padre
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_configuracion_cabecera", nullable = false)
    private HeaderConfiguration headerConfiguration;

    /**
     * Nombre alternativo de la cabecera
     * Este es el nombre que puede aparecer en el Excel y será mapeado a headerName
     */
    @Column(name = "alias", nullable = false, length = 100)
    private String alias;

    /**
     * Indica si este es el nombre principal de la cabecera
     * 1 = nombre principal (mismo que headerName)
     * 0 = alias alternativo
     */
    @Column(name = "es_principal", nullable = false)
    private Integer esPrincipal = 0;

    @CreatedDate
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Constructor para crear un alias
     */
    public HeaderAlias(HeaderConfiguration headerConfiguration, String alias, boolean esPrincipal) {
        this.headerConfiguration = headerConfiguration;
        this.alias = alias;
        this.esPrincipal = esPrincipal ? 1 : 0;
    }

    /**
     * Verifica si este alias es el nombre principal
     */
    public boolean isPrincipal() {
        return esPrincipal != null && esPrincipal == 1;
    }
}
