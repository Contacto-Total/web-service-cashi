package com.cashi.shared.application.services;

import com.cashi.shared.interfaces.rest.resources.GoogleDriveFileResource;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleDriveService {

    private static final String APPLICATION_NAME = "Cashi Drive Integration";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";

    private Drive driveService;

    public GoogleDriveService() {
        try {
            this.driveService = getDriveService();
        } catch (Exception e) {
            System.err.println("Error initializing Google Drive service: " + e.getMessage());
            System.err.println("Note: Make sure to place 'credentials.json' in src/main/resources/");
        }
    }

    /**
     * Creates an authorized Drive API client service using Service Account.
     */
    private Drive getDriveService() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Load credentials from resources folder
        GoogleCredentials credentials;
        try (InputStream in = new ClassPathResource(CREDENTIALS_FILE_PATH).getInputStream()) {
            credentials = GoogleCredentials.fromStream(in)
                    .createScoped(SCOPES);
        }

        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Lists all files from Google Drive
     */
    public List<GoogleDriveFileResource> listFiles() throws IOException {
        if (driveService == null) {
            throw new IllegalStateException("Google Drive service not initialized. Check credentials.json file.");
        }

        List<GoogleDriveFileResource> files = new ArrayList<>();
        String pageToken = null;

        do {
            FileList result = driveService.files().list()
                    .setPageSize(100)
                    .setFields("nextPageToken, files(id, name, mimeType, size, modifiedTime, webViewLink, iconLink, thumbnailLink, fileExtension)")
                    .setPageToken(pageToken)
                    .setOrderBy("modifiedTime desc")
                    .execute();

            for (File file : result.getFiles()) {
                files.add(mapToResource(file));
            }

            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return files;
    }

    /**
     * Search files by name
     */
    public List<GoogleDriveFileResource> searchFiles(String query) throws IOException {
        if (driveService == null) {
            throw new IllegalStateException("Google Drive service not initialized. Check credentials.json file.");
        }

        FileList result = driveService.files().list()
                .setQ("name contains '" + query + "'")
                .setPageSize(100)
                .setFields("files(id, name, mimeType, size, modifiedTime, webViewLink, iconLink, thumbnailLink, fileExtension)")
                .setOrderBy("modifiedTime desc")
                .execute();

        List<GoogleDriveFileResource> files = new ArrayList<>();
        for (File file : result.getFiles()) {
            files.add(mapToResource(file));
        }

        return files;
    }

    /**
     * Get file metadata by ID
     */
    public GoogleDriveFileResource getFileMetadata(String fileId) throws IOException {
        if (driveService == null) {
            throw new IllegalStateException("Google Drive service not initialized. Check credentials.json file.");
        }

        File file = driveService.files().get(fileId)
                .setFields("id, name, mimeType, size, modifiedTime, webViewLink, iconLink, thumbnailLink, fileExtension")
                .execute();

        return mapToResource(file);
    }

    /**
     * Download file content as byte array
     */
    public byte[] downloadFile(String fileId) throws IOException {
        if (driveService == null) {
            throw new IllegalStateException("Google Drive service not initialized. Check credentials.json file.");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream);

        return outputStream.toByteArray();
    }

    /**
     * Export Google Docs file to specific format (e.g., PDF, DOCX)
     */
    public byte[] exportFile(String fileId, String mimeType) throws IOException {
        if (driveService == null) {
            throw new IllegalStateException("Google Drive service not initialized. Check credentials.json file.");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().export(fileId, mimeType)
                .executeMediaAndDownloadTo(outputStream);

        return outputStream.toByteArray();
    }

    /**
     * Maps Google Drive File to Resource
     */
    private GoogleDriveFileResource mapToResource(File file) {
        return new GoogleDriveFileResource(
                file.getId(),
                file.getName(),
                file.getMimeType(),
                file.getSize(),
                formatFileSize(file.getSize()),
                file.getModifiedTime() != null ? file.getModifiedTime().toString() : null,
                file.getWebViewLink(),
                file.getIconLink(),
                file.getThumbnailLink(),
                file.getFileExtension(),
                getFileType(file.getMimeType())
        );
    }

    /**
     * Format file size to human-readable format
     */
    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";

        int unit = 1024;
        if (bytes < unit) return bytes + " B";

        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Get file type category based on MIME type
     */
    private String getFileType(String mimeType) {
        if (mimeType == null) return "unknown";

        if (mimeType.contains("folder")) return "folder";
        if (mimeType.contains("document")) return "document";
        if (mimeType.contains("spreadsheet")) return "spreadsheet";
        if (mimeType.contains("presentation")) return "presentation";
        if (mimeType.contains("pdf")) return "pdf";
        if (mimeType.contains("image")) return "image";
        if (mimeType.contains("video")) return "video";
        if (mimeType.contains("audio")) return "audio";
        if (mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("compressed")) return "archive";

        return "file";
    }

    /**
     * Check if service is initialized
     */
    public boolean isInitialized() {
        return driveService != null;
    }
}
