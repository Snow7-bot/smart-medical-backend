package com.smartmedical.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Slf4j
@Component
public class DeepSeekClient {

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public DeepSeekClient(@Value("${deepseek.api.url}") String apiUrl,
                          @Value("${deepseek.api.key}") String apiKey,
                          @Value("${deepseek.model}") String model) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.mapper = new ObjectMapper();
        this.httpClient = createHttpClient();
    }

    private static HttpClient createHttpClient() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new java.security.SecureRandom());
            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("SSL初始化失败", e);
            return HttpClient.newHttpClient();
        }
    }

    public String chat(String systemPrompt, String userMessage) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.3);

            ArrayNode messages = body.putArray("messages");
            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);

            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", userMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String respBody = response.body();
            log.info("DeepSeek 响应: status={}, body={}", response.statusCode(),
                    respBody.length() > 300 ? respBody.substring(0, 300) : respBody);
            JsonNode root = mapper.readTree(respBody);

            if (root.has("error")) {
                log.error("DeepSeek API 错误: {}", root.path("error").path("message").asText());
                return "抱歉，AI问诊服务暂时不可用。";
            }

            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (IOException | InterruptedException e) {
            log.error("DeepSeek API 调用失败", e);
            return "抱歉，AI问诊服务暂时不可用，请稍后重试。";
        }
    }
}
