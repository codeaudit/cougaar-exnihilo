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

import org.cougaar.robustness.exnihilo.plugin.EN4JPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.scalability.util.*;	// from Scalability_infoether_utilities.jar
import org.cougaar.util.log.Logger;

import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.io.FileWriter;
import java.io.OutputStreamWriter;


/**
	By-hand port of systemdesignx.bas. <P>
	Mostly just defines data structures that were global in the VB,
	and contains utility methods for UL usage.<P>


	@version EN2.20 - output iteration count.
	@version EN2.09 - removed fields: thread, subthreadopt, statelistpath, blendpfailhammingfirstpass
					- reduced size of: pathsave
					- added EN 2.03 code to blend load balancing and messaging, as noted
	@version 26May04 - Allow resetting of numfunctionmax
	@version 17Mar04 - Removed fixHammingForAgentsOnKilledNodes, useless.
 	@version 18jun03 - fixed "integration violations" (most)
	@version 30apr03 - supporting latest ENVB mods.
    @version 03apr03 - for Cougaar.
    @version 27mar03 - added test case "C" for large society
 	@version 1.1 - port of EN 1.1; incorporated changes by hand; global logger (ok?)

	@author robert.e.cranfill@boeing.com
**/
public class SystemDesign {

// Logger to use.
private static Logger   logger_ = LoggingService.NULL;

// some debug output stuff. should use logger...
private boolean writeAnnealResultToConsole_ = false;
private boolean writeAnnealStateToFiles_ = false;

private AnnealForm annealForm_;

// Is messaging A->A paid attention to? Not at this time.
private boolean  ignoreSelfMessing_ = true; 

private long     lastAnnealIterCount_ = 0;


//
// Constants
//
/** 'darparun' controls a lot of optional processing,
 *  most/all of which is untested for the 'darparun==false' case!
 */
protected final boolean	darparun = true;

// Magic numbers???
private static final float	SOFT_CONSTRAINTS_BIGNUM = Float.MAX_VALUE; 	// was 9E+19f;
private static final int 	MAGIC_8BALL = Integer.MAX_VALUE - 1000;		// was 8000000000
private static final int	MAGIC_9BALL = Integer.MAX_VALUE; 	 	 	// was 9000000000

//
// Max sizes of things. Semi-arbitrary, and you can probably change these values.
//

// 1.1: comments say numfunctionmax=70, but the code still says 100, so...
//
// 26May04 - allow override of some of these limits:
//
public static /* 26May04 - final */ int numfunctionmax = 100; // 26May04: back to 100 default; 23sept03: 200 from 100;	// Max number of 'functions', that is, agents.
public static /* 26May04 - final */ int numfunctionmaxsquared = numfunctionmax * numfunctionmax; // 1.1
public static final int numnodemax          =  40; // 26May04: to 40; 23sept03 to 50 from 100

public static final int numsavedstatesmax   =   2; // 23sept03 - was 3
public static final int numfgroupmax        = 100;
public static final int numnodeeventmax     = 200;
public static final int treedepthmax        =  30;

// Derived/dependent constants - from other constants, above.
public static /* 26May04 - final */ int numfunctionnodemax  = numfunctionmax * numnodemax; // 1400; // 1.1 - originally numfunctionmax * numnodemax ???
public static final int numlinkmax          = numnodemax * 2; // was hardcoded to 60
public static final int treedepthmax2       = treedepthmax * 2; 		// (sometimes foo2 means foo*2...
public static final int numnodemax2 		= numnodemax * numnodemax;  //      ...and sometimes foo^2!)

public static final int numnodemaxchoose2 = numnodemax * (numnodemax - 1) / 2;	// EN2.21 bugfix for array dimensions - 16Sept04

// Network-oriented values?
public static final int tcpoverhead = 20; // 'bytes
public static final int tcpmtu 		= 65515;
public static final int ipoverhead 	= 20; // 'bytes
public static final int ipmtu 		= 65515;

// does this really belong here?
public static String	EN_TEMP_NODE_NAME = "_ENTempNode_";


// Inconstants - grouped by class, sorted by variable name.
// - Primitive types first, objects and arrays next...
//
public boolean	iteranel;		// controls iteration of the annealing solver 
public boolean	lockState;
public boolean 	mobilitygroupsenabled;
//// 'en 2.03 cleanup: DELETE subthreadopt
// public boolean	subthreadopt;

public int		activetracefunction;
public int		activetracenode;
public int		numfcallevent;
public int		numfgroup;
public int		numfunction;	// number of functions - that is, agents - in system 
public int 		numlink;		// number of links in system 
public int 		numnode;		// number of nodes in system 
public int 		numnodeevent;
public int 		numpath;
public int 		numpathnode;
public int		numstatelist;
public int 		srcfn;
public int 		srcnod;
public int		gensamplecount;	// originally a long, but we can't generate that many samples!

public float	annealtemp;
public float	fixedcost;
public float	fixedcostave;
public float	fixedcostmax;
public float	fixedcostmin;
public float	fixedcostsamplemax;
public float	fixedcostsamplemin;
public float	fixedcostscale;
public float	ftracetime;
public float	learnrate;
public float	maxannealingincrements = Integer.MAX_VALUE; // was 9000000000# in systemdesign.frm ???
public float	monthlycost;
public float	monthlycostave;
public float	monthlycostmax;
public float	monthlycostmin;
public float	monthlycostsamplemax;
public float	monthlycostsamplemin;
public float	monthlycostscale;
public float	objectiveave;
public float	pfailureave = 0.5f;	// ??? 19aug03
public float	pfailuremax;
public float	pfailuremin;
public float	pfailuresamplemax;
public float	pfailuresamplemin;
public float	pfailurescale;
public float	psuccess;
public float	responsetimeave;
public float	responsetimemax;
public float	responsetimemin;
public float	responsetimesamplemax;
public float	responsetimesamplemin;
public float	responsetimescale;

public float dmemorypctmax, dmemorypctmin;


// Objects and arrays
//
public Agent[]           agent = new Agent[numfunctionmax+1]; // "Agent" here is not the same as Function, which more directly represents UL agents.
public CallList[]        calllist = new CallList[100+1];
public FCallEvent[]      fcallevent = new FCallEvent[numfunctionnodemax+1];
public String[]          fgroup = new String[numfgroupmax+1];
public int[]             ifninitcall = new int[numfunctionmax+1];
public int[]             linkcount = new int[numlinkmax+1];
public int[][]           linkid = new int[numnodemax+1][numnodemax+1];
public Link[]		     linkinfo = new Link[numlinkmax+1];
public NodeEvent[]       nodeevent = new NodeEvent[numnodeeventmax+1];
public Node[]            nodeinfo = new Node[numnodemax+1];
public ObjFnJobSet       objfnjobset = new ObjFnJobSet();
public int[]             path = new int[numnodemax+1];

// EN2.09
//'en 2.07 cleanup of pathsave:
//'in a SWITCHED ETHERNET, there are a maximum of 3 nodes per path, rather than numnodemax
//'IMPORTANT: THIS DOES NOT WORK IN A GENERAL MULTIPLY CONNECTED TOPOLOGY !!! (Ultralog ONLY)
public int[][]           pathsave = new int[numfunctionmaxsquared+1][3+1];	

public float[]           plink = new float[numlinkmax+1];
public int[]             requiredfunctions = new int[numfunctionmax+1];
public int[]             requiredlans = new int[numnodemax+1];
public int[]             requirednodes = new int[numnodemax+1];
public int[]             requiredrouters = new int[numnodemax+1];
public State             state = new State();
public StateList[]       statelist = new StateList[numsavedstatesmax+1];

// EN2.09: 'en 2.04 statelistpath removal: delete the global var statelistpath - reduce memory footprint
// public int				 statelistpath[][][] = new int[numsavedstatesmax+1][numfunctionmaxsquared+1][numnodemax+1]; // 1.1 ??? rather large array

// EN2.09: 'en 2.03 cleanup: DELETE thread(1)
// public SampleThread[]    thread = new SampleThread[10+1];
public Function[]        xnfunction = new Function[numfunctionmax+1]; // Array of functions; that is, agents.

public NodeOverload[]	nodeoverload = new NodeOverload[numnodemax+1]; // 30april03
public int 				numnodeoverload;

// 'en 2.05 cleanup: delete this entropy section
// public static final int entropyobjsampledim = 200;
// public boolean 			entropyrun = false;
// public Entropy			entropy = new Entropy();


// 'messaging mods
public float remotetraffic;
public float remotetrafficave;
public float remotetrafficscale;
public float remotetrafficmin;
public float remotetrafficmax;

public float remotetrafficsamplemin;
public float remotetrafficsamplemax;

// 'marc EN 1.1 smoothing
public float  pfailuresurvive;
public float  pfailuresurviveave;
public float  pfailuresurvivescale;
public float  pfailuresurvivemin = 0.0001f;	
protected float  pfailuresurvivemax = 0;		// this needs to be set to objfnjobset.numobjfn once we know it.

// 'en 1.1 hamming
public float  hamming;
public float  hammingave;
public float  hammingscale;
public float  hammingmin;
public float  hammingmax;

// 'marc EN 1.1 smoothing
public float pfailuresurvivesamplemin;
public float pfailuresurvivesamplemax;

// 'marc EN 1.1 hamming
public float hammingsamplemin;
public float hammingsamplemax;

// 'marc EN 1.1 Smoothing
public 		boolean blendpfailsurvive  = false;
public 		boolean blendpfailmessaging  = false;
protected 	boolean blendpfailsurvivefirstpass  = false;
protected 	boolean blendpfailmessagingfirstpass  = false;

// 'marc EN 1.1 hamming
public 		boolean blendpfailhamming  = false;
// 'marc en 2.02 cleanup, eliminate unused var
// protected 	boolean blendpfailhammingfirstpass  = false;

// 'marc en 1.1 speedup and misc
public boolean tracethisstate;
public boolean solverwarmup;

// 'marc en 1.1 soft constraints
public float dcpupctmaxmin;
public float dmemorypctmaxmin;
public float dcpupctave;
public float dmemorypctave;
public float cpuoverloadbinarysearchtolerance;
public float memoryoverloadbinarysearchtolerance;

// 1.1
public FunctionSmoothing xnfunctionfirstpass[] = new FunctionSmoothing[numfunctionmax+1];

// 'marc en 1.1 soft constraints
public float cpufactor;
public float memoryfactor;

public float totcpuavail;
public float totcpurequired;
public float totcpurequiredmin;
public float totcpurequired25;
public float totcpurequired50;
public float totcpurequired75;
public float totcpurequired100;
public float totmemoryavail;
public float totmemoryrequired;
public float totmemoryrequiredmin;
public float totmemoryrequired25;
public float totmemoryrequired50;
public float totmemoryrequired75;
public float totmemoryrequired100;

// 'en 1.1 performance map
// 'geneticfusionmode = 0 (no fusion), = 1 (cold fusion), = 2 (warm fusion)
public int geneticfusionmode;

protected int minultralognodes; // 'minimum number of nodes required for an Ultralog run

// 'genetic algo stuff:
public float[][]	plinkannealtemp = new float[10+1][numlinkmax+1];
public float[] 		plinkannealtemptotal = new float[numlinkmax+1];
public float		currentmapfusionrate;
public boolean[]	linkgenepool = new boolean[numlinkmax+1];
public boolean[]	linkgenepoolstep = new boolean[numlinkmax+1];
public int			genpixload;
public int			genpixtotalload;
public int[]		nlev = new int[4+1];
public int[]		plinkminlev = new int[4+1];

// 'linked list data for dykstra
public float[] 		dist = new float[numnodemax+1];
public float[] 		disttime = new float[numnodemax+1];
public float[][] 	dtime = new float[numnodemax+1][numnodemax+1];
public float[] 		distfail = new float[numnodemax];	// 'used for dykstra with failure paths
public int[] 		nextvnear = new int[numnodemax+1];
public int 			headvnear;                 
public int[] 		nextvfar = new int[numnodemax+1];
public int[] 		prevvfar = new int[numnodemax+1];
public int 			headvfar;
public int 			tailvfar;
public int[][] 		edge = new int[numnodemax2+1][2+1];

//rec - these are already defined
// 'path(i) is a list of nodes in reverse order, from finish to start
// public int[] path = new int[numnodemax+1];
// public int numpathnode;  // 'number of nodes on the defined path path(inode)

// 'pathedge(i) is a list of all defined links associated with the above path
public int[] 		pathedge = new int[numnodemax+1];

private int			warnMsgPath_; // how many "no message path" warnings

// flag to set so that AnnealForm.MetroParams will solve for survivability or min traffic (UGLY!!! FIX???)
protected boolean		solveWithSurvivability_ = false; 
protected boolean		solveWithTraffic_ = false;

public boolean			writeENDataFiles_ = false;


// 'marc EN 2.03 blend load balancing and messaging
public float loadbalminfirstpass;
public float loadbalmaxfirstpass;

// 'marc EN 2.01 load balancing soft constraints
boolean blendpfailloadbal;

// 'marc EN 2.02 blend load balancing and messaging
boolean blendloadbalmessaging;
boolean blendloadbalmessagingfirstpass;

// 'en 2.01 load balancing, constraints on load balancing
boolean binarysearchsoftconstraints;
boolean loadbalconstraints;

// 'en 2.0 load balance
public float  loadbal;
public float  loadbalave;
public float  loadbalscale;
public float  loadbalmin;
public float  loadbalmax;
public float  loadbalcpumemratio;
public float  loadbalsamplemin;
public float  loadbalsamplemax;


public static void
setNewMaxAgents(int newMaxAgents) {
	
	numfunctionmax = newMaxAgents;
	numfunctionmaxsquared = numfunctionmax * numfunctionmax;
	numfunctionnodemax = numfunctionmax * numnodemax;
	}


/**
 * For all agents on all nodes in the list of killed nodes,
 * set the hamming node to the EN_TEMP_NODE_NAME node, 
 * instead of the now-clobbered node.
 * 
 * 17Mar04 - This didn't work, becase if a node had been in the killed list, 
 * it was simply never created in the CS data.
 * So we can't find it's node ID to fix any agents that were hamming to it.
 * Now, we set the hamming ID for agents that were on killed nodes to the _TEMP_ node,
 * which has almost zero capacity, and thus the agents won't/can't stay there.
 * QED.
 * 
 * @version 17Mar04 - no longer needed, no longer used.
 * @param moved
 */
//public void
//fixHammingForAgentsOnKilledNodes(List killedNodes) {
//
//	if (killedNodes == null  ||  killedNodes.isEmpty())
//		return;
//
//	// find the temp node to be used for hamming
//	int tempNodeIndex = 0;
//	for (int i=1; i<=numnode; i++) {
//		if (nodeinfo[i].name.equals(SystemDesign.EN_TEMP_NODE_NAME)) {
//			tempNodeIndex = i;
//			break;
//			}
//		}
//	if (tempNodeIndex == 0) {
//		if (this.isErrorEnabled())
//			logError("clearHammingForAgent: Can't find temp node!");
//		return;
//		}
//	if (this.isDebugEnabled())
//		logDebug("clearHammingForAgent: temp node is # " + tempNodeIndex);
//
//
//	// Find every agent on each killed node, set its hamming id to the temp node.
//	//
//	Iterator iNode = killedNodes.iterator();
//	while (iNode.hasNext()) {
//
//		String killedNode = (String)iNode.next();
//		if (this.isDebugEnabled())
//			logDebug("clearHammingForAgent: checking agents on node " + killedNode);
//
//		// Find the index of this node
//		int killedNodeIndex = 0;
//		for (int i=1; i<=numnode; i++) {
//			if (nodeinfo[i].name.equals(killedNode)) {
//				killedNodeIndex = i;
//				break;
//				}
//			}
//		if (killedNodeIndex == 0) {	// this would always happen
//			if (this.isErrorEnabled())
//				logError("clearHammingForAgent: Can't find index for killed node!");
//			continue;
//			}
//
//		// Every agent hamming to that node can't go there any more.
//		//
//		for (int i=1; i<=numfunction; i++) {
//			if (agent[i].hamminghostid == killedNodeIndex) {
//				agent[i].hamminghostid = tempNodeIndex;
//				if (this.isDebugEnabled())
//					logDebug("clearHammingForAgent: setting agent " + xnfunction[i].name 
//									+ " hamming id=" + tempNodeIndex);
//				// 17Mar04 - no break;
//				}
//			} // every agent
//		} // each node
//
//
//	} // fixHammingForAgentsOnKilledNodes
//

/**
 * #1 or 7
 * also used for 2/8
 */
public boolean
setSolverMode_MinPfail(boolean doHamming) {
	
	solveWithSurvivability_ = false; 
	solveWithTraffic_ 		= false;

	if (doHamming) {
		this.blendpfailhamming = true;
		setupHamming(this.objfnjobset.numobjfn);
		}
	return true;
	}


private void
setupHamming(float hammingMax) {
	
	// these are only pertinent if we're doing hamming
	this.hammingmax = hammingMax;
	this.hammingmin = 0.0001f;
	}


/**
 * #2 or 8
 * @param nodeCount
 * @param doHamming
 * @return
 */
public boolean 
setSolverMode_MinPfailWithMinNodeCount(int nodeCount, boolean doHamming) {
	
	if (nodeCount<0  || nodeCount>this.numnode) {
		if (this.isErrorEnabled())
			logError("setSolverMode_MinPfailWithMinNodeCount: bad nodeCount: " + nodeCount);
		return false;
		}

	setSolverMode_MinPfail(doHamming);

	this.minultralognodes = nodeCount;
	
	return true;
	}


/**
 * #3 or 9
 * also used for 4/10
 */
public boolean 
setSolverMode_LoadBalance(boolean doHamming) {
	
	solveWithSurvivability_ = true;
	solveWithTraffic_ 		= false;

	this.pfailuresurvivemax = this.objfnjobset.numobjfn;

	return true;
	}


/**
 * #4 or 10
 * @param doHamming
 * @return
 */
public boolean 
setSolverMode_BlendPfailLoadBalance(boolean doHamming) {
	
	setSolverMode_LoadBalance(doHamming); 

	blendpfailsurvive = true;

	return true;
	}



/**
 * #5 or 11
 * 					NOT YET TESTED !!!
 * @param doHamming
 * @return
 */
public boolean 
setSolverMode_MinMessaging(boolean doHamming) {

	solveWithSurvivability_ = false; 
	solveWithTraffic_ 		= true;

	this.blendpfailmessaging = false;

	remotetrafficmin = 1;
	remotetrafficmax = Integer.MAX_VALUE;

	return true;
	}


/**
 * #6 or 12
 * 					NOT YET TESTED !!!
 * @param doHamming
 * @return
 */
public boolean 
setSolverMode_BlendPfailMinMessaging(boolean doHamming) {

	solveWithSurvivability_ = true; 
	solveWithTraffic_ 		= true;

	this.blendpfailmessaging = true;

	return true;
	}

/**
 * Given an SD with loaded EN data, return a CS representing it.
 *
 *@param useInitialData - if true, use initfn (original loc of agent),
 *							otherwise use annealhostid
 */
public CougaarSociety
getCSForCurrentData(boolean useHammingData, boolean createMessagingData) {

	CougaarSociety soc = new CougaarSociety("Created by EN4J from internal data " + new java.util.Date());

	// create the nodes - skip the router (index=numnode)
	//
	for (int inode=1; inode<this.numnode; inode++) {

		CougaarNode cougNode = soc.newNode(this.nodeinfo[inode].name);

	// debug why EN_TEMP_NODE_NAME still populated...
		if (nodeinfo[inode].name.equals(EN_TEMP_NODE_NAME)) {
			
			if (this.isDebugEnabled())
				logDebug("getCSForCurrentData: " + EN_TEMP_NODE_NAME + " is #" + inode);

			}

/*
 * xnfunction[ifn].cpurate[inod]
 */
		cougNode.setAttribute(CougaarNode.CPU,    ""+(int)this.nodeinfo[inode].cpuutilmax); // ???
		cougNode.setAttribute(CougaarNode.MEMORY, ""+(int)this.nodeinfo[inode].memory);
		cougNode.setAttribute(CougaarNode.PROBABILITY_OF_FAILURE, ""+this.nodeinfo[inode].pfail);

		// agents on the node
		for (int iagent=1; iagent<=numfunction; iagent++) {

			Agent thisAgent = this.agent[iagent];
			String thisAgentName = xnfunction[iagent].name;
			
			int targetHostID;
			if (useHammingData) {
				targetHostID = thisAgent.hamminghostid;
				}
			else {
				targetHostID = thisAgent.annealhostid;
				}

			if (targetHostID == inode) {

				CougaarAgent cougAgent = cougNode.newAgent(thisAgentName);
				float cpu = xnfunction[iagent].cpurate[inode]*100.0f; // ???
				float ram = xnfunction[iagent].memory[inode];
				cougAgent.setRequirement(CougaarAgent.CPU,    ""+(int)cpu);
				cougAgent.setRequirement(CougaarAgent.MEMORY, ""+(int)ram);


//				int hid = this.agent[iagent].annealhostid;	// ??? or hamminghostid, above ???

				for (int jagent=1; jagent<=numfunction; jagent++) {
	
					float thisMessaging = xnfunction[iagent].fcall[jagent].sendmsgrate;
					if (thisMessaging > 0) {
	
						// find node that target agent is on
						int srcID = this.agent[jagent].annealhostid;

//	// THIS IS GOING TO GO AWAY! we will use the CS for this data..
//						interNodeMessaging_[srcID][targetHostID] += thisMessaging;

						String thatAgentName = xnfunction[jagent].name;
//						System.out.println("[" + thisAgentName + "]BandwidthSent_" + thatAgentName + ": " + thisMessaging);

						cougAgent.setRequirement("BandwidthSent_" + thatAgentName, ""+thisMessaging);
						}
					}
				}

			} // agents
		} // nodes


//	System.out.println("");
//	System.out.println("" + soc.toXML());
//	System.out.println("");

	return soc;
	}


/**
 * 
 * Top-level object for the EN4J solver.
 *
 *  Inititializes all object arrays, except:
 * 
 *   - Whoever loads data (RnR?) will init:
 * 			nodeinfo, fgroup
 * 
 * Note that we pre-allocate a lot of these arrays to some maximum allowed size; very wasteful.
 * 
**/
public SystemDesign() {

    /*
    public Agent[]           agent = new Agent[numfunctionmax+1];
    public CallList[]        calllist = new CallList[100+1];
    public FCallEvent[]      fcallevent = new FCallEvent[numfunctionnodemax+1];
    public String[]          fgroup = new String[numfgroupmax+1];
    public Link[]            linkinfo = new Link[numlinkmax+1];
    public NodeEvent[]       nodeevent = new NodeEvent[numnodeeventmax+1];
    public Node[]            nodeinfo = new Node[numnodemax+1];
    public StateList[]       statelist = new StateList[numsavedstatesmax+1];
    public SampleThread[]    thread = new SampleThread[10+1];
    public Function[]        xnfunction = new Function[numfunctionmax+1];
    public NodeOverload[]	nodeoverload = new NodeOverload[numnodemax+1]; // 1.1

    */
	for (int i=1; i<=numnodemax; i++) {
		nodeoverload[i] = new NodeOverload();
		path[i] = 0;
		}
    
	for (int i=1; i<=numfunctionmax; i++) {
		agent[i] = new Agent();
		xnfunction[i] = new Function();
		xnfunctionfirstpass[i] = new FunctionSmoothing();
		}
    
    for (int i=1; i<=100; i++) {
        calllist[i] = new CallList();
    	}

    for (int i=1; i<=numfunctionnodemax; i++) {
        fcallevent[i] = new FCallEvent();
    	}

    for (int i=1; i<=numlinkmax; i++) {
        linkinfo[i] = new Link();
    	}

    for (int i=1; i<=numnodeeventmax; i++) {
        nodeevent[i] = new NodeEvent();
    	}

    for (int i=0; i<=numsavedstatesmax; i++) {  // Note: zeroth item *is* used, for annealing state.
        statelist[i] = new StateList();
    	}

// EN2.09
//	for (int i=1; i<=10; i++) {
//		thread[i] = new SampleThread();
//		}

    numfgroup = 0;
	minultralognodes = 1;

	// Stuff from VB form's .load method
	//

	// 'marc EN 1.1 smoothing
	blendpfailsurvivefirstpass = true;
	tracethisstate = false;

	// 'marc EN 1.1 hamming
// en 2.02 cleanup, eliminate unused var
//	blendpfailhammingfirstpass = true;

	//  'marc en 2.02 blend load balancing and messaging
	blendloadbalmessagingfirstpass = true;

	// 'en 1.1 soft constraints
	dcpupctmaxmin = SOFT_CONSTRAINTS_BIGNUM;
	dmemorypctmaxmin = Float.MAX_VALUE; // 9E+19f;
	memoryoverloadbinarysearchtolerance = 0.1f;
	cpuoverloadbinarysearchtolerance = 0.1f;

	// 'en 1.1 performance map
	geneticfusionmode = 0; // 'no fusion by default
	currentmapfusionrate = 0.5f;

	// 2.0 - annealForm.frm 5488-5489
	binarysearchsoftconstraints = false;
	loadbalconstraints = true;


	// rec - pick a default solver mode?
	// blendpfailsurvive = true;


	} // SystemDesign constructor



/**
 * Given a CougaarSociety, create internal data structures representing it
 *
 * @return true iff successful.
 * 
 * Assumptions, simplifications:
 *  - All agents networked to all others in a 'star' topology through the one-and-only router.
 * 
 * Questions:
 *  - Is the "MinNodes" attribute still going to be used in 2003? ??? 
 * 		- ans: yes, but not embedded in CougaarSociety (so, the answer here is 'no')
 * 
 * @version 1.1 - set hamminghostid for input agents
 * 
 */
public boolean
loadDataFromSociety(CougaarSociety soc) {

    int minNodesRequested = 0;  // still need to support this hack? ???

	if (soc == null ) {
		if (this.isErrorEnabled()) 
			SystemDesign.logError("loadDataFromSociety: No society defined?");
		return false;
		}
	if (soc.getNodes() == null) {
		if (this.isErrorEnabled()) 
			SystemDesign.logError("loadDataFromSociety: No nodes defined? Metrics failure?");
		return false;
		}

	// Build hash of agent name and index.
	// 
    Hashtable agentFunctionIndex = new Hashtable();
    int agentIndex = 0;
    for (Iterator nodes=soc.getNodes(); nodes.hasNext(); ) {
        for (Iterator agents=((CougaarNode)nodes.next()).getAgents(); agents.hasNext(); ) {
            agentIndex++;
            String agentName = ((CougaarAgent)agents.next()).getName();
            agentFunctionIndex.put(agentName, new Integer(agentIndex));
            }
        }

    //
    // Navigate the objects, create EN data.
    //

    // We need to know how many nodes there are now, so run thru the iterator.
    //
    int nNodesNotRouter = 0;
    for(Iterator nodes=soc.getNodes(); nodes.hasNext(); nodes.next()) {
        nNodesNotRouter++;
        }
    int routerIndex = nNodesNotRouter + 1;

    int iENNode  = 0;    // index of nodes created (will be pre-incremented so it starts at 1)
    int iENAgent = 0;
    int iLink    = 0;

    org.cougaar.robustness.exnihilo.Node thisENNode = null;

    float cpuAttributeForNode[] = new float[numnodemax+1];

    // For each UL Node...
    //
    Iterator nodes = soc.getNodes();

    Object nObj = null;
    while (nodes.hasNext()) {

        nObj = nodes.next();
        if (nObj instanceof CougaarNode == false) {
        	
        	if (this.isWarnEnabled())
            	logWarn("loadDataFromSociety: Expected CougaarNode! (got " 
            			+ nObj.getClass().getName() + ")");
            
            return false;
            }
        iENNode++;
        if (iENNode > SystemDesign.numnodemax) {
			
			if (this.isErrorEnabled())
            	logError("Too many nodes defined! (max is " + SystemDesign.numnodemax + ")");

            return false;
            }


        CougaarNode node = (CougaarNode)nObj;
        if (this.isDebugEnabled())
        	logDebug("CougaarNode: '" + node.getName() + "' - EN node #" + iENNode);

        // Create the node.
        //
		int memory = 10;	// if parseInt fails.
        try {
			memory = Integer.parseInt(node.getAttribute("Memory"));
			}
		catch (NumberFormatException nfe) {	//11sept03 - parsing a float here killed EN
			
			if (this.isWarnEnabled())
				logWarn("Bad Memory attr for node [" + iENNode + "]: " + node.getAttribute("Memory"));
		
				try {
					memory = (int)Float.parseFloat(node.getAttribute("Memory"));
					}
				catch (NumberFormatException nfe2) {
					logWarn("Not good as a Float, either!");
					}
			}
        thisENNode = new Node(node.getName(), Node.TYPE_SERVER, 1, 1, memory);
        thisENNode.memorymax = memory; // per MB 15july02

		thisENNode.pfail = 0; // if parseFloat fails
		try {
        	thisENNode.pfail = Float.parseFloat(node.getAttribute("ProbabilityOfFailure"));
			}
		catch  (NumberFormatException nfe) {
			if (this.isWarnEnabled())
				logWarn("Bad ProbabilityOfFailure attr for node [" + iENNode + "]: " + node.getAttribute("ProbabilityOfFailure"));
			}
        nodeinfo[iENNode] = thisENNode;

        // Keep each CPU's capacity so we can figure what fraction each agent takes (cpurate_)
        //
		float thisCPUCapacity = 10;	// if parseFloat fails
        try {
        	thisCPUCapacity = Float.parseFloat(node.getAttribute("CPU"));
        	}
		catch (NumberFormatException nfe) {
			if (this.isWarnEnabled())
				logWarn("Bad CPU attr for node [" + iENNode + "]: " + node.getAttribute("CPU"));
			}

		if (this.isDebugEnabled())
	        logDebug("    CPU attr [" + iENNode + "] = " + thisCPUCapacity);

        cpuAttributeForNode[iENNode] = thisCPUCapacity;

        // If we don't already have a minNodes, look for it.
        // (notice that the "MinNodes" key string isn't in the CougaarNode object like the others)
        //
        if (minNodesRequested == 0) {
            String testMinReq = node.getAttribute("MinNodes");
            if (testMinReq != null) {
                minNodesRequested = Integer.parseInt(testMinReq);
				if (this.isWarnEnabled())
                	logWarn("(OK; deprecated function) MinNodes attr (set from node " + iENNode + ") = " + minNodesRequested);
				this.minultralognodes = minNodesRequested;
                }
            }

        // Each Node has two links: to and from router.
        //
        org.cougaar.robustness.exnihilo.Link thisLink = null;
        
        thisLink = new Link(thisENNode.name + " -> Router", Link.LINK_TYPE_WAN, iENNode, routerIndex);
        linkinfo[++iLink] = thisLink;

        thisLink = new Link("Router -> " + thisENNode.name, Link.LINK_TYPE_WAN, routerIndex, iENNode);
        linkinfo[++iLink] = thisLink;

        // For each Agent in Node...
        //
        Iterator agents = node.getAgents();
        Object aObj = null;
        while (agents.hasNext()) {

            aObj = agents.next();
            if (aObj instanceof CougaarAgent == false) {
				if (this.isErrorEnabled())
                	logError("Expected CougaarAgent! (got " + aObj.getClass().getName() + ")");
                return false;
                }
            iENAgent++;
            if (iENAgent > SystemDesign.numfunctionmax) {
				if (this.isErrorEnabled())
	                logError("Too many agents defined! (max is " + SystemDesign.numfunctionmax + ")");
                return false;
                }

            CougaarAgent cAgent = (CougaarAgent)aObj;

            // Preserve the Node info? We could, but no.
            //
            String ourAgentName = /* node.getName() + "." + */ cAgent.getName();
			if (this.isDebugEnabled())
	            logDebug(" Agent: '" + ourAgentName + "'");

            // An UL Agent is an EN "Function".
            //
            Function func = new Function(ourAgentName);
            xnfunction[iENAgent] = func;


            // Set this agent's cpurate_ on all nodes to its REQUIREMENT value;
            // Later, we'll adjust ('normalize') this by dividing by each node's ATTRIBUTE (capability)
            //
            float thisAgentCPUReq = EN4JPlugin.MINIMAL_CPU_FOR_AGENT;
            float thisAgentMemReq = EN4JPlugin.MINIMAL_RAM_FOR_AGENT;
			String thisF = null;
            try {
            	thisF = cAgent.getRequirement("CPU");
                thisAgentCPUReq = Float.parseFloat(thisF);
				thisF = cAgent.getRequirement("Memory");
                thisAgentMemReq = Float.parseFloat(thisF);
            }
            catch (NumberFormatException nfe) {
				if (this.isWarnEnabled())
	                logWarn("Bad CPU or Memory req for agent " + ourAgentName + ": '" + thisF + "'");
//                return false;
            }

			if (this.isDebugEnabled()) {
	            logDebug("  CPU req: " + thisAgentCPUReq);
	            logDebug("  RAM req: " + thisAgentMemReq);
				}
			
            // Set this agent's CPU and RAM requirements for all nodes,
            // to be adjusted later by scaling to node's capacity.
            // Eligibility to be adjusted later, also. Assume eligible unless some requirement not met.
            //
            for (int n=1; n<=nNodesNotRouter; n++) {
                func.cpurate[n] = thisAgentCPUReq;
                func.memory[n]  = thisAgentMemReq;
                func.felig[n]   = 1;
                }


            // Set eligibility for each agent on each node to 0 iff 
            // any agent Requirement (other than CPU, Memory, BandwithSent or BandwithReceived)
            // is unsatisfied (that is, not matched exactly) by an attribute of the node.
            // That is, if an agent has "Requirement OS=Linux", node must have "Attribute OS=Linux"
            //
            for (Iterator itReq = cAgent.getRequirementNames(); itReq.hasNext(); ) {
                String reqName = (String)itReq.next();

                if (reqName.equals(CougaarAgent.CPU)==false && 
                    reqName.equals(CougaarAgent.MEMORY)==false && 
                    reqName.startsWith("BandwidthSent_")==false &&
                    reqName.startsWith("BandwidthReceived_")==false ) {

                        String reqVal = cAgent.getRequirement(reqName);
						if (this.isInfoEnabled())
							logInfo("    Agent '" + ourAgentName + "' requires '" + reqName + "'='" + reqVal + "'");

                        // For any node that doesn't have a given requirement (as an attribute), set elig=0
                        //
                        Iterator nodes2 = soc.getNodes();
                        Object nObj2 = null;
                        int iENNode2 = 0;
                        while (nodes2.hasNext()) {

                            nObj2 = nodes2.next();
                            if (nObj2 instanceof CougaarNode == false) {
								if (this.isWarnEnabled())
                                	logWarn("Expected CougaarNode! (got " + nObj2.getClass().getName() + ")");
                                break;
                                }
                            iENNode2++;
                            CougaarNode node2 = (CougaarNode)nObj2;
                            if (reqVal.equals(node2.getAttribute(reqName)) == false) {
                                func.felig[iENNode2] = 0;

								if (this.isInfoEnabled())
                                	logInfo("      CougaarNode '" + node2.getName() + "' does not meet requirement");
                                }
                            } // nodes2
                        }
                    } // requirements

            // Get all the "Bandwith" - messaging - info
            //
			if (this.isDebugEnabled())
	            logDebug("   Bandwidth info:");

            for (Iterator itReq = cAgent.getRequirementNames(); itReq.hasNext(); ) {
                String nextReq = (String)itReq.next();
                if (nextReq.startsWith("BandwidthSent_")) {

                    float sent = Float.parseFloat(cAgent.getRequirement(nextReq));
                    String otherName = nextReq.substring("BandwidthSent_".length());
                    int otherIndex = -1;
                    try {
                        otherIndex = ((Integer)agentFunctionIndex.get(otherName)).intValue();
                    	}
                    catch (java.lang.NullPointerException npe) {
						if (this.isInfoEnabled())
                        	logInfo("*** Can't find agent '" + otherName + "' in inter-agent messaging table!"); // shouldn't happen
                        }
					if (this.isDebugEnabled())
						logDebug("    " + ourAgentName + " (#" + iENAgent + ") sent " + sent + " to   " + 
                             otherName + " (#" + otherIndex + ")");

                    // Care about agents talking to self? (Not for UL.)
                    //
                    if (ignoreSelfMessing_  && iENAgent == otherIndex) {
						if (this.isDebugEnabled())
                            logDebug("\t\t** Ignoring self-messaging for agent '" + ourAgentName + "'");
                        }
                    else {
						if (otherIndex > -1) {	// 10sept03 - ioob
	                        func.fcall[otherIndex].sendmsgrate = sent;
	                        func.fcall[otherIndex].sendmsgsize = 0;   // unused items
	                        func.fcall[otherIndex].callpct     = 0;
	                        func.fcall[otherIndex].callfreq    = 0;
							}
                        }
                    }

//  else { 
//  //  process "BandwidthReceived_" data 
//    }
//    The Cougaar data includes both sides of each pairwise communication.
//    We don't care about recieved data, only sent data 
//     (could do it the other way 'round - whatever, they should be equal, tho we don't check that.)


                }   // requirements


			//  Objective function of each agent is itself.
			//
			objfnjobset.initfn[iENAgent] = iENAgent;
		 
			// Also set up the 'agent' array. ???
			agent[iENAgent] = new Agent();
			agent[iENAgent].currenthostid = iENNode;
			agent[iENAgent].testhostid = iENNode;			// 15may03

			// 1.1 - set the agent's current host as the one it wants to stick to, hammingwise.
			//
			agent[iENAgent].hamminghostid = iENNode;
 
            } // agents
        } // nodes

    // Add the last node, the router.
    //
    iENNode++;
	thisENNode = new Node("Router", Node.TYPE_ROUTER, 1, 1, 100);
    nodeinfo[iENNode] = thisENNode;

	if (this.isDebugEnabled())
    	logDebug("Router - EN node #" + iENNode);

    this.numnode = iENNode;
    this.numfunction = iENAgent;
    this.numlink = iLink;

	objfnjobset.numobjfn = iENAgent;

    // Adjust cpurate for each node, as stated above. 
    // Also some misc. other things :)
    //
//    logDebug("Adjusted CPU rates:");
    for (int n=1; n<iENNode; n++) {     // don't do the last node, the router
        for (int f=1; f<=numfunction; f++) {
            xnfunction[f].cpurate[n] = xnfunction[f].cpurate[n] / cpuAttributeForNode[n];
//            logDebug("  node " + n + ": " + xnfunction[f].cpurate[n]);
            }
        }

	if (this.isDebugEnabled())
		logDebug("");

    // Agents don't run on routers.
    //
    for (int f=1; f<=iENAgent; f++) {
        xnfunction[f].felig[iENNode]   = 0; // can't run on the router
        xnfunction[f].cpurate[iENNode] = 0; // CPU usage on router; if elig=0, not used - MB sez set = 0
        xnfunction[f].memory[iENNode]  = 0; // ditto
        }

// 30apr03 - TraceFunctionForm uses zeroth node - for annealing sol'n?
	nodeinfo[0] = new Node();

/*

//14MAY03 ADJUST ELIG?
	 for (int ifn=1; ifn<=numfunction; ifn++) {
		int j = 0;
		for (int inod=1; inod<=numnode; inod++) {
		   if (xnfunction[ifn].felig[inod] == 1) {
			  j++;
			  xnfunction[ifn].nodelist[j] = inod;
	 		} // End If
	 	} // Next inod

		// 'we store the number of elements in the eligibility list xnfunction(ifn).nodelist(*) in the
		// 'array element xnfunction(ifn).nodelist(0)
		xnfunction[ifn].nodelist[0] = j;

		// just for display/debug
		if (j < 1) {
			logWarn("loadDataFromSociety: function " + ifn + " has no eligible nodes!!!");
		}
		else {
			agent[ifn].testhostid = xnfunction[ifn].nodelist[1]; 	// cranfill ???
	        if (this.isDebugEnabled())
				logDebug("loadDataFromSociety: function " + ifn + " eligible on node " + agent[ifn].testhostid);
		}

	 } // Next ifn
   */


/*
//	14may03 debug
    if (this.isDebugEnabled())
		logDebug("****** SystemDesign: objfnjobset.numobjfn=" + objfnjobset.numobjfn);
	 for (int ifnjob=1; ifnjob<=objfnjobset.numobjfn; ifnjob++) {  
	 	        
	   int ifnx = objfnjobset.initfn[ifnjob];
	   if (this.isDebugEnabled())
		   logDebug("****** SystemDesign: objfnjobset.initfn[ifnjob]=" + ifnx);

	   if (ifnx == 8) {
		int inod = agent[ifnx].testhostid; 
		   if (xnfunction[ifnx].felig[inod] == 0) {
			   logWarn("****** SystemDesign: Invalid eligibility!!! ifnx=" + ifnx + "; inod=" + inod);
		   }
		}
	 }
*/

	if (this.isInfoEnabled())
		logInfo("loadDataFromSociety: created EN data OK");

    return true;
	} // loadDataFromSociety


/**
	checkProject - was checkProjectmenu_Click from systemdesign.frm. <P>

	Sanity-check of the data structures. Return true iff all good.
	Will output error messages for all errors it finds, not just the first one.

	Ported by hand.

	@return true iff we think data is OK.
	@version 30april03.

**/
public boolean 
checkProject() {

	boolean result = true;

	// If no data's been loaded, this is the first data item that causes the $#!+ to hit the fan.
	//
	if (agent[1] == null) {
		if (this.isErrorEnabled())
			logError("checkProject: No agent data loaded?!");	
		return false;
		}


	// 'check that all links connected to lan nodes have lan node as target (node2)
	//
	for (int ilink=1; ilink<=numlink; ilink++) {
		if (linkinfo[ilink].type == Link.LINK_TYPE_LAN) { // 'lan link
			int node1 = linkinfo[ilink].node1;
			int node2 = linkinfo[ilink].node2;
			if (nodeinfo[node1].type == Node.TYPE_LAN) {
				if (this.isErrorEnabled()) {
					logError("Data error: Bad LAN definition:");
					logError("  Source node of link is a LAN node");
					logError("  Link name = " + linkinfo[ilink].name);
					logError("  Source node = " + node1);
					logError("  Target node = " + node2);
					logError("  LAN links require the source node be a server or router");
					}
				result = false;
				}

			if (nodeinfo[node2].type != Node.TYPE_LAN) {
				if (this.isErrorEnabled()) {
					logError("Data error: Bad LAN definition:");
					logError("  Target node of link is not a LAN Node");
					logError("  Link name = " + linkinfo[ilink].name);
					logError("  Source node = " + node1);
					logError("  Target node = " + node2);
					logError("  LAN links require the target node to be a LAN node");
					}
				result = false;
				}
			}
		}

	//	   'check hosts
	//	   'mtu > 0
	//	   'turn off entropy before shipping
	//	   'numnode > numnodemax
	//	   'numlink > numlinkmax
	//
	for (int inod=1; inod<=numnode; inod++) {
		if (nodeinfo[inod].type == Node.TYPE_SERVER) {

			if (nodeinfo[inod].cpubackground > 100) {
				if (this.isErrorEnabled()) {
					logError("Data error: Node Number = " + inod);
					logError("  Node name = " + nodeinfo[inod].name);
					logError("  Node has CPU background load > 100%");
					logError("  (value is " + nodeinfo[inod].cpubackground + ")");
					}
				result = false;
				}
		
			if (nodeinfo[inod].cpucount == 0) {
				if (this.isErrorEnabled()) {
					logError("Data error: node number = " + inod);
					logError("  Node name = " + nodeinfo[inod].name);
					logError("  Node has a zero CPU count");
					}
				result = false;
				}
		
			if (nodeinfo[inod].cpucountmax == 0) {
				if (this.isErrorEnabled()) {
					logError("Data error: node number = " + inod);
					logError("  Node name = " + nodeinfo[inod].name);
					logError("  Node has a zero maximum CPU count");
					}
				result = false;
				}
		
			if ((nodeinfo[inod].cpuutilmax != 0) && (nodeinfo[inod].cpuutilmax > 100)) {

				if (this.isErrorEnabled()) {
					logError("Data error: node number = " + inod);
					logError("  Node name = " + nodeinfo[inod].name);
					logError("  Nodes are required to have a maximum utilization > 0 and < 100 percent");
					logError("  (value is " + nodeinfo[inod].cpuutilmax + ")");
					}
				result = false;
				}
		
			if (nodeinfo[inod].memory <= 0) {
				if (this.isErrorEnabled()) {
					logError("Data error: node number = " + inod);
					logError("  Node name = " + nodeinfo[inod].name);
					logError("  Host memory must be a positive number");
					logError("  (value is " + nodeinfo[inod].memory + ")");
					}
				result = false;
				}
		
			if (nodeinfo[inod].memorymax <= 0) {
				if (this.isErrorEnabled()) {
					logError("Data error: node number = " + inod);
					logError("  Node name = " + nodeinfo[inod].name);
					logError("  Host maximum memory must be a positive number");
					logError("  (value is " + nodeinfo[inod].memorymax + ")");
					}
				result = false;
				}
		
			if (nodeinfo[inod].pfail < 0 || nodeinfo[inod].pfail > 1) {
				if (this.isErrorEnabled()) {
					logError("Data error: node number = " + inod);
					logError("  Node name = " + nodeinfo[inod].name);
					logError("  Node probability of failure should be between 0 and 1");
					logError("  (value is " + nodeinfo[inod].pfail + ")");
					}
				result = false;
				}
			}
		} // inod


	for (int ilink=1; ilink<=numlink; ilink++) {
	
		if (linkinfo[ilink].background < 0) {
			if (this.isErrorEnabled()) {
				logError("Data error: link number = " + ilink);
				logError("  Link name = " + linkinfo[ilink].name);
				logError("  Link has background load less than zero");
				logError("  (value is " + linkinfo[ilink].background + ")");
				}
			result = false;
			}
	
		if (linkinfo[ilink].background > 100) {
			if (this.isErrorEnabled()) {
				logError("Data error: link number = " + ilink);
				logError("  Link name = " + linkinfo[ilink].name);
				logError("  Link has background load greater than 100%");
				logError("  (value is " + linkinfo[ilink].background + ")");
				}
			result = false;
			}
	
		if (linkinfo[ilink].bandwidth <= 0) {
			if (this.isErrorEnabled()) {
				logError("Data error: link number = " + ilink);
				logError("  Link name = " + linkinfo[ilink].name);
				logError("  Link has bandwidth less than or equal to zero");
				logError("  (value is " + linkinfo[ilink].bandwidth + ")");
				}
			result = false;
			}
	
		if (linkinfo[ilink].latency < 0 
			|| (linkinfo[ilink].latency == 0 && linkinfo[ilink].type != Link.LINK_TYPE_CONT)) { // '03/03/03
			if (this.isErrorEnabled()) {
				logError("Data error: link number = " + ilink);
				logError("  Link name = " + linkinfo[ilink].name);
				logError("  Link latency must be positive");
				logError("  (value is " + linkinfo[ilink].latency + ")");
				}
			result = false;
			}
	
		if (linkinfo[ilink].mtu <= 0) {
			logError("Data error: link number = " + ilink);
			logError("  Link name = " + linkinfo[ilink].name);
			logError("  Link has MTU less than or equal to zero");
			logError("  (value is " + linkinfo[ilink].mtu + ")");
			result = false;
			}
	
		if (linkinfo[ilink].node1 <= 0 || linkinfo[ilink].node1 > numnode) {
			if (this.isErrorEnabled()) {
				logError("Data error: link number = " + ilink);
				logError("  Link name = " + linkinfo[ilink].name);
				logError("  Link is connected to a node number that is either negative, or greater than the number of nodes in the system");
				logError("  (value is " + linkinfo[ilink].node1 + ")");
				}
			result = false;
			}
	
		if (linkinfo[ilink].node2 <= 0 || linkinfo[ilink].node2 > numnode) {
			if (this.isErrorEnabled()) {
				logError("Data error: link number = " + ilink);
				logError("  Link name = " + linkinfo[ilink].name);
				logError("  Link is connected to a node number that is either negative, or greater than the number of nodes in the system");
				logError("  (value is " + linkinfo[ilink].node2 + ")");
				}
			result = false;
			}
	
		if (linkinfo[ilink].pfail < 0 || linkinfo[ilink].pfail > 1) {
			if (this.isErrorEnabled()) {
				logError("Data error: link number = " + ilink);
				logError("  Link name = " + linkinfo[ilink].name);
				logError("  Link probability of failure should be between 0 and 1");
				logError("  (value is " + linkinfo[ilink].pfail + ")");
				}
			result = false;
			}

		} // ilink


	if (darparun) {
		for (int ifn=1; ifn<=numfunction; ifn++) {
			if (xnfunction[ifn].objfn) {

				for (int inod=1; inod<=numnode; inod++) {
					if (xnfunction[ifn].felig[inod] == 1) {

						if (xnfunction[ifn].cpurate[inod] <= 0) {
							if (this.isErrorEnabled()) {
								logError("Data error: function number = " + ifn);
								logError("  Function name = " + xnfunction[ifn].name);
								logError("  Node number = " + inod);
								logError("  Node name = " + nodeinfo[inod].name);
								logError("  Function CPU burn rate must be greater than zero");
								}
							result = false;
							}

//// not sure what the hell this is trying to do - aug'03 - is this right???
////
//						if (xnfunction[ifn].cpurate[inod] > nodeinfo[inod].cpucount) {
//							if (this.isErrorEnabled()) {
//								logError("Data error: function number = " + ifn);
//								logError("  Function name = " + xnfunction[ifn].name);
//								logError("  Node number = " + inod);
//								logError("  Node name = " + nodeinfo[inod].name);
//								logError("  Function CPU burn rate must be less than the number of CPU processors");
//								}
//							result = false;
//							}

						if (xnfunction[ifn].memory[inod] <= 0) {
							if (this.isErrorEnabled()) {
								logError("Data error: function number = " + ifn);
								logError("  Function name = " + xnfunction[ifn].name);
								logError("  Node number = " + inod);
								logError("  Node name = " + nodeinfo[inod].name);
								logError("  Function memory requirement must be positive");
								}
							result = false;
							}
						}
					}

				for (int ifn2=1; ifn2<=numfunction; ifn2++) {
					if (xnfunction[ifn].fcall[ifn2].sendmsgrate < 0) {
						if (this.isErrorEnabled()) {
							logError("Data error: calling function number = " + ifn);
							logError("  Calling function name = " + xnfunction[ifn].name);
							logError("  Called function Number = " + ifn2);
							logError("  Called function name = " + xnfunction[ifn2].name);
							logError("  Function messaging rate from calling function to called function must be non-negative");
							}
						result = false;
						}

					if (xnfunction[ifn].fcall[ifn2].sendmsgrate > 0) {
						if ( !xnfunction[ifn2].objfn) {
							if (this.isErrorEnabled()) {
								logError("Data error: calling function number = " + ifn);
								logError("  Calling function name = " + xnfunction[ifn].name);
								logError("  Called function number = " + ifn2);
								logError("  Called function name = " + xnfunction[ifn2].name);
								logError("  Called function is being called with a positive messaging rate, but is not defined as an active function");
								}
							result = false;
							}
						}
					} // for ifn2
				}
			} // if objfn
		} // if darparun

	if (minultralognodes <= 0) {
		if (this.isErrorEnabled()) {
			logError("Data error:");
			logError("  Minimum number of hosts in Ultralog run must be greater than zero");
			}
		result = false;
		}

	if (minultralognodes > numnode) {
		if (this.isErrorEnabled()) {
			logError("Data error:");
			logError("  Minimum number of hosts in Ultralog run must be less or equal to the total number of hosts defined in the system");
			}
		result = false;
		}

	if (this.isInfoEnabled())
		logInfo("checkProject: Check complete; OK = " + result);

	return result;
	} // checkProject



/**
	From getnodefixedhardwarecost in systemdesignx.bas.
**/
public float
getnodefixedhardwarecost(int inod){

   return 	nodeinfo[inod].costbase + 
			nodeinfo[inod].cpucount * nodeinfo[inod].costpercpuproc + 
			nodeinfo[inod].diskcount * nodeinfo[inod].costperdisk + 
			nodeinfo[inod].memory * nodeinfo[inod].costpermbmemory;
}


/**
	from getnodefixedsoftwarecost in systemdesignx.bas
**/
public float
getnodefixedsoftwarecost(int inod) {

   float cost = 0f;
   for (int ifnx=1; ifnx<=requiredfunctions[0]; ifnx++) {
      int ifn = requiredfunctions[ifnx];
      if (nodeinfo[inod].fcalltot[ifn] > 0) {
         cost += xnfunction[ifn].costpernode[inod];
      }
   }
   
   return cost;
}


/**
	From getnodemonthlyhardwarecost in systemdesignx.bas.
**/
public float
getnodemonthlyhardwarecost(int inod){
	return 0.0f;
}


/**
	From getnodemonthlysoftwarecost in systemdesignx.bas.
**/
public float
getnodemonthlysoftwarecost(int inod){
	return 0.0f;
}


/**
	Was queuemgr in systemdesignx.bas.
**/
public void
queueMgr(int[] qarray, int qdim, int iqfirst, int iqlast, int iqdata, int iqmode) {

   //'iqmode=0 for read, iqmode=1 for write
   // 'initially ifirst = iqlast for empty queue. iqlast is the insertion point for the NEXT data item
  
	if (iqmode == 1) {
		if (iqlast + 1 == iqfirst) {
			logWarn("SystemDesign.queueMgr: Dimension of queue has been exceeded! (1)");
			return;
		}
		qarray[iqlast] = iqdata;
		if (iqlast == qdim) {
			iqlast = 1;
		} 
		else {
			iqlast++;
		}
		return;
	}

	if (iqmode == 0) {
		if (iqlast == iqfirst) {
			logWarn("SystemDesign.queueMgr: Dimension of queue has been exceeded! (2)");
			return;
		}
		iqdata = qarray[iqfirst];
		if (iqfirst == qdim) {
			iqfirst = 1;
		}
		else {
			iqfirst++;
		}
		return;
	}

} // queueMgr




/**
 * Clean up after something dies.
 */
public boolean
stopThreads() {

    if (annealForm_ != null) {
        annealForm_.stopThreads();
    	}
    return true;    
	}


/**
 *  If no anneal time specified, use -1, 
 *  which is to say, use AnnealForm's default.
 */
public boolean
runConfig() {
	return runConfig(-1);
	}


/** 
	Solve the current data.
	@return false if something bad happens.
    
    If annealTime>0, set AnnealForm's annealing time.
**/
public boolean
runConfig(int annealTime) {

	boolean annotate = true; // put comments in data files?
	if (writeENDataFiles_) {

	    // Output the EN data files of the input data (pre-loadbalancing) for hand inspection/debugging.
	    //
	    // This is a warning because maybe we shouldn't write files in the UL environment?
	    //
	    logWarn("Dumping pre-balanced EN config files to 'society_before' (file write OK?)");
	    RnR.saveNodes("society_before", 	this, annotate);
	    RnR.saveFunctions("society_before", this, annotate);
	    RnR.saveLinks("society_before", 	this, annotate);
		}

	// Minimal sanity check.
	//
	if (checkProject() == false) {
		return false;
		}


	// Here's something needs to be verified...
	//
	if (solveWithSurvivability_ && pfailuresurvivemax==0) {
		
		if (this.isErrorEnabled()) {
			logError("solveWithSurvivability_==t but pfailuresurvivemax==0!");
			logError(" - bad initialization!");
			}
		return false;
		}
	
	
	warnMsgPath_ = 0;

    // We hang onto the AnnealForm in case something blows,
    // we can shut down its threads.
    //
    annealForm_ = new AnnealForm(this);


	// Set some behavior-affecting globals.
	// (checked by AnnealForm, at least)
	//
	annealForm_.randomAgentSeed = true;
	activetracefunction = 1;				// this came from the GUI.
	iteranel = true;
    if (annealTime > 0) {
        annealForm_.setAnnealingTime(annealTime);
    	}

	if (this.isInfoEnabled()) {
		logInfo("");
		logInfo("runConfig:      AnnealForm.setAnnealingTime = " + annealTime);
		logInfo("runConfig:       AnnealForm.randomAgentSeed = " + annealForm_.randomAgentSeed);
		logInfo("runConfig: SystemDesign.activetracefunction = " + activetracefunction);
		logInfo("runConfig:            SystemDesign.iteranel = " + iteranel);
		}

/*
 	// also being not Cougaar-friendly:

	logInfo("  **** Config before annealing run:");
	annealForm.writeAgentState(new OutputStreamWriter(System.out), 
				xnfunction, numfunction,
				linkinfo, numlink,
				nodeinfo, numnode,
				agent, objfnjobset,
				true);
	logInfo("  **** End config before");
*/

	long startMillis = System.currentTimeMillis();
	Date start = new Date();

// rec 1.1 ???
   annealForm_.initialLaydown();

	Date stop = new Date();
	long delta = System.currentTimeMillis() - startMillis;

	if (this.isInfoEnabled()) {
	    logInfo("runConfig: started: " + start);
	    logInfo("runConfig:   ended: " + stop);
	    logInfo("runConfig:      for " + delta + " milliseconds");
		}
	
	if (warnMsgPath_ > 0) {
		if (this.isWarnEnabled())
			logWarn("There were " + warnMsgPath_ + " 'no message path' warnings.");		
		}

	if (state.status == true) { // success/failure?
		if (this.isInfoEnabled())
        	logInfo("runConfig: state.status==true: solution found! <--=");
    	}
    else {
		if (this.isWarnEnabled()) {
	        logWarn("runConfig:*****************************************");
	        logWarn("runConfig: state.status==false: no solution found? ");
	        logWarn("runConfig:*****************************************"); 
			}      
    	}

	// The state.status check doesn't seem to be an accurate guage of success. 
	// This checking for annealhostid==0 any better?
    // Also (optionally) display the new laydown.
    //
    boolean goodAnneal = true;
	if (this.isDebugEnabled())
	    logDebug("runConfig: Agent->Host annealing assignments (annealed/original)");
	int countMoved = 0;
    for (int i=1; i<=numfunction; i++) {
		logDebug("  agent " + i + ", '" + xnfunction[i].name + "' -> Host " + agent[i].annealhostid + "/" + agent[i].hamminghostid);
        if (agent[i].annealhostid != agent[i].hamminghostid) {
			countMoved++;
			}
		if (agent[i].annealhostid == 0) {
            goodAnneal = false;
	        }
	    }

	// print hamming dist
	if (this.isDebugEnabled())
		logDebug("runConfig: Moved " + countMoved + " of " + numfunction + " agents!");
	
	if (!goodAnneal) {
        logWarn("No annealing solution found!");
        logWarn("The input data may be unsolvable!!");
        return false;   
    	}

	if (this.isInfoEnabled())
		logInfo("Final system energy: " + statelist[0].e);


// I'd like to check CSVisualizer.isWiIthinLimits(), but we don't have a CougaaSociety here...
// do it in the plugin?
//	CSVisualizer csv = new CSVisualizer();

    // Just FYI, we track iteration count.
    //
    lastAnnealIterCount_ = annealForm_.getIterationCount();



// debug, readin' and writin' files....

    if (writeAnnealResultToConsole_) {
            
		if (this.isDebugEnabled()) {

	        logDebug("  **** Config after annealing run:");

	        // not Cougaar-friendly....
        	annealForm_.writeAgentState(new OutputStreamWriter(System.out), 
                        xnfunction, numfunction,
        				linkinfo, numlink,
        				nodeinfo, numnode,
        				agent, objfnjobset,
        				true);

			logDebug("  **** End config after");
			}
	    }

    if (writeAnnealStateToFiles_) {
    	try {
            String filename = "agents.state" + System.currentTimeMillis();
    		annealForm_.writeAgentState(new FileWriter(filename), 
    					xnfunction, numfunction,
    					linkinfo, numlink,
    					nodeinfo, numnode,
    					agent, objfnjobset,
    					true);
    	}
    	catch (Exception e) {
    		SystemDesign.logError("Exception:\n", e);
    	}
    }

	// Write EN-style datafiles of output soc
	if (writeENDataFiles_) {
	    logWarn("writing EN config files to 'society_after.*'");
	    annotate = true; // put comments in data files?
	    RnR.saveNodes("society_after", 		this, annotate);
	    RnR.saveFunctions("society_after", 	this, annotate);
	    RnR.saveLinks("society_after", 		this, annotate);
		}

/*
	19Sept04 - for test folks, log a bunch of stuff.
	As specified in "en2.20-testplan-091504.doc":

	Objective Function Components (LOG FILE):
		hamming (0-NumAgents) = Hamming
		loadbal = Load Balance
		pfailuresurvive (0-NumAgents) = Agent Risk
		1 - psuccess (0-1) = State Failure Rate
		remotetraffic = Remote Traffic

	Objective Function Sample Ranges (LOG FILE):
		hammingsamplemax = Hamming Sample Maximum
		hammingsamplemin = Hamming Sample Minimum
		loadbalmaxfirstpass = Load Balancing Sample Maximum from First Pass of Two Pass Strategy
		loadbalminfirstpass = Load Balancing Sample Minimum from First Pass of Two Pass Strategy
		loadbalsamplemax = Load Balancing Sample Maximum
		loadbalsamplemin = Load Balancing Sample Minimum
		pfailuresamplemax = State Failure Sample Maximum
		pfailuresamplemin = State Failure Sample Minimum
		pfailuresurvivesamplemax = Agent Risk Sample Maximum
		pfailuresurvivesamplemin = Agent Risk Sample Minimum
		remotetrafficsamplemax = Remote Messaging Sample Maximum
		remotetrafficsamplemin = Remote Messaging Sample Minimum

*/
	if (this.isDebugEnabled()) {
	
        logDebug("######## EN Run Data ");
        logDebug("hamming: " + this.hamming);
        logDebug("loadbal: " + this.loadbal);
        logDebug("pfailuresurvive: " + this.pfailuresurvive);
        logDebug("psuccess: " + this.psuccess);
        logDebug("remotetraffic: " + this.remotetraffic);
        
        logDebug("hammingsamplemin: " + this.hammingsamplemin);
        logDebug("hammingsamplemax: " + this.hammingsamplemax);
        logDebug("loadbalminfirstpass: " + this.loadbalminfirstpass);
        logDebug("loadbalmaxfirstpass: " + this.loadbalmaxfirstpass);
        logDebug("loadbalsamplemin: " + this.loadbalsamplemin);
        logDebug("loadbalsamplemax: " + this.loadbalsamplemax);
        
        logDebug("pfailuresamplemin: " + this.pfailuresamplemin);
        logDebug("pfailuresamplemax: " + this.pfailuresamplemax);
        logDebug("pfailuresurvivesamplemin: " + this.pfailuresurvivesamplemin);
        logDebug("pfailuresurvivesamplemax: " + this.pfailuresurvivesamplemax);
        logDebug("remotetrafficsamplemin: " + this.remotetrafficsamplemin);
        logDebug("remotetrafficsamplemax: " + this.remotetrafficsamplemax);
 
        logDebug("iterations: " + this.annealForm_.iterations_);
        
        logDebug("######## End EN Run Data ");
		}

	return true;
	}


/**
 * VB can set the values of its call-by-reference args by side effect; Java can't.
 * These are public vars equivalent to the args of routeMsgTime that we can set
 * on return from routeMsgTime, and then the caller (TraceFunctionForm) can read.
 * 
 */
//never set: public float   routeMsgTime_msgsize;
//never set: public float   routeMsgTime_msgfreq;
//never set: public int     routeMsgTime_pathnode1;
//never set: public int     routeMsgTime_pathnode2;
public boolean routeMsgTime_msgsent;
public float   routeMsgTime_distx;
public float   routeMsgTime_distxtime;
// never set: public boolean routeMsgTime_specifiedpath;


/**
    Was "routemsgtimemenu" in "systemdesignx.bas"
    
    Quick solution to problematic 'specifiedpath' use of GoTo: since as a matter of fact,
    this method never gets called with specifiedpath=true, we'll not implement it, and just
    complain if we ever get called otherwise. :/

    @version 1.1 - aug'03 - fixed call-by-reference problems, removed unused GUI params
    @version 30april03

**/
public void 
routeMsgTime(
    float   msgsize,        // As Single, 
    float   msgfreq,        // As Single, 
    int     pathnode1,      // As Integer, 
    int     pathnode2,      // As Integer, 
 
// removed GUI-oriented params
// unused    boolean drawmsgpath,    // As Integer, // rec - declared as int, but used as boolean; changed to boolean
// unused    long    drawmsgcolor,   // As Long,
// unused    int     drawmsgwidth,   // As Integer,
// unused    int     drawmsgstyle,   // As Integer,
// unused    int     drawmsgtype,    // As Integer,

    boolean msgsent,        // As Integer, // rec - declared as int, but used as boolean; changed to boolean
// rec - never used!    int     msgtype,        // As Integer, 
    float   distx,          // As Single, 
    float   distxtime,      // As Single, 
    boolean specifiedpath) {    // As Integer // rec - declared as int, but used as boolean; changed to boolean


	if (specifiedpath) {
		logError("routeMsgTime called with specifiedpath=true - NOT IMPLEMENTED!");
		return;
		}

	int nodenear = 0, nodefar = 0, nodex, node1, node2;
	
	nodex = -1;
	
	
	// 'NOTE: For DARPA runs, the msgsize is the total message rate, in bytes/sec
	
	boolean msgforward = true; // = 1;
	
	boolean probcostrouting = false; // metrparmex.probcostroutingCheck.Value;

	if (pathnode1 == pathnode2) {
		msgsent = true;
		distx = 0;
		distxtime = 0;
		numpathnode = 2;
		path[1] = pathnode1;
		path[2] = pathnode2;

		//rec - set side-effect return values
		routeMsgTime_msgsent = msgsent;
		routeMsgTime_distx = distx;
		routeMsgTime_distxtime = distxtime;

		return;
		}
	
	// 'initialize dtime
	for (int inode = 1; inode <= numnode; inode++) {
		for (int jnode = 1; jnode <= numnode; jnode++) {
			dtime[inode][jnode] = MAGIC_9BALL;
			}
		}

	// 'fill dtime(inode,jnode) array
	for (int ilink = 1; ilink <= numlink; ilink++) {
		node1 = linkinfo[ilink].node1;
		node2 = linkinfo[ilink].node2;
	
	//      'backmsgsize = (linkinfo(ilink).background / 100#) * linkinfo(link).bandwidth
	//      'msgsizetot = backmsgsize + msgsize
	
		float dtnew = linkinfo[ilink].latency;
	
		//			if (1 == 2)  {
		//			  x = linkinfo[ilink].name;
		//			  }
	
	//	'normally a network uses path MTU discovery to find the minimum MTU
	//	'We will just assume Path MTU discovery is turned off, and packets
	//	'are fragmented at router
	//      
	//	'msgsize is in bytes
	
		float numpackets;
	
		float mtu = linkinfo[ilink].mtu;
		numpackets = (msgsize + tcpoverhead) /(mtu - ipoverhead);
	
		if (numpackets - RnR.Int(numpackets) > 0) {
			numpackets = RnR.Int(numpackets) + 1;
		} else {
			numpackets = RnR.Int(numpackets);
		}
	
		float overhead =
			tcpoverhead
				+ numpackets * (ipoverhead + linkinfo[ilink].protocoloverhead);
	
		if (linkinfo[ilink].bandwidth > 0) {
			
			float utiltot = 0;
			float servicetime = 0;
			if (!darparun) {
	
	//			'update for background 3/29/02
	//			'bug: using servicetime here with the background correction is double counting!!!!!!!!
	//			'servicetime = 8 * (msgsize + overhead) / (linkinfo(ilink).bandwidth * (1# - linkinfo(ilink).background / 100#))
				servicetime = 8 *(msgsize + overhead) /linkinfo[ilink].bandwidth;
	
	//			'find the projected utilization of this link, needed for queuing
	//			'msgfreq is the number of times per second the message of size
	//			'msgsize is sent down this link
				float utilmsg = 8 * msgsize * msgfreq / linkinfo[ilink].bandwidth;
				utiltot = utilmsg + linkinfo[ilink].background / 100;
	
	//          'now add contributions from previously scheduled function calls:
				utiltot += 8 * linkinfo[ilink].traffic / linkinfo[ilink].bandwidth;
			} else {
				float utilmsg = 8 * msgsize * msgfreq / linkinfo[ilink].bandwidth;
				utiltot = utilmsg + linkinfo[ilink].background / 100;
	//            'now add contributions from previously scheduled function calls:
				utiltot += 8 * linkinfo[ilink].traffic / linkinfo[ilink].bandwidth;
			}
	
			if (utiltot >= 1) {
				dtnew = MAGIC_9BALL;
			} 
			else {
				if (!darparun) {
					if (linkinfo[ilink].type != Link.LINK_TYPE_LAN) {
	
	//					'dtnew = dtnew + msgsizetot / linkinfo(ilink).bandwidth 'old stuff before msgfreq
	
						float dtresponse = servicetime / (1 - utiltot);
						dtnew += dtresponse;
					} 
					else { //  'LAN link
						if (nodeinfo[node2].type == Node.TYPE_LAN) { //  'LAN shared hub node
	
	//                      'dtnew = dtnew + msgsizetot / linkinfo(ilink).bandwidth  'old stuff before msgfreq
							float dtresponse = servicetime / (1 - utiltot);
							dtnew += dtresponse;
							
	//						'LAN links are bidirectional. The distance from an external
	//						'node to a LAN node (type 3) is the normal dtnew. The distance
	//						'from a LAN node to an external node is always defined as zero.
							dtime[node2][node1] = 0;
						} 
						else {
							dtnew = 0;
						}
					}
				} 
				else {
	
	//				'there are no Lan links in darpa (currently). Everything is modeled as switched ethernet.
	//				'noop. leave dtnew as pure latency for darpa.
					if (nodeinfo[node2].type == Node.TYPE_LAN) { // 'LAN shared hub node
						dtime[node2][node1] = 0;
					} 
					else {
						dtnew = 0;
					}
				}
			}
		} 
		else {
			dtnew = MAGIC_9BALL;
		}
		dtime[node1][node2] = dtnew;
	} //  'fill dtime(inode,jnode) array

	int edgenode1=0, edgenode2=0;


//	rec - NOTE - NOT IMPLEMENTED 
// (we check for specifiedpath==true at start of method, complain and exit if so)
//
//	// 'check for specifiedpath -
//	//
//	if (specifiedpath) {
//		distx = dtime[pathnode1][pathnode2];
//		distxtime = distx; // '?????????????????????????????????????
//		if (distx >= MAGIC_9BALL) {
//	
//			// 'MsgBox "No Message Path from Source to Destination !!"
//			if (warnMsgPath_ == 0) {
//				logWarn("routeMsgTime: No message path from source to destination (1)! node " + pathnode1 + "->" + pathnode2 );
//			}
//			warnMsgPath_++;
//
//			msgsent = false;		
//			
//			// rec - set side-effect return values
//			routeMsgTime_msgsize  = msgsize;
//			routeMsgTime_msgfreq = msgfreq;
//			routeMsgTime_pathnode1 = pathnode1;
//			routeMsgTime_pathnode2 = pathnode2;
//			routeMsgTime_msgsent = msgsent;
//			routeMsgTime_msgtype = msgtype; 
//			routeMsgTime_distx = distx;
//			routeMsgTime_distxtime = distxtime;
//
//			return;		// GoTo exitpart
//		}
//		edgenode1 = pathnode1;
//		edgenode2 = pathnode2;
//		
//		// this is the GoTo that's problematic (altho there may be a simple solution)
//		// VBJ2_XLATE_FAILURE	GoTo specifiedpathexit
//	}


	// '*****************************************************
	//
	// 'Dikstra:
	// 'Define a set of near nodes and a set of far nodes
	// 'initially all nodes are in the far list
	//
	
	tailvfar = numnode;
	for (int inode = 1; inode <= numnode - 1; inode++) {
		nextvfar[inode] = inode + 1;
		prevvfar[inode] = inode - 1;
	}
	nextvfar[numnode] = 0;
	prevvfar[numnode] = numnode - 1;
	
	// 'headvnear = 0
	headvfar = 1;
	
	// 'initialize by putting the starting node, pathnode1, in the near list
	headvnear = pathnode1;
	nextvnear[pathnode1] = 0;
	int tailvnear = pathnode1;
	
	//'the real distance should include fixed cost, monthly cost, failure, and time contributions
	//'for initial node, this doesn't matter, since ALL paths include both the initial node pathnode1
	//'and the target node pathnode2:
	dist[1] = 0;
	disttime[1] = 0;
	
	if (probcostrouting) {
		distfail[1] = nodeinfo[pathnode1].pfail;
	}
	
	//    'remove pathnode1 from far list:
	if (pathnode1 == headvfar) {
	//       'pathnode1 is head of far list, so find a new head of far list
		headvfar = nextvfar[pathnode1];
	} 
	else {
		
	//	'pathnode1 is in middle or end of far list, so remove it, and splice the list
	//	'so node before pathnode1 has next node in list pointing to node after pathnode1
	//	'also works for pathnode1=tailvfar which sets following:
	//	'nextvfar(prevvfar(pathnode1) = nextvfar(pathnode1) = nextvfar(tailvfar) = nextvfar(numnode) = 0
	//	'prevvfar(nextvfar(pathnode1) = prevvfar(pathnode1) = prevvfar(tailvfar) = prevvfar(numnode) = numnode-1
		nextvfar[prevvfar[pathnode1]] = nextvfar[pathnode1];
		prevvfar[nextvfar[pathnode1]] = prevvfar[pathnode1];
	}
	
	float distxfail = 0, distxfailnew = 0;
	
	int k = 1;
	//'loop over all vertices in near set, and for each near
	//'vertex determine total distance from pathnode1 to
	//'to all nodes in far set, distx:

// 1.1: changed "iterk" label to "iterkname"
//
iterkname: while(true) {	// rec - replaced GoTo
		
		for (int inear=1; inear<=k; inear++) { // 'loop over near set, containing k nodes
		
			if (inear == 1) {
				nodenear = headvnear;
			}
			else {
				nodenear = nextvnear[nodenear];
			}
		
			for (int ifar=1; ifar<=numnode-k; ifar++) { // 'loop over all nodes in far set, containing numnode-k nodes
				if (ifar == 1) {
					nodefar = headvfar;
				} 
				else {
					nodefar = nextvfar[nodefar];
				}
		
				if (inear == 1 && ifar == 1) {
		
		//			'distx is distance from source to current near node plus distance from near to far node
		//			'for inear=1, then source node is near node, so dist(inear) = dist(1) = 0
		
					distx = dist[inear] + dtime[nodenear][nodefar];
					distxtime = disttime[inear] + dtime[nodenear][nodefar];
		
					if (probcostrouting) {
		
		//				'PROBLEMS: if the cost function doesn't scale linearly, like when there is
		//				'a maximum cost or response time limit for the WHOLE SYSTEM, then the routing
		//				'contributions below are not correct:
		//               
		//				'PROBLEMS: Need to find the link ilink corresponding to the nodenear,nodefar node set
		//				'We assume that for any two nodes inod, jnod, there is ONLY ONE LINK
		//				'In the real world, there may be multiple physical paths between two nodes.
		
						int ilinkx = linkid[nodenear][nodefar];
						
						if (ilinkx == 0) {
							distx = MAGIC_9BALL;
							distxfailnew = 0;
						} else {
							
		//                   'fixed cost stuff
							float fixedcostx =
								getnodefixedhardwarecost(nodefar)
									+ getnodefixedsoftwarecost(nodefar)
									+ linkinfo[ilinkx].costbase;
							if (fixedcostx < MAGIC_8BALL) {
								fixedcostx = fixedcostscale * fixedcostx;
						// rec - unused?		fixedcostok = true;
							} 
							// 18jun03 - unused: else {
						// rec - unused?		fixedcostok = false;
							// 18jun03 - unused: }
		
		//                  'monthly cost stuff:
							float monthlycostx =
								getnodemonthlyhardwarecost(nodefar)
									+ getnodemonthlysoftwarecost(nodefar)
									+ linkinfo[ilinkx].costpermonth;
							if (monthlycostx < MAGIC_8BALL) {
								monthlycostx = monthlycostscale * monthlycostx;
						// rec - unused?		monthlycostok = true;
							}// 18jun03 - unused: else {
						// rec - unused?		monthlycostok = false;
							// 18jun03 - unused: }
		
		//                  'failure stuff
							float psuccessx = (1 - distfail[inear]) 
								      * (1 - nodeinfo[nodenear].pfail) 
								      * (1 - linkinfo[ilinkx].pfail);
		
							float pfailurex = pfailurescale * (1 - psuccessx);
		
		//                  'probability is a problem, since it is multiplicative, not additive
		
							float dtimex = dtime[nodenear][nodefar];
							if (dtimex< MAGIC_8BALL && fixedcostx < MAGIC_8BALL 
							 && monthlycostx < MAGIC_8BALL && pfailurex < MAGIC_8BALL) {
		
								distxtime = disttime[inear] + dtimex;
								float dtx = dtimex * responsetimescale;
								dtx += fixedcostx + monthlycostx + pfailurex;
								distx = dist[inear] + dtx;
								distxfailnew = (1 - psuccessx);
						// rec - unused?		pfailureok = true;
							} 
							else {
								distx = MAGIC_9BALL;
						// rec - unused?		pfailureok = false;
							}
						}
					}
		
					edgenode1 = nodenear;
					edgenode2 = nodefar;
		
		//             ''' NEW message forwarding node stuff
		
					if (msgforward) {
						if (nodeinfo[nodefar].msgforward != 1 && nodefar != pathnode2) {
							distx = MAGIC_9BALL;
						}
					}
		
				} 
				else {
					float distxnew = dist[inear] + dtime[nodenear][nodefar];
					float distxtnew = disttime[inear] + dtime[nodenear][nodefar];
		
					if (probcostrouting) {
		
		//				'PROBLEMS: if the cost function doesn't scale linearly, like when there is
		//				'a maximum cost or response time limit for the WHOLE SYSTEM, then the routing
		//				'contributions below are not correct:
		//               
		//				'PROBLEMS: Need to find the link ilink corresponding to the nodenear,nodefar node set
		//				'We assume that for any two nodes inod, jnod, there is ONLY ONE LINK
		//				'In the real world, there may be multiple physical paths between two nodes.
		
						int ilinkx = linkid[nodenear][nodefar];
		
						if (ilinkx == 0) {
							distxnew = MAGIC_9BALL;
							distxfailnew = 0;
						} else {
							float fixedcostx =
								getnodefixedhardwarecost(nodefar)
									+ getnodefixedsoftwarecost(nodefar)
									+ linkinfo[ilinkx].costbase;
							if (fixedcostx < MAGIC_8BALL) {
								fixedcostx = fixedcostscale * fixedcostx;
							}
		
							float monthlycostx =
								getnodemonthlyhardwarecost(nodefar)
									+ getnodemonthlysoftwarecost(nodefar)
									+ linkinfo[ilinkx].costpermonth;
							if (monthlycostx < MAGIC_8BALL) {
								monthlycostx = monthlycostscale * monthlycostx;
							}
		
		//                  'failure stuff
							float psuccessx = (1 - distfail[inear])
									  * (1 - nodeinfo[nodenear].pfail)
									  * (1 - linkinfo[ilinkx].pfail);
							float pfailurex = pfailurescale * (1 - psuccessx);
		
		//                   'probability is a problem, since it is multiplicative, not additive
		
							float dtimex = dtime[nodenear][nodefar];
		
							if (dtimex < MAGIC_8BALL && fixedcostx < MAGIC_8BALL 
								&& monthlycostx < MAGIC_8BALL && pfailurex < MAGIC_8BALL) {
									
								distxtnew = disttime[inear] + dtimex;
								float dtx = dtimex * responsetimescale;
								dtx += fixedcostx + monthlycostx + pfailurex;
								distxnew = dist[inear] + dtx;
								distxfailnew = (1 - psuccessx);
							} else {
								distxnew = MAGIC_9BALL;
								// unused: boolean pfailureok = false;
							}
						}
					}
		
		//            ''' NEW message forwarding node stuff
		
					if (msgforward) {
						if (nodeinfo[nodefar].msgforward != 1 && nodefar != pathnode2) {
							distxnew = MAGIC_9BALL;
						}
					}
		
					if (distxnew < distx) {
						distx = distxnew;
						distxtime = distxtnew;
						distxfail = distxfailnew;
						// 'distfailx = distfailxnew ???
						edgenode1 = nodenear;
						edgenode2 = nodefar;
					}
				}
			}
		}
		
		if (distx >= MAGIC_8BALL) {
		
			if (warnMsgPath_ == 0) {
				if (isErrorEnabled())
					logError("routeMsgTime: No message path from source to destination (2)! node " + pathnode1 + "->" + pathnode2 );
			}
			warnMsgPath_++;

			msgsent = false;
			
			// rec - set side-effect return values
			routeMsgTime_msgsent = msgsent;
			routeMsgTime_distx = distx;
			routeMsgTime_distxtime = distxtime;

			return; // GoTo exitpart
			}
		
		dist[k+1] = distx;
		disttime[k+1] = distxtime;
		distfail[k+1] = distxfail;
		edge[k+1][1] = edgenode1;
		edge[k+1][2] = edgenode2;
		
		// 'add edgenode2 to near list
		nextvnear[tailvnear] = edgenode2;
		nextvnear[edgenode2] = 0;
		tailvnear = edgenode2;
		
		// 'remove edgenode2 from far list
		if (edgenode2 == headvfar) {
			headvfar = nextvfar[edgenode2];
			prevvfar[headvfar] = 0;
		} else {
			nextvfar[prevvfar[edgenode2]] = nextvfar[edgenode2];
			if (edgenode2 != tailvfar) {
				prevvfar[nextvfar[edgenode2]] = prevvfar[edgenode2];
			}
		}

		if (edgenode2 == pathnode2) {

		// ''done
			msgsent = true;
		
		// 'path(i) is a list of nodes in reverse order, from finish to start
			path[1] = pathnode2;
			path[2] = edgenode1;
			nodex = edgenode1;
			numpathnode = 2;
			if (nodex == pathnode1) {

				//  'This draw section is only used for drawing a specified path when there is only one link
				//	if (drawmsgpath) {
				// 	VBJ2_XLATE_FAILURE: drawmessagepath numpathnode, drawmsgcolor, drawmsgwidth, drawmsgstyle, drawmsgtype
				//	}

				// rec - set side-effect return values
				routeMsgTime_msgsent = msgsent;
				routeMsgTime_distx = distx;
				routeMsgTime_distxtime = distxtime;
	
				return; // GoTo exitpart
				}
		
		// rec - re-worked GoTo
			boolean keepLooping = true;
nextpathnode:
			while (keepLooping) {
				keepLooping = false;
				for (int i=2; i<=k; i++) {
					if (edge[i][2] == nodex) {
						nodex = edge[i][1];
						numpathnode++;
						path[numpathnode] = nodex;
						if (nodex == pathnode1) {

							//	if (drawmsgpath) {
							//		 VBJ2_XLATE_FAILURE: drawmessagepath numpathnode, drawmsgcolor, drawmsgwidth, drawmsgstyle, drawmsgtype
							//	}

							// rec - set side-effect return values
							routeMsgTime_msgsent = msgsent;
							routeMsgTime_distx = distx;
							routeMsgTime_distxtime = distxtime;

							return; // GoTo exitpart
						} else {
							// VBJ2_XLATE_FAILURE: GoTo nextpathnode
							keepLooping = true;
							continue nextpathnode;
						}
					}
				} // for i=2 to k
			} // rec: keepLooping

			break iterkname; // rec - iterk loop

		} // edgenode2 == pathnode2
		else {
			k++;
			// VBJ2_XLATE_FAILURE: GoTo iterk
		}

	} // while(true) - iterkname loop

	// rec: "exitpart" label did nothing but GUI cleanup, 
	// so we can replace any jumps to it with "return"s, above.
// exitpart:

} // routemsgtime


/**
 * For debugging, I need to compare two SystemDesign objects.
 * We'll either return 0 for equal, or -1, for not equal. Doesn't really mean this<that.
 * Also only compares pertinent fields, of course.
 */
public int
compareTo(SystemDesign that) {

//	if (this.isDebugEnabled())
//	    logDebug("compareTo: comparing sd1 (?) and sd2 (OK)");


    try {
 
        cfInts("numnode",     this.numnode,     that.numnode);
        cfInts("numlink",     this.numlink,     that.numlink);
        cfInts("numfunction", this.numfunction, that.numfunction);
        cfInts("numfgroup",   this.numfgroup,   that.numfgroup);
        cfInts("objfnjobset.numobjfn", this.objfnjobset.numobjfn, that.objfnjobset.numobjfn);

//		if (this.isDebugEnabled())
//			logDebug("compareTo: OK");

    	}
    catch (ValueMismatchError vme) {

//    	if (this.isDebugEnabled()) {
//			logDebug(vme.getMessage() + " <<<< MISMATCH! <<<<<<<<<<<<<<<<<<<<<");
//			logDebug("compareTo: COMPARISON FAILS!");
//    		}

        return (vme.firstIsLess_?-1:1);  // a<b, or t'other way 'round?
    	}

    return 0;
	} // compareData


private void
cfInts(String what, int a, int b)
throws ValueMismatchError {

    if (a != b) {

//		if (this.isDebugEnabled())
//	        logDebug("cfInts: " + what + " - " + a + " != " + b);

        throw new ValueMismatchError(what, (a<b));
    	}
	}


/**
 *  For the comparison, it's easier to throw an exception.
 */
private class
ValueMismatchError extends Error {
public boolean firstIsLess_;

    ValueMismatchError(String varName, boolean firstIsLess) {
        super("Value mismatch in " + varName);
        firstIsLess_ = firstIsLess;
    	}

	} // class ValueMismatchError



public long
getLastAnnealIterCount() {
	return lastAnnealIterCount_;
	}


/**
 *  Use the given  logging facility.
 */
public static void
setLogger(Logger logger) {
    logger_ = logger;
	}


// needs setters for these ???
private boolean isDebugEnabled_ = true;
private boolean isInfoEnabled_  = true;
private boolean isWarnEnabled_  = true;
private boolean isErrorEnabled_ = true;


/**
  Convenience method for non-Cougaar usage.
 */
public static void
logDebug(String msg) {

	if (logger_ == LoggingService.NULL) {
		System.out.println("EN4J:DEBUG: " + msg);
		}
	else {
		logger_.debug(msg);
		}
	}


public boolean
isDebugEnabled() {

	if (logger_ == LoggingService.NULL) {
		return isDebugEnabled_;
		}
	else  {
		return logger_.isDebugEnabled();
		}
	}


/**
 * Convenience method for non-Cougaar usage.
 */
public static void
logInfo(String msg) {

	if (logger_ == LoggingService.NULL) {
		System.out.println("EN4J:INFO:  " + msg);
		}
	else {
		logger_.info(msg);
		}
	}


public boolean
isInfoEnabled() {

	if (logger_ == LoggingService.NULL) {
		return isInfoEnabled_;
		}
	else  {
		return logger_.isInfoEnabled();
		}
	}



/**
 * Convenience method for non-Cougaar usage.
 */
public static void
logWarn(String msg) {

	if (logger_ == LoggingService.NULL) {
		System.out.println("EN4J:WARN:  " + msg);
		}
	else {
		logger_.warn(msg);
		}
    }


public boolean
isWarnEnabled() {

	if (logger_ == LoggingService.NULL) {
		return isWarnEnabled_;
		}
	else  {
		return logger_.isWarnEnabled();
		}
	}


/**
 *  Convenience method for non-Cougaar usage.
 * Output to both stderr and stdout so even if stdout is redirected, 
 * we'll still see the error??? (though this results in double messages if not. hmmm.)
 */
public static void
logError(String msg) {

	if (logger_ == LoggingService.NULL) {
		System.out.println("EN4J:ERROR: " + msg);
//		System.err.println("EN4J:ERROR(err): " + msg);
		}
	else {
		logger_.error(msg);
		}
	}


/**
 *  Convenience method for non-Cougaar usage.
 * Output to both stderr and stdout so even if stdout is redirected, 
 * we'll still see the error. (though this results in double messages if not. hmmm.)
 */
public static void
logError(String msg, Throwable t) {

	if (logger_ == LoggingService.NULL) {
		logError(msg);
		t.printStackTrace();
		}
	else {
		logger_.error(msg, t);
		}
	}


public boolean
isErrorEnabled() {

	if (logger_ == LoggingService.NULL) {
		return isErrorEnabled_;
	}
	else  {
		return logger_.isErrorEnabled();
		}
	}


/**
 * Move the CougaarAgents in the given CougaarSociety 
 * to reflect the current annealing state solution.
 * 
 * NPE was caused by doing a move "from" a node that doesn't exist,
 * as in moving agent from _ENTempNode_, after it's been cleaned.
 * 
 */
public boolean
moveAgentsToSolution(CougaarSociety cs) {

	if (this.isDebugEnabled())
		logDebug(".moveAgents: generating new CougaarSociety ('move' may be a misnomer)...");

	if (cs == null) {
		if (this.isDebugEnabled())
			logDebug(".moveAgents: input soc null; doing nothing.");
		return false;
		}

	for (int i=1; i<=numfunction; i++) {

		String agentName = xnfunction[i].name;
		if (agent[i].annealhostid == 0) {
			logWarn(".moveAgents: agent[" + i + "].annealhostid == 0!");
			continue;
			}
		String whichNode = nodeinfo[agent[i].annealhostid].name;
		cs.getAgent(agentName).moveTo(whichNode);		// NPE 01Sept03
		if (this.isDebugEnabled())
			logDebug(".moveAgents: agent " + agentName + " on node " + whichNode);
			}
	if (this.isDebugEnabled())
		logDebug(".moveAgents: done.");

	return true;
	}


/**
 * Remove any bogus nodes we created for solving.
 * We look to see that there are no agents on the temp node, 
 * but if there are, we just emit an error message and continue. What else ???
 * 
 * @param cs	Input CougaarSociety, not changed by this method.
 * @return		Cleaned soc, with temp node removed, even if it had agents (error emitted)
 */
public CougaarSociety
cleanTempNodes(CougaarSociety cs) {

    // 02Sept04 - preserve the caption
    String oldIdentifier = cs.getIdentifier();
	CougaarSociety resultCS = new CougaarSociety(oldIdentifier);

	Iterator iNodes = cs.getNodes();
	while (iNodes.hasNext()) {
		CougaarNode oldNode = (CougaarNode)iNodes.next();
		if (oldNode.getName().equals(EN_TEMP_NODE_NAME)) {

			if (isDebugEnabled())
				logDebug("cleanTempNodes: " + EN_TEMP_NODE_NAME + " exists, not copying!");

			int agentCount = 0;
			Iterator iAgents = oldNode.getAgents();
			while (iAgents.hasNext()) {
				agentCount++;
				iAgents.next();
				}
			if (agentCount>0) {
				// This means the load balance didn't move the agents off this bogus node.
				// Something bad happend. Log it, and  ???
				if (isErrorEnabled()) 
					logError("cleanTempNodes: " + EN_TEMP_NODE_NAME + " NOT EMPTY!");

				}
			}
		else {

			oldNode.copy(resultCS);

			}
		}

	return resultCS;
	} // cleanTempNodes


} // SystemDesign


