package com.ggzed.springai.ctl;

import com.ggzed.springai.common.result.Result;
import com.ggzed.springai.model.request.RagRequest;
import com.ggzed.springai.service.HybridSearchService;
import com.ggzed.springai.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * 检索调试 API（不走 LLM，直接看召回结果）
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    /** 混合检索默认返回条数 */
    private static final int HYBRID_TOP_N = 5;

    private final RagService ragService;
    private final HybridSearchService hybridSearchService;

    /**
     * 纯向量语义检索
     */
    @PostMapping("/semantic")
    public Result<List<Document>> semantic(@RequestParam String query) {
        return Result.success(ragService.search(query));
    }

    /**
     * 混合检索（BM25 + KNN，应用层 RRF 融合）
     */
    @PostMapping("/hybrid")
    public Result<List<String>> hybrid(@RequestBody @Valid RagRequest request) throws IOException {
        return Result.success(hybridSearchService.hybridSearch(request.getQuery(), HYBRID_TOP_N));
    }
}
