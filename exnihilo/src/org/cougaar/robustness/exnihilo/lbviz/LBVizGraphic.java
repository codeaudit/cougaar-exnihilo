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

package org.cougaar.robustness.exnihilo.lbviz;

import org.cougaar.scalability.util.*;
import org.cougaar.robustness.exnihilo.*;

/* the following are from jai_core.jar and jai_codec.jar,
 * part of Sun's Java Advanced Imaging package;
 * see http://java.sun.com/products/java-media/jai
*/
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.ImageCodec;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;


/**
 * LBVizGraphic - Create a 'before and after load balancing' diagram of the Cougaar Society.
 * Also can do a 'sanity check' of before and after, to make sure no constraints were violated.
 * 
 * Used by LBVizServlet.
 * 
 * @author robert.e.cranfill@boeing.com
 * @version 1.0
 */
public class LBVizGraphic {

private static boolean 	spew_ = false;	// verbose output to stdout?

public boolean	doMessageTraffic_ = false;
public boolean	USE_JPEG_ENCODING = true;
public float	JPEG_COMPRESSION_LEVEL = 1.0f;

public final static int 	IMAGE_SIZE = 800;
private final static double	GREAT_CIRCLE_SIZE_FACTOR = 0.7;			// fraction of IMAGE_SIZE
private final static int	NODE_RADIUS = IMAGE_SIZE/10; // was 64;						// the CPU pie radius
private final static double	NODE_RADIUS_2 = NODE_RADIUS * 0.6;		// the RAM pie radius
private final static int	AGENT_RADIUS = NODE_RADIUS/8;
protected final static int	THRESHOLD_YELLOW = (int)(360*.85);	// thresholds are in degrees!
protected final static int	THRESHOLD_RED = 360;
protected final static int	MAX_MESSAGE_LINE_WIDTH = 16;

private final Color			colorPageBG_ = new Color(0xDD, 0xDD, 0xDD);//  #FAEBD7 	AntiqueWhite
private final Color			colorUnusedNode_ = new Color(0x88, 0x88, 0x88);
private final Font 			fontNode_ = new Font("ARIAL", Font.BOLD, 14);
private final Font 			fontMessaging_ = new Font("COURIER NEW", Font.PLAIN, 12);

// key: String: (nodeName1 + "-" + nodeName2); value: Float
private Hashtable nodeToNodeMessagingBefore_ = new Hashtable();
private Hashtable nodeToNodeMessagingAfter_  = new Hashtable();
private float maxMessages_ = 0;


// test data
private TreeMap		nodes_;	// of NodeData; use TreeMap so nodes are sorted alphabetically.

private ArrayList	sanityViolations_ = new ArrayList();
// a very simply sanity check
int totalAgentsBefore_ = 0, totalAgentsAfter_ = 0;


private String title_ = null;
private String identifierBefore_ = "", identifierAfter_ = "";
private int	titleX_, titleY_;


/**
 * Create a graphic; will the setProperty stuff work in UL???
 *
 */
public
LBVizGraphic(boolean doMessageTraffic) {


//	String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
//	for (int i=0; i<names.length; i++)
//		System.out.println(i + " " + names[i]);
	
	doMessageTraffic_ = doMessageTraffic;

	if (spew_) {
		SystemDesign.logDebug("");
		SystemDesign.logDebug("LBVizGraphic instantiated: " + this);
		}

	// To fix the "can't run headless" bug; this should perhaps be in the servlet?
	//
	String headless = System.getProperty("java.awt.headless");
	if(spew_)SystemDesign.logDebug("Headless was " + headless);
	if ("true".equals(headless) == false) {
		System.setProperty("java.awt.headless", "true");
		headless = System.getProperty("java.awt.headless");
		if(spew_)SystemDesign.logDebug("Headless now is " + headless);
		}
	}


/**
 * Load the two societies into the 'before' and 'after' data structures.
 * @param csBefore - The 'before' society to display.
 * @param csAfter - Gee, guess!
 */
public ArrayList
loadBeforeAndAfterData(CougaarSociety csBefore, CougaarSociety csAfter) {

	// the data we're building
	//
	nodes_ = new TreeMap();
	parseCougaarSocData(csBefore, true);
	parseCougaarSocData(csAfter, false);
	
	return sanityViolations_;
	}


/**
 * Given a CougaarSociety object, load our internal data structures so we can show it.
 * This can either be a pre- or post-loadbalancing dataset.
 * 
 * @param cs			- the cougaarsociety to datafy
 * @param isBeforeData	- true iff this is pre-loadbalancing data
 */
private void
parseCougaarSocData(CougaarSociety cs, boolean isBeforeData) {

	Hashtable nodeAgentIsOn = new Hashtable();

	Iterator nodes = cs.getNodes();
	while (nodes.hasNext()) {

		CougaarNode node = (CougaarNode)nodes.next();
		String nodeName = node.getName();

		NodeData nd = (NodeData)nodes_.get(nodeName);
		if (nd == null) {
			nd = new NodeData(nodeName);
			nodes_.put(nodeName, nd);
			}
		if (isBeforeData) {
			nd.existed = true;
			}
		else {
			nd.exists = true;
			}

		double nodeCPU = parseDouble(node.getAttribute(CougaarNode.CPU), 
									 "Node " + nodeName + "'s CPU value");

		double nodeRAM = parseDouble(node.getAttribute(CougaarNode.MEMORY), 
									 "Node " + nodeName + "'s MEMORY value");

		// count the agents, sum thier CPU and RAM req.
		//
		int agentCount = 0;
		double agentCPU = 0, agentRAM = 0;
		Iterator agents = node.getAgents();
		while (agents.hasNext()) {

			CougaarAgent agent = (CougaarAgent)agents.next();
			String agentName = agent.getName();

			agentCount++;
			agentCPU += parseDouble(agent.getRequirement(CougaarAgent.CPU), 
										 "Agent " + agentName + "'s CPU value");
			agentRAM += parseDouble(agent.getRequirement(CougaarAgent.MEMORY), 
										 "Agent " + agentName + "'s MEMORY value");

			// show message traffic?
			if (doMessageTraffic_) {

				nodeAgentIsOn.put(agentName, nodeName);
				} // doMessageTraffic_

			} // for each agent



		if(spew_)SystemDesign.logDebug("nodeName: " + nodeName);

		if (isBeforeData) {
			nd.agentCountBefore_ = agentCount;
			nd.CPUbefore_ = agentCPU/nodeCPU;
			nd.RAMbefore_ = agentRAM/nodeRAM;
			
			if(spew_)SystemDesign.logDebug("CPUbefore_: " + nd.CPUbefore_);
			if(spew_)SystemDesign.logDebug("RAMbefore_: " + nd.RAMbefore_);
			totalAgentsBefore_ += agentCount;
			
			identifierBefore_ = cs.getIdentifier();
			if (identifierBefore_ == null) {
			    identifierBefore_ = "?";
			    System.out.println(" **** YES ID IS NULL! *** -----------------------------");
				}
			}
		else {
			nd.agentCountAfter_ = agentCount;
			nd.CPUafter_ = agentCPU/nodeCPU;
			nd.RAMafter_ = agentRAM/nodeRAM;
			
			if(spew_)SystemDesign.logDebug("CPUafter_: " + nd.CPUafter_);
			if(spew_)SystemDesign.logDebug("RAMafter_: " + nd.RAMafter_);
			totalAgentsAfter_ += agentCount;

			identifierAfter_ = cs.getIdentifier();
			}
		} // nodes


	// second k for messaging - build messagingTable data
	//
	if (doMessageTraffic_) {
		
		Hashtable messagingTable = nodeToNodeMessagingAfter_;
		if (isBeforeData)
			messagingTable = nodeToNodeMessagingBefore_;

		nodes = cs.getNodes();
		while (nodes.hasNext()) {

			CougaarNode node = (CougaarNode)nodes.next();
			String nodeName = node.getName();

			Iterator agents = node.getAgents();
			while (agents.hasNext()) {

				CougaarAgent agent = (CougaarAgent)agents.next();
				String agentName = agent.getName();

				Iterator iReq = agent.getRequirementNames();
				while (iReq.hasNext()) {
					String reqName = (String)iReq.next();
					if (reqName.startsWith("BandwidthSent_")) {
						try {
							String destAgent = reqName.substring("BandwidthSent_".length());
							String reqSVal = agent.getRequirement(reqName);
							float reqVal = Float.parseFloat(reqSVal);

							String destNode = (String)nodeAgentIsOn.get(destAgent);
							if (destNode == null) {
//								System.out.println("Can't find node for agent " + destAgent);
								continue;
								}

//							System.out.println("BandwidthSent_ to " + destAgent + ": " + reqVal 
//													+ " on node " + destNode);

							if (nodeName.equals(destNode) == false) {	// ignore same-node messages
								
								// we want "A->B" and "B->A" to accumulate,
								// so sort and store together
								String key = nodeName + "->" + destNode;
								if (nodeName.compareTo(destNode) > 0)
									key = destNode + "->" + nodeName;
								Float messages = (Float)messagingTable.get(key);
								if (messages == null) {
									messages = new Float(0);
									}
								messages = new Float(messages.floatValue() + reqVal);
								
								// calc maxMessages_ only based on 'before' data
								if (isBeforeData) {
									maxMessages_ = (float)Math.max(maxMessages_, messages.floatValue());
									}
								messagingTable.put(key, messages);
								}
							}
						catch (NumberFormatException nfe) {
							}
						}
					}
				} // agents
			} // nodes pass two
			
		SystemDesign.logDebug("messagingTable (" + isBeforeData + "): " + messagingTable); 
		SystemDesign.logDebug("maxMessages: " + maxMessages_); 
		if (maxMessages_ == 0) {
		    maxMessages_ = 100f;	// when there's no data, this makes things look better
			}

		} // doMessageTraffic_

	
	if (isBeforeData) {
		SystemDesign.logDebug("totalAgentsBefore: " + totalAgentsBefore_);
		}
	else {
		SystemDesign.logDebug("totalAgentsAfter: " + totalAgentsAfter_); 
		}

	// this is dumb
	if (isBeforeData == false) { // check after
		if (totalAgentsBefore_ != totalAgentsAfter_) {
			sanityViolations_.add("1 - Agents before LB: " + 
				totalAgentsBefore_ + ", totalAgentsAfter: " + totalAgentsAfter_);
			}
		}
	
	} // parseCougaarSocData


/**
 * Util method - silly?
 * @param putativeDoubleString - what we're trying to parse
 * @param whatIsIt - what to complain about if exception
 * @return - the value, or zero if error (we'll have to use *some* value, so zero's good)
 */
private double
parseDouble(String putativeDoubleString, String whatIsIt) {
	
	double result = Double.NaN;
	try {
		result = Double.parseDouble(putativeDoubleString);
		}
	catch (NumberFormatException nfe) {
		if(spew_)SystemDesign.logDebug("Error parsing " + whatIsIt + "; setting to zero." + 
							" (was '" + putativeDoubleString + "')");
		result = 0; // what else can we do?
		}
	return result;
	}


/**
 * Draw a pie, pre-filling with background color.
 * 
 * @param g - the Graphics2D to draw into
 * @param val - the value for this wedge
 * @param dia - diameter of circle
 */

private void
doPie(Graphics2D g, double arc, double x, double y, double dia) {

	if(spew_)SystemDesign.logDebug("draw node at " + x + ", " + y + " @ " + arc);

	// The whole deal
	//
	Arc2D.Double pieBG = new Arc2D.Double(
										x-dia/2, y-dia/2, dia, dia, 
										0, 360, Arc2D.PIE);
	g.setColor(colorUnusedNode_);
	g.fill(pieBG);

	// The wedge
	//
	Arc2D.Double nodePie = new Arc2D.Double(
										x-dia/2, y-dia/2, dia, dia, 
										90, -arc, Arc2D.PIE);

	Color pieColor = new Color(0, 200, 0);	// dark green							
	if (arc > THRESHOLD_YELLOW)
		pieColor = Color.yellow;
	if (arc > THRESHOLD_RED)
		pieColor = Color.red;

	g.setColor(pieColor);
	g.fill(nodePie);

	}


/**
 * Create a bitmapped display of the current CougaarSociety.
 * 
 * @param stream - Output stream we will, uh, stream the image out to.
 * @returnString - MIME type encoded, or null for failure
 * @throws IOException
 */
public String 
createImage(OutputStream stream) 
throws IOException {

	if(spew_)SystemDesign.logDebug("LBVizGraphic.createImage()");

	if (nodes_ == null) {
		if(spew_)SystemDesign.logDebug("No data loaded?");
		return null;
		}

//	this.IMAGE_SIZE = 640;
//	if (nodes_.size() > 10) {
//		this.IMAGE_SIZE *= 1.2;
//		}
//	if (nodes_.size() > 20) {
//		this.IMAGE_SIZE *= 1.2;
//		}

	int paddedWidth = (int)(IMAGE_SIZE*1.2);
	BufferedImage bi = new BufferedImage(paddedWidth, IMAGE_SIZE, BufferedImage.TYPE_BYTE_INDEXED);
	Graphics2D graphics = bi.createGraphics();

	Rectangle frameRect = new Rectangle(paddedWidth, IMAGE_SIZE);
	graphics.setColor(colorPageBG_);
	graphics.fill(frameRect);

	double NODE_AFTER_OFFSET = 3*NODE_RADIUS/5;

// Zinky doesn't like the big arc behind it all. Neiter do I, now.
//	
//	double size = IMAGE_SIZE*GREAT_CIRCLE_SIZE_FACTOR;
//	Arc2D.Double greatArc = new Arc2D.Double(
//				(IMAGE_SIZE-size)/2 + NODE_AFTER_OFFSET/2, (IMAGE_SIZE-size)/2 + NODE_AFTER_OFFSET/2,
//				size, size, 0, 360, 1);
////	greatArc.setFrame((IMAGE_SIZE-size)/2, (IMAGE_SIZE-size)/2, size, size);
////	greatArc.setAngleStart(0);
////	greatArc.setAngleExtent(360);
//	graphics.setColor(Color.cyan);
//	graphics.draw(greatArc);

	// Show what the hell this all means.
	//
	drawKey(graphics);

	double x, y;
	double center = IMAGE_SIZE/2;


	// pass 1 is before, 2 is after
	//
	for (int pass=1; pass<=2; pass++) {

		// Offset the "after" diagrams by 3/5 diameter.
		//
		if (pass == 2) {
			center += NODE_AFTER_OFFSET;
			}

		// draw the messaging lines first, so they're behind everything?
		Iterator iterNode = nodes_.values().iterator();
		int nNodes = nodes_.size();
		int i = -1;
		if (doMessageTraffic_) {

		while (iterNode.hasNext()) {
			NodeData nd = (NodeData)iterNode.next();
			i++;

			// Messaging
			// nodes go round the big circle starting at the 12 o'clock position.
			//
			double theta = 360 * i / nNodes;
			x = center + (IMAGE_SIZE*GREAT_CIRCLE_SIZE_FACTOR/2)*Math.sin(Math.toRadians(theta));
			y = center - (IMAGE_SIZE*GREAT_CIRCLE_SIZE_FACTOR/2)*Math.cos(Math.toRadians(theta));

			Iterator iterNode2 = nodes_.values().iterator();
			int i2 = -1;
			while (iterNode2.hasNext()) {
				i2++;
				NodeData nd2 = (NodeData)iterNode2.next();
				if (i == i2)
					continue;	// don't bother with A->A msgs

				String key = nd.nodeName_ + "->" + nd2.nodeName_;
				if (nd.nodeName_.compareTo(nd2.nodeName_) > 0)
					key = nd2.nodeName_ + "->" + nd.nodeName_;

				Hashtable msgTable = nodeToNodeMessagingAfter_;
				if (pass==1)
					msgTable = nodeToNodeMessagingBefore_;

				Float msgTraffic = (Float)msgTable.get(key);
				if (msgTraffic == null) {
//					continue;					// hide nothings?
					msgTraffic = new Float(0);	// do show zeros
					}
				if (msgTraffic.floatValue() == -1)
					continue;
				// kill entry so we don't plot it twice (coming and going)
				msgTable.put(key, new Float(-1));
				

				double theta2 = 360 * i2 / nNodes;
				double x2 = center + (IMAGE_SIZE*GREAT_CIRCLE_SIZE_FACTOR/2)*Math.sin(Math.toRadians(theta2));
				double y2 = center - (IMAGE_SIZE*GREAT_CIRCLE_SIZE_FACTOR/2)*Math.cos(Math.toRadians(theta2));
//				System.out.println(key2 + "=  " + msgTraffic + " @ " + x2 + ", " + y2);

				// Normalize to some max width
				int lineWidth = (int)(MAX_MESSAGE_LINE_WIDTH * msgTraffic.floatValue()/maxMessages_);
				Stroke oldStroke = graphics.getStroke();
				graphics.setStroke(new BasicStroke(lineWidth));

				// Color 'before' and 'after' differently...
				if (pass==1)
					graphics.setColor(Color.orange);
				else
					graphics.setColor(Color.blue);

				// ...but all zeros are colored the same.
				if (msgTraffic.floatValue() == 0) {
					float [] dash = {10,10};
					graphics.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10f, dash, 0f));
					graphics.setColor(Color.darkGray);					
					}

				graphics.drawLine((int)x, (int)y, (int)x2, (int)y2);
				graphics.setStroke(oldStroke);

				// show messaging values
				double x3 = (x+x2)/2;
				double y3 = (y+y2)/2;	
				// anti center-collision?
				if (true) {
					}
				graphics.setColor(Color.gray);
				graphics.setFont(fontMessaging_);
				
				float valueToDraw = msgTraffic.floatValue();
				// scale to 100?
				valueToDraw = 100 * valueToDraw / maxMessages_;
				String valueStr = "" + valueToDraw;
				graphics.drawString(valueStr.substring(0,Math.min(4,valueStr.length())), (int)x3, (int)y3);

//				System.out.println(pass + " plotted " + msgTraffic + " @ " + x3 + ", " + y3);

				} // iterNode2
			} // for nodes
		} // doMessageTraffic_

