package de.uhd.ifi.se.acgen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Stores the payload of the HTTP response, including acceptance criteria and
 * metrics, in the syntax required by the Feed UVL API.
 * 
 * @see <a href="https://github.com/feeduvl/uvl-acceptance-criteria/blob/main/swagger.yaml">https://github.com/feeduvl/uvl-acceptance-criteria/blob/main/swagger.yaml</a>
 * for the API documentation
 */
public class UvlResponse {
    
    /**
     * A JSON object storing the acceptance criteria.
     */
    JsonObject acceptanceCriteria;

    /**
     * A JSON object associating user stories with the acceptance criteria.
     */
    JsonObject userStoryAcceptanceCriteriaIdMap;

    /**
     * A JSON object storing the computed metrics.
     */
    JsonObject metrics;

    /**
     * The constructor of the {@link UvlResponse} initializing the member
     * variables with empty JSON objects.
     */
    public UvlResponse() {
        acceptanceCriteria = new JsonObject();
        userStoryAcceptanceCriteriaIdMap = new JsonObject();
        metrics = new JsonObject();
    };

    
    /** 
     * Creates a JSON object from all member variables with the names required
     * by the Feed UVL API.
     * 
     * @return a JSON object containing all member variables
     * 
 * @see <a href="https://github.com/feeduvl/uvl-acceptance-criteria/blob/main/swagger.yaml">https://github.com/feeduvl/uvl-acceptance-criteria/blob/main/swagger.yaml</a>
     * for the API documentation
     */
    public JsonObject toJson() {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.add("topics", this.acceptanceCriteria);
        jsonResponse.add("doc_topic", this.userStoryAcceptanceCriteriaIdMap);
        jsonResponse.add("metrics", this.metrics);
        return jsonResponse;
    }

    
    /** 
     * Creates a string containing the {@link UvlResponse} in JSON format as
     * created by the {@code toJson()} method.
     * 
     * @return a string containing the {@link UvlResponse} in JSON format
     */
    public String toString() {
        return this.toJson().toString();
    }

    
    /** 
     * Counts the acceptance criteria that are currently stored for a user
     * story.
     * 
     * @param usNumber the ID of the user story
     * @return the number of acceptance criteria stored for the user story
     */
    private int getAcceptanceCriteriaCountOfUserStory(int usNumber) {
        try {
            JsonArray acceptanceCriteriaOfUserStory = this.userStoryAcceptanceCriteriaIdMap.get(Integer.toString(usNumber)).getAsJsonArray();
            return acceptanceCriteriaOfUserStory.size();
        } catch (Exception e) {
            // if there are no acceptance criteria stored for the user story
            return -1;
        }
    }

    
    /** 
     * Adds an acceptance criterion to the response and associates it with a
     * user story.
     * 
     * @param acceptanceCriterion the acceptance criterion to be stored
     * @param usNumber the ID of the corresponding user story
     */
    public void addAcceptanceCriterion(AcceptanceCriterion acceptanceCriterion, int usNumber) {
        JsonArray acceptanceCriterionAsArray = new JsonArray();
        acceptanceCriterionAsArray.add(acceptanceCriterion.toString());
        int acceptanceCriteriaCountOfUserStory = getAcceptanceCriteriaCountOfUserStory(usNumber);

        // if there are no acceptance criteria stored for the user story, an
        // entry in the ID map must be created first
        if (acceptanceCriteriaCountOfUserStory < 0) {
            this.userStoryAcceptanceCriteriaIdMap.add(Integer.toString(usNumber), new JsonArray());
            acceptanceCriteriaCountOfUserStory = 0;
        }

        // The acceptance criterion is assigned an index which allows for the
        // inference of the ID of the user story it is associated to using bit-
        // shifting
        int acceptanceCriterionIndex = (usNumber << 16) + acceptanceCriteriaCountOfUserStory;

        // Add the acceptance criterion to the acceptance criteria object
        this.acceptanceCriteria.add(Integer.toString(acceptanceCriterionIndex), acceptanceCriterionAsArray);

        // Add an entry to the ID map associating user stories with acceptance
        // criteria
        JsonArray acIndexArray = new JsonArray();
        acIndexArray.add(acceptanceCriterionIndex);
        acIndexArray.add(1); // a value describing the association “strength”,
                             // currently not in use and defaulted to 1.
        this.userStoryAcceptanceCriteriaIdMap.get(Integer.toString(usNumber)).getAsJsonArray().add(acIndexArray);
    }

    
    /** 
     * Adds a metric to the metric object
     * 
     * @param string the name of the metric
     * @param value the metric value
     */
    public void addMetric(String string, long value) {
        metrics.addProperty(string, value);
    }
}
