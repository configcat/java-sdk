package com.configcat;

class FormattableLogMessage {

    private String cachedMessage;
    private final String message;
    private final Object[] args;

    FormattableLogMessage(String message, Object... args) {
        this.message = message;
        this.args = args;
    }

    public String formatLogMessage(){
        return String.format(message, args);
    }

    @Override
    public String toString() {
        if(cachedMessage == null) {
            cachedMessage = formatLogMessage();
        }
        return cachedMessage;
    }
}
