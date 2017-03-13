package org.jcider.indexer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.eclipse.jetty.util.ConcurrentArrayQueue;
import org.jcider.store.KeyStorage;


public class ValueIndexEngine {
	//Properties
	static String 	applicationPath;
	static int 		  ramBufferSizeMB;
	static int 		  cycleDelay;
	static int 		  defaultResultLimit;

	//Document indexer
	private static StandardAnalyzer 	analyzer;
	private static IndexWriterConfig 	config;
	private static Directory 			    index;
	private static IndexWriter 			  writer;

	//Queue Object for indexing
	private static ScheduledExecutorService 					        executor;
	private static ConcurrentArrayQueue<Map<String, Object>> 	docQueue;

	//Object parser for JSON Data
	private static ObjectMapper  objectMapper;

	//Other statuses
	private static byte []       nonexists 	= "{\"resp\": \"non-exists\"}".getBytes();

	//ValueIndexEngine class
	public ValueIndexEngine() {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("cider.conf"));
			applicationPath		  = prop.getProperty("applicationPath");
			ramBufferSizeMB		  = Integer.parseInt(prop.getProperty("ramBufferSizeMB"));
			cycleDelay			    = Integer.parseInt(prop.getProperty("cycleDelay"));
			defaultResultLimit	= Integer.parseInt(prop.getProperty("defaultResultLimit"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//Initialize
		analyzer 		  = new StandardAnalyzer();
		executor 		  = Executors.newScheduledThreadPool(2);
		docQueue 		  = new ConcurrentArrayQueue<>();
		objectMapper 	= JsonFactory.create();
		try {
			index  = FSDirectory.open(Paths.get(applicationPath));
			config = new IndexWriterConfig(analyzer);
			writer = new IndexWriter(index, config);
			config.setOpenMode(OpenMode.CREATE_OR_APPEND);
			config.setRAMBufferSizeMB(ramBufferSizeMB);
			writer.commit();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//Handle JVM shutdown on exit signal
		Runtime.getRuntime().addShutdownHook(new signalExit());

		//Setup Queue processor for incoming documents
		Runnable qtask = () -> { processDocumentQueue(); };

		// Process documents until completed, then pause for X seconds before re-checking and processing the queue
		executor.scheduleWithFixedDelay(qtask, 0, cycleDelay, TimeUnit.MICROSECONDS);
	}

	//Handle shutdown signal
	static class signalExit extends Thread {
		public void run() {
			System.out.println("System is shutting down, closing Lucene index");
			closeWritter();
		}
    }

	//Document Queue processor
	private static void processDocumentQueue() {
		try{

			for(Iterator<Map<String, Object>> apkg = docQueue.iterator(); apkg.hasNext();){
				//Get next item in queue
				Map<String, Object> pkg = apkg.next();

				//Add the document to indexer
				try {
					Document doc = new Document();
					doc.add(new StringField("key", pkg.get("key").toString(), Field.Store.YES));
					doc.add(new TextField("data",  pkg.get("data").toString(), Field.Store.NO));
					if (pkg.containsKey("meta")){
						doc.add(new TextField("meta", pkg.get("meta").toString(), Field.Store.YES));
					}
					writer.updateDocument(new Term("key", pkg.get("key").toString()), doc);
				} catch (Exception e){
					e.printStackTrace();
				} finally {
					//No matter what, we are ditching this entry
					docQueue.remove(pkg);
				}
			}

			//commit changes to index
			if (writer.isOpen()){
				writer.commit();
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	//Document queue to be indexed
	public static void documentQueue(Map<String, Object> obj){
			docQueue.add(obj);
	}

	//Document deletion function
	public static void deleteDocument(String key) {
		try {
			writer.deleteDocuments(new Term("key", key));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Query meta and data indexes, depending on given query
	public static SearchResults search(Map<String, Object> obj){
		SearchResults sro 	= new SearchResults();
		try{
			IndexSearcher searcher 	= new IndexSearcher(DirectoryReader.open(index));
			QueryParser qparser 	  = new QueryParser("data", analyzer);

			//Get Limit setting
			int resultLimit 				= (int) obj.getOrDefault("limit", defaultResultLimit);
			TopScoreDocCollector collector 	= TopScoreDocCollector.create(resultLimit);
			sro.setLimit(resultLimit);

			//Search and collect
			Query fquery = qparser.parse((String) obj.get("query"));
			searcher.search(fquery, collector);
			ScoreDoc[] results = collector.topDocs().scoreDocs;

			//Take results and build return object
			for (int i = 0; i<results.length; i++){
				ResultObject ro 	= new ResultObject();
				int docId 			  = results[i].doc;
				Document rdoc 		= searcher.doc(docId);
				String resultKey 	= rdoc.get("key");

				//Set fields of result object, how are we returning results
				// - Do we want to also return the data associated to each key?
				if((Boolean) obj.getOrDefault("full", true)){
					byte[] xdata = KeyStorage.memStore.getOrDefault(resultKey.getBytes(), nonexists);
					if (xdata.equals(nonexists)){
						ValueIndexEngine.deleteDocument(resultKey);
					}
					Map<String, Object> map = objectMapper.parser().parseMap(xdata);
					ro.setobject(map);
					ro.setScore(results[i].score);
				} else {
					ro.setKey(resultKey);
					ro.setScore(results[i].score);
					// - Do we have meta data to include?
					if (rdoc.getFields().toString().contains("meta")){
						ro.setMeta(rdoc.get("meta"));
					}
				}
				//Add ResultObject to SearchResults object
				sro.addResult(ro);
			}
			sro.setCount(results.length);
			sro.setQuery(fquery.toString());
			DirectoryReader.open(index).close();
		} catch(Exception e){
			e.printStackTrace();
		}
		return sro;
	}

	//Ensure the writer is closed when the JVM is shutdown
	public static void closeWritter() {
		try {
			if(writer.isOpen() == true){
				if (writer.hasUncommittedChanges() == true || docQueue.isEmpty() == false ){
					System.out.println("Writer has uncommited changes yet to be written...");
				}
				int counter = 0;
				while (writer.hasUncommittedChanges() == true || docQueue.isEmpty() == false ){
					try {
						counter++;
						System.out.println(docQueue.size() + " Items in queue waiting to be processed");
						//Give queue time to process events 2 min, then kill queue
						Thread.sleep(1000);
						if(counter >= 120){
							System.out.println("Unable to clear queue before exit from application, indexed data may have been lost");
							break;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				writer.close();
			}
		} catch (IOException e) {
				e.printStackTrace();
		}
	}


}
