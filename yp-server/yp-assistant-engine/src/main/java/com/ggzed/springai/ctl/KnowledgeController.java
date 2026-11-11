package com.ggzed.springai.ctl;

import com.ggzed.springai.common.result.Result;
import com.ggzed.springai.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库管理 API
 */
@Slf4j
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final RagService ragService;

    /**
     * 初始化向量库（读取 PDF -> 切分 -> 向量化 -> 写入 ES）
     */
    @PostMapping("/init")
    public Result<String> init() {
        int count = ragService.initVectorStore();
        if (count == 0) {
            return Result.success("向量库已经初始化过了，无需重复加载。");
        }
        return Result.success("向量库初始化成功，共存入 " + count + " 个片段。");
    }

    /**
     * 读取 PDF 并切分，预览切分效果（不入库）
     */
    @PostMapping("/split_preview")
    public Result<String> splitPreview() {
        List<Document> splitDocuments = ragService.readAndSplit();

        // 打印前 3 个切片看看效果
        for (int i = 0; i < Math.min(3, splitDocuments.size()); i++) {
            log.info("\n--- 切片 {} ---\n{}", i + 1, splitDocuments.get(i).getFormattedContent());
        }

        return Result.success("PDF 切分成功，切分后碎片数: " + splitDocuments.size());
    }
}
