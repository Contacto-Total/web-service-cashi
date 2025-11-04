package com.cashi.customermanagement.interfaces.rest.controllers;

import com.cashi.customermanagement.interfaces.rest.resources.FolderResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/file-system")
@Tag(name = "File System", description = "Navegación del sistema de archivos del servidor")
@CrossOrigin(origins = "*")
public class FileSystemController {

    @Operation(summary = "Listar unidades de disco disponibles",
               description = "Retorna todas las unidades de disco del servidor (C:, D:, G:, etc.)")
    @GetMapping("/drives")
    public ResponseEntity<List<FolderResource>> listDrives() {
        try {
            File[] roots = File.listRoots();
            List<FolderResource> drives = Arrays.stream(roots)
                    .filter(File::exists)
                    .map(root -> {
                        String path = root.getAbsolutePath();
                        // Verificar si tiene subcarpetas
                        boolean hasSubfolders = hasAccessibleSubfolders(root);

                        return new FolderResource(
                                path,  // name
                                path,  // path
                                true,  // isDirectory
                                hasSubfolders
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(drives);
        } catch (Exception e) {
            System.err.println("Error listando unidades: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @Operation(summary = "Listar carpetas en una ruta",
               description = "Retorna todas las carpetas dentro de una ruta específica")
    @GetMapping("/browse")
    public ResponseEntity<?> browseFolders(
            @Parameter(description = "Ruta a explorar", example = "C:\\Users")
            @RequestParam String path) {

        try {
            // Validar que la ruta no esté vacía
            if (path == null || path.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La ruta no puede estar vacía"));
            }

            File directory = new File(path);

            // Validar que existe
            if (!directory.exists()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La ruta no existe: " + path));
            }

            // Validar que es un directorio
            if (!directory.isDirectory()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La ruta no es un directorio: " + path));
            }

            // Listar carpetas (solo carpetas, no archivos)
            File[] files = directory.listFiles();

            if (files == null) {
                // Sin permisos o error de lectura
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<FolderResource> folders = Arrays.stream(files)
                    .filter(File::isDirectory)
                    .filter(f -> !f.isHidden()) // Filtrar carpetas ocultas
                    .sorted(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
                    .map(folder -> {
                        String folderPath = folder.getAbsolutePath();
                        boolean hasSubfolders = hasAccessibleSubfolders(folder);

                        return new FolderResource(
                                folder.getName(),
                                folderPath,
                                true,
                                hasSubfolders
                        );
                    })
                    .collect(Collectors.toList());

            System.out.println("📁 Listando carpetas en: " + path + " - Encontradas: " + folders.size());
            return ResponseEntity.ok(folders);

        } catch (SecurityException e) {
            System.err.println("❌ Error de permisos al listar: " + path);
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            System.err.println("❌ Error explorando carpeta: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Error al explorar la ruta: " + e.getMessage()));
        }
    }

    @Operation(summary = "Validar si una ruta existe y es accesible",
               description = "Verifica si una ruta existe en el servidor y si es un directorio válido")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validatePath(
            @Parameter(description = "Ruta a validar") @RequestParam String path) {

        try {
            File directory = new File(path);

            Map<String, Object> result = new HashMap<>();
            result.put("exists", directory.exists());
            result.put("isDirectory", directory.isDirectory());
            result.put("canRead", directory.canRead());
            result.put("canWrite", directory.canWrite());
            result.put("path", path);

            if (directory.exists() && directory.isDirectory()) {
                result.put("valid", true);
                result.put("message", "La ruta es válida y accesible");
            } else if (!directory.exists()) {
                result.put("valid", false);
                result.put("message", "La ruta no existe");
            } else {
                result.put("valid", false);
                result.put("message", "La ruta no es un directorio");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("valid", false);
            error.put("exists", false);
            error.put("message", "Error al validar: " + e.getMessage());
            return ResponseEntity.ok(error);
        }
    }

    /**
     * Verifica si un directorio tiene subcarpetas accesibles
     */
    private boolean hasAccessibleSubfolders(File directory) {
        try {
            File[] subFiles = directory.listFiles();
            if (subFiles == null) {
                return false;
            }

            // Verificar si tiene al menos una subcarpeta no oculta
            return Arrays.stream(subFiles)
                    .anyMatch(f -> f.isDirectory() && !f.isHidden());
        } catch (Exception e) {
            return false;
        }
    }
}
