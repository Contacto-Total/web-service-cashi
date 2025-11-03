package com.cashi.shared.interfaces.rest.resources;

public record GoogleDriveFileResource(
        String id,
        String name,
        String mimeType,
        Long size,
        String sizeFormatted,
        String modifiedTime,
        String webViewLink,
        String iconLink,
        String thumbnailLink,
        String fileExtension,
        String fileType
) {
}
