package com.ggzed.admin.controller.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ggzed.admin.entity.SysAgent;
import com.ggzed.admin.mapper.SysAgentMapper;
import com.ggzed.admin.model.Result;
import com.ggzed.admin.model.dto.AgentItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent 管理 CRUD。
 *
 * <p>对应前端 API 文档 F-15 ~ F-22（不含导出 F-21）：
 * <ul>
 *   <li>F-15 列表筛选</li>
 *   <li>F-16 编辑回填详情</li>
 *   <li>F-17 新建（草稿）</li>
 *   <li>F-18 更新配置</li>
 *   <li>F-19 发布</li>
 *   <li>F-20 下线</li>
 *   <li>F-22 向导可选项</li>
 * </ul>
 */
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private static final Set<String> HIGH_RISK_TOOL_IDS = Set.of("freeze_quota", "big_debit");
    private static final String GUARD_BLOCK = "block";
    private static final String STATUS_DRAFT = "草稿";
    private static final String STATUS_PUBLISHED = "已发布";
    private static final String STATUS_OFFLINE = "已下线";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final SysAgentMapper agentMapper;

    /** F-15 Agent 列表与筛选（status 精确匹配，keyword 模糊匹配名称或 Code）。 */
    @GetMapping
    public Result<Page<AgentItemVO>> page(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "20") long size,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysAgent> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(SysAgent::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysAgent::getName, keyword).or().like(SysAgent::getCode, keyword));
        }
        wrapper.orderByDesc(SysAgent::getUpdateTime);

        Page<SysAgent> result = agentMapper.selectPage(new Page<>(page, size), wrapper);

        Page<AgentItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toItemVO).toList());
        return Result.ok(voPage);
    }

    /** F-16 编辑回填完整配置。 */
    @GetMapping("/{code}")
    public Result<SysAgent> detail(@PathVariable String code) {
        SysAgent agent = findByCode(code);
        if (agent == null) {
            return Result.fail(404, "Agent 不存在: " + code);
        }
        return Result.ok(agent);
    }

    /** F-17 新建并保存为草稿。 */
    @PostMapping
    public Result<SysAgent> create(@Valid @RequestBody SysAgent agent) {
        if (agent.getCode() == null || agent.getCode().isBlank()) {
            return Result.fail(400, "Agent Code 不能为空");
        }
        if (!agent.getCode().matches("^[a-z][a-z0-9_]{2,}$")) {
            return Result.fail(400, "Code 需以小写字母开头，仅含小写字母、数字与下划线");
        }
        if (findByCode(agent.getCode()) != null) {
            return Result.fail(400, "Agent Code 已存在: " + agent.getCode());
        }

        if (agent.getStatus() == null || agent.getStatus().isBlank()) {
            agent.setStatus(STATUS_DRAFT);
        }
        if (agent.getOwner() == null || agent.getOwner().isBlank()) {
            agent.setOwner("系统管理员");
        }
        if (agent.getCalls() == null) {
            agent.setCalls(0L);
        }

        agentMapper.insert(agent);
        return Result.ok(agent);
    }

    /** F-18 更新配置。 */
    @PutMapping("/{code}")
    public Result<SysAgent> update(@PathVariable String code, @Valid @RequestBody SysAgent agent) {
        SysAgent existing = findByCode(code);
        if (existing == null) {
            return Result.fail(404, "Agent 不存在: " + code);
        }

        agent.setId(existing.getId());
        agent.setCode(code);
        agentMapper.updateById(agent);
        // 返回落库后的完整实体（请求体中未传字段为 null，直接返回会误导前端）
        return Result.ok(findByCode(code));
    }

    /** 逻辑删除（文档未列，作为标准 CRUD 补充）。 */
    @DeleteMapping("/{code}")
    public Result<Void> delete(@PathVariable String code) {
        SysAgent existing = findByCode(code);
        if (existing == null) {
            return Result.fail(404, "Agent 不存在: " + code);
        }
        agentMapper.deleteById(existing.getId());
        return Result.ok(null);
    }

    /** F-19 发布：高危工具必须启用 block 护栏，发布后写入 Nacos（此处模拟返回 dataId）。 */
    @PostMapping("/{code}/publish")
    public Result<Map<String, Object>> publish(@PathVariable String code) {
        SysAgent agent = findByCode(code);
        if (agent == null) {
            return Result.fail(404, "Agent 不存在: " + code);
        }

        if (hasHighRiskTool(agent) && !hasGuard(agent, GUARD_BLOCK)) {
            return Result.fail(400, "已挂载高危工具，必须启用「高危阻断」护栏后才能发布");
        }

        agent.setStatus(STATUS_PUBLISHED);
        agentMapper.updateById(agent);

        String dataId = "agt_" + agent.getCode();
        return Result.ok(Map.of(
                "dataId", dataId,
                "nacosDataId", "agent-" + agent.getCode(),
                "endpoint", "/assistant/chat/" + agent.getCode(),
                "effectiveIn", "30s"
        ));
    }

    /** F-20 下线。 */
    @PostMapping("/{code}/offline")
    public Result<Void> offline(@PathVariable String code) {
        SysAgent agent = findByCode(code);
        if (agent == null) {
            return Result.fail(404, "Agent 不存在: " + code);
        }
        agent.setStatus(STATUS_OFFLINE);
        agentMapper.updateById(agent);
        return Result.ok(null);
    }

    /** F-22 向导可选项：模型、部门、知识库、工具分类、护栏。 */
    @GetMapping("/options")
    public Result<Map<String, Object>> options() {
        return Result.ok(Map.of(
                "models", List.of("DeepSeek-V3", "Qwen2.5-72B", "豆包Pro-32K"),
                "depts", List.of("零售业务部", "信用卡中心", "小微金融", "公司金融部", "风控运营中心"),
                "knowledgeBases", List.of(
                        Map.of("code", "risk_rules_v3", "name", "risk_rules_v3 · 风控规则库"),
                        Map.of("code", "collection_rules", "name", "collection_rules · 催收合规库"),
                        Map.of("code", "none", "name", "不挂载知识库")
                ),
                "toolCategories", List.of(
                        Map.of("category", "查询类", "items", List.of(
                                Map.of("id", "query_account", "name", "账户余额查询", "desc", "查询指定账户的实时余额与冻结金额", "risk", false),
                                Map.of("id", "query_credit", "name", "征信报告查询", "desc", "拉取客户征信简版报告", "risk", false)
                        )),
                        Map.of("category", "交易类", "items", List.of(
                                Map.of("id", "transfer", "name", "转账", "desc", "发起同行/跨行转账", "risk", false),
                                Map.of("id", "big_debit", "name", "大额扣款", "desc", "单笔 ≥ 50 万的代扣操作", "risk", true)
                        )),
                        Map.of("category", "风控类", "items", List.of(
                                Map.of("id", "freeze_quota", "name", "冻结额度", "desc", "冻结客户可用额度", "risk", true),
                                Map.of("id", "risk_scan", "name", "风险扫描", "desc", "扫描交易流水并输出风险标签", "risk", false)
                        ))
                ),
                "guards", List.of(
                        Map.of("id", "block", "name", "高危阻断", "desc", "命中高危工具或敏感词时直接拦截并转人工"),
                        Map.of("id", "audit", "name", "操作审计", "desc", "所有工具调用写入合规审计日志"),
                        Map.of("id", "rate_limit", "name", "频控限流", "desc", "单用户每分钟调用次数上限"),
                        Map.of("id", "sensitive_mask", "name", "敏感词脱敏", "desc", "输出侧对身份证号、手机号等脱敏")
                )
        ));
    }

    /* ---------------- 私有辅助 ---------------- */

    private SysAgent findByCode(String code) {
        return agentMapper.selectOne(new LambdaQueryWrapper<SysAgent>()
                .eq(SysAgent::getCode, code));
    }

    private AgentItemVO toItemVO(SysAgent agent) {
        AgentItemVO vo = new AgentItemVO();
        vo.setName(agent.getName());
        vo.setCode(agent.getCode());
        vo.setType(agent.getType());
        vo.setModel(agent.getModel());
        vo.setStatus(agent.getStatus());
        vo.setTools(agent.getTools() == null ? 0 : agent.getTools().length);
        vo.setCalls(agent.getCalls() == null ? 0L : agent.getCalls());
        vo.setBy(agent.getOwner());
        vo.setTime(agent.getUpdateTime() == null ? null : agent.getUpdateTime().format(TIME_FMT));
        return vo;
    }

    private boolean hasHighRiskTool(SysAgent agent) {
        return agent.getTools() != null && Arrays.stream(agent.getTools()).anyMatch(HIGH_RISK_TOOL_IDS::contains);
    }

    private boolean hasGuard(SysAgent agent, String guardId) {
        return agent.getGuards() != null && Arrays.asList(agent.getGuards()).contains(guardId);
    }
}
