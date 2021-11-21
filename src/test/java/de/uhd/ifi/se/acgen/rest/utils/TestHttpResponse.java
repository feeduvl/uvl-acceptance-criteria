package de.uhd.ifi.se.acgen.rest.utils;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;

public class TestHttpResponse {

    public static void testStatusOKAndContentJSON(HttpResponse httpResponse) {
        assertEquals(200, httpResponse.getStatusLine().getStatusCode());
        assertEquals("application/json", ContentType.getOrDefault(httpResponse.getEntity()).getMimeType());
    }
    
    public static void testStatusServerError(HttpResponse httpResponse) {
        assertEquals(500, httpResponse.getStatusLine().getStatusCode());
    }
    
}
