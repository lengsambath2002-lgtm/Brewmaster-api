package com.sambath.admincafe.superadmin;

import com.sambath.admincafe.superadmin.dto.CreateTenantRequest;
import com.sambath.admincafe.superadmin.dto.CreateUserRequest;
import com.sambath.admincafe.superadmin.dto.OwnerView;
import com.sambath.admincafe.superadmin.dto.PlatformStats;
import com.sambath.admincafe.superadmin.dto.ResetPasswordRequest;
import com.sambath.admincafe.superadmin.dto.TenantDetail;
import com.sambath.admincafe.superadmin.dto.TenantSummary;
import com.sambath.admincafe.superadmin.dto.UpdateTenantRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService service;

    @GetMapping("/tenants")
    public List<TenantSummary> listTenants() {
        return service.listTenants();
    }

    @PostMapping("/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantDetail createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return service.createTenant(request);
    }

    @GetMapping("/tenants/{id}")
    public TenantDetail getTenant(@PathVariable Long id) {
        return service.getTenant(id);
    }

    @PatchMapping("/tenants/{id}")
    public TenantDetail updateTenant(@PathVariable Long id,
                                     @Valid @RequestBody UpdateTenantRequest request) {
        return service.updateTenant(id, request);
    }

    @PostMapping("/tenants/{id}/suspend")
    public TenantSummary suspendTenant(@PathVariable Long id) {
        return service.setActive(id, false);
    }

    @PostMapping("/tenants/{id}/activate")
    public TenantSummary activateTenant(@PathVariable Long id) {
        return service.setActive(id, true);
    }

    @GetMapping("/tenants/{id}/users")
    public List<OwnerView> listUsers(@PathVariable Long id) {
        return service.listUsers(id);
    }

    @PostMapping("/tenants/{id}/users")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerView addUser(@PathVariable Long id,
                             @Valid @RequestBody CreateUserRequest request) {
        return service.addUser(id, request);
    }

    @PostMapping("/tenants/{id}/users/{userId}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id,
                              @PathVariable Long userId,
                              @Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(id, userId, request);
    }

    @GetMapping("/stats")
    public PlatformStats stats() {
        return service.stats();
    }
}
