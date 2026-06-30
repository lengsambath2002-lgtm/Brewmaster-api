package com.sambath.admincafe.tenant;

import com.sambath.admincafe.auth.AuthService;
import com.sambath.admincafe.auth.PasswordEncoderConfig;
import com.sambath.admincafe.khqr.KhqrProperties;
import com.sambath.admincafe.user.User;
import com.sambath.admincafe.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class TenantBootstrap {

    public static final String PLATFORM_SLUG = "__platform__";

    @Bean
    ApplicationRunner tenantBootstrapRunner(TenantBootstrapService service) {
        return args -> service.bootstrap();
    }

    // Lives in a separate Spring-managed bean so the @Transactional advice is
    // applied — calling a @Transactional method on `this` from inside the same
    // @Configuration class bypasses the proxy and the transaction never starts.
    @Service
    @RequiredArgsConstructor
    @Slf4j
    static class TenantBootstrapService {

        static final String DEFAULT_SLUG = "default";
        private static final String DEFAULT_ADMIN_EMAIL = "admin@brewmaster.com";
        private static final String DEFAULT_ADMIN_PASSWORD = "brew1234";
        private static final String DEFAULT_ADMIN_NAME = "Alex Rivera";

        private static final String SUPER_ADMIN_EMAIL = "super@brewmaster.com";
        private static final String SUPER_ADMIN_PASSWORD = "super1234";
        private static final String SUPER_ADMIN_NAME = "Platform Super Admin";

        private final TenantRepository tenantRepository;
        private final UserRepository userRepository;
        private final KhqrProperties khqrProperties;
        private final PasswordEncoder passwordEncoder;

        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void bootstrap() {
            // Seed the default tenant first so its id stays stable (= 1 on a fresh DB).
            // On existing prod databases the platform tenant is added later but its id
            // doesn't matter — it's looked up by slug.
            Tenant tenant = tenantRepository.findBySlug(DEFAULT_SLUG).orElseGet(() -> {
                Tenant t = new Tenant();
                t.setSlug(DEFAULT_SLUG);
                t.setName("Admin Cafe");
                t.setActive(true);
                t.setKhqr(seedKhqrFromProperties());
                Tenant saved = tenantRepository.save(t);
                log.info("Seeded default tenant '{}' (id={})", DEFAULT_SLUG, saved.getId());
                return saved;
            });

            seedUser(tenant.getId(), DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_NAME,
                    "Owner", DEFAULT_ADMIN_PASSWORD);

            Tenant platform = tenantRepository.findBySlug(PLATFORM_SLUG).orElseGet(() -> {
                Tenant t = new Tenant();
                t.setSlug(PLATFORM_SLUG);
                t.setName("Platform");
                t.setActive(true);
                Tenant saved = tenantRepository.save(t);
                log.info("Seeded platform tenant '{}' (id={})", PLATFORM_SLUG, saved.getId());
                return saved;
            });

            seedUser(platform.getId(), SUPER_ADMIN_EMAIL, SUPER_ADMIN_NAME,
                    AuthService.ROLE_SUPER_ADMIN, SUPER_ADMIN_PASSWORD);

            backfillTenantIds(tenant.getId());
            ensureOrdersUniqueConstraint();
        }

        // Create the user if missing; if it exists with a plaintext password,
        // migrate it to a BCrypt hash. Existing hashed passwords are left alone
        // so this method is safe to run on every restart.
        private void seedUser(Long tenantId, String email, String name, String role, String rawPassword) {
            User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (user == null) {
                User u = new User();
                u.setTenantId(tenantId);
                u.setEmail(email);
                u.setName(name);
                u.setRole(role);
                u.setPasswordHash(passwordEncoder.encode(rawPassword));
                userRepository.save(u);
                log.info("Seeded user '{}' (role={}) for tenant id={}", email, role, tenantId);
                return;
            }
            if (!PasswordEncoderConfig.looksHashed(user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
                log.info("Migrated user '{}' password to BCrypt", email);
            }
        }

        // ddl-auto=update is supposed to add tenant_id as nullable, but on managed
        // Postgres (Supabase/Render) it sometimes silently skips the ALTER when the
        // table predates the multi-tenant migration. Add the column defensively,
        // then backfill legacy rows with the default tenant.
        private void backfillTenantIds(Long defaultTenantId) {
            for (String table : new String[]{"products", "categories", "orders", "transactions"}) {
                try {
                    em.createNativeQuery(
                                    "ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS tenant_id BIGINT")
                            .executeUpdate();
                } catch (Exception e) {
                    log.warn("Could not ensure tenant_id column on {}: {}", table, e.getMessage());
                    continue;
                }
                try {
                    int updated = em.createNativeQuery(
                                    "UPDATE " + table + " SET tenant_id = :id WHERE tenant_id IS NULL")
                            .setParameter("id", defaultTenantId)
                            .executeUpdate();
                    if (updated > 0) {
                        log.info("Backfilled {} {} rows to tenant_id={}", updated, table, defaultTenantId);
                    }
                } catch (Exception e) {
                    log.warn("Could not backfill tenant_id on {}: {}", table, e.getMessage());
                }
            }
        }

        // ddl-auto=update adds the new unique constraint but cannot drop the
        // legacy global one (uk_orders_date_daily_number). Without dropping it,
        // two tenants sharing the same (order_date, daily_number) would still
        // collide on insert.
        private void ensureOrdersUniqueConstraint() {
            try {
                em.createNativeQuery(
                                "ALTER TABLE orders DROP CONSTRAINT IF EXISTS uk_orders_date_daily_number")
                        .executeUpdate();
            } catch (Exception e) {
                log.warn("Could not drop legacy orders constraint: {}", e.getMessage());
            }
        }

        private TenantKhqrSettings seedKhqrFromProperties() {
            TenantKhqrSettings s = new TenantKhqrSettings();
            s.setBakongAccountId(khqrProperties.getBakongAccountId());
            s.setMerchantName(khqrProperties.getMerchantName());
            s.setMerchantCity(khqrProperties.getMerchantCity());
            s.setAcquiringBank(khqrProperties.getAcquiringBank());
            s.setMerchantId(khqrProperties.getMerchantId());
            s.setMerchantCategoryCode(khqrProperties.getMerchantCategoryCode());
            s.setCurrency(khqrProperties.getCurrency());
            s.setStoreLabel(khqrProperties.getStoreLabel());
            s.setTerminalLabel(khqrProperties.getTerminalLabel());
            s.setMobileNumber(khqrProperties.getMobileNumber());
            return s;
        }
    }
}
