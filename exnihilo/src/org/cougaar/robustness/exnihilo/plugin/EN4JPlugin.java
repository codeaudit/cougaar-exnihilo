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


import org.cougaar.robustness.exnihilo.SystemDesign;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.community.*;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.robustness.dos.manager.CommunityFinder; // from robustness-bbn.jar
import org.cougaar.robustness.dos.tmatrix.CommunityTrafficMatrixService;
import org.cougaar.robustness.dos.tmatrix.TrafficMatrix;
import org.cougaar.robustness.dos.tmatrix.TrafficMatrix.TrafficRecord;
import org.cougaar.scalability.util.*; // from scalability_infoether_utilities.jar
import org.cougaar.util.log.Logger;

import java.util.*;


/**
 * EN4JPlugin - ExNihilo-for-Java Cougaar plugin.
 * 
 * <UL>
 *   <LI>Subscribes to LoadBalanceRequest (where isResult()==false),</LI>
 *   <LI>does a "load balance" on each one,</LI>
 *   <LI>and publishChanges each result.</LI>
 * </UL>
 *  <B>Although this has never been tested with more than one request at a time!</B>
 * 
 * Constantly accumulates LB data from metrics service.
 * 
 * If LB request contains stipulated data, solve that, ignoring metrics; 
 * otherwise use accumulated metrics data to solve the 'current' system.
 * 
 * Requires:
 * <UL>
 * <LI>bbn-robustness.jar</LI>
 *  for CommunityFinder
 * <LI>scalability_infoether_utilities.jar</LI>
 * 	for CougaarSociety etc
 * </UL>
 * 
 * @version 28Sept04 - Handle what happens when MAX_MESSAGE_TRAFFIC_TABLE is not big enough. (Bug 13690)
 * @version 20Aug04 - "1.20" - New traffic matrix stuff. 
 * @version 17Mar04 - Removed call to fixHammingForAgentsOnKilledNodes, useless.
 * @version 04Nov03
 *		bug #13285 - changed to INFO level, from WARN
 *		bug #13313 - changed from "CPU_MeanTimeBetweenFailure"
 *
 * @author robert.e.cranfill@boeing.com
 */
public class EN4JPlugin
     extends ComponentPlugin
  implements Runnable, Observer, CommunityResponseListener {


// these seem to be rather important...
public static final long	INITIAL_DELAY = 60 * 1000; // wait for first query to metrics - for communities?
public final long			dataCollectionPeriod_ = 120 * 1000; // get metrics data every x sec


//	public vars
public static int AGENT_ORIENTED_HASH_SIZE = 353;
public static int NODE_ORIENTED_HASH_SIZE = 53;

public static int DEFAULT_ANNEALING_TIME_SECONDS = 20;

// keep asking for metrics that aren't credible to this extent.
public double desiredMetricsCredibility_ =
	org.cougaar.core.qos.metrics.Constants.USER_BASE_CREDIBILITY;

// A system property to look for;
// value is a String representing a boolean, as Boolean.getBoolean() wants.
public static final String COLLECT_MESSAGE_TRAFFIC_PROP =
	"org.cougaar.robustness.exnihilo.plugin.COLLECT_MESSAGE_TRAFFIC";


// private vars

public static final String SEARCH_FILTER_NODES = "(&(EntityType=Node)(Role=Member))";

// Values to use if metrics data not available. Needed to keep EN from choking.
// These are values used to solve in the case where we don't have actual node and/or agent info.
//
// The agent values need to be lots bigger than the node values to keep EN from
// finding a solution violating "soft constraints", that is,
// placing one agent on the node even tho it uses 100% of the CPU, for example.
//
public static final float MINIMAL_CPU_FOR_NODE = 1.0f;
public static final float MINIMAL_RAM_FOR_NODE = 1.0f; // should be an int!
public static final float MINIMAL_CPU_FOR_AGENT = 10 * MINIMAL_CPU_FOR_NODE;
public static final float MINIMAL_RAM_FOR_AGENT = 10 * MINIMAL_RAM_FOR_NODE;

// Our subscriptions (the results thereof)
// If this is null, we may be running a test, in which case use lbReqsList_
private IncrementalSubscription lbReqs_;
private List					lbReqsList_;	// of LoadBalanceRequest

// Buncha Cougaar stuff.
//
private Logger 				logger_ = LoggingService.NULL;
private MetricsService 		metricsService_ = null;
private CommunityService 	communityService_;
private EventService 		eventService_ = null;

private WhitePagesService whitePagesService_;
private Community community_;

// The latest data snapshot. This isn't averaged, or historied, or anything,
// it's just an accumulation of the latest-and-greatest.
//
private DataBlob data_;

private boolean initedSchedulable_ = false;

private String agentIDStr_ = ""; // which agent we're loaded into

/**
 * So we can show our work, keep the last CS we solved, before and after load balancing.
 * Currently we have no way to output these; we should support BB req for them
 */
private CougaarSociety lastInputCS_ = null;
private CougaarSociety lastSolvedCS_ = null;

/**
 * Stop accumulating data when we're chewing on a LB problem.
 */
private boolean collectData_ = true;

// Hash of (String)agentName -> (String)nodeName
private Hashtable agentNodeTable_ = new Hashtable(AGENT_ORIENTED_HASH_SIZE);

// What to do with a node that the WP node->host lookup fails on?
// We don't know the node's characteristics, so I see two alternatives:
//  pile node's agents on a (the) TEMP node and ignore the unknown node,
// or use some nominal characteristics for the unknown node.
// 
//public  boolean			createUnknownNodes_ = true;
private Hashtable unfoundNodes_ = new Hashtable(NODE_ORIENTED_HASH_SIZE);
// just (String)nodeName, (String)nodeName
private float unknownNodeCPU_ = MINIMAL_CPU_FOR_NODE;
// will be updated with a running average
private float unknownNodeRAM_ = MINIMAL_RAM_FOR_NODE;
private float unknownNodePOF_ = 0.001f;
private int nNodesAveraged_ = 0;

// For agents that we can't deduce the host of, and for hamming ID for moved agents.
private CougaarNode tempNode_ = null;

// Do data collection on message traffic? Can be overridden by system property.
private boolean		collectMessageTraffic_ = true;

// 20Aug04
private CommunityTrafficMatrixService trafficMatrixService_;


/**
 * We don't do much in our constructor. Can't log info yet.
 * 
 */
public EN4JPlugin() {

	data_ = new DataBlob();
	}



/**
 * For regression testing module.
 * Set up whatever it takes for us to run in a non-Cougaar environment (that is, just a Java app).
 * 
 * @param	comma-delimited list of nodes, from command line.
 */
public void 
test(CougaarSociety inputSociety, ArrayList killedNodesList, Logger logger) {

// not terribly informative
	agentIDStr_ = "test";

	// Create a System.out logger so we can see what's happening. 
	// Perhaps there's some better way to do this, but here we are.
	//
//	logger_ = new StdOutLogger();
	logger_ = logger;

	int annealT = 10;
	int mode = LoadBalanceRequest.SOLVER_MODE_BLEND_PFAIL_LOAD_BALANCE;
	boolean hamming = false;

	ArrayList newNodesList = new ArrayList();
	ArrayList leaveAsIsNodesList = new ArrayList();

	// Create a LB request, put in global list.
	//
	lbReqsList_ = new ArrayList();
	LoadBalanceRequest lbr = new LoadBalanceRequest(annealT, mode, hamming, 
													newNodesList, killedNodesList, leaveAsIsNodesList);
	lbr.setCougaarSociety(inputSociety);
	lbReqsList_.add(lbr);


	// Need to set up the data blob!
	//



	execute();
	}

/**
 * Test routine needs this to test result.
 * @return
 */
public CougaarSociety getLastSolvedCS() {
	return lastSolvedCS_;
	}


/*
 * Get the various services we need, and register for CommunityFinder.
 * 
 * @see org.cougaar.util.GenericStateModel#load()
 */
public void load() {

	super.load();

	ServiceBroker sb = getServiceBroker();
	if (logger_ == LoggingService.NULL) {	
		logger_ = (Logger) sb.getService(this, LoggingService.class, null);	// 24mar04, was LoggingService
		}


	// 06Oct03 - If not set at all, don't change collectMessageTraffic default value.
	//
	try {
		String collectOpt = System.getProperty(COLLECT_MESSAGE_TRAFFIC_PROP);
		if (collectOpt != null)
			collectMessageTraffic_ = Boolean.valueOf(collectOpt).booleanValue();
		}
	catch (SecurityException se) {
		if (logger_.isWarnEnabled())
			logger_.warn(agentIDStr_ + ".load: SecurityException trying to get system property.");
		}


	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + " EN4JPlugin 1.20 instantiated.");
		logger_.debug(agentIDStr_ + " Startup delay: " + INITIAL_DELAY + " ms");
		logger_.debug(agentIDStr_ + " Collect message traffic? " + collectMessageTraffic_);
		logger_.debug(agentIDStr_ + "   Collection interval: " + dataCollectionPeriod_ + " ms");
		}

