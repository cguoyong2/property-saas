package com.yongquan.propertysaas.security.scope;

import com.yongquan.propertysaas.tenant.context.TenantContext;
import org.springframework.stereotype.Component;

@Component
public class ProjectDataScope {

    public Long currentProjectId() {
        return TenantContext.getProjectId();
    }

    public boolean hasProjectContext() {
        return TenantContext.getProjectId() != null;
    }
}
