package com.ggzed.admin.controller.console;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ggzed.admin.entity.SysApp;
import com.ggzed.admin.mapper.SysAppMapper;
import com.ggzed.admin.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 应用中心（我的 Agent）CRUD（F-07）。
 */
@RestController
@RequestMapping("/apps")
@RequiredArgsConstructor
public class AppController {

    private final SysAppMapper appMapper;

    /** 应用卡片列表。 */
    @GetMapping
    public Result<Page<SysApp>> page(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "20") long size,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysApp> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(SysApp::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysApp::getName, keyword).or().like(SysApp::getCode, keyword));
        }
        wrapper.orderByDesc(SysApp::getUpdateTime);
        return Result.ok(appMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @GetMapping("/{code}")
    public Result<SysApp> detail(@PathVariable String code) {
        SysApp app = findByCode(code);
        if (app == null) {
            return Result.fail(404, "应用不存在: " + code);
        }
        return Result.ok(app);
    }

    @PostMapping
    public Result<SysApp> create(@RequestBody SysApp app) {
        if (app.getCode() == null || app.getCode().isBlank()) {
            return Result.fail(400, "应用 Code 不能为空");
        }
        if (findByCode(app.getCode()) != null) {
            return Result.fail(400, "应用 Code 已存在: " + app.getCode());
        }
        if (app.getCalls() == null) {
            app.setCalls(0L);
        }
        appMapper.insert(app);
        return Result.ok(app);
    }

    @PutMapping("/{code}")
    public Result<SysApp> update(@PathVariable String code, @RequestBody SysApp app) {
        SysApp existing = findByCode(code);
        if (existing == null) {
            return Result.fail(404, "应用不存在: " + code);
        }
        app.setId(existing.getId());
        app.setCode(code);
        appMapper.updateById(app);
        return Result.ok(findByCode(code));
    }

    @DeleteMapping("/{code}")
    public Result<Void> delete(@PathVariable String code) {
        SysApp existing = findByCode(code);
        if (existing == null) {
            return Result.fail(404, "应用不存在: " + code);
        }
        appMapper.deleteById(existing.getId());
        return Result.ok(null);
    }

    private SysApp findByCode(String code) {
        return appMapper.selectOne(new LambdaQueryWrapper<SysApp>().eq(SysApp::getCode, code));
    }
}
