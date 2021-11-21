package de.uhd.ifi.se.acgen;

import de.uhd.ifi.se.acgen.rest.RunRest;
import de.uhd.ifi.se.acgen.rest.StatusRest;

import static spark.Spark.*;

public class App {

    public App(int port) {
        port(port);

        StatusRest statusRest = new StatusRest();
        RunRest runRest = new RunRest();
        get("/hitec/generate/acceptance-criteria/status", statusRest::createResponse);
        post("/hitec/generate/acceptance-criteria/run", runRest::createResponse);
    }

    public static void main( String[] args ) {
        new App(9696);
    }
    
}
