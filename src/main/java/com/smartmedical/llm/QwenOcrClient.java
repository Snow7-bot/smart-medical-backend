package com.smartmedical.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class QwenOcrClient {

    private final ObjectMapper mapper;

    @Value("${qwen.api.key}")
    private String apiKey;

    @Value("${qwen.api.url}")
    private String apiUrl;

    @Value("${qwen.model:qwen-vl-plus}")
    private String model;

    public QwenOcrClient() {
        this.mapper = new ObjectMapper();
    }

    /** 提取图片中的文字，通过系统 curl 调用避免 Java SSL 问题 */
    public String extractText(File imageFile) throws IOException {
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String mime = getMime(imageFile.getName());

        log.info("千问OCR(curl): {}, model={}, fileSize={}", apiUrl, model, imageBytes.length);

        String body = mapper.writeValueAsString(Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "image_url", "image_url", Map.of("url", "data:" + mime + ";base64," + base64)),
                                Map.of("type", "text", "text", "请提取图片中的所有文字，只输出文字内容，不要任何解释。")
                        ))
                ),
                "max_tokens", 1000
        ));

        // 用系统 curl 调 API，绕过 Java SSL 证书问题
        ProcessBuilder pb = new ProcessBuilder(
                "curl", "-s", "-w", "\n%{http_code}",
                apiUrl,
                "-H", "Authorization: Bearer " + apiKey,
                "-H", "Content-Type: application/json",
                "-d", body,
                "--connect-timeout", "15",
                "--max-time", "30"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output;
        try {
            output = new String(p.getInputStream().readAllBytes());
            p.waitFor();
        } catch (Exception e) {
            throw new IOException("curl 执行失败: " + e.getMessage(), e);
        }

        // 解析：最后一行是 HTTP 状态码
        String[] lines = output.split("\n");
        int httpCode = 0;
        StringBuilder respBody = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i == lines.length - 1 && lines[i].matches("\\d{3}")) {
                httpCode = Integer.parseInt(lines[i]);
            } else {
                if (respBody.length() > 0) respBody.append("\n");
                respBody.append(lines[i]);
            }
        }

        if (httpCode != 200) {
            throw new IOException("千问HTTP " + httpCode + ": " + truncate(respBody.toString(), 300));
        }

        JsonNode root;
        try {
            root = mapper.readTree(respBody.toString());
        } catch (Exception e) {
            throw new IOException("千问返回非JSON: " + truncate(respBody.toString(), 200));
        }

        if (root.has("error")) {
            String errMsg = root.path("error").path("message").asText("未知");
            String errCode = root.path("error").path("code").asText("");
            throw new IOException("千问API错误[" + errCode + "]: " + errMsg);
        }

        String text = root.path("choices").get(0).path("message").path("content").asText();
        if (text == null || text.isBlank()) {
            throw new IOException("千问返回空, body: " + truncate(respBody.toString(), 200));
        }

        return text;
    }

    private String getMime(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "image/png";
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
