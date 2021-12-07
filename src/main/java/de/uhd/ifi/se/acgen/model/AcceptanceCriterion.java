package de.uhd.ifi.se.acgen.model;

public class AcceptanceCriterion {

    String rawString;
    AcceptanceCriterionType type;
    
    public AcceptanceCriterion(String _rawString, AcceptanceCriterionType _type) {
        rawString = _rawString;
        type = _type;
    }

    public String getRawString() {
        return this.rawString;
    }

    public AcceptanceCriterionType getType() {
        return this.type;
    }

    public String toString() {
        return type.getGherkinKeyword() + (type.isLog() ? "" : " ") + type.getPrefix() + rawString + type.getSuffix();
    }

}
