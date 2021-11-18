package de.uhd.ifi.se.acgen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static spark.Spark.*;

public class App 
{
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
        post("/hitec/generate/acceptance-criteria/run", (request, response) -> {
            System.out.println("Run triggered.");
            try {
                JsonObject jsonRequest = new Gson().fromJson(request.body(), JsonObject.class);
                response.header("Content-Type", "application/json");
                JsonArray documents = jsonRequest.get("dataset").getAsJsonObject().get("documents").getAsJsonArray();
                JsonObject jsonResponse = new JsonObject();
                JsonObject topics = new JsonObject();
                JsonObject doc_topic = new JsonObject();
                JsonObject metrics = new JsonObject();

                metrics.addProperty("count", documents.size());

                jsonResponse.add("topics", topics);
                jsonResponse.add("doc_topics", doc_topic);
                jsonResponse.add("metrics", metrics);
                return jsonResponse;     
            } catch (Exception e) {
                response.status(500);
                return "<h1>500 Internal Server Error</h1>";
            }
        });
    }
}
