package com.demo.service;

import com.demo.dto.request.LoginRequest;
import com.demo.dto.request.RegisterRequest;
import com.demo.dto.response.LoginResponse;
import com.demo.dto.response.UserProfileResponse;
import com.demo.entity.SysUser;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserProfileResponse getCurrentUserProfile(Long userId);

    SysUser requireActiveUser(Long userId);
}
