package de.uhd.ifi.se.acgen.generator;

import java.util.List;

import de.uhd.ifi.se.acgen.exception.TokenNotFoundException;
import de.uhd.ifi.se.acgen.model.AcceptanceCriterion;
import de.uhd.ifi.se.acgen.model.UserStory;

/**
 * Generates acceptance criteria for a user story.
 */
public interface Generator {
    
    /**
     * Generates acceptance criteria for a user story.
     * 
     * @param userStory the user story acceptance criteria are generated for
     * @param debug whether to include debug information
     * @return a list of acceptance criteria
     * @throws TokenNotFoundException if an essential part of a user story such
     * as the verb or the subject could not be identified by the Stanford
     * CoreNLP tools.
     */
    public List<AcceptanceCriterion> generate(UserStory userStory, boolean debug) throws TokenNotFoundException;

}
