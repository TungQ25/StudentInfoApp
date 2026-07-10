package com.example.taskmanager.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import com.example.taskmanager.dto.AuthResponse;
import com.example.taskmanager.dto.LoginRequest;
import com.example.taskmanager.dto.LogoutRequest;
import com.example.taskmanager.dto.RefreshTokenRequest;
import com.example.taskmanager.dto.RegisterRequest;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.entity.UserSession;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.repository.UserSessionRepository;
import com.example.taskmanager.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtService jwtService;
    private final long refreshExpirationMs;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            JwtService jwtService,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.jwtService = jwtService;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        long now = System.currentTimeMillis();
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return createSessionResponse(userRepository.save(user), request.deviceId());
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        String normalizedEmail = identifier.toLowerCase(Locale.ROOT);

        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password");
        }

        return createSessionResponse(user, request.deviceId());
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        UserSession session = userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(hashToken(request.refreshToken()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        long now = System.currentTimeMillis();
        if (session.getExpiresAt() <= now) {
            session.setRevokedAt(now);
            session.setUpdatedAt(now);
            userSessionRepository.save(session);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        String nextRefreshToken = newRefreshToken();
        session.setDeviceId(normalizeDeviceId(request.deviceId(), session.getDeviceId()));
        session.setRefreshTokenHash(hashToken(nextRefreshToken));
        session.setUpdatedAt(now);
        session.setExpiresAt(now + refreshExpirationMs);
        return toResponse(user, userSessionRepository.save(session), nextRefreshToken);
    }

    public void logout(LogoutRequest request) {
        userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(hashToken(request.refreshToken()))
                .ifPresent(session -> {
                    long now = System.currentTimeMillis();
                    session.setRevokedAt(now);
                    session.setUpdatedAt(now);
                    userSessionRepository.save(session);
                });
    }

    public void logoutAll(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        long now = System.currentTimeMillis();
        userSessionRepository.findByUserIdAndRevokedAtIsNull(userId).forEach(session -> {
            session.setRevokedAt(now);
            session.setUpdatedAt(now);
            userSessionRepository.save(session);
        });
    }

    private AuthResponse createSessionResponse(User user, String requestedDeviceId) {
        long now = System.currentTimeMillis();
        String refreshToken = newRefreshToken();
        UserSession session = new UserSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setDeviceId(normalizeDeviceId(requestedDeviceId, null));
        session.setRefreshTokenHash(hashToken(refreshToken));
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setExpiresAt(now + refreshExpirationMs);
        return toResponse(user, userSessionRepository.save(session), refreshToken);
    }

    private AuthResponse toResponse(User user, UserSession session, String refreshToken) {
        long expiresAt = System.currentTimeMillis() + jwtService.getExpirationMs();
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                jwtService.generateToken(user),
                "Bearer",
                expiresAt,
                refreshToken,
                session.getExpiresAt(),
                session.getDeviceId(),
                session.getId()
        );
    }

    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String normalizeDeviceId(String requestedDeviceId, String fallbackDeviceId) {
        if (requestedDeviceId != null && !requestedDeviceId.isBlank()) {
            return requestedDeviceId.trim();
        }
        if (fallbackDeviceId != null && !fallbackDeviceId.isBlank()) {
            return fallbackDeviceId;
        }
        return UUID.randomUUID().toString();
    }
}
