package com.sambath.admincafe.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Object> {

    @Override
    public Object resolveCurrentTenantIdentifier() {
        Long id = TenantContext.get();
        return id != null ? id : TenantContext.SYSTEM;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public boolean isRoot(Object tenantId) {
        return TenantContext.SYSTEM.equals(tenantId);
    }
}
