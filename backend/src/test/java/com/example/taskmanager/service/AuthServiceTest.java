package com.example.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.example.taskmanager.dto.AuthResponse;
import com.example.taskmanager.dto.LoginRequest;
import com.example.taskmanager.dto.RefreshTokenRequest;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.entity.UserSession;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.repository.UserSessionRepository;
import com.example.taskmanager.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final long REFRESH_TTL = 2_592_000_000L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private JwtService jwtService;

    @Test
    void loginCreatesRefreshSessionWithHashedToken() {
        User user = testUser();
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("tung", "tung")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(jwtService.getExpirationMs()).thenReturn(60_000L);
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService service = new AuthService(userRepository, userSessionRepository, jwtService, REFRESH_TTL);
        AuthResponse response = service.login(new LoginRequest("tung", "secret123", "device-a"));

        assertEquals("access-token", response.token());
        assertEquals("device-a", response.deviceId());
        assertNotNull(response.refreshToken());
        assertFalse(response.refreshToken().isBlank());

        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(captor.capture());
        UserSession savedSession = captor.getValue();
        assertEquals(user.getId(), savedSession.getUserId());
        assertEquals("device-a", savedSession.getDeviceId());
        assertNotEquals(response.refreshToken(), savedSession.getRefreshTokenHash());
        assertEquals(AuthService.hashToken(response.refreshToken()), savedSession.getRefreshTokenHash());
    }

    @Test
    void refreshRotatesRefreshToken() {
        String oldRefreshToken = "old-refresh-token";
        User user = testUser();
        UserSession session = new UserSession();
        session.setId("session-1");
        session.setUserId(user.getId());
        session.setDeviceId("device-a");
        session.setRefreshTokenHash(AuthService.hashToken(oldRefreshToken));
        session.setCreatedAt(System.currentTimeMillis());
        session.setUpdatedAt(System.currentTimeMillis());
        session.setExpiresAt(System.currentTimeMillis() + REFRESH_TTL);

        when(userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(eq(AuthService.hashToken(oldRefreshToken))))
                .thenReturn(Optional.of(session));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("next-access-token");
        when(jwtService.getExpirationMs()).thenReturn(60_000L);
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService service = new AuthService(userRepository, userSessionRepository, jwtService, REFRESH_TTL);
        AuthResponse response = service.refresh(new RefreshTokenRequest(oldRefreshToken, "device-b"));

        assertEquals("next-access-token", response.token());
        assertEquals("device-b", response.deviceId());
        assertNotEquals(oldRefreshToken, response.refreshToken());
        assertEquals(AuthService.hashToken(response.refreshToken()), session.getRefreshTokenHash());
        assertEquals("device-b", session.getDeviceId());
    }

    private static User testUser() {
        User user = new User();
        user.setId("user-1");
        user.setUsername("tung");
        user.setEmail("tung@example.com");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("secret123"));
        user.setCreatedAt(1_700_000_000_000L);
        user.setUpdatedAt(1_700_000_000_000L);
        return user;
    }
}