		iterNode = nodes_.values().iterator();
		i = -1;
		while (iterNode.hasNext()) {

			NodeData nd = (NodeData)iterNode.next();
			i++;

			// Messaging
			// nodes go round the big circle starting at the 12 o'clock position.
			//
			double theta = 360 * i / nNodes;
			x = center + (IMAGE_SIZE*GREAT_CIRCLE_SIZE_FACTOR/2)*Math.sin(Math.toRadians(theta));
			y = center - (IMAGE_SIZE*GREAT_CIRCLE_SIZE_FACTOR/2)*Math.cos(Math.toRadians(theta));


			// Only do label on second pass; must do it before we exit for non-existant nodes.
			//
			if (pass == 2) {
				graphics.setColor(Color.black);
				graphics.setFont(fontNode_);
				graphics.drawString(nd.nodeName_, (int)x, (int)y+(2*NODE_RADIUS/3));
				}

			// the entire node
			//
			Arc2D.Double nodeCirc = new Arc2D.Double(x-NODE_RADIUS/2, y-NODE_RADIUS/2, 
											NODE_RADIUS, NODE_RADIUS, 0, 360, 1);

			if (pass == 1 && !nd.existed) {
				if(spew_)SystemDesign.logDebug("skipping node that didn't exist! " + nd.nodeName_);
				graphics.setColor(new Color(0xAA, 0xAA, 0xAA));
				graphics.fill(nodeCirc);
				continue;	// skip this node
				}
			if (pass == 2 && !nd.exists) {
				if(spew_)SystemDesign.logDebug("skipping node that doesn't exist! " + nd.nodeName_);
				graphics.setColor(new Color(0xAA, 0xAA, 0xAA));
				graphics.fill(nodeCirc);
				continue;	// skip this node
				}

			// draw outer pies first, so they don't step on the inner.
			//
			if (pass == 1) {
				doPie(graphics, 360*nd.RAMbefore_, x, y, NODE_RADIUS);
				doPie(graphics, 360*nd.CPUbefore_, x, y, NODE_RADIUS_2);
				}
			else {
				doPie(graphics, 360*nd.RAMafter_, x, y, NODE_RADIUS);
				doPie(graphics, 360*nd.CPUafter_, x, y, NODE_RADIUS_2);
				}

			// ring around inner pie for contrast
			// why doesn't this match up with the inner circle (sometimes it doesn't) ?
			//
			Arc2D.Double perim = new Arc2D.Double(x-NODE_RADIUS_2/2-1, y-NODE_RADIUS_2/2-1,
										NODE_RADIUS_2+1, NODE_RADIUS_2+1, 0, 360, 1);
			graphics.setColor(Color.black);
			graphics.draw(perim);

			// put the agents in the upper-left corner of the node
			double aX = x - .3*NODE_RADIUS;
			double aY = y - .3*NODE_RADIUS;

			int agentsPerRow = 10;
			int rowX = -AGENT_RADIUS, rowY = -AGENT_RADIUS;

			// draw the agents
			int ac = nd.agentCountBefore_;
			if (pass == 2) {
				ac = nd.agentCountAfter_;
				}
			for (int j=0; j<ac; j++) {

				rowX += AGENT_RADIUS;
				if (j % agentsPerRow == 0) {
					rowX = 0;
					rowY += AGENT_RADIUS;
					}
				drawAgent(graphics, aX+rowX, aY+rowY);
				} // agents
			} // nodes
		} // pass 1 or 2


	if (title_ != null) {
		graphics.setColor(Color.black);
		graphics.setFont(fontNode_);
		graphics.drawString(title_, titleX_, titleY_);

		graphics.drawString(identifierBefore_, titleX_+8, titleY_+16);	// 02Sept04 - fixed NPE in identifierBefore_ when using EN data
		graphics.drawString(identifierAfter_,  titleX_+8, titleY_+32);
		}


	// Encode the image.
	//
	String result = null;
	try {

		// JPEG used to look ugly, as though the quality param wasn't working.
		// Looks OK now, though. So you can use either.
		//
		if (USE_JPEG_ENCODING) {
	
			JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(stream);
			com.sun.image.codec.jpeg.JPEGEncodeParam jep = encoder.getDefaultJPEGEncodeParam(bi); // encoder.get or JPEGCodec?
			jep.setQuality(JPEG_COMPRESSION_LEVEL, false);
			encoder.encode(bi, jep);
			result = "image/jpg";
			}
		else {

			// PNG is not lossy, so it looks much better 
			// (it's also bigger, but that's not such a concern here, right?).
			//
			ImageEncoder enc = ImageCodec.createImageEncoder("PNG", stream, null);
			enc.encode(bi);
			result = "image/png";
			}
		}
	catch (Exception e){
		if(spew_)SystemDesign.logDebug("Image encoding failure: " + e.getMessage());
		}
	return result;
	} // createImage


