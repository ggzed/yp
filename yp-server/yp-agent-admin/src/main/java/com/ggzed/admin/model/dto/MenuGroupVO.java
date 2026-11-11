package com.ggzed.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 左侧菜单分组。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuGroupVO implements Serializable {

    private String id;
    private String label;
    private String icon;
    private List<MenuItemVO> items;
}
