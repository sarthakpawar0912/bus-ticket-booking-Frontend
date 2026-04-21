package com.busfrontend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Base class for REST-backed API clients. Hides RestTemplate boilerplate and
 * translates HTTP errors from the backend into a frontend-friendly
 * {@link BackendException} carrying the status + server message.
 */
public abstract class AbstractApiClient {

    protected final RestTemplate restTemplate;

    @Value("${backend.base-url}")
    protected String baseUrl;

    protected AbstractApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    protected String url(String path) { return baseUrl + path; }

    protected <T> T get(String path, Class<T> type) {
        try {
            return restTemplate.getForObject(url(path), type);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    protected <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> ref) {
        try {
            ResponseEntity<List<T>> resp = restTemplate.exchange(
                    url(path), HttpMethod.GET, null, ref);
            List<T> body = resp.getBody();
            return body != null ? body : List.of();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    protected <T, B> T post(String path, B body, Class<T> type) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            return restTemplate.postForObject(url(path), new HttpEntity<>(body, h), type);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    protected <T, B> T put(String path, B body, Class<T> type) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<T> resp = restTemplate.exchange(
                    url(path), HttpMethod.PUT, new HttpEntity<>(body, h), type);
            return resp.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    protected void delete(String path) {
        try {
            restTemplate.delete(url(path));
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    protected byte[] getBytes(String path) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setAccept(List.of(MediaType.APPLICATION_PDF, MediaType.APPLICATION_OCTET_STREAM));
            ResponseEntity<byte[]> resp = restTemplate.exchange(
                    url(path), HttpMethod.GET, new HttpEntity<>(h), byte[].class);
            byte[] body = resp.getBody();
            return body != null ? body : new byte[0];
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    private BackendException translate(Exception ex) {
        int status = 500;
        String message = ex.getMessage();
        if (ex instanceof HttpClientErrorException c) {
            status = c.getStatusCode().value();
            message = extractMessage(c.getResponseBodyAsString(), message);
        } else if (ex instanceof HttpServerErrorException s) {
            status = s.getStatusCode().value();
            message = extractMessage(s.getResponseBodyAsString(), message);
        }
        return new BackendException(status, message);
    }


     private String extractMessage(String body, String fallback) {
        if (body == null || body.isBlank()) return fallback;
        int idx = body.indexOf("\"message\":");
        if (idx < 0) return body;
        int start = body.indexOf('"', idx + 10) + 1;
        int end = body.indexOf('"', start);
        return (start > 0 && end > start) ? body.substring(start, end) : body;
    }
}
