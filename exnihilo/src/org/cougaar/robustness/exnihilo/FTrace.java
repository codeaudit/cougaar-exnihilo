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
	By-hand port of "ftracetype" type, from systemdesignx.bas:

    <PRE>
		Type ftracetype
		   f(treedepthmax) As Integer   'f(1) -> f(2) -> f(3) -> f(4) -> f(numfcall) == active function being traced
		   node(treedepthmax) As Integer  'function call in traceback ftrace(i) resides on node(i)
		   numftrace As Integer 'total number of functions in traceback
		   path(treedepthmax2) As Integer 'list of ALL nodes along path for LAST call, from f(numcall-1) -> f(numcall)
		   numpathnode As Integer 'number of nodes in route
		End Type
    </PRE>

	@author robert.e.cranfill@boeing.com
**/
public class FTrace {

public int[]	f = new int[SystemDesign.treedepthmax];
public int[]	node = new int[SystemDesign.treedepthmax];
public int		numftrace;
public int[]	path = new int[SystemDesign.treedepthmax2];
public int		numpathnode;


/**
	Constructor
**/
public FTrace() {

} // constructor ()


} // class FTrace


