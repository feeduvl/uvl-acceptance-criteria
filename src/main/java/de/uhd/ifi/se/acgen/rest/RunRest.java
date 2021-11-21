package de.uhd.ifi.se.acgen.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.uhd.ifi.se.acgen.model.uvlResponse;
import spark.Request;
import spark.Response;

public class RunRest {
    
    public static uvlResponse splitDocumentsInWords(JsonArray documents, uvlResponse response) {
        for (JsonElement document : documents) {
            int number = document.getAsJsonObject().get("number").getAsInt();
            String text = document.getAsJsonObject().get("text").getAsString();
            String[] words = text.split("\n");
            for (String word : words) {
                response.addAC(word, number);
            }
        }
        return response;
    }
    
    public Object createResponse(Request req, Response res) throws Exception {
        try {
            JsonObject jsonRequest = new Gson().fromJson(req.body(), JsonObject.class);
            res.header("Content-Type", "application/json");
            JsonArray documents = jsonRequest.get("dataset").getAsJsonObject().get("documents").getAsJsonArray();
            uvlResponse response = new uvlResponse();
            response.addMetric("count", documents.size());
            splitDocumentsInWords(documents, response);
            return response;     
        } catch (Exception e) {
            res.status(500);
            return "<h1>500 Internal Server Error</h1>";
        }
    }

}
