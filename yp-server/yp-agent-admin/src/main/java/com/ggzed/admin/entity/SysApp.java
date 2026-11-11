package com.ggzed.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用中心卡片表，对应前端「我的 Agent」页。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yp_app")
public class SysApp extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 端点标识，如 rule_qa_v3 */
    private String code;

    private String icon;

    @JsonProperty("desc")
    private String description;

    /** 今日会话数 */
    private Long calls;

    /** 平均时延，如 0.9s */
    private String latency;

    /** 满意度，如 96% */
    private String satisfaction;

    /** online / gray / offline */
    private String status;

    /** 前端路由，如 /apps/rule-qa */
    private String route;
}
