package com.yongquan.propertysaas.device.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongquan.propertysaas.device.domain.AccessPermissionView;
import com.yongquan.propertysaas.device.domain.DeviceConfigView;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class GenericHttpDeviceAdapter implements DeviceAdapter {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public GenericHttpDeviceAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AdapterResult syncAccessPermission(DeviceConfigView device, AccessPermissionView permission, String requestBody) {
        String endpointUrl = endpointUrl(device.configJson());
        if (endpointUrl == null || endpointUrl.isBlank()) {
            return new AdapterResult(false, null, null, "设备未配置适配器 endpointUrl", 0);
        }
        long started = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpointUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int costMs = (int) ((System.nanoTime() - started) / 1_000_000);
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            String error = success ? null : "厂商接口 HTTP " + response.statusCode();
            return new AdapterResult(success, endpointUrl, response.body(), error, costMs);
        } catch (IllegalArgumentException | IOException ex) {
            int costMs = (int) ((System.nanoTime() - started) / 1_000_000);
            return new AdapterResult(false, endpointUrl, null, ex.getMessage(), costMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            int costMs = (int) ((System.nanoTime() - started) / 1_000_000);
            return new AdapterResult(false, endpointUrl, null, "同步请求被中断", costMs);
        }
    }

    private String endpointUrl(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(configJson);
            JsonNode endpoint = node.get("endpointUrl");
            return endpoint == null || endpoint.isNull() ? null : endpoint.asText();
        } catch (IOException ex) {
            return null;
        }
    }
}
