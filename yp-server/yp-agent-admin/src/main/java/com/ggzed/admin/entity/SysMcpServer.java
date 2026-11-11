package com.ggzed.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP Server 注册表，对应前端「MCP Server 注册」页。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yp_mcp_server")
public class SysMcpServer extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 服务名，唯一 */
    private String name;

    /** 地址，如 10.24.8.11:9090 */
    private String address;

    /** 协议，如 MCP / SSE、Dubbo / RPC */
    private String protocol;

    /** 超时配置 */
    private String timeout;

    /** 降级话术 */
    private String fallback;

    /** 在线 / 降级 */
    private String status;
}
