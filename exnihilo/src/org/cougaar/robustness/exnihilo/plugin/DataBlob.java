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

import org.cougaar.core.service.LoggingService;
import java.util.Hashtable;


/**
 * Being an encapsulation of a buncha arrays and hashtables
 * keeping hold of the agent/node/message info.
 * 
 * @version 28Sept04 - Increased MAX_MESSAGE_TRAFFIC_TABLE (Bug 13690)
 * @author robert.e.cranfill@boeing.com
 *
 */
public class DataBlob {

public static final int	MAX_MESSAGE_TRAFFIC_TABLE = 800; //  (Bug 13690)28Sept04 - was 650, needed 651!? ; // 26May04 - was 600; 584 actual agents was getting too close

protected double[][] 	messageTraffic_;	// [from][to]
protected String[]	 	agents_;			// agent names
protected int			agentCount_ = 0;
protected Hashtable 	trafficIndices_;	// key: String: agent name; value: Integer: agent index in messageTraffic_ and agents_
protected Hashtable 	cpuLoadJIPS_;		// key: String: agent name; value: Double: CPU load
protected Hashtable 	ramUsageK_;			// key: String: agent name; value: Double: RAM usage
protected Hashtable 	nodes_;				// key: String: node name;  value: HostMetricData: host data
// protected Hashtable		nodeForAgents_;

private LoggingService 	logger_ = LoggingService.NULL;

private boolean		doneDidWarnTrafficTableFull_ = false;


/**
 * Constructor.
 *
 */
public DataBlob() {

	messageTraffic_ = new double[MAX_MESSAGE_TRAFFIC_TABLE][MAX_MESSAGE_TRAFFIC_TABLE];
	agents_			= new String[MAX_MESSAGE_TRAFFIC_TABLE];
	
	trafficIndices_ = new Hashtable(EN4JPlugin.AGENT_ORIENTED_HASH_SIZE);
	cpuLoadJIPS_ 	= new Hashtable(EN4JPlugin.AGENT_ORIENTED_HASH_SIZE);
	ramUsageK_ 		= new Hashtable(EN4JPlugin.AGENT_ORIENTED_HASH_SIZE);
	
	nodes_			= new Hashtable(EN4JPlugin.NODE_ORIENTED_HASH_SIZE);
// 	nodeForAgents_	= new Hashtable(EN4JPlugin.NODE_ORIENTED_HASH_SIZE);

	} // constructor


public void
setLogger(LoggingService logger) {
	logger_ = logger;
	}

//
//public void
//setAgentNode(String agentName, String nodeName) {
//	nodeForAgents_.put(agentName, nodeName);
//	}
//
//
//public String
//getAgentNode(String agentName) {
//	return (String)nodeForAgents_.get(agentName);
//	}
//


public String []
getAgents() {
	return agents_;
	}


/**
 * Get the Hashtable of HostMetricData node info.
 *
 * @return ArrayList of String node names.
 */
public Hashtable
getNodes() {

	return nodes_;
	}

/**
 * Get the HostMetricData node info.
 *
 * @return ArrayList of String node names.
 */
public EN4JPlugin.HostMetricData
getNodeData(String nodeName) {

	return (EN4JPlugin.HostMetricData)nodes_.get(nodeName);
	}

/**
 * Get the HostMetricData node info.
 *
 * @return ArrayList of String node names.
 */
public void
setNodeData(String nodeName, EN4JPlugin.HostMetricData nodeData) {

	nodes_.put(nodeName, nodeData);
	}


public void
setCPULoadJIPS(String agentName, double load) {
	
//	if (logger_.isDebugEnabled()) {
//		logger_.debug("DataBlob.setCPULoad: agent " + agentName + "=" + load);
//		}

	cpuLoadJIPS_.put(agentName, new Double(load));
}


public double
getCPULoadJIPS(String agentName) {

	Double load = (Double)cpuLoadJIPS_.get(agentName);
	if (load == null) {
		if (logger_.isWarnEnabled()) {
			logger_.warn("DataBlob.getCPULoadJIPS: agent " + agentName + " not found!");
			}
		return 0;	// ???
	}
	return load.doubleValue();
}


public void
setRAMUsage(String agentName, double usage) {
	
	ramUsageK_.put(agentName, new Double(usage));
}


public double
getRAMUsageK(String agentName) {

	Double usage = (Double)ramUsageK_.get(agentName);
	if (usage == null) {
		if (logger_.isWarnEnabled()) {
			logger_.warn("DataBlob.getRAMUsageK: agent " + agentName + " not found!");
			}
		return 0;	// ???
		}
	return usage.doubleValue();
	}


/**
 * Setter for message traffic info. 
 * If either agent doesn't exist, logs error as noted.
 * 
 * @param fromAgent :name of 'from' agent
 * @param toAgent: name of 'to' agent
 * @param traffic: traffic metric
 */
public void
setMessageTraffic(String fromAgent, String toAgent, double traffic) {

	int iFrom = getAgentTrafficIndex(fromAgent, true);
	int iTo = getAgentTrafficIndex(toAgent, true);
	if (iFrom == -1 || iTo == -1) {
		return;	// getAgentIndex will have logged specific error
		}
	messageTraffic_[iFrom][iTo] = traffic;
	}


/**
 * Accessor for message traffic data.
 * 
 * @param fromAgent: name of 'from' agent
 * @param toAgent: name of 'to' agent
 * @return traffic metric or -1 if error (either agent not found)
 */
public double
getMessageTraffic(String fromAgent, String toAgent) {

	int iFrom = getAgentTrafficIndex(fromAgent, false);
	int iTo   = getAgentTrafficIndex(toAgent, false);
	if (iFrom == -1 || iTo == -1) {
		return -1;	// getAgentIndex will have logged specific error
		}
	return messageTraffic_[iFrom][iTo];
	}


/**
 * Look up the named agent, return its index in the traffic matrix.
 * If given agent doesn't exist in table and 'create' flag is set, create an entry.
 * 
 * @param agentName	agent to look up.
 * @param create	create it if it doesn't exist?
 * @return	index, or -1 if not found
 */
public int
getAgentTrafficIndex(String agentName, boolean create) {

	Integer index = (Integer)trafficIndices_.get(agentName);
	if (index == null) {

		if (create == false) {
			if (logger_.isErrorEnabled()) {
				logger_.error("DataBlob.getAgentIndex: agent " + agentName + " not found!");
				}
			return -1;
			}

		if (agentCount_ == MAX_MESSAGE_TRAFFIC_TABLE) {
			
			if (doneDidWarnTrafficTableFull_ == false) {
				if (logger_.isErrorEnabled()) {
					logger_.error("DataBlob.getAgentIndex: message traffic table is full! (size " 
									+ MAX_MESSAGE_TRAFFIC_TABLE + ")");
					}
				doneDidWarnTrafficTableFull_ = true;
				}
			return -1;
			}
		index = new Integer(agentCount_);
		trafficIndices_.put(agentName, index);

// logger_.debug("DataBlob.getAgentIndex: PUT " +  agentName + " @" + messageTrafficUsed_);

		agents_[agentCount_] = agentName;
		agentCount_++;
		}
	return index.intValue();
	}


/**
 * Copy this data blob so others can't modify our data.
 * @return a copy of this object; Hashtables are shallow copy only.
 */
public DataBlob
copy() {

	DataBlob newblob = new DataBlob();

	for (int i=0; i<MAX_MESSAGE_TRAFFIC_TABLE; i++) {
		for (int j=0; j<MAX_MESSAGE_TRAFFIC_TABLE; j++) {
			newblob.messageTraffic_ [i][j] = this.messageTraffic_[i][j];
			}
		}

	// Hashtable and ArrayList .clone() is shallow, 
	// but that's OK cuz the keys and values are Strings, and so are immutable.
	//
	newblob.trafficIndices_	= (Hashtable)this.trafficIndices_.clone();
	newblob.cpuLoadJIPS_ 	= (Hashtable)this.cpuLoadJIPS_.clone();
	newblob.ramUsageK_ 		= (Hashtable)this.ramUsageK_.clone();
		
	newblob.nodes_ 			= (Hashtable)this.nodes_.clone();	
	newblob.agents_ 		= (String[])this.agents_.clone();	

	return newblob;
	} // copy


/**
 * Pretty print ourself.
 */
public String
toString() {

	return "DataBlob: "	+ agentCount_ + " agents, " + nodes_.size() + " nodes.";
	} // toString


} // DataBlob
