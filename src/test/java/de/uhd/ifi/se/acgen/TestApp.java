package de.uhd.ifi.se.acgen;

import java.net.Socket;

import org.junit.BeforeClass;

public class TestApp {
    
	@BeforeClass
	public static void init() {
		try {
			(new Socket("localhost", 9696)).close();
		}
		catch(Exception e) {
			new App(9696);
		}
	}

    public String baseUrl = "http://localhost:9696/hitec/generate/acceptance-criteria/";

}
