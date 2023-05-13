package com.middleware.zeus.common.model.registry;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
public class Permission {
    private String name;
    private String includesPattern;
    private String excludesPattern;
    private List<String> repositories;
    private PermissionPrincipal principals;
}
