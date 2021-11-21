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
			App.main(new String[0]);
		}
	}

    public String baseUrl = "http://localhost:9696/hitec/generate/acceptance-criteria/";

}
