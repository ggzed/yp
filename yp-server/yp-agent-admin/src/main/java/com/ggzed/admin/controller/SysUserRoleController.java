package com.ggzed.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ggzed.admin.entity.SysUserRole;
import com.ggzed.admin.mapper.SysUserRoleMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户角色分配。
 */
@RestController
@RequestMapping("/user-roles")
@RequiredArgsConstructor
public class SysUserRoleController {

    private final SysUserRoleMapper userRoleMapper;

    @GetMapping
    public List<SysUserRole> list(@RequestParam Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
    }

    @PostMapping
    public SysUserRole assign(@Valid @RequestBody SysUserRole userRole) {
        userRoleMapper.insert(userRole);
        return userRole;
    }

    @DeleteMapping("/{id}")
    public void unassign(@PathVariable Long id) {
        userRoleMapper.deleteById(id);
    }
}
