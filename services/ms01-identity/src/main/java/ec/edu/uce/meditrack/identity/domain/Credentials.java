package ec.edu.uce.meditrack.identity.domain;

import java.util.regex.Pattern;

/**
 * User name and password presented by somebody who wants to sign in.
 *
 * <p>Values that can never belong to an account are rejected here, before the directory is contacted.
 * An empty password matters most: a directory can treat it as an anonymous sign-in and accept it.
 *
 * <p>The password is kept only in memory and is never part of the text form of this object.
 */
public record Credentials(String username, String password) {

    // Characters and length of the sAMAccountName attribute of Active Directory
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{1,20}");
    private static final int MAX_PASSWORD_LENGTH = 128;

    public Credentials {
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new InvalidCredentialsException();
        }
        if (password == null || password.isEmpty() || password.length() > MAX_PASSWORD_LENGTH) {
            throw new InvalidCredentialsException();
        }
    }

    @Override
    public String toString() {
        return "Credentials[username=" + username + ", password=***]";
    }
}
