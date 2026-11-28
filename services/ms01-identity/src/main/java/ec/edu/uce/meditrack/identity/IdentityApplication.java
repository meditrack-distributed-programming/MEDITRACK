package ec.edu.uce.meditrack.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Entry point of MS-01 Identity and Access.
 *
 * <p>The default in-memory user of Spring Security is excluded because Active Directory is the only
 * password authority of MediTrack.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
