package com.example.demo.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean
    @Qualifier("geminiRestTemplate")
    public RestTemplate geminiRestTemplate(GeminiProperties properties) {
        int normalizedTimeoutSeconds = Math.max(1, properties.getTimeoutSeconds());
        int timeoutMillis = Math.toIntExact(Duration.ofSeconds(normalizedTimeoutSeconds).toMillis());

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return new RestTemplate(requestFactory);
    }
}
