package com.codesense.prototype.web.dto;

import com.codesense.prototype.llm.LlmService;
import com.codesense.prototype.model.LlmMode;
import com.codesense.prototype.model.LlmProvider;
import java.util.List;

public record HealthResponse(
        boolean ok,
        String scope,
        LlmMode llm,
        LlmMode mode,
        LlmProvider provider,
        String model,
        boolean ready,
        String error,
        boolean hasKey,
        boolean ollamaUp,
        List<String> models) {

    public static HealthResponse from(LlmService.HealthView view) {
        return new HealthResponse(
                true,
                "workshop",
                view.mode(),
                view.mode(),
                view.provider(),
                view.model(),
                view.ready(),
                view.error(),
                view.hasKey(),
                view.ollamaUp(),
                view.models());
    }
}
