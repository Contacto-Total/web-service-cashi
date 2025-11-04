package com.cashi.shared.interfaces.rest.controllers;

import com.cashi.shared.application.services.GoogleDriveService;
import com.cashi.shared.interfaces.rest.resources.GoogleDriveFileResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/google-drive")
@Tag(name = "Google Drive", description = "Integración con Google Drive para gestión de archivos")
public class GoogleDriveController {

    private final GoogleDriveService googleDriveService;

    public GoogleDriveController(GoogleDriveService googleDriveService) {
        this.googleDriveService = googleDriveService;
    }

    @Operation(summary = "Check service status", description = "Verifica si el servicio de Google Drive está inicializado correctamente")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean isInitialized = googleDriveService.isInitialized();
        return ResponseEntity.ok(Map.of(
                "initialized", isInitialized,
                "message", isInitialized
                        ? "Google Drive service is ready"
                        : "Google Drive service not initialized. Check credentials.json file in src/main/resources/"
        ));
    }

    @Operation(summary = "List all files", description = "Lista todos los archivos disponibles en Google Drive")
    @GetMapping("/files")
    public ResponseEntity<?> listFiles() {
        try {
            List<GoogleDriveFileResource> files = googleDriveService.listFiles();
            return ResponseEntity.ok(files);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error listing files: " + e.getMessage()));
        }
    }

    @Operation(summary = "Search files", description = "Busca archivos por nombre en Google Drive")
    @GetMapping("/files/search")
    public ResponseEntity<?> searchFiles(
            @Parameter(description = "Query string to search") @RequestParam String query) {
        try {
            List<GoogleDriveFileResource> files = googleDriveService.searchFiles(query);
            return ResponseEntity.ok(files);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error searching files: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get file metadata", description = "Obtiene los metadatos de un archivo específico")
    @GetMapping("/files/{fileId}")
    public ResponseEntity<?> getFileMetadata(
            @Parameter(description = "Google Drive file ID") @PathVariable String fileId) {
        try {
            GoogleDriveFileResource file = googleDriveService.getFileMetadata(fileId);
            return ResponseEntity.ok(file);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error getting file metadata: " + e.getMessage()));
        }
    }

    @Operation(summary = "Download file", description = "Descarga un archivo desde Google Drive")
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<?> downloadFile(
            @Parameter(description = "Google Drive file ID") @PathVariable String fileId) {
        try {
            // Get file metadata first to get the filename
            GoogleDriveFileResource metadata = googleDriveService.getFileMetadata(fileId);
            byte[] fileContent = googleDriveService.downloadFile(fileId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", metadata.name());
            headers.setContentLength(fileContent.length);

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error downloading file: " + e.getMessage()));
        }
    }

    @Operation(summary = "Export Google Docs file", description = "Exporta un archivo de Google Docs a un formato específico")
    @GetMapping("/files/{fileId}/export")
    public ResponseEntity<?> exportFile(
            @Parameter(description = "Google Drive file ID") @PathVariable String fileId,
            @Parameter(description = "Target MIME type (e.g., application/pdf, application/vnd.openxmlformats-officedocument.wordprocessingml.document)")
            @RequestParam String mimeType) {
        try {
            GoogleDriveFileResource metadata = googleDriveService.getFileMetadata(fileId);
            byte[] fileContent = googleDriveService.exportFile(fileId, mimeType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));

            // Determine file extension based on MIME type
            String extension = getExtensionForMimeType(mimeType);
            String filename = metadata.name() + extension;

            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileContent.length);

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error exporting file: " + e.getMessage()));
        }
    }

    /**
     * Helper method to get file extension based on MIME type
     */
    private String getExtensionForMimeType(String mimeType) {
        return switch (mimeType) {
            case "application/pdf" -> ".pdf";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
            case "text/plain" -> ".txt";
            case "text/csv" -> ".csv";
            default -> "";
        };
    }
}
