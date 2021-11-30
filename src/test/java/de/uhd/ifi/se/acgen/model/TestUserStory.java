package de.uhd.ifi.se.acgen.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import de.uhd.ifi.se.acgen.exception.NoUserStoryException;

public class TestUserStory {
    
    @Test
    public void testEmptyUserStory() {
        assertThrows(NoUserStoryException.class, () -> {
            new UserStory("");
        });

        try {
            new UserStory("");
        } catch (NoUserStoryException e) {
            assertEquals("A role could not be found. Please make sure the role of the user story is declared using the syntax \"As a(n) [role]\".", e.getMessage());
        }
    }

    @Test
    public void testRoleOnlyUserStory() {

        String userStoryString = "As a developer I wrote a crappy user story for this test to fail.";
        
        assertThrows(NoUserStoryException.class, () -> {
            new UserStory(userStoryString);
        });

        try {
            new UserStory(userStoryString);
        } catch (NoUserStoryException e) {
            assertEquals("A goal could not be found. Please make sure the goal of the user story is declared after the role using the syntax \"I want [goal]\".", e.getMessage());
        }
    }

    @Test
    public void testUserStoriesWithoutReason() {
        String userStoryString = "This is not part of the user story.\n" +
                "As a developer, I want to write this valid user story. Did I make it?";
        try {
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getRole().contains("As a developer"));
            assertTrue(userStory.getGoal().contains("I want to write this valid user story."));
            assertEquals("", userStory.getReason());
            assertFalse(userStory.containsReason());
            assertFalse(userStory.getUserStoryString().contains("This is not part of the user story"));
            assertFalse(userStory.getUserStoryString().contains("Did I make it?"));
        } catch (NoUserStoryException e) {
            fail();
        }

        userStoryString = "This is not part of the user story.\n" +
                "As a developer, I want to write this valid user story and omit the sentence period";
        try {
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getRole().contains("As a developer"));
            assertTrue(userStory.getGoal().contains("I want to write this valid user story and omit the sentence period"));
            assertEquals("", userStory.getReason());
            assertFalse(userStory.containsReason());
            assertFalse(userStory.getUserStoryString().contains("This is not part of the user story"));
        } catch (NoUserStoryException e) {
            fail();
        }
    }

    @Test
    public void testUserStoriesWithReason() {
        String userStoryString = "This is not part of the user story.\n" +
                "As a developer, I want to write this valid user story,\n" +
                "so that I can test my user story extractor.\n" + 
                "This is another sentence, also not being part of the user story.";
        try {
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getRole().contains("As a developer"));
            assertTrue(userStory.getGoal().contains("I want to write this valid user story,"));
            assertTrue(userStory.getReason().contains("so that I can test my user story extractor."));
            assertTrue(userStory.containsReason());
            assertFalse(userStory.getUserStoryString().contains("This is not part of the user story"));
            assertFalse(userStory.getUserStoryString().contains("This is another sentence, also not being part of the user story"));
        } catch (NoUserStoryException e) {
            fail();
        }

        userStoryString = "This is not part of the user story.\n" +
                "As a developer, I want to write this valid user story and omit the sentence period,\n" +
                "so that I can test my user story extractor";
        try {
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getRole().contains("As a developer"));
            assertTrue(userStory.getGoal().contains("I want to write this valid user story and omit the sentence period,"));
            assertTrue(userStory.getReason().contains("so that I can test my user story extractor"));
            assertTrue(userStory.containsReason());
            assertFalse(userStory.getUserStoryString().contains("This is not part of the user story"));
        } catch (NoUserStoryException e) {
            fail();
        }
    }

    @Test
    public void testUserStoriesWithEgAndDots() {
        String userStoryString = "As a developer, I want to include an example in my user story (e.g. hello, world, ...).";
        try {
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getGoal().contains("I want to include an example in my user story (e.g. hello, world, …)."));
        } catch (NoUserStoryException e) {
            fail();
        }

        userStoryString = "As a developer, I want to put an exemplary letter to the end of my user story and omit the sentence period, e.g. a";
        try {
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getGoal().contains("e.g. a"));
            assertTrue(userStory.getUserStoryString().equals(userStoryString));
        } catch (NoUserStoryException e) {
            fail();
        }
    }

    @Test
    public void testUserStoriesWithStarredLine() {
        String userStoryString = "As a developer, I want this user story to\n" +
                "* contain\n" +
                "* a\n" +
                "* starred\n" +
                "* list.\n" +
                "and see what happens.\n" + 
                "This is another sentence, also not being part of the user story.";
        try {
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getGoal().contains("and see what happens"));
        } catch (NoUserStoryException e) {
            fail();
        }

        userStoryString = "As a developer, I want this user story to\n" +
                "* not\n" +
                "* contain\n" +
                "* a\n" +
                "* sentence\n" +
                "* period.\n" +
                "and see what happens";
        try {
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getGoal().contains("and see what happens"));
            assertTrue(userStory.getUserStoryString().equals(userStoryString));
        } catch (NoUserStoryException e) {
            fail();
        }
    }

}
