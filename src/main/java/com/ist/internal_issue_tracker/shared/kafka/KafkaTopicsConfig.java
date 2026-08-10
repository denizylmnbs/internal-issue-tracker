package com.ist.internal_issue_tracker.shared.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topics are declared here rather than created on demand - the broker runs with
 * {@code KAFKA_AUTO_CREATE_TOPICS_ENABLE=false}, so a misspelled name fails loudly instead of
 * quietly producing into a new empty topic.
 *
 * <p>Three partitions each, keyed on the aggregate id. Partitions can be added later but never
 * removed, and adding them re-maps every key.
 */
@Configuration
class KafkaTopicsConfig {

  static final String ISSUE_EVENTS = "issue-events";
  static final String SPRINT_EVENTS = "sprint-events";
  static final String PROJECT_EVENTS = "project-events";

  /** What DeadLetterPublishingRecoverer appends by default. It was ".DLT" before Spring Kafka 3. */
  private static final String DLT_SUFFIX = "-dlt";

  private static final int PARTITIONS = 3;
  private static final short REPLICAS = 1; // single-node broker; a development setting only

  @Bean
  NewTopic issueEventsTopic() {
    return TopicBuilder.name(ISSUE_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
  }

  @Bean
  NewTopic sprintEventsTopic() {
    return TopicBuilder.name(SPRINT_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
  }

  @Bean
  NewTopic projectEventsTopic() {
    return TopicBuilder.name(PROJECT_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
  }

  // Same partition count as the topic each one shadows: the recoverer writes a failed record to the
  // partition it came from, so a single-partition DLT would have nowhere to put partition 1 or 2.

  @Bean
  NewTopic issueEventsDlt() {
    return TopicBuilder.name(ISSUE_EVENTS + DLT_SUFFIX)
        .partitions(PARTITIONS)
        .replicas(REPLICAS)
        .build();
  }

  @Bean
  NewTopic sprintEventsDlt() {
    return TopicBuilder.name(SPRINT_EVENTS + DLT_SUFFIX)
        .partitions(PARTITIONS)
        .replicas(REPLICAS)
        .build();
  }

  @Bean
  NewTopic projectEventsDlt() {
    return TopicBuilder.name(PROJECT_EVENTS + DLT_SUFFIX)
        .partitions(PARTITIONS)
        .replicas(REPLICAS)
        .build();
  }
}
