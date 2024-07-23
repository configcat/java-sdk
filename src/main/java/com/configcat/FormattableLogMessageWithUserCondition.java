package com.configcat;

class FormattableLogMessageWithUserCondition extends FormattableLogMessage {

    private String cachedMessage;
    private final String message;
    private final Object[] args;
    private final UserCondition userCondition;

    FormattableLogMessageWithUserCondition(String message, UserCondition userCondition, Object... args) {
        super(message,args);
        this.message = message;
        this.args = args;
        this.userCondition = userCondition;
    }

    @Override
    public String formatLogMessage() {
        final int argsLength = args.length;
        Object[] newArgs = new Object[argsLength + 1];
        System.arraycopy(args, 0, newArgs, 1, argsLength);
        newArgs[0] = EvaluateLogger.formatUserCondition(userCondition);
        return String.format(message, newArgs);
    }
}
