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

import org.cougaar.scalability.util.*;
import java.util.*;


/**
 * LoadBalanceRequest encapsulates a CougaarSociety object
 * and any other LB data we need....
 * 
 * isResult indicates whether this is a load balancing request (isResult==false),
 * or a solution (isResult==true).
 *
 * 
 * annealTimes are specified in seconds; 
 *      annealTimes=-1 indicates default time (as determined by AnnealForm).
 * 
 * @author robert.e.cranfill@boeing.com
 *
 */
public class LoadBalanceRequest 
implements java.io.Serializable {


/** Arbitrary but distinct identifiers (happen to correspond to test cases, but not necessarily) **/
public static final int		SOLVER_MODE_MIN_PFAIL = 1;
public static final int		SOLVER_MODE_MIN_PFAIL_WITH_MIN_NODE_COUNT = 2;
public static final int		SOLVER_MODE_LOAD_BALANCE = 3;
public static final int		SOLVER_MODE_BLEND_PFAIL_LOAD_BALANCE = 4;
public static final int		SOLVER_MODE_MIN_MESSAGING = 5;
public static final int		SOLVER_MODE_BLEND_PFAIL_MIN_MESSAGING = 6;

/** Which mode we'll operate in **/
private int 			solverMode_;

/** Our encapsulated CougaarSociety; holds society/node/agent info. */
private CougaarSociety	society_;

/** Requested amount of time to spend annealing, in seconds; -1 indicates to use EN default. */ 
private int				annealTime_;

/** 
 * If false, this is an annealing request; if true, it's a result. 
 * Note that 'result' just means it's a reply sent back from EN; 
 * it may not be a good solution, however - 
 * you should check annealingSolutionFound_ to see if it's a good solution. 
 **/
private boolean			isResult_ = false;

/** 
 * Indicates whether EN thinks it found a good solution. 
 **/
private boolean			annealingSolutionFound_ = false;

/** 
 * Probabilty of failure, as calculated by EN; 
 * valid only if isResult_==true and annealingSolutionFound_==true 
 **/
private float			pFail_ = -1.0f;

/** 
 * For fun, the number of iterations a result took; 
 * only valid if isResult==true. 
 **/
private long			iterations_ = 0;

/**
 * Use 'hamming', that is, minimize unnecessary moves? Usually true.
 * 
 */
private boolean doHamming_;


//protected List		movedAgents_ 	= new ArrayList();
protected List		newNodes_ 		= new ArrayList();
protected List		killedNodes_ 	= new ArrayList();
protected List		leaveAsIsNodes_ = new ArrayList();


/**
 * Some more output data re: the loadbalance solution. Not sure that anyone will care.
 */
public float	finalEngergy_ = -1f;
public float	pfailuresurvive_= -1f;
public float	pfailureave_= -1f;
public float	objectiveave_= -1f;
public float	hamming_= -1f;


/**
 * No-arg constructor will
 * <UL><LI>use metric data for CPU and RAM requirements,</LI>
 * <LI>load balance (SOLVER_MODE_BLEND_PFAIL_LOAD_BALANCE: minimize pFail*agentCount per node),</LI>
 * <LI>use default annealing time (AnnealForm.DEFAULT_ANNEALING_TIME_SEC).</LI></UL>
 *
*/
public
LoadBalanceRequest() {

    society_    = null;
    isResult_   = false;
    annealingSolutionFound_ = false;

	annealTime_ = -1;       // use SystemDesign's default
	solverMode_ = SOLVER_MODE_LOAD_BALANCE; // seems like the most useful mode
	doHamming_ = true;
	}


/**
 * This constructor will
 * <UL><LI>use metric data for CPU and RAM requirements (but not message traffic),</LI>
 * <LI>load balance (minimize pFail*agentCount per node).</LI></UL>
 *
*/
public 
LoadBalanceRequest(int annealTime, int solverMode, boolean doHamming) {

	this();

	annealTime_ = annealTime;
	solverMode_ = solverMode;
	doHamming_  = doHamming;
	}


public int
getSolverMode() {
	return solverMode_;
	}


/**
 * This constructor will
 * <UL><LI>use metric data for CPU and RAM requirements (but not message traffic),</LI>
 * <LI>use a particular solver mode,</LI>
 * <LI>specify hamming or not,</LI>
 * <LI>specify a list of agents that were on the killed node(s).</LI></UL>
 *
*/
public 
LoadBalanceRequest(int annealTime, int solverMode, boolean doHamming, 
					List newNodes, List killedNodes, List leaveAsIsNodes) {

	this(annealTime, solverMode, doHamming);

	newNodes_	 	= newNodes;
	killedNodes_ 	= killedNodes;
	leaveAsIsNodes_	= leaveAsIsNodes;

//	movedAgents_ = hammeredAgents;
	}


/**
 * Getter
 * @param hammeredNodeName
 */
public List
getNewNodes() {
	return newNodes_;
	}


/**
 * Getter
 * @return
 */
public List
getKilledNodes() {
	return killedNodes_;
	}


/**
 * Getter
 * @return
 */
public List
getLeaveAsIsNodes() {
	return leaveAsIsNodes_;
	}


/**
 * Set 'do hamming' flag.
 */
protected void
setDoHamming(boolean doHamming) {
	doHamming_ = doHamming;
	}


/**
 * Get 'do hamming' flag.
 */
public boolean
getDoHamming() {
	return doHamming_;
	}


/**
 * Set the probability of failure.
 */
protected void
setPFail(float pFail) {
	pFail_ = pFail;
	}


/**
 * Get the probability of failure.
 */
public float
getPFail() {
	return pFail_;
	}


/**
 * Set the 'found an annealing solution' flag.
 */
protected void
setSolutionFound(boolean found) {
	annealingSolutionFound_ = found;
	}


/**
 * Get the annealing solution state. Only valid if isResult==true.
 */
public boolean
wasSolutionFound() {
	return annealingSolutionFound_;
	}


/**
 * Set the annealing time, in seconds.
 */
public void
setAnnealTime(int annealTime) {
    annealTime_ = annealTime;
	}


/**
 * Get the annealing time, in seconds.
 */
public int
getAnnealTime() {
    return annealTime_;
	}


/**
 * Set the iteration count.
 */
protected void
setIterations(long iters) {
    iterations_ = iters;
	}


/**
 * Get the iteration count.
 */
public long
getIterations() {
    return iterations_;
	}


/**
 * Public accessor.
 */
public void
setCougaarSociety(CougaarSociety cs) {
    society_ = cs;
	}


/**
 * Public accessor.
 */
public CougaarSociety
getCougaarSociety() {
    return society_;
	}


/**
 */
protected void
isResult(boolean isResult) {
    isResult_ = isResult;
	}


/**
 * Public accessor.
 */
public boolean
isResult() {
    return isResult_;
	}


/**
 * Pretty-print to HTML.
 */
public String
toHTML() {

    StringBuffer result = new StringBuffer();
    
    result.append("<TABLE BGCOLOR=\"#DDDDDD\" CELLPADDING=\"4\" CELLSPACING=\"6\" BORDER=\"2\">\n");
//     result.append("<TABLE COLS=\"2\" BGCOLOR=\"#DDDDDD\" CELLPADDING=\"4\" CELLSPACING=\"6\" BORDER=\"2\">\n");

    result.append("<TR><TH>Node</TH><TH>Agent</TH></TR>\n");

    for (Iterator it = society_.getNodes(); it.hasNext();) {
        
        CougaarNode node = (CougaarNode)it.next();
        boolean firstListing = true;
        int iAgent = 0;
        for (Iterator it2 = node.getAgents(); it2.hasNext(); iAgent++) {

            if (firstListing) {
                result.append("<TR><TD>" + node.getName() + "</TD>");
                firstListing = false;
            	}
            else {
                CougaarAgent agent = (CougaarAgent)it2.next();
                result.append("<TD>" + agent.getName() + "</TD>\n");  
                if (iAgent % 4 == 0) {
                    result.append("</TR>\n<TR><TD></TD>\n");
	                }
	            }
	        }
	    }
    result.append("</TABLE>\n");
    
    return result.toString();
	}


/**
 * Show different fields depending on whether we're a result or a request.
 */
public String
toString() {

	int nodes=0;
	int agents=0;
	if (society_ != null) {
		for (Iterator it = society_.getNodes(); it.hasNext(); nodes++) {
			for (Iterator it2 = ((CougaarNode)it.next()).getAgents(); it2.hasNext(); agents++, it2.next()) {
				}
			}
		}

	return "[LoadBalanceRequest: t=" + annealTime_ + ", " + 
        (isResult_?("iter=" + iterations_ + ", "):"") + // only show iter for solutions
        (isResult_?("annealed=" + annealingSolutionFound_ + ", "):"") + // only show annealingSolutionFound_ for (purported) solutions
        nodes + " node" + ((nodes==1)?"":"s") + ", " +
        agents + " agent" + ((agents==1)?"":"s") + "]";
	}


} // LoadBalanceRequest
