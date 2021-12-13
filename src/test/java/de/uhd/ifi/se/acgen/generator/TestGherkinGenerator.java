package de.uhd.ifi.se.acgen.generator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import org.junit.jupiter.api.Test;

import de.uhd.ifi.se.acgen.exception.TokenNotFoundException;
import de.uhd.ifi.se.acgen.model.AcceptanceCriterion;
import de.uhd.ifi.se.acgen.model.AcceptanceCriterionType;
import de.uhd.ifi.se.acgen.model.UserStory;

public class TestGherkinGenerator {

    @Test
    public void testGherkinGenerator() {
        assertDoesNotThrow(() -> {
            Gson gson = new Gson();
            JsonReader reader;
            reader = new JsonReader(new FileReader("src/test/java/de/uhd/ifi/se/acgen/generator/TestUserStories.json"));
            JsonArray userStories = gson.fromJson(reader, JsonArray.class);
            for (JsonElement userStoryElement : userStories) {
                String userStoryString = ((JsonObject) userStoryElement).get("userStory").getAsString();
                JsonArray expectedAcceptanceCriteriaJsonArray = ((JsonObject) userStoryElement).get("acceptanceCriteria").getAsJsonArray();
                List<String> expectedAcceptanceCriteriaStrings = new ArrayList<String>();
                expectedAcceptanceCriteriaJsonArray.forEach(expectedAcceptanceCriterion -> expectedAcceptanceCriteriaStrings.add(expectedAcceptanceCriterion.getAsString()));
                List<String> actualAcceptanceCriteriaStrings = new ArrayList<String>();
                assertDoesNotThrow(() -> {
                    List<AcceptanceCriterion> actualAcceptanceCriteria = new UserStory(userStoryString).getAcceptanceCriteria(new GherkinGenerator(), false);
                    actualAcceptanceCriteria.forEach(actualAcceptanceCriterion -> actualAcceptanceCriteriaStrings.add(actualAcceptanceCriterion.toString()));
                });
                assertEquals(expectedAcceptanceCriteriaStrings, actualAcceptanceCriteriaStrings);
            }
        });
    }

    @Test
    public void testDebugMessage() {
        String userStoryString = "As an overly possessive person, I want my things to be mine, so that I keep my things for myself.";
        assertDoesNotThrow(() -> {
            List<AcceptanceCriterion> acceptanceCriteria = new UserStory(userStoryString).getAcceptanceCriteria(new GherkinGenerator(), true);
            AcceptanceCriterion expectedAcceptanceCriterion = new AcceptanceCriterion("As an overly possessive person, the user wants the user’s things to be the user’s, so that the user keeps the user’s things for themself.", AcceptanceCriterionType.DEBUG);
            assertEquals(expectedAcceptanceCriterion.toString(), acceptanceCriteria.get(2).toString());
        });
    }

    @Test
    public void testExceptions() {
        String userStoryStringNoSubject = "As a stutterer, IIIIIIIIII want to stop stuttering.";
        String userStoryStringNoVerb = "As a stutterer, I wantttttttttt to stop stuttering.";
        TokenNotFoundException noSubjectException = assertThrows(TokenNotFoundException.class, () -> new UserStory(userStoryStringNoSubject).getAcceptanceCriteria(new GherkinGenerator(), false));
        assertEquals("The subject of the user story could not be identified.", noSubjectException.getMessage());
        TokenNotFoundException noVerbException = assertThrows(TokenNotFoundException.class, () -> new UserStory(userStoryStringNoVerb).getAcceptanceCriteria(new GherkinGenerator(), false));
        assertEquals("The verb of the user story could not be identified.", noVerbException.getMessage());
    }

    @Test
    public void testReusingAcceptanceCriteria() {
        assertDoesNotThrow(() -> {
            String userStoryString = "As a user I want to sleep so that I am no longer tired";
            UserStory userStory = new UserStory(userStoryString);
            List<AcceptanceCriterion> acceptanceCriteriaFirst = userStory.getAcceptanceCriteria(new GherkinGenerator(), false);
            List<AcceptanceCriterion> acceptanceCriteriaSecond = userStory.getAcceptanceCriteria(new GherkinGenerator(), false);
            assertSame(acceptanceCriteriaFirst, acceptanceCriteriaSecond);
        });
    }

}
