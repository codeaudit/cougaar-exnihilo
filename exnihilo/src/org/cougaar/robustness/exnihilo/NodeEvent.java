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
	By-hand port of "nodeeventtype" type, from systemdesignx.bas:

    <PRE>
		Type nodeeventtype
		   node As Integer
		   diskio As Single
		   callfreq As Single
		   cpu As Single
		   memory As Single
		   msgsize As Single 'message data size added to all links between node pairs in the path(*) list
		   ftrace As ftracetype
		   f As Integer 'function being called for current event
		   fcalledby As Integer 'function f is called by function fcalledby
		   fnodecalledby As Integer 'fnodecalledby is the node where function fcalledby is executed
		   ftime As Single 'nodeevent(*).ftime is event time when call to f occurs
		End Type
    </PRE>

	@author robert.e.cranfill@boeing.com
**/
public class NodeEvent {

public int		node;
public int		f;
public int		fcalledby;
public int		fnodecalledby;
public float	diskio;
public float	callfreq;
public float	cpu;
public float	memory;
public float	msgsize;
public float	ftime;
public FTrace	ftrace;


/**
	Constructor
**/
public NodeEvent() {


} // constructor ()

} // class NodeEvent

