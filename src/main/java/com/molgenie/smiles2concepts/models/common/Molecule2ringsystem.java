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

import java.io.BufferedInputStream;

/**
 * Copyright MolGenie GmbH
 * 
 */
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.TreeSet;

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.IsomericSmilesCreator;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.RingCollection;
import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.util.IntArrayComparator;
import com.molgenie.assignment.AssignCompounds.AssignmentParameters;
import com.molgenie.assignment.OntologyLoader.OntologyData;


public class Molecule2ringsystem {
	
	private static final int TYPE_PLAIN_RINGS = 0;
	private static final int TYPE_SUBSTITION_PATTERN = 1;
	private static final int TYPE_SUBSTITUENT_CLASSES = 2;
	
	SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
	Date dateNow = new Date();
	
	public static void main( String[] _args ) throws Exception {
		
		//String smiles = "C1NCCOPOCCNC1";
		//String smiles = "C1C23C4=C5C6C7C8C9C%10C6C12C1C%10C2C6C%10=C(C%11)CC(C%12CC%13)C6C1C%12C3C%13CC4C=CC5CC7CC(C1C3)C8C1C1C9C2C2C%10C%11CC3C12";
		String smiles = "C1C2C1CCCCCCCCCCCCCC2";
		
		StereoMolecule mol = new StereoMolecule();
		mol.ensureHelperArrays(Molecule.cHelperRings);
		SmilesParser smilesP = new SmilesParser( 0, false );
		smilesP.parse( mol, smiles);
		mol.ensureHelperArrays(Molecule.cHelperRings);
		if ( mol.getRingSet().getSize() == 0 ) System.out.println("rings: null");
		
		HashSet<String> rings = smiles2ringsystem(smiles);
		System.out.println("ringsystems: "+rings.size());
		for ( String ring : rings ) {
			IDCodeParser idParser = new IDCodeParser();
			StereoMolecule nmol = new StereoMolecule();
			idParser.parse(nmol,ring);
			String nsmi = new IsomericSmilesCreator( nmol ).getSmiles();
			System.out.println(ring);
			System.out.println(nsmi);
		}
		getRings(smiles);
	}
	
	public static HashSet<String> smiles2ringsystem( String _smiles ) throws IOException{
		HashSet<String> rSet = new java.util.HashSet();
		String IDcode = "";
		int count = 0;
		try {
			StereoMolecule mol 		= new StereoMolecule();
			SmilesParser smilesP 	= getOclSmilesParser();
			smilesP.parse( mol, _smiles );
			
			StereoMolecule[] backboneArray = getRingSystems( mol, 0 );
			if ( backboneArray != null ) {
				String backboneString = "";
				for ( int i=0; i<backboneArray.length; i++) {
					StereoMolecule backbone = backboneArray[i];
					backbone.setFragment( false );
					String nsmi = new IsomericSmilesCreator( backbone ).getSmiles();
					//System.out.println(nsmi);
					backboneString = backbone.getIDCode();
					rSet.add(backboneString);
				}
			} 
		} catch ( Exception e5 ) {
			System.out.println("OCL problem converting smiles: " + _smiles );
		}
		return rSet;
	}
	
	private static void getRings( String _smiles ) {
		String smiles = _smiles;
	    StereoMolecule mol = new StereoMolecule();
	    try { new SmilesParser( 0, false ).parse( mol, smiles); } catch (Exception e) { e.printStackTrace(); }
	    mol.ensureHelperArrays(Molecule.cHelperRings);
	    TreeSet<int[]> ringList = new TreeSet<>(new IntArrayComparator());
	    int[] pathAtom = new int[mol.getAtoms()];
	    boolean[] neglectBond = new boolean[mol.getBonds()];
	    for (int bond=0; bond<mol.getBonds(); bond++) {
	       if (mol.isRingBond(bond)) {
	          int atom1 = mol.getBondAtom(0, bond);
	          int atom2 = mol.getBondAtom(1, bond);
	          neglectBond[bond] = true;
	          int size = 1+mol.getPath(pathAtom, atom1, atom2, 256, null, neglectBond);
	          neglectBond[bond] = false;
	          int[] ringAtom = Arrays.copyOf(pathAtom, size);
	          Arrays.sort(ringAtom);
	          ringList.add(ringAtom);
	       }
	    }
	   
	    int rings = 0;
	    for (int[] ringAtom : ringList) {
	       int ringsize = 0;
	       System.out.print("ring atoms:");
	       for (int atom : ringAtom) {
	    	   ringsize++;
	    	   System.out.print(" "+atom);
	       }
	       System.out.println("\nringsize: " + ringsize);
	       rings++;
	    }
	    System.out.println("\nrings: " + rings);
	}

