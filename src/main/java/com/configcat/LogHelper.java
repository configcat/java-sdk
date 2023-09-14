package com.configcat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class LogHelper {

    private static final String HASHED_VALUE = "<hashed value>";
    private static final String INVALID_VALUE = "<invalid value>";

    private static final int MAX_LIST_ELEMENT = 10;

    private LogHelper(){/* prevent from instantiation*/}

    private static String formatStringListComparisonValue(String[] comparisonValue, boolean isSensitive ){
        if(comparisonValue == null){
            return INVALID_VALUE;
        }
        List<String> comparisonValues = new ArrayList<>(Arrays.asList(comparisonValue));
        comparisonValues.replaceAll(String::trim);
        comparisonValues.removeAll(Arrays.asList(null, ""));
        if(comparisonValues.isEmpty()){
            return  INVALID_VALUE;
        }
        String formattedList;
        if(isSensitive){
            String sensitivePostFix = comparisonValues.size() == 1 ? "value" : "values";
            formattedList = "<" + comparisonValues.size()+" hashed " + sensitivePostFix + ">";
        }else {
            String listPostFix = "";
            if(comparisonValues.size() > MAX_LIST_ELEMENT){
                int count = comparisonValues.size() - MAX_LIST_ELEMENT;
                String countPostFix = count == 1 ? "value" : "values";
                listPostFix = "... <" + count +" more " + countPostFix+">";
            }
            List<String> subList = comparisonValues.subList(0, Math.min(MAX_LIST_ELEMENT, comparisonValues.size()));
            StringBuilder formatListBuilder = new StringBuilder();
            subList.forEach(s -> formatListBuilder.append("'").append(s).append("', "));
            formatListBuilder.append(listPostFix);
            formattedList = formatListBuilder.toString();
        }

        return "[" + formattedList + "]";
    }

    private static String formatStringComparisonValue(String comparisonValue, boolean isSensitive){
        return isSensitive ? HASHED_VALUE : comparisonValue;
    }

    private static String formatDoubleComparisonValue(Double comparisonValue, boolean isDate){
        if(comparisonValue == null){
            return INVALID_VALUE;
        }
        if(isDate){
            return comparisonValue + " (" + DateTimeUtils.doubleToFormattedUTC(comparisonValue) + " UTC)";
        }
        return comparisonValue.toString();
    }

    public static String formatUserCondition(UserCondition userCondition){
        Comparator userComparator = Comparator.fromId(userCondition.getComparator());
        String comparisonValue;
        switch (userComparator){
            case CONTAINS_ANY_OF:
            case NOT_CONTAINS_ANY_OF:
            case SEMVER_IS_ONE_OF:
            case SEMVER_IS_NOT_ONE_OF:
                comparisonValue = formatStringListComparisonValue(userCondition.getStringArrayValue(), false);
                break;
            case SEMVER_LESS:
            case SEMVER_LESS_EQULAS:
            case SEMVER_GREATER:
            case SEMVER_GREATER_EQUALS:
                comparisonValue = formatStringComparisonValue(userCondition.getStringValue(), false);
                break;
            case NUMBER_EQUALS:
            case NUMBER_NOT_EQUALS:
            case NUMBER_LESS:
            case NUMBER_LESS_EQUALS:
            case NUMBER_GREATER:
            case NUMBER_GREATER_EQUALS:
                comparisonValue = formatDoubleComparisonValue(userCondition.getDoubleValue(), false);
                break;
            case SENSITIVE_IS_ONE_OF:
            case SENSITIVE_IS_NOT_ONE_OF:
            case HASHED_STARTS_WITH:
            case HASHED_NOT_STARTS_WITH:
            case HASHED_ENDS_WITH:
            case HASHED_NOT_ENDS_WITH:
            case HASHED_ARRAY_CONTAINS:
            case HASHED_ARRAY_NOT_CONTAINS:
                comparisonValue = formatStringListComparisonValue(userCondition.getStringArrayValue(), true);
                break;
            case DATE_BEFORE:
            case DATE_AFTER:
                comparisonValue = formatDoubleComparisonValue(userCondition.getDoubleValue(), true);
                break;
            case HASHED_EQUALS:
            case HASHED_NOT_EQUALS:
                comparisonValue = formatStringComparisonValue(userCondition.getStringValue(), true);
                break;
            default:
                comparisonValue = INVALID_VALUE;
        }

        return "User." + userCondition.getComparisonAttribute() + " " + userComparator.getName() + " '" + comparisonValue + "'";
    }

    public static String formatPrerequisiteFlagCondition(PrerequisiteFlagCondition prerequisiteFlagCondition){
        String prerequisiteFlagKey = prerequisiteFlagCondition.getPrerequisiteFlagKey();
        PrerequisiteComparator prerequisiteComparator = PrerequisiteComparator.fromId(prerequisiteFlagCondition.getPrerequisiteComparator());
        String prerequisiteValue = prerequisiteFlagCondition.getValue().toString();
        String comparisonValue = prerequisiteValue ==  null ? INVALID_VALUE : prerequisiteValue;
        return "Flag '"+prerequisiteFlagKey+"' "+prerequisiteComparator.getName()+" '"+comparisonValue+"'";
    }


    public static String formatCircularDependencyList(List<String> visitedKeys, String key){
        StringBuilder builder = new StringBuilder();
        visitedKeys.forEach((visitedKey) -> builder.append("'").append(visitedKey).append("' -> "));
        builder.append("'").append(key).append("'");
        return builder.toString();
    }
}
