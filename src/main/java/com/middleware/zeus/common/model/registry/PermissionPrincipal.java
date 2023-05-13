package com.middleware.zeus.common.model.registry;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
public class PermissionPrincipal {
    private Map<String, List<String>> users;
    private Map<String, List<String>> groups;
}
