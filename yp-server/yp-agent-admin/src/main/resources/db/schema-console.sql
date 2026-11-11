-- 控制台菜单基础 CRUD 表
-- 对应实体：SysAlert / SysApp / SysTool / SysMcpServer / SysKbDocument

-- 告警提示
CREATE TABLE IF NOT EXISTS yp_alert (
    id          BIGSERIAL PRIMARY KEY,
    alert_time  TIMESTAMPTZ,
    level       VARCHAR(8),                 -- P1 / P2 / P3
    title       VARCHAR(255),
    service     VARCHAR(64),
    impact      TEXT,
    state       VARCHAR(16)  DEFAULT '未处理', -- 未处理 / 处理中 / 已恢复
    is_read     BOOLEAN      DEFAULT FALSE,
    claimed_by  VARCHAR(64),
    create_time TIMESTAMPTZ,
    update_time TIMESTAMPTZ,
    is_deleted  SMALLINT     DEFAULT 0
);

-- 我的 Agent（应用卡片）
CREATE TABLE IF NOT EXISTS yp_app (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    code         VARCHAR(64)  NOT NULL UNIQUE, -- 端点标识
    icon         VARCHAR(64),
    description  TEXT,
    calls        BIGINT       DEFAULT 0,
    latency      VARCHAR(32),
    satisfaction VARCHAR(16),
    status       VARCHAR(16),                  -- online / gray / offline
    route        VARCHAR(128),                 -- 前端路由
    create_time  TIMESTAMPTZ,
    update_time  TIMESTAMPTZ,
    is_deleted   SMALLINT     DEFAULT 0
);

-- 工具分类目录
CREATE TABLE IF NOT EXISTS yp_tool (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    cat         VARCHAR(32),                   -- 交易类 / 客户类 / 规则类 / 外部数据
    proto       VARCHAR(16),                   -- RPC / MCP
    status      VARCHAR(16),                   -- 在线 / 降级
    timeout     VARCHAR(32),
    latency     VARCHAR(32),
    risk        BOOLEAN      DEFAULT FALSE,
    create_time TIMESTAMPTZ,
    update_time TIMESTAMPTZ,
    is_deleted  SMALLINT     DEFAULT 0
);

-- MCP Server 注册
CREATE TABLE IF NOT EXISTS yp_mcp_server (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL UNIQUE,
    address     VARCHAR(255),
    protocol    VARCHAR(64),
    timeout     VARCHAR(32),
    fallback    TEXT,
    status      VARCHAR(16),                   -- 在线 / 降级
    create_time TIMESTAMPTZ,
    update_time TIMESTAMPTZ,
    is_deleted  SMALLINT     DEFAULT 0
);

-- 知识库文档
CREATE TABLE IF NOT EXISTS yp_kb_document (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(16),                   -- PDF / WORD / CSV
    size        VARCHAR(32),
    chunks      INTEGER      DEFAULT 0,
    status      VARCHAR(16)  DEFAULT '解析中',  -- 已向量化 / 解析中 / 解析失败
    index_name  VARCHAR(64),
    owner       VARCHAR(64),
    create_time TIMESTAMPTZ,
    update_time TIMESTAMPTZ,
    is_deleted  SMALLINT     DEFAULT 0
);
