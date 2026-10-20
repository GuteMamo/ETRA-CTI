package com.etra.cti.auth;

import com.etra.cti.auth.dto.*;
import com.etra.cti.user.User;
import com.etra.cti.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final UserRepository users;

    public AuthController(AuthService auth, UserRepository users) {
        this.auth = auth;
        this.users = users;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public TokenPair login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/refresh")
    public TokenPair refresh(@RequestBody RefreshRequest req) {
        return auth.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) LogoutRequest req,
                       @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        String token = req != null ? req.refreshToken() : null;
        UUID userId = me != null ? me.id() : null;
        auth.logout(userId, token);
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal JwtAuthFilter.AuthedUser principal) {
        User u = users.findById(principal.id()).orElseThrow();
        return UserDto.from(u);
    }

    public record RefreshRequest(String refreshToken) {}
    public record LogoutRequest(String refreshToken) {}
}
