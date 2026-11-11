package com.ggzed.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ggzed.admin.entity.SysAlert;
import com.ggzed.admin.entity.SysRole;
import com.ggzed.admin.entity.SysUser;
import com.ggzed.admin.entity.SysUserRole;
import com.ggzed.admin.mapper.SysAlertMapper;
import com.ggzed.admin.mapper.SysRoleMapper;
import com.ggzed.admin.mapper.SysUserMapper;
import com.ggzed.admin.mapper.SysUserRoleMapper;
import com.ggzed.admin.model.Result;
import com.ggzed.admin.model.dto.MenuGroupVO;
import com.ggzed.admin.model.dto.MenuItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 登录后按用户角色下发左侧菜单。
 * 菜单结构随代码版本发布；角色与用户的绑定关系读取 yp_sys_user / yp_sys_role。
 */
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysAlertMapper alertMapper;

    /**
     * @param username 登录用户名，用于查询该用户拥有的角色
     * @param role     当前视角角色（可选），须属于该用户，用于角色切换后的菜单重算
     */
    @GetMapping
    public Result<List<MenuGroupVO>> menus(@RequestParam(required = false) String username,
                                           @RequestParam(required = false) String role) {
        Set<String> roles = resolveRoles(username, role);
        if (roles.isEmpty()) {
            return Result.ok(List.of());
        }

        long unreadAlerts = alertMapper.selectCount(
                new LambdaQueryWrapper<SysAlert>().eq(SysAlert::getRead, false));

        List<MenuGroupVO> groups = new ArrayList<>();
        for (MenuGroupDef group : MENUS) {
            List<MenuItemVO> items = new ArrayList<>();
            for (MenuItemDef item : group.items()) {
                List<String> actions = item.actionsFor(roles);
                if (actions.isEmpty()) {
                    continue;
                }
                String badge = item.badge();
                if (ALERT_PATH.equals(item.path()) && unreadAlerts > 0) {
                    badge = String.valueOf(unreadAlerts);
                }
                items.add(new MenuItemVO(item.path(), item.label(), badge, item.tag(), actions));
            }
            if (!items.isEmpty()) {
                groups.add(new MenuGroupVO(group.id(), group.label(), group.icon(), items));
            }
        }
        return Result.ok(groups);
    }

    /** 未登录时按传入角色计算；登录后以数据库中的角色为准。 */
    private Set<String> resolveRoles(String username, String role) {
        if (username == null || username.isBlank()) {
            return role == null || role.isBlank() ? Set.of() : Set.of(role);
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            return Set.of();
        }
        List<SysUserRole> links = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        if (links.isEmpty()) {
            return Set.of();
        }
        Set<String> owned = new LinkedHashSet<>();
        roleMapper.selectBatchIds(links.stream().map(SysUserRole::getRoleId).toList()).stream()
                .filter(item -> !Integer.valueOf(0).equals(item.getStatus()))
                .map(SysRole::getRoleCode)
                .forEach(owned::add);
        if (role != null && !role.isBlank() && owned.contains(role)) {
            return Set.of(role);
        }
        return owned;
    }

    /* ------------------------------ 菜单定义 ------------------------------ */

    private static final String ALERT_PATH = "/dashboard/alerts";

    private static final List<String> VIEW = List.of("view");
    private static final List<String> APP_SALES = List.of("view", "use");
    private static final List<String> APP_ADMIN = List.of("view", "use", "manage");
    private static final List<String> ALERT_ADMIN = List.of("view", "claim", "manage");
    private static final List<String> TOOL_ADMIN = List.of("view", "manage", "debug");
    private static final List<String> SERVER_ADMIN = List.of("view", "manage", "test");
    private static final List<String> DOC_ADMIN = List.of("view", "manage", "retry");
    private static final List<String> RECALL_ADMIN = List.of("view", "test");
    private static final List<String> AGENT_ADMIN = List.of("view", "manage", "publish");

    private static final List<MenuGroupDef> MENUS = List.of(
            new MenuGroupDef("dashboard", "仪表盘", "grid", List.of(
                    item("/dashboard/overview", "系统总览", null, null,
                            Map.of("sales", VIEW, "admin", VIEW, "ops", VIEW)),
                    item(ALERT_PATH, "告警提示", "2", null,
                            Map.of("admin", ALERT_ADMIN, "ops", VIEW))
            )),
            new MenuGroupDef("agents", "智能应用与编排", "nodes", List.of(
                    item("/apps", "我的 Agent", null, null,
                            Map.of("sales", APP_SALES, "admin", APP_ADMIN)),
                    item("/apps/workflow-chat", "工作流对话", null, null,
                            Map.of("sales", APP_SALES, "admin", APP_ADMIN)),
                    item("/agents", "Agent 列表", null, null,
                            Map.of("admin", AGENT_ADMIN))
            )),
            new MenuGroupDef("hub", "工具与插件广场", "sliders", List.of(
                    item("/hub/tools", "工具分类目录", null, null,
                            Map.of("admin", TOOL_ADMIN, "ops", VIEW)),
                    item("/hub/servers", "MCP Server 注册", null, null,
                            Map.of("admin", SERVER_ADMIN, "ops", VIEW)),
                    item("/hub/debug", "接口调试", null, null,
                            Map.of("admin", List.of("view", "debug")))
            )),
            new MenuGroupDef("kb", "知识库管理", "book", List.of(
                    item("/kb/docs", "文档列表", null, null,
                            Map.of("admin", DOC_ADMIN, "ops", VIEW)),
                    item("/kb/recall", "召回测试", null, null,
                            Map.of("admin", RECALL_ADMIN, "ops", VIEW))
            )),
            new MenuGroupDef("audit", "运营与审计", "chart", List.of(
                    item("/audit/billing", "Token 计费大盘", null, null,
                            Map.of("admin", VIEW, "ops", VIEW)),
                    item("/audit/trace", "全链路追踪", null, null,
                            Map.of("admin", VIEW, "ops", VIEW)),
                    item("/audit/compliance", "合规审计", null, null,
                            Map.of("ops", VIEW))
            ))
    );

    private static MenuItemDef item(String path, String label, String badge, String tag,
                                    Map<String, List<String>> actionsByRole) {
        return new MenuItemDef(path, label, badge, tag, actionsByRole);
    }

    private record MenuGroupDef(String id, String label, String icon, List<MenuItemDef> items) {
    }

    private record MenuItemDef(String path, String label, String badge, String tag,
                               Map<String, List<String>> actionsByRole) {

        /** 角色未命中时 actions 为空，表示该菜单不可见 */
        List<String> actionsFor(Set<String> roles) {
            return roles.stream()
                    .filter(actionsByRole::containsKey)
                    .flatMap(item -> actionsByRole.get(item).stream())
                    .distinct()
                    .toList();
        }
    }
}