// 24Mar03 - FIX THIS!
//	data_.setLogger(logger_);

	// Get all the Cougaar services we'll need.
	//
	metricsService_			= (MetricsService) sb.getService(this, MetricsService.class, null);
	communityService_		= (CommunityService) sb.getService(this, CommunityService.class, null);
	whitePagesService_		= (WhitePagesService) sb.getService(this, WhitePagesService.class, null);
	trafficMatrixService_	= (CommunityTrafficMatrixService) sb.getService(this, CommunityTrafficMatrixService.class, null);

	if (metricsService_ == null) {
		if (logger_.isErrorEnabled())
			logger_.error(agentIDStr_ + ".load: metricsService_ null!");
		}

	MessageAddress agentId = getAgentIdentifier();
	agentIDStr_ = agentId.toString();

	// CommunityFinder.ForManager will call update()
	//
	CommunityFinder finder = new CommunityFinder.ForManager(communityService_, agentId);
	if (finder == null) {
		if (logger_.isErrorEnabled())
			logger_.error(agentIDStr_ + ".load: CommunityFinder.ForManager returned null!");
		} 
	else {
		finder.addObserver(this);
		}

	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + ".load OK");
		logger_.debug(agentIDStr_ + ".load agentId: " + agentIDStr_);
		}

	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + ".load calling initSchedulable");
		}

	initSchedulable();

	} // load


/**
 * Called when we are loaded - subscribe to LoadBalanceRequest request objects.
 */
protected void setupSubscriptions() {

	// We want LoadBalance requests, not results (we post results).
	//
	lbReqs_ =
		(IncrementalSubscription) getBlackboardService().subscribe(
			new LoadBalanceRequestPredicate(false));

	logger_ =
		(LoggingService) getServiceBroker().getService(this, LoggingService.class, null);

	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + ".setupSubscriptions OK");
		}

	} // setupSubscriptions


/**
 * Callback for Community Service
 * @param cs
 */
public void setCommunityService(CommunityService cs) {
	communityService_ = cs;
	}


/**
 * Callback for Event Service.
 * @param evtSvc
 */
public void setEventService(EventService evtSvc) {
	eventService_ = evtSvc;
	}



/**
 * Called when there is a change on my subscriptions;
 * extract CougaarSociety object and balance it!
 * 
 */
