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

import org.cougaar.robustness.exnihilo.CSParser;
// import org.cougaar.robustness.exnihilo.SystemDesign;

import org.cougaar.scalability.util.*;

import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.LoggingService;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;


/**
 * 
 * Allow user to generate a load balancing request for EN4JPlugin to process, 
 * and display the result.
 *
 *
 * ??? Are we using the right object model? We don't get subscribtion notices
 * thru the 'execute' method, like most plugins. We just peek/poke the blackboard
 * on command from the user. <P>
 * 
 * 
 * Options/commands for the URL:<P>
 * <UL>
 * 
 * <LI> ?loadXML={cougaar_society_xml_path}[+annealTime={time_in_seconds}]<P>
 *      Loads an XML file representing a CougaarSociety; 
 *      accesses <U>servlet's</U> local file system.
 * 
 * <LI> ?displayBlackboard=<P>
 *      Displays blackboard contents (LB solutions only) [param value not used]
 * 
 * <LI> ?clearBlackboard=<P>
 *      Clears blackboard contents (LB solutions only) [param value not used]
 * 
 * </UL>
 * 
 * @author robert.e.cranfill@boeing.com
 */
public class EN4JTestServlet
     extends BaseServletComponent 
  implements BlackboardClient {


// The "commands", that is, HTTP 'GET' request keyword args.
//
public static final String REQ_PARAM_NAME_LOAD_XML      = "loadXML";
public static final String REQ_PARAM_NAME_ANNEAL_TIME   = "annealTime";
public static final String REQ_PARAM_NAME_DISPLAY_BB    = "displayBlackboard";
public static final String REQ_PARAM_NAME_CLEAR_BB      = "clearBlackboard";

public static final String REQ_PARAM_NAME_DO_LAPS      	= "doLaps";
public static final String REQ_PARAM_LAP_TARGET      	= "lapTarget";

public static final String REQ_PARAM_NAME_GRAPH         = "graph";

private static LoggingService	loggingService_;
private BlackboardService       blackboardService_;
private PrintWriter             out_;

// params for requests; have to use globals cuz of weird ParamVisitor... ?
private String      xmlFileName_ = null;
private String      annealTimeStr_ = null;

private String httpCommand_ = "";

/** I think the manual clearing thing was a bad idea.... */
public boolean autoClearSolutions_ = true;

/** does manual "clear solutions" button just clear solutions, or does it also clear requests? */
public boolean onlyClearSolutions_ = false;  // clear all

public boolean	doLaps_ = false; // do multiple tests?
public int		lapTarget_;

private boolean gotResult_ =false;


// Holds our subscription for LoadBalanceRequest objects.
//
private IncrementalSubscription  reqs_ = null;


/**
 * Checks BB for results, and if found, 
 * removes them from BB and returns them in the result ArrayList.
 * @return
 */
private ArrayList // of CougaarSociety 
checkSubs() {

	ArrayList result = new ArrayList();
	try {

		blackboardService_.openTransaction();
		IncrementalSubscription lbrs =
			(IncrementalSubscription) blackboardService_.subscribe(
										new LoadBalanceRequestPredicate(true));

		Enumeration newReqs = lbrs.getAddedList();
		while (newReqs.hasMoreElements()) {
	
			// Only process solutions, not pending requests.
			//
			LoadBalanceRequest lbr = (LoadBalanceRequest)newReqs.nextElement();
			if (lbr.isResult()) {
				result.add(lbr);
				blackboardService_.publishRemove(lbr);
			}
		} // while
	}
	catch (Exception e) {
		logInfo(".checkSubs error: " + e.getMessage());    
	}
	finally {
		blackboardService_.closeTransactionDontReset();
	}

	return result;
} // checkSubs


/**
 * Called when we are loaded - subscribe to LoadBalanceRequest result objects.
 * 
 * ??? Either the subscription is wrong, or execute() is, cuz we don' get nuthin'!
 * 
 */
protected void 
setupSubscriptions() {

	if (reqs_ != null) {
		logWarn(".setupSubscriptions: we already have reqs! (OK)");
		return;
	}

	if (blackboardService_ == null) {
		logWarn(".setupSubscriptions: blackboardService_ is null!");
		return;
	}

	try {
		blackboardService_.openTransaction();
		reqs_ = (IncrementalSubscription)blackboardService_.subscribe(new LoadBalanceRequestPredicate(true));
		logInfo(".doLaps: subscribed OK?");
	}
	catch (Exception e) {
		logWarn("Exception: " + e + "<BR>");
	}
	finally {
		blackboardService_.closeTransactionDontReset();
	}

	logInfo(".setupSubscriptions done.");
}

/**
 * 
 * ??? Either the subscription is wrong, or execute() is, cuz we don' get nuthin'!
 * 
 */
protected void execute() {

	logInfo(" @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ .execute!!!! reqs_ size " + reqs_.size());

	gotResult_ = true;
	for (Iterator it=reqs_.iterator(); it.hasNext();) {
/*
		Task t = (Task) it.next();
			if (t.getPlanElement() == null) { // is it unexpanded?
			// expand Task t
*/
	}
}


/**
 * This method is called by Cougaar to give us a handle to the blackboard.
 */
public void setBlackboardService(BlackboardService bs) {
	blackboardService_ = bs;
}


/**
 * Called when we're started.
 * Use it to get a handle to the logging service 
 * 	(there's no "setLoggingService" callback?)
 */
public void load() {

	// get the logging service
	loggingService_ = (LoggingService) serviceBroker.getService(
			             this, LoggingService.class, null);

	super.load();
	
	// Apparently I need to do this manually.
//	setupSubscriptions();
}


/**
    Load test data from an XML file, return a CougaarSociety from it.
    Public so our test module can access it (should move it elsewhere?)
**/
public CougaarSociety 
loadSocietyFromXML(String filename) {

    CSParser csp = new CSParser();
    csp.echoStruct_ = false;    // be quiet
    CougaarSociety society  = null;
    try {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename));
        society = csp.parseCSXML(bis);
        bis.close();
    }
    catch (Exception e) {
        logInfo(".loadSocietyFromXML: " + e);
        if (out_!=null)
        	out_.println("Error in .loadSocietyFromXML: " + e + "<BR>");
    }

    return society;
} // loadSocietyFromXML


