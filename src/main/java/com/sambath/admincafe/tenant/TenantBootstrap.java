package com.sambath.admincafe.tenant;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class TenantBootstrap {

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

        private final TenantRepository tenantRepository;
        private final UserRepository userRepository;
        private final KhqrProperties khqrProperties;

        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void bootstrap() {
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

            userRepository.findByEmailIgnoreCase(DEFAULT_ADMIN_EMAIL).orElseGet(() -> {
                User u = new User();
                u.setTenantId(tenant.getId());
                u.setEmail(DEFAULT_ADMIN_EMAIL);
                u.setName(DEFAULT_ADMIN_NAME);
                u.setRole("Owner");
                u.setPasswordHash(DEFAULT_ADMIN_PASSWORD);
                User saved = userRepository.save(u);
                log.info("Seeded default admin user '{}' for tenant '{}'", DEFAULT_ADMIN_EMAIL, tenant.getSlug());
                return saved;
            });

            backfillTenantIds(tenant.getId());
            ensureOrdersUniqueConstraint();
        }

        // Hibernate ddl-auto=update adds tenant_id columns as nullable. Existing
        // rows (legacy single-tenant data) need to be attributed to the default
        // tenant before @TenantId filtering hides them.
        private void backfillTenantIds(Long defaultTenantId) {
            for (String table : new String[]{"products", "categories", "orders", "transactions"}) {
                int updated = em.createNativeQuery(
                                "UPDATE " + table + " SET tenant_id = :id WHERE tenant_id IS NULL")
                        .setParameter("id", defaultTenantId)
                        .executeUpdate();
                if (updated > 0) {
                    log.info("Backfilled {} {} rows to tenant_id={}", updated, table, defaultTenantId);
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
