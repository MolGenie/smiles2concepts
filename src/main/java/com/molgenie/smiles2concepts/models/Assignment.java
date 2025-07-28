package com.molgenie.smiles2concepts.models;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"classID",
	"className"
})
public class Assignment {
	@JsonProperty("classID")
	private String classID ;
	@JsonProperty("className")
	private String className;
	
	@JsonProperty("classID")
	public String getClassID() {
		return classID;
	}
	@JsonProperty("classID")
	public void setClassID(String classID) {
		this.classID = classID; 
	}
	
	@JsonProperty("className")
	public String getClassName() {
		return className;
	}
	@JsonProperty("className")
	public void setClassName(String className) {
		this.className = className; 
	}
  
}