/**
 * Required by BaseServletComponent.
 */
public String 
getPath() {
    return "/EN4JTestServlet";
}


/**
 * Create our inner servlet.
 */
protected Servlet 
createServlet() {

	return new EN4JServlet();
}



/**
 * Inner class that is our raw servlet.
 */
private class EN4JServlet 
extends HttpServlet {

public void 
doGet(HttpServletRequest request, HttpServletResponse response)
throws java.io.IOException {

    parseParams(request, response);
} // doGet

} // inner class EN4JServlet


/**
 * This is structured like this so this method can access instance vars of EN4JTestServlet.
 * Seems a rather arcane way to do this....
 */
private void 
parseParams(HttpServletRequest request, HttpServletResponse response) 
throws IOException {

    httpCommand_ = "";
    xmlFileName_ = null;
    annealTimeStr_ = "";
	doLaps_ = false;

    // Create a URL parameter visitor - is this really that useful?
    // Can set global xmlFileName_, httpCommand_
    //
    ServletUtil.ParamVisitor vis = new ServletUtil.ParamVisitor() {
    	public void setParam(String name, String value) {

    		if (name.equalsIgnoreCase(REQ_PARAM_NAME_LOAD_XML)) {
    			xmlFileName_ = value;
                httpCommand_ = REQ_PARAM_NAME_LOAD_XML;
            }
            else
            if (name.equalsIgnoreCase(REQ_PARAM_NAME_ANNEAL_TIME)) {
                annealTimeStr_ = value;
                httpCommand_ = REQ_PARAM_NAME_LOAD_XML;
           }
            else
            if (name.equalsIgnoreCase(REQ_PARAM_NAME_DISPLAY_BB)) {
                httpCommand_ = REQ_PARAM_NAME_DISPLAY_BB;
            }
            else
            if (name.equalsIgnoreCase(REQ_PARAM_NAME_CLEAR_BB)) {
                httpCommand_ = REQ_PARAM_NAME_CLEAR_BB;
            }
			else
			if (name.equalsIgnoreCase(REQ_PARAM_NAME_GRAPH)) {
				httpCommand_ = REQ_PARAM_NAME_GRAPH;
			}
			else
			if (name.equalsIgnoreCase(REQ_PARAM_NAME_DO_LAPS)) {
				doLaps_ = true;
				logInfo("setting doLaps_ = true");
			}
			else
			if (name.equalsIgnoreCase(REQ_PARAM_LAP_TARGET)) {
				lapTarget_ = Integer.parseInt(value);
			}
    	}
    };

    // Visit the URL parameters
    //
    ServletUtil.parseParams(vis, request);

    logInfo("HTTP Command: '" + httpCommand_ + "'");

    // If it's a graph request, we don't want to get the Writer,
    // cuz you can only do that once.
    //
    if (httpCommand_.equals(REQ_PARAM_NAME_GRAPH)) {
		logWarn("Warning: " + httpCommand_ + " not supported!");
//       displayGraph(response);
        return;
    }

    out_ = response.getWriter();

	if (httpCommand_ == null || httpCommand_.equals("")) {
		showFrontPage();
	}
    else
    if (httpCommand_.equals(REQ_PARAM_NAME_LOAD_XML)) {
 
 		if (doLaps_) {
 			doLaps(xmlFileName_);
 		}
 		else {
			doXMLSociety(xmlFileName_);
 		}
    }
    else
    if (httpCommand_.equals(REQ_PARAM_NAME_DISPLAY_BB)) {
        displayBB();
    }
    else
    if (httpCommand_.equals(REQ_PARAM_NAME_CLEAR_BB)) {
        clearBB();
    }
    else {
        logInfo("unknown command!");
    }

} // parseParams


