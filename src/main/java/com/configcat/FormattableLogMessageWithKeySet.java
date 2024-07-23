package com.configcat;

import java.util.Set;
import java.util.stream.Collectors;

class FormattableLogMessageWithKeySet extends FormattableLogMessage {

    private final String message;
    private final Object[] args;

    FormattableLogMessageWithKeySet(String message, Object... args) {
        super(message,args);
        this.message = message;
        this.args = args;
    }

    @Override
    public String formatLogMessage() {
        Object keySetObject = args[args.length - 1];
        if(keySetObject instanceof Set) {
            Set<String> keySet = (Set<String>) keySetObject;
            args[args.length - 1] = keySet.stream().map(keyTo -> "'" + keyTo + "'").collect(Collectors.joining(", "));
        }
        return String.format(message, args);
    }
}
