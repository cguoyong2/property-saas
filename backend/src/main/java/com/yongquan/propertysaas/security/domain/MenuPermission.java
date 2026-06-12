package com.yongquan.propertysaas.security.domain;

public record MenuPermission(
        Long menuId,
        Long parentId,
        String menuName,
        String menuType,
        String permissionCode,
        String routePath,
        String apiPath,
        String component,
        String moduleCode,
        Integer sortNo,
        Boolean visible
) {
}