protected void execute() {

	if (lbReqs_ == null) {
		if (logger_.isInfoEnabled()) {
			logger_.info(agentIDStr_ + ".execute: lbReqs_ is null - subscriptions not set up? - test mode??");
			}
		// return;
		}

	// Handle LoadBalanceRequests, either containing CougaarSociety data or not.
	//
	Iterator lbReqs;
	if (lbReqs_ != null)
		lbReqs = lbReqs_.iterator();
	else
		lbReqs = lbReqsList_.iterator();

	while (lbReqs.hasNext()) {

		LoadBalanceRequest lbr = (LoadBalanceRequest) lbReqs.next();

		sendCougaarEvent("STATUS", "EN4JPlugin: Got load balance request: " + lbr);

		// We only process requests, not solutions (which are what we post).
		// (we shouldn't get anything here, since we asked for requests only.)
		//
		if (lbr.isResult()) {

			if (logger_.isInfoEnabled()) {
				logger_.info(".execute ignoring solution: " + lbr);
			}
			continue;
		}

		// If this request contains no CougaarSociety data,
		// or if we are configged to ignore input data,
		// we need to form CS data from our collected metrics.
		//
		CougaarSociety originalSociety = lbr.getCougaarSociety();
		if (originalSociety == null) {

			if (logger_.isInfoEnabled()) {
				logger_.info(agentIDStr_
						+ ".execute: No stipulated data - using accumulated metrics data.");
				}
			originalSociety = createCSFromLBData(lbr.getNewNodes(), lbr.getKilledNodes(), lbr.getLeaveAsIsNodes());
			}
		else {
			// we don't otherwise show the input soc???
			if (logger_.isInfoEnabled()) {
				logger_.info(agentIDStr_  + ".execute: Input society is:");
				logger_.info(originalSociety.toXML());
				logger_.info("");
				}
			}

		if (logger_.isInfoEnabled()) {
			logger_.info(agentIDStr_ + ".execute got LoadBalanceRequest: " + lbr);
			}


		// Create a SystemDesign object, 
		// load the CougaarSociety data into its internal data structures,
		// and ask for a solution.
		//

		SystemDesign.setNewMaxAgents(data_.agentCount_ + 10);	// 26May04 - about the min we can get away with?
		if (logger_.isInfoEnabled()) {
			logger_.info(agentIDStr_  + ".execute: Set numfunctionmax = " + SystemDesign.numfunctionmax);
			}
		SystemDesign sd = new SystemDesign();
		SystemDesign.setLogger(logger_);

		// Wrap this all in a nice try/catch block, so we can output any exceptions to the Logger.
		//
		try {

			// loadDataFromSociety() also sets hamming ids
			//
			if (sd.loadDataFromSociety(originalSociety) == false) {
				if (logger_.isErrorEnabled()) {
					logger_.error(agentIDStr_ + ".execute: Error in loadDataFromSociety!");
					}
				sendCougaarEvent("STATUS", "EN4JPlugin: .execute exiting abnormally; see logfiles.");
				return;
				}

			if (sd.checkProject() == false) {
				if (logger_.isErrorEnabled()) {
					logger_.error(agentIDStr_ + ".execute: Data fails checkProject!");
					}
				sendCougaarEvent(
					"STATUS","EN4JPlugin: Error: Data fails 'checkProject'! See logfile for more info. Exiting.");
				return;
				}

			// Set the anneal time, and run it
			//
			int annealTime = lbr.getAnnealTime();
			if (annealTime == -1) {
				annealTime = DEFAULT_ANNEALING_TIME_SECONDS;
				if (logger_.isInfoEnabled()) {
					logger_.info(
						agentIDStr_ + ".execute: Setting default annealing time to " +
						annealTime + " seconds!");
					}
				}
			if (logger_.isInfoEnabled()) {
				logger_.info(agentIDStr_ + ".execute: checkProject OK!");
				}


			// Only support the two main modes to be used by final testing. 19Sept04
			//
			switch (lbr.getSolverMode()) {

			    case LoadBalanceRequest.SOLVER_MODE_BLEND_PFAIL_LOAD_BALANCE:
					sd.setSolverMode_BlendPfailLoadBalance(true);
			        break;

			    case LoadBalanceRequest.SOLVER_MODE_MIN_MESSAGING:
					sd.setSolverMode_MinMessaging(true);
			        break;

			    default:

					if (logger_.isErrorEnabled()) {
    					logger_.error(agentIDStr_ + ".execute: UNSUPPORTED SOLVER MODE: " 
    							+ lbr.getSolverMode() + "; using SOLVER_MODE_BLEND_PFAIL_LOAD_BALANCE)");
    					sendCougaarEvent("STATUS", "EN4JPlugin: Warning: UNSUPPORTED SOLVER MODE: "
    							+ lbr.getSolverMode() + "; resetting and continuing.");

    					sd.setSolverMode_BlendPfailLoadBalance(true);
    					}
			        break;
			
				} // switch (lbr.getSolverMode()
	
			if (lbr.getDoHamming() == false) {

				if (logger_.isInfoEnabled()) {
					logger_.info(agentIDStr_ + ".execute: FORCING HAMMING MODE! (I assume UL wants hamming.");
					}

				lbr.setDoHamming(true);
				}

			// So-called 'hamming' solution?
			//
			if (lbr.getDoHamming()) {

				if (logger_.isInfoEnabled()) {
					logger_.info(agentIDStr_ + ".execute: 'Hamming' solution requested.");
					}

				sd.blendpfailhamming = true;

				// these are only pertinent if we're doing hamming
				sd.hammingmax = sd.objfnjobset.numobjfn;
				sd.hammingmin = 0.0001f;
				for (int i = 1; i <= sd.numfunction; i++) {
					sd.objfnjobset.initfn[i] = i;
					}

				// sd.loadDataFromSociety will have set the input hamming host ids

				// Get the list of nodes that were killed.
				// agents can't be allowed to request one of those for hamming.
				//
				List killedNodes = lbr.getKilledNodes();

				// for test.
				//if (logger_.isDebugEnabled()) {
				//	logger_.debug(agentIDStr_ + ".execute ADDING A MOVED AGENT: 16-ENGBN");
				//	}
				//if (movedAgents == null)
				//	movedAgents = new ArrayList();
				//movedAgents.add("16-ENGBN");

//	17Mar04 - This didn't work, becase if a node had been in the killed list, 
//	it was simply never created in the CS data.
//				if (killedNodes != null && !killedNodes.isEmpty()) {
//					sd.fixHammingForAgentsOnKilledNodes(killedNodes);
//					}

				} // hamming 

			if (logger_.isInfoEnabled()) {
				logger_.info(agentIDStr_ + ".execute: Annealing " + annealTime + " seconds....");
				}

			//
			// This is what actually solves the data. Whew!
			//
			if (sd.runConfig(annealTime) == false) {
				if (logger_.isWarnEnabled())
					logger_.warn(agentIDStr_ + ".execute: Cannot find annealing solution");
				sendCougaarEvent("STATUS", "EN4JPlugin: Warning: Cannot find annealing solution");
				//                return;   24sept03
				}

			//
			// Re-post the solution
			//

			// We used to use the built-in .copy method on CougaarSociety.
			// But we also needed to then clean the temp node out of our result,
			// but there seems to be no facility for removing a CougaarNode from
			// a CougaarSociety, so.... we'll write our own methods to do those two
			// things.

			// Note: NPE 01Sept03: if we clean the node first, moveAgentsToSolution fails,
			// because the 'from' node no longer exists!
			//
			CougaarSociety newSociety = originalSociety.copy();
			
			/* boolean okMove = */ sd.moveAgentsToSolution(newSociety);

			CougaarSociety newSociety2 = sd.cleanTempNodes(newSociety);

			// 04Nov03 - for bug 13285 - rec - changed to INFO level from WARN
			if (logger_.isInfoEnabled()) {
				logger_.info(agentIDStr_ + "------------------------------------------------------");
				logger_.info(agentIDStr_ + ".execute: Final new society is:" );
				logger_.info("\n" + newSociety2.toXML());
				logger_.info(agentIDStr_ + "------------------------------------------------------");
				}

			// return some stuff that no one will ever look at.
			lbr.setCougaarSociety(newSociety2);
			lbr.isResult(true);
			lbr.setSolutionFound(sd.state.status);
			// status of annealing step
			lbr.setPFail(sd.pfailureave);
			lbr.setIterations(sd.getLastAnnealIterCount()); // for fun

			sendCougaarEvent("STATUS", "EN4JPlugin: Finished load balance; posting solution: " + lbr);


			// Doesn't need to be in a transaction?
			//
				
			// Might not be in Cougaar environment...
			if (getBlackboardService() == null) {
				if (logger_.isWarnEnabled()) {
					logger_.warn(agentIDStr_ + ".execute: No blackboard service - running in test mode?");
					}
				}
			else {
				if (logger_.isInfoEnabled()) {
					logger_.info(agentIDStr_ + ".execute: Doing publishChange of solution...");
					}

				getBlackboardService().publishChange(lbr);
	
				if (logger_.isInfoEnabled()) {
					logger_.info(agentIDStr_ + ".execute: Solution published OK!");
					}
				}

			// Happy or sad?
			//
			CSVisualizer csv = new CSVisualizer(newSociety2);
			boolean withinLimits = csv.isWithinLimits();
			sendCougaarEvent("STATUS", "EN4JPlugin: Solution within constraints? " + withinLimits);

			if (withinLimits) {
				if (logger_.isInfoEnabled()) {
					logger_.info(agentIDStr_ + ".execute: Proposed solution satisfies constraints!");
					}
				} 
			else {
				if (logger_.isErrorEnabled()) {
					logger_.error(agentIDStr_
							+ ".execute: Proposed solution DOES NOT SATISFY constraints!!!");
					}
				}

			if (logger_.isInfoEnabled()) {
				logger_.info(agentIDStr_ + ".execute: Is annealing solution? "
						+ (lbr.wasSolutionFound() ? "Yes" : "NO!"));
				}

			// Only hang on to the last pair, not all of 'em - OK?
			// I don't expect we'll really get multiple requests, so this isn't a problem.
			//
			lastInputCS_ = originalSociety.copy();
			lastSolvedCS_ = newSociety2.copy();

			} 
		catch (Exception e) {
			if (logger_.isErrorEnabled()) {

				//				logger_.error(RnR.getStackTrace(e));
				//				logger_.error(agentIDStr_ + " *******************");
				//				logger_.error(agentIDStr_ + " Exception in EN4JPlugin.execute: " + e.toString());

				// Ah, I see Cougaar has a method to do this...
				logger_.error("Exception in EN4JPlugin.execute: ", e);
				}
			} 
		finally {
			if (sd != null) {
				sd.stopThreads();
				}
			}

		} // LoadBalanceRequests subscription list

	if (logger_.isInfoEnabled()) {
		logger_.info(agentIDStr_ + ".execute finished!");
		}

	sendCougaarEvent("STATUS", "EN4JPlugin: .execute exiting normally.");

	} // execute


