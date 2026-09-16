package com.etra.cti.auth;

import com.etra.cti.auth.dto.*;
import com.etra.cti.user.User;
import com.etra.cti.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final UserRepository users;

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

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal JwtAuthFilter.AuthedUser principal) {
        User u = users.findById(principal.id()).orElseThrow();
        return UserDto.from(u);
    }

    public record RefreshRequest(String refreshToken) {}
}
