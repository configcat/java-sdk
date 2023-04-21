package com.configcat;

import java.util.ArrayList;
import java.util.List;

public class EvaluateLogger {

    public EvaluateLogger(final String key) {
        entries.add("Evaluating getValue(" + key + ").");
    }

    private final List<String> entries = new ArrayList<>();

    public void logReturnValue(String value) {
        entries.add("Returning " + value + ".");
    }

    public void logPercentageEvaluationReturnValue(String value) {
        entries.add("Evaluating % options. Returning " + value + ".");
    }

    public void logUserObject(User user) {
        entries.add("User object: " + user + ".");
    }

    public void logMatch(String comparisonAttribute, String userValue, int comparator, String comparisonValue, Object value) {
        entries.add("Evaluating rule: [" + comparisonAttribute + ":" + userValue + "] [" + RolloutEvaluator.COMPARATOR_TEXTS[comparator] + "] [" + comparisonValue + "] => match, returning: " + value + "");
    }

    public void logNoMatch(String comparisonAttribute, String userValue, int comparator, String comparisonValue) {
        entries.add("Evaluating rule: [" + comparisonAttribute + ":" + userValue + "] [" + RolloutEvaluator.COMPARATOR_TEXTS[comparator] + "] [" + comparisonValue + "] => no match");
    }

    public String logFormatError(String comparisonAttribute, String userValue, int comparator, String comparisonValue, Exception exception) {
        String message = "Evaluating rule: [" + comparisonAttribute + ":" + userValue + "] [" + RolloutEvaluator.COMPARATOR_TEXTS[comparator] + "] [" + comparisonValue + "] => SKIP rule. Validation error: " + exception + "";
        entries.add(message);
        return message;
    }

    public String toPrint() {
        return String.join(System.lineSeparator(), this.entries);
    }
}
