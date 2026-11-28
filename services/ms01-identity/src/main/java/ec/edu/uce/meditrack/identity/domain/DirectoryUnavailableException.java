package ec.edu.uce.meditrack.identity.domain;

/**
 * The directory cannot answer: it is unreachable, too slow or not trusted. Nothing is known about the
 * credentials, so this is never reported as a wrong password.
 */
public class DirectoryUnavailableException extends RuntimeException {

    public DirectoryUnavailableException(Throwable cause) {
        super("Identity provider unavailable", cause);
    }
}