/**
 * Just for testing, make some data.
 * @param nNodes Number of nodes to populate.
 */
protected void
makeTestData(int nNodes) {

	nodes_ = new TreeMap();
	int totalAgents = 0;

	// before
	for (int i=0; i<nNodes; i++) {
		NodeData nd = new NodeData("Node_" + i);
		nd.agentCountBefore_ = i*3;
		totalAgents += nd.agentCountBefore_;	// so i don't have to do the math...
		nd.CPUbefore_ = i/7.0;
		nd.RAMbefore_ = Math.pow(nd.CPUbefore_, .5);		
		nodes_.put(nd.nodeName_, nd);

		nd.existed = true;
		nd.exists = true;

		// 2 is a new node
		if (i == 2) {
			nd.existed = false;
			}

		// 3 went away
		if (i == 3) {
			nd.exists = false;
			}

		}
	if(spew_)SystemDesign.logDebug("totalAgents: " + totalAgents);

	// after
	Iterator itNodes = nodes_.values().iterator();
	while (itNodes.hasNext()) {

		NodeData nd = (NodeData)itNodes.next();
		nd.agentCountAfter_ = totalAgents / nNodes;
		nd.CPUafter_ = .3;
		nd.RAMafter_ = .7;
		}

	} // makeTestData


/**
* Test entry point - outputs an image file to indicated location.
*/
public static void 
main(String[] args) {

	if (args.length < 1) {
		if(spew_)SystemDesign.logDebug("to test, use:");		
		if(spew_)SystemDesign.logDebug("  LBVizGraphic {outputfilename}");
		return;	
		}

	String filename = args[0];
	
	try {
		FileOutputStream f = new FileOutputStream(filename);
		LBVizGraphic control = new LBVizGraphic(false);	 // no messaging?
		control.makeTestData(9);
		control.createImage(f);
		f.close();
		if(spew_)SystemDesign.logDebug("Created file '" + filename + "'!");
		}
	catch (Exception e) {
		e.printStackTrace();
		}
	} // main


