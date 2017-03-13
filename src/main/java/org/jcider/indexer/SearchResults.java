package org.jcider.indexer;

import java.util.ArrayList;
import java.util.List;


public class SearchResults {
	// - search query issued
	private String query = "";
	
	// - number of results found
	private Integer count = 0;
	
	// - results found
	private List<ResultObject> results = new ArrayList<ResultObject>();
	
	// - result limited to x count
	private Integer limit;

	
	public Integer getLimit() {
		return this.limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public String getQuery() {
		return this.query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Integer getCount() {
		return this.count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}
	
	public List<ResultObject> getResults() {
		return this.results;
	}

	public void addResult(ResultObject object) {
		this.results.add(object);
	}
}
