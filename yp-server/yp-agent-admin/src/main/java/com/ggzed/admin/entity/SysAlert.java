package com.ggzed.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * 告警记录表，对应前端「告警提示」页。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yp_alert")
public class SysAlert extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 告警发生时间 */
    @TableField("alert_time")
    private OffsetDateTime time;

    /** P1 / P2 / P3 */
    private String level;

    private String title;

    /** 关联服务名 */
    private String service;

    private String impact;

    /** 未处理 / 处理中 / 已恢复 */
    private String state;

    /** 是否已读，用于侧边栏角标 */
    @TableField("is_read")
    private Boolean read;

    /** 认领人 */
    private String claimedBy;
}
