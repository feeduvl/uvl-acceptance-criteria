package de.uhd.ifi.se.acgen.model;

public class AcceptanceCriterion implements Comparable<AcceptanceCriterion> {

    String rawString;
    AcceptanceCriterionType type;
    int beginReplacementIndex;
    int endReplacementIndex;
    
    public AcceptanceCriterion(String _rawString, AcceptanceCriterionType _type) {
        rawString = _rawString;
        type = _type;
        beginReplacementIndex = -1;
        endReplacementIndex = -1;
    }

    public AcceptanceCriterion(String _rawString, AcceptanceCriterionType _type, int _beginReplacementIndex, int _endReplacementIndex) {
        rawString = _rawString;
        type = _type;
        beginReplacementIndex = _beginReplacementIndex;
        endReplacementIndex = _endReplacementIndex;
    }

    public String getRawString() {
        return rawString;
    }

    public AcceptanceCriterionType getType() {
        return type;
    }

    public int getBeginReplacementIndex() {
        return beginReplacementIndex;
    }

    public int getEndReplacementIndex() {
        return endReplacementIndex;
    }

    public String toString() {
        return type.getGherkinKeyword() + (type.isLog() ? "" : " ") + type.getPrefix() + rawString + type.getSuffix();
    }

    public int compareTo(AcceptanceCriterion other) {
        return this.getType().equals(other.getType()) ? this.getBeginReplacementIndex() - other.getBeginReplacementIndex() : this.getType().compareTo(other.getType());
    }

}
