package ec.edu.uce.meditrack.identity.adapters.ldap;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Connection to the directory. Only these values change between the local Samba controller and the
 * Windows Server controller of AWS.
 *
 * @param url address of the domain controller; only {@code ldaps} is accepted
 * @param domain domain appended to the user name to sign in
 * @param baseDn branch that holds the MediTrack accounts; accounts outside it cannot sign in
 * @param caCertificate PEM file of the certificate authority to trust; when empty, only the public
 *     authorities known by Java are trusted
 * @param connectTimeout limit to open the connection, including the TLS handshake
 * @param readTimeout limit to wait for each answer
 */
@ConfigurationProperties("meditrack.directory")
public record DirectoryProperties(
        @DefaultValue("ldaps://localhost:636") URI url,
        @DefaultValue("meditrack.local") String domain,
        @DefaultValue("OU=MediTrack,DC=meditrack,DC=local") String baseDn,
        @DefaultValue("") String caCertificate,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout) {

    public DirectoryProperties {
        if (!"ldaps".equalsIgnoreCase(url.getScheme())) {
            throw new IllegalArgumentException(
                    "meditrack.directory.url must use ldaps, passwords are never sent without TLS");
        }
    }
}
