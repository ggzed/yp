package com.ggzed.admin.controller;

import com.ggzed.admin.entity.SysRole;
import com.ggzed.admin.mapper.SysRoleMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理。
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleMapper roleMapper;

    @GetMapping
    public List<SysRole> list() {
        return roleMapper.selectList(null);
    }

    @PostMapping
    public SysRole create(@Valid @RequestBody SysRole role) {
        roleMapper.insert(role);
        return role;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        roleMapper.deleteById(id);
    }
}
