package org.junit;

public class AssertUtil {

    // Expose a package private method
    public static String format(String message, Object expected, Object actual) {
        return Assert.format(message, expected, actual);
    }

}
