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

package org.cougaar.robustness.exnihilo.plugin;


/**
 * Predicate which matches all LoadBalanceRequest objects.
 * Perhaps should allow user to differentiate between solutions and requests?
 * (instead of just returning "instanceof LoadBalanceRequest", 
 *   return "(obj instanceof LoadBalanceRequest) && (obj.isSolution())"
 * or something like that....)
 * 
 * @author robert.e.cranfill@boeing.com
 *
 */
public class LoadBalanceRequestPredicate 
implements org.cougaar.util.UnaryPredicate {

private final boolean wantResults_;

public 
LoadBalanceRequestPredicate(boolean wantResults) {
	
	wantResults_ = wantResults;	
}

/**
 * Match any LoadBalanceRequest.
 */
public boolean 
execute(Object obj) {

	if ((obj instanceof LoadBalanceRequest) == false) {
		return false;
	}
	return !(wantResults_ ^ ((LoadBalanceRequest)obj).isResult());	// NOT XOR
}


} // LoadBalanceRequestPredicate
