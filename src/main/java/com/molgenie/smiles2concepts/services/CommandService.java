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
package com.molgenie.smiles2concepts.services;

import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.StereoMolecule;
import com.molgenie.assignment.AssignCompounds;
import com.molgenie.assignment.AssignCompounds.AssignmentParameters;
import com.molgenie.assignment.OntologyLoader.OntologyData;
import com.molgenie.smiles2concepts.config.IClassifyerSettings;
import com.molgenie.smiles2concepts.models.Assignment;
import com.molgenie.smiles2concepts.models.SmilesRequest;
import com.molgenie.smiles2concepts.models.SmilesResponse;
import com.molgenie.smiles2concepts.models.common.ClassificationResult;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CommandService implements IService {
	private static final Logger log = LoggerFactory.getLogger(CommandService.class);
	private final IClassifyerSettings settings;
	
	@Inject
	public CommandService(IClassifyerSettings settings) {
		this.settings = settings;
	}

	/* perform smiles classification
	 * 
	 * SmilesRequest contains smiles from http post
	 * oData contain data from loaded ontology
	 */
	public ClassificationResult performClassification( SmilesRequest request, OntologyData oData ) {
		
		String inputSmiles = request.getSmiles();
		String classifier = request.getClassifierName();
		String timeOut = settings.timeOut();

		String idcode = "1";
		try {
			StereoMolecule mol = new StereoMolecule();
			int mode = SmilesParser.SMARTS_MODE_IS_SMILES | SmilesParser.MODE_SKIP_COORDINATE_TEMPLATES;
			new SmilesParser( mode, false ).parse( mol, inputSmiles.getBytes() );
			idcode = mol.getIDCode();
		} catch (Exception e) {
			log.error("could not convert smiles to OpenChemLib mol...");
		}
		
		AssignmentParameters parameters = new AssignmentParameters();
		parameters.setId(idcode);
		parameters.setSmiles(inputSmiles);
		parameters.setTimeOut(timeOut);
		
		HashMap<String,String> classOutput = new HashMap();
		try {
			classOutput = AssignCompounds.runAssignment( parameters, oData );
		} catch (Exception e) {
			log.error("could not assign smiles chemical classes...");
		}
		
		return new ClassificationResult( inputSmiles, classifier, buildSmilesResponse(classOutput) );
	}
	
	private SmilesResponse buildSmilesResponse( HashMap<String,String> assMap ) {
		//parse the output and build the response object
		ArrayList<Assignment> assignments = new ArrayList();
		for ( String classID : assMap.keySet() ) {
			Assignment ass = new Assignment();
			ass.setClassID(classID);
			ass.setClassName(assMap.get(classID));
			assignments.add(ass);
		}
		return new SmilesResponse( assignments );
	}

} 