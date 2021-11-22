package de.uhd.ifi.se.acgen.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import de.uhd.ifi.se.acgen.TestApp;
import de.uhd.ifi.se.acgen.rest.util.TestHttpResponseHelper;

public class TestStatusRest extends TestApp {

    @Test
    public void testStatus() throws Exception {
        HttpUriRequest request = new HttpGet(baseUrl + "status");
        HttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );

        assertTrue(TestHttpResponseHelper.testStatusOKAndContentJSON(httpResponse));
        JsonObject expectedResponseBody = new JsonObject();
        expectedResponseBody.addProperty("status", "operational");
        assertEquals(expectedResponseBody, new Gson().fromJson(EntityUtils.toString(httpResponse.getEntity()), JsonObject.class));
    }
}
