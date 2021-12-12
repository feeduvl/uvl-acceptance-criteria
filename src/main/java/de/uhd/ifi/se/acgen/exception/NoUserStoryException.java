package de.uhd.ifi.se.acgen.exception;

/**
 * An exception that is thrown when a user story document does not contain a
 * well-formatted user story string in the syntax “As a [role], I want [goal]
 * (so that [reason]).”
 * 
 * @see de.uhd.ifi.se.acgen.model.UserStory
 * @see Exception
 */
public class NoUserStoryException extends Exception {

    /**
     * The constructor of the {@link NoUserStoryException} which calls the
     * super constructor of the {@link java.lang.Exception} class.
     * 
     * @param errorMessage an error message describing the error
     */
    public NoUserStoryException(String errorMessage) {
        super(errorMessage);
    }
}