/**
 * Do a buncha tests! Similar to doXMLSociety()
 * 
 * @param xmlFileName
 */
private void
doLaps(String xmlFilePath) {

	// The output from this test routine is on the logger and in the output file.
	// The HTML page just tells you where to look.
	//
	out_.println("<HTML>");
	out_.println("<HEAD>");
	out_.println("<META HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\">");
	out_.println("<TITLE>EN4JTestServlet Lap Test</TITLE>");
	out_.println("</HEAD>");
	out_.println("<BODY>");

	int annealTime = 30;
	try {
		annealTime = Integer.parseInt(annealTimeStr_);
		logInfo("Requested anneal time: " + annealTime + " seconds<BR>");
	}
	catch (NumberFormatException nfe) {
		logWarn("Bad anneal time '" + annealTimeStr_ + 
						"'; using default " + annealTime + ")<BR>");
	}

	String resultsFileName = xmlFilePath + "-results.text";
	logInfo(".doLaps: opening output file '" + resultsFileName + "'.");
	PrintWriter resultsOut = null;
	try {
		resultsOut = new PrintWriter(new FileWriter(resultsFileName, true));
	}
	catch (IOException ioe) {
		logInfo(".doLaps: IOException! " + ioe.getMessage());

		out_.println("<H2> Lap Test failed!</H2>");
		out_.println("Exception '" + ioe.getMessage() + "' opening input file!<BR>");
		out_.println("</BODY></HTML>");
		out_.close();

		// ioe.printStackTrace();
		return;
	}

	out_.println("<H2> Lap Test underway....</H2>");
	out_.println("See local file '" + resultsFileName + "' for test results.<BR>");
	out_.println("</BODY></HTML>");
	out_.close();

	logInfo(".doLaps: loading data from file '" + xmlFilePath + "'...");

	// we don't need to poll for results too terribly often.
	//
	int sleepTime = 1; // second
	if (annealTime > 60) {
		sleepTime = 5;
	}

	// Create a CougaarSociety
	//
	CougaarSociety testSociety = loadSocietyFromXML(xmlFilePath);
	if (testSociety == null) {
		logWarn(".doLaps: error loading XML data!");
	}
	else {

		resultsOut.println("Ta" + "\t" + 
							"#" + "\t" + 
							"ann'l?" + "\t" + 
							"Good?" + "\t" + 
							"iter" + "\t" + 
							"end time" + "\t" + 
							"xmlFilePath"
							);

		for (int iTest=1; iTest<=lapTarget_; iTest++) {

			// Encapsulate the CougaarSociety in a load balancing request.
			//
			LoadBalanceRequest loadBalReq = new LoadBalanceRequest();
			loadBalReq.setCougaarSociety(testSociety);
			loadBalReq.setAnnealTime(annealTime);

			try {
				Date now = new Date();            
				logInfo(".doLaps: Publishing LBRequest @" + now + "...");
	
				// Start a transaction, publish the request, close the transaction.
				//
				blackboardService_.openTransaction();
				blackboardService_.publishAdd(loadBalReq);
	
				logInfo(".doLaps: Published: " + loadBalReq + " OK!");
			}
			catch (Exception e) {
				logWarn("Exception: " + e + "<BR>");
			}
			finally {
				blackboardService_.closeTransactionDontReset();
				logInfo("EN4JTestServlet closed xaction");
			}

			// Wait for results; if our subscription worked, we'd do this differently...
			//
			gotResult_ = false;
			ArrayList subResults = null;
			while (gotResult_ == false) {

				try {
					Thread.sleep(sleepTime * 1000);
				}
				catch (InterruptedException ie) {
					logInfo(".doLaps: InterruptedException!");
				}
				subResults = checkSubs();
				gotResult_ = (subResults.size() > 0);
			}
			if (subResults.size() != 1) {
				logWarn(".doLaps: YOU GOTTA CLEAR THE OLD RESULTS!");
			}

			LoadBalanceRequest lbr = (LoadBalanceRequest)subResults.get(0);

			// Visualizer/checker
			//
			CSVisualizer csv = new CSVisualizer(lbr.getCougaarSociety());

			logInfo(".doLaps: finished test #" + iTest);
			logInfo("");

			// Output to the results file.
			//
			resultsOut.println(	annealTime + "\t" +
								iTest + "\t" + 
								lbr.wasSolutionFound() + "\t" +
								csv.isWithinLimits() + "\t" +
								lbr.getIterations() + "\t" +
								new java.util.Date() + "\t" + 
								xmlFilePath
							   );

		}
	}

	logInfo(".doLaps finished!");
	logInfo("");
	resultsOut.flush();
	resultsOut.close();

}

