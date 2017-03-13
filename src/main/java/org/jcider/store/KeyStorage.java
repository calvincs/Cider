package org.jcider.store;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.mapdb.*;

public class KeyStorage {
	//Properties
	static String 	applicationPath;
	static int 		startSizeAllocation;
	static int 		allocateIncrement;
	static int 		concurrencyScaleDisk;
	static int 		expireAfterCreateMS;
	static int 		concurrencyScaleMem;
	static float 	compactionThreshold;
	static int   	expireAfterGetMem;
	static boolean  countDbOnStartup;

	//Storage
	static  DB diskStoreInstance;
	static  DB memoryStoreInstance;

	//Maps used to store
	public static ConcurrentMap<byte[], byte[]> keyStore;
	public static ConcurrentMap<byte[], byte[]> memStore;

	public KeyStorage(){
		/*
		 * Setup storage
		 * Remember to set -Xmx128M to reduce max head size, and increased memory allocation -XX:MaxDirectMemorySize=4G
		 */
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("cider.conf"));
			applicationPath			  = prop.getProperty("applicationPath");
			startSizeAllocation 	= Integer.parseInt(prop.getProperty("startSizeAllocation"))  * 1024 * 1024;
			allocateIncrement 		= Integer.parseInt(prop.getProperty("allocateIncrement"))    * 1024 * 1024;
			concurrencyScaleDisk 	= Integer.parseInt(prop.getProperty("concurrencyScaleDisk"));
			expireAfterCreateMS		= Integer.parseInt(prop.getProperty("expireAfterCreateMS"));
			concurrencyScaleMem 	= Integer.parseInt(prop.getProperty("concurrencyScaleMem"));
			compactionThreshold  	= Float.parseFloat(prop.getProperty("compactionThreshold"));
			expireAfterGetMem		  = Integer.parseInt(prop.getProperty("expireAfterGetMem"));
			countDbOnStartup		  = Boolean.valueOf(prop.getProperty("countDbOnStartup"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//Disk Storage
		diskStoreInstance = DBMaker
				.fileDB(applicationPath + "jCiderKeyStorage.db")
				.fileChannelEnable()
				.concurrencyScale(concurrencyScaleDisk)
				.fileMmapEnableIfSupported()
				.fileMmapPreclearDisable()
				.cleanerHackEnable()
				.allocateStartSize( startSizeAllocation )
				.allocateIncrement( allocateIncrement  )
				.make();

		//In memory Storage
		memoryStoreInstance = DBMaker
				.memoryDirectDB()
				.concurrencyScale(concurrencyScaleMem)
				.make();

		//Storage Maps
		keyStore = diskStoreInstance.hashMap("diskStore", Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY)
				.createOrOpen();

		memStore = memoryStoreInstance.hashMap("diskStore", Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY)
				.expireAfterCreate(expireAfterCreateMS, TimeUnit.MILLISECONDS)
				.expireCompactThreshold(compactionThreshold)
				.expireAfterUpdate()
				.expireAfterGet(expireAfterGetMem, TimeUnit.MILLISECONDS)
				.expireOverflow(keyStore)
				.createOrOpen();

		//Startup messages
		System.out.println("Starting KeyStorageEngine");
		System.out.println("May take awhile if your kv database is large, please be patient");
		diskStoreInstance.getStore().fileLoad();
		if(countDbOnStartup){
			System.out.println("Fetching volume size of database");
			System.out.println("Volume size: " + keyStore.size());
		}

		//Shutdown watch
		Runtime.getRuntime().addShutdownHook(new signalExit());
	}

	static class signalExit extends Thread {
		public void run() {
			System.out.println("System is shutting down, closing database");
			if (diskStoreInstance.isClosed() == false){
				diskStoreInstance.commit();
				diskStoreInstance.close();
			}
		}
    }

}
