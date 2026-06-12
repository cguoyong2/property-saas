package com.yongquan.propertysaas.system.domain;

public record MenuView(
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
        Boolean visible,
        String status
) {
}
