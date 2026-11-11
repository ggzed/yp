package com.ggzed.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档表，对应前端「文档列表」页。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yp_kb_document")
public class SysKbDocument extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** PDF / WORD / CSV 等 */
    private String type;

    private String size;

    /** 切片数 */
    private Integer chunks;

    /** 已向量化 / 解析中 / 解析失败 */
    private String status;

    /** 所属知识库索引 */
    @JsonProperty("index")
    @TableField("index_name")
    private String indexName;

    /** 上传人，前端展示为 by */
    @JsonProperty("by")
    @TableField("owner")
    private String owner;
}
