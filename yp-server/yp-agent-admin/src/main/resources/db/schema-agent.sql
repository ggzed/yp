-- Agent 配置表
-- 对应实体 com.ggzed.admin.entity.SysAgent
CREATE TABLE IF NOT EXISTS yp_sys_agent (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128)  NOT NULL,
    code            VARCHAR(64)   NOT NULL UNIQUE,
    type            VARCHAR(32),
    description     TEXT,
    dept            VARCHAR(64),
    model           VARCHAR(64),
    temperature     DOUBLE PRECISION,
    top_p           DOUBLE PRECISION,
    prompt          TEXT,
    knowledge_base  VARCHAR(64),
    top_k           INTEGER,
    threshold       DOUBLE PRECISION,
    hybrid          BOOLEAN,
    memory          BOOLEAN,
    rounds          INTEGER,
    tools           TEXT,            -- JSON 数组字符串，如 ["query_account","transfer"]
    guards          TEXT,            -- JSON 数组字符串，如 ["block","audit"]
    quota           INTEGER,
    note            TEXT,
    status          VARCHAR(16)   DEFAULT '草稿',
    owner           VARCHAR(64),
    calls           BIGINT        DEFAULT 0,
    create_time     TIMESTAMPTZ,
    update_time     TIMESTAMPTZ,
    is_deleted      SMALLINT      DEFAULT 0
);

COMMENT ON TABLE yp_sys_agent IS 'Agent 配置表';
COMMENT ON COLUMN yp_sys_agent.tools IS '挂载的工具 ID 列表（JSON 数组）';
COMMENT ON COLUMN yp_sys_agent.guards IS '启用的护栏 ID 列表（JSON 数组）';
COMMENT ON COLUMN yp_sys_agent.status IS '草稿 / 已发布 / 已下线';
