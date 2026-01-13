package com.cashi.shared.domain.model.entities;

import com.cashi.shared.domain.model.valueobjects.LoadType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * HeaderChangeHistory Entity - Historial de cambios en configuración de cabeceras
 * Registra cambios como: alias agregados, columnas nuevas detectadas, columnas ignoradas
 */
@Entity
@Table(name = "historial_cambios_cabeceras", indexes = {
    @Index(name = "idx_historial_subcartera", columnList = "id_subcartera"),
    @Index(name = "idx_historial_fecha", columnList = "fecha_cambio"),
    @Index(name = "idx_historial_tipo", columnList = "tipo_cambio")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class HeaderChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Subcartera afectada
     */
    @Column(name = "id_subcartera", nullable = false)
    private Integer subPortfolioId;

    /**
     * Tipo de carga: INICIAL o ACTUALIZACION
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_carga", nullable = false, length = 20)
    private LoadType loadType;

    /**
     * Tipo de cambio realizado
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cambio", nullable = false, length = 50)
    private ChangeType changeType;

    /**
     * Nombre de la columna tal como aparece en el Excel
     */
    @Column(name = "nombre_columna_excel", nullable = false, length = 100)
    private String excelColumnName;

    /**
     * Nombre interno de la cabecera (si aplica)
     */
    @Column(name = "nombre_cabecera_interna", length = 100)
    private String internalHeaderName;

    /**
     * Referencia a la configuración de cabecera afectada (si aplica)
     */
    @Column(name = "id_configuracion_cabecera")
    private Integer headerConfigurationId;

    /**
     * Usuario que realizó el cambio
     */
    @Column(name = "usuario", length = 100)
    private String username;

    /**
     * Fecha y hora del cambio
     */
    @CreatedDate
    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    /**
     * Información adicional en formato JSON
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /**
     * Tipos de cambios posibles
     */
    public enum ChangeType {
        ALIAS_AGREGADO,      // Se agregó un alias a una cabecera
        ALIAS_REMOVIDO,      // Se eliminó un alias de una cabecera
        COLUMNA_NUEVA,       // Se detectó y creó una nueva columna
        COLUMNA_IGNORADA,    // Se marcó una columna como ignorada
        COLUMNA_MAPEADA      // Una columna del Excel se mapeó a una existente via alias
    }

    /**
     * Constructor para crear historial de cambio
     */
    public HeaderChangeHistory(Integer subPortfolioId, LoadType loadType, ChangeType changeType,
                               String excelColumnName, String internalHeaderName,
                               Integer headerConfigurationId, String username) {
        this.subPortfolioId = subPortfolioId;
        this.loadType = loadType;
        this.changeType = changeType;
        this.excelColumnName = excelColumnName;
        this.internalHeaderName = internalHeaderName;
        this.headerConfigurationId = headerConfigurationId;
        this.username = username;
    }

    /**
     * Constructor simplificado para alias
     */
    public static HeaderChangeHistory aliasAdded(Integer subPortfolioId, LoadType loadType,
                                                  String aliasName, String internalHeaderName,
                                                  Integer headerConfigId, String username) {
        return new HeaderChangeHistory(subPortfolioId, loadType, ChangeType.ALIAS_AGREGADO,
                aliasName, internalHeaderName, headerConfigId, username);
    }

    /**
     * Constructor simplificado para columna nueva
     */
    public static HeaderChangeHistory newColumn(Integer subPortfolioId, LoadType loadType,
                                                 String columnName, String username) {
        return new HeaderChangeHistory(subPortfolioId, loadType, ChangeType.COLUMNA_NUEVA,
                columnName, columnName, null, username);
    }

    /**
     * Constructor simplificado para columna ignorada
     */
    public static HeaderChangeHistory ignoredColumn(Integer subPortfolioId, LoadType loadType,
                                                     String columnName, String username) {
        return new HeaderChangeHistory(subPortfolioId, loadType, ChangeType.COLUMNA_IGNORADA,
                columnName, null, null, username);
    }
}
