package ec.edu.uce.meditrack.identity.adapters.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.uce.meditrack.identity.domain.Credentials;
import ec.edu.uce.meditrack.identity.domain.DirectoryAccount;
import ec.edu.uce.meditrack.identity.domain.DirectoryUnavailableException;
import ec.edu.uce.meditrack.identity.domain.InvalidCredentialsException;
import ec.edu.uce.meditrack.identity.ports.out.DirectoryPort;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Signs in against a temporary Samba domain controller built from {@code deploy/samba-ad}. It is
 * skipped when Docker is not running.
 */
@Testcontainers(disabledWithoutDocker = true)
class SambaDirectoryIntegrationTest {

    private static final String SECRETS = "/var/lib/samba/meditrack-secrets/";
    private static final String CA_IN_CONTAINER = "/var/lib/samba/private/tls/ca.crt";

    // The Testcontainers extension starts and closes this container, which the compiler cannot see
    @SuppressWarnings("resource")
    @Container
    private static final GenericContainer<?> SAMBA = new GenericContainer<>(
                    new ImageFromDockerfile("meditrack/samba-ad-test", false)
                            .withFileFromPath(".", Path.of("../../deploy/samba-ad")))
            // The certificate of the controller names this host, as in the Compose service
            .withCreateContainerCmdModifier(command -> command.withHostName("dc1"))
            .withExposedPorts(636)
            .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)));

    @TempDir
    private static Path certificates;

    private static Map<String, String> passwords;
    private static Path certificateAuthority;

    @BeforeAll
    static void readTheGeneratedSecrets() throws Exception {
        passwords = read(SECRETS + "synthetic-users.csv")
                .lines()
                .skip(1)
                .map(line -> line.split(","))
                .collect(Collectors.toMap(fields -> fields[0], fields -> fields[1]));
        certificateAuthority = certificates.resolve("directory-ca.pem");
        Files.writeString(certificateAuthority, read(CA_IN_CONTAINER));
    }

    @Test
    void authenticatesAnEnabledAccount() {
        DirectoryAccount account = signIn("physician01", passwords.get("physician01"));

        assertThat(account.username()).isEqualTo("physician01");
        assertThat(account.objectGuid()).isNotNull();
    }

    @Test
    void returnsTheSameIdentifierOnEverySignIn() {
        String password = passwords.get("patient01");

        assertThat(signIn("patient01", password).objectGuid())
                .isEqualTo(signIn("patient01", password).objectGuid());
    }

    @Test
    void rejectsAWrongPassword() {
        assertThatThrownBy(() -> signIn("physician01", "not the password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsAnUnknownUserInTheSameWay() {
        assertThatThrownBy(() -> signIn("nobody", "not the password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsADisabledAccountWithItsRealPassword() {
        assertThatThrownBy(() -> signIn("physician04", passwords.get("physician04")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsADirectoryAccountOutsideTheMediTrackBranch() throws Exception {
        String password = read(SECRETS + "administrator-password").strip();

        assertThatThrownBy(() -> signIn("Administrator", password))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void doesNotConnectToAServerWithAnUntrustedCertificate() throws Exception {
        SAMBA.execInContainer("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes",
                "-subj", "/CN=Another Authority",
                "-keyout", "/tmp/other.key", "-out", "/tmp/other.crt");
        Path otherAuthority = certificates.resolve("other-ca.pem");
        Files.writeString(otherAuthority, read("/tmp/other.crt"));
        DirectoryPort directory = adapter(otherAuthority);

        assertThatThrownBy(() -> directory.authenticate(
                        new Credentials("physician01", passwords.get("physician01"))))
                .isInstanceOf(DirectoryUnavailableException.class);
    }

    @Test
    void doesNotConnectWhenTheCertificateNamesAnotherHost() {
        // Same server and trusted authority, reached through an address missing in the certificate
        DirectoryPort directory = adapter("127.0.0.2", certificateAuthority);

        assertThatThrownBy(() -> directory.authenticate(
                        new Credentials("physician01", passwords.get("physician01"))))
                .isInstanceOf(DirectoryUnavailableException.class)
                .hasStackTraceContaining("No subject alternative");
    }

    private static DirectoryAccount signIn(String username, String password) {
        return adapter(certificateAuthority).authenticate(new Credentials(username, password));
    }

    private static DirectoryPort adapter(Path authority) {
        return adapter(SAMBA.getHost(), authority);
    }

    private static DirectoryPort adapter(String host, Path authority) {
        DirectoryProperties properties = new DirectoryProperties(
                URI.create("ldaps://" + host + ":" + SAMBA.getMappedPort(636)),
                "meditrack.local",
                "OU=MediTrack,DC=meditrack,DC=local",
                authority.toString(),
                Duration.ofSeconds(2),
                Duration.ofSeconds(3));
        LdapDirectoryConfiguration configuration = new LdapDirectoryConfiguration();
        var contextSource = configuration.directoryContextSource(properties);
        contextSource.afterPropertiesSet();
        return configuration.directoryPort(contextSource, properties);
    }

    private static String read(String file) throws IOException, InterruptedException {
        return SAMBA.execInContainer("cat", file).getStdout();
    }
}
