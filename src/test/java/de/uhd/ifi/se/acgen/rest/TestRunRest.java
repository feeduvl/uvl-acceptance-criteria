package de.uhd.ifi.se.acgen.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;

import de.uhd.ifi.se.acgen.TestApp;
import de.uhd.ifi.se.acgen.rest.util.TestHttpResponseHelper;

public class TestRunRest extends TestApp {

    @Test
    public void testRunSuccessful() {
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("method", "acceptance-criteria");
        JsonObject params = new JsonObject();
        params.addProperty("debug", false);
        jsonRequest.add("params", params);
        JsonObject dataset = new JsonObject();
        JsonArray documents = new JsonArray();
        JsonObject document1 = new JsonObject();
        document1.addProperty("number", 1);
        document1.addProperty("text", "As a developer, I want to write a user story without a reason.");        
        JsonObject document2 = new JsonObject();
        document2.addProperty("number", 2);
        document2.addProperty("text", "As a developer, I want to write a user story with a reason so that I can test my API.");
        JsonObject document3 = new JsonObject();
        document3.addProperty("number", 3);
        document3.addProperty("text", "This is not a user story.");
        JsonObject document4 = new JsonObject();
        document4.addProperty("number", 4);
        document4.addProperty("text", "As a developer, I want to write a user story with a list so that I can\n* test my API\n* interrupt the user story\n* screw everything up");
        documents.add(document1);
        documents.add(document2);
        documents.add(document3);
        documents.add(document4);
        dataset.add("documents", documents);
        jsonRequest.add("dataset", dataset);

        assertDoesNotThrow(() -> {
            HttpPost request = new HttpPost(baseUrl + "run");
            StringEntity entity = new StringEntity(jsonRequest.toString());
            request.setEntity(entity);
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );
            assertTrue(TestHttpResponseHelper.testStatusOKAndContentJSON(httpResponse));
        });
    }

    @Test
    public void testRunServerError() {
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("method", "acceptance-criteria");
        
        assertDoesNotThrow(() -> {
            HttpPost request = new HttpPost(baseUrl + "run");
            StringEntity entity = new StringEntity(jsonRequest.toString());
            request.setEntity(entity);
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );

            assertTrue(TestHttpResponseHelper.testStatusServerError(httpResponse));
        });
    }
}
