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
	By-hand port of "entropytype" type, from systemdesignx.bas:
<PRE>
	Type entropytype
	   numentropysamples As Integer
	   entropydataready As Boolean
	   ientropyobjsamplelast As Integer
	   objlist(entropyobjsampledim) As Double
	   obj2list(entropyobjsampledim) As Double
	   temp(5000) As Single
	   c(5000) As Single
	End Type
</PRE>
	@version EN2.09 - removed fields objlist & obj2list, even though this object is now obsolete
	@author robert.e.cranfill@boeing.com
**/
public class Entropy {

public int 		numentropysamples;
public boolean	entropydataready;
public int		ientropyobjsamplelast;
//public float[]	objlist  = new float[SystemDesign.entropyobjsampledim];
//public float[]	obj2list = new float[SystemDesign.entropyobjsampledim];
public float[]	temp = new float[5000];
public float[]	c = new float[5000];


/**
	Constructor
**/
public Entropy() {

} // constructor ()

} // class Entropy

