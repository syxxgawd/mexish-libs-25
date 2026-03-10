

package net.mexish.libs.jdiff;

/**
 * An exception that indicates a malformed bsdiff header.
 *
 * @author malensek
 */

public class InvalidHeaderException extends Exception {

    private static final long serialVersionUID = -3712364093810940826L;

    public InvalidHeaderException() {
        super();
    }

    public InvalidHeaderException(String detail) {
        super(detail);
    }

    /**
     * Creates an InvalidHeaderException with details about the invalid field
     * that was set, and its value.
     *
     * @param fieldName invalid field name
     * @param value the value of the invalid field
     */
    public InvalidHeaderException(String fieldName, long value) {
        super("Invalid header field; " + fieldName + " = " + value);
    }
}

