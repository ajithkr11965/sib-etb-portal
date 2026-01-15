package com.sib.portal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class ExternalApiService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalApiService.class);

    private final RestTemplate restTemplate;

    @Value("${step.api.mock.enabled:true}")
    private boolean mockEnabled;

    public ExternalApiService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Generic method to make GET requests
     */
    public <T> T get(String url, Class<T> responseType) {
        if (mockEnabled) {
            logger.info("Mock API Call [GET] to: {}", url);
            return null; // Mocks should be handled in Business Services, this layer just logs if mock is
                         // global
        }

        try {
            logger.info("External API Call [GET] to: {}", url);
            ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);
            return response.getBody();
        } catch (Exception e) {
            logger.error("API Error [GET] {}: {}", url, e.getMessage());
            throw new RuntimeException("External API unavailable");
        }
    }

    /**
     * Generic method to make POST requests
     */
    public <T, R> R post(String url, T requestBody, Class<R> responseType) {
        if (mockEnabled) {
            logger.info("Mock API Call [POST] to: {}", url);
            return null;
        }

        try {
            logger.info("External API Call [POST] to: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<R> response = restTemplate.postForEntity(url, entity, responseType);
            return response.getBody();
        } catch (Exception e) {
            logger.error("API Error [POST] {}: {}", url, e.getMessage());
            throw new RuntimeException("External API unavailable");
        }
    }
}
