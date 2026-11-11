package com.ggzed.admin.controller.console;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ggzed.admin.entity.SysAlert;
import com.ggzed.admin.mapper.SysAlertMapper;
import com.ggzed.admin.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

/**
 * 告警提示 CRUD（F-04 ~ F-06）。
 */
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private static final String STATE_NEW = "未处理";
    private static final String STATE_PROCESSING = "处理中";

    private final SysAlertMapper alertMapper;

    /** 告警列表与筛选。 */
    @GetMapping
    public Result<Page<SysAlert>> page(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "20") long size,
                                       @RequestParam(required = false) String state,
                                       @RequestParam(required = false) String level) {
        LambdaQueryWrapper<SysAlert> wrapper = new LambdaQueryWrapper<>();
        if (state != null && !state.isBlank()) {
            wrapper.eq(SysAlert::getState, state);
        }
        if (level != null && !level.isBlank()) {
            wrapper.eq(SysAlert::getLevel, level);
        }
        wrapper.orderByDesc(SysAlert::getTime);
        return Result.ok(alertMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 告警详情。 */
    @GetMapping("/{id}")
    public Result<SysAlert> detail(@PathVariable Long id) {
        SysAlert alert = alertMapper.selectById(id);
        if (alert == null) {
            return Result.fail(404, "告警不存在: " + id);
        }
        return Result.ok(alert);
    }

    /** 新建告警。 */
    @PostMapping
    public Result<SysAlert> create(@RequestBody SysAlert alert) {
        if (alert.getTime() == null) {
            alert.setTime(OffsetDateTime.now());
        }
        if (alert.getState() == null || alert.getState().isBlank()) {
            alert.setState(STATE_NEW);
        }
        if (alert.getRead() == null) {
            alert.setRead(false);
        }
        alertMapper.insert(alert);
        return Result.ok(alert);
    }

    /** 更新告警。 */
    @PutMapping("/{id}")
    public Result<SysAlert> update(@PathVariable Long id, @RequestBody SysAlert alert) {
        SysAlert existing = alertMapper.selectById(id);
        if (existing == null) {
            return Result.fail(404, "告警不存在: " + id);
        }
        alert.setId(id);
        alertMapper.updateById(alert);
        return Result.ok(alertMapper.selectById(id));
    }

    /** 删除告警（逻辑删除）。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (alertMapper.selectById(id) == null) {
            return Result.fail(404, "告警不存在: " + id);
        }
        alertMapper.deleteById(id);
        return Result.ok(null);
    }

    /** 认领告警：未处理 → 处理中。 */
    @PostMapping("/{id}/claim")
    public Result<SysAlert> claim(@PathVariable Long id,
                                  @RequestParam(required = false) String claimedBy) {
        SysAlert alert = alertMapper.selectById(id);
        if (alert == null) {
            return Result.fail(404, "告警不存在: " + id);
        }
        alert.setState(STATE_PROCESSING);
        alert.setClaimedBy(claimedBy);
        alertMapper.updateById(alert);
        return Result.ok(alert);
    }

    /** 全部标记已读。 */
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        SysAlert patch = new SysAlert();
        patch.setRead(true);
        alertMapper.update(patch, new LambdaUpdateWrapper<SysAlert>().eq(SysAlert::getRead, false));
        return Result.ok(null);
    }
}
