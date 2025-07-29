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
package com.molgenie.smiles2concepts;

import com.molgenie.assignment.AssignCompounds;
import com.molgenie.assignment.AssignCompounds.AssignmentParameters;
import com.molgenie.assignment.OntologyLoader.OntologyData;
import com.molgenie.assignment.SdfLoader;
import com.molgenie.smiles2concepts.config.*;
import com.molgenie.smiles2concepts.models.*;
import com.molgenie.smiles2concepts.models.common.ClassificationResult;
import com.molgenie.smiles2concepts.models.common.ErrorResponse;
import com.molgenie.smiles2concepts.services.IService;

import io.javalin.Javalin;
import java.io.IOException;
import java.util.HashMap;

import org.int4.dirk.api.InstanceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	
	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	public static void main( String[] args ) throws IOException {
		
		var settings = AppPropertiesLoader.load();
		var resolver = DiBuilder.build(settings);

		if (args.length > 0) {
			// run and quit after processing the command line arguments
			executeCommand( args, resolver, settings );
		} else {
			// run api server and wait for requests to come in
			var app = Api.create( settings );
			configureEndpoints( app, resolver, settings );
			app.start();
		}
	}

	private static void executeCommand(String[] args, InstanceResolver resolver, AppProperties settings) throws IOException {
		//When running as command line app, adjust logging
		System.setProperty("logging.file.name", ""); // Disable file logging
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		root.setLevel(ch.qos.logback.classic.Level.ERROR);

		switch (args[0]) {
			case "-h", "--help" -> {
				System.out.println("Usage: java -jar smiles2concepts.jar [options]");
				System.out.println("Options:");
				System.out.println("  -h, --help     	Show this help message");
				System.out.println("  -i <smiles> 	 	Process single smiles");
				System.out.println("  -sdf <filename>  	Process SDF file");
				System.out.println("  <NO OPTIONS>   	Run the API server and wait for requests");
				System.exit(0);
			}
			case "-i" -> {
				if (args.length < 2) {
					System.out.println("Error: Missing input smiles");
					System.exit(1);
				}
				var smiles = args[1];
				try {
					com.actelion.research.chem.StereoMolecule mol = new com.actelion.research.chem.StereoMolecule();
					com.actelion.research.chem.SmilesParser sP = new com.actelion.research.chem.SmilesParser();
					sP.parse(mol,smiles.getBytes());
				} catch (Exception e) {
					System.err.println("Error: invalid smiles " + smiles);
					System.exit(1);
				}
				try {
					var service = resolver.getInstance( IService.class );
					SmilesRequest request = new SmilesRequest();
					request.setSmiles(smiles);
					request.setClassifierName("MolGenie Ambit/CDK/OpenChemLib Classifyer");
					
					AssignCompounds.AssignmentParameters parameters = new AssignmentParameters();
					parameters.setOntologyFilename(settings.ontologyFilename() );
					parameters.setModule( settings.module() );
					
					OntologyData oData = new OntologyData();
					oData = AssignCompounds.loadOntology( parameters );
					ClassificationResult result = service.performClassification(request, oData);
					SmilesResponse sr = result.smilesResponse();
					
					StringBuilder sB = new StringBuilder();
					sB.append("{");
					sB.append("\"id\": \""+result.smiles()+"\", ");
					sB.append("\"smiles\": \""+result.smiles()+"\", ");
					sB.append("\"classifier\": \""+result.classifier()+"\", ");
					sB.append("\"classifications\": [ ");
					for ( int i=0; i<sr.classifications().size();i++) {
						String classID = sr.classifications().get(i).getClassID();
						String className = sr.classifications().get(i).getClassName();
						if ( i==sr.classifications().size()-1 ) sB.append("{ \"classID\": \""+classID+"\", \"className\": \""+className+"\" }");
						else sB.append("{ \"classID\": \""+classID+"\", \"className\": \""+className+"\" }, ");
					}
					sB.append("]}");
					System.out.println(sB);
					
				} catch (Exception e) {
					log.error("Error processing smiles: " + smiles, e);
					System.err.println("Error processing smiles: " + e.getMessage());
					System.exit(1);
				}
				System.exit(0);
			}
			case "-sdf" -> {
				if (args.length < 2) {
					System.out.println("Error: Missing input sdf");
					System.exit(1);
				}
				String sdfFile = args[1];
				HashMap<String,String> molMap = SdfLoader.readSDF( args[1] );
				System.out.println("converted SDF to smiles input...");
				
				try {
					var service = resolver.getInstance( IService.class );
					StringBuilder sB = new StringBuilder();
					
					AssignCompounds.AssignmentParameters parameters = new AssignmentParameters();
					parameters.setOntologyFilename(settings.ontologyFilename() );
					parameters.setModule( settings.module() );
					
					OntologyData oData = new OntologyData();
					oData = AssignCompounds.loadOntology( parameters );
					int count = 0;
					for ( String name : molMap.keySet() ) {
						String smiles = molMap.get(name);
						SmilesRequest request = new SmilesRequest();
						request.setSmiles(smiles);
						count++;
						//if ( count%100 == 0 ) System.out.println(count);
						sB.append("{");
						//sB.append("\"classifier\": \"MolGenie Ambit/CDK/OpenChemLib Classifyer\", ");
						ClassificationResult result = service.performClassification(request, oData);
						SmilesResponse sr = result.smilesResponse();
						sB.append("\"cid\": \""+name+"\", ");
						sB.append("\"smiles\": \""+result.smiles()+"\", ");
						sB.append("\"classifications\": [ ");
						for ( int i=0; i<sr.classifications().size();i++) {
							String classID = sr.classifications().get(i).getClassID();
							String className = sr.classifications().get(i).getClassName();
							if ( i==sr.classifications().size()-1 ) sB.append("{ \"classID\": \""+classID+"\", \"className\": \""+className+"\" }");
							else sB.append("{ \"classID\": \""+classID+"\", \"className\": \""+className+"\" }, ");
						}
						sB.append("]}\n");
						System.out.println(sB);
					}
				} catch (Exception e) {
					System.err.println("Error processing smiles: " + e.getMessage());
					System.exit(1);
				}
				System.exit(0);
			}
			default -> {
				System.err.println("Error: Unknown option: " + args[0]);
				System.err.println("Use -h or --help for usage information");
				System.exit(1);
			}
		}
	}

	private static void configureEndpoints( Javalin app, InstanceResolver resolver, AppProperties settings ) throws IOException {
		
		AssignmentParameters parameters = new AssignmentParameters();
		parameters.setOntologyFilename(settings.ontologyFilename() );
		parameters.setModule( settings.module() );
		OntologyData ontData = AssignCompounds.loadOntology( parameters );
		
		var service = resolver.getInstance( IService.class );
		
		// API endpoint classification of a smiles string
		app.post( settings.baseApiPath() + "/classify", ctx -> {
			var payload = ctx.bodyAsClass( SmilesRequest.class );
			if (( payload.getSmiles() == null ) || (payload.getSmiles().length()<1) ) {
				ctx.status(400);
				ErrorResponse eR = new ErrorResponse("no or empty smiles found", 100);
				ctx.json(eR);
				return;
			}
			try {
				com.actelion.research.chem.StereoMolecule mol = new com.actelion.research.chem.StereoMolecule();
				com.actelion.research.chem.SmilesParser sP = new com.actelion.research.chem.SmilesParser();
				sP.parse(mol,payload.getSmiles().getBytes());
			} catch (Exception e) {
				payload.setSmiles(null);
				ErrorResponse eR = new ErrorResponse("invalid smiles", 200);
				ctx.json(eR);
				return;
			}

			payload.setClassifierName("MolGenie Ambit+OpenChemLib Classifier v1.0");
			ClassificationResult result = service.performClassification( payload, ontData );
			
			ctx.json(result);
		});
	}
	
}
