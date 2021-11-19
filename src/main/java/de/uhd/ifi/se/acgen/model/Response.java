package de.uhd.ifi.se.acgen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Response {
    JsonObject ac;
    JsonObject us_ac;
    JsonObject metrics;

    public Response() {
        ac = new JsonObject();
        us_ac = new JsonObject();
        metrics = new JsonObject();
    };

    public Response(JsonObject _ac, JsonObject _us_ac, JsonObject _metrics) {
        ac = _ac;
        us_ac = _us_ac;
        metrics = _metrics;
    };

    public JsonObject getAC() {
        return this.ac;
    }

    public JsonObject getUSAC() {
        return this.us_ac;
    }

    public JsonObject getMetrics() {
        return this.metrics;
    }

    public JsonObject toJson() {
        JsonObject ret = new JsonObject();
        ret.add("topics", this.ac);
        ret.add("doc_topic", this.us_ac);
        ret.add("metrics", this.metrics);
        return ret;
    }

    public String toString() {
        return this.toJson().toString();
    }

    public void setAC(JsonObject _ac) {
        this.ac = _ac;
    }

    public void setUSAC(JsonObject _us_ac) {
        this.us_ac = _us_ac;
    }

    public void setMetrics(JsonObject _metrics) {
        this.metrics = _metrics;
    }

    public int getACCountOfUS(int usNumber) {
        try {
            JsonArray ACOfUS = this.us_ac.get(Integer.toString(usNumber)).getAsJsonArray();
            return ACOfUS.size();
        } catch (Exception e) {
            return -1;
        }
    }

    public void addAC(String ac, int usNumber) {
        JsonArray acAsArray = new JsonArray();
        acAsArray.add(ac);
        int ACCountOfUS = getACCountOfUS(usNumber);
        if (ACCountOfUS < 0) {
            this.us_ac.add(Integer.toString(usNumber), new JsonArray());
            ACCountOfUS = 0;
        }
        int acIndex = (usNumber << 16) + ACCountOfUS;
        this.ac.add(Integer.toString(acIndex), acAsArray);
        JsonArray acIndexArray = new JsonArray();
        acIndexArray.add(acIndex);
        acIndexArray.add(1);
        this.us_ac.get(Integer.toString(usNumber)).getAsJsonArray().add(acIndexArray);
    }

    public void addMetric(String string, int value) {
        metrics.addProperty(string, value);
    }
}
