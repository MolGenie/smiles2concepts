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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Read structure ontology to perform compound assignment, creating container
 * containing is_a, has_a and querylist.
 *
 * @author lutz.weber@molgenie.com
 */
public class OntologyLoader {
	
	/**
	 * Class holding ontology data.
	 */
	public final static class OntologyData {
		
		private final Map<String,String>       idNameMap   			= new HashMap<>();
		private final Map<String,String>       idIdcodeMap   		= new HashMap<>();
	    private final Map<String,Set<String>>  idChildMap  			= new HashMap<>();
	    private final Map<String,List<String>> idSmartsMap 			= new HashMap<>();
	    private final Map<String,Set<String>>  idSmilesMap 			= new HashMap<>();
	    private final Map<String,Set<String>>  idParentMap 			= new HashMap<>();
	    private final Map<String,Set<String>>  idOffspringsMap 		= new HashMap<>();
	    private final Map<String,Set<String>>  idAncestorsMap 		= new HashMap<String,Set<String>>();
	    private final HashSet<String>  		   checkSet 			= new HashSet<String>();
	    private String 				   		   rootId 				= null;
	    
	    public Map<String,String>       getIdNameMap()   		{ return idNameMap; }
	    public Map<String,String>       getIdIdcodeMap()   		{ return idIdcodeMap; }
	    public Map<String,Set<String>>  getIdChildMap()  		{ return idChildMap; }
	    public Map<String,List<String>> getIdSmartsMap() 		{ return idSmartsMap; }
	    public Map<String,Set<String>>  getIdSmilesMap() 		{ return idSmilesMap; }
	    public Map<String,Set<String>>  getIdParentMap() 		{ return idParentMap; }
	    public Map<String,Set<String>>  getIdOffspringsMap()  	{ return idOffspringsMap; }
	    public Map<String,Set<String>>  getIdAncestorsMap()  	{ return idAncestorsMap; }
	    public HashSet<String>          getCheckSet()   		{ return checkSet; }
	    public String 					getRootId() 	 		{ return rootId; }
	    
	    public void setIdName(String _id,String _name)  					{idNameMap.put(_id,_name);}
	    public void setIdIdcode(String _id,String _idcode)  				{idIdcodeMap.put(_id,_idcode);}
	    public void setIdChildrenMap(String _id,Set<String> _childrens) 	{idChildMap.put(_id,_childrens);}
	    public void addIdChildren(String _id,String _children) {
	    	Set<String> childs = idChildMap.get(_id);
	    	childs.add(_children);
	    	}
	    public void setIdParentsMap(String _id,Set<String> _parents) 		{idParentMap.put(_id, _parents);}
	    public void addIdParent(String _id,String _parent) {
	    	Set<String> parents = idParentMap.get(_id);
	    	parents.add(_parent);
	    	}
	    public void setIdSmartsMap(String _id,List<String> _smarts) 		{idSmartsMap.put(_id, _smarts);}
	    public void addIdSmart(String _id,String _smart) {
	    	List<String> smarts = idSmartsMap.get(_id);
	    	smarts.add(_smart);
	    	}
	    public void setIdSmilesMap(String _id,Set<String> _smilesSet) 		{idSmilesMap.put(_id, _smilesSet);}
	    public void addIdSmiles(String _id,String _smiles) {
	    	Set<String> smiles = idSmilesMap.get(_id);
	    	smiles.add(_smiles);
	    	}
	    public void setIdOffspringsMap(String _id,Set<String> _offspring) 	{idOffspringsMap.put(_id,_offspring);}
	    public void addIdOffspring(String _id,String _offspring) {
	    	Set<String> offsprings = idOffspringsMap.get(_id);
	    	offsprings.add(_offspring);
	    	}
	    public void setIdAncestorsMap(String _id,Set<String> _ancestor) 	{idAncestorsMap.put(_id,_ancestor);}
	    public void addIdAncestor(String _id,String _ancestor) {
	    	Set<String> ancestors = idAncestorsMap.get(_id);
	    	ancestors.add(_ancestor);
	    	}
	    public void setCheckSet(HashSet<String> _idL) 						{ checkSet.addAll(_idL); }
	    public void setRootId(String _id) 	 								{ rootId =_id; }
	}
	
