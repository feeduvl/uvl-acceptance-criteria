package de.uhd.ifi.se.acgen.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.uhd.ifi.se.acgen.exception.NoUserStoryException;

public class TestUserStory {
    
    @Test
    public void testEmptyUserStory() {
        NoUserStoryException e = assertThrows(NoUserStoryException.class, () -> new UserStory(""));
        assertEquals("A role could not be found. Please make sure the role of the user story is declared using the syntax \"As a(n) [role]\".", e.getMessage());
    }

    @Test
    public void testRoleOnlyUserStory() {
        NoUserStoryException e = assertThrows(NoUserStoryException.class, () -> new UserStory("As a developer I wrote a crappy user story for this test to fail."));
        assertEquals("A goal could not be found. Please make sure the goal of the user story is declared after the role using the syntax \"I want [goal]\".", e.getMessage());
    }

    @Test
    public void testUserStoriesWithoutReason() {
        assertDoesNotThrow(() -> {
            String userStoryString = "This is not part of the user story.\n" +
                    "As a developer, I want to write this valid user story. Did I make it?";
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getRole().contains("As a developer"));
            assertTrue(userStory.getGoal().contains("I want to write this valid user story."));
            assertEquals("", userStory.getReason());
            assertFalse(userStory.containsReason());
            assertFalse(userStory.getUserStoryString().contains("This is not part of the user story"));
            assertFalse(userStory.getUserStoryString().contains("Did I make it?"));
        });

        assertDoesNotThrow(() -> {
            String userStoryString = "This is not part of the user story.\n" +
                    "As a developer, I want to write this valid user story and omit the sentence period";
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getRole().contains("As a developer"));
            assertTrue(userStory.getGoal().contains("I want to write this valid user story and omit the sentence period"));
            assertEquals("", userStory.getReason());
            assertFalse(userStory.containsReason());
            assertFalse(userStory.getUserStoryString().contains("This is not part of the user story"));
        });

        assertDoesNotThrow(() -> {
            String userStoryString = "This is not part of the user story.\n" +
                    "As a developer, I want to write this valid user story. This sentence contains a completely different reason, so that I can confuse the algorithm – or not?";
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getRole().contains("As a developer"));
            assertTrue(userStory.getGoal().contains("I want to write this valid user story."));
            assertEquals("", userStory.getReason());
            assertFalse(userStory.containsReason());
            assertFalse(userStory.getUserStoryString().contains("This sentence contains a completely different reason"));
        });
    }

    @Test
    public void testUserStoriesWithReason() {
        assertDoesNotThrow(() -> {
            String userStoryString = "This is not part of the user story.\n" +
                    "As a developer, I want to write this valid user story,\n" +
                    "so that I can test my user story extractor.\n" + 
                    "This is another sentence, also not being part of the user story.";
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getRole().contains("As a developer"));
            assertTrue(userStory.getGoal().contains("I want to write this valid user story,"));
            assertTrue(userStory.getReason().contains("so that I can test my user story extractor."));
            assertTrue(userStory.containsReason());
            assertFalse(userStory.getUserStoryString().contains("This is not part of the user story"));
            assertFalse(userStory.getUserStoryString().contains("This is another sentence, also not being part of the user story"));
        });

        assertDoesNotThrow(() -> {
            String userStoryString = "This is not part of the user story.\n" +
                    "As a developer, I want to write this valid user story and omit the sentence period,\n" +
                    "so that I can test my user story extractor";
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getRole().contains("As a developer"));
            assertTrue(userStory.getGoal().contains("I want to write this valid user story and omit the sentence period,"));
            assertTrue(userStory.getReason().contains("so that I can test my user story extractor"));
            assertTrue(userStory.containsReason());
            assertFalse(userStory.getUserStoryString().contains("This is not part of the user story"));
        });
    }

    @Test
    public void testUserStoriesWithEgAndDots() {
        assertDoesNotThrow(() -> {
            String userStoryString = "As a developer, I want to include an example in my user story (e.g. hello, world, ...).";
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getGoal().contains("I want to include an example in my user story (e.g. hello, world, …)."));
        });

        assertDoesNotThrow(() -> {
            String userStoryString = "As a developer, I want to put an exemplary letter to the end of my user story and omit the sentence period, e.g. a";
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getGoal().contains("e.g. a"));
            assertTrue(userStory.getUserStoryString().equals(userStoryString));
        });
    }

    @Test
    public void testUserStoriesWithList() {
        assertDoesNotThrow(() -> {
            String userStoryString = "As a developer, I want this user story to\n" +
                    "* contain\n" +
                    "* a\n" +
                    "* starred\n" +
                    "* list.\n" +
                    "and see what happens.\n" + 
                    "This is another sentence, also not being part of the user story.";
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.wasCutAtListOrNote());
            assertTrue(userStory.getGoal().endsWith("to […]"));
            assertFalse(userStory.getGoal().contains("* contain"));
            assertFalse(userStory.getGoal().contains("and see what happens"));
        });

        assertDoesNotThrow(() -> {
            String userStoryString = "As a developer, I want this user story to\n" +
                    "- contain\n" +
                    "- a\n" +
                    "- dashed\n" +
                    "- list.\n" +
                    "and see what happens.\n" + 
                    "This is another sentence, also not being part of the user story.";
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.wasCutAtListOrNote());
            assertTrue(userStory.getGoal().endsWith("to […]"));
            assertFalse(userStory.getGoal().contains("* contain"));
            assertFalse(userStory.getGoal().contains("and see what happens"));
        });
    }

    @Test
    public void testUserStoriesWithNote() {
        assertDoesNotThrow(() -> {
            String userStoryString = "As a developer, I want this user story to contain a note\n" +
                    "\\\\ Note:\n" +
                    "This is the note.\n" +
                    "\n" +
                    "so that this part is removed.\n" + 
                    "This is another sentence, also not being part of the user story.";
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getGoal().endsWith("note […]"));
            assertFalse(userStory.containsReason());
            assertTrue(userStory.wasCutAtListOrNote());
        });

        assertDoesNotThrow(() -> {
            String userStoryString = "As a developer, I want this user story to contain a note.\n" +
            "\\\\ Note:\n" +
                    "This is the note.\n";
            UserStory userStory = new UserStory(userStoryString);
            assertTrue(userStory.getGoal().endsWith("note."));
            assertFalse(userStory.containsReason());
            assertFalse(userStory.wasCutAtListOrNote());
        });
    }
}