/**
 * Create a CougaarSociety representing current metrics data in DataBlob data_.
 * 
 * If there's a list of nodes to exclude from consideration in keepOffNodes_, do so.
 * 
 * @param	newNodes	Any newly-created nodes we might not know about via metrics
 * @param	killedNodes	Any newly-destroyed nodes we might not know about via metrics (not useful?)
 * @param	keepOffNodes
 * @return	i just told you!
 */
private CougaarSociety createCSFromLBData(List newNodes, List killedNodes, List keepAsIsNodes) {

	// Turn off metrics service data collection. 
	//
	collectData_ = false;

	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + ".createCSFromLBData: data blob is: " + data_);
		}

	// If we have no metrics data, how to tell?
	// The $#!+ will hit the fan after this.... fix. ???
	//
	if (data_.nodes_.size() == 0) {
		if (logger_.isErrorEnabled()) {
			logger_.error(
				agentIDStr_ + ".createCSFromLBData data_.nodes_.size() is zero! metrics collection failure?");
			return null;
			}
		}

	// Ensure we can do listy things - in particular, .contains() - with these.
	if (newNodes == null) {
		newNodes = new ArrayList();
		}
	if (killedNodes == null) {
		killedNodes = new ArrayList();
		}
	if (keepAsIsNodes == null) {
		keepAsIsNodes = new ArrayList();
		}

	CougaarSociety newSoc = new CougaarSociety("Created by EN4J from metrics, " + new java.util.Date());
	CougaarNode node = null;
	CougaarAgent agent = null;

	ArrayList nodesCreated = new ArrayList();
	// so we don't create anything twice

	// Create the nodes we do have metrics data for...
	//
	Enumeration nodeNameEnum = data_.getNodes().keys();
	while (nodeNameEnum.hasMoreElements()) {

		String nodeName = (String) nodeNameEnum.nextElement();

		// If node is in keepAsIsNodes list, pretend it doesn't exist.
		//
		if (keepAsIsNodes.contains(nodeName)) {
			if (logger_.isDebugEnabled())
				logger_.debug(agentIDStr_ + ".createCSFromLBData: node " + nodeName + 
					" is in keep-as-is list; ignoring.");
			continue;
			}


		// If node is in killed list, it really *doesn't* exist any more.
		//
		if (killedNodes.contains(nodeName)) {
			if (logger_.isDebugEnabled())
				logger_.debug(agentIDStr_
						+ ".createCSFromLBData: node " + nodeName + " is in killed list; ignoring.");
			continue;
			}

		node = newSoc.newNode(nodeName);
		nodesCreated.add(nodeName);

		// this won't happen - if we don't have data, it won't be in the list!
		// but leave this, it's OK.
		HostMetricData hostData = data_.getNodeData(nodeName);
		if (hostData == null) {

			if (logger_.isErrorEnabled())
				logger_.error(agentIDStr_ + ".createCSFromLBData: HostMetricData not found for " + nodeName);

			} 
		else {

			// Set Host attributes (remember that node==host, sorta)
			//  - EN wants integer values for CPU and RAM...
			node.setAttribute(CougaarNode.CPU, 					  "" + (int)hostData.jips_);
			node.setAttribute(CougaarNode.MEMORY, 				  "" + (int)hostData.totalMemory_);
			node.setAttribute(CougaarNode.PROBABILITY_OF_FAILURE, "" + hostData.pFail_);

			if (logger_.isDebugEnabled())
				logger_.debug(agentIDStr_ + 
						".createCSFromLBData: Node " + nodeName + 
						": CPU(jips)=" + hostData.jips_ + ", RAM(totalK)=" + hostData.totalMemory_ +
						", pFail=" + hostData.pFail_ + ", cred=" + hostData.credibility_);
			}

		} // nodes with metrics data


	// ...also create any "unknown" nodes, nodes we didn't get metrics info for.
	//
	nodeNameEnum = unfoundNodes_.elements();
	while (nodeNameEnum.hasMoreElements()) {

		String nodeName = (String) nodeNameEnum.nextElement();
		node = newSoc.newNode(nodeName);
		nodesCreated.add(nodeName);

		if (logger_.isErrorEnabled())
			logger_.error(agentIDStr_
					+ ".createCSFromLBData: Using AVERAGE data for unknown host of node " + nodeName);

		// Set Host attributes (remember that node==host, sorta)
		//  - EN wants integer values for CPU and RAM...
		node.setAttribute(CougaarNode.CPU, 					  "" + (int)unknownNodeCPU_);
		node.setAttribute(CougaarNode.MEMORY, 				  "" + (int)unknownNodeRAM_);
		node.setAttribute(CougaarNode.PROBABILITY_OF_FAILURE, "" + unknownNodePOF_);

		if (logger_.isDebugEnabled())
			logger_.debug(agentIDStr_ + 
					".createCSFromLBData: AVG Node " + nodeName +
					": CPU(jips)=" + unknownNodeCPU_ + ", RAM(totalK)=" + unknownNodeRAM_ + 
					", pFail=" + unknownNodePOF_);

		} // un-metricatated nodes


	// Always create a temp node for hamming moved agents onto.
	// It will get filtered from the output, and we need to create it up front,
	// so we'll create it now, even tho sometimes we don't end up using it.
	//
	// We want this to be a node nothing can run on.
	//
	tempNode_ = newSoc.newNode(SystemDesign.EN_TEMP_NODE_NAME);
	tempNode_.setAttribute(CougaarNode.CPU, 				   "" + (int)MINIMAL_CPU_FOR_NODE);
	tempNode_.setAttribute(CougaarNode.MEMORY, 				   "" + (int)MINIMAL_RAM_FOR_NODE);
	tempNode_.setAttribute(CougaarNode.PROBABILITY_OF_FAILURE, "0.9999"); //should be int?

	// For any totally new nodes specified by our caller, create them
	// if we haven't already (possibly they were caught by our metrics scan?)
	//
	Iterator newNodeIter = newNodes.iterator();
	while (newNodeIter.hasNext()) {

		String newNodeName = (String) newNodeIter.next();
		if (nodesCreated.contains(newNodeName)) {
			continue;
			}

		// create the node
		node = newSoc.newNode(newNodeName);
		nodesCreated.add(newNodeName);

		// can we get host info now? sure why not.
		int tryCount = 0;
		double cred = org.cougaar.core.qos.metrics.Constants.NO_CREDIBILITY;
		while (tryCount < 2 && cred < desiredMetricsCredibility_) {
			tryCount++;
			HostMetricData hmd = getHostDataForNode(newNodeName);
			if (hmd == null) {

				if (logger_.isDebugEnabled())
					logger_.debug(agentIDStr_
							+ ".createCSFromLBData:Using AVERAGE data for new node " + newNodeName);

				hmd = new HostMetricData();
				hmd.jips_ = unknownNodeCPU_;
				hmd.totalMemory_ = unknownNodeRAM_;
				hmd.pFail_ = unknownNodePOF_;
			}

			node.setAttribute(CougaarNode.CPU, 					  "" + (int)hmd.jips_);
			node.setAttribute(CougaarNode.MEMORY,				  "" + (int)hmd.totalMemory_);
			node.setAttribute(CougaarNode.PROBABILITY_OF_FAILURE, "" + hmd.pFail_);

			cred = hmd.credibility_;

			if (logger_.isDebugEnabled())
				logger_.debug(agentIDStr_ + ".createCSFromLBData: new Node " +
						newNodeName + " #" + tryCount + ": CPU(jips)=" + hmd.jips_ +
						", RAM(totalK)=" + hmd.totalMemory_ + ", pFail=" +
						hmd.pFail_ + ", cred=" + hmd.credibility_);

			} // get good cred
		} // newNodes

	int cpuErrors = 0;
	int ramErrors = 0;

	// Put all the agents on thier nodes, or the temp node if node was killed.
	//
	for (int i=0; i<data_.agentCount_; i++) {

		String agentName = data_.agents_[i];
		String nodeForAgent = (String)agentNodeTable_.get(agentName);
		if (nodeForAgent != null) {

			// If this agent's node is in the keepAsIsNodes list, ignore the agent, too.
			//
			if (keepAsIsNodes.contains(nodeForAgent)) {
				if (logger_.isInfoEnabled())
					logger_.info(agentIDStr_ + ".createCSFromLBData: agent " + agentName
							+ " is on node " + nodeForAgent + " and will be left as-is.");
				continue;
				}

			// If the agent was on a killed node, put it on temp node, to be loadbalanced to elsewhere.
			//
			if (killedNodes.contains(nodeForAgent)) {

				if (logger_.isInfoEnabled())
					logger_.info(
						agentIDStr_ + ".createCSFromLBData: agent " + agentName +
							" was on killed node " + nodeForAgent + " and will be moved.");
				nodeForAgent = null;
				}

			} // nodeForAgent != null

		// Create a CougaarAgent on either the found node, or the temp node if not found.
		//
		if (nodeForAgent == null) {

			// We don't know what node the agent belongs on, or may have been on killed node,
			// so put it on the unusable temp node.
			//
			agent = tempNode_.newAgent(agentName);
			} 
		else {

			// Put the agent on its node.
			//
			CougaarNode oldNode = newSoc.getNode(nodeForAgent);
			
			//  28Sept04 - when MAX_MESSAGE_TRAFFIC_TABLE isn't big enough, this bad thing happens.
			if (agent == null) {
			    if (logger_.isErrorEnabled()) {
			        logger_.error("EN4JPlugin: can't get society node '" + nodeForAgent + "' for agent '" + agentName + "'");
					agent = tempNode_.newAgent(agentName);
			    	}
				}
			else {
			    agent = oldNode.newAgent(agentName);
				}
			}

		// Create RAM and CPU requirements on the agent.
		// If metrics data zero, use some small nonzero amount.
		// (make that amount significanly bigger than the attributes of the temp node,
		// so the agent can't possibly run there.)
		//
		double cpu = data_.getCPULoadJIPS(agentName);
		if (cpu <= 0) {

			cpuErrors++; // 26sept03
			if (logger_.isInfoEnabled()) {
				logger_.info(".createCSFromLBData: agent " + agentName + " CPU <= 0! setting to " +
				 				MINIMAL_CPU_FOR_AGENT + " (was " + cpu + ")");
				}
			cpu = MINIMAL_CPU_FOR_AGENT;
			}
		agent.setRequirement(CougaarAgent.CPU, "" + cpu);

		double ram = data_.getRAMUsageK(agentName);
		if (ram <= 0) {

			ramErrors++; // 26sept03
			if (logger_.isInfoEnabled()) {
				logger_.info(".createCSFromLBData: agent " + agentName + " RAM <= 0! setting to " +
								 MINIMAL_RAM_FOR_AGENT + " (was " + ram + ")");
				}
			ram = MINIMAL_RAM_FOR_AGENT;
			}
		agent.setRequirement(CougaarAgent.MEMORY, "" + ram);

		if (logger_.isDebugEnabled()) {
			logger_.debug(agentIDStr_ + ".createCSFromLBData: agent "
							+ agentName + " CPU: " + cpu + ", RAM: " + ram);
			}

		// We don't use OS info any more.
		//	agent.setRequirement(CougaarAgent.OPERATING_SYSTEM, "Linux");

		// Message traffic - EN uses only 'sent' data
		// (presumably [A sent to B] == [B received from A])
		//
		if (collectMessageTraffic_) {	// 26May04 - don't bother if no traffic data

			int thisAgentIndex = data_.getAgentTrafficIndex(agentName, false);
			if (thisAgentIndex > -1) {
	
				for (int j=0; j<data_.agentCount_; j++) {
	
					double traffic = data_.messageTraffic_[thisAgentIndex][j];
					if (traffic > 0) {
	
						agent.setRequirement(
							"BandwidthSent_" + data_.agents_[j],
							"" + traffic);
						if (logger_.isDebugEnabled()) {
							logger_.debug(agentIDStr_ + ".createCSFromLBData: BandwidthSent_ "
									+ agentName + " to " + data_.agents_[j] + "=" + traffic);
							}
		
						} // >0 traffic
					}
				}
			}

		} // agentIter

	if (cpuErrors > 0 && logger_.isInfoEnabled()) {
		logger_.info(agentIDStr_ + ".createCSFromLBData: There were "
						+ cpuErrors + " agents with unknown/uacceptable CPU requirements.");
		}

	if (ramErrors > 0 && logger_.isInfoEnabled()) {
		logger_.info(agentIDStr_ + ".createCSFromLBData: There were "
						+ ramErrors + " agents with unknown/uacceptable CPU requirements.");
		}

	if (logger_.isInfoEnabled()) {
		logger_.info(agentIDStr_
				+ ".createCSFromLBData: CougaarSociety from metrics data:\n"
				+ newSoc.toXML());
		}

	// Turn data collection back on
	//
	collectData_ = true;

	return newSoc;
	} // createCSFromLBData


