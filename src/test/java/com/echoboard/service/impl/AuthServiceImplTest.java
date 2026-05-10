package com.echoboard.service.impl;

import com.echoboard.dto.auth.AuthResponse;
import com.echoboard.dto.auth.LoginRequest;
import com.echoboard.dto.auth.LogoutRequest;
import com.echoboard.dto.auth.LogoutResponse;
import com.echoboard.dto.auth.RegisterRequest;
import com.echoboard.dto.auth.RegisterResponse;
import com.echoboard.dto.user.UserProfileResponse;
import com.echoboard.entity.User;
import com.echoboard.enums.UserRole;
import com.echoboard.exception.AppException;
import com.echoboard.exception.ErrorCode;
import com.echoboard.mapper.UserMapper;
import com.echoboard.repository.UserRepository;
import com.echoboard.security.JwtService;
import com.echoboard.service.CurrentUserService;
import com.echoboard.service.RefreshTokenService;
import com.echoboard.util.UserEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.echoboard.exception.ErrorCode.EMAIL_ALREADY_EXISTS;
import static com.echoboard.exception.ErrorCode.FORBIDDEN;
import static com.echoboard.exception.ErrorCode.INVALID_CREDENTIALS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_whenEmailDoesNotExist_shouldCreateUserAndReturnRegisterResponse() {
        RegisterRequest request = new RegisterRequest(
                "Mohamed",
                "  MOHAMED@example.com  ",
                "password123"
        );

        User mappedUser = new User();
        mappedUser.setName(request.getName());

        User savedUser = UserEntityFactory.presenter();
        savedUser.setName("Mohamed");
        savedUser.setEmail("mohamed@example.com");

        RegisterResponse expectedResponse = new RegisterResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                null
        );

        when(userRepository.existsByEmail("mohamed@example.com")).thenReturn(false);
        when(userMapper.RegisterRequestToUser(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userMapper.userToRegisterResponse(savedUser)).thenReturn(expectedResponse);

        RegisterResponse actualResponse = authService.register(request);

        assertEquals(savedUser.getId(), actualResponse.getId());
        assertEquals("Mohamed", actualResponse.getName());
        assertEquals("mohamed@example.com", actualResponse.getEmail());
        assertEquals(UserRole.PRESENTER, actualResponse.getRole());
        assertEquals("Account created successfully", actualResponse.getMessage());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).existsByEmail("mohamed@example.com");
        verify(userMapper).RegisterRequestToUser(request);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(userCaptor.capture());
        verify(userMapper).userToRegisterResponse(savedUser);

        User userBeforeSave = userCaptor.getValue();

        assertEquals("mohamed@example.com", userBeforeSave.getEmail());
        assertEquals("encoded-password", userBeforeSave.getPasswordHash());
        assertEquals(UserRole.PRESENTER, userBeforeSave.getRole());
        assertEquals(true, userBeforeSave.isEnabled());

        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void register_whenEmailAlreadyExists_shouldThrowEmailAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest(
                "Mohamed",
                "mohamed@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("mohamed@example.com")).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.register(request)
        );

        assertEquals(EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals("Email already exists", exception.getMessage());

        verify(userRepository).existsByEmail("mohamed@example.com");
        verify(userMapper, never()).RegisterRequestToUser(any(RegisterRequest.class));
        verify(userRepository, never()).save(any(User.class));

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void login_whenCredentialsAreValid_shouldReturnAuthResponse() {
        LoginRequest request = new LoginRequest(
                "  PRESENTER@example.com  ",
                "password123"
        );

        User user = UserEntityFactory.presenter();
        user.setEmail("presenter@example.com");
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("presenter@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());

        verify(userRepository).findByEmail("presenter@example.com");
        verify(passwordEncoder).matches("password123", "encoded-password");
        verify(jwtService).generateAccessToken(user);
        verify(refreshTokenService).createRefreshToken(user);

        verifyNoInteractions(userMapper);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void login_whenEmailDoesNotExist_shouldThrowInvalidCredentialsException() {
        LoginRequest request = new LoginRequest(
                "missing@example.com",
                "password123"
        );

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.login(request)
        );

        assertEquals(INVALID_CREDENTIALS, exception.getErrorCode());
        assertEquals("Invalid email or password", exception.getMessage());

        verify(userRepository).findByEmail("missing@example.com");

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
        verifyNoInteractions(userMapper);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void login_whenPasswordIsWrong_shouldThrowInvalidCredentialsException() {
        LoginRequest request = new LoginRequest(
                "presenter@example.com",
                "wrong-password"
        );

        User user = UserEntityFactory.presenter();
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("presenter@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.login(request)
        );

        assertEquals(INVALID_CREDENTIALS, exception.getErrorCode());
        assertEquals("Invalid email or password", exception.getMessage());

        verify(userRepository).findByEmail("presenter@example.com");
        verify(passwordEncoder).matches("wrong-password", "encoded-password");

        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
        verifyNoInteractions(userMapper);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void login_whenUserIsDisabled_shouldThrowForbiddenException() {
        LoginRequest request = new LoginRequest(
                "presenter@example.com",
                "password123"
        );

        User user = UserEntityFactory.disabledPresenter();
        user.setEmail("presenter@example.com");
        user.setPasswordHash("encoded-password");

        when(userRepository.findByEmail("presenter@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.login(request)
        );

        assertEquals(FORBIDDEN, exception.getErrorCode());
        assertEquals("Account is disabled", exception.getMessage());

        verify(userRepository).findByEmail("presenter@example.com");
        verify(passwordEncoder).matches("password123", "encoded-password");

        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
        verifyNoInteractions(userMapper);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void logout_shouldRevokeRefreshTokenAndReturnLogoutResponse() {
        LogoutRequest request = new LogoutRequest("refresh-token");

        LogoutResponse response = authService.logout(request);

        assertNotNull(response);
        assertEquals("Logged out successfully", response.getMessage());

        verify(refreshTokenService).revokeToken("refresh-token");

        verifyNoInteractions(userRepository);
        verifyNoInteractions(userMapper);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(currentUserService);
    }

    @Test
    void getUserProfileResponse_shouldReturnCurrentUserProfile() {
        User user = UserEntityFactory.presenter();

        UserProfileResponse expectedResponse = new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled()
        );

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userMapper.userToUserProfileResponse(user)).thenReturn(expectedResponse);

        UserProfileResponse actualResponse = authService.getUserProfileResponse();

        assertEquals(expectedResponse, actualResponse);

        verify(currentUserService).getCurrentUser();
        verify(userMapper).userToUserProfileResponse(user);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
    }
}