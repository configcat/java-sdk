package com.configcat;

class FormattableLogMessageWithUserCondition extends FormattableLogMessage {

    private final String message;
    private final Object[] args;

    FormattableLogMessageWithUserCondition(String message, Object... args) {
        super(message,args);
        this.message = message;
        this.args = args;
    }

    @Override
    public String formatLogMessage() {
        Object userConditionObject = args[0];
        if(userConditionObject instanceof UserCondition) {
            UserCondition userCondition = (UserCondition) userConditionObject;
            args[0] = EvaluateLogger.formatUserCondition(userCondition);
        }
        return String.format(message, args);
    }
}
