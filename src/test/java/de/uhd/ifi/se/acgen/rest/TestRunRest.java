package de.uhd.ifi.se.acgen.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;

import de.uhd.ifi.se.acgen.TestApp;
import de.uhd.ifi.se.acgen.rest.utils.TestHttpResponse;

public class TestRunRest extends TestApp {

    @Test
    public void testRunSuccessful() throws Exception {
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("method", "acceptance-criteria");
        JsonObject params = new JsonObject();
        params.addProperty("alpha", 0.1);
        jsonRequest.add("params", params);
        JsonObject dataset = new JsonObject();
        JsonArray documents = new JsonArray();
        JsonObject document1 = new JsonObject();
        document1.addProperty("number", 1);
        document1.addProperty("text", "This is a sentence.");        
        JsonObject document2 = new JsonObject();
        document2.addProperty("number", 2);
        document2.addProperty("text", "This is also a sentence.\nThis is another sentence on a new line.");
        documents.add(document1);
        documents.add(document2);
        dataset.add("documents", documents);
        jsonRequest.add("dataset", dataset);

        HttpPost request = new HttpPost(baseUrl + "run");
        StringEntity entity = new StringEntity(jsonRequest.toString());
        request.setEntity(entity);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );

        TestHttpResponse.testStatusOKAndContentJSON(httpResponse);
    }

    @Test
    public void testRunServerError() throws Exception {
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("method", "acceptance-criteria");

        HttpPost request = new HttpPost(baseUrl + "run");
        StringEntity entity = new StringEntity(jsonRequest.toString());
        request.setEntity(entity);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );

        TestHttpResponse.testStatusServerError(httpResponse);
    }
}
