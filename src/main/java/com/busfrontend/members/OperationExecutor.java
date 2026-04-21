package com.busfrontend.members;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Executes an Operation by calling the existing backend REST endpoints
 * via RestTemplate on the same server. Keeps the Member-Explorer UI fully
 * integrated with the real APIs (no mocks).
 */
@Component
public class OperationExecutor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Value("${backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    public OperationExecutor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String baseUrl() {
        return backendBaseUrl;
    }

    /**
     * @param operation the operation metadata
     * @param pathId    optional path variable for {id}
     * @param formData  raw form field values (strings)
     */
    public ExecutionResult execute(Operation operation, String pathId, Map<String, String> formData) {
        String endpoint = operation.getEndpoint();
        if (endpoint.contains("{id}")) {
            if (pathId == null || pathId.isBlank()) {
                return ExecutionResult.failure("An ID is required for this operation.", "", null, 0);
            }
            endpoint = endpoint.replace("{id}", pathId.trim());
        }
        String url = baseUrl() + endpoint;
        HttpMethod httpMethod = HttpMethod.valueOf(operation.getMethod().toUpperCase());

        // For QUERY operations, append form fields as query string
        if ("QUERY".equals(operation.getInputKind())) {
            String qs = buildQueryString(formData);
            if (!qs.isEmpty()) {
                url = url + (url.contains("?") ? "&" : "?") + qs;
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.ALL));

        HttpEntity<String> entity;
        String requestBodyPretty = null;
        try {
            if (needsBody(operation.getInputKind())) {
                Map<String, Object> body = buildBody(operation.getFields(), formData);
                String json = objectMapper.writeValueAsString(body);
                requestBodyPretty = objectMapper.writeValueAsString(body);
                entity = new HttpEntity<>(json, headers);
            } else {
                entity = new HttpEntity<>(headers);
            }
        } catch (Exception ex) {
            return ExecutionResult.failure("Could not build request body: " + ex.getMessage(), "", null, 0);
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, entity, String.class);
            String pretty = prettify(response.getBody());
            return ExecutionResult.success(url, httpMethod.name(), requestBodyPretty,
                    pretty, response.getStatusCode().value());
        } catch (HttpStatusCodeException ex) {
            String pretty = prettify(ex.getResponseBodyAsString());
            return ExecutionResult.failure(
                    "Backend returned " + ex.getStatusCode().value(),
                    url, pretty, ex.getStatusCode().value());
        } catch (Exception ex) {
            return ExecutionResult.failure("Request failed: " + ex.getMessage(), url, null, 0);
        }
    }

    private boolean needsBody(String kind) {
        return "BODY".equals(kind) || "ID_AND_BODY".equals(kind);
    }

    private String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String v = e.getValue();
            if (v == null || v.isBlank()) continue;
            if (!sb.isEmpty()) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private Map<String, Object> buildBody(List<FieldDef> fields, Map<String, String> formData) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (fields == null) return body;
        for (FieldDef f : fields) {
            String raw = formData.get(f.getName());
            if (raw == null || raw.isBlank()) continue;
            body.put(f.getName(), coerce(raw, f.getValueType()));
        }
        return body;
    }

    private Object coerce(String raw, String valueType) {
        if (valueType == null) return raw;
        try {
            switch (valueType) {
                case "integer": return Integer.parseInt(raw.trim());
                case "number":  return new java.math.BigDecimal(raw.trim());
                case "integer-list":
                    List<Integer> ids = new ArrayList<>();
                    for (String s : raw.split(",")) {
                        s = s.trim();
                        if (!s.isEmpty()) ids.add(Integer.parseInt(s));
                    }
                    return ids;
                default: return raw;
            }
        } catch (NumberFormatException ex) {
            return raw; // server-side validation will catch it
        }
    }

    private String prettify(String raw) {
        if (raw == null || raw.isBlank()) return "";
        try {
            Object parsed = objectMapper.readValue(raw, Object.class);
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception ex) {
            return raw;
        }
    }

    // ---------- Result ----------

    public static class ExecutionResult {
        private final boolean success;
        private final String message;
        private final String url;
        private final String httpMethod;
        private final String requestBody;
        private final String responseBody;
        private final int statusCode;

        private ExecutionResult(boolean success, String message, String url, String httpMethod,
                                String requestBody, String responseBody, int statusCode) {
            this.success = success;
            this.message = message;
            this.url = url;
            this.httpMethod = httpMethod;
            this.requestBody = requestBody;
            this.responseBody = responseBody;
            this.statusCode = statusCode;
        }

        public static ExecutionResult success(String url, String method, String req, String resp, int status) {
            return new ExecutionResult(true, "Operation executed successfully", url, method, req, resp, status);
        }

        public static ExecutionResult failure(String message, String url, String resp, int status) {
            return new ExecutionResult(false, message, url, null, null, resp, status);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getUrl() { return url; }
        public String getHttpMethod() { return httpMethod; }
        public String getRequestBody() { return requestBody; }
        public String getResponseBody() { return responseBody; }
        public int getStatusCode() { return statusCode; }
    }
}
