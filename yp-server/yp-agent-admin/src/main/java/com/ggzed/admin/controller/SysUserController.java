package com.ggzed.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ggzed.admin.entity.SysUser;
import com.ggzed.admin.mapper.SysUserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理。
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping
    public Page<SysUser> page(@RequestParam(defaultValue = "1") long page,
                              @RequestParam(defaultValue = "20") long size,
                              @RequestParam(required = false) String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(SysUser::getUsername, username);
        }
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @PostMapping
    public SysUser create(@Valid @RequestBody SysUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userMapper.deleteById(id);
    }
}
