package com.demo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.auth.JwtTokenService;
import com.demo.dto.request.LoginRequest;
import com.demo.dto.request.RegisterRequest;
import com.demo.dto.response.LoginResponse;
import com.demo.entity.SysUser;
import com.demo.exception.BizException;
import com.demo.exception.UnauthorizedException;
import com.demo.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("alice");
        registerRequest.setPassword("password123");
        registerRequest.setNickname("Alice");
    }

    @Test
    void registerShouldHashPasswordAndReturnToken() {
        when(sysUserMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtTokenService.generateToken(1L, "alice")).thenReturn("jwt-token");
        when(jwtTokenService.getExpirationSeconds()).thenReturn(3600L);
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });

        LoginResponse response = authService.register(registerRequest);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(captor.capture());
        assertEquals("encoded-password", captor.getValue().getPasswordHash());
        assertEquals("jwt-token", response.getToken());
        assertEquals("alice", response.getUser().getUsername());
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        when(sysUserMapper.selectOne(any())).thenReturn(new SysUser());

        assertThrows(BizException.class, () -> authService.register(registerRequest));
    }

    @Test
    void loginShouldRejectWrongPassword() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setPasswordHash("encoded");
        user.setStatus(1);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(toLoginRequest()));
    }

    @Test
    void loginShouldReturnJwtForActiveUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setNickname("Alice");
        user.setPasswordHash("encoded");
        user.setStatus(1);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtTokenService.generateToken(1L, "alice")).thenReturn("jwt-token");
        when(jwtTokenService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(toLoginRequest());

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(3600L, response.getExpiresIn());
    }

    private LoginRequest toLoginRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("alice");
        loginRequest.setPassword("password123");
        return loginRequest;
    }
}
