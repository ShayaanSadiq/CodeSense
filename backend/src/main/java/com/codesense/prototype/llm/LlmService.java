package com.codesense.prototype.llm;

import com.codesense.prototype.config.CodeSenseProperties;
import com.codesense.prototype.model.LlmMode;
import com.codesense.prototype.model.LlmProvider;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LlmService implements LlmCompleter {

    private static final List<String> PREFERRED_OLLAMA =
            List.of("qwen3:8b", "qwen3:4b", "llama3.2", "llama3.1", "gemma2");

    private final CodeSenseProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private LlmMode mode = LlmMode.RULES;
    private LlmProvider provider = LlmProvider.RULES;
    private String model = "";
    private boolean ready;
    private String error = "No model connected. Start Ollama or add an API key.";
    private String completeUrl;
    private String apiKey;
    private String ollamaRoot;
    private ChatClient chatClient;
    private List<String> models = List.of();

    public LlmService(CodeSenseProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        reload();
    }

    public synchronized LlmMode currentMode() {
        return mode;
    }

    public synchronized void reload() {
        apply(resolve());
        this.models = listOllamaModels(ollamaRoot());
    }

    public synchronized HealthView publicStatus() {
        LlmConfigFile saved = readSaved();
        List<String> installed = listOllamaModels(ollamaRoot());
        this.models = installed;
        boolean hasKey = (saved != null && saved.apiKey != null && !saved.apiKey.isBlank())
                || notBlank(properties.getOpenaiApiKey())
                || notBlank(properties.getGeminiApiKey())
                || notBlank(properties.getGroqApiKey());
        return new HealthView(mode, provider, model, ready, error, hasKey, !installed.isEmpty(), installed);
    }

    public synchronized HealthView save(LlmProvider nextProvider, String nextModel, String nextKey, String nextBaseUrl) {
        if (nextProvider == LlmProvider.RULES) {
            throw new IllegalArgumentException("Choose ollama, openai, gemini, or groq.");
        }
        LlmConfigFile config = new LlmConfigFile();
        config.provider = nextProvider.value();
        config.model = safeModel(nextModel);
        config.apiKey = safeKey(nextKey);
        // Client-supplied base URLs are ignored (SSRF). Use env/server defaults only.
        config.baseUrl = null;
        writeSaved(config);
        reload();
        return publicStatus();
    }

    @Override
    public synchronized String complete(String system, String user) {
        if (!ready || chatClient == null) {
            return null;
        }
        return LlmChat.complete(chatClient, system, user);
    }

    private Resolved resolve() {
        LlmConfigFile saved = readSaved();
        List<String> installed = listOllamaModels(ollamaRoot());
        if (saved != null && "openai".equals(saved.provider) && notBlank(saved.apiKey)) {
            return cloud(LlmMode.OPENAI, LlmProvider.OPENAI,
                    first(saved.model, properties.getLlmModel(), "gpt-4o-mini"),
                    properties.getOpenaiBaseUrl(),
                    saved.apiKey);
        }
        if (saved != null && "gemini".equals(saved.provider) && notBlank(saved.apiKey)) {
            return cloud(LlmMode.GEMINI, LlmProvider.GEMINI,
                    first(saved.model, properties.getLlmModel(), "gemini-2.0-flash"),
                    null,
                    saved.apiKey);
        }
        if (saved != null && "groq".equals(saved.provider) && notBlank(saved.apiKey)) {
            return cloud(LlmMode.GROQ, LlmProvider.GROQ,
                    first(saved.model, properties.getLlmModel(), "llama-3.1-8b-instant"),
                    "https://api.groq.com/openai/v1",
                    saved.apiKey);
        }
        if (notBlank(properties.getOpenaiApiKey()) && (saved == null || "openai".equals(saved.provider))) {
            return cloud(LlmMode.OPENAI, LlmProvider.OPENAI,
                    first(properties.getLlmModel(), "gpt-4o-mini"),
                    properties.getOpenaiBaseUrl(),
                    properties.getOpenaiApiKey());
        }
        if (notBlank(properties.getGeminiApiKey()) && saved == null) {
            return cloud(LlmMode.GEMINI, LlmProvider.GEMINI,
                    first(properties.getLlmModel(), "gemini-2.0-flash"),
                    null,
                    properties.getGeminiApiKey());
        }
        if (notBlank(properties.getGroqApiKey()) && saved == null) {
            return cloud(LlmMode.GROQ, LlmProvider.GROQ,
                    first(properties.getLlmModel(), "llama-3.1-8b-instant"),
                    "https://api.groq.com/openai/v1",
                    properties.getGroqApiKey());
        }
        if (!installed.isEmpty()) {
            String requested = saved != null && "ollama".equals(saved.provider)
                    ? saved.model
                    : properties.getOllamaModel();
            return ollama(pickOllamaModel(installed, requested), ollamaRoot());
        }
        if (saved != null && "ollama".equals(saved.provider)) {
            return rules("Ollama is not running. Start it, then refresh.");
        }
        return rules("No model connected. Start Ollama or add an API key.");
    }

    private void apply(Resolved resolved) {
        this.mode = resolved.mode;
        this.provider = resolved.provider;
        this.model = resolved.model;
        this.ready = resolved.ready;
        this.error = resolved.error;
        this.completeUrl = resolved.url;
        this.apiKey = resolved.apiKey;
        this.ollamaRoot = resolved.ollamaRoot;
        this.chatClient = resolved.ready
                ? LlmChat.client(resolved.provider, resolved.model, resolved.url, resolved.apiKey, resolved.ollamaRoot)
                : null;
        if (resolved.ready && this.chatClient == null) {
            this.ready = false;
            this.error = "Spring AI could not build a ChatClient for " + resolved.provider.value() + ".";
        }
    }

    private Resolved cloud(LlmMode mode, LlmProvider provider, String model, String baseUrl, String key) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return new Resolved(mode, provider, model, true, null, null, key, ollamaRoot());
        }
        String root = httpUrl(baseUrl, "https://api.openai.com/v1");
        String url = root.replaceAll("/$", "") + "/chat/completions";
        return new Resolved(mode, provider, model, true, null, url, key, ollamaRoot());
    }

    private Resolved ollama(String model, String root) {
        return new Resolved(LlmMode.OLLAMA, LlmProvider.OLLAMA, model, true, null, null, null, root);
    }

    private Resolved rules(String message) {
        return new Resolved(LlmMode.RULES, LlmProvider.RULES, "", false, message, null, null, ollamaRoot());
    }

    private List<String> listOllamaModels(String root) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(root.replaceAll("/$", "") + "/api/tags"))
                    .timeout(Duration.ofMillis(800))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return List.of();
            }
            JsonNode modelsNode = mapper.readTree(response.body()).path("models");
            List<String> names = new ArrayList<>();
            if (modelsNode.isArray()) {
                for (JsonNode item : modelsNode) {
                    if (item.path("name").isTextual()) {
                        names.add(item.path("name").asText());
                    }
                }
            }
            return names;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String pickOllamaModel(List<String> installed, String requested) {
        if (requested != null && !requested.isBlank()) {
            for (String name : installed) {
                if (name.equals(requested) || name.startsWith(requested + ":")) {
                    return requested.contains(":") ? name : requested;
                }
            }
        }
        for (String preferred : PREFERRED_OLLAMA) {
            for (String name : installed) {
                if (name.equals(preferred) || name.startsWith(preferred + ":")) {
                    return name;
                }
            }
        }
        return installed.get(0);
    }

    private String ollamaRoot() {
        return httpUrl(properties.getOllamaBaseUrl(), "http://127.0.0.1:11434");
    }

    private static String httpUrl(String raw, String fallback) {
        String value = notBlank(raw) ? raw.trim() : fallback;
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return fallback.replaceAll("/v1/?$", "");
            }
            return value.replaceAll("/v1/?$", "");
        } catch (Exception ignored) {
            return fallback.replaceAll("/v1/?$", "");
        }
    }

    private static String safeModel(String model) {
        if (model == null || model.isBlank()) {
            return "";
        }
        String trimmed = model.trim();
        if (trimmed.length() > 80 || !trimmed.matches("[A-Za-z0-9._:/-]+")) {
            throw new IllegalArgumentException("Choose a valid model name.");
        }
        return trimmed;
    }

    private static String safeKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.length() > 256) {
            throw new IllegalArgumentException("That API key is too long.");
        }
        return trimmed;
    }

    private LlmConfigFile readSaved() {
        Path file = properties.llmConfigFile();
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return mapper.readValue(file.toFile(), LlmConfigFile.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeSaved(LlmConfigFile config) {
        try {
            Path file = properties.llmConfigFile();
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), config);
        } catch (Exception ignored) {
            throw new IllegalStateException("Could not save model settings.");
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String first(String... values) {
        for (String value : values) {
            if (notBlank(value)) {
                return value;
            }
        }
        return "";
    }

    public record HealthView(
            LlmMode mode,
            LlmProvider provider,
            String model,
            boolean ready,
            String error,
            boolean hasKey,
            boolean ollamaUp,
            List<String> models) {
    }

    public static class LlmConfigFile {
        public String provider;
        public String model;
        public String apiKey;
        public String baseUrl;
    }

    private record Resolved(
            LlmMode mode,
            LlmProvider provider,
            String model,
            boolean ready,
            String error,
            String url,
            String apiKey,
            String ollamaRoot) {
    }
}
