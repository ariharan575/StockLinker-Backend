package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.constant.AuditAction;
import com.backend.StockLinker.AuthService.exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.AuthService.dto.response.UserInfoResponse;
import com.backend.StockLinker.AuthService.mapper.UserMapper;
import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuditService auditService;

    // =========================================================
    // 🔍 GET USER BY ID (WITH CUSTOM EXCEPTION)
    // =========================================================
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    // =========================================================
    // 🔍 GET USER WITH ROLES (WITH CUSTOM EXCEPTION)
    // =========================================================
    public User getUserWithRoles(String userId) {
        return userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    // =========================================================
    // 📋 GET USER INFO RESPONSE (USING MAPPER)
    // =========================================================
    public UserInfoResponse getUserInfo(String userId) {
        User user = getUserWithRoles(userId);
        boolean isNewUser = user.getRoles().isEmpty();
        return userMapper.toUserInfoResponse(user, isNewUser);
    }

    // =========================================================
    // 🔒 BLOCK USER (ADMIN ONLY)
    // =========================================================
    public void blockUser(String userId, User adminUser, HttpServletRequest request) {
        User user = getUserById(userId);

        if (user.getAccountStatus() == User.AccountStatus.BLOCKED) {
            throw new ResourceNotFoundException("User already blocked");
        }

        user.setAccountStatus(User.AccountStatus.BLOCKED);
        userRepository.save(user);

        // Audit log
        auditService.log(auditService.success(
                adminUser,
                AuditAction.USER_BLOCKED.name(),
                AuditLog.ResourceType.USER.name(),
                userId,
                getClientIp(request),
                request.getHeader("User-Agent")
        ));

        log.info("User {} blocked by admin {}", userId, adminUser.getId());
    }

    // =========================================================
    // 🔓 UNBLOCK USER (ADMIN ONLY)
    // =========================================================
    public void unblockUser(String userId, User adminUser, HttpServletRequest request) {
        User user = getUserById(userId);

        if (user.getAccountStatus() != User.AccountStatus.BLOCKED) {
            throw new ResourceNotFoundException("User is not blocked");
        }

        user.setAccountStatus(User.AccountStatus.ACTIVE);
        user.resetFailedAttempts();
        userRepository.save(user);

        // Audit log
        auditService.log(auditService.success(
                adminUser,
                AuditAction.USER_UNBLOCKED.name(),
                AuditLog.ResourceType.USER.name(),
                userId,
                getClientIp(request),
                request.getHeader("User-Agent")
        ));

        log.info("User {} unblocked by admin {}", userId, adminUser.getId());
    }

    // =========================================================
    // 🌐 IP HELPER
    // =========================================================
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xf = request.getHeader("X-Forwarded-For");
        return xf != null ? xf.split(",")[0] : request.getRemoteAddr();
    }
}
