package com.ist.internal_issue_tracker.shared.kafka;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.ByteArrayJacksonJsonMessageConverter;
import org.springframework.kafka.support.mapping.DefaultJacksonJavaTypeMapper;
import org.springframework.kafka.support.mapping.JacksonJavaTypeMapper;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.json.JsonMapper;

@Configuration
class KafkaMessagingConfig {

  /**
   * Takes over the converter Modulith registers {@code @ConditionalOnMissingBean}. Both settings
   * below are load-bearing.
   */
  @Bean
  ByteArrayJacksonJsonMessageConverter kafkaMessageConverter(
      ObjectProvider<JsonMapper> jsonMapper) {

    var converter =
        new ByteArrayJacksonJsonMessageConverter(jsonMapper.getIfUnique(JsonMapper::new));
    var typeMapper = new DefaultJacksonJavaTypeMapper();

    // TYPE_ID, not the default INFERRED. Each topic carries several event types, so the type has to
    // come from the __TypeId__ header. Inferred, a class-level listener resolves to Object, Jackson
    // returns a LinkedHashMap, no typed handler matches, and the record is consumed and dropped.
    typeMapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.TYPE_ID);

    // Deserialising whatever class a header names is how a topic gets to instantiate anything it
    // likes. Default trusts java.util and java.lang only, which would reject every event here.
    typeMapper.addTrustedPackages("com.ist.internal_issue_tracker.shared.event");

    converter.setTypeMapper(typeMapper);
    return converter;
  }

  /**
   * Three attempts, then the record goes to {@code <topic>-dlt}. Without a recoverer a record that
   * always fails is retried forever and stops its partition - under the publication registry the
   * same event held up only its own row. Deserialisation failures are not retried at all; {@code
   * DefaultErrorHandler} already treats them as fatal.
   */
  @Bean
  DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> operations) {
    return new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(operations), new FixedBackOff(2000L, 3L));
  }
}
