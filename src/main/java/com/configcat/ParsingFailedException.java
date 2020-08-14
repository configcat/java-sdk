package com.configcat;

class ParsingFailedException extends Exception {
    private String json;
    private Exception innerException;

    public String getJson() {
        return this.json;
    }

    public Exception getInnerException() {
        return this.innerException;
    }

    ParsingFailedException(String message, String json, Exception exception) {
        super(message);
        this.json = json;
        this.innerException = exception;
    }

    ParsingFailedException(String message, String json) {
        super(message);
        this.json = json;
    }
}