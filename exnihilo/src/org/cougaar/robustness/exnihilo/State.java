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
	By-hand port of "statetype" type, from systemdesignx.bas:
	
    <PRE>
		Type statetype
		   ' A function call to function ifn, when called from node inod,
		   ' will be executed on node number fstatecall(ifn,inod)
		   fstatecall(numfunctionmax, numnodemax) As Integer
		   'e(0) is energy of annealing state is
		   'e(1-numsavedstatesmax) is energy of the ten best found states
		   'e(numsavedstatesmax) As Single
		   'fixedcost(numsavedstatesmax) As Single
		   'monthlycost(numsavedstatesmax) As Single
		   'responsetime(numsavedstatesmax) As Single
		   'fixedcostscale(numsavedstatesmax) As Single
		   'monthlycostscale(numsavedstatesmax) As Single
		   'pfailurescale(numsavedstatesmax) As Single
		   'responsetimescale(numsavedstatesmax) As Single
		   'temp(numsavedstatesmax) As Single
		   
		   numfstatecallsave As Integer
		   'fstatecallsave(0,ifn,inod) is the annealing state definition
		   'fstatecallsave(1-10,ifn,inod) are the ten best found states
		   'numfstatecallsave is the number of best found states
		   'fstatecallsave(numsavedstatesmax, numfunctionmax, numnodemax) As Integer 'stored annealing states
		   status As Boolean
		   
			'marc EN 1.1 5/9/03:
			numpath(numsavedstatesmax) As Integer 'number of paths (node pairs or calling pairs) to define state i

		   'path(i, inod) is a list of nodes in reverse order, from finish to start, in state i
		   'path(numsavedstatesmax, numnodemax, numnodemax) As Integer
		   'numpathnode(numsavedstatesmax, numnodemax2) As Integer  'number of nodes on the defined path path(inode) for state i
		   'pathedge(i,inod) is a list of all defined links associated with the above path
		   'pathedge(numsavedstatesmax, numnodemax) As Integer
		
		   'psuccess = probability of no failures in state
		   'psuccess(numsavedstatesmax) As Single
		   'pfailure(numsavedstatesmax) As Single
		   
		   'nodeused(numsavedstatesmax, numnodemax) As Boolean
		   'linkused(numsavedstatesmax, numlinkmax) As Boolean
		   
		End Type
    </PRE>

	@author robert.e.cranfill@boeing.com
**/
public class State {

public int[][]	fstatecall = new int[SystemDesign.numfunctionmax+1][SystemDesign.numnodemax+1];
public int		numfstatecallsave;
public boolean	status;
public int[]	numpath = new int[SystemDesign.numsavedstatesmax+1];	// 1.1 (sorta)


/**
	Constructor
**/
public State() {

} // constructor ()


} // class State

