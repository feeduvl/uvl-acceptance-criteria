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

public class UserStory {

    String role;
    String goal;
    String reason;
    boolean containsListOrNote;
    boolean wasCutAtListOrNote;
    Map<String, List<String>> acceptanceCriteria;

    public UserStory(String userStoryString) throws NoUserStoryException {
        identifyParts(userStoryString.replaceAll("\\.{3,}", "…"));
        acceptanceCriteria = new HashMap<String, List<String>>();
    };

    private void identifyParts(String userStoryString) throws NoUserStoryException {
        int indexAsA = userStoryString.toUpperCase().indexOf("AS A", 0);
        if (indexAsA == -1) {
             throw new NoUserStoryException("A role could not be found. Please make sure the role of the user story is declared using the syntax \"As a(n) [role]\".");
        }
        String shortenedUserStoryString = userStoryString.substring(indexAsA);
        int listOrNoteAfterStartOfUserStory = indexOfListOrNote(shortenedUserStoryString);
        shortenedUserStoryString = shortenedUserStoryString.substring(0, listOrNoteAfterStartOfUserStory);
        int indexIWant = shortenedUserStoryString.toUpperCase().indexOf("I WANT", 0);
        if (indexIWant == -1) {
            throw new NoUserStoryException("A goal could not be found. Please make sure the goal of the user story is declared after the role using the syntax \"I want [goal]\".");
        }
        role = shortenedUserStoryString.substring(0, indexIWant).replaceAll("\\*", "").replaceAll("\\s+", " ");
        int indexSoThat = shortenedUserStoryString.toUpperCase().indexOf("SO THAT", indexIWant);
        wasCutAtListOrNote = false;
        if (indexSoThat == -1) {
            goal = shortenedUserStoryString.substring(indexIWant, findSentencePeriodOrEndOfString(shortenedUserStoryString, indexIWant)).replaceAll("\\*", "").replaceAll("\\s+", " ");
            reason = "";
        } else {
            goal = shortenedUserStoryString.substring(indexIWant, indexSoThat).replaceAll("\\*", "").replaceAll("\\s+", " ");
            reason = shortenedUserStoryString.substring(indexSoThat, findSentencePeriodOrEndOfString(shortenedUserStoryString, indexSoThat)).replaceAll("\\*", "").replaceAll("\\s+", " ");
        }
    }

    private boolean isSentenceEnding(String userStoryString, int indexOfPeriod) {
        try {
            List<String> abbreviations = Arrays.asList("e.g.", "etc.", "approx.", "i.e.", "cf.", "encl.", "p.a.");
            if (Character.isWhitespace(userStoryString.charAt(indexOfPeriod + 1))) {
                for (String abbreviation : abbreviations) {
                    if (userStoryString.substring(0, indexOfPeriod + 1).endsWith(abbreviation)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        } catch (StringIndexOutOfBoundsException e) {
            return true;
        }
    }
    
    private int findSentencePeriodOrEndOfString(String shortenedUserStoryString, int indexOfLastKeyword) {
        int indexOfPeriod = indexOfLastKeyword;
        do {
            indexOfPeriod = shortenedUserStoryString.indexOf(".", indexOfPeriod + 1);
            if (indexOfPeriod != -1 && isSentenceEnding(shortenedUserStoryString, indexOfPeriod)) {
                return indexOfPeriod + 1;
            }
        } while (indexOfPeriod != -1);
        
        if (containsListOrNote) {
            wasCutAtListOrNote = true;
        }
        return shortenedUserStoryString.length();
    }

    private int indexOfListOrNote(String userStoryString) {
        Pattern newLineandStarOrDash = Pattern.compile("\\R\\s*(\\*|-|\\\\\\\\)\\s");
        Matcher matcher = newLineandStarOrDash.matcher(userStoryString);
        if (matcher.find()) {
            containsListOrNote = true;
            return matcher.start();
        }
        containsListOrNote = false;
        return userStoryString.length();
    }

    public String getUserStoryString() {
        return role + goal + reason;
    }

    public String getRole() {
        return role;
    }

    public String getGoal() {
        return goal;
    }

    public String getReason() {
        return reason;
    }

    public boolean containsReason() {
        return !reason.equals("");
    }

    public boolean wasCutAtListOrNote() {
        return wasCutAtListOrNote;
    }

    public List<String> getAcceptanceCriteria(Generator generator) throws TokenNotFoundException {
        String generatorName = generator.getClass().getName();
        assert(!generatorName.equals(""));
        if (!acceptanceCriteria.containsKey(generatorName)) {
            acceptanceCriteria.put(generatorName, generator.generate(this));
        }
        return acceptanceCriteria.get(generatorName);
    }
}
