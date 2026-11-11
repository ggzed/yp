package com.ggzed.admin.controller.console;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ggzed.admin.entity.SysKbDocument;
import com.ggzed.admin.mapper.SysKbDocumentMapper;
import com.ggzed.admin.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库文档 CRUD（F-30 ~ F-33）。
 */
@RestController
@RequestMapping("/kb/documents")
@RequiredArgsConstructor
public class KbDocumentController {

    private static final String STATUS_PARSING = "解析中";

    private final SysKbDocumentMapper documentMapper;

    /** 文档列表与筛选。 */
    @GetMapping
    public Result<Page<SysKbDocument>> page(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) String index,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysKbDocument> wrapper = new LambdaQueryWrapper<>();
        if (index != null && !index.isBlank()) {
            wrapper.eq(SysKbDocument::getIndexName, index);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(SysKbDocument::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SysKbDocument::getName, keyword);
        }
        wrapper.orderByDesc(SysKbDocument::getUpdateTime);
        return Result.ok(documentMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @GetMapping("/{id}")
    public Result<SysKbDocument> detail(@PathVariable Long id) {
        SysKbDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            return Result.fail(404, "文档不存在: " + id);
        }
        return Result.ok(doc);
    }

    @PostMapping
    public Result<SysKbDocument> create(@RequestBody SysKbDocument doc) {
        if (doc.getName() == null || doc.getName().isBlank()) {
            return Result.fail(400, "文档名称不能为空");
        }
        if (doc.getStatus() == null || doc.getStatus().isBlank()) {
            doc.setStatus(STATUS_PARSING);
        }
        if (doc.getChunks() == null) {
            doc.setChunks(0);
        }
        documentMapper.insert(doc);
        return Result.ok(doc);
    }

    @PutMapping("/{id}")
    public Result<SysKbDocument> update(@PathVariable Long id, @RequestBody SysKbDocument doc) {
        SysKbDocument existing = documentMapper.selectById(id);
        if (existing == null) {
            return Result.fail(404, "文档不存在: " + id);
        }
        doc.setId(id);
        documentMapper.updateById(doc);
        return Result.ok(documentMapper.selectById(id));
    }

    /** 重新解析：状态重置为解析中。 */
    @PostMapping("/{id}/retry")
    public Result<SysKbDocument> retry(@PathVariable Long id) {
        SysKbDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            return Result.fail(404, "文档不存在: " + id);
        }
        doc.setStatus(STATUS_PARSING);
        documentMapper.updateById(doc);
        return Result.ok(doc);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (documentMapper.selectById(id) == null) {
            return Result.fail(404, "文档不存在: " + id);
        }
        documentMapper.deleteById(id);
        return Result.ok(null);
    }
}
