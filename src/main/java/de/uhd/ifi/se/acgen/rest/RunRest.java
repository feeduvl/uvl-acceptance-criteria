package de.uhd.ifi.se.acgen.rest;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.uhd.ifi.se.acgen.generator.GherkinGenerator;
import de.uhd.ifi.se.acgen.model.UserStory;
import de.uhd.ifi.se.acgen.model.UvlResponse;
import spark.Request;
import spark.Response;

public class RunRest {
    
    public static UvlResponse addAcceptanceCriteriaToResponse(JsonArray documents, UvlResponse response, boolean debug) {
        int errors = 0;
        int warnings = 0;
        int infos = 0;
        for (JsonElement document : documents) {
            int userStoryNumber = document.getAsJsonObject().get("number").getAsInt();
            String userStoryText = document.getAsJsonObject().get("text").getAsString();
            try {
                UserStory userStory = new UserStory(userStoryText);
                List<String> acceptanceCriteria = userStory.getAcceptanceCriteria(new GherkinGenerator(), debug);
                for (String acceptanceCriterion : acceptanceCriteria) {
                    response.addAC(acceptanceCriterion, userStoryNumber);
                }
                if (!userStory.containsReason()) {
                    infos += 1;
                    response.addAC("INFO: A reason could not be found. If you wish to include a reason, please make sure the reason of the user story is declared after the role and the goal using the syntax “so that [reason]”.", userStoryNumber);
                }
                if (userStory.wasCutAtListOrNote()) {
                    warnings += 1;
                    response.addAC("WARNING: The user story was cut at a bullet point list or a part of text starting with “\\\\”. Please refrain from using these syntaxes within a user story and make sure to end your user story with a sentence period.", userStoryNumber);
                }
            } catch (Exception e) {
                errors += 1;
                response.addAC("ERROR: " + e.getMessage(), userStoryNumber);
            }
        }
        response.addMetric("errorCount", errors);
        response.addMetric("warningCount", warnings);
        response.addMetric("infoCount", infos);
        return response;
    }
    
    public Object createResponse(Request req, Response res) throws Exception {
        try {
            long start = System.currentTimeMillis();
            JsonObject jsonRequest = new Gson().fromJson(req.body(), JsonObject.class);
            res.header("Content-Type", "application/json");
            JsonArray documents = jsonRequest.get("dataset").getAsJsonObject().get("documents").getAsJsonArray();
            boolean debug = jsonRequest.get("params").getAsJsonObject().get("debug").getAsBoolean();
            UvlResponse response = new UvlResponse();
            response.addMetric("count", documents.size());
            addAcceptanceCriteriaToResponse(documents, response, debug);
            long finish = System.currentTimeMillis();
            response.addMetric("runtime", finish - start);
            return response;     
        } catch (Exception e) {
            res.status(500);
            return "<h1>500 Internal Server Error</h1>";
        }
    }

}
