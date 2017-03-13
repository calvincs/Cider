package org.jcider.indexer;

import java.util.Map;

public class ResultObject {
	

	private String key;
	private Map<String, Object> object;
	private String meta;
	private Float  score;
	
	public String getKey() {
		return this.key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public Map<String, Object> getobject() {
		return this.object;
	}
	public void setobject(Map<String, Object>kvo) {
		this.object = kvo;
	}
	public String getMeta() {
		return this.meta;
	}
	public void setMeta(String meta) {
		this.meta = meta;
	}
	public Float getScore() {
		return this.score;
	}
	public void setScore(Float score) {
		this.score = score;
	}

}
