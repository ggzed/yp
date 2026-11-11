package com.ggzed.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 配置表。
 *
 * <p>字段命名按 Java 规范，JSON 层通过 {@link JsonProperty} 对齐前端 WizardForm 契约
 * （如 desc / kb / temp / topP）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "yp_sys_agent", autoResultMap = true)
public class SysAgent extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 运行时唯一标识，发布后不可随意更改 */
    private String code;

    /** 问答型 / 长链路型 */
    private String type;

    @JsonProperty("desc")
    private String description;

    private String dept;

    private String model;

    @JsonProperty("temp")
    private Double temperature;

    @JsonProperty("topP")
    private Double topP;

    private String prompt;

    @JsonProperty("kb")
    private String knowledgeBase;

    @JsonProperty("topK")
    private Integer topK;

    private Double threshold;

    private Boolean hybrid;

    private Boolean memory;

    private Integer rounds;

    /** 挂载的工具 ID 列表，以 JSON 数组存储 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String[] tools;

    /** 启用的护栏 ID 列表，以 JSON 数组存储 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String[] guards;

    /** 日 Token 配额 */
    private Integer quota;

    private String note;

    /** 草稿 / 已发布 / 已下线 */
    private String status;

    /** 负责人 */
    private String owner;

    /** 今日调用次数 */
    private Long calls;
}
