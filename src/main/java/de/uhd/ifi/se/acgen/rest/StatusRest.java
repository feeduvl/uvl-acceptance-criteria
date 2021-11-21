package de.uhd.ifi.se.acgen.rest;

import com.google.gson.JsonObject;

import spark.Request;
import spark.Response;

public class StatusRest {

    public Object createResponse(Request req, Response res) throws Exception {
        res.header("Content-Type", "application/json");
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("status", "operational");
        return jsonResponse;
    }

}
