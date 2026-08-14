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

  // login/register: aynı IP'den dakikada 5 deneme
  public static Bandwidth perIp() {
    return Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build();
  }

  // aynı hesaba dağıtık IP'lerden dakikada 3 deneme (credential stuffing koruması)
  public static Bandwidth perAccount() {
    return Bandwidth.builder().capacity(3).refillGreedy(3, Duration.ofMinutes(1)).build();
  }

  // kimliği doğrulanmış genel API kullanımı
  public static Bandwidth perUser() {
    return Bandwidth.builder().capacity(50).refillGreedy(50, Duration.ofSeconds(1)).build();
  }

  // Dosya yükleme: istek başına maliyet genel API'den bambaşka - AV taraması bir ağ turu, decode
  // ise onlarca MB'lık geçici raster. perUser() ile aynı kovada olsalardı tek kullanıcı saniyede
  // 50 decode tetikleyebilirdi. Bkz. ImageNormalizer sınıf Javadoc'u.
  public static Bandwidth perUpload() {
    return Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build();
  }

  public boolean tryConsume(String key, Bandwidth bandwidth) {
    Supplier<BucketConfiguration> configSupplier =
        () -> BucketConfiguration.builder().addLimit(bandwidth).build();
    return proxyManager.builder().build(key, configSupplier).tryConsume(1);
  }
}
