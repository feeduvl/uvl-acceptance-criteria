package de.uhd.ifi.se.acgen.rest;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.uhd.ifi.se.acgen.exception.NoUserStoryException;
import de.uhd.ifi.se.acgen.exception.SubjectNotFoundException;
import de.uhd.ifi.se.acgen.generator.GherkinGenerator;
import de.uhd.ifi.se.acgen.model.UserStory;
import de.uhd.ifi.se.acgen.model.UvlResponse;
import spark.Request;
import spark.Response;

public class RunRest {
    
    public static UvlResponse addAcceptanceCriteriaToResponse(JsonArray documents, UvlResponse response) {
        int errors = 0;
        int warnings = 0;
        for (JsonElement document : documents) {
            int userStoryNumber = document.getAsJsonObject().get("number").getAsInt();
            String userStoryText = document.getAsJsonObject().get("text").getAsString();
            try {
                UserStory userStory = new UserStory(userStoryText);
                List<String> acceptanceCriteria = userStory.getAcceptanceCriteria(new GherkinGenerator());
                for (String acceptanceCriterion : acceptanceCriteria) {
                    response.addAC(acceptanceCriterion, userStoryNumber);
                }
                if (!userStory.containsReason()) {
                    warnings += 1;
                    response.addAC("WARNING: A reason could not be found. Please make sure the reason of the user story is declared after the role and the goal using the syntax \"so that [reason]\".", userStoryNumber);
                }
            } catch (NoUserStoryException | SubjectNotFoundException e) {
                errors += 1;
                response.addAC("ERROR: " + e.getMessage(), userStoryNumber);
            }
        }
        response.addMetric("errorCount", errors);
        response.addMetric("warningCount", warnings);
        return response;
    }
    
    public Object createResponse(Request req, Response res) throws Exception {
        try {
            long start = System.currentTimeMillis();
            JsonObject jsonRequest = new Gson().fromJson(req.body(), JsonObject.class);
            res.header("Content-Type", "application/json");
            JsonArray documents = jsonRequest.get("dataset").getAsJsonObject().get("documents").getAsJsonArray();
            UvlResponse response = new UvlResponse();
            response.addMetric("count", documents.size());
            addAcceptanceCriteriaToResponse(documents, response);
            long finish = System.currentTimeMillis();
            response.addMetric("runtime", finish - start);
            return response;     
        } catch (Exception e) {
            res.status(500);
            return "<h1>500 Internal Server Error</h1>";
        }
    }

}
