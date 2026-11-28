package ec.edu.uce.meditrack.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CredentialsTest {

    @Test
    void acceptsAnAccountNameAndAPassword() {
        Credentials credentials = new Credentials("physician01", "any password");

        assertThat(credentials.username()).isEqualTo("physician01");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "user with spaces",
        "*",
        "admin)(objectClass=*",
        "physician01@meditrack.local",
        "a-name-longer-than-twenty-characters"
    })
    void rejectsValuesThatCannotBeAccountNames(String username) {
        assertThatThrownBy(() -> new Credentials(username, "any password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsAMissingPasswordBeforeTheDirectoryIsContacted(String password) {
        assertThatThrownBy(() -> new Credentials("physician01", password))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsAnOversizedPassword() {
        assertThatThrownBy(() -> new Credentials("physician01", "x".repeat(129)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void textFormNeverContainsThePassword() {
        Credentials credentials = new Credentials("physician01", "Secret-Value-1");

        assertThat(credentials.toString())
                .contains("physician01")
                .doesNotContain("Secret-Value-1");
    }
}
