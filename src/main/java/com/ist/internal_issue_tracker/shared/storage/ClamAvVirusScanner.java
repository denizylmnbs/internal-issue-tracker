package com.ist.internal_issue_tracker.shared.storage;
// Magic Byte Content Check
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Talks to clamd over its {@code INSTREAM} command on a plain TCP socket.
 *
 * <p>No client library: the protocol is a length-prefixed chunk stream and a one-line reply, which
 * is less code than configuring a dependency would be, and it keeps the AV integration free of any
 * new Maven coordinate.
 *
 * <p>The wire format, since it is easy to get subtly wrong: send {@code zINSTREAM\0}, then for each
 * chunk a 4-byte big-endian length followed by that many bytes, then a zero-length chunk (four zero
 * bytes) to close the stream. clamd replies {@code stream: OK\0} or {@code stream: <Signature>
 * FOUND\0}. The {@code z} prefix is what makes replies NUL-terminated rather than newline-
 * terminated; both prefixes exist and mixing them up produces a reply that never arrives.
 *
 * <p>Every failure mode collapses to {@link Status#UNAVAILABLE} rather than an exception -
 * connection refused, read timeout, a half-closed socket, an unrecognized reply. The caller decides
 * what that means; see {@link VirusScanner}.
 *
 * <p>Registered only when {@code app.storage.av.enabled} is true. It defaults to true when the
 * property is missing entirely, so an incomplete configuration fails toward scanning rather than
 * silently away from it.
 */
@Component
@ConditionalOnProperty(name = "app.storage.av.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
class ClamAvVirusScanner implements VirusScanner {

  private static final byte[] INSTREAM_COMMAND = "zINSTREAM\0".getBytes(StandardCharsets.US_ASCII);
  private static final int CHUNK_SIZE = 8192;
  private static final int MAX_REPLY_BYTES = 512;

  private final String host;
  private final int port;
  private final int timeoutMillis;

  ClamAvVirusScanner(
      @Value("${app.storage.av.host}") String host,
      @Value("${app.storage.av.port}") int port,
      @Value("${app.storage.av.timeout}") Duration timeout) {
    this.host = host;
    this.port = port;
    this.timeoutMillis = (int) timeout.toMillis();
    log.info("Malware scanning enabled against clamd at {}:{}", host, port);
  }

  @Override
  public ScanResult scan(byte[] content) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), timeoutMillis);
      socket.setSoTimeout(timeoutMillis);
      sendStream(socket.getOutputStream(), content);
      return interpret(readReply(socket.getInputStream()));
    } catch (IOException e) {
      log.warn("clamd at {}:{} is unreachable; treating the scan as unavailable", host, port, e);
      return ScanResult.unavailable();
    }
  }

  private static void sendStream(OutputStream raw, byte[] content) throws IOException {
    DataOutputStream out = new DataOutputStream(raw);
    out.write(INSTREAM_COMMAND);
    for (int offset = 0; offset < content.length; offset += CHUNK_SIZE) {
      int length = Math.min(CHUNK_SIZE, content.length - offset);
      out.writeInt(length);
      out.write(content, offset, length);
    }
    out.writeInt(0);
    out.flush();
  }

  private static String readReply(InputStream in) throws IOException {
    // Bounded: an unbounded read here would let a misbehaving (or impersonated) clamd stream
    // memory into the process on every upload.
    byte[] buffer = new byte[MAX_REPLY_BYTES];
    int read = 0;
    while (read < buffer.length) {
      int next = in.read();
      if (next < 0 || next == 0) {
        break;
      }
      buffer[read++] = (byte) next;
    }
    return new String(buffer, 0, read, StandardCharsets.US_ASCII).trim();
  }

  private ScanResult interpret(String reply) {
    if (reply.endsWith("OK")) {
      return ScanResult.clean();
    }
    if (reply.endsWith("FOUND")) {
      // "stream: Eicar-Test-Signature FOUND" -> "Eicar-Test-Signature"
      int start = reply.indexOf(':');
      String signature =
          start >= 0 ? reply.substring(start + 1, reply.length() - "FOUND".length()).trim() : reply;
      return ScanResult.infected(signature);
    }
    log.warn("Unrecognized reply from clamd: {}", reply);
    return ScanResult.unavailable();
  }
}
