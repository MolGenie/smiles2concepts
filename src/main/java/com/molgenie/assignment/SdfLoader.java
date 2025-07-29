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
import java.util.zip.GZIPInputStream;

import com.actelion.research.chem.IsomericSmilesCreator;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.StereoMolecule;

/**
 * Read mol and name from SDF input file.
 * 
 * <h3>Changelog</h3>
 * <ul>
 *   <li>2025-07-29
 *     <ul>
 *       <li>first version</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * @author lutz.weber@molgenie.com
 */
public class SdfLoader {
	
	private final static Pattern PAT_TAB_SPLIT = Pattern.compile( "\t" );

	private final static Logger LOG = Logger.getLogger( SdfLoader.class.getName() );
	
	/**
	* Read SDF file. 
	* 
	* @param _fileName  SDF file
	* 
	* @return map with Name as key, SMILES as value
	* @throws Exception 
	*/
	public static HashMap<String,String> readSDF( String _inputFile ) throws IOException {
		
		StereoMolecule mol = new StereoMolecule();	
		HashMap<String,String> mols = new HashMap();
		
		BufferedReader inSDF = null;
		if ( _inputFile.endsWith("sdf.gz") ) {
			FileInputStream inputStream = new FileInputStream(_inputFile);
			GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
			InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream);
			inSDF = new BufferedReader(inputStreamReader);
		} else if ( _inputFile.endsWith(".sdf") ) {
			FileInputStream inputStream = new FileInputStream(_inputFile);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			inSDF = new BufferedReader(inputStreamReader);
		} else {
			System.out.println("Error: SDF file name needs to end with sdf.gz or .sdf ... stopping.");
			System.exit(0);
		}
	    
		String csvLine = "", dataLine="", molString = "", name = "";
		boolean inMol = true, newMol = true;
		StringBuilder sB = new StringBuilder();
		while ( ( csvLine = inSDF.readLine() ) != null ) {
			if ( newMol ) { 
				name = csvLine.trim();
				newMol = false;
			}
			
			if ( inMol && csvLine.startsWith("M  END") ) {
				sB = sB.append( "M  END" );
				inMol = false;
				try {
					molString = sB.toString();
					StereoMolecule molOCL = new StereoMolecule();
					MolfileParser mP = new MolfileParser();
					mP.parse(molOCL,molString);
					String smiles = IsomericSmilesCreator.createSmiles(molOCL);
					mols.put(name,smiles);
				} catch ( Exception e5 ) {
					System.out.println( "mol2smiles problem: " + molString);
				}
			}
			
			if ( csvLine.contains("$$$$") ) {
				// here the molecule ends ...
				inMol = true;
				newMol = true;
				mol = new StereoMolecule();
				dataLine = "";
				sB = new StringBuilder();
				continue; // go to next line to get next molecule structure
			} else {
				sB = sB.append( csvLine + "\n" );
			}
		}
		return mols;
	}

}

