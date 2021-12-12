package de.uhd.ifi.se.acgen.rest;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.uhd.ifi.se.acgen.generator.GherkinGenerator;
import de.uhd.ifi.se.acgen.model.AcceptanceCriterion;
import de.uhd.ifi.se.acgen.model.AcceptanceCriterionType;
import de.uhd.ifi.se.acgen.model.UserStory;
import de.uhd.ifi.se.acgen.model.UvlResponse;
import spark.Request;
import spark.Response;

/**
 * The /run API endpoint class which handles requests to the /run endpoint and
 * starts the acceptance criteria generation process.
 * @see https://github.com/feeduvl/uvl-acceptance-criteria/blob/main/swagger.yaml
 * for the API documentation
 */
public class RunRest {
    
    /** 
     * Starts the acceptance criteria generation and creates a
     * {@link UvlResponse} which contains acceptance criteria, log messages and
     * metrics in the syntax required by the FeedUVL API.
     * 
     * @param documents a part of the HTTP request payload containing the user
     * stories
     * @param debug whether to include debug information in the HTTP response.
     * @return a {@link UvlResponse} object containing acceptance criteria, log
     * messages and metrics in the syntax required by the FeedUVL API.
     * 
     * @see UvlResponse
     * @see https://github.com/feeduvl/uvl-acceptance-criteria/blob/main/swagger.yaml
     * for the API documentation
     */
    public static UvlResponse addAcceptanceCriteriaToResponse(JsonArray documents, boolean debug) {
        UvlResponse response = new UvlResponse();
        int errors = 0;
        int warnings = 0;
        int infos = 0;
        for (JsonElement document : documents) { // for every user story
            int userStoryNumber = document.getAsJsonObject().get("number").getAsInt();
            String userStoryText = document.getAsJsonObject().get("text").getAsString();
            try {
                // Extract the user story from the string
                UserStory userStory = new UserStory(userStoryText);

                // Generate Gherkin acceptance criteria
                List<AcceptanceCriterion> acceptanceCriteria = userStory.getAcceptanceCriteria(new GherkinGenerator(), debug);

                for (AcceptanceCriterion acceptanceCriterion : acceptanceCriteria) {
                    response.addAcceptanceCriterion(acceptanceCriterion, userStoryNumber);
                }
                if (!userStory.containsReason()) {
                    // If the user story does not contain a reason (i.e., the
                    // “so that” part), an info message is added to the
                    // response.
                    infos += 1;
                    response.addAcceptanceCriterion(new AcceptanceCriterion("A reason could not be found. If you wish to include a reason, please make sure the reason of the user story is declared after the role and the goal using the syntax “so that [reason]”.", AcceptanceCriterionType.INFO), userStoryNumber);
                }
                if (userStory.wasCutAtListOrNote()) {
                    // If the user story was cut at a note or bullet point list
                    // and it is likely that information was lost by that,
                    // a warning message is added to the response.
                    warnings += 1;
                    response.addAcceptanceCriterion(new AcceptanceCriterion("The user story was cut at a bullet point list or a part of text starting with “\\\\”. Please refrain from using these syntaxes within a user story and make sure to end your user story with a sentence period.", AcceptanceCriterionType.WARNING), userStoryNumber);
                }
            } catch (Exception e) {
                // If an exception is thrown (e.g. if the user story could not
                // be identified), the exception message is added as an error
                // message to the response, and the next user story can be
                // processed.
                errors += 1;
                response.addAcceptanceCriterion(new AcceptanceCriterion(e.getMessage(), AcceptanceCriterionType.ERROR), userStoryNumber);
            }
        }

        // Add the log message counts to the response
        response.addMetric("errorCount", errors);
        response.addMetric("warningCount", warnings);
        response.addMetric("infoCount", infos);
        return response;
    }
    
    
    /** 
     * Creates a response for requests to the /run API and starts the
     * acceptance criteria generation process
     * 
     * @param req the HTTP request sent to the /run endpoint whose payload
     * contains a dataset of user stories and the debug parameter
     * @param res the HTTP response containing header and HTTP status code
     * information
     * @return an object used as payload for the HTTP response which contains
     * acceptance criteria, log messages and metrics in the form of a 
     * {@link UvlResponse} object on success, or an error message in the form
     * of a string if an exception is thrown and catched
     */
    public Object createResponse(Request req, Response res) {
        try {
            // Start measuring the runtime
            long start = System.currentTimeMillis();

            // Interpret the payload of the HTTP request as JSON and extract
            // the user stories (called documents) and parameters
            JsonObject jsonRequest = new Gson().fromJson(req.body(), JsonObject.class);
            JsonArray documents = jsonRequest.get("dataset").getAsJsonObject().get("documents").getAsJsonArray();
            boolean debug = jsonRequest.get("params").getAsJsonObject().get("debug").getAsBoolean();

            // Generate acceptance criteria and put them into the form required
            // by the API
            UvlResponse response = addAcceptanceCriteriaToResponse(documents, debug);

            response.addMetric("count", documents.size());

            // Stop measuring the runtime
            long finish = System.currentTimeMillis();
            response.addMetric("runtime", finish - start);
            res.header("Content-Type", "application/json");
            return response;     
        } catch (Exception e) {
            res.status(500);
            return "<h1>500 Internal Server Error</h1><code>" + e.getMessage().replaceAll("\\n", "<br>") + "</code>";
        }
    }

}