///**
// * Display a graph of some sort? Probably never will be used.
// */
private void
displayGraph(HttpServletResponse response) {
//    
//    CSVisualizer csg = new CSVisualizer();
//    csg.drawResult(response);
}


/**
 * In response to user's request, clear the Blackboard.
 * This could mean they'll clear off stuff other than what they thought was there, but hey!
 */
private void
clearBB() {

    out_.println("<HTML>");
    out_.println("<HEAD>");
    out_.println("<META HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\">");
    out_.println("<TITLE>EN4JTestServlet BB clear results</TITLE>");
    out_.println("</HEAD>");
    out_.println("<BODY>");
    out_.println("<H2>Attempting to clear LBRs from Blackboard....</H2>");

    if (blackboardService_ == null) {
        logWarn("Blackboard service is null! - can't clear it!");
        out_.println("Can't get blackboard service!<BR>");
        
        out_.println("</BODY></HTML>");
        out_.flush();
        out_.close();

        return;
    }

    try {

        // Start a transaction, register our subscription, close the transaction.
        //
        blackboardService_.openTransaction();

        IncrementalSubscription lbrs =
            (IncrementalSubscription) blackboardService_.subscribe(
                                        new LoadBalanceRequestPredicate(true));
        if (lbrs.size() == 0) {
            logSevere("No LBRs posted.<BR>");
        }
        else {
            out_.println(lbrs.size() + ((lbrs.size()==1)?" LBR":" LBRs") + " found:<BR>");

            Enumeration newReqs = lbrs.getAddedList();
            while (newReqs.hasMoreElements()) {

                LoadBalanceRequest lbr = (LoadBalanceRequest)newReqs.nextElement();

                // Only remove solutions?
                //
                if (onlyClearSolutions_ & !lbr.isResult()) {
                    out_.println("(<U>Not</U> clearing unsatisfied request " + lbr + ")<BR>");
                }
                else {
                    out_.println("Clearing: " + lbr + ":<BR>");
                    blackboardService_.publishRemove(lbr);
                }
            } // while
        }

        logInfo(".clearBB cleared OK!");    
    }
    catch (Exception e) {
        logInfo(".clearBB ERROR: " + e.getMessage());    
    }
    finally {
        blackboardService_.closeTransactionDontReset();
    }

    // For convenience
    out_.println("<TT>");
    out_.println("<FORM METHOD=\"GET\">");
    out_.println("  <INPUT TYPE=\"SUBMIT\" VALUE=\" Back to Start \">");
    out_.println("</FORM>");
    out_.println("</TT>");


    out_.flush();

} // clearBB


/**
 * Check for LoadBalance results, and display the HTML of all found.
 */
