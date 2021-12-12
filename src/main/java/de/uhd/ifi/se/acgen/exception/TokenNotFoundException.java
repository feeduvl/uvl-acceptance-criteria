package de.uhd.ifi.se.acgen.exception;

/**
 * An exception that is thrown when an essential part of a user story such as
 * the verb or the subject could not be identified by the Stanford CoreNLP
 * tools.
 * 
 * @see Exception
 */
public class TokenNotFoundException extends Exception{

    /**
     * The constructor of the {@link TokenNotFoundException} which calls the
     * super constructor of the {@link java.lang.Exception} class.
     * 
     * @param errorMessage an error message describing the error
     */

    public TokenNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
