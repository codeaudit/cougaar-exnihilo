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
import java.util.Iterator;


/**
 * Given a CougaarSociety object, visualize it in a nice HTML table.
 * @author robert.e.cranfill@boeing.com
 */
public class CSVisualizer 
extends org.cougaar.scalability.util.CougaarSociety {

// Levels for the visual indicators.
//.
public float   CPU_YELLOW_LEVEL = .75f;
public float   CPU_RED_LEVEL  = .95f;
public float   RAM_YELLOW_LEVEL = .75f;
public float   RAM_RED_LEVEL  = .95f;

public String   HTML_COLOR_ERROR   = "red";
public String   HTML_COLOR_WARNING = "yellow";
public String   HTML_COLOR_HAPPY   = "lime"; // "green" is too dark

private CougaarSociety cougaarSociety_;
private boolean isWithinLimits_;


/**
 * Create a viz of the given society.
 */
public 
CSVisualizer(CougaarSociety cs) {

    cougaarSociety_ = cs;
}


/**
 * Check to see that no node has more load on it than it can handle, and that all agent requirements pairs are met.
 * The real checking is done by the toHTML method, so we call it and toss the result.
 */
public boolean
isWithinLimits() {

    toHTML();
    return isWithinLimits_;
}


/**
 *  Return a spiffy HTML representation of the CougaarSociety.
 * Also check that the agents are within the limits of the node they're on.
 * We check:
 * <UL>
 * <LI>CPU requirements <= 100%
 * <LI>RAM requirements <= 100%
 * <LI>Each agent's Requirement must not disagree with the node's Attribute - IS THIS SUFFICIENT?
 * 		or must there actually be a matching Attribute?
 * </UL>
 */
public String
toHTML() {

    if (cougaarSociety_ == null) {
        return null;
    }

    StringBuffer buff = new StringBuffer();
    buff.append("<B>Node Utilization &amp; Requirements</B><BR>");                   
    buff.append("CPU: yellow &gt; " + (CPU_YELLOW_LEVEL*100) + "%, ");
    buff.append("red &gt; " + (CPU_RED_LEVEL*100) + "%.<BR>\n");
    buff.append("RAM: yellow &gt;  " + (RAM_YELLOW_LEVEL*100) + "%, ");
    buff.append("red &gt; " + (RAM_RED_LEVEL*100) + "%.<BR>\n");
    buff.append("<BR>\n");
    
    isWithinLimits_ = true; // for the isWithinLimits() usage

    Iterator iterN = cougaarSociety_.getNodes(); 
    while (iterN.hasNext()) {

        CougaarNode node = (CougaarNode)iterN.next();
        
        // July'03 - use float, not int
        float nodeCPU = Float.parseFloat(node.getAttribute(CougaarNode.CPU));
		float nodeRAM = Float.parseFloat(node.getAttribute(CougaarNode.MEMORY));
        
        // We'll have to divide the node CPU and RAM by sum of agent's reqmts.
        //
        float agentCPUtotal = 0;
        float agentRAMtotal = 0;
        StringBuffer agentRequiremnts = new StringBuffer();

        Iterator iterA = node.getAgents();
        while (iterA.hasNext()) {

            CougaarAgent agent = (CougaarAgent)iterA.next();
            
            // July'03 - use float, not int
            float agentCPU = Float.parseFloat(agent.getRequirement(CougaarAgent.CPU));
			float agentRAM = Float.parseFloat(agent.getRequirement(CougaarAgent.MEMORY));
            agentCPUtotal += agentCPU;
            agentRAMtotal += agentRAM;
 
            // Check all non-CPU/non-RAM/non-messaging requirements of each agent.
            //
            Iterator iterR = agent.getRequirementNames();
            boolean agentsFirstReq = true;  // only show agent name once
            while (iterR.hasNext()) {
                String reqName = (String)iterR.next();
                
                if (   !reqName.equals(CougaarAgent.CPU) 
                     & !reqName.equals(CougaarAgent.MEMORY) 
                     & !reqName.startsWith("BandwidthSent_")
                     & !reqName.startsWith("BandwidthReceived_")
                     ) {

                    String reqValue = agent.getRequirement(reqName);
                    agentRequiremnts.append("<TR>");
                    if (agentsFirstReq) {
                        agentRequiremnts.append("<TD>" + agent.getName() + "</TD>\n");
                    }
                    else {
                        agentRequiremnts.append("<TD></TD>\n");
                    }
                    agentsFirstReq = false;
                    agentRequiremnts.append("<TD>" + reqName + "</TD>\n");

                    // Prior to 07May03, we considered an absent attribute to be OK.
                    // That is, if agent has "OS=foo", the node is only a mismatch if it
                    // has "OS={something other than foo}", which would allow 
                    // a node to be OS-agnostic. 
                    // But I don't think that's the more useful way to look at it.
                    // So we'll be strict. Agent Req's *must* match the Node Attr.
                    //
					String reqValCellColor = HTML_COLOR_HAPPY;

					String nodeAttr = node.getAttribute(reqName);
                    if (nodeAttr == null) {
						reqValCellColor = HTML_COLOR_ERROR;
						isWithinLimits_ = false;
                    }
                    else {
                        if (nodeAttr.equals(reqValue)) {
                        	; // do nothing, we're happy
                        }
                        else {
							reqValCellColor = HTML_COLOR_ERROR;
							isWithinLimits_ = false;           
                        }
                    }
                    agentRequiremnts.append("<TD BGCOLOR=\"" + 
                                            reqValCellColor + "\">" +
                                            reqValue + "</TD>\n");
                    agentRequiremnts.append("</TR>\n");      
                }
            }
        }
        
        buff.append("<TABLE COLS=\"2\" BGCOLOR=\"#DDDDDD\" CELLPADDING=\"2\"");
        buff.append(" CELLSPACING=\"3\" BORDER=\"1\">\n");
        buff.append("<CAPTION ALIGN=TOP><B>" + node.getName() + "</CAPTION>\n");
        buff.append("<TR><TH>CPU</TH><TH>RAM</TH></TR>");
        buff.append("<TR>");

        // CPU 
        buff.append("<TD");
        if (agentCPUtotal > nodeCPU * CPU_YELLOW_LEVEL) {
            buff.append(" BGCOLOR=\"" + 
                ((agentCPUtotal > nodeCPU * CPU_RED_LEVEL)?HTML_COLOR_ERROR:HTML_COLOR_WARNING) + "\"");
        }
        else {
            buff.append(" BGCOLOR=\"" + HTML_COLOR_HAPPY + "\""); 
        }
        buff.append(">" + agentCPUtotal + "/" + nodeCPU + 
                    " (" + percentOf(agentCPUtotal, nodeCPU) + "%)</TD>");
        

        // RAM
        buff.append("<TD");
        if (agentRAMtotal > nodeRAM * RAM_YELLOW_LEVEL) {
            buff.append(" BGCOLOR=\"" + 
                ((agentRAMtotal > nodeRAM * RAM_RED_LEVEL)?HTML_COLOR_ERROR:HTML_COLOR_WARNING) + "\"");
        }
        else {
            buff.append(" BGCOLOR=\"" + HTML_COLOR_HAPPY + "\""); 
        }
        buff.append(">" + agentRAMtotal + "/" + nodeRAM +
                    " (" + percentOf(agentRAMtotal, nodeRAM) + "%)</TD>");
        

        buff.append("</TR>\n");
        buff.append("</TABLE>\n");  // end node info
        
        // If there are requiremnts to display, this string
        // is the contents of a table.
        //
        if (agentRequiremnts.length() > 0) {
            
            buff.append("<TABLE COLS=\"3\" BGCOLOR=\"#DDDDDD\" CELLPADDING=\"2\"");
            buff.append(" CELLSPACING=\"3\" BORDER=\"1\">\n");
            buff.append("<TR><TH>Agent</TH><TH>Requirement</TH><TH>Req. value</TH></TR>");
            buff.append(agentRequiremnts);
            buff.append("</TABLE>\n");
        }
     
        buff.append("<BR>\n");  // blank line after node

        if (agentCPUtotal>nodeCPU | agentRAMtotal>nodeRAM) {
            isWithinLimits_ = false;
        }

    }
    
    return buff.toString();
}

/**
 * Return the percent that represents the ratio of 'part' to 'whole'.
 * 
 * @param part
 * @param whole
 * @return integer percentage representing the ratio of 'part' to 'whole'
 */
private int
percentOf(float part, float whole) {
    return (int)((float)part/(float)whole * 100f);
}

} // CSVisualizer
