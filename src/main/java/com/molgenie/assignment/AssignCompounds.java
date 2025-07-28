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
package com.molgenie.assignment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.molgenie.assignment.OntologyLoader.OntologyData;
import com.molgenie.smiles2concepts.models.common.Molecule2ringsystem;

/**
 * Compound assignment to chemical structure classes in a ontology
 * version 2.00
 *
 * 2025-07-01
 * seventh version
 * stereochemistry assignment via CDK
 * smarts assignment via Ambit-CDK
 * ring system assignment via OpenChemLib ID-Codes
 * 
 * @author lutz.weber@molgenie.com
 */
public class AssignCompounds {  
	
	private final static Logger LOG = Logger.getLogger( AssignCompounds.class.getName() );
	private static final OntologyData oData = new OntologyData();
	
	public class comments{
		// allowed in Ambit query
		//[C;R0],[#6;v4;H2;R0],[CH2;v4;R0],[$([NH0](C)C)]
		//2EXACT[#6;\!$(C=[O,N,S])]-[#7](-[#1,#6;\!$(C=[O,N,S])])-[#1,#6;\!$(C=[O,N,S])]
		//[$(N(C)C),$([NH]C),$([NH2])]-[#7;A;H0;X2]=[#6;A;X3;\!$(C(N)=[O,N,S])]
		//1EXACT[#6;H1,H0;$([#6](F)(F));\!$([#6](F)(F)(F))]
		//not : [!#6][#6,#1][#1], one must eliminate all [#1] or [H]
	}
	
	public static class AssignmentParameters {
		
	    private String  module = "ambit";
	    private String  ontologyFilename = "./src/main/resources/mol_classes_2025-07-18.obo";
	    //private String  ontologyFilename = "/Users/lweber/Desktop/assignment/mol_classes_2025-07-18.obo";
	    private String  smiles = null;
	    private String  compId = null;
	    private String  timeOut = null;
	    private Level  logLevel = Level.SEVERE;
	    private boolean writeLeafsOnly = false;
    
	    public String getOntologyFilename() { return ontologyFilename; }
	    public AssignmentParameters setOntologyFilename( String _fileName ) {
	    	ontologyFilename = _fileName;
	    	return this;
	    }
	    
	    public Level getLogLevel() { return logLevel; }
	    public AssignmentParameters setLogLevel( Level _logLevel ) {
	    	logLevel = _logLevel;
	    	return this;
	    }
	    
	    public String getTimeOut() { return timeOut; }
	    public AssignmentParameters setTimeOut( String _timeOut ) {
	    	timeOut = _timeOut;
	    	return this;
	    }
	    
	    public String getModule() { return module; }
	    public AssignmentParameters setModule( String _module ) {
	    	module = _module;
	    	return this;
	    }
   
	    public Boolean getWriteLeafsOnly() { return writeLeafsOnly; }
	    public AssignmentParameters setWriteLeafsOnly( boolean _writeLeafsOnly ) {
	    	writeLeafsOnly = _writeLeafsOnly; 
	    	return this;
	    }
	    
	    public String getSmiles() { return smiles; }
	    public AssignmentParameters setSmiles( String _smiles ) {
	    	smiles = _smiles; 
	    	return this;
	    }
	    
	    public String getId() { return compId; }
	    public AssignmentParameters setId( String _compId ) {
	    	compId = _compId; 
	    	return this;
	    }
    
	    public void checkParameters() throws IOException {
	    	checkNotNull( "module", module );
	    }
    
	    @Override
	    public String toString() {
	    	try {
	    		return new ObjectMapper().writeValueAsString( this );
	    	} catch ( IOException ioe ) {
	    		return "ERROR serializing AssignmentParameters to JSON: " + ioe.getMessage(); 
	    	}
	    }
    
	    private void checkNotNull( String _parameter, String _value ) throws IOException {
	    	if ( _value == null ) {
	    		throw new IOException( "Parameter '" + _parameter + "' not set" );
	    	}
	    }
	}
  
