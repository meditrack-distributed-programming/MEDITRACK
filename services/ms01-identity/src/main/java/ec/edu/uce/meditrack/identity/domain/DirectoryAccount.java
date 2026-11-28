package ec.edu.uce.meditrack.identity.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Account that the institutional directory has authenticated.
 *
 * @param objectGuid identifier assigned by the directory; it does not change when the account is
 *     renamed or moved
 * @param username user name as stored in the directory
 */
public record DirectoryAccount(UUID objectGuid, String username) {

    public DirectoryAccount {
        Objects.requireNonNull(objectGuid, "objectGuid");
        Objects.requireNonNull(username, "username");
    }
}
