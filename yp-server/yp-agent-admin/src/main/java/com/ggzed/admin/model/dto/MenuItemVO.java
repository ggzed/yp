package com.ggzed.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 左侧菜单项：path 与前端路由一致，actions 表示当前角色可执行的操作。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemVO implements Serializable {

    private String path;
    private String label;
    private String badge;
    private String tag;
    private List<String> actions;
}