	public static OntologyData loadOntology( AssignmentParameters _parameters ) throws IOException {
		
		_parameters.checkParameters();
		LOG.setLevel(_parameters.getLogLevel());
		LOG.info( "loading: " + _parameters.getOntologyFilename() );
		LOG.info( "using: " + _parameters.getModule() );
		
		long startTime = System.nanoTime();
		boolean aromatic = true;
		boolean verbose = false;
		
		//step 1: read chemistry ontology
	    OntologyData ontData = OntologyLoader.readObo( _parameters.getOntologyFilename(), _parameters.getModule(), aromatic );
	    final Map<String,Set<String>>  idClass2childMap   		= ontData.getIdChildMap();
	    final Map<String,Set<String>>  idClass2parentMap  		= ontData.getIdParentMap();
	    final Map<String,String>       idClass2nameMap  		= ontData.getIdNameMap();
	   
	    LOG.info("find root class ...");
	   	String rootId = null;
	    int countRoots = 0;
	    for ( String id : idClass2parentMap.keySet() ) {
	    	if ( idClass2parentMap.get( id ).isEmpty() ) {
	    		rootId = id;
	    		ontData.setRootId(id);
	    		LOG.info( "root_id: " + rootId );
	    		countRoots++;
	    	}
	    }
	    if ( countRoots > 1 ) {
	    	throw new IOException( "error - more than 1 root concept found: " + countRoots );
	    } else if ( rootId == null ) {
	    	throw new IOException( "no root concept found :(" );
	    } 
	    
		if ( idClass2childMap.get(rootId).isEmpty() ) {
			LOG.info("generating idClass2childMap ...");
			final List<String> idList1 = new ArrayList<>();
	   		idClass2parentMap.forEach( ( key, value )->{ idList1.addAll( value ); } );
			for ( String id : idList1 ) {
				List<String> list1 = new ArrayList<>();
				Iterator<String> itre = idClass2parentMap.keySet().iterator();
				while ( itre.hasNext() ) {
					String key = itre.next();
					Set<String> parents = idClass2parentMap.get(key);
					if( parents.contains( id ) ) list1.add( key );
				}
				HashSet<String> hsList1 = new HashSet<String>( list1 );
				idClass2childMap.put( id, hsList1 );
			}
		}
		
	    LOG.info("calculate ancestors and offsprings ...");
	   
	    int idClassC = 0; 
	    for ( String idClass : idClass2nameMap.keySet() ) {
	    	Set<String> ancestorSet = ancestors( idClass, idClass2parentMap );
	    	ontData.setIdAncestorsMap(idClass, ancestorSet);
	    	//LOG.info( "class: " + idClass + " " + ancestorSet );
	    }
	    for ( String idClass : idClass2nameMap.keySet() ) {
	    	idClassC++;
	    	//LOG.info( "class: "+idClassC+" "+idClass+" "+idClass2nameMap.get(idClass) );
	    	Set<String> offspringSet = offsprings( idClass, idClass2childMap );
	    	ontData.setIdOffspringsMap( idClass, offspringSet);
	    	//if ( offspringSet.size() > 0 ) LOG.info( idClassC +" "+ idClass + " offsprings: " + offspringSet.size() );
	    }
	   
	    return ontData;
	}
	
