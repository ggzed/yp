package com.ggzed.admin.controller.console;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ggzed.admin.entity.SysTool;
import com.ggzed.admin.mapper.SysToolMapper;
import com.ggzed.admin.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 工具分类目录 CRUD（F-23 ~ F-24）。
 */
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
public class ToolController {

    private final SysToolMapper toolMapper;

    /** 工具列表与筛选。 */
    @GetMapping
    public Result<Page<SysTool>> page(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "20") long size,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String status) {
        LambdaQueryWrapper<SysTool> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            wrapper.eq(SysTool::getCat, category);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(SysTool::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysTool::getName, keyword).or().like(SysTool::getCode, keyword));
        }
        wrapper.orderByDesc(SysTool::getUpdateTime);
        return Result.ok(toolMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 分类筛选项。 */
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        List<Object> values = toolMapper.selectObjs(
                new QueryWrapper<SysTool>().select("DISTINCT cat").orderByAsc("cat"));
        return Result.ok(values.stream().filter(Objects::nonNull).map(String::valueOf).toList());
    }

    @GetMapping("/{code}")
    public Result<SysTool> detail(@PathVariable String code) {
        SysTool tool = findByCode(code);
        if (tool == null) {
            return Result.fail(404, "工具不存在: " + code);
        }
        return Result.ok(tool);
    }

    @PostMapping
    public Result<SysTool> create(@RequestBody SysTool tool) {
        if (tool.getCode() == null || tool.getCode().isBlank()) {
            return Result.fail(400, "工具 Code 不能为空");
        }
        if (findByCode(tool.getCode()) != null) {
            return Result.fail(400, "工具 Code 已存在: " + tool.getCode());
        }
        if (tool.getRisk() == null) {
            tool.setRisk(false);
        }
        toolMapper.insert(tool);
        return Result.ok(tool);
    }

    @PutMapping("/{code}")
    public Result<SysTool> update(@PathVariable String code, @RequestBody SysTool tool) {
        SysTool existing = findByCode(code);
        if (existing == null) {
            return Result.fail(404, "工具不存在: " + code);
        }
        tool.setId(existing.getId());
        tool.setCode(code);
        toolMapper.updateById(tool);
        return Result.ok(findByCode(code));
    }

    @DeleteMapping("/{code}")
    public Result<Void> delete(@PathVariable String code) {
        SysTool existing = findByCode(code);
        if (existing == null) {
            return Result.fail(404, "工具不存在: " + code);
        }
        toolMapper.deleteById(existing.getId());
        return Result.ok(null);
    }

    private SysTool findByCode(String code) {
        return toolMapper.selectOne(new LambdaQueryWrapper<SysTool>().eq(SysTool::getCode, code));
    }
}
