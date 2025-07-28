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

import java.util.Locale;

/**
 * Constants for chemical libraries.
 *
 *
 * <h3>Changelog</h3>
 * <ul>
 *   <li>2022-04-24
 *     <ul>
 *       <li>second version</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class ChemLib {

	public final static String CHEMLIB_AMBIT = "ambit";
	public final static String CHEMLIB_CDK   = "cdk";
	public final static String CHEMLIB_CA    = "chemaxon";
	public final static String CHEMLIB_OCL   = "ocl";

	private final static String[] KNOWN_CHEMLIBS = new String[] {
		CHEMLIB_AMBIT,
		CHEMLIB_CA,
		CHEMLIB_CDK,
		CHEMLIB_OCL
	};
  
  	/**
  	 * Returns normalized chemical library name for provided name.
  	 * 
  	 * @param _name
  	 * 
  	 * @return  known normalized library name or <code>null</code>
  	 */
	public final static String resolveChemLib( String _name ) {
		if ( _name != null ) {
			final String nameLc = _name.toLowerCase( Locale.ENGLISH );
			for( String chemLib : KNOWN_CHEMLIBS ) {
				if ( chemLib.equals( nameLc ) ) {
					return chemLib;
				}
			}
		}
		return null;
	}
  
	/**
	 * Returns <code>true</code> if provided name is a known chemical
	 * library ({@link #resolveChemLib(String)} returns a non-null value).
	 * 
	 * @param _name
	 * 
	 * @return
	 */
	public final static boolean isKnownChemLib( String _name ) {
		return resolveChemLib( _name ) != null;
	}
}
