package com.configcat;

/**
 * Represents an exception which is being thrown when the parsing of a given json fails.
 */
public class ParsingFailedException extends Exception {
    private String json;

    /**
     * Gets the json string which was failed to parse.
     *
     * @return the json string which was failed to parse.
     */
    public String getJson() {
        return json;
    }

    /**
     * Constructs a parse exception object.
     *
     * @param message the message of the exception.
     * @param json the json string which was failed to parse.
     */
    public ParsingFailedException(String message, String json) {
        super(message);
        this.json = json;
    }
}