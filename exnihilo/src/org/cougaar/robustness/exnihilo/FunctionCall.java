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
	By-hand port of "functioncalltype" type, from systemdesignx.bas:
	
    <PRE>
		Type functioncalltype
		   ifgroup As Integer
		   sendmsgsize As Single
		   recvmsgsize As Single
		   
		   sendmsgrate As Single 'xnfunction(ifnsource).sendmsgrate(ifntarg) bytes/sec
		   
		   callpct As Single
		   callfreq As Single
		   fcallconstraint As Integer
		   nodcallconstraint(numnodemax) As Integer
		   orcallgroup As String * 10
		End Type
    </PRE>

	@version EN2.09 - removed unused fields fcallconstraint, nodcallconstraint, orcallgroup
	@author robert.e.cranfill@boeing.com
**/
public class FunctionCall {

public int		ifgroup;
public float	sendmsgsize;
public float	recvmsgsize;
public float	sendmsgrate;
public float	callpct;
public float	callfreq;

//   'en 2.06 cleanup of xnfunction, remove fcallconstraint, nodcallconstraint, orcallgroup
//public int		fcallconstraint;
//public int[]	nodcallconstraint = new int[SystemDesign.numnodemax+1];
//public String	orcallgroup;


/**
	Constructor
**/
public FunctionCall() {

} // constructor ()


} // class FunctionCall
