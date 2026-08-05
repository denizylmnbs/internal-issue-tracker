package com.ist.internal_issue_tracker.shared.cache;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Two distinct consumers share this connection: {@code @Cacheable} call-result caching (any
 * module) and {@code auth}'s refresh-token store. Only the first goes through beans declared here
 * - the second talks to Redis directly for full control over key shape and TTL per token, and gets
 * that from Spring Boot's auto-configured {@code StringRedisTemplate} rather than anything below.
 */
@Configuration
@EnableCaching
public class RedisConfig {

  /**
   * Polymorphic round-tripping through {@code Object} - what lets one cache/template hold DTOs
   * from every module and hand each one back as its original class - needs default typing turned
   * on. {@code GenericJacksonJsonRedisSerializer}'s no-arg form leaves it off precisely because
   * unrestricted default typing lets a crafted payload name an arbitrary class to instantiate on
   * deserialize; scoping the validator to this app's own base package keeps the round-trip without
   * reopening that hole.
   */
  private static PolymorphicTypeValidator typeValidator() {
    return BasicPolymorphicTypeValidator.builder()
        .allowIfSubType("com.ist.internal_issue_tracker")
        .allowIfSubType("java.util")
        .build();
  }

  /**
   * One cache name per {@code IssueMetricsService} method, kept here rather than as a constant on
   * the service so eviction policy (currently: none - see the class javadoc) stays a caching
   * concern and the service can be read without this in mind. Each metric result is expensive to
   * recompute but read-only and non-security-sensitive, so a short TTL rather than active eviction
   * is the deliberate choice: exact invalidation would need either a wildcard/SCAN delete or a
   * per-project cache region, and neither earns its complexity against "the chart is up to five
   * minutes old."
   */
  private static final List<String> METRICS_CACHE_NAMES =
      List.of(
          "metrics-cycleTime",
          "metrics-leadTime",
          "metrics-throughput",
          "metrics-timeInStatus",
          "metrics-flowEfficiency",
          "metrics-reopenRate",
          "metrics-wip",
          "metrics-netFlow",
          "metrics-throughputBreakdown",
          "metrics-defectRatio",
          "metrics-bugMttr",
          "metrics-velocity",
          "metrics-burndown",
          "metrics-cumulativeFlow");

  /**
   * Authorization-decision caches: {@code isLeaderOfProject}, {@code isParticipantOfProject} (keyed
   * {@code projectId + ':' + userId}) and {@code activeTeamIdsOfUser} (keyed by {@code userId}).
   * Every write path that can change one of these evicts the exact key(s) it affects - see {@code
   * ProjectService}, {@code TeamMemberService} and {@code ProjectParticipantCacheEvictionListener} -
   * so this TTL is a safety net for the paths that do not evict (project deletion, user/team
   * deactivation cascades), not the primary invalidation mechanism the way it is for metrics. Kept
   * far shorter than the metrics TTL because a stale {@code true} here is a stale grant, not a stale
   * chart.
   */
  private static final List<String> AUTH_CACHE_NAMES =
      List.of("project-leader", "project-participant", "user-teams");

  /**
   * Values are JSON, not JDK serialization: a Java-serialized cache entry breaks the moment the
   * cached class's shape changes, which turns a routine field rename into a startup-time
   * deserialization crash instead of a cache miss. Null results are never cached - a lookup that
   * legitimately found nothing should stay a lookup, not calcify into a stale negative.
   */
  @Bean
  public RedisCacheManager cacheManager(
      RedisConnectionFactory connectionFactory,
      @Value("${app.cache.default-ttl:PT15M}") Duration defaultTtl,
      @Value("${app.cache.metrics-ttl:PT5M}") Duration metricsTtl,
      @Value("${app.cache.auth-ttl:PT2M}") Duration authTtl) {
    RedisSerializationContext.SerializationPair<Object> valueSerialization =
        RedisSerializationContext.SerializationPair.fromSerializer(
            GenericJacksonJsonRedisSerializer.builder().enableDefaultTyping(typeValidator()).build());

    RedisCacheConfiguration configuration =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(defaultTtl)
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(valueSerialization);

    RedisCacheConfiguration metricsConfiguration = configuration.entryTtl(metricsTtl);
    RedisCacheConfiguration authConfiguration = configuration.entryTtl(authTtl);

    Map<String, RedisCacheConfiguration> overrides =
        METRICS_CACHE_NAMES.stream()
            .collect(Collectors.toMap(name -> name, name -> metricsConfiguration));
    AUTH_CACHE_NAMES.forEach(name -> overrides.put(name, authConfiguration));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(configuration)
        .withInitialCacheConfigurations(overrides)
        .build();
  }

  /**
   * For manual reads/writes outside {@code @Cacheable} - e.g. invalidating a key a mutation just
   * made stale. Serializers match {@link #cacheManager} so a key written through one is readable
   * through the other.
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    GenericJacksonJsonRedisSerializer serializer =
        GenericJacksonJsonRedisSerializer.builder().enableDefaultTyping(typeValidator()).build();

    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(serializer);
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(serializer);
    template.afterPropertiesSet();
    return template;
  }
}
