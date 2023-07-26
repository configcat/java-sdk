package com.configcat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

    public void logMatch(String comparisonAttribute, String userValue, Comparator comparator, String comparisonValue, Object value) {
        entries.add("Evaluating rule: [" + comparisonAttribute + ":" + userValue + "] [" + comparator.getName() + "] [" + comparisonValue + "] => match, returning: " + value + "");
    }

    public void logMatchDate(String comparisonAttribute, double userValue, Comparator comparator, double comparisonValue, Object value) {
        entries.add("Evaluating rule: [" + comparisonAttribute + ":" + userValue + " (" + doubleToDateFormat(userValue) +")] [" + comparator.getName() + "] [" + comparisonValue + " (" + doubleToDateFormat(comparisonValue) +")] => match, returning: " + value + "");
    }

    public void logNoMatch(String comparisonAttribute, String userValue, Comparator comparator, String comparisonValue) {
        entries.add("Evaluating rule: [" + comparisonAttribute + ":" + userValue + "] [" + comparator.getName() + "] [" + comparisonValue + "] => no match");
    }

    public String logFormatError(String comparisonAttribute, String userValue, Comparator comparator, String comparisonValue, Exception exception) {
        String message = "Evaluating rule: [" + comparisonAttribute + ":" + userValue + "] [" + comparator.getName() + "] [" + comparisonValue + "] => SKIP rule. Validation error: " + exception + "";
        entries.add(message);
        return message;
    }

    public String toPrint() {
        return String.join(System.lineSeparator(), this.entries);
    }

    private static String doubleToDateFormat(double dateInDouble) {
        //TODO is this the format we want
        long dateInMillisec = (long) dateInDouble * 1000;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat.format(new Date(dateInMillisec));
    }
}
