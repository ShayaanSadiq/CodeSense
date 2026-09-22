package com.codesense.prototype.llm;

import com.codesense.prototype.config.CodeSenseProperties;
import com.codesense.prototype.model.LlmMode;
import com.codesense.prototype.model.LlmProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
        config.model = nextModel == null ? "" : nextModel.trim();
        config.apiKey = nextKey == null || nextKey.isBlank() ? null : nextKey;
        config.baseUrl = nextBaseUrl == null || nextBaseUrl.isBlank() ? null : nextBaseUrl;
        writeSaved(config);
        reload();
        return publicStatus();
    }

    @Override
    public synchronized String complete(String system, String user) {
        if (!ready) {
            return null;
        }
        if (provider == LlmProvider.OLLAMA) {
            return ollamaChat(system, user);
        }
        if (provider == LlmProvider.GEMINI) {
            return geminiGenerate(system, user);
        }
        return chatCompletions(system, user, Duration.ofSeconds(20));
    }

    private Resolved resolve() {
        LlmConfigFile saved = readSaved();
        List<String> installed = listOllamaModels(ollamaRoot());
        if (saved != null && "openai".equals(saved.provider) && notBlank(saved.apiKey)) {
            return cloud(LlmMode.OPENAI, LlmProvider.OPENAI,
                    first(saved.model, properties.getLlmModel(), "gpt-4o-mini"),
                    first(saved.baseUrl, properties.getOpenaiBaseUrl()),
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
    }

    private Resolved cloud(LlmMode mode, LlmProvider provider, String model, String baseUrl, String key) {
        String url = baseUrl == null ? null : baseUrl.replaceAll("/$", "") + "/chat/completions";
        return new Resolved(mode, provider, model, true, null, url, key, ollamaRoot());
    }

    private Resolved ollama(String model, String root) {
        return new Resolved(LlmMode.OLLAMA, LlmProvider.OLLAMA, model, true, null, null, null, root);
    }

    private Resolved rules(String message) {
        return new Resolved(LlmMode.RULES, LlmProvider.RULES, "", false, message, null, null, ollamaRoot());
    }

    private String ollamaChat(String system, String user) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("stream", false);
            body.put("format", "json");
            body.put("think", false);
            ArrayNode messages = body.putArray("messages");
            messages.add(mapper.createObjectNode().put("role", "system").put("content", system));
            messages.add(mapper.createObjectNode().put("role", "user").put("content", user));
            body.putObject("options").put("temperature", 0.2);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaRoot.replaceAll("/$", "") + "/api/chat"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return null;
            }
            JsonNode json = mapper.readTree(response.body());
            JsonNode content = json.path("message").path("content");
            return content.isTextual() ? LlmJson.stripThink(content.asText()) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String chatCompletions(String system, String user, Duration timeout) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.2);
            ArrayNode messages = body.putArray("messages");
            messages.add(mapper.createObjectNode().put("role", "system").put("content", system));
            messages.add(mapper.createObjectNode().put("role", "user").put("content", user));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(completeUrl))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return null;
            }
            JsonNode json = mapper.readTree(response.body());
            JsonNode content = json.path("choices").path(0).path("message").path("content");
            return content.isTextual() ? LlmJson.stripThink(content.asText()) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String geminiGenerate(String system, String user) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + URLEncoder.encode(model, StandardCharsets.UTF_8)
                    + ":generateContent?key="
                    + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            ObjectNode body = mapper.createObjectNode();
            body.putObject("systemInstruction").putArray("parts").add(mapper.createObjectNode().put("text", system));
            body.putArray("contents").add(mapper.createObjectNode()
                    .put("role", "user")
                    .putArray("parts")
                    .add(mapper.createObjectNode().put("text", user)));
            body.putObject("generationConfig").put("temperature", 0.2).put("responseMimeType", "application/json");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return null;
            }
            JsonNode json = mapper.readTree(response.body());
            JsonNode content = json.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            return content.isTextual() ? LlmJson.stripThink(content.asText()) : null;
        } catch (Exception ignored) {
            return null;
        }
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
        String fromSaved = readSaved() != null ? readSaved().baseUrl : null;
        String raw = notBlank(fromSaved) ? fromSaved : properties.getOllamaBaseUrl();
        return raw.replaceAll("/v1/?$", "");
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
