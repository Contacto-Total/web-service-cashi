package com.cashi.customermanagement.interfaces.rest.resources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FolderResource {
    private String name;
    private String path;
    private boolean isDirectory;
    private boolean hasSubfolders;
}
