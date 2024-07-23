package com.configcat;

class FormattableLogMessage {

    private String cachedMessage;
    protected final String message;
    protected final Object[] args;

    FormattableLogMessage(String message, Object... args) {
        this.message = message;
        this.args = args;
    }

    protected String formatLogMessage(){
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
