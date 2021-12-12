package de.uhd.ifi.se.acgen.model;

/**
 * Provides keywords and other text modules for acceptance criteria.
 * 
 * @see AcceptanceCriterion
 */
public enum AcceptanceCriterionType {
    
    ROLE("GIVEN", "", " is using the software"),
    UI("GIVEN", "the active user interface is ", ""),
    ACTION("WHEN", "", ""),
    RESULT("THEN", "", ""),
    ACTION_IN_REASON("WHEN", "", ""),
    RESULT_IN_REASON("THEN", "", ""),
    ERROR("", "ERROR: ", ""),
    WARNING("", "WARNING: ", ""),
    INFO("", "INFO: ", ""),
    DEBUG("", "DEBUG: ", "");

    private String keyword;
    private String prefix;
    private String suffix;

    /**
     * The constructor for an acceptance criterion type. Stores the data
     * provided in the enum declaration in the member variables.
     * 
     * @param _keyword the Gherkin keyword for Gherkin acceptance criteria
     * or the severity keyword of log messages
     * @param _prefix a string that is part of every acceptance criterion of
     * this type and comes directly before its individual content (the raw
     * string of the {@code AcceptanceCriterion}.
     * @param _suffix a string that is part of every acceptance criterion of
     * this type and comes directly after its individual content (the raw
     * string of the {@code AcceptanceCriterion}.
     * 
     * @see AcceptanceCriterion
     */
    private AcceptanceCriterionType(String _keyword, String _prefix, String _suffix) {
        keyword = _keyword;
        prefix = _prefix;
        suffix = _suffix;
    }

    /**
     * Returns the keyword of the acceptance criterion type.
     * @return the keyword of the acceptance criterion type
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Returns the prefix of the acceptance criterion type.
     * @return the prefix of the acceptance criterion type
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the suffix of the acceptance criterion type.
     * @return the suffix of the acceptance criterion type
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Returns whether the acceptance criterion type is a log type.
     * 
     * @return {@code true} if the acceptance criterion type is a log type
     */
    public boolean isLog() {
        return this == ERROR || this == WARNING || this == INFO || this == DEBUG;
    }

}
