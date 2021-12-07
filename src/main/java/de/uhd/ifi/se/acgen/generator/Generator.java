package de.uhd.ifi.se.acgen.generator;

import java.util.List;

import de.uhd.ifi.se.acgen.exception.TokenNotFoundException;
import de.uhd.ifi.se.acgen.model.AcceptanceCriterion;
import de.uhd.ifi.se.acgen.model.UserStory;

public interface Generator {
    
    public List<AcceptanceCriterion> generate(UserStory userStory, boolean debug) throws TokenNotFoundException;

}
