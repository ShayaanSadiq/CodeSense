package com.codesense.prototype.llm;

import com.codesense.prototype.model.LlmProvider;
import com.google.genai.Client;
import com.openai.client.OpenAIClient;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;

final class LlmChat {

    private LlmChat() {
    }

    static ChatClient client(LlmProvider provider, String model, String baseUrl, String apiKey, String ollamaRoot) {
        ChatModel chatModel = switch (provider) {
            case OLLAMA -> ollama(model, ollamaRoot);
            case OPENAI, GROQ -> openAi(model, baseUrl, apiKey);
            case GEMINI -> gemini(model, apiKey);
            default -> null;
        };
        return chatModel == null ? null : ChatClient.builder(chatModel).build();
    }

    static String complete(ChatClient client, String system, String user) {
        if (client == null) {
            return null;
        }
        try {
            String content = client.prompt()
                    .system(system == null ? "" : system)
                    .user(user == null ? "" : user)
                    .call()
                    .content();
            return content == null ? null : LlmJson.stripThink(content);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ChatModel ollama(String model, String root) {
        OllamaApi api = OllamaApi.builder().baseUrl(root).build();
        return OllamaChatModel.builder()
                .ollamaApi(api)
                .options(OllamaChatOptions.builder()
                        .model(model)
                        .temperature(0.2)
                        .disableThinking()
                        .build())
                .build();
    }

    private static ChatModel openAi(String model, String completeUrl, String apiKey) {
        String root = completeUrl == null || completeUrl.isBlank()
                ? "https://api.openai.com/v1"
                : completeUrl.replaceAll("/chat/completions$", "");
        OpenAIClient client = OpenAiSetup.setupSyncClient(
                root,
                apiKey,
                null,
                null,
                null,
                null,
                false,
                false,
                model,
                Duration.ofSeconds(90),
                1,
                null,
                null,
                ObservationRegistry.NOOP,
                null,
                List.of());
        return OpenAiChatModel.builder()
                .openAiClient(client)
                .options(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.2)
                        .build())
                .build();
    }

    private static ChatModel gemini(String model, String apiKey) {
        Client genAi = Client.builder().apiKey(apiKey).build();
        return GoogleGenAiChatModel.builder()
                .genAiClient(genAi)
                .options(GoogleGenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.2)
                        .build())
                .build();
    }
}
