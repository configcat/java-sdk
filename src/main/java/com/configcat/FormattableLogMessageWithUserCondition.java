package com.configcat;

class FormattableLogMessageWithUserCondition extends FormattableLogMessage {

    FormattableLogMessageWithUserCondition(String message, Object... args) {
        super(message,args);
    }

    @Override
    protected String formatLogMessage() {
        Object userConditionObject = args[0];
        if(userConditionObject instanceof UserCondition) {
            UserCondition userCondition = (UserCondition) userConditionObject;
            args[0] = EvaluateLogger.formatUserCondition(userCondition);
        }
        return String.format(message, args);
    }
}
