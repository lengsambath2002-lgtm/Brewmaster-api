package com.sambath.admincafe.auth;

import com.sambath.admincafe.auth.dto.AuthUser;
import com.sambath.admincafe.auth.dto.LoginRequest;
import com.sambath.admincafe.auth.dto.LoginResponse;
import com.sambath.admincafe.auth.dto.MeResponse;
import com.sambath.admincafe.common.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public MeResponse me(HttpServletRequest request) {
        AuthUser user = (AuthUser) request.getAttribute(AuthInterceptor.USER_ATTR);
        if (user == null) {
            throw new UnauthorizedException("Invalid or missing token.");
        }
        return new MeResponse(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = (String) request.getAttribute(AuthInterceptor.TOKEN_ATTR);
        if (token != null) {
            authService.logout(token);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
