package com.sib.portal.config;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

/**
 * RestTemplate Configuration with SSL support and connection pooling.
 * This configuration creates a RestTemplate bean that can be autowired throughout the application.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a RestTemplate bean with SSL support and connection pooling.
     *
     * Features:
     * - SSL/TLS support with trust all certificates (for development/testing)
     * - Connection pooling for performance (max 200 total connections)
     * - Connection timeout: 5 seconds
     * - Read timeout: 10 seconds
     *
     * @return RestTemplate configured with SSL and connection pooling
     * @throws Exception if SSL context creation fails
     */
    @Bean
    public RestTemplate restTemplate() throws Exception {
        // Build SSL context that trusts all certificates
        // WARNING: Use proper certificate validation in production
        SSLContext sslContext = SSLContextBuilder
                .create()
                .loadTrustMaterial((chain, authType) -> true)  // Trust all certificates
                .build();

        // Create SSL socket factory with NoopHostnameVerifier
        // WARNING: Use proper hostname verification in production
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE
        );

        // Configure connection pooling for better performance
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);  // Maximum total connections
        connectionManager.setDefaultMaxPerRoute(20);  // Maximum connections per route

        // Build HttpClient with SSL and connection pooling
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslSocketFactory)
                .setConnectionManager(connectionManager)
                .build();

        // Create request factory with timeouts
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(5000);  // 5 seconds connection timeout
        requestFactory.setReadTimeout(10000);    // 10 seconds read timeout

        return new RestTemplate(requestFactory);
    }
}
