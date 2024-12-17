package com.configcat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FormattableLogMessageTests {

    @Test
    void hashCodeTest() {
        FormattableLogMessage obj1 = new FormattableLogMessage("message %s", "formatted");
        FormattableLogMessage obj2 = new FormattableLogMessage("message %s", "formatted");

        //test hashcode consistency
        assertEquals(obj1.hashCode(), obj1.hashCode());

        //test hashcode equality
        assertEquals(obj1.hashCode(), obj2.hashCode());

        //test hashcode distribution
        List<FormattableLogMessage> objects = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            objects.add(new FormattableLogMessage("message %s %d", "formatted", i));
        }
        Set<Integer> hashCodes = new HashSet<>();
        for (FormattableLogMessage obj : objects) {
            hashCodes.add(obj.hashCode());
        }
        assertEquals(objects.size(), hashCodes.size());
    }

    @Test
    void equalsTest() {
        FormattableLogMessage obj1 = new FormattableLogMessage("message %s", "formatted");
        FormattableLogMessage obj2 = new FormattableLogMessage("message %s", "formatted");

        // assert equals
        boolean equalsResult = obj1.equals(obj2);
        assertTrue(equalsResult);

        // assert equals reverse
        equalsResult = obj2.equals(obj1);
        assertTrue(equalsResult);

        //assert not equals
        FormattableLogMessage obj3 = new FormattableLogMessage("message %s", "different");
        equalsResult = obj1.equals(obj3);
        assertFalse(equalsResult);

        //assert null
        equalsResult = obj1.equals(null);
        assertFalse(equalsResult);
    }


}
