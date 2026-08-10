package com.ist.internal_issue_tracker.shared.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * The topics this application publishes to and reads back, declared so that {@code KafkaAdmin}
 * creates them on startup.
 *
 * <p>They have to be declared somewhere, because the broker is configured with {@code
 * KAFKA_AUTO_CREATE_TOPICS_ENABLE=false} - see compose.yaml. That is the safer setting: with
 * auto-creation on, a misspelled topic name is answered with a brand new empty topic and the mistake
 * only shows up as a consumer that never receives anything. With it off, the producer is told the
 * topic does not exist, the publication stays in {@code event_publication}, and the failure is loud.
 *
 * <p>The names below are repeated in two other places - the {@code @Externalized} routing on the
 * records in {@code shared.event}, and the {@code @KafkaListener} declarations in {@code activity} -
 * where they stay literal, because half of a routing string is a SpEL expression and splitting it
 * would cost more in readability than the duplication costs in risk. The duplication is safe to
 * carry: with auto-creation off, a side that names a topic nobody declared fails where it stands
 * rather than quietly talking to itself. The constants here are for this class and its dead letter
 * names only.
 *
 * <p>Three partitions each, keyed on the aggregate id, so events about one issue - or one sprint, or
 * one project - are ordered with respect to each other and unordered with respect to everything
 * else, which is the only ordering anything here needs. Partitions can be added to a topic later but
 * never removed, and adding them re-maps every key, so this number is not one to change casually.
 *
 * <p>Replication factor 1 because the broker is a single node. That is a development setting and
 * nothing else: one broker means one copy, and a lost disk is a lost topic.
 */
@Configuration
class KafkaTopicsConfig {

  static final String ISSUE_EVENTS = "issue-events";
  static final String SPRINT_EVENTS = "sprint-events";
  static final String PROJECT_EVENTS = "project-events";

  private static final int PARTITIONS = 3;
  private static final short REPLICAS = 1;

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

  /**
   * The suffix {@code DeadLetterPublishingRecoverer} appends by default. Spelled out here because it
   * is the one string in this class nothing else in the codebase chooses: get it wrong and the
   * topics below are created, sit empty, and the recoverer quietly writes somewhere else - or fails
   * to, and the record it was setting aside keeps blocking its partition.
   */
  private static final String DLT_SUFFIX = "-dlt";

  /**
   * Where a record goes when its listener has failed every retry - see {@link KafkaMessagingConfig}.
   *
   * <p>Same partition count as the topic each one shadows, and not fewer. The recoverer sends a
   * failed record to the partition it came from, so a dead letter topic with one partition would
   * have nowhere to put anything that failed on partition 1 or 2.
   */
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