	public static HashMap<String,String> runAssignment( AssignmentParameters _parameters, OntologyData oData ) throws Exception {
		
		boolean aromatic = true;
		boolean verbose = false;
		HashMap<String,String> conceptMap = new HashMap();
		
	    String smiles = _parameters.getSmiles();
	    String compId = _parameters.getId();
	    long startTime = System.nanoTime();
	   
	    //follow hierarchy of class id top down and assign, write into idAssignmentMap
	    Map<String,Set<String>> idAssignmentMap = 
	    		AssignmentUtils.hierarchicalClassAssignment( _parameters.getModule(), 
	    												aromatic, verbose, smiles, compId, oData);

	    Set<String> idClassSet1 = new HashSet<String>(); //all concepts only
	    Set<String> idClassSet2 = new HashSet<String>(); //leaf concepts only
	    
	    for ( String id : idAssignmentMap.keySet() ) {
	    	idClassSet1.clear(); 
	        idClassSet2.clear();
	        final Set<String> idClassSet = idAssignmentMap.get( id );
	        
	        // check ancestors, omit concept if a parent is missing
		    for ( String idClass : idClassSet ) {
		        final Set<String> classAncestorsSet = oData.getIdAncestorsMap().get(idClass);
		        // leave out if a concept has no parents
		        if ( classAncestorsSet==null || classAncestorsSet.isEmpty() ) continue;
		        boolean missingParent = false;
	        	//leave out if an ancestor concept is missing
				for ( String ancestor : classAncestorsSet ) {
					if ( !idClassSet.contains( ancestor ) ) {
						//System.out.println( idClass+" missing parent: "+ancestor);
						missingParent = true;
						continue;
					}
    			}
				boolean eligible = false;
				if ( oData.getIdSmartsMap().get(idClass).isEmpty() && oData.getIdIdcodeMap().get(idClass) == null )
					eligible = true;
				if ( oData.getIdIdcodeMap().get(idClass) != null ) {
					Set<String> ancestorList = oData.getIdAncestorsMap().get(idClass);
					idClassSet1.addAll( ancestorList );
				}
	        	if ( missingParent || eligible ) {
	        		continue;
	        	} else {
	        		idClassSet1.add( idClass );
	        	}
	        }
	        
	        // check for children present, omit concept if valid child with smarts has been found
	        for ( String idClass : idClassSet1 ) {
	        	boolean validChild = false;
	        	final Set<String> classAllOffspringsSet = oData.getIdOffspringsMap().get(idClass);
	        	//System.out.println( idClass + " offsprings: "+classAllOffspringsSet.size() );
	        	
	        	//leave out if offspring with smarts is present, one is enough
	        	if ( classAllOffspringsSet != null ) {
	        		for ( String offspring : classAllOffspringsSet ) {
	        			if ( idClassSet1.contains( offspring ) && oData.getIdSmartsMap().containsKey( offspring ) ) {
	        				validChild = true;
	        				break;
	        			}
	        		}
	        	}
	        	if ( validChild ) {
	        		continue;
	        	} else {
	        		idClassSet2.add( idClass );
	        	}
	        }
	        
	        if ( _parameters.writeLeafsOnly ) {
	        	//System.out.println( "leaf concepts only:");
	        	//for ( String newClass : idClassSet2 ) System.out.println( "\t" + newClass + "\t" + oData.getIdNameMap().get( newClass ));
	        	for ( String newClass : idClassSet2 ) {
	        		if (newClass.equals("MGN100001928") || newClass.equals("MGN100003353")) continue;
	        		conceptMap.put(newClass,oData.getIdNameMap().get( newClass ));
	        	}
	        } else {
	        	//System.out.println( "all concepts:");
	        	//for ( String newClass : idClassSet1 ) System.out.println( "\t" + newClass + "\t" + oData.getIdNameMap().get( newClass ));
	        	for ( String newClass : idClassSet1 ) {
	        		if (newClass.equals("MGN100001928") || newClass.equals("MGN100003353")) continue;
	        		conceptMap.put(newClass,oData.getIdNameMap().get( newClass ));
	        	}
	        }
	        idClassSet1 = new HashSet<String>();
	        idClassSet2 = new HashSet<String>();
	    }  
	    
	    long duration = System.nanoTime() - startTime;
    
	   if ( verbose ) LOG.info( "Elapsed time (s): " + TimeUnit.NANOSECONDS.toSeconds( duration ) );
	    
	   return conceptMap;
	}
    
	private static Set<String> ancestors( String _classId, Map<String,Set<String>> _parentMap ) {
		
		final Set<String> ancestorList = new HashSet<>();
		
		Set<String> idList = Collections.singleton( _classId );
		
		while ( ! idList.isEmpty() ) {
	    final Set<String> newIdList = new HashSet<>();  //list of all ids for one hierarchy level
			for ( String id : idList ) {
			  final Set<String> parentList = _parentMap.get( id );
				if ( ( parentList != null ) && ( ! parentList.isEmpty() ) ) {
				  newIdList.addAll( parentList );
				}
			}
			idList = newIdList;
			ancestorList.addAll( newIdList ); //list of all ids for down of classId
		}
		return ancestorList;
	}

	private static Set<String> offsprings( String _classId, Map<String,Set<String>> _childMap ){
		
		Set<String> idList 			= new HashSet<String>();
		Set<String> childList  		= new HashSet<String>();
		Set<String> offspringList	= new HashSet<String>();
		Set<String> parsedList		= new HashSet<String>();
		
		idList.add( _classId );
		//System.out.println("ocid: "+_classId);
		String flag = "toContinue";
		
		while ( flag.equals( "toContinue" ) ) {
			HashSet<String> newIdList = new HashSet<String>(); 	//list of all ids for one hierarchy level
			flag = "Stop" ;
			for ( String id : idList ) {
				if ( parsedList.contains( id ) ) continue;
				parsedList.add(id);
				childList = _childMap.get( id );
				if ( childList != null ) {
					newIdList.addAll( childList );
					flag = new String("toContinue");
				}
			}
			idList = newIdList;
			offspringList.addAll( newIdList ); //list of all ids for down of classId
		}
		//System.out.println("\t"+idList.size()+" childList: "+offspringList);
		return offspringList;
	}
	
	private static void help( int _exitCode ) {
    
		System.err.println( "Run compound assignment.\n" +
                        "usage: java " + AssignCompounds.class.getName() + " OPTIONS\n" +
                        "   -s    --smiles	     SMILES\n" +
                        "   -a    --ancestors    ANCESTORS\n" +
                        "                          all - creates output with all assigned classes\n" + 
                        "                          parents - creates output with assigned parent classes\n" + 
                        "   -h    --help           print this help and exit\n" 
                      );
		System.exit( _exitCode );
	}
  
