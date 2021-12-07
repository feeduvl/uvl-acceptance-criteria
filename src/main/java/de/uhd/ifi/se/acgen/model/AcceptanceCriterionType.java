package de.uhd.ifi.se.acgen.model;

public enum AcceptanceCriterionType {
    
    ROLE("GIVEN", "", " is using the software"),
    UI("GIVEN", "the active user interface is ", ""),
    CAUSE("WHEN", "", ""),
    CAUSE_INTERACTION("WHEN", "the user clicks ", ""),
    EFFECT("THEN", "", ""),
    ERROR("", "ERROR: ", ""),
    WARNING("", "WARNING: ", ""),
    INFO("", "INFO: ", ""),
    DEBUG("", "DEBUG: ", "");

    private String gherkinKeyword;
    private String prefix;
    private String suffix;

    private AcceptanceCriterionType(String _gherkinKeyword, String _prefix, String _suffix) {
        gherkinKeyword = _gherkinKeyword;
        prefix = _prefix;
        suffix = _suffix;
    }

    public String getGherkinKeyword() {
        return gherkinKeyword;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public boolean isLog() {
        return this == ERROR || this == WARNING || this == INFO || this == DEBUG;
    }

}
