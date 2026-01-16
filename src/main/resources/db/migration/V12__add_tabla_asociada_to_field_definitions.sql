-- ========================================
-- MIGRACIÓN V12: Agregar tabla_asociada a definiciones_campos
-- Permite identificar a qué tabla de BD pertenece cada campo
-- ========================================

-- 1. Agregar columna tabla_asociada
ALTER TABLE definiciones_campos
ADD COLUMN tabla_asociada VARCHAR(100) DEFAULT NULL;

-- 2. Crear índice para búsquedas por tabla
CREATE INDEX idx_tabla_asociada ON definiciones_campos(tabla_asociada);

-- 3. Poblar tabla_asociada para campos de la tabla CLIENTES
UPDATE definiciones_campos SET tabla_asociada = 'clientes' WHERE codigo_campo IN (
    -- Identificación
    'codigo_identificacion',
    'documento',
    'id_cliente',

    -- Información personal
    'nombre_completo',
    'primer_nombre',
    'segundo_nombre',
    'primer_apellido',
    'segundo_apellido',

    -- Datos demográficos
    'fecha_nacimiento',
    'edad',
    'estado_civil',

    -- Información laboral
    'ocupacion',
    'tipo_cliente',

    -- Ubicación
    'direccion',
    'distrito',
    'provincia',
    'departamento',

    -- Referencias
    'referencia_personal',

    -- Cuenta
    'numero_cuenta_linea_prestamo',

    -- Información de deuda
    'dias_mora',
    'monto_mora',
    'monto_capital'
);

-- 4. Poblar tabla_asociada para campos de METODOS_CONTACTO (informacion_contacto)
UPDATE definiciones_campos SET tabla_asociada = 'metodos_contacto' WHERE codigo_campo IN (
    'telefono_principal',
    'telefono_secundario',
    'telefono_trabajo',
    'telefono_celular',
    'telefono_domicilio',
    'telefono_laboral',
    'telefono_referencia_1',
    'telefono_referencia_2',
    'email',
    'correo_electronico'
);

-- 5. Insertar campos faltantes de la tabla clientes si no existen
INSERT IGNORE INTO definiciones_campos (codigo_campo, nombre_campo, descripcion, tipo_dato, tabla_asociada, fecha_creacion)
VALUES
    ('codigo_identificacion', 'Código de Identificación', 'Código único del cliente en el sistema origen', 'TEXTO', 'clientes', NOW()),
    ('documento', 'Documento', 'Número de documento de identidad (DNI, RUC, etc.)', 'TEXTO', 'clientes', NOW()),
    ('nombre_completo', 'Nombre Completo', 'Nombre completo del cliente', 'TEXTO', 'clientes', NOW()),
    ('primer_nombre', 'Primer Nombre', 'Primer nombre del cliente', 'TEXTO', 'clientes', NOW()),
    ('segundo_nombre', 'Segundo Nombre', 'Segundo nombre del cliente', 'TEXTO', 'clientes', NOW()),
    ('primer_apellido', 'Primer Apellido', 'Apellido paterno', 'TEXTO', 'clientes', NOW()),
    ('segundo_apellido', 'Segundo Apellido', 'Apellido materno', 'TEXTO', 'clientes', NOW()),
    ('fecha_nacimiento', 'Fecha de Nacimiento', 'Fecha de nacimiento del cliente', 'FECHA', 'clientes', NOW()),
    ('edad', 'Edad', 'Edad del cliente en años', 'NUMERICO', 'clientes', NOW()),
    ('estado_civil', 'Estado Civil', 'Estado civil del cliente', 'TEXTO', 'clientes', NOW()),
    ('ocupacion', 'Ocupación', 'Ocupación o profesión del cliente', 'TEXTO', 'clientes', NOW()),
    ('tipo_cliente', 'Tipo de Cliente', 'Clasificación del tipo de cliente', 'TEXTO', 'clientes', NOW()),
    ('direccion', 'Dirección', 'Dirección del domicilio', 'TEXTO', 'clientes', NOW()),
    ('distrito', 'Distrito', 'Distrito de residencia', 'TEXTO', 'clientes', NOW()),
    ('provincia', 'Provincia', 'Provincia de residencia', 'TEXTO', 'clientes', NOW()),
    ('departamento', 'Departamento', 'Departamento de residencia', 'TEXTO', 'clientes', NOW()),
    ('referencia_personal', 'Referencia Personal', 'Persona de referencia del cliente', 'TEXTO', 'clientes', NOW()),
    ('numero_cuenta_linea_prestamo', 'Número de Cuenta/Préstamo', 'Número de cuenta o línea de préstamo', 'TEXTO', 'clientes', NOW()),
    ('dias_mora', 'Días de Mora', 'Cantidad de días en mora', 'NUMERICO', 'clientes', NOW()),
    ('monto_mora', 'Monto en Mora', 'Monto total en mora', 'NUMERICO', 'clientes', NOW()),
    ('monto_capital', 'Monto Capital', 'Monto del capital adeudado', 'NUMERICO', 'clientes', NOW());

-- 6. Insertar campos de métodos de contacto si no existen
INSERT IGNORE INTO definiciones_campos (codigo_campo, nombre_campo, descripcion, tipo_dato, tabla_asociada, fecha_creacion)
VALUES
    ('telefono_principal', 'Teléfono Principal', 'Número de teléfono principal', 'TEXTO', 'metodos_contacto', NOW()),
    ('telefono_secundario', 'Teléfono Secundario', 'Número de teléfono secundario', 'TEXTO', 'metodos_contacto', NOW()),
    ('telefono_celular', 'Teléfono Celular', 'Número de celular', 'TEXTO', 'metodos_contacto', NOW()),
    ('telefono_domicilio', 'Teléfono Domicilio', 'Teléfono del domicilio', 'TEXTO', 'metodos_contacto', NOW()),
    ('telefono_laboral', 'Teléfono Laboral', 'Teléfono del trabajo', 'TEXTO', 'metodos_contacto', NOW()),
    ('telefono_referencia_1', 'Teléfono Referencia 1', 'Teléfono de primera referencia', 'TEXTO', 'metodos_contacto', NOW()),
    ('telefono_referencia_2', 'Teléfono Referencia 2', 'Teléfono de segunda referencia', 'TEXTO', 'metodos_contacto', NOW()),
    ('email', 'Email', 'Correo electrónico del cliente', 'TEXTO', 'metodos_contacto', NOW());

-- 7. Actualizar campos que no tienen tabla_asociada como 'otros' (campos adicionales/custom)
-- UPDATE definiciones_campos SET tabla_asociada = 'otros' WHERE tabla_asociada IS NULL;
