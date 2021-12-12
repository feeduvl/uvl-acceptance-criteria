package de.uhd.ifi.se.acgen;

import de.uhd.ifi.se.acgen.rest.RunRest;
import de.uhd.ifi.se.acgen.rest.StatusRest;

import static spark.Spark.*;

/**
 * The main class of the application which starts a server and creates the
 * API endpoint listeners.
 * 
 * @see RunRest
 * @see StatusRest
 * @see https://github.com/feeduvl/uvl-acceptance-criteria/blob/main/swagger.yaml
 * for the API documentation
 */
public class App {

    /**
     * The constructor of the {@link App} class containing the API endpoint
     * definitions.
     * 
     * @param port The port the server is listening to
     */
    public App(int port) {
        port(port);

        StatusRest statusRest = new StatusRest();
        RunRest runRest = new RunRest();
        get("/hitec/generate/acceptance-criteria/status", statusRest::createResponse);
        post("/hitec/generate/acceptance-criteria/run", runRest::createResponse);
    }

    
    /** 
     * The main method of the {@link App} which starts a server listening to
     * port 9696.
     * 
     * @param args The command line arguments passed to the application
     */
    public static void main( String[] args ) {
        new App(9696);
    }
    
}
