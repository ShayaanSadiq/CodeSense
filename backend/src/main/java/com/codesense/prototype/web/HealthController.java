package com.codesense.prototype.web;

import com.codesense.prototype.llm.LlmService;
import com.codesense.prototype.model.LlmProvider;
import com.codesense.prototype.web.dto.HealthResponse;
import com.codesense.prototype.web.dto.LlmConfigRequest;
import com.codesense.prototype.workshop.WorkshopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final LlmService llm;
    private final WorkshopService workshop;

    public HealthController(LlmService llm, WorkshopService workshop) {
        this.llm = llm;
        this.workshop = workshop;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.from(llm.publicStatus());
    }

    @GetMapping("/llm")
    public HealthResponse llm() {
        return HealthResponse.from(llm.publicStatus());
    }

    @PostMapping("/llm")
    public HealthResponse saveLlm(@RequestBody LlmConfigRequest request) {
        LlmProvider provider = LlmProvider.from(request.getProvider());
        if (provider == LlmProvider.RULES) {
            throw new IllegalArgumentException("Choose ollama, openai, gemini, or groq.");
        }
        HealthResponse status = HealthResponse.from(
                llm.save(provider, request.getModel(), request.getApiKey(), request.getBaseUrl()));
        workshop.useLlm(llm, llm.currentMode());
        return status;
    }
}
