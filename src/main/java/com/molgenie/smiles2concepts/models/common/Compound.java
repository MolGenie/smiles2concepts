/*
* Copyright MolGenie GmbH, 
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* 
*/
package com.molgenie.smiles2concepts.models.common;

public class Compound {
	String smiles 		= null;
	float confidence 	= 0;
	int resolution 		= 0;
	String image 		= null;
	int page 			= 0;
	String position 	= null;
	String idCode 		= null;
	String inchi 		= null;
	String inchiKey 	= null;
	
	public String getSmiles() {
		return smiles;
	}
	public void setSmiles(String _smiles) {
		this.smiles = _smiles; 
	}
	
	public float getConfidence() {
		return confidence;
	}
	public void setConfidence(float _conf) {
		this.confidence = _conf;
	}
	
	public Integer getResolution() {
		return resolution;
	}
	public void setResolution(int _resolution) {
		this.resolution = _resolution;
	}
	
	public String getImage() {
		return image;
	}
	public void setImage(String _image) {
		this.image = _image;
	}
	
	public Integer getPage() {
		return page;
	}
	public void setPage(int _page) {
		this.page = _page;
	}
	
	public String getPosition() {
		return position;
	}
	public void setPosition(String _position) {
		this.position = _position;
	}
	
	public String getIDCode() {
		return idCode;
	}
	public void setIDCode(String _idcode) {
		this.idCode = _idcode;
	}
	
	public String getInchi() {
		return inchi;
	}
	public void setInchi(String _inchi) {
		this.inchi = _inchi;
	}
	
	public String getInchiKey() {
		return inchiKey;
	}
	public void setInchiKey(String _inchiKey) {
		this.inchiKey = _inchiKey;
	}
	
}
