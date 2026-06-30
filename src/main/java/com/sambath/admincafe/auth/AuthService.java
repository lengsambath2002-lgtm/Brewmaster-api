package com.sambath.admincafe.auth;

import com.sambath.admincafe.auth.dto.AuthUser;
import com.sambath.admincafe.auth.dto.LoginRequest;
import com.sambath.admincafe.auth.dto.LoginResponse;
import com.sambath.admincafe.common.UnauthorizedException;
import com.sambath.admincafe.tenant.Tenant;
import com.sambath.admincafe.tenant.TenantRepository;
import com.sambath.admincafe.user.User;
import com.sambath.admincafe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ROLE_SUPER_ADMIN = "SuperAdmin";

    private final TokenStore tokenStore;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }
        if (!ROLE_SUPER_ADMIN.equals(user.getRole())) {
            Tenant tenant = tenantRepository.findById(user.getTenantId())
                    .orElseThrow(() -> new UnauthorizedException("Tenant not found."));
            if (!tenant.isActive()) {
                throw new UnauthorizedException("Tenant is suspended.");
            }
        }
        AuthUser authUser = toAuthUser(user);
        String token = tokenStore.issue(authUser);
        return new LoginResponse(token, authUser);
    }

    public AuthUser requireUser(String token) {
        return tokenStore.resolve(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or missing token."));
    }

    public void logout(String token) {
        tokenStore.revoke(token);
    }

    private static AuthUser toAuthUser(User user) {
        return new AuthUser(
                "u_" + user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }
}
