package com.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.auth.JwtTokenService;
import com.demo.dto.request.LoginRequest;
import com.demo.dto.request.RegisterRequest;
import com.demo.dto.response.LoginResponse;
import com.demo.dto.response.UserProfileResponse;
import com.demo.entity.SysUser;
import com.demo.exception.BizException;
import com.demo.exception.NotFoundException;
import com.demo.exception.UnauthorizedException;
import com.demo.mapper.SysUserMapper;
import com.demo.service.AuthService;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(SysUserMapper sysUserMapper, JwtTokenService jwtTokenService, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        SysUser existingUser = findByUsername(username);
        if (existingUser != null) {
            throw new BizException(400, "用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(buildNickname(request.getNickname(), username));
        user.setStatus(1);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        sysUserMapper.insert(user);

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        SysUser user = findByUsername(username);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new UnauthorizedException("账号已被禁用");
        }
        return buildLoginResponse(user);
    }

    @Override
    public UserProfileResponse getCurrentUserProfile(Long userId) {
        return toProfile(requireActiveUser(userId));
    }

    @Override
    public SysUser requireActiveUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new UnauthorizedException("账号已被禁用");
        }
        return user;
    }

    private SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
    }

    private LoginResponse buildLoginResponse(SysUser user) {
        return LoginResponse.builder()
                .token(jwtTokenService.generateToken(user.getId(), user.getUsername()))
                .tokenType("Bearer")
                .expiresIn(jwtTokenService.getExpirationSeconds())
                .user(toProfile(user))
                .build();
    }

    private UserProfileResponse toProfile(SysUser user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }

    private String buildNickname(String nickname, String username) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return username;
        }
        return nickname.trim();
    }
}