	private final static Logger LOG = Logger.getLogger( OntologyLoader.class.getName() );
  
	public static OntologyData readObo( String _inObo, String _module, boolean _aromatic ) throws IOException {
		
		final OntologyData ontData = new OntologyData();

		final boolean isModuleCdkOrAmbit = ChemLib.CHEMLIB_CDK.equals( _module.toLowerCase() ) 
				|| ChemLib.CHEMLIB_AMBIT.equals( _module.toLowerCase() );
		
		FileInputStream inputStream = new FileInputStream(_inObo);
		GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
	    InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream);
	    BufferedReader inFile = new BufferedReader(inputStreamReader);
		
		try ( BufferedReader inObo = new BufferedReader( 
		                               new InputStreamReader( 
		                                  new GZIPInputStream (
		                                     new FileInputStream( new File( _inObo ) ) ), "UTF8" ) ); ) {
		
	  		String inLine = null;
	  		while ( ( inLine = inObo.readLine() ) != null ) {
	  			
	  			if ( inLine.startsWith( "[Term]" ) ) {
	  				final List<String> smartsList	= new ArrayList<>();
	  				final Set<String>  smilesSet	= new HashSet<>();
	  				final Set<String>  childSet		= new HashSet<>();
	  				final Set<String>  parentSet	= new HashSet<>();
	  				final HashSet<String>  checkSet	= new HashSet<>();
	  				String name 					= null;
	  				String idcode					= null;
	  				String id   					= null;
	  				boolean obsolete 				= false;
	  				boolean check					= true;
	
	  				String inConceptLine = null;
	  				while ( ( inConceptLine = inObo.readLine() ) != null ) {
	
				        if ( inConceptLine.startsWith( "[" ) ) {
				        	throw new IOException( "Start of new Stanza without previous empty line.");
				        }
			        
				        if ( inConceptLine.trim().isEmpty() ) break;
				        
				        int tagSepOff = inConceptLine.indexOf( ':' );
				        if ( tagSepOff < 2 ) continue;
				        
					    final String tag   = inConceptLine.substring( 0, tagSepOff );
					    String value = inConceptLine.substring( tagSepOff + 1 ).trim();
			        
					    int commentSepOff = value.indexOf( " !" );
					    if ( commentSepOff >= 0 ) {
					    	value = value.substring( 0, commentSepOff ).trim();
					    }
					    
					    if ( tag.startsWith( "is_obsolete" )) obsolete = true;
			        
				        if ( "id".equals( tag ) ) 			id = value;
				        if ( checkSet.contains(id)) continue;
				        
				        else if ( "name".equals( tag ) ) 	name = value;
				        else if ( "idcode".equals( tag ) ) 	{
				        	idcode = value;
				        	//String example = "f`yA@@@ILtsJmJrcZ\{sUUUSUT@@" MGN300044907;
				        	
				        	boolean prob = false;
				        	if ( idcode.contains("\\\\\\\\") ) {
				        		//System.out.println( "idcode o: "+ idcode);
				        		idcode = idcode.replace("\\\\\\\\", "YYYY");
				        		prob = true;
				        	}
				        	if ( idcode.contains("\\\\\\{") ) {
				        		//System.out.println( "idcode o: "+ idcode);
				        		idcode = idcode.replace("\\\\\\{", "WWWW");
				        		prob = true;
				        	}
				        	while ( idcode.contains("\\{") ) {
					        		//System.out.println( "idcode o: "+ idcode);
					        		idcode = idcode.replace("\\{","{");
					        		//System.out.println( "idcode n: "+ idcode);
					        } 
				        	if ( idcode.contains("\\\\\\}") ) {
				        		//System.out.println( "idcode o: "+ idcode);
				        		idcode = idcode.replace("\\\\\\}", "PPPP");
				        		prob = true;
				        	}
				        	while ( idcode.contains("\\}") ) {
				        		//System.out.println( "idcode o: "+ idcode);
				        		idcode = idcode.replace("\\}","}");
				        		//System.out.println( "idcode n: "+ idcode);
				        	} 
				        	
				        	while ( idcode.contains("\\\\") ) {
				        		//System.out.println( "\tidcode o: "+ idcode);
				        		idcode = idcode.replace("\\\\","\\");
				        		//System.out.println( "\tidcode n: "+ idcode);
				        	} 
				        	
				        	if (prob) {
				        		idcode = idcode.replace("WWWW","\\{");
				        		idcode = idcode.replace("PPPP","\\}");
				        		idcode = idcode.replace("YYYY","\\\\");
				        		//System.out.println( "idcode n: "+ idcode);
				        	}
				        	if ( id.equals("MGN300046583")) idcode="f`iPQ@FZIPs`AF@aJZ[UY\\\\e[WjjjjYj`@@";
				        }
				        else if ( "smiles".equals( tag ) ) 	{
				        	String smiles = value;
				        	while ( smiles.contains("\\\\") ) {
				        		//System.out.println( "\tsmiles o: "+ smiles);
				        		smiles = smiles.replace("\\\\","\\");
				        		//System.out.println( "\tsmiles n: "+ smiles);
				        	} 
				        	smilesSet.add( smiles ); 
				        }
				        else if ( "check".equals( tag ) ) 	{
				        	if ( value.equals("false") ) checkSet.add(id);
				        }
					    else if ( "is_a".equals( tag ) ) 	parentSet.add( value ); 
				        else if ( "has_a".equals( tag ) ) 	childSet.add( value );
				        
				        else if ( tag.endsWith( "smarts" ) ) {
				        	String smarts = null;
    						if ( isModuleCdkOrAmbit ) {
    							if ( _aromatic ) {
    								if ( inConceptLine.startsWith("ambit_aromsmarts: ") ) {
    									smarts = inConceptLine.substring(18);
    									//System.out.println( "smarts: "+ smarts);
    								}
    							} else {
    								if ( inConceptLine.startsWith("ambit_smarts: ") ) {
    									smarts = inConceptLine.substring(14);
    									//System.out.println( "smarts: "+ smarts);
    								}
    							}
    						}
    						
    						if ( smarts != null ) {
	    						if ( smarts.contains(" ! ")) {
	    							int startS = smarts.indexOf(" ! ");
	    							smarts = smarts.substring(0,startS);
	    						}
	    						smarts = smarts.replace("\\!","!");
	    						smarts = smarts.replace("\\\\","\\");
	    						//System.out.println( smarts );
	    						smartsList.add( smarts );
    						}
				        }
	  				}
			      
	  				if ( id != null ) {
			            if ( obsolete ) continue;
			            if ( name != null ) ontData.setIdName( id, name );
			  		    if ( idcode != null ) ontData.setIdIdcode( id, idcode );
			  		    if ( childSet != null ) ontData.setIdChildrenMap( id, childSet );
			  		    if ( smilesSet != null ) ontData.setIdSmilesMap( id, smilesSet );
			  		    ontData.setIdParentsMap( id, parentSet );
			  		    ontData.setIdSmartsMap( id, smartsList );
			  		    ontData.setCheckSet( checkSet );
			            obsolete = false;
	  				}
	  			}
	  		}
		}
		return ontData;
	}
	
	public static void main( String[] _args ) throws Exception {
		readObo( _args[0], _args[1], Boolean.valueOf( _args[2] )  );
	}

}
