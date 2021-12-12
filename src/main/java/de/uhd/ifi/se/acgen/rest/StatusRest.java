package de.uhd.ifi.se.acgen.rest;

import com.google.gson.JsonObject;

import spark.Request;
import spark.Response;

/**
 * The /status API endpoint class which handles requests to the /status
 * endpoint.
 * 
 * @see <a href="https://github.com/feeduvl/uvl-acceptance-criteria/blob/main/swagger.yaml">https://github.com/feeduvl/uvl-acceptance-criteria/blob/main/swagger.yaml</a>
 * for the API documentation
 */
public class StatusRest {
    
    /** 
     * Creates a response for requests to the /status API and confirms that the
     * API is operational.
     * 
     * @param req the HTTP request sent to the /status endpoint
     * @param res the HTTP response containing header and HTTP status code
     * information
     * @return a JSON object used as payload for the HTTP response
     */
    public Object createResponse(Request req, Response res) {
        res.header("Content-Type", "application/json");
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("status", "operational");
        return jsonResponse;
    }

}