/**
 * Draw a 'key' for the diagram, displaying what's what.
 * This doesn't scale properly w/r/t IMAGE_SIZE.
 * 
 * @param g - what to draw into.
 * 
 */
private void
drawKey(Graphics2D g) {

	boolean labelBA = false;

	for (int pass=1; pass<=2; pass++) {

		double x = IMAGE_SIZE/20; // was 35;
		if (pass == 2) {
			x += 3*NODE_RADIUS/5; 
			}
		double y = x;

		Arc2D.Double nodeCirc = new Arc2D.Double(x-NODE_RADIUS/2, y-NODE_RADIUS/2, 
									NODE_RADIUS, NODE_RADIUS, 0, 360, 1);
		g.setColor(colorUnusedNode_);
		g.fill(nodeCirc);

		// RAM and CPU usage examples.
		// It would have been nice to show a red and a yellow 'before' but that looks ugly.
		//
		int cpu = 180+45;
		int ram = 360-45;
		if (pass == 2) {
			cpu = 180-45;
			ram = 270-45;
			}

		doPie(g, cpu, x, y, NODE_RADIUS);
		doPie(g, ram, x, y, NODE_RADIUS_2);
	
		// ring around inner pie for contrast
		Arc2D.Double perim = new Arc2D.Double(x-NODE_RADIUS_2/2-1, y-NODE_RADIUS_2/2-1, 
									NODE_RADIUS_2+1, NODE_RADIUS_2+1, 0, 360, 1);
		g.setColor(Color.black);
		g.draw(perim);
	
		g.setColor(Color.black);
		int yLine = (int)y-NODE_RADIUS/5;

		String beforeOrAfter = (pass==1?"before LB":"after LB");
// dunno if i like that....
		if (labelBA) {
			beforeOrAfter = "";
			}
		
		g.drawLine((int)(x+NODE_RADIUS_2/4), yLine-12, (int)x+40, yLine-12);
		g.drawString("  RAM " + beforeOrAfter, (int)x+40, (int)y-18);
	
		g.drawLine((int)(x+NODE_RADIUS_2/4), yLine, (int)x+40, yLine);
		g.drawString("  CPU " + beforeOrAfter, (int)x+40, (int)y-6);
		} // pass

	if (labelBA) {

		g.drawLine(12, 65, 12, 100);
		g.drawString("Before", 5, 115);

		g.drawLine(68, 110, 68, 115);
		g.drawString("After", 60, 130);
		}

	// diagonal separator
	g.setColor(Color.darkGray);
	g.drawLine(40, 220, 220, 40);

	// an agent
	g.setColor(Color.blue);
	
	drawAgent(g, 15, 130);
	drawAgent(g, 15+AGENT_RADIUS, 130);
	drawAgent(g, 15+2*AGENT_RADIUS, 130);

	g.setColor(Color.black);
	g.drawString("Agents", 46, 137);

	} // drawKey


