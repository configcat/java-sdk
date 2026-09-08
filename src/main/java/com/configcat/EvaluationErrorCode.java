package com.configcat;

/**
 * Specifies the possible evaluation error codes.
 */
public enum EvaluationErrorCode implements ErrorCode {

    /** An unexpected error occurred during the evaluation. */
    UNEXPECTED_ERROR(-1),

    /** No error occurred (the evaluation was successful). */
    NONE(0),

    /**
     * The evaluation failed because of an error in the config model.
     * (Most likely, invalid data was passed to the SDK via flag overrides.)
     */
    INVALID_CONFIG_MODEL(1),

    /**
     * The evaluation failed because of a type mismatch between the evaluated
     * setting value and the specified default value.
     */
    SETTING_VALUE_TYPE_MISMATCH(2),

    /** The evaluation failed because the config JSON was not available locally. */
    CONFIG_JSON_NOT_AVAILABLE(1000),

    /**
     * The evaluation failed because the key of the evaluated setting was not found in
     * the config JSON.
     */
    SETTING_KEY_MISSING(1001);

    public final int code;

    EvaluationErrorCode(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return this.code;
    }

    public static EvaluationErrorCode fromException(Throwable exception) {
        if (exception instanceof InvalidConfigModelException) {
            return INVALID_CONFIG_MODEL;
        }
        if (exception instanceof EvaluationException) {
            return SETTING_VALUE_TYPE_MISMATCH;
        }
        return UNEXPECTED_ERROR;
    }
}
