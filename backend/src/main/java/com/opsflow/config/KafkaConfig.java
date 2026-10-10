package com.opsflow.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opsflow.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    private static final int PARTITIONS = 3;

    /**
     * What happens when a listener throws: try again twice, half a second apart, then move the
     * message to "<topic>.DLT" and carry on, so one bad message can't block the partition.
     * A message that can't be parsed goes there straight away, because retrying won't change it.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(500L, 2));
        handler.addNotRetryableExceptions(JsonProcessingException.class, IllegalArgumentException.class);
        return handler;
    }

    /** Topics are created at startup, but only when a broker is configured. */
    @Bean
    @ConditionalOnProperty(name = "opsflow.kafka.enabled", havingValue = "true")
    public KafkaAdmin.NewTopics opsflowTopics() {
        return new KafkaAdmin.NewTopics(
                topic(Topics.REQUEST), topic(Topics.REQUEST + Topics.DEAD_LETTER_SUFFIX),
                topic(Topics.INVESTIGATION), topic(Topics.INVESTIGATION + Topics.DEAD_LETTER_SUFFIX),
                topic(Topics.APPROVAL), topic(Topics.APPROVAL + Topics.DEAD_LETTER_SUFFIX));
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(1).build();
    }
}
