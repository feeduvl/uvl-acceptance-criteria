package de.uhd.ifi.se.acgen.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uhd.ifi.se.acgen.exception.NoUserStoryException;
import de.uhd.ifi.se.acgen.exception.TokenNotFoundException;
import de.uhd.ifi.se.acgen.generator.Generator;

/**
 * Stores a user story consisting of a role, a goal and a reason, and allows
 * for extraction of such user story from a larger document. Also stores
 * acceptance criteria generated for the user story.
 * 
 * The user story syntax is given as “As a [role], I want [goal] (so that 
 * [reason]).”
 * 
 * @see Generator
 */
public class UserStory {

    String role;
    String goal;
    String reason;
    boolean containsListOrNote;
    boolean wasCutAtListOrNote;
    Map<String, List<AcceptanceCriterion>> acceptanceCriteria;

    /**
     * The constructor for a user story, which always initializes the
     * identification of the user story parts.
     * 
     * @param userStoryString the string containing a user story
     * @throws NoUserStoryException if the string does not contain a valid user
     * story according to the user story syntax.
     */
    public UserStory(String userStoryString) throws NoUserStoryException {
        wasCutAtListOrNote = false;
        acceptanceCriteria = new HashMap<String, List<AcceptanceCriterion>>();

        // also replaces three or more dots by the character “…” so that
        // multiple dots are not interpreted as a sentence ending.
        identifyParts(userStoryString.replaceAll("\\.{3,}", "…"));
    };

    
    /** 
     * Extract role, goal and reason of a user story from a string.
     * 
     * @param userStoryString the string containing a user story
     * @throws NoUserStoryException if the string does not contain a valid user
     * story according to the user story syntax.
     * 
     * @see UserStory
     */
    private void identifyParts(String userStoryString) throws NoUserStoryException {
        int indexAsA = userStoryString.toUpperCase().indexOf("AS A", 0);
        if (indexAsA == -1) {
            // if a user story does not contain a role specified using the
            // syntax “As a(n) [role]”
            throw new NoUserStoryException("A role could not be found. Please make sure the role of the user story is declared using the syntax \"As a(n) [role]\".");
        }

        // cut the user story string to start at the beginning of the role part
        String shortenedUserStoryString = userStoryString.substring(indexAsA);

        // if the user story contains at one point a bullet point list or a
        // note possibly interrupting the user story sentence, the user story
        // string is cut before that interrupting. This is done because a
        // bullet point list or a note within the user story sentence severely
        // interrupts the NLP analysis of the user story sentence and it is
        // difficult to distinguish whether lines after such interruption
        // belong to the user story sentence or not.
        int listOrNoteAfterStartOfUserStory = indexOfListOrNote(shortenedUserStoryString);
        shortenedUserStoryString = shortenedUserStoryString.substring(0, listOrNoteAfterStartOfUserStory);
        if (listOrNoteAfterStartOfUserStory < userStoryString.length()) {
            // denoting to the user that information has been removed hereafter
            shortenedUserStoryString += " […]";
        }

        int indexIWant = shortenedUserStoryString.toUpperCase().indexOf("I WANT", 0);
        if (indexIWant == -1) {
            // if a user story does not contain a goal specified using the
            // syntax “I want [goal]”
            throw new NoUserStoryException("A goal could not be found. Please make sure the goal of the user story is declared after the role using the syntax \"I want [goal]\".");
        }

        // some sanitizing happens here, i.e., remove asterisks (usually used
        // for formatting) and replace multiple whitespace characters by a
        // single space (also removing line breaks)
        role = shortenedUserStoryString.substring(0, indexIWant).replaceAll("\\*", "").replaceAll("\\s+", " ");

        int indexSoThat = shortenedUserStoryString.toUpperCase().indexOf("SO THAT", indexIWant);
        int indexSentencePeriod = findSentencePeriodOrEndOfString(shortenedUserStoryString, indexIWant);
        if (indexSoThat == -1 || indexSoThat > indexSentencePeriod) {
            // The user story does not contain a reason. That is okay, we can
            // still work with it, but the user is later notified that no
            // reason was found and is asked to provide one.

            // some sanitizing happens here, see above.
            goal = shortenedUserStoryString.substring(indexIWant, indexSentencePeriod).replaceAll("\\*", "").replaceAll("\\s+", " ");
            reason = "";
        } else {
            // The user story does contain a reason.

            // some sanitizing happens here, see above.
            goal = shortenedUserStoryString.substring(indexIWant, indexSoThat).replaceAll("\\*", "").replaceAll("\\s+", " ");
            reason = shortenedUserStoryString.substring(indexSoThat, indexSentencePeriod).replaceAll("\\*", "").replaceAll("\\s+", " ");
        }
    }

    
    /** 
     * Determines whether a period denotes the end of a sentence.
     * 
     * @param userStoryString a string containing the sentence
     * @param indexOfPeriod an index of the period to be checked
     * @return {@code true} if the period at the index denotes the end of a
     * sentence
     */
    private boolean isSentenceEnding(String userStoryString, int indexOfPeriod) {
        try {
            List<String> abbreviations = Arrays.asList("e.g.", "etc.", "approx.", "i.e.", "cf.", "encl.", "p.a.", "Dr.", "Prof.", "no.");
            if (Character.isWhitespace(userStoryString.charAt(indexOfPeriod + 1))) {
                // here, the period is followed by a whitespace character
                for (String abbreviation : abbreviations) {
                    if (userStoryString.substring(0, indexOfPeriod + 1).endsWith(abbreviation)) {
                        // here, the period is part of a common abbreviation,
                        // i.e., it is not a sentence period
                        return false;
                    }
                }
                // here, the period is followed by a whitespace character and
                // not part of a common abbreviation, i.e., it is most likely a
                // sentence period
                return true;
            }
            // here, the period is not followed by a whitespace character and 
            // thereby most likely not a sentence period
            return false;
        } catch (StringIndexOutOfBoundsException e) {
            // If this exception is thrown, the period is not followed by any
            // character and therefore is a sentence period
            return true;
        }
    }
    
    
    /** 
     * Finds the index of a sentence period in or the end of a string,
     * whichever occurs first.
     * 
     * @param shortenedUserStoryString the string to be searched
     * @param indexOfLastKeyword the index before which a sentence period
     * is ignored
     * @return the index of a sentence period in or the end of the string,
     * whichever occurs first.
     */
    private int findSentencePeriodOrEndOfString(String shortenedUserStoryString, int indexOfLastKeyword) {
        int indexOfPeriod = indexOfLastKeyword;
        do {
            // find first period after the last period investigated (or the
            // index before which a sentence period shall be ignored)
            indexOfPeriod = shortenedUserStoryString.indexOf(".", indexOfPeriod + 1);

            if (indexOfPeriod != -1 && isSentenceEnding(shortenedUserStoryString, indexOfPeriod)) {
                // the period indeed denotes a sentence ending, so
                return indexOfPeriod + 1; // return its index
            }
        } while (indexOfPeriod != -1);  // while there is still a sentence
                                        // period that has not been
                                        // investigated
        
        // here, no sentence period has been found
        if (containsListOrNote) {
            // it is very likely that the sentence period has been removed
            // while a bullet point list or a note was cut off.
            wasCutAtListOrNote = true;
        }
        return shortenedUserStoryString.length(); // the end of the string
    }

    
    /** 
     * Finds the index of a bullet point list or a note, whichever occurs
     * first. Returns the end of the string, if none is found.
     * 
     * A bullet point list is given in the format
     * 
     * * …
     * 
     * or 
     * 
     * - …
     * 
     * with a newline character beforehand.
     * 
     * A note is given in the format
     * 
     * \\ …
     * 
     * with a newline character beforehand.
     * 
     * @param userStoryString the string to be searched
     * @return the index of a bullet point list, a note or the end of the
     * string, whichever occurs first.
     */
    private int indexOfListOrNote(String userStoryString) {
        Pattern newLineandStarOrDash = Pattern.compile("\\R\\s*(\\*|-|\\\\\\\\)\\s");
        Matcher matcher = newLineandStarOrDash.matcher(userStoryString);
        if (matcher.find()) {
            // The bullet point list or note pattern has been found
            containsListOrNote = true;
            return matcher.start(); // the index of the first character of the
                                    // bullet point list or note
        }
        // No bullet point list or note has been found
        containsListOrNote = false;
        return userStoryString.length(); // the end of the string
    }

    
    /** 
     * Composes the string of the user story from the role, goal and reason
     * parts.
     * 
     * @return a string containing the user story
     */
    public String getUserStoryString() {
        return role + goal + reason;
    }

    
    /** 
     * Returns the role of the user story.
     * 
     * @return the role of the user story
     */
    public String getRole() {
        return role;
    }

    
    /** 
     * Returns the goal of the user story.
     * 
     * @return the goal of the user story
     */
    public String getGoal() {
        return goal;
    }

    
    /** 
     * Returns the reason of the user story.
     * 
     * @return the reason of the user story
     */
    public String getReason() {
        return reason;
    }

    
    /** 
     * Returns a boolean value indicating whether the user story has a reason.
     * 
     * @return {@code true} if the user story has a reason
     */
    public boolean containsReason() {
        return !reason.equals("");
    }

    
    /** 
     * Returns a boolean value indicating whether the user story was cut at a
     * bullet point list or note and it is likely that information was lost
     * during that process.
     *
     * @return {@code true} if the user story was cut at a bullet point list or
     * note
     */
    public boolean wasCutAtListOrNote() {
        return wasCutAtListOrNote;
    }

    
    /** 
     * Provides the acceptance criteria of a user story and generates them
     * unless already done.
     * 
     * @param generator the generator to be used for generation of acceptance
     * criteria
     * @param debug whether to include debug information
     * @return a list of acceptance criteria generated by the generator
     * provided
     * @throws TokenNotFoundException if an essential part of a user story such
     * as the verb or the subject could not be identified by the Stanford
     * CoreNLP tools.
     * 
     * @see Generator
     */
    public List<AcceptanceCriterion> getAcceptanceCriteria(Generator generator, boolean debug) throws TokenNotFoundException {
        // The name of the generator class is used to check whether this type
        // of generator has already been used for generating acceptance
        // criteria for this user story.
        String generatorName = generator.getClass().getName();
        if (!acceptanceCriteria.containsKey(generatorName)) {
            // If acceptance criteria have not been generated before, generate
            // them and add them to the map.
            acceptanceCriteria.put(generatorName, generator.generate(this, debug));
        }
        return acceptanceCriteria.get(generatorName);
    }
}