private void
displayBB() {

    out_.println("<HTML>");
    out_.println("<HEAD>");
    out_.println("<META HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\">");
    out_.println("<TITLE>EN4JTestServlet results</TITLE>");
    out_.println("</HEAD>");
    out_.println("<BODY>");
    out_.println("<H2>LoadBalanceRequests (results) from blackboard</H2>");


    if (blackboardService_ == null) {
        out_.println("Blackboard service is null! - can't check for results!<BR>");
        logWarn("Blackboard service is null! - can't check for results!");
    }
    else {

        try {

            out_.println("Checking blackboard...");
            blackboardService_.openTransaction();
            IncrementalSubscription lbrs =
                (IncrementalSubscription) blackboardService_.subscribe(
                                            new LoadBalanceRequestPredicate(true));
            if (lbrs.size() == 0) {
                out_.println("No LBRs posted.<BR>");
            }
            else {
                out_.println(lbrs.size() + ((lbrs.size()==1)?" LBR":" LBRs") + " found!<BR>");
    //            out_.println("<HR WIDTH=\"50%\">");
    
    
    if (!autoClearSolutions_) {
        
                // an easier-to-get-at copy of the button 
                // that's also at the bottom of this page.
                out_.println("<TT>");
                out_.println("<FORM METHOD=\"GET\">");
                out_.println("  <INPUT TYPE=\"SUBMIT\" VALUE=\" Clear LBResults from Blackboard \">");
                out_.println("  <INPUT TYPE=\"HIDDEN\"  NAME=\"" + REQ_PARAM_NAME_CLEAR_BB + "\">");
                out_.println("</FORM>");
                out_.println("</TT>");
    //            out_.println("<HR WIDTH=\"50%\">");
    }
           
                Enumeration newReqs = lbrs.getAddedList();
                while (newReqs.hasMoreElements()) {
    
                    LoadBalanceRequest lbr = (LoadBalanceRequest)newReqs.nextElement();
    
                    // Only process solutions, not pending requests.
                    //
                    if (lbr.isResult()) {

                        //
                        // Summarize results
                        //
 
                        out_.println("<BR>");
                        out_.println("Found solution: " + lbr + ":<BR>");
 
 // testB is rather obsolete now...
       //                 boolean testBResult = verifyTestB(lbr.getCougaarSociety());
       //                 out_.println("Passes testB?: " + (testBResult?"YES":"NO") + "<BR>");
                        
                        // It would appear that if this is false, we can just go home.
                        // But we'll gamely solier on even if that's the case....
                        //
                        out_.println("Annealing solution?: " + (lbr.wasSolutionFound()?"YES":"<B>NO</B>") + "<BR>");

                        // Visualizer/checker
                        //
                        CSVisualizer csv = new CSVisualizer(lbr.getCougaarSociety());
						// Don't really know what are good values for these. 
						// They really are 'soft' limits....
						//
						csv.CPU_YELLOW_LEVEL = .9f;
						csv.CPU_RED_LEVEL = 1.1f;
						csv.RAM_YELLOW_LEVEL = 1.0f; 
						csv.RAM_RED_LEVEL = 2.0f;

                        out_.println("Within limits? " + 
                                     (csv.isWithinLimits()?"YES":"<B>NO</B>") + "</B><BR>\n");

                        if (lbr.wasSolutionFound() && !csv.isWithinLimits()) {
                            out_.println("<B>THIS IS BAD</B> (wasSolutionFound==T, isWithinLimits==F)<BR>");
                        }

                        //
                        // Detail results
                        //

                        out_.println(lbr.toHTML());
                    
                        // Display CougaarSociety 'visualization'
                        //
                        out_.println("<HR WIDTH=\"50%\">\n");

                        String html = csv.toHTML();
                        out_.println("<BR>\n");
                        out_.println(html);

                        if (autoClearSolutions_) {
                            out_.println("(Auto-clearing this LBR...)<BR>");
                            blackboardService_.publishRemove(lbr);
                        }
                    }
                    else {
                        out_.println("(Unsatisfied request " + lbr + ")<BR>");
                    }
                    
                    if (newReqs.hasMoreElements()) {
                        out_.println("<HR WIDTH=\"25%\">");
                    }

                } // while
            }
        }
        catch (Exception e) {
            logInfo(".createServlet ERROR: " + e.getMessage());    
        }
        finally {
            blackboardService_.closeTransactionDontReset();
        }

		if (!autoClearSolutions_) {
		            
		        out_.println("<BR>");
		        out_.println("<HR WIDTH=\"50%\">");
		        out_.println("<TT>");
		        out_.println("<FORM METHOD=\"GET\">");
		        out_.println("  <INPUT TYPE=\"SUBMIT\" VALUE=\" Clear LBResults from Blackboard \">");
		        out_.println("  <INPUT TYPE=\"HIDDEN\"  NAME=\"" + REQ_PARAM_NAME_CLEAR_BB + "\">");
		        out_.println("</FORM>");
		        out_.println("</TT>");
		}
   
    }

    out_.println("</BODY>");
    out_.println("</HTML>");

    out_.flush();
    out_.close();

} // displayResults


