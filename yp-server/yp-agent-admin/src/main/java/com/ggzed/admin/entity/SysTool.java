package com.ggzed.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具目录表，对应前端「工具分类目录」页。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yp_tool")
public class SysTool extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 工具唯一编码 */
    private String code;

    /** 分类，如 交易类 / 客户类 / 规则类 / 外部数据 */
    private String cat;

    /** RPC / MCP */
    private String proto;

    /** 在线 / 降级 */
    private String status;

    /** 超时配置，如 800ms */
    private String timeout;

    /** 实测耗时，如 86ms */
    private String latency;

    /** 是否高危写操作 */
    private Boolean risk;
}
