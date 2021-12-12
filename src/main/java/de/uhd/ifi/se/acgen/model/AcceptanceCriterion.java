package de.uhd.ifi.se.acgen.model;

/** 
 * Stores information related to an acceptance criterion. Also allows for
 * comparing two acceptance criteria by implementing the Comparable
 * interface.
 * 
 * @see Comparable
 */
public class AcceptanceCriterion implements Comparable<AcceptanceCriterion> {

    String rawString;
    AcceptanceCriterionType type;
    int beginReplacementIndex;
    int endReplacementIndex;
    
    /**
     * The constructor for an acceptance criterion with unspecified replacement
     * range.
     * 
     * @param _rawString the string of the acceptance criterion, not including
     * keywords or text modules resulting from the type.
     * @param _type the type of the acceptance criterion. Can be part of the
     * Gherkin syntax or a log message.
     * 
     * @see AcceptanceCriterionType
     */
    public AcceptanceCriterion(String _rawString, AcceptanceCriterionType _type) {
        rawString = _rawString;
        type = _type;
        beginReplacementIndex = -1;
        endReplacementIndex = -1;
    }

    /**
     * The constructor for an acceptance criterion which is generated from a
     * certain part of the user stories denoted by the 1-based indices of the
     * first and last word in the dependency graph.
     * 
     * @param _rawString the string of the acceptance criterion, not including
     * keywords or text modules resulting from the type.
     * @param _type the type of the acceptance criterion. Can be part of the
     * Gherkin syntax or a log message.
     * @param _beginReplacementIndex the 1-based index of the first word the
     * acceptance criterion is derived from
     * @param _endReplacementIndex the 1-based index of the last word the
     * acceptance criterion is derived from
     * 
     * @see AcceptanceCriterionType
     */
    public AcceptanceCriterion(String _rawString, AcceptanceCriterionType _type, int _beginReplacementIndex, int _endReplacementIndex) {
        rawString = _rawString;
        type = _type;
        beginReplacementIndex = _beginReplacementIndex;
        endReplacementIndex = _endReplacementIndex;
    }

    
    /** 
     * Returns the raw string of the acceptance criterion, not including
     * keywords or text modules resulting from the type.
     * 
     * @return the raw string of the acceptance criterion
     */
    public String getRawString() {
        return rawString;
    }

    
    /** 
     * Returns the type of the acceptance criterion.
     * 
     * @return the type of the acceptance criterion
     * 
     * @see AcceptanceCriterionType
     */
    public AcceptanceCriterionType getType() {
        return type;
    }

    
    /** 
     * Returns the 1-based index of the first word the acceptance criterion is
     * derived from
     * 
     * @return the 1-based index of the first word the acceptance criterion is
     * derived from
     */
    public int getBeginReplacementIndex() {
        return beginReplacementIndex;
    }

    
    /** 
     * Returns the 1-based index of the last word the acceptance criterion is
     * derived from
     * 
     * @return the 1-based index of the last word the acceptance criterion is
     * derived from
     */
    public int getEndReplacementIndex() {
        return endReplacementIndex;
    }

    
    /** 
     * {@inheritDoc}
     * 
     * The string representation consists of the raw string of the acceptance
     * criterion surrounded by keywords and text modules resulting from the
     * type of the acceptance criterion.
     * 
     * @see AcceptanceCriterionType
     */
    public String toString() {
        return type.getKeyword() + (type.isLog() ? "" : " ") + type.getPrefix() + rawString + type.getSuffix();
    }

    
    /** 
     * {@inheritDoc}
     * 
     * <p>The order of two acceptance criteria is derived from the order of
     * their type, then from their respective {@code beginReplacementIndex}.
     * </p>
     */
    public int compareTo(AcceptanceCriterion other) {
        return this.getType().equals(other.getType()) ? this.getBeginReplacementIndex() - other.getBeginReplacementIndex() : this.getType().compareTo(other.getType());
    }

}
