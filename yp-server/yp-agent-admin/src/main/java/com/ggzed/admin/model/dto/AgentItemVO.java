package com.ggzed.admin.model.dto;

import lombok.Data;

/**
 * Agent 列表项，对应前端 AgentItem 契约。
 * 与 {@link com.ggzed.admin.entity.SysAgent} 的区别是：tools 为数量而非数组，
 * by/time 为展示用字段。
 */
@Data
public class AgentItemVO {

    private String name;

    private String code;

    private String type;

    private String model;

    /** 已发布 / 草稿 / 已下线 */
    private String status;

    /** 已挂载工具数量 */
    private Integer tools;

    /** 今日调用次数 */
    private Long calls;

    /** 负责人 */
    private String by;

    /** 更新时间（ISO 字符串） */
    private String time;
}
