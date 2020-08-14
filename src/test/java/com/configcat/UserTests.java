package com.configcat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserTests {

    @Test
    public void builderWorksWithEmptyOrNullId() {
        User u1 = User.newBuilder().build(null);
        assertEquals("", u1.getIdentifier());
        User u2 = User.newBuilder().build("");
        assertEquals("", u2.getIdentifier());
    }

    @Test
    public void getAttributeThrowsWhenArgumentInvalid() {
        User user = User.newBuilder().build("a");
        assertThrows(IllegalArgumentException.class, () -> user.getAttribute(null));
        assertNull(user.getAttribute(""));
    }

    @Test
    public void getAttributeCaseSensitivityTest() {
        String email = "a@a.com";
        String country = "b";
        User user = User.newBuilder().email(email).country("b").build("a");
        assertEquals(email, user.getAttribute("Email"));
        assertNotEquals(email, user.getAttribute("EMAIL"));
        assertNotEquals(email, user.getAttribute("email"));

        assertEquals(country, user.getAttribute("Country"));
        assertNotEquals(country, user.getAttribute("COUNTRY"));
        assertNotEquals(country, user.getAttribute("country"));
    }
}
