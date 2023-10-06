package com.configcat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class UserAttributeHelperTest {

    private final String type;
    private final Object input;
    private final String expected;

    @Parameterized.Parameters(name
            = "{index}: Test {0} user attribute value formatter")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"datetime", "2023-09-19T11:01:35.000+0000", "1695121295"},
                {"datetime", "2023-09-19T13:01:35.000+0200", "1695121295"},
                {"datetime", "2023-09-19T11:01:35.051+0000", "1695121295.051"},
                {"datetime", "2023-09-19T13:01:35.051+0200", "1695121295.051"},
                {"double", 3d, "3.0"},
                {"double", 3.14, "3.14"},
                {"double", -1.23E-100, "-1.23E-100"},
                {"int", 3, "3"},
                {"stringlist", "a,,b,c", "[\"a\",\"\",\"b\",\"c\"]"},
        });
    }

    public UserAttributeHelperTest(String type, Object input, String expected) {
        this.type = type;
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void testUserAttributeHelperMethod() throws ParseException {
        String result;
        if ("datetime".equals(type)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            result = User.attributeValueFrom(sdf.parse((String) input));
        } else if ("double".equals(type)) {
            double doubleInput = (double) input;
            result = User.attributeValueFrom(doubleInput);
        } else if ("int".equals(type)) {
            int intInput = (int) input;
            result = User.attributeValueFrom(intInput);
        } else {
            String stringInput = (String) input;
            String[] splitInput = stringInput.split(",");
            result = User.attributeValueFrom(splitInput);
        }

        assertEquals("Formatted user attribute is not matching.", expected, result);

    }

}