/*
 * for CommunityResponseListener
 * 	??? how/why is this different from update() ???
 * 
 * - this only happens once, ever.
 * 
 */
public void getResponse(CommunityResponse response) {

	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + ".getResponse: " + response);
		}

	if (response.getStatus() == CommunityResponse.SUCCESS) {
		try {
			Community content = (Community)response.getContent();
			handleCommunity(content);
		} 
		catch (ClassCastException cce) {
			if (logger_.isErrorEnabled()) {
				logger_.error(agentIDStr_+ ".getResponse: response is class " 
								+ response.getClass().getName());
				}
			}
		}
	}


/* 
 * Observable i/f for CommunityFinder
 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
 */
public void update(Observable o, Object arg) {

	String communityName = (String)arg;

	if (logger_.isDebugEnabled()) {
		logger_.debug(
			agentIDStr_ + ".update: communityName: " + communityName);
		}

	Community community =
		communityService_.getCommunity(communityName, this);
	if (community != null) {
		handleCommunity(community);
		}
	
	}	// update


/*
 * 	This could be invoked by either getResponse() or update() - which is it?
 * 
 *	- get handle to community
 *	- schedule this object to be run() peridocally via ThreadService
 */
private void handleCommunity(Community community) {

	community_ = community;

	initSchedulable();

	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + ".handleCommunity got cmty: " + community_.getName());
		logger_.debug(agentIDStr_ + ".handleCommunity setting up search for: " + SEARCH_FILTER_NODES);

		//		logger_.debug(agentIDStr_ + ".handleCommunity communityService_==commSvc_? " +
		//									(communityService_==commSvc_?"yes":"no"));

		}

	// 
	// create a search for nodes in this cmty???

	// Add a listener for changes in communities of interest
	//
	communityService_.addListener(new CommunityChangeListener() {

		public void communityChanged(CommunityChangeEvent cce) {
			doSearchNodes(community_.getName(), SEARCH_FILTER_NODES);
			// find nodes in community
			}

		public String getCommunityName() {
			return community_.getName();
			}

		});

	} // handleCommunity


