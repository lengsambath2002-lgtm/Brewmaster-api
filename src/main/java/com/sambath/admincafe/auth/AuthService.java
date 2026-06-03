package com.sambath.admincafe.auth;

import com.sambath.admincafe.auth.dto.AuthUser;
import com.sambath.admincafe.auth.dto.LoginRequest;
import com.sambath.admincafe.auth.dto.LoginResponse;
import com.sambath.admincafe.common.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final AuthUser ADMIN =
            new AuthUser("u_1", "admin@brewmaster.com", "Alex Rivera", "Owner");
    private static final String ADMIN_PASSWORD = "brew1234";

    private final TokenStore tokenStore;

    public LoginResponse login(LoginRequest request) {
        if (!ADMIN.email().equalsIgnoreCase(request.email())
                || !ADMIN_PASSWORD.equals(request.password())) {
            throw new UnauthorizedException("Invalid email or password.");
        }
        String token = tokenStore.issue(ADMIN);
        return new LoginResponse(token, ADMIN);
    }

    public AuthUser requireUser(String token) {
        return tokenStore.resolve(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or missing token."));
    }

    public void logout(String token) {
        tokenStore.revoke(token);
    }
}
