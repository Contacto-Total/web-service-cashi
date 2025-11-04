-- V7: Add file hash for duplicate detection based on content

ALTER TABLE historial_importaciones
ADD COLUMN file_hash VARCHAR(32) COMMENT 'MD5 hash of file content for duplicate detection';

-- Add index for fast hash lookup (this is critical for performance)
CREATE INDEX idx_historial_file_hash ON historial_importaciones(file_hash, estado);

-- Note: MD5 generates a 32-character hexadecimal string
-- This allows detection of duplicate files regardless of filename
