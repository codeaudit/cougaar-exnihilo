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
	By-hand port of "functiontype" type, from systemdesignx.bas:
	
    <PRE>
	Type functiontype
	   name As String * 40
	   ifgroup As Integer
	   id As Integer
	   costpercall(numnodemax) As Single
	   costpernode(numnodemax) As Single
	   costpermonth(numnodemax) As Single
	   
	   cpu(numnodemax) As Single  'xnfunction(ifn).cpu(inod) = cpu burn per function execution on node inod
	   cpurate(numnodemax) As Single  'xnfunction(ifn).cpurate(inod) = cpu burn rate per function execution on node inod, units of (cpu sec)/(real sec)
	   
	   memory(numnodemax) As Single 'memory requirement for function ifn on node inod
	   diskiopercall(numnodemax) As Single
	   diskspacereq(numnodemax) As Single   'storage space required to store function, in mb
	   felig(numnodemax) As Integer  'eligible to have current function assigned to a node inod
	   flock(numnodemax) As Integer  'function is required to be on node inod
	   floadtype As Integer  'this is the function load type. Initial Workload = 1, other called loads = 0
	   numfunctioncall As Integer
	   fcall(numfunctionmax) As functioncalltype
	   
	   'run time variables:
	   'xnfunction(ifn).callrate(isrcnod, itargnod) ifn is called from node iscrnod, and run on node itargnod
	   callrate(numnodemax, numnodemax) As Single
	   callinit As Integer '-1=> no initial call, 0=> single initial call, 9=> call at rate 9/sec
	
	   'xnfunction(ifn).nodelist(i) is a list of all nodes for which ifn is installed
	   'xnfunction(ifn).nodelist(0) is the total number of nodes in the list
	   'this is used in annealing, so we don't have to sample states where nodes have felig(inod) = 0
	   nodelist(numnodemax) As Integer
	   
	   objfn As Boolean
	End Type
    </PRE>

	@version EN2.09 - removed fields: callrate, callinit
	@author robert.e.cranfill@boeing.com
**/
public class Function {

public String 		name;
public int			ifgroup;
public int			id;
public float[]		costpercall = new float[SystemDesign.numnodemax+1];
public float[]		costpernode = new float[SystemDesign.numnodemax+1];
public float[]		costpermonth = new float[SystemDesign.numnodemax+1];
public float[]		cpu = new float[SystemDesign.numnodemax+1];
public float[]		cpurate = new float[SystemDesign.numnodemax+1];
public float[]		memory = new float[SystemDesign.numnodemax+1];
public float[]		diskiopercall = new float[SystemDesign.numnodemax+1];
public float[]		diskspacereq = new float[SystemDesign.numnodemax+1];
public int[]		felig = new int[SystemDesign.numnodemax+1];	// used like a boolean
public int[]		flock = new int[SystemDesign.numnodemax+1]; // ditto
public int			floadtype;
public int			numfunctioncall;
public FunctionCall	fcall[] = new FunctionCall[SystemDesign.numfunctionmax+1];

// 'en 2.06 cleanup of xnfunction, remove callrate, callinit:
//public float[][]	callrate = new float[SystemDesign.numnodemax+1][SystemDesign.numnodemax+1];
//public int		callinit;

public int			nodelist[] = new int[SystemDesign.numnodemax+1];
public boolean		objfn = true;


/**
	Constructor
**/
public Function() {

    // Must init non-primitive types.
    //
    for (int i=1; i<=SystemDesign.numfunctionmax; i++) {

        fcall[i] = new FunctionCall();
    }
    
    for (int i=1; i<=SystemDesign.numnodemax; i++) {

        costpernode[i]  = 1.0f;      // zero cost gives EN the willies.
        costpermonth[i] = 1.0f;
        }
}	// end no-arg, mininal constructor


/**
	Nice constructor
**/
public Function(String name) {

	this();
    this.name = name;



} // mice constructor


} // class Function

