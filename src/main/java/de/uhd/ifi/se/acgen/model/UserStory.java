package de.uhd.ifi.se.acgen.model;

import de.uhd.ifi.se.acgen.exception.NoUserStoryException;

public class UserStory {

    String role;
    String goal;
    String reason;

    public UserStory(String userStoryString) throws NoUserStoryException {
        int indexAsA = userStoryString.toUpperCase().indexOf("AS A", 0);
        if (indexAsA == -1) {
             throw new NoUserStoryException("A role could not be found. Please make sure the role of the user story is declared using the syntax \"As a(n) [role]\".");
        }
        int indexIWant = userStoryString.toUpperCase().indexOf("I WANT", indexAsA);
        if (indexIWant == -1) {
            throw new NoUserStoryException("A goal could not be found. Please make sure the goal of the user story is declared after the role using the syntax \"I want [goal]\".");
        }
        role = userStoryString.substring(indexAsA, indexIWant);
        int indexSoThat = userStoryString.toUpperCase().indexOf("SO THAT", indexIWant);
        if (indexSoThat == -1) {
            goal = userStoryString.substring(indexIWant, findEndOfUserStory(userStoryString, indexIWant));
            reason = "";
        } else {
            goal = userStoryString.substring(indexIWant, indexSoThat);
            reason = userStoryString.substring(indexSoThat, findEndOfUserStory(userStoryString, indexSoThat));
        }
    };
    
    private boolean isEg(String userStoryString, int indexOfPeriod) {
        try {
            return userStoryString.toLowerCase().substring(indexOfPeriod - 3, indexOfPeriod + 1).equals("e.g.") || 
                userStoryString.toLowerCase().substring(indexOfPeriod - 1, indexOfPeriod + 3).equals("e.g.");
        } catch (StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    private int findEndOfUserStory(String userStoryString, int indexLastKeyword) {
        int indexOfPeriod = indexLastKeyword;
        do {
            indexOfPeriod = userStoryString.indexOf(".", indexOfPeriod + 1);
            if (indexOfPeriod != -1 && !isEg(userStoryString, indexOfPeriod)) {
                return indexOfPeriod + 1;
            }
        } while (indexOfPeriod != -1);
        
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
}
