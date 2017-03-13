package org.jcider.controllers;

import java.util.Map;
import java.util.Set;
import org.jcider.indexer.ValueIndexEngine;
import org.jcider.store.KeyStorage;
import com.google.common.collect.Sets;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;

public class WRController {

	//Required keys in JSON message
	private static Set<String> reqKeys 		= Sets.newHashSet("index", "key", "data");
	private static Set<String> reqQuery 	= Sets.newHashSet("query");
	private static String 	   okresp 		= "{\"resp\": \"ok\"}";
	private static String 	   badresp 		= "{\"resp\": \"failed\"}";
	private static String 	   nonexists 	= "{\"resp\": \"non-exists\"}";

	//Object parser for JSON Data
	private static ObjectMapper objectMapper = JsonFactory.create();

	//Start Controller
	public WRController(){
		System.out.println("Started WRController");
	}

	//Write data to KeyStorage
	public static String writeData(String obj){
		Map<String, Object> map = objectMapper.parser().parseMap(obj);
		if (map.keySet().containsAll(reqKeys)){
			KeyStorage.memStore.put(map.get("key").toString().getBytes(), obj.getBytes());
			if (map.get("index").equals(Boolean.TRUE)){
				ValueIndexEngine.documentQueue(map);
			}
			return okresp;
		}
		return badresp;
	}

	//Get data from KeyStorage
	public static String getData(String key){
		return new String(KeyStorage.memStore.getOrDefault(key.getBytes(), nonexists.getBytes()));
	}

	//Delete data from KeyStorage if exists
	public static String deleteData(String key){
		ValueIndexEngine.deleteDocument(key); //Try to delete if its exists
		KeyStorage.memStore.remove(key.getBytes());
		return okresp;
	}

	//Search Storage data via Lucene index
	public static String search(String obj) {
		Map<String, Object> map = objectMapper.parser().parseMap(obj);
		if (map.keySet().containsAll(reqQuery)){
			return objectMapper.toJson(ValueIndexEngine.search(map));
		}
		return badresp;
	}

}
