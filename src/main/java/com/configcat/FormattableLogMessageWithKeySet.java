package com.configcat;

import java.util.Set;
import java.util.stream.Collectors;

class FormattableLogMessageWithKeySet extends FormattableLogMessage {

    private String cachedMessage;
    private final String message;
    private final Object[] args;
    private final Set<String> keySet;

    FormattableLogMessageWithKeySet(String message, Set<String> keySet, Object... args) {
        super(message,args);
        this.message = message;
        this.args = args;
        this.keySet = keySet;
    }

    @Override
    public String formatLogMessage() {
        final int argsLength = args.length;
        Object[] newArgs = new Object[argsLength + 1];
        System.arraycopy(args, 0, newArgs, 0, argsLength);
        newArgs[argsLength + 1] = keySet.stream().map(keyTo -> "'" + keyTo + "'").collect(Collectors.joining(", "));
        return String.format(message, newArgs);
    }
}
