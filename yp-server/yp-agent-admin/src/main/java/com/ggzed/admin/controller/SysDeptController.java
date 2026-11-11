package com.ggzed.admin.controller;

import com.ggzed.admin.entity.SysDept;
import com.ggzed.admin.mapper.SysDeptMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理。
 */
@RestController
@RequestMapping("/depts")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptMapper deptMapper;

    @GetMapping
    public List<SysDept> list() {
        return deptMapper.selectList(null);
    }

    @PostMapping
    public SysDept create(@Valid @RequestBody SysDept dept) {
        deptMapper.insert(dept);
        return dept;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        deptMapper.deleteById(id);
    }
}
