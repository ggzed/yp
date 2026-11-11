package com.ggzed.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ggzed.admin.entity.SysRole;
import com.ggzed.admin.entity.SysUser;
import com.ggzed.admin.entity.SysUserRole;
import com.ggzed.admin.mapper.SysRoleMapper;
import com.ggzed.admin.mapper.SysUserMapper;
import com.ggzed.admin.mapper.SysUserRoleMapper;
import com.ggzed.admin.model.Result;
import com.ggzed.admin.model.dto.LoginResponse;
import com.ggzed.admin.model.request.LoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 认证接口：账号密码登录，返回用户信息与角色编码。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.fail(401, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.fail(403, "账号已停用，请联系管理员");
        }

        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId()))
                .stream().map(SysUserRole::getRoleId).toList();
        List<String> roles = roleIds.isEmpty()
                ? List.of()
                : roleMapper.selectBatchIds(roleIds).stream()
                        .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                        .map(SysRole::getRoleCode)
                        .toList();

        user.setLastLoginTime(OffsetDateTime.now());
        userMapper.updateById(user);

        LoginResponse data = new LoginResponse(
                UUID.randomUUID().toString(),
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getDeptId(),
                roles);
        return Result.ok(data);
    }
}
