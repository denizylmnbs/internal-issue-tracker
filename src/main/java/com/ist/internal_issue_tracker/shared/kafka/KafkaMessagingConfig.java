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

/**
 * How a domain event becomes bytes on the way out and a record again on the way in, and what happens
 * to one that cannot be processed.
 */
@Configuration
class KafkaMessagingConfig {

  /**
   * Replaces the converter Spring Modulith would otherwise register, for two reasons: which type a
   * record is read as, and which types are allowed to be read at all.
   *
   * <p>The converter writes the payload's fully-qualified class name into a {@code __TypeId__}
   * header on the way out, and reads it back on the way in to decide what to deserialise into. That
   * lookup is deliberately restricted, because a header naming an arbitrary class is a header that
   * can ask this process to instantiate one. The default trusts {@code java.util} and {@code
   * java.lang} and nothing else, so without this the events would go out fine and every one of them
   * would fail to come back.
   *
   * <p>Only {@code shared.event} is trusted. That is the whole published vocabulary, and nothing
   * else should ever arrive on these topics.
   *
   * <p>{@code TYPE_ID} rather than the default {@code INFERRED}, and this is the setting the
   * consumers do not work without. Inferred means "deserialise into whatever the handler method
   * takes", which is right for a topic carrying one type and wrong for every topic here: the
   * listeners are class-level {@code @KafkaListener}s whose handlers take three different types, and
   * a default handler taking {@code Object}. Inference resolves that to {@code Object}, Jackson
   * dutifully produces a {@code LinkedHashMap}, no typed handler matches it, and the default handler
   * drops it - a record consumed, an offset committed, and no row written. The header says what the
   * record is; on these topics it is the only thing that does.
   *
   * <p>Modulith declares its own converter {@code @ConditionalOnMissingBean}, so defining this one
   * takes it over rather than colliding with it. It also switches the Kafka-level serializers to
   * byte arrays, which is why there is no {@code JsonSerializer} here to configure - the JSON is
   * produced by this converter, not by the serializer.
   */
  @Bean
  ByteArrayJacksonJsonMessageConverter kafkaMessageConverter(ObjectProvider<JsonMapper> jsonMapper) {

    var converter = new ByteArrayJacksonJsonMessageConverter(jsonMapper.getIfUnique(JsonMapper::new));

    var typeMapper = new DefaultJacksonJavaTypeMapper();
    typeMapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.TYPE_ID);
    typeMapper.addTrustedPackages("com.ist.internal_issue_tracker.shared.event");
    converter.setTypeMapper(typeMapper);

    return converter;
  }

  /**
   * Retries a failed record a few times, then moves it aside to {@code <topic>.DLT}.
   *
   * <p>This is the part the publication registry used to handle and no longer does, and it is not
   * optional. A record that fails is retried in place, and a record that fails every time is retried
   * forever - which stops that partition, and every record queued behind it, indefinitely. Under the
   * registry a poisonous event held up only its own row. Here it would hold up a third of the topic.
   *
   * <p>Three attempts two seconds apart, then out. The failures worth retrying are the ones that
   * pass on their own - a database briefly unreachable, a lock timeout - and those clear in seconds
   * or do not clear at all. Deserialisation and conversion failures are not retried even once,
   * because a message that cannot be read will not become readable; {@code DefaultErrorHandler}
   * already classifies those as fatal, so there is nothing to configure for it.
   *
   * <p>What lands in a dead letter topic is a question, not an answer: it means an event was
   * accepted from a publisher and never written to the activity log. The topic wants watching.
   */
  @Bean
  DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> operations) {
    return new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(operations), new FixedBackOff(2000L, 3L));
  }
}
