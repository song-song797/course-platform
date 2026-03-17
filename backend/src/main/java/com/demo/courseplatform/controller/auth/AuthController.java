package com.demo.courseplatform.controller.auth;

import com.demo.courseplatform.common.ApiResponse;
import com.demo.courseplatform.domain.dto.DemoRequests;
import com.demo.courseplatform.domain.vo.DemoViews;
import com.demo.courseplatform.security.TokenService;
import com.demo.courseplatform.service.DemoPlatformService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final DemoPlatformService demoPlatformService;
    private final TokenService tokenService;

    public AuthController(DemoPlatformService demoPlatformService, TokenService tokenService) {
        this.demoPlatformService = demoPlatformService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ApiResponse<DemoViews.LoginVo> login(@Valid @RequestBody DemoRequests.LoginRequest request) {
        DemoViews.UserProfileVo profile = demoPlatformService.authenticate(request.username(), request.password());
        String token = tokenService.issueToken(profile.id());
        return ApiResponse.success(new DemoViews.LoginVo(token, profile));
    }

    @GetMapping("/me")
    public ApiResponse<DemoViews.UserProfileVo> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.getUserProfile(userId));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(HttpServletRequest request, @Valid @RequestBody DemoRequests.ChangePasswordRequest requestBody) {
        Long userId = (Long) request.getAttribute("currentUserId");
        demoPlatformService.changePassword(userId, requestBody.newPassword());
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = (String) request.getAttribute("currentToken");
        tokenService.revoke(token);
        return ApiResponse.success();
    }
}
