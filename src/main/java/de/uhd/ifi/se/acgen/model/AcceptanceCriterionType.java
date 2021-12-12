package de.uhd.ifi.se.acgen.model;

/**
 * Provides keywords and other text modules for acceptance criteria.
 * 
 * @see AcceptanceCriterion
 */
public enum AcceptanceCriterionType {
    
    /**
     * A role precondition acceptance criterion type of the format “GIVEN
     * [role] is using the software”
     */
    ROLE("GIVEN", "", " is using the software"),

    /**
     * A UI precondition acceptance criterion type of the format “GIVEN the
     * active user interface is [UI description]”.
     */
    UI("GIVEN", "the active user interface is ", ""),

    /**
     * An action acceptance criterion of the format “WHEN [action]”.
     */
    ACTION("WHEN", "", ""),

    /**
     * An expected result acceptance criterion of the format “THEN [expected
     * result]”
     */
    RESULT("THEN", "", ""),

    /**
     * An action acceptance criterion of the format “WHEN [action]”, derived
     * from the reason of a user story.
     */
    ACTION_IN_REASON("WHEN", "", ""),

    /**
     * An expected result acceptance criterion of the format “THEN [expected
     * result]”, derived from the reason of a user story.
     */
    RESULT_IN_REASON("THEN", "", ""),

    /**
     * An error log message.
     */
    ERROR("", "ERROR: ", ""),

    /**
     * A warning log message.
     */
    WARNING("", "WARNING: ", ""),

    /**
     * An info log message.
     */
    INFO("", "INFO: ", ""),

    /**
     * A debug log message.
     */
    DEBUG("", "DEBUG: ", "");

    /**
     * The Gherkin keyword for Gherkin acceptance criteria or the severity
     * keyword of log messages.
     */
    private String keyword;

    /**
     * A string that is part of every acceptance criterion of this type and
     * comes directly before its individual content (the raw string of the
     * {@code AcceptanceCriterion}.
     */
    private String prefix;

    /**
     * A string that is part of every acceptance criterion of this type and
     * comes directly after its individual content (the raw string of the
     * {@code AcceptanceCriterion}.
     */
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
