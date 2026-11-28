package ec.edu.uce.meditrack.identity.adapters.ldap;

import ec.edu.uce.meditrack.identity.ports.out.DirectoryPort;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;

/** Builds the directory adapter. No connection is opened until somebody signs in. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DirectoryProperties.class)
class LdapDirectoryConfiguration {

    @Bean
    LdapContextSource directoryContextSource(DirectoryProperties properties) {
        DirectorySocketFactory.trust(properties.caCertificate());

        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(properties.url().toString());
        // Every sign-in uses its own connection, authenticated as that user
        contextSource.setPooled(false);
        contextSource.setBaseEnvironmentProperties(Map.of(
                "com.sun.jndi.ldap.connect.timeout",
                Long.toString(properties.connectTimeout().toMillis()),
                "com.sun.jndi.ldap.read.timeout",
                Long.toString(properties.readTimeout().toMillis()),
                "java.naming.ldap.factory.socket",
                DirectorySocketFactory.class.getName(),
                "java.naming.ldap.attributes.binary",
                "objectGUID"));
        return contextSource;
    }

    @Bean
    DirectoryPort directoryPort(
            LdapContextSource directoryContextSource, DirectoryProperties properties) {
        return new LdapDirectoryAdapter(directoryContextSource, properties);
    }
}
