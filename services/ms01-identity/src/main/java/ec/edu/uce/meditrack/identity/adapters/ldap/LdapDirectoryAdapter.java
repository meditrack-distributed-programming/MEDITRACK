package ec.edu.uce.meditrack.identity.adapters.ldap;

import ec.edu.uce.meditrack.identity.domain.Credentials;
import ec.edu.uce.meditrack.identity.domain.DirectoryAccount;
import ec.edu.uce.meditrack.identity.domain.DirectoryUnavailableException;
import ec.edu.uce.meditrack.identity.domain.InvalidCredentialsException;
import ec.edu.uce.meditrack.identity.ports.out.DirectoryPort;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.support.LdapUtils;

/**
 * Authenticates against Active Directory: signs in (LDAP bind) with the credentials of the user and
 * then reads the account of that user with the same connection. No service account is needed.
 */
class LdapDirectoryAdapter implements DirectoryPort {

    // Flags of the userAccountControl attribute
    private static final int ACCOUNT_DISABLED = 0x0002;
    private static final int LOCKED_OUT = 0x0010;

    // The user name is passed as an argument, so the directory API escapes it
    private static final String ACCOUNT_FILTER = "(&(objectClass=user)(sAMAccountName={0}))";

    private final ContextSource contextSource;
    private final DirectoryProperties properties;

    LdapDirectoryAdapter(ContextSource contextSource, DirectoryProperties properties) {
        this.contextSource = contextSource;
        this.properties = properties;
    }

    @Override
    public DirectoryAccount authenticate(Credentials credentials) {
        DirContext context = null;
        try {
            context = contextSource.getContext(
                    credentials.username() + "@" + properties.domain(), credentials.password());
            return readAccount(context, credentials.username());
        } catch (org.springframework.ldap.AuthenticationException e) {
            // The cause is dropped on purpose: it tells why the directory refused the account
            throw new InvalidCredentialsException();
        } catch (org.springframework.ldap.NamingException | javax.naming.NamingException e) {
            throw new DirectoryUnavailableException(e);
        } finally {
            LdapUtils.closeContext(context);
        }
    }

    private DirectoryAccount readAccount(DirContext context, String username)
            throws javax.naming.NamingException {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setCountLimit(1);
        controls.setTimeLimit(Math.toIntExact(properties.readTimeout().toMillis()));
        controls.setReturningAttributes(
                new String[] {"sAMAccountName", "objectGUID", "userAccountControl"});

        NamingEnumeration<SearchResult> results = context.search(
                properties.baseDn(), ACCOUNT_FILTER, new Object[] {username}, controls);
        try {
            // A valid directory account outside the MediTrack branch is not a MediTrack account
            if (!results.hasMore()) {
                throw new InvalidCredentialsException();
            }
            Attributes account = results.next().getAttributes();
            int flags = Integer.parseInt((String) account.get("userAccountControl").get());
            if ((flags & (ACCOUNT_DISABLED | LOCKED_OUT)) != 0) {
                throw new InvalidCredentialsException();
            }
            return new DirectoryAccount(
                    toUuid((byte[]) account.get("objectGUID").get()),
                    (String) account.get("sAMAccountName").get());
        } finally {
            results.close();
        }
    }

    /**
     * Converts the binary objectGUID. Active Directory stores its first three groups in little-endian
     * order and the last eight bytes as they are written.
     */
    static UUID toUuid(byte[] objectGuid) {
        ByteBuffer bytes = ByteBuffer.wrap(objectGuid).order(ByteOrder.LITTLE_ENDIAN);
        long first = Integer.toUnsignedLong(bytes.getInt());
        long second = Short.toUnsignedLong(bytes.getShort());
        long third = Short.toUnsignedLong(bytes.getShort());
        long mostSignificant = first << 32 | second << 16 | third;
        long leastSignificant = bytes.order(ByteOrder.BIG_ENDIAN).getLong();
        return new UUID(mostSignificant, leastSignificant);
    }
}
