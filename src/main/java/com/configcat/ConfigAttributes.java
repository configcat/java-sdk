package com.configcat;

class Config {
    static final String Preferences = "p";
    static final String Entries = "f";
}

class Preferences {
    static final String BaseUrl = "u";
    static final String Redirect = "r";
}

class Setting {
    static final String Value = "v";
    static final String Type = "t";
    static final String RolloutPercentageItems = "p";
    static final String RolloutRules = "r";
    static final String VariationId = "i";
}

class RolloutRules {
    static final String Value = "v";
    static final String ComparisonAttribute = "a";
    static final String Comparator = "t";
    static final String ComparisonValue = "c";
    static final String VariationId = "i";
}

class RolloutPercentageItems {
    static final String Value = "v";
    static final String Percentage = "p";
    static final String VariationId = "i";
}