/**
 * Do this only once?
 *
 */
private void initSchedulable() {

	if (initedSchedulable_)
		return;

	initedSchedulable_ = true;

	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + ".initSchedulable:");
		logger_.debug(agentIDStr_ + "  scheduling thread: delay: " + 
						INITIAL_DELAY + ", period: " + dataCollectionPeriod_);
		}

	ServiceBroker sb = getServiceBroker();
	ThreadService threadService = (ThreadService)sb.getService(this, ThreadService.class, null);
	Schedulable schedulable = threadService.getThread(this, this, "EN4JDataGathererSchedulable");
	schedulable.schedule(INITIAL_DELAY, dataCollectionPeriod_);
	sb.releaseService(this, ThreadService.class, threadService);

	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + ".initSchedulable OK");
		}
	
	} // initSchedulable


/*
 * Runnable - the body of the Schedulable.
 * 
 * This will look up the community's Entities for each call,
 * since there's no way to know when all the members have registered (is there?).
 * 
 * @see java.lang.Runnable#run()
 */
public void run() {

	long startMS = System.currentTimeMillis();

	if (collectData_ == false) {
		if (logger_.isInfoEnabled()) {
			logger_.info( agentIDStr_ + ".run: collectData_ is FALSE; *NOT* collecting data.");
			}
		return;
		}

	if (logger_.isDebugEnabled()) 
		logger_.debug(agentIDStr_ + ".run: Begin metrics data collection.");

	if (community_ == null) {
		if (logger_.isInfoEnabled()) 
			logger_.info(agentIDStr_ + ".run: community_ is null; exiting without community");
		return;
		}

	if (metricsService_ == null) {
		if (logger_.isErrorEnabled())
			logger_.error(agentIDStr_ + ".run: NO METRICS SERVICE!!");
		return;
		}

	// Get the names of all our agents, and collect data on 'em.
	//
	HashSet agentNames = new HashSet();
	Iterator i = community_.getEntities().iterator();
	while (i.hasNext()) {
		Entity entity = (Entity) i.next();
		agentNames.add(entity.getName());
		}

	// Get the data from the metrics service.
	//
	lookupCPU(agentNames);
	lookupSize(agentNames);
	lookupAgentNodeInfo(agentNames);

	// 06Oct03 - Don't do this if sys prop says not to.
	//
	if (collectMessageTraffic_) {
		lookupTraffic(agentNames); 
		}


	if (logger_.isDebugEnabled()) {

		// ??? just for debug
		dumpTrafficData();

		logger_.debug(agentIDStr_ + ".run OK");
		logger_.debug(agentIDStr_ + " DataBlob now is " + data_);
		logger_.debug(agentIDStr_ + " Elapsed ms = " + (System.currentTimeMillis() - startMS));
		}
	
	} // run


/**
 * Populate the agents-to-nodes table.
 * Also, if traffic data is not being collected, populate agent table.
 * 
 * @param agentNames
 */
private void lookupAgentNodeInfo(Collection agentNames) {

	Iterator iAgents = agentNames.iterator();
	while (iAgents.hasNext()) {

		String agentName = (String)iAgents.next();
		getNodeDataForAgent(agentName);

// 08Sept04 - This is good to do any time
//		// 26May04 - have to accumulate agent info if traffic is not being collected.
//		if (collectMessageTraffic_ == false) {

			int agentIndex = -1;
			for (int i=0; i<data_.agentCount_; i++) {
				if (data_.agents_[i].equals(agentName)) {
					agentIndex = i;
					break;
					} 
				}

			if (agentIndex == -1) {
				if (data_.agentCount_ == DataBlob.MAX_MESSAGE_TRAFFIC_TABLE) {
					if (logger_.isWarnEnabled()) {
						logger_.warn(agentIDStr_ + ".lookupAgentNodeInfo: AGENT TABLE FULL! (" 
								+ data_.agentCount_ + ") - can't add agent " + agentName);
						}
					}
				else {
					data_.agents_[data_.agentCount_] = agentName;
					data_.agentCount_++;
					if (logger_.isDebugEnabled()) {
						logger_.debug(agentIDStr_ + ".lookupAgentNodeInfo: added agent " + agentName +"; " + data_.agentCount_ + " total");
						}
					}
				}

//			} // ! collectMessageTraffic_

		}
	} // lookupAgentNodeInfo


/**
 * Display for debug/info.
 */
private void dumpTrafficData() {

	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + ".dumpTrafficData: " + data_.agentCount_ + " used");
		}

	int countNonzero = 0;
	for (int i=0; i<data_.agentCount_; i++) {
		for (int j=0; j<data_.agentCount_; j++) {

			if (data_.messageTraffic_[i][j] != 0) {
				countNonzero++;

// too much information!				
//				logger_.debug(agentIDStr_ + ".dumpTrafficData: "
//						+ i + "," + j + "=" + data_.messageTraffic_[i][j]);

				}
			}
		}

	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + ".dumpTrafficData: " + countNonzero + " are nonzero.");
		}
	
	} // dumpTrafficData


/*
 * Get the CPU usage per agent.
 */
private void lookupCPU(Collection agents) {

	if (metricsService_ == null) {
		return;
		}

	String agentName = null;
	String path = null;
	int countNonzero = 0;

	Iterator i = agents.iterator();
	while (i.hasNext()) {
		agentName = (String) i.next();
		
		if (logger_.isDebugEnabled()) {
		    logger_.debug(agentIDStr_ + "lookupCPU: looking up agent " + agentName);
			}
		
		path = "Agent(" + agentName + ")"
				+ org.cougaar.core.qos.metrics.Constants.PATH_SEPR
				+ org.cougaar.core.qos.metrics.Constants.CPU_LOAD_MJIPS_1000_SEC_AVG;

		// 25sept03 - was 100 sec

		double value = metricsService_.getValue(path).doubleValue();

		// Metric is in MJIPS, we use JIPS.
		//
		value *= 1E6;
		if (value > 1000) {
			value = Math.floor(value); // truncate big numbers for tidiness
			}
		data_.setCPULoadJIPS(agentName, value);
		if (value != 0.0) {
			countNonzero++;

			if (logger_.isDebugEnabled()) 
				logger_.debug(agentIDStr_ + "lookupCPU: agent " + agentName + " cpu=" + value);

			}
	
		}

	if (logger_.isDebugEnabled()) 
		logger_.debug(agentIDStr_ + ".lookupCPU: got " + countNonzero + " nonzero CPU datapoints");

	
	} // lookupCPU


/*
 * Get the RAM usage per agent.
 * Uses persistance size as a proxy for RAM usage.
 */
