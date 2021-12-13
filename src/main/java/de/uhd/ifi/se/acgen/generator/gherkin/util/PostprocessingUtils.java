package de.uhd.ifi.se.acgen.generator.gherkin.util;

import de.uhd.ifi.se.acgen.model.AcceptanceCriterion;

/**
 * Provides various utility functions for postprocessing of acceptance
 * criteria.
 */
public class PostprocessingUtils {
    
    /**
     * The private constructor of the utility class, preventing instantiation.
     */
    private PostprocessingUtils() {
        throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates an acceptance criterion which contains information from one
     * acceptance criterion and does not contain information from another
     * acceptance criterion, i.e., remove all information contained in the
     * second acceptance criterion from the first acceptance criterion.
     * 
     * @param acceptanceCriterion the acceptance criterion to be cut
     * @param other the acceptance criterion containing information which is
     * to be removed from the first acceptance criterion
     * @return the shortened acceptance criterion
     * 
     * @see de.uhd.ifi.se.acgen.generator.gherkin.GherkinGenerator#resolveDuplicateInformation
     */
    public static AcceptanceCriterion cutOffAtNextAcceptanceCriterion(AcceptanceCriterion acceptanceCriterion, AcceptanceCriterion other) {
        if (other.getBeginReplacementIndex() < acceptanceCriterion.getEndReplacementIndex() && acceptanceCriterion.getRawString().indexOf(other.getRawString()) != -1) {
            // if the other acceptance criterion starts before the the first
            // acceptance criterion ends and we can find the raw string of the
            // other acceptance criterion in the raw string of the first
            // acceptance criterion
            
            // cut the first acceptance criterion’s raw string
            String newRawString = acceptanceCriterion.getRawString().substring(0, acceptanceCriterion.getRawString().indexOf(other.getRawString()));
            
            // remove spaces at the end of the cut raw string
            while (newRawString.endsWith(" ")) {
                newRawString = newRawString.substring(0, newRawString.length() - 1);
            }
            for (String conditionalStarterString : SharedUtils.conditionalStarterStrings) {
                if (newRawString.endsWith(conditionalStarterString)) {
                    // if we have identified the conditional starter string of
                    // the other acceptance criterion, we remove it
                    newRawString = newRawString.substring(0, newRawString.lastIndexOf(conditionalStarterString));

                    // again, remove spaces at the end of the cut raw string
                    while (newRawString.endsWith(" ")) {
                        newRawString = newRawString.substring(0, newRawString.length() - 1);
                    }        
                    return new AcceptanceCriterion(newRawString, acceptanceCriterion.getType(), acceptanceCriterion.getBeginReplacementIndex(), other.getBeginReplacementIndex() - 1);
                }
            }
        }

        // return the unmodified acceptance criterion, if the acceptance
        // criteria do not overlap or if the conditional starter string of the
        // other acceptance criterion could not be identified and removed.
        return acceptanceCriterion;
    }

    /**
     * Creates an acceptance criterion which replaces a given UI description by
     * a generic expression
     * 
     * @param acceptanceCriterion the acceptance criterion to be modified
     * @param uiDescription the UI description
     * @return the modified acceptance criterion
     * 
     * @see de.uhd.ifi.se.acgen.generator.gherkin.GherkinGenerator#resolveDuplicateInformation
     */
    public static AcceptanceCriterion replaceUIDescription(AcceptanceCriterion acceptanceCriterion, String uiDescription) {
        if (uiDescription != null && acceptanceCriterion.getRawString().contains(uiDescription)) {
            // If the acceptance criterion contains the UI description, it is
            // replaced by the words “the active user interface”
            return new AcceptanceCriterion(acceptanceCriterion.getRawString().replace(uiDescription, "the active user interface"), acceptanceCriterion.getType(), acceptanceCriterion.getBeginReplacementIndex(), acceptanceCriterion.getEndReplacementIndex());
        }
        return acceptanceCriterion;
    }

}