	private static StereoMolecule[] getRingSystems( StereoMolecule mol, int substitutionMode ) {
		
		mol.ensureHelperArrays(Molecule.cHelperRings);
		//if ( mol.getRingSet().getSize() == 0 ) return null;
		
		// mark all non-ring atoms for deletion
		for ( int atom=0; atom<mol.getAtoms(); atom++)
			if (!mol.isRingAtom(atom) )
				mol.setAtomMarker(atom, true);
	
		boolean checkFurther = true;
		while (checkFurther) {	// extend ring systems by atoms connected via non-single bonds
			checkFurther = false;
			for (int bond=0; bond<mol.getBonds(); bond++) {
				if (mol.getBondOrder(bond) > 1) {
					for (int i=0; i<2; i++) {
						int atom1 = mol.getBondAtom(i, bond);
						int atom2 = mol.getBondAtom(1-i, bond);
						if (mol.isMarkedAtom(atom1) && !mol.isMarkedAtom(atom2)) {
							mol.setAtomMarker(atom1, false);
							checkFurther = true;
						}
					}
				}
			}
		}

		if ( substitutionMode != TYPE_PLAIN_RINGS ) {
			for (int atom=0; atom<mol.getAtoms(); atom++)
				if (!mol.isMarkedAtom(atom))
					mol.setAtomQueryFeature(atom, com.actelion.research.chem.Molecule.cAtomQFNoMoreNeighbours, true);
	
			for (int bond=0; bond<mol.getBonds(); bond++) {
				for (int i=0; i<2; i++) {
					int atom1 = mol.getBondAtom(i, bond);
					int atom2 = mol.getBondAtom(1-i, bond);
					if ((!mol.isMarkedAtom(atom1) && mol.isMarkedAtom(atom2))
					 || (!mol.isMarkedAtom(atom1) && !mol.isMarkedAtom(atom2)
					  && !mol.isRingBond(bond)
					  && mol.getBondOrder(bond) == 1)) {
						if (substitutionMode == TYPE_SUBSTITION_PATTERN) {
							mol.setAtomQueryFeature(atom1, com.actelion.research.chem.Molecule.cAtomQFNoMoreNeighbours, false);
							mol.setAtomQueryFeature(atom1, com.actelion.research.chem.Molecule.cAtomQFMoreNeighbours, true);
							}
						else {
							int newAtom = mol.addAtom( mol.getAtomicNo(atom2) );
							mol.setAtomQueryFeature( newAtom, com.actelion.research.chem.Molecule.cAtomQFNoMoreNeighbours, false);
							mol.addBond(atom1, newAtom, (i == 0) ? mol.getBondType(bond) : 1);	// retain stereo bond if applicable
							if ( substitutionMode == TYPE_SUBSTITUENT_CLASSES ) {
								mol.setAtomicNo(newAtom, 0);
							}
							/* ehemals
							if ( mol.getAtomicNo(newAtom) != 6) {
                                int[] carbonList = new int[1];
								carbonList[0] = 0;
								mol.setAtomList(newAtom, carbonList, true);
								}
							}*/
								
							}
						}
					}
				}
			}
	
		if ( substitutionMode == TYPE_SUBSTITION_PATTERN ) {
			for (int atom=0; atom<mol.getAtoms(); atom++) {
				if (!mol.isMarkedAtom(atom)
				 && (mol.getAtomQueryFeatures(atom) & com.actelion.research.chem.Molecule.cAtomQFMoreNeighbours) == 0)
					mol.setAtomQueryFeature(atom, com.actelion.research.chem.Molecule.cAtomQFNoMoreNeighbours, true);
				}
			}
	
		for (int bond=0; bond<mol.getBonds(); bond++) {	// mol.getBonds() doesn't consider added bonds!!!
			if (mol.isMarkedAtom(mol.getBondAtom(0, bond))
			 || mol.isMarkedAtom(mol.getBondAtom(1, bond))
			 || (!mol.isRingBond(bond)
			  && mol.getBondOrder(bond) == 1))
				mol.setBondType(bond, com.actelion.research.chem.Molecule.cBondTypeDeleted);
			}
	
		for (int atom=0; atom<mol.getAtoms(); atom++)
			if (mol.isMarkedAtom(atom))
				mol.markAtomForDeletion(atom);
	
		mol.deleteMarkedAtomsAndBonds();
	
		return mol.getFragments();
	}

	public static SmilesParser getOclSmilesParser() {
		int mode = SmilesParser.SMARTS_MODE_IS_SMILES | SmilesParser.MODE_SKIP_COORDINATE_TEMPLATES;
		SmilesParser smilesP = new SmilesParser( mode, false );
		return smilesP;
	}


}