/**
 * Make 'em a little smaller so we can see behind/thru?
 * @param g
 * @param x
 * @param y
 */
private void
drawAgent(Graphics2D g, double x, double y) {

	g.setColor(Color.blue);
	Arc2D.Double aDot = new Arc2D.Double(x, y, AGENT_RADIUS-1, AGENT_RADIUS-1, 0, 360, 1);
	g.fill(aDot);

	// spiffy little hilite
	g.setColor(Color.white);
	aDot = new Arc2D.Double(x+2, y+1, 2, 2, 0, 360, 1);
	g.fill(aDot);
	}

/**
 * Set the graphic's title
 */
public void
setTitle(String title, int x, int y) {
	title_ = title;
	titleX_ = x;
	titleY_ = y;
	}



/**
 * Display the contents of two XML files 
 * (as opposed to the usual deal of displaying an input file and the reuslts of an EN solver run)
 * Can be used to test how the graphics layout will handle various results, not too useful otherwise.
 * 
 * @param fileBefore
 * @param fileAfter
 */
public static void
displayTwoXMLFiles(String fileBefore, String fileAfter) {

	CougaarSociety cs1 = null, cs2 = null;

	CSParser pcs = new CSParser();
	try {
		cs1 = pcs.parseCSXML(new FileInputStream(fileBefore));
		cs2 = pcs.parseCSXML(new FileInputStream(fileAfter));
		}
	catch (FileNotFoundException fnfe) {
		System.out.println("displayTwoXMLFiles: can't read input file?" + fnfe);
		return;
		}

	cs1.setIdentifier("Input data from: " + fileBefore);
	cs2.setIdentifier("Result data from: " + fileAfter);

	LBVizGraphic control = new LBVizGraphic(false); // don't show messaging

	java.util.ArrayList problems = control.loadBeforeAndAfterData(cs1, cs2);
	if (problems != null) {
		java.util.Iterator pIter = problems.iterator();
		while (pIter.hasNext()) {
			System.out.println((String)pIter.next());
			}
		}

	control.setTitle("Visualised XML files:", 300, 16);

	try {
		// "viz-u-lize it, yeah, yeah...."
		FileOutputStream f = new FileOutputStream(fileBefore + ".jpeg");
		control.createImage(f);
		f.close();
		}
	catch (Exception e) {
		System.out.println(e.getMessage());
		}

	} // displayTwoXMLFiles
	
	
/**
 * Class to embody what we need to know about a node in order to draw a representation of it.
 */
private class
NodeData {

protected String 	nodeName_;
protected boolean 	existed, exists;
protected int		agentCountBefore_;
protected int		agentCountAfter_;
protected double	CPUbefore_;		// CPU fraction, 1.0=100%
protected double	CPUafter_;		// similiar
protected double	RAMbefore_;		// likewise
protected double	RAMafter_;		// analogous

public NodeData(String nodeName) {
	nodeName_ = nodeName;
	agentCountBefore_ = 0;
	agentCountAfter_ = 0;
	existed = false;
	exists = false;
	}
} // class NodeData

} // LBVizGraphic

