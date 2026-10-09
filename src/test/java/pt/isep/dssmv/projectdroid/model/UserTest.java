package pt.isep.dssmv.projectdroid.model;

import org.junit.jupiter.api.Test;
import pt.isep.dssmv.projectdroid.exceptions.InvalidDataException;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    public void testValidUserCreation() throws InvalidDataException {
        User user = new User("u123", "Andre Silva", "andre@isep.ipp.pt");

        assertEquals("u123", user.getUserId());
        assertEquals("Andre Silva", user.getName());
        assertEquals("andre@isep.ipp.pt", user.getEmail());
        assertNotNull(user.getRegistrationDate());
    }

    @Test
    public void testEmptyUserIdThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new User("", "Andre", "andre@test.com");
        });
    }

    @Test
    public void testShortNameThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new User("u123", "A", "andre@test.com");
        });
    }

    @Test
    public void testInvalidEmailThrowsException() {
        assertThrows(InvalidDataException.class, () -> {
            new User("u123", "Andre", "email-sem-arroba");
        });
    }

    @Test
    public void testToStringContainsNameAndEmail() throws InvalidDataException {
        User user = new User("u123", "Andre", "andre@test.com");
        assertEquals("Andre (andre@test.com)", user.toString());
    }
}
