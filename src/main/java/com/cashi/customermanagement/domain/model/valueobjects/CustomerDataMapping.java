package com.cashi.customermanagement.domain.model.valueobjects;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Configuración de mapeo de datos de clientes por tenant
 */
@Data
public class CustomerDataMapping {
    private String importSourceType;
    private String sourceTableName;
    private Map<String, String> columnMapping;
    private CustomerDisplayConfig customerDisplayConfig;

    @Data
    public static class CustomerDisplayConfig {
        private String title;
        private List<DisplaySection> sections;
    }

    @Data
    public static class DisplaySection {
        private String sectionTitle;
        private String colorClass;
        private List<DisplayField> fields;
    }

    @Data
    public static class DisplayField {
        private String field;
        private String label;
        private Integer displayOrder;
        private String format;
        private Boolean highlight;
    }
}
