/* 
 * <copyright>
 *  
 *  Copyright 2003-2004 The Boeing Company
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.robustness.exnihilo;


/**
	By-hand port of "statelisttype" type, from systemdesignx.bas.

    <PRE>
		Type agenttType statelisttype
		
			'e(0) is energy of annealing state is
			'e(1-numsavedstatesmax) is energy of the ten best found states

			e As Single
			fixedcost As Single
			monthlycost As Single
			responsetime As Single
			fixedcostscale As Single
			monthlycostscale As Single
			pfailurescale As Single
			responsetimescale As Single
			
			pfailuresurvivescale As Single 'marc EN 1.1 smoothing
			hammingscale As Single 'marc EN 1.1 hamming

			temp As Single
			
			'fstatecallsave(0,ifn,inod) is the annealing state definition
			'fstatecallsave(1-10,ifn,inod) are the ten best found states
			'numfstatecallsave is the number of best found states
			fstatecallsave(numfunctionmax, numnodemax) As Integer  'stored annealing states
			'don't need to have statelist(*).status, since we only stored successful states!
			'status As Boolean
			
			'marc EN 1.1 5/9/03:
			numpath As Integer 'number of paths (node pairs) to define state i
			'path(i, inod) is a list of nodes in reverse order, from finish to start, in path i
			'change numnodemax to numfunctionmax 7/16/02
			'path(numnodemax, numnodemax) As Integer
			path(numfunctionmax, numnodemax) As Integer
			numpathnode(numnodemax2) As Integer   'number of nodes on the defined path path(inode) for state i
			'pathedge(i,inod) is a list of all defined links associated with the above path
			pathedge(numnodemax) As Integer
			
			'psuccess = probability of no failures in state
			psuccess As Single
			pfailure As Single

		   pfailuresurvive As Single 'marc EN 1.1 smoothing
		   hamming As Single 'marc EN 1.1 hamming

			nodeused(numnodemax) As Boolean
			linkused(numlinkmax) As Boolean
			
			utiltot(numnodemax) As Single
			memtot(numnodemax) As Single

// 30april03
			'messaging mods
			remotetraffic As Single
			remotetrafficscale As Single

		End Type
    </PRE>

	@version EN2.09 - added: loadbal, loadbalscale
   
   
    @author robert.e.cranfill@boeing.com

**/
public class StateList {

public float	e; // As Single
public float	fixedcost; // As Single
public float	monthlycost; // As Single
public float	responsetime; // As Single
public float	fixedcostscale; // As Single
public float	monthlycostscale; // As Single
public float	pfailurescale; // As Single
public float	responsetimescale; // As Single
public float	temp; // As Single

// 'fstatecallsave(0,ifn,inod) is the annealing state definition
// 'fstatecallsave(1-10,ifn,inod) are the ten best found states
// 'numfstatecallsave is the number of best found states
public int		fstatecallsave[][] = new int[SystemDesign.numfunctionmax+1][SystemDesign.numnodemax+1];

// 'don't need to have statelist(*).status, since we only stored successful states!
// 'status As Boolean

public int		numpath; // As Integer 'number of paths (node pairs) to define state i

// 'path(i, inod) is a list of nodes in reverse order, from finish to start, in path i
// 'change numnodemax to numfunctionmax 7/16/02
// 'path(numnodemax, numnodemax); // As Integer


// rec - 05may03 - changed size of path - OK???
// originally:
//    public int		path[][] = new int[SystemDesign.numfunctionmax+1][SystemDesign.numnodemax+1]; 
//
// so try this for now....
public int		path[][] = new int[(SystemDesign.numfunctionmax*2)+1][SystemDesign.numnodemax+1]; 



public int		numpathnode[] = new int[SystemDesign.numnodemax2+1]; 

// 'pathedge(i,inod) is a list of all defined links associated with the above path
public int		pathedge[] = new int[SystemDesign.numnodemax+1];

// 'psuccess = probability of no failures in state
public float	psuccess; // As Single
public float	pfailure; // As Single

public boolean	nodeused[] = new boolean[SystemDesign.numnodemax+1];
public boolean	linkused[] = new boolean[SystemDesign.numlinkmax+1];

public float	utiltot[] = new float[SystemDesign.numnodemax+1];
public float	memtot[] = new float[SystemDesign.numnodemax+1];

public float remotetraffic;
public float remotetrafficscale;


public float pfailuresurvive; // 'marc EN 1.1 smoothing
public float pfailuresurvivescale; // 'marc EN 1.1 smoothing
public float hamming; // 'marc EN 1.1 hamming
public float hammingscale; // 'marc EN 1.1 hamming

public float loadbal, loadbalscale; // EN2.09


/**
	Constructor
**/
public StateList() {

} // constructor ()


} // class StateList
