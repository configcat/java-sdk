package com.configcat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserTests {

    @Test
    public void builderThrowsWhenArgumentInvalid() {
        assertThrows(IllegalArgumentException.class, () -> User.newBuilder().build(null));
        assertThrows(IllegalArgumentException.class, () -> User.newBuilder().build(""));
    }

    @Test
    public void getAttributeThrowsWhenArgumentInvalid() {
        User user = User.newBuilder().build("a");
        assertThrows(IllegalArgumentException.class, () -> user.getAttribute(null));
        assertThrows(IllegalArgumentException.class, () -> user.getAttribute(""));
    }
}
