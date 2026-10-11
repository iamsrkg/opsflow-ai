package com.opsflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Time limits for every outgoing HTTP call (the language model and the embedding model).
 * Without them a model server that stops answering would hold a request thread for ever.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClientCustomizer outgoingCallTimeouts(
            @Value("${opsflow.ai.connect-timeout-seconds:5}") int connectSeconds,
            @Value("${opsflow.ai.read-timeout-seconds:60}") int readSeconds) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(connectSeconds))
                .withReadTimeout(Duration.ofSeconds(readSeconds));
        return builder -> builder.requestFactory(ClientHttpRequestFactories.get(settings));
    }
}
