package de.uhd.ifi.se.acgen;

import java.net.Socket;

import org.junit.jupiter.api.BeforeAll;

public class TestApp {
    
    public String baseUrl = "http://localhost:9696/hitec/generate/acceptance-criteria/";

	@BeforeAll
	public static void init() {
		try {
			(new Socket("localhost", 9696)).close();
		}
		catch(Exception e) {
			App.main(new String[0]);
		}
	}

}