	private static AssignmentParameters parseCommandLine( String[] _args ) {
    
		final AssignmentParameters parameters = new AssignmentParameters();
    
		for ( int argI = 0; argI < _args.length; argI++ ) {
      
			final String arg = _args[ argI ];
			argI++;
			
			final String nextArg = argI < _args.length ? _args[ argI ] : null;
			if ( "-s".equals( arg ) || "--smiles".equals( arg ) ) {
				parameters.setSmiles( nextArg );
			} else if ( "-a".equals( arg ) || "--ancestors".equals( arg ) ) {
				if ( nextArg.equals("all") ) parameters.setWriteLeafsOnly(false);
				else if ( nextArg.equals("parents") ) parameters.setWriteLeafsOnly(true);
				else {  System.err.println( nextArg+" Unknown parameter '" + arg + "'" ); help( 1 ); }
			} else if ( "-h".equals( arg ) || "--help".equals( arg ) ) {
				help( 0 );
				argI = argI - 1;
			} else {
				System.err.println( nextArg + " Unknown parameter '" + arg + "'" );
				help( 1 );
			}
		}
		return parameters;
	}
  
	public static HashMap<String,String> parseSmiles( AssignmentParameters aParam, OntologyData oData ) throws Exception {
		HashMap<String,String> conceptMap = runAssignment( aParam, oData );
		return conceptMap;
	}
	
	public static void checkOntology( AssignmentParameters aParam, OntologyData oData ) throws Exception {
		int countOK = 0;
		int countProblem = 0;
		Map<String, Set<String>> smilesMap = oData.getIdSmilesMap();
		int countP = 0;
		for (String id : smilesMap.keySet() ) {
			if ( !id.equals("MGN100003100") ) continue;
			if ( oData.getCheckSet().contains(id)) continue;
			countP++;
			if ( countP%100 == 0) System.out.println("classes checked: "+countP+" "+id);
			
			Set<String> smilesSet = smilesMap.get(id);
			for ( String smiles : smilesSet ) {
				boolean problem = false;
				aParam.setSmiles(smiles);
				aParam.setId(id);
				aParam.setWriteLeafsOnly(false);
				//if ( oData.getIdIdcodeMap().get(id) != null ) continue;
				HashMap<String,String> conceptMap = runAssignment( aParam, oData );
				//if ( !conceptMap.containsKey(id) ) {
					countProblem++;
					problem = true;
					System.out.println( countP + " " + id+" "+oData.getIdNameMap().get(id)+" "+smiles);
					//if ( problem ) continue;
					for ( String classe : conceptMap.keySet() ) {
					System.out.println("\tass: " + classe +" "+oData.getIdNameMap().get(classe));
					}
					if ( oData.getIdIdcodeMap().get(id) != null ) {
						System.out.println("\tclass idcode: " + oData.getIdIdcodeMap().get(id));
						HashSet<String> compIdCode = Molecule2ringsystem.smiles2ringsystem(smiles);
						if ( !compIdCode.isEmpty() ) System.out.println("\tidcode: " + compIdCode.iterator().next());
						else System.out.println("\tcomp: ID-Code empty " + compIdCode);
					} 
					Set<String> parSet = oData.getIdParentMap().get(id);
					for ( String par : parSet ) {
						System.out.println("\t\tpar: " + par+" "+oData.getIdNameMap().get(par));
					}
					Set<String> ancSet = oData.getIdAncestorsMap().get(id);
					for ( String anc : ancSet ) {
						System.out.println("\t\tanc: " + anc+" "+oData.getIdNameMap().get(anc));
					}
				//} else {
				//	countOK++;
				//	System.out.println("ok: "+countOK+" "+id);
				//}
			}
		}
		System.out.println("count OK: "+countOK);
		System.out.println("count problem: "+countProblem);
	}
	
	public static void main( String[] _args ) throws Exception {
		boolean aromatic = true;
		boolean verbose = false;
		
		AssignmentParameters parameters = parseCommandLine( _args );
		parameters.setSmiles("O=C(C1(CC2)C=CC22OCCO2)NC2=C1C=CC=C2");
		
		OntologyData oData = new OntologyData();
		oData = loadOntology(parameters);
		
		//check consistency of ontology: if a concept with a smiles is assigned to its own class.
		//checkOntology(parameters, oData);
		
		//make assignment for a single smiles and print assignments
		HashMap<String,String> conceptMap = parseSmiles( parameters, oData );
		for ( String id : conceptMap.keySet() ) System.out.println( id+"\t"+conceptMap.get(id));
	}

}

