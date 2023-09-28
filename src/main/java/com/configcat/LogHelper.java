package com.configcat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class LogHelper {

    private static final String HASHED_VALUE = "<hashed value>";
    public static final String INVALID_VALUE = "<invalid value>";
    public static final String INVALID_NAME = "<invalid name>";
    public static final String INVALID_REFERENCE = "<invalid reference>";

    private static final int MAX_LIST_ELEMENT = 10;

    private LogHelper(){/* prevent from instantiation*/}

    private static String formatStringListComparisonValue(String[] comparisonValue, boolean isSensitive ){
        if(comparisonValue == null){
            return INVALID_VALUE;
        }
        List<String> comparisonValues = new ArrayList<>(Arrays.asList(comparisonValue));
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
                listPostFix = " ... <" + count +" more " + countPostFix+">";
            }
            List<String> subList = comparisonValues.subList(0, Math.min(MAX_LIST_ELEMENT, comparisonValues.size()));
            StringBuilder formatListBuilder = new StringBuilder();
            int subListSize = subList.size();
            for (int i = 0; i < subListSize; i++){
                formatListBuilder.append("'").append(subList.get(i)).append("'");
                if( i != subListSize - 1){
                    formatListBuilder.append(", ");
                }
            }
            formatListBuilder.append(listPostFix);
            formattedList = formatListBuilder.toString();
        }

        return "[" + formattedList + "]";
    }

    private static String formatStringComparisonValue(String comparisonValue, boolean isSensitive){
        return "'" + (isSensitive ? HASHED_VALUE :  comparisonValue) + "'";
    }

    private static String formatDoubleComparisonValue(Double comparisonValue, boolean isDate){
        if(comparisonValue == null){
            return INVALID_VALUE;
        }
        DecimalFormat decimalFormat = new DecimalFormat("0.#####");
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.UK));
        if(isDate){
            return "'" + decimalFormat.format(comparisonValue) + "' (" + DateTimeUtils.doubleToFormattedUTC(comparisonValue) + " UTC)";
        }
        return "'" + decimalFormat.format(comparisonValue) + "'";
    }

    public static String formatUserCondition(UserCondition userCondition){
        Comparator userComparator = Comparator.fromId(userCondition.getComparator());
        if (userComparator == null) {
            throw new IllegalArgumentException("Comparison operator is invalid.");
        }
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

        return "User." + userCondition.getComparisonAttribute() + " " + userComparator.getName() + " " + comparisonValue;
    }
    public static String formatSegmentFlagCondition(SegmentCondition segmentCondition, Segment segment){
        String segmentName;
        if(segment != null){
            segmentName = segment.getName();
            if(segmentName == null || segmentName.isEmpty()){
                segmentName = INVALID_NAME;
            }
        } else {
            segmentName = INVALID_REFERENCE;
        }
        SegmentComparator segmentComparator = SegmentComparator.fromId(segmentCondition.getSegmentComparator());
        if (segmentComparator == null) {
            throw new IllegalArgumentException("Segment comparison operator is invalid.");
        }
        return "User " + segmentComparator.getName() + " '" + segmentName + "'";
    }
    public static String formatPrerequisiteFlagCondition(PrerequisiteFlagCondition prerequisiteFlagCondition){
        String prerequisiteFlagKey = prerequisiteFlagCondition.getPrerequisiteFlagKey();
        PrerequisiteComparator prerequisiteComparator = PrerequisiteComparator.fromId(prerequisiteFlagCondition.getPrerequisiteComparator());
        if (prerequisiteComparator == null) {
            throw new IllegalArgumentException("Prerequisite Flag comparison operator is invalid.");
        }
        SettingsValue prerequisiteValue = prerequisiteFlagCondition.getValue();
        String comparisonValue = prerequisiteValue ==  null ? INVALID_VALUE : prerequisiteValue.toString();
        return "Flag '"+prerequisiteFlagKey+"' "+prerequisiteComparator.getName()+" '"+comparisonValue+"'";
    }


    public static String formatCircularDependencyList(List<String> visitedKeys, String key){
        StringBuilder builder = new StringBuilder();
        visitedKeys.forEach((visitedKey) -> builder.append("'").append(visitedKey).append("' -> "));
        builder.append("'").append(key).append("'");
        return builder.toString();
    }
}
