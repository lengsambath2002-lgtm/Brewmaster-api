package com.sambath.admincafe.superadmin;

import com.sambath.admincafe.common.ConflictException;
import com.sambath.admincafe.common.NotFoundException;
import com.sambath.admincafe.superadmin.SuperAdminRepository.TenantCounts;
import com.sambath.admincafe.superadmin.SuperAdminRepository.TenantRevenue;
import com.sambath.admincafe.superadmin.dto.CreateTenantRequest;
import com.sambath.admincafe.superadmin.dto.CreateUserRequest;
import com.sambath.admincafe.superadmin.dto.KhqrSettingsDto;
import com.sambath.admincafe.superadmin.dto.OwnerView;
import com.sambath.admincafe.superadmin.dto.PlatformStats;
import com.sambath.admincafe.superadmin.dto.ResetPasswordRequest;
import com.sambath.admincafe.superadmin.dto.TenantBreakdown;
import com.sambath.admincafe.superadmin.dto.TenantDetail;
import com.sambath.admincafe.superadmin.dto.TenantSummary;
import com.sambath.admincafe.superadmin.dto.UpdateTenantRequest;
import com.sambath.admincafe.tenant.Tenant;
import com.sambath.admincafe.tenant.TenantBootstrap;
import com.sambath.admincafe.tenant.TenantKhqrSettings;
import com.sambath.admincafe.tenant.TenantRepository;
import com.sambath.admincafe.user.User;
import com.sambath.admincafe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private static final Pattern SLUG_ALLOWED = Pattern.compile("^[a-z0-9-]+$");
    private static final String RESERVED_DEFAULT = "default";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SuperAdminRepository aggregates;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<TenantSummary> listTenants() {
        List<Tenant> tenants = tenantRepository.findBySlugNotOrderByCreatedAtDesc(TenantBootstrap.PLATFORM_SLUG);
        if (tenants.isEmpty()) return List.of();
        List<Long> ids = tenants.stream().map(Tenant::getId).toList();
        Map<Long, TenantCounts> counts = aggregates.countsByTenant(ids);
        return tenants.stream().map(t -> {
            TenantCounts c = counts.getOrDefault(t.getId(), new TenantCounts(0, 0, 0));
            return new TenantSummary(
                    t.getId(), t.getSlug(), t.getName(), t.isActive(), t.getCreatedAt(),
                    c.users(), c.products(), c.orders());
        }).toList();
    }

    @Transactional(readOnly = true)
    public TenantDetail getTenant(Long id) {
        Tenant t = requireTenant(id);
        List<OwnerView> owners = userRepository.findByTenantIdOrderByEmail(t.getId()).stream()
                .map(SuperAdminService::toOwnerView)
                .toList();
        return new TenantDetail(
                t.getId(), t.getSlug(), t.getName(), t.isActive(), t.getCreatedAt(),
                toKhqrDto(t.getKhqr()), owners);
    }

    @Transactional
    public TenantDetail createTenant(CreateTenantRequest req) {
        String slug = normalizeSlug(req.slug());
        if (tenantRepository.existsBySlug(slug)) {
            throw new ConflictException("Tenant slug already exists: " + slug);
        }
        if (userRepository.existsByEmailIgnoreCase(req.ownerEmail())) {
            throw new ConflictException("Email already in use: " + req.ownerEmail());
        }
        Tenant tenant = new Tenant();
        tenant.setSlug(slug);
        tenant.setName(req.name().trim());
        tenant.setActive(true);
        tenant.setKhqr(toKhqrEntity(req.khqr()));
        Tenant saved = tenantRepository.save(tenant);

        User owner = new User();
        owner.setTenantId(saved.getId());
        owner.setEmail(req.ownerEmail().trim());
        owner.setName(req.ownerName().trim());
        owner.setRole("Owner");
        owner.setPasswordHash(passwordEncoder.encode(req.ownerPassword()));
        userRepository.save(owner);

        return new TenantDetail(
                saved.getId(), saved.getSlug(), saved.getName(), saved.isActive(),
                saved.getCreatedAt(), toKhqrDto(saved.getKhqr()),
                List.of(toOwnerView(owner)));
    }

    @Transactional
    public TenantDetail updateTenant(Long id, UpdateTenantRequest req) {
        Tenant t = requireTenant(id);
        t.setName(req.name().trim());
        t.setKhqr(toKhqrEntity(req.khqr()));
        tenantRepository.save(t);
        return getTenant(id);
    }

    @Transactional
    public TenantSummary setActive(Long id, boolean active) {
        Tenant t = requireTenant(id);
        t.setActive(active);
        tenantRepository.save(t);
        TenantCounts c = aggregates.countsByTenant(List.of(t.getId()))
                .getOrDefault(t.getId(), new TenantCounts(0, 0, 0));
        return new TenantSummary(
                t.getId(), t.getSlug(), t.getName(), t.isActive(), t.getCreatedAt(),
                c.users(), c.products(), c.orders());
    }

    @Transactional(readOnly = true)
    public List<OwnerView> listUsers(Long tenantId) {
        requireTenant(tenantId);
        return userRepository.findByTenantIdOrderByEmail(tenantId).stream()
                .map(SuperAdminService::toOwnerView)
                .toList();
    }

    @Transactional
    public OwnerView addUser(Long tenantId, CreateUserRequest req) {
        Tenant tenant = requireTenant(tenantId);
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("Email already in use: " + req.email());
        }
        User u = new User();
        u.setTenantId(tenant.getId());
        u.setEmail(req.email().trim());
        u.setName(req.name().trim());
        String role = req.role() == null || req.role().isBlank() ? "Owner" : req.role().trim();
        u.setRole(role);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        userRepository.save(u);
        return toOwnerView(u);
    }

    @Transactional
    public void resetPassword(Long tenantId, Long userId, ResetPasswordRequest req) {
        Tenant tenant = requireTenant(tenantId);
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (!tenant.getId().equals(u.getTenantId())) {
            throw new NotFoundException("User not found in tenant.");
        }
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        userRepository.save(u);
    }

    @Transactional(readOnly = true)
    public PlatformStats stats() {
        List<Tenant> tenants = tenantRepository.findBySlugNotOrderByCreatedAtDesc(TenantBootstrap.PLATFORM_SLUG);
        long total = tenants.size();
        long active = tenants.stream().filter(Tenant::isActive).count();
        long totalOrders = aggregates.countAllOrders(TenantBootstrap.PLATFORM_SLUG);
        BigDecimal totalRevenue = aggregates.sumAllRevenue(TenantBootstrap.PLATFORM_SLUG);

        List<Long> ids = tenants.stream().map(Tenant::getId).toList();
        Map<Long, TenantRevenue> rev = aggregates.revenueByTenant(ids);
        List<TenantBreakdown> perTenant = tenants.stream()
                .map(t -> {
                    TenantRevenue r = rev.getOrDefault(t.getId(), new TenantRevenue(0, BigDecimal.ZERO));
                    return new TenantBreakdown(t.getId(), t.getSlug(), t.getName(), t.isActive(),
                            r.orders(), r.revenue());
                })
                .sorted((a, b) -> b.revenue().compareTo(a.revenue()))
                .toList();

        return new PlatformStats(total, active, totalOrders, totalRevenue, perTenant);
    }

    private Tenant requireTenant(Long id) {
        Tenant t = tenantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));
        if (TenantBootstrap.PLATFORM_SLUG.equals(t.getSlug())) {
            throw new NotFoundException("Tenant not found: " + id);
        }
        return t;
    }

    private static String normalizeSlug(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Slug is required.");
        }
        String slug = raw.trim().toLowerCase();
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("Slug must not be empty.");
        }
        if (!SLUG_ALLOWED.matcher(slug).matches()) {
            throw new IllegalArgumentException("Slug may only contain lowercase letters, digits, and hyphens.");
        }
        if (RESERVED_DEFAULT.equals(slug) || TenantBootstrap.PLATFORM_SLUG.equals(slug)) {
            throw new IllegalArgumentException("Slug is reserved: " + slug);
        }
        return slug;
    }

    private static OwnerView toOwnerView(User u) {
        return new OwnerView(u.getId(), u.getEmail(), u.getName(), u.getRole());
    }

    private static KhqrSettingsDto toKhqrDto(TenantKhqrSettings k) {
        if (k == null) {
            return new KhqrSettingsDto(null, null, null, null, null, null, null, null, null, null);
        }
        return new KhqrSettingsDto(
                k.getBakongAccountId(), k.getMerchantName(), k.getMerchantCity(),
                k.getAcquiringBank(), k.getMerchantId(), k.getMerchantCategoryCode(),
                k.getCurrency(), k.getStoreLabel(), k.getTerminalLabel(), k.getMobileNumber());
    }

    private static TenantKhqrSettings toKhqrEntity(KhqrSettingsDto dto) {
        TenantKhqrSettings k = new TenantKhqrSettings();
        if (dto == null) return k;
        k.setBakongAccountId(dto.bakongAccountId());
        k.setMerchantName(dto.merchantName());
        k.setMerchantCity(dto.merchantCity());
        k.setAcquiringBank(dto.acquiringBank());
        k.setMerchantId(dto.merchantId());
        k.setMerchantCategoryCode(dto.merchantCategoryCode());
        k.setCurrency(dto.currency());
        k.setStoreLabel(dto.storeLabel());
        k.setTerminalLabel(dto.terminalLabel());
        k.setMobileNumber(dto.mobileNumber());
        return k;
    }
}
