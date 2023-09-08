package com.configcat;

import java.util.List;

public class EvaluateLogger {

    private final StringBuilder stringBuilder = new StringBuilder();

    public EvaluateLogger(final String key) {
        stringBuilder.append("Evaluating '" + key + "'");
        indentLevel = 0;
    }

    private int indentLevel;

    public final void logUserObject(final User user){
        stringBuilder.append(" for User '" +user.toString()+"'");

    }

    public final void append(final String line){
        stringBuilder.append(line);
    }

    public final void increaseIndentLevel(){
        indentLevel++;
    }

    public final void decreaseIndentLevel(){
        //TODO validate it cannot be less then 0?
        indentLevel--;
    }

    public final void newLine(){

    }

    public String toPrint() {
        return stringBuilder.toString();
    }

    public void logReturnValue(String toString) {
        //TODO implement something similar
    }

    public String logFormatError(String comparisonAttribute, String userValue, Comparator comparator, List<String> inSemVerValues, Exception e) {
        //TODO implement something similar
        return "";
    }

    public String logFormatError(String comparisonAttribute, String userValue, Comparator comparator, String stringValue, Exception e) {
        //TODO implement something similar
        return "";
    }

    public String logFormatError(String comparisonAttribute, String userValue, Comparator comparator, Double doubleValue, NumberFormatException e) {
        //TODO implement something similar
        return "";
    }

    public void logPercentageEvaluationReturnValue(String toString) {
        //TODO implement something similar
    }
}