/**
 * Given the path to an XML file representing CougaarSociety data,
 * load the data publish it to tbe blackboard, to be loadbalanced.
 */
private void
doXMLSociety(String xmlFilePath) {

	if ("".equals(xmlFilePath)) {
		logInfo(".doXMLSociety: XML filename is empty; creating empty LBReq.");
		}
	else {
		logInfo(".doXMLSociety: loading data from file '" + xmlFilePath + "'...");
		}

    out_.println("<HTML>");
    out_.println("<HEAD>");
    out_.println("<META HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\">");
    out_.println("<TITLE>EN4JTestServlet loading XML</TITLE>");
    out_.println("</HEAD>");
    out_.println("<BODY>");
    out_.println("Loading XML data. from '" + xmlFileName_ + "'....<BR>");

    int annealTime = 30;
    try {
        annealTime = Integer.parseInt(annealTimeStr_);
        out_.println("Requested anneal time: " + annealTime + " seconds<BR>");
    }
    catch (NumberFormatException nfe) {
        out_.println("Bad anneal time '" + annealTimeStr_ + 
                        "'; using default " + annealTime + ")<BR>");
    }

    // Create a CougaarSociety
    //
    CougaarSociety testSociety = null;
    if (xmlFilePath.equals("") == false) {
    	
    	loadSocietyFromXML(xmlFilePath); 
	    if (testSociety == null) {
	        out_.println("Error creating test data!<BR>");
	        logInfo(".doXMLSociety: error loading XML data!");
	        return;
	    	}
    	}
    
    // Encapsulate the CougaarSociety in a load balancing request.
    //
    LoadBalanceRequest loadBalReq = new LoadBalanceRequest();
	loadBalReq.setCougaarSociety(testSociety);
	loadBalReq.setAnnealTime(annealTime);

    try {

        Date now = new Date();            
        logInfo(".doXMLSociety: Publishing LBRequest @" + now + "...");
        out_.println("Publishing LBRequest @" + now + "...<BR>");

        // Start a transaction, publish the request, close the transaction.
        //
        blackboardService_.openTransaction();
        blackboardService_.publishAdd(loadBalReq);

        logInfo(".doXMLSociety: Published: " + loadBalReq + " OK!");
        out_.println("Published: " + loadBalReq + " OK!<BR>");
    	}
    catch (Exception e) {
        out_.println("Exception: " + e + "<BR>");
    	}
    finally {
        blackboardService_.closeTransactionDontReset();
        logInfo("EN4JTestServlet closed xaction");
    	}



    out_.println("<BR>");
    out_.println("<TT>");
    out_.println("<FORM METHOD=\"GET\">");
    out_.println("  <INPUT TYPE=\"SUBMIT\" VALUE=\"Display LB Results\">");
    out_.println("  <INPUT TYPE=\"HIDDEN\"  NAME=\"" + REQ_PARAM_NAME_DISPLAY_BB + "\">");
    out_.println("</FORM>");
    out_.println("</TT>");

    out_.println("</BODY>");
    out_.println("</HTML>");

    out_.flush();
    out_.close();
    
    logInfo(".doXMLSociety finished!");
    
} // doXMLSociety


/**
 * An invocation of the servlet with no args gets this page; 
 * present a form for requesting a load balance, 
 * with data to be read from the specified (local) XML file.
 */
