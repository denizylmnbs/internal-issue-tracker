package com.ist.internal_issue_tracker.shared.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

  private final ProxyManager<String> proxyManager;

  public RateLimiterService(ProxyManager<String> proxyManager) {
    this.proxyManager = proxyManager;
  }

  public boolean tryConsume(String key, Bandwidth bandwidth) {
    Supplier<BucketConfiguration> configSupplier =
        () -> BucketConfiguration.builder().addLimit(bandwidth).build();
    return proxyManager.builder().build(key, configSupplier).tryConsume(1);
  }

  // login/register: aynı IP'den dakikada 10 deneme
  public static Bandwidth perIp() {
    return Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build();
  }

  // aynı hesaba dağıtık IP'lerden dakikada 5 deneme (credential stuffing koruması)
  public static Bandwidth perAccount() {
    return Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build();
  }

  // kimliği doğrulanmış genel API kullanımı
  public static Bandwidth perUser() {
    return Bandwidth.builder().capacity(20).refillGreedy(20, Duration.ofSeconds(1)).build();
  }
}
