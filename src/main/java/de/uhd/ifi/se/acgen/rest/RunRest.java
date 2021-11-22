package de.uhd.ifi.se.acgen.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.uhd.ifi.se.acgen.exception.NoUserStoryException;
import de.uhd.ifi.se.acgen.model.UserStory;
import de.uhd.ifi.se.acgen.model.UvlResponse;
import spark.Request;
import spark.Response;

public class RunRest {
    
    public static UvlResponse splitUserStoriesInParts(JsonArray documents, UvlResponse response) {
        for (JsonElement document : documents) {
            int number = document.getAsJsonObject().get("number").getAsInt();
            String text = document.getAsJsonObject().get("text").getAsString();
            try {
                UserStory userStory = new UserStory(text);
                response.addAC(userStory.getRole(), number);
                response.addAC(userStory.getGoal(), number);
                if (userStory.containsReason()) {
                    response.addAC(userStory.getReason(), number);
                } else {
                    response.addAC("WARNING: A reason could not be found. Please make sure the reason of the user story is declared after the role and the goal using the syntax \"so that [reason]\".", number);
                }
            } catch (NoUserStoryException e) {
                response.addAC("ERROR: " + e.getMessage(), number);
            }
        }
        return response;
    }
    
    public Object createResponse(Request req, Response res) throws Exception {
        try {
            JsonObject jsonRequest = new Gson().fromJson(req.body(), JsonObject.class);
            res.header("Content-Type", "application/json");
            JsonArray documents = jsonRequest.get("dataset").getAsJsonObject().get("documents").getAsJsonArray();
            UvlResponse response = new UvlResponse();
            response.addMetric("count", documents.size());
            splitUserStoriesInParts(documents, response);
            return response;     
        } catch (Exception e) {
            res.status(500);
            return "<h1>500 Internal Server Error</h1>";
        }
    }

}
