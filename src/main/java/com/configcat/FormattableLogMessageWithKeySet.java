package com.configcat;

import java.util.Set;
import java.util.stream.Collectors;

class FormattableLogMessageWithKeySet extends FormattableLogMessage {

    FormattableLogMessageWithKeySet(String message, Object... args) {
        super(message,args);
    }

    @Override
    protected String formatLogMessage() {
        Object keySetObject = args[args.length - 1];
        if(keySetObject instanceof Set) {
            Set<String> keySet = (Set<String>) keySetObject;
            args[args.length - 1] = keySet.stream().map(keyTo -> "'" + keyTo + "'").collect(Collectors.joining(", "));
        }
        return String.format(message, args);
    }
}