private void lookupSize(Collection agents) {

	if (metricsService_ == null) {
		return;
		}

	String agentName = null;
	String path = null;
	int countNonzero = 0;

	Iterator i = agents.iterator();
	while (i.hasNext()) {
		agentName = (String) i.next();
		path = "Agent(" + agentName + ")"
				+ org.cougaar.core.qos.metrics.Constants.PATH_SEPR
				+ org.cougaar.core.qos.metrics.Constants.PERSIST_SIZE_LAST;

		double value = metricsService_.getValue(path).doubleValue();
		data_.setRAMUsage(agentName, value);
		if (value != 0.0) {
			countNonzero++;

			if (logger_.isDebugEnabled()) 
				logger_.debug(agentIDStr_ + ".lookupSize: agent " + agentName + " size=" + value);

			}
		}

	if (logger_.isDebugEnabled()) {
		logger_.debug(agentIDStr_ + ".lookupSize: got " + countNonzero + " nonzero size datapoints");
		}
	
	} // lookupSize


/*
 * Get the message traffic data.
 * 
 * @version 20Aug04 - new traffic matrix stuff
 * 
 */
private void lookupTraffic(Collection agents) {

	if (trafficMatrixService_ == null) {
		if (logger_.isWarnEnabled()) {
			logger_.warn(agentIDStr_ + ".lookupTraffic: trafficMatrixService_ null!"); 
			}
		return;
		}

	// Get snapshot of data, process it.
	//
	TrafficMatrix agentFlowSnapshot = trafficMatrixService_.snapshotMatrix();
	if (agentFlowSnapshot == null) {
		if (logger_.isWarnEnabled()) {
			logger_.warn(agentIDStr_ + ".lookupTraffic: agentFlowSnapshot null!"); 
			}
		return;
	    }

	// Walk the list of non-zero items.
	//
    TrafficMatrix.TrafficIterator iterTraffic = agentFlowSnapshot.getIterator();
    while (iterTraffic.hasNext()) {

		TrafficRecord traffRecord = (TrafficRecord)iterTraffic.next();
		MessageAddress origin = iterTraffic.getOrig();
		MessageAddress target = iterTraffic.getTarget();
		if (logger_.isDebugEnabled()) {
		    logger_.debug("Origin: " + origin + ", Target: " + target + ": " + traffRecord);
			}

	    double msgCt  = traffRecord.msgCount;
	    double byteCt = traffRecord.byteCount;
//	    double msgCt  = agentFlowSnapshot.getMsgCount(origin, target);
//	    double byteCt = agentFlowSnapshot.getByteCount(origin, target);
	    if (logger_.isDebugEnabled()) {
	        logger_.debug("        msgCount: " + msgCt + ", byteCount: " + byteCt);
		    }

	    data_.setMessageTraffic(origin.toString(), target.toString(), byteCt);
	
		}

/*
	Iterator j = null;
	String source = null;
	String destination = null;
	String path = null;

	int countNonzero = 0;

	Iterator i = agents.iterator();
	while (i.hasNext()) {
		source = (String) i.next();
		j = agents.iterator();
		while (j.hasNext()) {
			destination = (String) j.next();
			path = "AgentFlow(" + source + "," + destination + ")"
					+ org.cougaar.core.qos.metrics.Constants.PATH_SEPR
					+ org.cougaar.core.qos.metrics.Constants.MSG_RATE_1000_SEC_AVG;
			// 25sept03 - was 100 sec

			double value = metricsService_.getValue(path).doubleValue();
			data_.setMessageTraffic(source, destination, value);

			if (value != 0.0) {
				countNonzero++;

//				if (logger_.isDebugEnabled()) {
//					logger_.debug(agentIDStr_ + ".lookupTraffic: agent " + agentName + " traffic to " 
//							+ destination + "=" + value);
//					}

			}

		//	 We don't use the credibility stuff here. Elsewhere?
		//
		//				if (metric.getCredibility() > DEFAULT_CREDIBILITY) {
		//					...

		}
	}

	if (logger_.isDebugEnabled()) 
		logger_.debug(agentIDStr_ + ".lookupTraffic: got " + countNonzero + " nonzero traffic datapoints");

*/

	} // lookupTraffic


/**
* List community members with attribute "EntityType=Node".
* Will call back to nodeCommunityResponseListener
*/
private void doSearchNodes(String community, String filter) {

	if (logger_.isDebugEnabled())
		logger_.debug(agentIDStr_ + ".doSearchNodes");

	// If community is found in cache results are returned
	// immediately.  Otherwise, the CommunityResponseListener
	// callback is invoked later with results.
	// Since we've asked for this, the data's already
	// cached, and we *will* get it back right away, not via 
	// nodeCommunityResponseListener, it seems.
	// 
	// why not communityService_
	Collection nodes = 
		communityService_.searchCommunity(
			community, filter, false, Community.ALL_ENTITIES, nodeCommunityResponseListener);

	// If data is cached, we get it back now, else via CommunityResponseListener
	//
	if (nodes != null) {

		if (logger_.isDebugEnabled())
			logger_.debug(agentIDStr_ + ".doSearchNodes -> addNodesInCommunity");

		addNodesInCommunity(nodes);
		}

	} // doSearch


/**
 * Listener for node data, if the data wasn't cached.
 * Seems not to happen.
 */
private CommunityResponseListener nodeCommunityResponseListener =
	new CommunityResponseListener() {

	public void getResponse(CommunityResponse resp) {
		Collection nodes = (Collection) resp.getContent();
		if (nodes != null && !nodes.isEmpty()) {

			if (logger_.isDebugEnabled())
				logger_.debug( agentIDStr_ + ".CommunityResponseListener -> addNodesInCommunity");

			addNodesInCommunity(nodes);
			}
		}
	}; // nodeCommunityResponseListener


/**
 * Add each node in the collection to our data blob.
 * Also collect node CPU data.
 * 
 * @param nodes - Collection of nodes in community.
 */
private void addNodesInCommunity(Collection nodes) {

	if (metricsService_ == null) {
		return;
		}

	if (logger_.isDebugEnabled())
		logger_.debug(agentIDStr_ + ".addNodesInCommunity: nodes: " + nodes);

	Iterator nodeIt = nodes.iterator();
	while (nodeIt.hasNext()) {

		String nodeName = ((Entity) nodeIt.next()).getName();

		// If we already have node data for this node, don't get it again.
		//
		HostMetricData hmd = data_.getNodeData(nodeName);
		if (hmd == null || hmd.credibility_ < desiredMetricsCredibility_) {

			if (logger_.isDebugEnabled() && hmd != null)
				logger_.debug(agentIDStr_
						+ ".addNodesInCommunity: getNodeData("
						+ nodeName + ") again; hmd.credibility_ was: " + hmd.credibility_);

			// Try again (or the first time)
			//
			HostMetricData hostData = getHostDataForNode(nodeName);
			if (hostData == null) {
				unfoundNodes_.put(nodeName, nodeName);
				} 
			else {

				// if it was previously unknown, remove it
				String wasUnk = (String) unfoundNodes_.get(nodeName);
				if (wasUnk != null) {
					unfoundNodes_.remove(nodeName);
					if (logger_.isDebugEnabled())
						showUnfoundNodeList();
					}
				data_.setNodeData(nodeName, hostData);
				}
	
			} // try to get data again
		} //each node

	} // addNodesInCommunity


/**
 * Only called if debug enabled, so just do it.
 */
