package com.smartmedical.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class QwenOcrClient {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    @Value("${qwen.api.key}")
    private String apiKey;

    @Value("${qwen.api.url}")
    private String apiUrl;

    @Value("${qwen.model:qwen-vl-plus}")
    private String model;

    public QwenOcrClient() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
        this.mapper = new ObjectMapper();
    }

    /** 提取图片中的文字 */
    public String extractText(File imageFile) {
        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String mime = getMime(imageFile.getName());

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", List.of(
                                    Map.of("type", "image_url", "image_url", Map.of("url", "data:" + mime + ";base64," + base64)),
                                    Map.of("type", "text", "text", "请提取图片中的所有文字，只输出文字内容，不要任何解释。")
                            ))
                    ),
                    "max_tokens", 1000
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                log.warn("千问OCR失败: status={}, body={}", resp.statusCode(), resp.body());
                return null;
            }

            JsonNode root = mapper.readTree(resp.body());
            String text = root.path("choices").get(0).path("message").path("content").asText();
            return text != null && !text.isBlank() ? text : null;

        } catch (Exception e) {
            log.warn("千问OCR异常: {}", e.getMessage());
            return null;
        }
    }

    private String getMime(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "image/png";
    }
}
