package com.sambath.admincafe.auth;

import com.sambath.admincafe.auth.dto.AuthUser;
import com.sambath.admincafe.auth.dto.LoginRequest;
import com.sambath.admincafe.auth.dto.LoginResponse;
import com.sambath.admincafe.common.UnauthorizedException;
import com.sambath.admincafe.user.User;
import com.sambath.admincafe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenStore tokenStore;
    private final UserRepository userRepository;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));
        if (!user.getPasswordHash().equals(request.password())) {
            throw new UnauthorizedException("Invalid email or password.");
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