private void 
showFrontPage() {

    logInfo(".showFrontPage");

    out_.println("<HTML>");
    
    out_.println("<HEAD>");
    out_.println("<META HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\">");
    out_.println("<TITLE>EN4JTestServlet</TITLE>");
    out_.println("</HEAD>");
 
    out_.println("<BODY>");
    
    out_.println("<H2>Submit an XML file for balancing</H2>");

    out_.println("<TT>");
    out_.println("<FORM METHOD=\"GET\">");
    out_.println("&nbsp;&nbsp;XML input: <INPUT TYPE=\"TEXT\" NAME=\"" + REQ_PARAM_NAME_LOAD_XML + "\" VALUE=\"solve.xml\" SIZE=\"25\"><BR>");
    out_.println("Anneal time: <INPUT TYPE=\"TEXT\" NAME=\"" + REQ_PARAM_NAME_ANNEAL_TIME + "\" VALUE=\"15\" SIZE=\"4\"> seconds<BR>");

	out_.println("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + REQ_PARAM_NAME_DO_LAPS + "\"> Repeat test ");
	out_.println("<INPUT TYPE=\"TEXT\" NAME=\"" + REQ_PARAM_LAP_TARGET + "\" VALUE=\"2\" SIZE=\"4\"> times<BR>");

	out_.println("  <INPUT TYPE=\"SUBMIT\" VALUE=\"LoadBalance\">");
    out_.println("</FORM>");
    out_.println("</TT>");
  
    out_.println("</BODY>");
    out_.println("</HTML>");
    
    out_.flush();
    out_.close();
}


/**
 * Constructor - does nothing.
 */
public
EN4JTestServlet() {
    
}


/**
 * BlackboardClient method. ???
 */
public String getBlackboardClientName() {
    return toString();  // uh....
}


/**
 * Required BlackboardClient method. ???
 */
public long currentTimeMillis() {
    return new Date().getTime();
}


/**
 * Required BlackboardClient method. ???
 */
public boolean triggerEvent(Object event) {
    return false;
}



/**
 * If we've got a logger, use it.
 */
private void
logInfo(String msg) {

//    if (loggingService_ == null) {
//        System.out.println("\nEN4JTestServlet(so): " + msg);
//    }
//   else {

	if (loggingService_ != null) {
        loggingService_.info(msg);
    }
}


/**
 * If we've got a logger, use it.
 */
private void
logWarn(String msg) {

//    if (loggingService_ == null) {
//        System.out.println("\nEN4JTestServlet(so)!: " + msg);
//    }
//    else {
	if (loggingService_ != null) {
        loggingService_.warn(msg);
    }
}


/**
 * If we've got a logger, use it.
 */
private void
logSevere(String msg) {

//    if (loggingService_ == null) {
//        System.out.println("\nEN4JTestServlet(so)!!: " + msg);
//    }
//    else {
	if (loggingService_ != null) {
        loggingService_.error(msg);
    }
}


/**
 * 
 * Verify the results of TestB - CougaarSociety-based version of verify method in SystemDesign.
 * Ensure that:
 *      - "Agent8" and "Agent9" are together on one of the nodes;
 *      - There is just one agent on each of the other nodes.
 * 
**/
public boolean 
verifyTestB(CougaarSociety cs) {

    logInfo(".verifyTestB: checking agent/host annealing assignments...");

    // All nodes should have 1 agent, except one node has two.
    //
    CougaarNode nodeWithTwoAgents = null;
    
	for (Iterator iterNode=cs.getNodes(); iterNode.hasNext();) {

		CougaarNode node = (CougaarNode)iterNode.next();
		int agents = 0;
		for (Iterator iterAgent=node.getAgents(); iterAgent.hasNext(); agents++, iterAgent.next()) {
		}
		if (agents != 1) {
			if (agents != 2) {
				return false;
			}

			if (nodeWithTwoAgents == null) {
                nodeWithTwoAgents = node;
			} else {
                logInfo(".verifyTestB: fails! more than one node containing more than one agent!");
				return false;
			}
		}
    }

    // See if Agent8 and Agent9 are on the node with two agents.
    //
    CougaarAgent testAgent = null;
    testAgent = nodeWithTwoAgents.getAgent("Agent8");
    if (testAgent == null) {
        logInfo(".verifyTestB: fails! Agent8 not co-located with Agent9!");
        return false;
    }
    testAgent = nodeWithTwoAgents.getAgent("Agent9");
    if (testAgent == null) {
        logInfo(".verifyTestB: fails! Agent9 not co-located with Agent8!");
        return false;
    }

    logInfo(".verifyTestB: passes! (Agents 8 & 9 on node " + nodeWithTwoAgents.getName() + ")");
    return true;
}


} // EN4JTestServlet
