# jCider 

### Description
A key/value cache with full text search of value and meta field data


## Requirements

Java must be available on the server.

Currently tested and working with Java 1.8, Lucene 6.3.0, Mapdb 3.0.3, SparkJava 2.5.3, BOON JSON 0.34

See pom.xml for all dependancies


## Setup

Create folder where code will run from then edit the cider.conf file to match.

Use Maven to resolve dependancies of the project.

Edit any additional settings in the configuration file to tune the application execution to your needs.

Before running code, make sure to set the JVM appropriatly on execution  

Recommended defaults -Xmx128M -XX:MaxDirectMemorySize=4G


## Rest Interface

### Add data to cache
HTTP PUT JSON http://< host >:< port >/cache
```
  { 
    "key" : key_to_use, 
    "index": True, 
    "meta": some_meta_data, 
    "data": some_value_data
  }
```
- Key:   (required) Usually a digest, how you will pull the data back
- Index: (required) Do you want to index data and meta field
- Meta:  (optional) If index is true, will also index field, will display in search results
- Data:  (required) Data you want to store, what will be retrieved via key

### Get data to cache
HTTP GET JSON http://< host >:< port >/cache/< key >
- Key:   (required) Usually a digest, will fetch data from system

### Delete data from cache
HTTP DELETE http://< host >:< port >/cache/< key >
- Key:   (required) Usually a digest, will delete data from system

### Search index data
HTTP POST JSON http://< host >:< port >/cache
```
  {
	  "query": "data: < text search > meta: <text search>",
	  "full": false,
	  "limit": 100
  }
```
- Query: (required) Use [Lucene search syntax](https://lucene.apache.org/core/2_9_4/queryparsersyntax.html) to search via the data or meta fields
- Full:  (optional) Return key and meta description with score, or return key, meta, score, and data? Default false
- Limit: (optional) Number of search results to return. Defaults to 100, or whats preset in cider.conf file

## Other notes

See additional comments in the code base for tweaking the application

Including on how to increase stability by enabling the WAL and throughput 2X increase in throughput with Rapidiod vs SparkJava
  

## License

Apache 2.0 - see LICENSE file for complete license details

## Author Information

Created in 2017 by Calvin Schultz