private void 
showUnfoundNodeList() {

	String unkList = "";
	Enumeration e = unfoundNodes_.keys();
	while (e.hasMoreElements()) {
		String nodeName = (String) e.nextElement();
		unkList += " " + nodeName;
		}

	// Only called if debug enabled.
	logger_.debug(agentIDStr_ + "Unfound nodes now:" + unkList);

	}


/**
 * Find the host the given node is on.
 * This is called during data collection.
 * @param nodeName
 */
private HostMetricData 
getHostDataForNode(String nodeName) {

	if (logger_.isDebugEnabled())
		logger_.debug(agentIDStr_ + ".getHostDataForNode: getting host/node info for node " + nodeName);

	String basePath = "Node(" + nodeName + ")" + org.cougaar.core.qos.metrics.Constants.PATH_SEPR;
	Metric m;

	// use default values from this as, um, defaults.
	HostMetricData hostData = new HostMetricData();

	double jips = hostData.jips_;
	double cred = 0.0;
	m = metricsService_.getValue(basePath + "Jips");
	if (m != null) {
		jips = m.doubleValue();
		cred = m.getCredibility(); // Use the JIPS measurement's credibility
		}

	double totalMemory = hostData.totalMemory_;
	m = metricsService_.getValue(basePath + "TotalMemory");
	if (m != null) {
		totalMemory = m.doubleValue();
		}

	// 04Nov03 - bug #13313 - changed from "CPU_MeanTimeBetweenFailure"
	m = metricsService_.getValue(basePath + "MeanTimeBetweenFailure");
	double mtbf = 24 * 365; // hours/year
	if (m != null) {
		mtbf = m.doubleValue();
		}

	// unused:
	//	path = basePath + "EffectiveMJips";
	//	Metric m = metricsService_.getValue(path);
	//	double effectiveMJips = m.doubleValue();
	//
	//	path = basePath + "LoadAverage";
	//	m = metricsService_.getValue(path);
	//	double loadAverage = m.doubleValue();
	//
	//	path = basePath + "Count";
	//	m = metricsService_.getValue(path);
	//	double count = m.doubleValue();
	//
	//	path = basePath + "FreeMemory";
	//	m = metricsService_.getValue(path);
	//	double freeMemory = m.doubleValue();

	if (logger_.isDebugEnabled()) {

		// was
		// 		logger_.debug(agentIDStr_ + "            host= " + hostName);
		logger_.debug(agentIDStr_ + "            node= " + nodeName);
		logger_.debug(agentIDStr_ + "            Jips= " + jips);
		logger_.debug(agentIDStr_ + "            MTBF= " + mtbf);
		logger_.debug(agentIDStr_ + "     TotalMemory= " + totalMemory);
		logger_.debug(agentIDStr_ + "   [ credibility= " + cred + " ]");
		//		logger_.debug(agentIDStr_ + " (EffectiveMJips= " + effectiveMJips);
		//		logger_.debug(agentIDStr_ + "    (LoadAverage= " + loadAverage);
		//		logger_.debug(agentIDStr_ + "          (Count= " + count);
		//		logger_.debug(agentIDStr_ + "     (FreeMemory= " + freeMemory);
		}

	// These are the fields we actually ultimately use...
	hostData.credibility_ = cred;
	hostData.jips_ = jips;
	hostData.totalMemory_ = totalMemory;
	hostData.pFail_ = 1 / mtbf;

	// accum avgs, to use on unknown (metrics data failed) nodes
	// could remove this, just set some nominal value....
	if (hostData.credibility_ >= desiredMetricsCredibility_) {

		nNodesAveraged_++;
		unknownNodeCPU_ =
			newMeanFloat((float) hostData.jips_, unknownNodeCPU_, nNodesAveraged_);
		unknownNodeRAM_ =
			newMeanFloat((float) hostData.totalMemory_, unknownNodeRAM_, nNodesAveraged_);
		unknownNodePOF_ =
			newMeanFloat((float) hostData.pFail_, unknownNodePOF_, nNodesAveraged_);
		}

	return hostData;
	} // getHostDataForNode


/**
 * Update a running mean value.
 * 
 * @param newVal	new data point to incorporate into mean
 * @param oldMean	previous mean value, for previous newN-1 data points
 * @param newN		number of data points, including the new one
 * 
 * @return			the new mean value
 */
private float newMeanFloat(float newVal, float oldMean, int newN) {

	return (newVal + oldMean * (newN - 1)) / newN;
	}


/**
 * Find the node the given agent is on??
 * This is called during data collection.
 * @param nodeName
 */
private void getNodeDataForAgent(String agentName) {

	// If data for this guy already exists, done
	//
	String nodeName = (String)agentNodeTable_.get(agentName);
	if (nodeName != null)
		return;

	// Look up node in WP to get host name
	//
	AddressEntry ae = null;
	int wpTimeoutSec = 60;

	//	if (logger_.isDebugEnabled())
	//		logger_.debug(agentIDStr_ + 
	//				".getNodeDataForAgent: getting nodeagent info for agent " + agentName);

	try {

		// what's a good value? Use "-1" for no timeout) ???
		// 10 sec timed out 29aug03; 20 sec on 30aug. maybe it's not really timing out?
		//
		ae = whitePagesService_.get(agentName, "topology", wpTimeoutSec * 1000);
		} 
	catch (Exception e) {

		// probably a timeout, sez Todd
		ae = null;
		if (logger_.isDebugEnabled())
			logger_.debug(agentIDStr_ + ".getNodeDataForAgent: whitePagesService_.get exception: "
					+ e.getMessage());
		}

	if (ae == null) {

		// Not listed in the white pages?
		if (logger_.isInfoEnabled())
			logger_.info(agentIDStr_ + ".getNodeDataForAgent: " + agentName + 
							": WhitePagesService timeout after " + wpTimeoutSec + " seconds");

		// agent name will be missing from agentNodeTable_

		return;
		}

	// The URI looks like "node://HOST/NODE", so nodes is the "path" part.
	//
	nodeName = ae.getURI().getPath();
	nodeName = nodeName.substring(1, nodeName.length());
	// minus the leading "/"

	if (logger_.isDebugEnabled())
		logger_.debug(agentIDStr_ + ".getNodeDataForAgent: " + agentName + " is on node " + nodeName);

	agentNodeTable_.put(agentName, nodeName);

	return;
	} // getNodeDataForAgent


/**
 * Send a Cougaar event.
 * 
 * @param type
 * @param message
 */
private void sendCougaarEvent(String type, String message) {

	if (eventService_ != null) {
		if (eventService_.isEventEnabled()) {
			eventService_.event("[" + type + "] " + message);
			}
		}
	}


/**
 * Inner class for collecting Host data for later use.
 * If node metrics fail, we use default values; what should they be?
 * Metrics service numbers are doubles, that's why we're using doubles here.
 */
public class HostMetricData {

	protected double jips_ = 1.23E6;
	// funny number so we can notice it in data
	protected double pFail_ = 0.001;
	protected double totalMemory_ = 123E3;

	protected double credibility_ =
		org.cougaar.core.qos.metrics.Constants.NO_CREDIBILITY;

	//we don't use these so why cart 'em around?
	//	protected double effectiveMJips_ = 1.23;
	//	protected double loadAverage_ = 0.0;
	//	protected double count_ 	  = 1.0;
	//	protected double freeMemory_  = 123E3;
	} // class HostMetricData

} // EN4JPlugin
