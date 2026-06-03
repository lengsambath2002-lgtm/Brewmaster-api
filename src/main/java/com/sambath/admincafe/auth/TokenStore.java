package com.sambath.admincafe.auth;

import com.sambath.admincafe.auth.dto.AuthUser;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private final ConcurrentHashMap<String, AuthUser> tokens = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(AuthUser user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, user);
        return token;
    }

    public Optional<AuthUser> resolve(String token) {
        if (token == null) return Optional.empty();
        return Optional.ofNullable(tokens.get(token));
    }

    public void revoke(String token) {
        if (token != null) tokens.remove(token);
    }
}
