package com.ist.internal_issue_tracker.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.ist.internal_issue_tracker.shared.storage.VirusScanner.ScanResult;
import com.ist.internal_issue_tracker.shared.storage.VirusScanner.Status;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * Stands up a socket that plays clamd rather than requiring a container - the INSTREAM wire format
 * is the part most likely to be wrong, and it is fully observable from the server side.
 */
class ClamAvVirusScannerTest {

  /** What the fake clamd saw and how it replied. */
  private record Exchange(byte[] command, byte[] body) {}

  private static ScanResult scanAgainst(String reply, byte[] content, Exchange[] captured)
      throws Exception {
    try (ServerSocket server = new ServerSocket(0)) {
      CompletableFuture<Exchange> received =
          CompletableFuture.supplyAsync(
              () -> {
                try (Socket client = server.accept()) {
                  DataInputStream in = new DataInputStream(client.getInputStream());
                  byte[] command = new byte[10]; // "zINSTREAM\0"
                  in.readFully(command);

                  ByteArrayOutputStream body = new ByteArrayOutputStream();
                  int chunkLength;
                  while ((chunkLength = in.readInt()) > 0) {
                    byte[] chunk = new byte[chunkLength];
                    in.readFully(chunk);
                    body.writeBytes(chunk);
                  }

                  OutputStream out = client.getOutputStream();
                  out.write(reply.getBytes(StandardCharsets.US_ASCII));
                  out.write(0);
                  out.flush();
                  return new Exchange(command, body.toByteArray());
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });

      ScanResult result =
          new ClamAvVirusScanner("localhost", server.getLocalPort(), Duration.ofSeconds(5))
              .scan(content);
      captured[0] = received.join();
      return result;
    }
  }

  @Test
  void scan_sendsTheInstreamCommandAndTheExactBody() throws Exception {
    byte[] content = new byte[20_000]; // spans more than one 8KB chunk
    for (int i = 0; i < content.length; i++) {
      content[i] = (byte) i;
    }
    Exchange[] captured = new Exchange[1];

    scanAgainst("stream: OK", content, captured);

    assertThat(new String(captured[0].command(), StandardCharsets.US_ASCII))
        .isEqualTo("zINSTREAM\0");
    // Reassembled from the length-prefixed chunks - proves the framing round-trips intact.
    assertThat(captured[0].body()).isEqualTo(content);
  }

  @Test
  void scan_returnsClean_whenClamdAnswersOk() throws Exception {
    ScanResult result = scanAgainst("stream: OK", "harmless".getBytes(), new Exchange[1]);

    assertThat(result.status()).isEqualTo(Status.CLEAN);
    assertThat(result.signature()).isNull();
  }

  @Test
  void scan_returnsInfectedWithTheSignatureName_whenClamdAnswersFound() throws Exception {
    ScanResult result =
        scanAgainst("stream: Eicar-Test-Signature FOUND", "eicar".getBytes(), new Exchange[1]);

    assertThat(result.status()).isEqualTo(Status.INFECTED);
    assertThat(result.signature()).isEqualTo("Eicar-Test-Signature");
  }

  @Test
  void scan_returnsUnavailable_whenClamdAnswersSomethingUnrecognized() throws Exception {
    ScanResult result = scanAgainst("ERROR: out of memory", "x".getBytes(), new Exchange[1]);

    assertThat(result.status()).isEqualTo(Status.UNAVAILABLE);
  }

  @Test
  void scan_returnsUnavailableRatherThanThrowing_whenNothingIsListening() throws Exception {
    int closedPort;
    try (ServerSocket probe = new ServerSocket(0)) {
      closedPort = probe.getLocalPort();
    }

    ScanResult result =
        new ClamAvVirusScanner("localhost", closedPort, Duration.ofSeconds(1))
            .scan("x".getBytes());

    assertThat(result.status()).isEqualTo(Status.UNAVAILABLE);
  }
}
