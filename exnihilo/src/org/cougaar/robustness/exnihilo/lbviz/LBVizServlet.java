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

import org.cougaar.robustness.exnihilo.CSParser;
import org.cougaar.scalability.util.CougaarSociety;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;

/**
 * Display a diagram of the Cougaar Society.
 *
 * URL params accepted:
 * 
 *  ?image={anything}
 * 		- returns image data (in JPEG or PNG format)
 * 	?help={anything}
 * 		- returns HTML help screen
 * 
 * 
 *  @author robert.e.cranfill@boeing.com
 *  @version 1.0
 */
public class LBVizServlet 
     extends HttpServlet {

private LBVizGraphic viz_ = null;


/** 
 * Initialize the servlet.
*/
public void init(ServletConfig config) 
throws ServletException {

	super.init(config);
	
	viz_ = new LBVizGraphic(true);	// show inter-node messaging

	boolean useXMLTestFile = true;
	if (useXMLTestFile) {

		CSParser csp = new CSParser();
		CougaarSociety socBefore = null, socAfter = null;
		try {
			socBefore = csp.parseCSXML(new FileInputStream("servlet_input_soc.xml"));
			socAfter  = csp.parseCSXML(new FileInputStream("servlet_input_soc_after.xml"));
//			System.out.println("CougaarSociety XML parsed OK");
			}
		catch (FileNotFoundException fnfe) {
//			System.out.println("*** Error: Couldn't read a CougaarSociety XML input file!");
			}

		viz_.loadBeforeAndAfterData(socBefore, socAfter);

		}
	else {
		// some primitive hard-coded test data
		// viz_.IMAGE_SIZE = 640;
		viz_.makeTestData(7);
		}

	} //init



/** 
 * Destroy the servlet.
*/
public void destroy() {

	viz_ = null;
	}


/** 
 * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
 *  @param request servlet request
 *  @param response servlet response
 */
protected void processRequest(HttpServletRequest request, HttpServletResponse response)
throws ServletException, java.io.IOException {

	if (viz_ != null) {

		// If it's a page request, "image" param will not exist; 
		// if it's an image request, it will (we don't care about the value)
		//
		if (request.getParameter("help") != null) {
			outputHelpPage(response);
			}
		else
		if (request.getParameter("image") != null) {

			// Create the graphic.
			//
			String contentType = viz_.createImage(response.getOutputStream());
			if (contentType == null) {
				outputErrorPage(response);
				return;
				}
			response.setContentType(contentType);
			}
		else {
			outputMainPage(response);
			}

		}
	else {
		outputErrorPage(response);
		}
	} // processRequest


/**
 * Output the HTML with a call back to us (via an IMG tag) to generate the image.
 *
 */
private void
outputHelpPage(HttpServletResponse resp) {
	
	resp.setContentType("text/html");
	try {
		PrintWriter out = resp.getWriter();
		out.println("<HTML><HEAD><TITLE>LBVisualizer Help</TITLE></HEAD>");
		out.println("<BODY BGCOLOR=\"#BBBBBB\">");
		out.println("<H2>Ultralog load balancing visualization servlet</H2>");

		out.println("The display consists of a pair of circles for each node in the system - ");
		out.println("before and after load balancing has taken place.<P>");

		out.println("Within each pair of circles, the one above and to the left");
		out.println("is the 'before load balancing' configuration; the lower right circle is the 'after'.");
		out.println("If a particular node did not exist in the original society, or did not exist");
		out.println("in the post-load balanced society, its 'before' or 'after' circle");
		out.println("is solid light grey.<P>");

		out.println("Each 'before' or 'after' circle has an outer ring, depicting the system");
		out.println("CPU utilization, that is green when within normal health limits ");
		out.println("(see below for threshold values),");
		out.println("yellow for marginal utilization, and red above a critical threshold.<P>");

		out.println("Similarly each circle has a center area which depicts the system");
		out.println("RAM utilization, using the same coloring scheme.<P>");

		out.println("Blue dots within each node depict agents, arrayed in rows of 10.<P>");

		out.println("Green/yellow threshold: " + 100 * LBVizGraphic.THRESHOLD_YELLOW/360 + "%<P>");
		out.println("Yellow/red threshold: " + 100 * LBVizGraphic.THRESHOLD_RED/360 + "%<P>");
		
		out.println("</BODY></HTML>");
		}
	catch (Exception e) {
		e.printStackTrace();
		}
	} // outputHelpPage


/**
 * Output the HTML with a call back to us (via an IMG tag) to generate the image.
 *
 */
private void
outputMainPage(HttpServletResponse resp) {

	resp.setContentType("text/html");
	try {
		PrintWriter out = resp.getWriter();
		out.println("<HTML><HEAD><TITLE>LBVisualizer</TITLE></HEAD>");
		out.println("<BODY BGCOLOR=\"#BBBBBB\">"); // was antiquewhite
		out.println("<H2>Load balance solution posted " + new java.util.Date() + "</H2><BR>");

		// Don't know why, but we only have to provide the relative URL, which is easy.
		//
		String imageURL = "LBViz?image=";
		out.println("<IMG SRC=\"" + imageURL + "\" BORDER=\"2\"><BR>");

		out.println("<A HREF=\"LBViz?help=\" TARGET=\"blank_\">Help</A><BR>");

		out.println("<A HREF=\"mailto:robert.e.cranfill@boeing.com" + 
						"?subject=Your marvelous Load Balance Visualizer\">Mail to rob</A><BR>");
		out.println("</BODY></HTML>");
		}
	catch (Exception e) {
		e.printStackTrace();
		}
	}


/**
 * If we couldn't create the control image... gotta show something!
 *
 */
private void
outputErrorPage(HttpServletResponse resp) {

	resp.setContentType("text/html");
	try {
		PrintWriter out = resp.getWriter();
		out.println("<HTML><HEAD><TITLE>LBViz error</TITLE></HEAD>");
		out.println("<BODY>Sorry, an error occured. What error, you ask? Glad you asked that!</BODY></HTML>");
		}
	catch (Exception e) {
		e.printStackTrace();
		}
	}


/** Handle the HTTP <code>GET</code> method.
 * @param request servlet request
 * @param response servlet response
*/
protected void doGet(HttpServletRequest request, HttpServletResponse response)
throws ServletException, java.io.IOException {
	
	processRequest(request, response);
	}


/** 
 * Handle the HTTP <code>POST</code> method.
 * @param request servlet request 
 * @param response servlet response
*/
protected void doPost(HttpServletRequest request, HttpServletResponse response)
throws ServletException, java.io.IOException {
	
	processRequest(request, response);
	}


/** 
 * Return a short description of this servlet.
*/
public String getServletInfo() {

	return "LBVizServlet: An UltraLog load balance visualizer";
	}

} // LBVizServlet
