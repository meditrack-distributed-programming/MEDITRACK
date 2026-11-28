package ec.edu.uce.meditrack.identity.domain;

/**
 * The sign-in is refused. The same failure covers an unknown user, a wrong password and a disabled or
 * locked account, so that a caller cannot find out which accounts exist.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
