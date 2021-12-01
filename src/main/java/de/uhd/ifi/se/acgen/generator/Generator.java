package de.uhd.ifi.se.acgen.generator;

import java.util.List;

import de.uhd.ifi.se.acgen.exception.TokenNotFoundException;
import de.uhd.ifi.se.acgen.model.UserStory;

public interface Generator {
    
    public List<String> generate(UserStory userStory) throws TokenNotFoundException;

}
