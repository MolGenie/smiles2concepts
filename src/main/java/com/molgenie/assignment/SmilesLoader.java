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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

public class SmilesLoader {
	
	private final static Pattern PAT_TAB_SPLIT = Pattern.compile( "\t" );

	private final static Logger LOG = Logger.getLogger( SmilesLoader.class.getName() );

	/**
	* Read SMILES file. 
	* 
	* @param _fileName  SMILES file; TAB separated text file with SMILES in first column,
	*                   OCID/ID in 2nd column
	* 
	* @return  map with OCID as key, SMILES as value
	* @throws Exception 
	*/
	public static Map<String,String> readSmiles( String _fileName, int _max, boolean _verbose ) throws Exception {
		
		LOG.info( "reading compounds: " + _fileName );
		final Map<String,String> targetMap = new HashMap<>();
		
		try ( BufferedReader inCsv = new BufferedReader( 
		                               new InputStreamReader(
		                                 new FileInputStream( _fileName ), "UTF8" ) );){	
			String inLine;
			int count=0;
			
			while ( ( inLine = inCsv.readLine() ) != null ) {
				if ( inLine.startsWith( "#" ) ) continue;
				count++;
				if ( _max != 0 ) 
					if ( count > _max ) continue;
				String[] splitLine = inLine.split( "," );
				if ( splitLine.length < 2 ) continue;
				String smiles = splitLine[0];
				String id     = splitLine[1];
				try {
					targetMap.put( id, smiles  );
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			LOG.info( "...read smiles: " + count );
		} catch ( Exception er ) {
			LOG.info( "ERROR: " + er.getLocalizedMessage() );
		}
		return targetMap;
	}
}

