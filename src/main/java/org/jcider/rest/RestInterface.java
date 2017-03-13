package org.jcider.rest;

import static spark.Spark.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.jcider.controllers.WRController;
import org.jcider.indexer.ValueIndexEngine;
import org.jcider.store.KeyStorage;


public class RestInterface {

	//Initialize Sub System
	static       int 					    servicePort;
	static final KeyStorage 	    ks  = new KeyStorage();
	static final ValueIndexEngine vie = new ValueIndexEngine();

	public static void main( String[] args ){

		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("cider.conf"));
			servicePort = Integer.parseInt(prop.getProperty("servicePort"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

    	System.out.println("Starting web server on port: " + servicePort);
    	port(servicePort);
    	/*
    	* Java Spark is ok for proof of concept, but http://www.rapidoid.org/ is 50%+ faster if your going to stick with http proto
    	* Adding ANYTHING creates overhead, the fact we are using http creates overhead, use a different protocol to reduce overhead
    	* Using Rapidoid I seen test benchmarks up to ~98k/sec reads & ~75k/sec inserts, 2x what I was able to achieve with the Spark framework
    	* However the Spark framework was easier to implement.  If you desire to fork and switch to Rapidoid, the conversion is simple.
    	*
    	* User input is only lightly validated, its not robust WHAT SO EVER, see comment on overhead
    	* In that same vein, there is NO authentication mechanism built into this code base
    	* This is NOT production ready
    	*
    	* If Cider dies, any items waiting in the Lucene queue will be lost
    	* WAL is disabled for MapDB, feel free to add it (its one line, see documentation for Mapdb).  Will provide better redundancy if system dies for key store
    	*
    	* Remember to set -Xmx128M to reduce max head size, and increased memory allocation -XX:MaxDirectMemorySize=4G (up to you to tune params, see configuration)
    	*
    	* See Lucene documentation for query syntax
    	* https://lucene.apache.org/core/2_9_4/queryparsersyntax.html#Fuzzy Searches
    	*
    	*/

		//Get Key Value
		get("/cache/:key", (req, res)->{
			res.type("application/json");
			return WRController.getData(req.params(":key"));
		});

		//Put Key Value
		put("/cache", (req, res)->{
			res.type("application/json");
			return WRController.writeData(req.body());
		});

		//Delete Key Value
		delete("/cache/:key", (req, res)->{
			res.type("application/json");
			return WRController.deleteData(req.params(":key"));
		});

		//Search for data in K/V space via values in "data" and/or "meta" data, advanced querying use Lucene query language
		post("/search", (req,res)->{
			res.type("application/json");
			return WRController.search(req.body());
		});

    }
}
