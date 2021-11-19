package de.uhd.ifi.se.acgen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.uhd.ifi.se.acgen.model.Response;

import static spark.Spark.*;

public class App 
{

    public static Response splitDocumentsInWords(JsonArray documents, Response response) {
        for (JsonElement document : documents) {
            int number = document.getAsJsonObject().get("number").getAsInt();
            String text = document.getAsJsonObject().get("text").getAsString();
            String[] words = text.split(" ");
            for (String word : words) {
                response.addAC(word, number);
            }
        }
        return response;
    }
    public static void main( String[] args )
    {
        port(9696);
        
        get("/hitec/generate/acceptance-criteria/status", (request, response) -> {
            System.out.println("Status triggered.");
            response.header("Content-Type", "application/json");
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("status", "operational");
            return jsonResponse;
        });
        post("/hitec/generate/acceptance-criteria/run", (req, res) -> {
            System.out.println("Run triggered.");
            try {
                JsonObject jsonRequest = new Gson().fromJson(req.body(), JsonObject.class);
                res.header("Content-Type", "application/json");
                JsonArray documents = jsonRequest.get("dataset").getAsJsonObject().get("documents").getAsJsonArray();
                Response response = new Response();
                response.addMetric("count", documents.size());
                splitDocumentsInWords(documents, response);
                System.out.println(response.toString());
                return response;     
            } catch (Exception e) {
                res.status(500);
                System.out.println("<h1>500 Internal Server Error</h1>");
                return "<h1>500 Internal Server Error</h1>";
            }
        });
    }
}
