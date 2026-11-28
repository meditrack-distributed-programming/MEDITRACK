package ec.edu.uce.meditrack.identity.ports.out;

import ec.edu.uce.meditrack.identity.domain.Credentials;
import ec.edu.uce.meditrack.identity.domain.DirectoryAccount;
import ec.edu.uce.meditrack.identity.domain.DirectoryUnavailableException;
import ec.edu.uce.meditrack.identity.domain.InvalidCredentialsException;

/** The institutional directory, which is the only authority on passwords. */
public interface DirectoryPort {

    /**
     * Checks the credentials against the directory.
     *
     * @return the authenticated account
     * @throws InvalidCredentialsException if the sign-in is refused
     * @throws DirectoryUnavailableException if the directory cannot answer
     */
    DirectoryAccount authenticate(Credentials credentials);
}
