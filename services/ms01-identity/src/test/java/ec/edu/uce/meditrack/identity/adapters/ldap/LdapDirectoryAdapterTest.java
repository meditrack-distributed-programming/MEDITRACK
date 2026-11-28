package ec.edu.uce.meditrack.identity.adapters.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.uce.meditrack.identity.domain.Credentials;
import ec.edu.uce.meditrack.identity.domain.DirectoryUnavailableException;
import ec.edu.uce.meditrack.identity.ports.out.DirectoryPort;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Behaviour that can be checked without a directory. */
class LdapDirectoryAdapterTest {

    private static final Credentials CREDENTIALS = new Credentials("physician01", "any password");

    @Test
    void convertsTheBinaryObjectGuid() {
        byte[] stored = HexFormat.of().parseHex("33221100554477668899aabbccddeeff");

        assertThat(LdapDirectoryAdapter.toUuid(stored))
                .hasToString("00112233-4455-6677-8899-aabbccddeeff");
    }

    @Test
    void refusesAConnectionWithoutTls() {
        assertThatThrownBy(() -> properties("ldap://localhost:389"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ldaps");
    }

    @Test
    void reportsAnUnreachableDirectoryAsUnavailable() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        DirectoryPort directory = adapter("ldaps://localhost:" + closedPort);

        assertThatThrownBy(() -> directory.authenticate(CREDENTIALS))
                .isInstanceOf(DirectoryUnavailableException.class);
    }

    @Test
    void givesUpWhenTheDirectoryDoesNotAnswerInTime() throws IOException {
        // The connection is accepted by the operating system but nothing ever answers
        try (ServerSocket silent = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            DirectoryPort directory = adapter("ldaps://localhost:" + silent.getLocalPort());
            long start = System.nanoTime();

            assertThatThrownBy(() -> directory.authenticate(CREDENTIALS))
                    .isInstanceOf(DirectoryUnavailableException.class);

            // Below the login budget of five seconds
            assertThat(Duration.ofNanos(System.nanoTime() - start))
                    .isLessThan(Duration.ofSeconds(5));
        }
    }

    private static DirectoryProperties properties(String url) {
        return new DirectoryProperties(
                URI.create(url),
                "meditrack.local",
                "OU=MediTrack,DC=meditrack,DC=local",
                "",
                Duration.ofSeconds(2),
                Duration.ofSeconds(3));
    }

    private static DirectoryPort adapter(String url) {
        LdapDirectoryConfiguration configuration = new LdapDirectoryConfiguration();
        DirectoryProperties properties = properties(url);
        var contextSource = configuration.directoryContextSource(properties);
        contextSource.afterPropertiesSet();
        return configuration.directoryPort(contextSource, properties);
    }
}
