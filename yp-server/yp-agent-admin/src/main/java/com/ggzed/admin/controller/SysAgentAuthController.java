package com.ggzed.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ggzed.admin.entity.SysAgentAuth;
import com.ggzed.admin.mapper.SysAgentAuthMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 数据权限授权管理。
 */
@RestController
@RequestMapping("/agent-auth")
@RequiredArgsConstructor
public class SysAgentAuthController {

    private final SysAgentAuthMapper authMapper;

    /** 按目标查询授权记录，type: 1 用户 / 2 部门 */
    @GetMapping
    public List<SysAgentAuth> list(@RequestParam Long targetId,
                                   @RequestParam(defaultValue = "1") Integer targetType) {
        return authMapper.selectList(new LambdaQueryWrapper<SysAgentAuth>()
                .eq(SysAgentAuth::getTargetId, targetId)
                .eq(SysAgentAuth::getTargetType, targetType));
    }

    @PostMapping
    public SysAgentAuth grant(@Valid @RequestBody SysAgentAuth auth) {
        authMapper.insert(auth);
        return auth;
    }

    @DeleteMapping("/{id}")
    public void revoke(@PathVariable Long id) {
        authMapper.deleteById(id);
    }
}
