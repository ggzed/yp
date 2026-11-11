package com.ggzed.admin.controller.console;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ggzed.admin.entity.SysMcpServer;
import com.ggzed.admin.mapper.SysMcpServerMapper;
import com.ggzed.admin.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * MCP Server 注册 CRUD（F-26 ~ F-29）。
 */
@RestController
@RequestMapping("/mcp-servers")
@RequiredArgsConstructor
public class McpServerController {

    private final SysMcpServerMapper serverMapper;

    /** 已注册服务列表。 */
    @GetMapping
    public Result<Page<SysMcpServer>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long size,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysMcpServer> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(SysMcpServer::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysMcpServer::getName, keyword)
                    .or().like(SysMcpServer::getAddress, keyword));
        }
        wrapper.orderByDesc(SysMcpServer::getUpdateTime);
        return Result.ok(serverMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @GetMapping("/{name}")
    public Result<SysMcpServer> detail(@PathVariable String name) {
        SysMcpServer server = findByName(name);
        if (server == null) {
            return Result.fail(404, "MCP Server 不存在: " + name);
        }
        return Result.ok(server);
    }

    @PostMapping
    public Result<SysMcpServer> create(@RequestBody SysMcpServer server) {
        if (server.getName() == null || server.getName().isBlank()) {
            return Result.fail(400, "服务名不能为空");
        }
        if (findByName(server.getName()) != null) {
            return Result.fail(400, "服务名已存在: " + server.getName());
        }
        serverMapper.insert(server);
        return Result.ok(server);
    }

    @PutMapping("/{name}")
    public Result<SysMcpServer> update(@PathVariable String name, @RequestBody SysMcpServer server) {
        SysMcpServer existing = findByName(name);
        if (existing == null) {
            return Result.fail(404, "MCP Server 不存在: " + name);
        }
        server.setId(existing.getId());
        server.setName(name);
        serverMapper.updateById(server);
        return Result.ok(findByName(name));
    }

    @DeleteMapping("/{name}")
    public Result<Void> delete(@PathVariable String name) {
        SysMcpServer existing = findByName(name);
        if (existing == null) {
            return Result.fail(404, "MCP Server 不存在: " + name);
        }
        serverMapper.deleteById(existing.getId());
        return Result.ok(null);
    }

    private SysMcpServer findByName(String name) {
        return serverMapper.selectOne(
                new LambdaQueryWrapper<SysMcpServer>().eq(SysMcpServer::getName, name));
    }
}
