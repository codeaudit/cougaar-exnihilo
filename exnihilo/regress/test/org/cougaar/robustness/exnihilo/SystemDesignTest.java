/* 
 * <copyright>
 * Copyright 2003 The Boeing Company
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package test.org.cougaar.robustness.exnihilo;

import junit.framework.*;

import org.cougaar.scalability.util.*;	// from Scalability_infoether_utilities.jar
import org.cougaar.util.log.Logger;
import org.cougaar.robustness.exnihilo.Agent;
import org.cougaar.robustness.exnihilo.Function;
import org.cougaar.robustness.exnihilo.Link;
import org.cougaar.robustness.exnihilo.Node;
import org.cougaar.robustness.exnihilo.RnR;
import org.cougaar.robustness.exnihilo.SystemDesign;
import org.cougaar.robustness.exnihilo.CSParser;
import org.cougaar.robustness.exnihilo.lbviz.LBVizGraphic;
import org.cougaar.robustness.exnihilo.plugin.EN4JTestServlet;
import org.cougaar.robustness.exnihilo.plugin.EN4JPlugin;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.*;


/**
 * A test suite for EN4J.
 * Only halfway implements JTest stuff.
 * 
 * @author robert.e.cranfill@boeing.com
 * 
 * @version 17Mar04 - can specify killed nodes
 * @version Aug 16, 2003
 */
public class SystemDesignTest 
extends TestCase {


/**
 * Input data to test when using JTest.
 */
private static CougaarSociety inputSociety_ = null;
	
/**
 * Last tested SD object, for test suite to look at. Gnarly.
 */
private static SystemDesign lastSD_;

public static boolean		createLBPicture_ = true;

private static String		originalSocID_;	// why does this get stepped on?

private float[][] 			fullTrafficMatrix_;	// inter-node messaging for display


///**
// * Our test case #1.
// */
//public void
//runTest() {
//
//	// uh....
//
//	}


/**
 * A JTest 'suite' for EN4J. One could enumerate more test cases here.
 * @return The test suite.
 */
public static Test 
suite() { 

	TestSuite suite= new TestSuite(); 
	suite.addTest(new SystemDesignTest(1)); 
//	suite.addTest(new SystemDesignTest(2)); 
	return suite;
	}


/**
 * Run a test and assert the result for JTest.
 * 
 * @param testCase
 */
public 
SystemDesignTest(int testCase) {

	// Create a CougaarSociety from XML data 
	// (see the end of this file)
	//
	CSParser pcs = new CSParser();
	inputSociety_ = pcs.societyFromXML(testDataA1AsString_);

	// Build a command line and run the test via main()!
	//
	String[] testArgs = new String[] {
				"10", 				// seconds annealing
				"-internal", 		// use "internal" data (our XML string data)
				"-t", ""+testCase	// test case #
				};

	doTest(testArgs);

	switch (testCase) {
		case 1:
			assertTrue(testCaseA1_1(lastSD_));	// Check the last solution for goodness.
			break;
		case 2:
			break;
		default:
			assertTrue(false);	// error! ???
		}

	}


/**
 * For command-line testing, just pass params to doTest().
 * 
 * @param args - see doTest() args.
 */
public static void
main(String[] args) {

	doTest(args);

	}


/**
  This test driver takes command-line type args.<P>

<PRE><TT>

	{annealSeconds}
		* annealing time in seconds

	{XMLinputfile}
		* name of XML CougaarSociety data to read
	  or
	-en {inputbasename}
		* name of ExNihilo config files to read (inputbasename.nodedef, etc)

  optionally
	-r {repeatCount}
		* how many times to repeat the test (default is 1; "-r" by itself = 100)
	-t {testCaseNumber}
		* test case to run; 1-12; only some implemented; default=1
	-v
		* verify (check results of) test case; default is no verify
	-k {killedNodeList}
		* Use the comma-delimited list of killed nodes (one node per line)

</TT></PRE>

 * @author robert.e.cranfill@boeing.com
 */
public static void
doTest(String[] args) {

	CougaarSociety originalSociety = null;

	if (args.length < 2) {
		SystemDesign.logError("SystemDesign {annealSeconds} ([{XMLinputfile}] | [-en {inputbasename}])" + 
					"[-r] [-v] [-t {testCaseNumber}] [-p] [-k {killedNode1[,killedNode2...]} [-l {logLevel}]");
		SystemDesign.logError("  -en       # use EN-style input files");
		SystemDesign.logError("  -r        # repeat test 100 times");
		SystemDesign.logError("  -v        # verify 'a1' test case results");
		SystemDesign.logError("  -t {n}    # testCaseNumber n=(1...7)");
		SystemDesign.logError("  -p        # use EN4JPlugin");
		SystemDesign.logError("  -k {list} # killed node list, comma separated, perhaps quoted");
		SystemDesign.logError(" or ");
		SystemDesign.logError("  -d {xmlFilenameBefore} {xmlFilenameAfter}	# display XML");
		return;
		}

	int logLevel = Logger.DEBUG;
	StdOutLogger logger = new StdOutLogger(logLevel);
	SystemDesign.setLogger(logger);


	// Display existing before and after XML data;
	// mostly for testing LBVizGraphic, or creating presentations?
	//
	if (args.length == 3 && args[0].equals("-d")==true) {
		LBVizGraphic.displayTwoXMLFiles(args[1], args[2]);
		return;
		}

	// Get the requested anneal time.
	//
	int annealTime;
	try {
		annealTime = Integer.parseInt(args[0]);
		if (annealTime < 1) {
			logger.error("Bad syntax - first param is annealing time, in seconds; >0 please!");
			return;
			}
		}
	catch (NumberFormatException nfe) {
		logger.error("Bad syntax - first param is annealing time, in seconds");
		return;
		}

	SystemDesign sd = new SystemDesign();

	sd.writeENDataFiles_ = true;	// output EN-style data so Marc can play with it

	boolean doVerify = false;
	boolean doRepeat = false;
	boolean testWithPlugin = false;

	int testCase = 4;	// test case 4 is most useful 
						//   LoadBalanceRequest.SOLVER_MODE_BLEND_PFAIL_LOAD_BALANCE
	int maxRuns  = 100;					// override with "-r {n}"
	String killedListArg = null;

	for (int i=0; i<args.length; i++) {

		if ("-r".equals(args[i])) {
			doRepeat = true;
			if (i <args.length-1) { // -r {n} ?
				try {
					maxRuns = Integer.parseInt(args[i+1]);
					if (maxRuns<1) {
						logger.error("Bad value on -r: an integer >0 please! ");					
						}
					}
				catch (NumberFormatException nfe) {
					logger.error("Bad syntax on -r: an integer >0 please!");
					return;
					}
				}
			}

		if ("-v".equals(args[i])) {
			doVerify = true;
			}

		if ("-t".equals(args[i]) && i<(args.length-1)) {
			try {
				testCase = Integer.parseInt(args[i+1]);
				if (testCase<0 || testCase>10) {
					logger.error("Bad value on -t: an integer 1...10 please! ");					
					}
				}
			catch (NumberFormatException nfe) {
				logger.error("Bad syntax on -t: an integer 1...10 please!");
				return;
				}
			}

		// 'killed nodes' list?
		if ("-k".equals(args[i]) && i<(args.length-1)) {
			killedListArg = args[i+1];
			i++;
			}

		if ("-p".equals(args[i])) {
			testWithPlugin = true;
			}

		if ("-l".equals(args[i]) ) {
			if (i>=(args.length-1)) {
				logger.error("Bad syntax on -l: an integer 1..." + Logger.FATAL + " please!");
				return;
				}
			try {
				logLevel = Integer.parseInt(args[i+1]);
				if (logLevel < Logger.DETAIL || logLevel > Logger.FATAL)
					throw new NumberFormatException("Value '" + args[i+1] + " out of bounds!");
				logger.setLogLevel(logLevel);
				i++;
				}
			catch (NumberFormatException nfe) {
				logger.error("Bad syntax on -l: an integer 1..." + Logger.FATAL + " please!");
				return;
				}

			}

		} // end scan of args

	// Echo the options
	logger.info("Test case: " + testCase);
	logger.info("   Verify: " + doVerify);
	logger.info("   Repeat: " + (doRepeat?(maxRuns + " times"):"false"));
	logger.info("   Killed: " + ((killedListArg==null)?"":killedListArg));
	logger.info("      Log: " + logLevel);


	// Need a List of killed nodes.
	//
	ArrayList killedNodes = new ArrayList();
	if (killedListArg != null) {

		if (testWithPlugin == false) {
			logger.error("If killed list specified (-k), must use plugin!");
			return;
			}

		StringTokenizer st = new StringTokenizer(killedListArg, ",");
		while (st.hasMoreTokens()) {
			killedNodes.add(st.nextToken());
			}
		logger.info("   Killed: " + killedNodes);
		}


	if (!doRepeat)
		maxRuns = 1;

	// read EN config files? or XML?
	//
	if (args[1].equals("-en")) {	// EN input data

		if (args.length < 3) {
			logger.error("Bad syntax - need EN input file base name!");
			return;
			}
		String baseName = args[2];
		RnR.readNodes    (sd, baseName + ".nodedef");	// nodes have to be read first!
		RnR.readFunctions(sd, baseName + ".functiondef");
		RnR.readLinks    (sd, baseName + ".linkdef");

		// gotta fix objfnjobset (only used by hamming test cases?)
		sd.objfnjobset.numobjfn = sd.numfunction;
		for (int i=1; i<=sd.numfunction; i++) {
			sd.objfnjobset.initfn[i] = i;
			}

		}
	else {							// XML input data

		// If there's non-null test data in XML, use it.
		// This is used by the -internal option
		//
		if (inputSociety_ == null) {
	
			// read XML
	
			EN4JTestServlet tester = new EN4JTestServlet();
			String xmlFilePath = args[1];
	
			// Create a CougaarSociety
			//
			originalSociety = tester.loadSocietyFromXML(xmlFilePath);
			if (originalSociety == null) {
				logger.error("Error creating test data! Check command syntax and input files?");
				return;
				}

			originalSocID_ = originalSociety.getIdentifier();

	//		Iterator itNodes = originalSociety.getNodes();
	//		while (itNodes.hasNext()) {
	//			CougaarNode node = (CougaarNode)itNodes.next();
	//			SystemDesign.logInfo("node: " + node.getName());
	//			}
	
			if (sd.loadDataFromSociety(originalSociety) == false) {
				logger.error("Error in loadDataFromSociety!");
				return;
				}
			} // inputSociety_ == null

		} // read XML (or parse internal string)


	int[] requestedHammingHosts = null;
	boolean doHamming = false;

	switch (testCase) {

		// my own ad-hoc hardcoded test case
		case 0:
		
			sd.setSolverMode_BlendPfailLoadBalance(true);

			// just for testing the LBViz, generate some 'before' data
			// smear the agents around evenly, ignoring the router
			logger.info("Test case 0: setSolverMode_BlendPfailLoadBalance(true)");
			for (int i=1; i<=sd.numfunction; i++) {
				sd.agent[i].hamminghostid = ((i-1) % (sd.numnode-1)) + 1;
				}

			break;


		// Test Minimum System Failure
		//
		case 1:
			logger.info("Test case 1: Minimize system failure probability");
			logger.info("");

			// Just for testing visualization, generate some 'before' data.
			// Smear the agents around evenly, ignoring the router
			logger.info("Test case 1: Generating bogus 'before' data...");
			for (int i=1; i<=sd.numfunction; i++) {
				sd.agent[i].hamminghostid = ((i-1) % (sd.numnode-1)) + 1;
				}

			sd.setSolverMode_MinPfail(doHamming);
			break;

		// Case 1 plus hamming
		//
		case 7:
			logger.info("Test case 7: Case 1 plus hamming");
			logger.info("");

			// Get testcase hamming IDs
			requestedHammingHosts = hammingIDsforTestCase(sd, testCase);
			if (requestedHammingHosts == null)
				return;	// bad test case? hammingIDsforTestCase() will have complained

			for (int i=1; i<=sd.numfunction; i++) {
				sd.agent[i].hamminghostid = requestedHammingHosts[i];
				}
	
			logger.info("Setting setSolverMode_MinPfail(true)   <--= SUFFICIENT???");
			doHamming = true;
			sd.setSolverMode_MinPfail(doHamming);
			break;


		// Test Minimum System Failure and MinUltralogNodes
		//
		case 2:
			logger.info("Test case 2: Minimize system failure probability, with MinUltralogNodes=6");
			logger.info("Solving with min nodes = 6");
			logger.info("");
			
			sd.setSolverMode_MinPfailWithMinNodeCount(6, false);

			break;

		// Load Balancing Risk
		//
		case 3:
			logger.info("Test case 3: Minimize load balancing risk");
			logger.info("");
	
			sd.setSolverMode_LoadBalance(false);

			break;


		case 10: // 4+9=10: Hamming + 4: Mixed Strategy: System Failure and Load Balanced Risk
			
			// just for testing the LBViz, generate some 'before' data
			// smear the agents around evenly, ignoring the router
			logger.info("Test case 10: Mixed Strategy: Minimize system failure and load balanced risk, with hamming");
			for (int i=1; i<=sd.numfunction; i++) {
				sd.agent[i].hamminghostid = ((i-1) % (sd.numnode-1)) + 1;
				}
			sd.setSolverMode_BlendPfailLoadBalance(true);

			break;

		
		// Mixed Strategy: System Failure and Load Balanced Risk
		//
		case 4:
			logger.info("Test case 4: Mixed Strategy: Minimize system failure and load balanced risk");
			logger.info("");
			
			sd.setSolverMode_BlendPfailLoadBalance(false);

			break;
		
		// Minimize Remote Messaging
		//
		case 5:
			logger.info("Test case 5: Minimize remote messaging");
			logger.info("");
			
			sd.setSolverMode_MinMessaging(false);
		
//			logger.info("Test case not implemented yet!");
//			return;
			break;

		// Blend Pfail and Messaging
		//
		case 6:
			logger.info("Test case 6: Minimize system failure and messaging");
			logger.info("");
			logger.info("Test case not implemented yet!");
			break;

		} // switch (testCase)


	// if we're looping, always verify
	if (doRepeat)
		doVerify = true;


	if (sd.checkProject() == false) {
		logger.error("Data fails checkProject!");
		return;
		}
	logger.info("checkProject OK!");

	logger.info("Annealing time " + annealTime + " seconds");

	int repeatCount = 0;
	int failureCount = 0;

	EN4JPlugin en4jplugin;

	while (true) {

		repeatCount++;
		if (sd.isInfoEnabled()) {
			logger.info("");
			logger.info("************** Start of test run #" + repeatCount + " of " + maxRuns);
			}

		if (testWithPlugin) {
			en4jplugin = new EN4JPlugin();
			en4jplugin.test(originalSociety, killedNodes, logger);

			// THIS is what we should be testing against! 24mar04
			//
			CougaarSociety lastSoln = en4jplugin.getLastSolvedCS();

			}
		else {
			if (sd.runConfig(annealTime) == false) {
				if (sd.isErrorEnabled())
					logger.error("Cannot find annealing solution?? (SystemDesign.runConfig returned false)!");
				return;   
				}
			}


		// Save the last run SD for test test harness to look at.
		//
		lastSD_ = sd;

//
//	// This is just to test SystemDesign.cleanTempNodes
//		// If we started with XML, do this cleanup thing of temp node, if any.
//		if (originalSociety != null) {
//			CougaarSociety newSociety = SystemDesign.cleanTempNodes(originalSociety);
//			
//			System.out.println("============================================================");
//			System.out.println("" + newSociety.toXML());
//			System.out.println("============================================================");
//
//			}
//

		// Verify results for test case?
		//
		if (doVerify) {

			if (sd.isInfoEnabled()) {
				logger.info("");
				logger.info("Verifying test case " + testCase + "....");
				}
			boolean verified = false;
			
			switch (testCase) {

				case 1:
					if (SystemDesignTest.testCaseA1_1(sd)) {
						verified = true;
						if (sd.isInfoEnabled())
							logger.info("Verify test case A1 #1 OK!                                                     <--= :)");
						}
					else {
						if (sd.isErrorEnabled())
							SystemDesign.logError("Verify test case A1 #1 FAILS!                                                 <--= :(");
						failureCount++;
						}
					break; // case 1

				case 2:

					if (SystemDesignTest.testCaseA1_2(sd)) {
						verified = true;
						if (sd.isInfoEnabled()) 
							logger.info("Verify test case A1 #2 OK!                                                     <--= :)");
						}
					else {
						if (sd.isErrorEnabled())
							SystemDesign.logError("Verify test case A1 #2 FAILS!                                                 <--= :(");
						failureCount++;
						}
					break; // case 2
					
					
				case 3:
				
					if (SystemDesignTest.testCaseA1_3(sd)) {
						verified = true;
						if (sd.isInfoEnabled()) 
							logger.info("Verify test case A1 #3 OK!                                                     <--= :)");
						}
					else {
						if (sd.isErrorEnabled())
							SystemDesign.logError("Verify test case A1 #3 FAILS!                                                 <--= :(");
						failureCount++;
						}
					break;

				case 4:
				
					if (SystemDesignTest.testCaseA1_4(sd)) {
						verified = true;
						if (sd.isInfoEnabled()) 
							logger.info("Verify test case A1 #4 OK!                                                     <--= :)");
						}
					else {
						if (sd.isErrorEnabled())
							SystemDesign.logError("Verify test case A1 #4 FAILS!                                                 <--= :(");
						failureCount++;
						}
					break;


				case 7:

					if (SystemDesignTest.testCaseA1_7(sd, requestedHammingHosts, 95)) {
						verified = true;
						if (sd.isInfoEnabled()) 
							logger.info("Verify test case A1 #7 OK!                                                     <--= :)");
						}
					else {
						if (sd.isErrorEnabled())
							SystemDesign.logError("Verify test case A1 #7 FAILS!                                                 <--= :(");
						failureCount++;
						}
					break; // case 7


				default:
					if (sd.isErrorEnabled())
						SystemDesign.logError("Test case #" + testCase + " CANNOT BE CHECKED YET! EYEBALL IT!!");
					break;
					
				} // switch test case

//			if (verified) {
				logger.info(" pfailuresurvive: " + sd.pfailuresurvive);
				logger.info("     pfailureave: " + sd.pfailureave);
				logger.info("    objectiveave: " + sd.objectiveave);
				logger.info("    hamming dist: " + sd.hamming);

				// not so meaningful?
//				logger.info("     pfailuremin: " + sd.pfailuremin);
//				logger.info("     pfailuremax: " + sd.pfailuremax);
//				logger.info("      annealtemp: " + sd.annealtemp);
//				logger.info("       cpufactor: " + sd.cpufactor);

//				}

			} // doVerify

		if (doRepeat == false)
			break;

		if (sd.isInfoEnabled()) {
			logger.info(" !");
			logger.info(" ! "+ new java.util.Date());
			logger.info(" ! Test case #" + testCase + "; anneal time " + annealTime + " seconds");
			logger.info(" ! " + failureCount + " of " + repeatCount + " test runs failed");
			logger.info(" !");
			}

		if (repeatCount >= maxRuns)
			break;

		} // while true

	if (sd.isInfoEnabled()) {
		logger.info("");
		logger.info("End test suite.");
		logger.info("");
		}


	if (createLBPicture_) {
		String filename = "loadBalance";
	
		try {

			LBVizGraphic control = new LBVizGraphic(true);	// show message traffic
			if (control.USE_JPEG_ENCODING) {
				filename += ".JPEG";
				}
			else {
				filename += ".PNG";
				}

			// cs1 is null if we're loading EN data, must re-construct it.
			//
			CougaarSociety cs1 = originalSociety;
			if (cs1 == null) {
				cs1 = sd.getCSForCurrentData(true, false);
				}

			cs1 = sd.cleanTempNodes(cs1);
			System.out.println("------------- society 'before' -------------");

			// cleanTempNodes steps on the ID (it shouldn't but it does); restore it
		 	cs1.setIdentifier(originalSocID_);

			System.out.println(cs1.toXML());
			System.out.println("----------- end society 'before' -----------");


			// Process the result soc
			//
			CougaarSociety cs2 = sd.getCSForCurrentData(false, true);
			cs2 = sd.cleanTempNodes(cs2);	// if empty temp node, remove it
			cs2.setIdentifier("Cleaned solution by SystemDesignTest, " + new java.util.Date());
			System.out.println("------------- society 'after' -------------");
			System.out.println(cs2.toXML());
			System.out.println("----------- end society 'after' -----------");

// output new XML?
			writeXMLToFile("SDTestResults.xml", cs2.toXML());
			
			java.util.ArrayList problems = control.loadBeforeAndAfterData(cs1, cs2);

			if (problems != null) {
				if (sd.isErrorEnabled()) {
					java.util.Iterator pIter = problems.iterator();
					while (pIter.hasNext()) {
						SystemDesign.logError((String)pIter.next());
						}
					}
				}
				
//			control.makeTestData(9);

			control.setTitle("Test case #" + testCase + "; anneal time " + annealTime + " seconds",							 300, 16);

			// "viz-u-lize it, yeah, yeah...."
			FileOutputStream f = new FileOutputStream(filename);
			control.createImage(f);
			f.close();
			if (sd.isInfoEnabled())
				logger.info("Created file '" + filename + "'!");
			}
		catch (Exception e) {
			if (sd.isInfoEnabled())
				SystemDesign.logError("SystemDesignTest.doTest: error: ", e);
			}
		}
		
	} // doTest


private static void
writeXMLToFile(String filename, String theXML) {
	
	try {
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		pw.write(theXML);
		pw.close();
		}
	catch (IOException ioe) {
		ioe.printStackTrace();
		}
	}

/**
 * Verify that the given SystemDesign is a proper solution
 *  to test case A1, scenario #1. <P>
 * (see en1.1-testplan-072103) <P>
 * 
 *		Input Files: A1.nodedef, A1.functiondef, A1.linkdef <BR>
 *		Output Files: A1.test1.state, A1.test1.hamming<BR>
 *		MinUltralogNodes = 1<BR>
 *
 * Success is defined as: <P>
 *
 *		Output: Avoids FWD-A (pfail=0.5) and FWD-B (pfail=0.8), 
 *		and then uses all other nodes. <P>
 * 
 * Other output values to look at: <P>
 * 
 *		System Pfail = 0.586   (min 0.585, max 0.959)<BR>
 *		Objective = 1.17<BR>
 *	
 *		System Pfail = 0.586   (min 0.586, max 0.959)<BR>
 *		Survivability = 3.2   (2.8, 13.6)<BR>
 *		Remote Traffic = 685   (485, 740)<BR>
 *		hamming = NA<BR>
 *
 * @param sd The proposed solution
 * @return true iff proposed solution satisfies success conditions (see above)
 */
public static boolean
testCaseA1_1(SystemDesign sd) {

	// Ensure "FWD-A" and "FWD-B" have no agents on them

	// Find host ids for these guys...
	// (acutally, we know they are node #1 and #6, but let's pretend we don't)
	//
	int nodeIDFWDA = -1, nodeIDFWDB = -1;
	for (int i=1; i<=sd.numnode; i++) {
		if (sd.nodeinfo[i].name.equals("FWD-A")) 
			nodeIDFWDA = i;
		if (sd.nodeinfo[i].name.equals("FWD-B")) 
			nodeIDFWDB = i;
		}
	if (nodeIDFWDA == -1 || nodeIDFWDB == -1) {
		SystemDesign.logError("Verify error: can't find node FWD-A and/or FWD-B?!");
		return false;
		}
	SystemDesign.logInfo("FWD-A host ID: " + nodeIDFWDA + ", FWD-B: " + nodeIDFWDB);

	// ...now make sure no annealing host is them.
	// (also count agents per node, so we can count zero-agent nodes)
	//
	int agentCountPerNode[] = new int[sd.numnode+1];
	for (int i=1; i<=sd.numfunction; i++) {
		if (sd.agent[i].annealhostid == nodeIDFWDA) {
			SystemDesign.logError("FAILS: Node FWD-A is used by agent #" + i);
			return false;		
			}
		if (sd.agent[i].annealhostid == nodeIDFWDB) {
			SystemDesign.logError("FAILS: Node FWD-B is used by agent #" + i);
			return false;	
			}
		
		// also accumulate count of agents per node
		agentCountPerNode[sd.agent[i].annealhostid]++;
		}

	int nodesWithAgents = 0;
	for (int i=1; i<=sd.numnode; i++) {
		if (sd.nodeinfo[i].type == Node.TYPE_SERVER) {
			if (agentCountPerNode[i] > 0) {
				nodesWithAgents++;
				}
			SystemDesign.logInfo("node " + i + " has " + agentCountPerNode[i] + " agents");
			}
		else {
			SystemDesign.logInfo("(node " + i + " is the router)");		
			}
		}
	SystemDesign.logInfo("nodesWithAgents: " + nodesWithAgents);
	if (nodesWithAgents != 5) {
		SystemDesign.logError("WRONG NUMBER OF NODES IN USE: " + nodesWithAgents);
		return false;
		}
	
	return true;
	}


/**
 * Verify that the given SystemDesign is a proper solution 
 *  to test case A1, scenario #2. <P>
 * (see en1.1-testplan-072103) <P>
 * 
 * 
 * 	Input Files: A1.nodedef, A1.functiondef, A1.linkdef
 *	Output Files: A1.test2.state, A1.test2.hamming
 *	MinUltralogNodes = 6 (There are 7 nodes total in the system)
 *
 * Success is defined as <P>
 *	Output: Avoids FWD-B (pfail=0.8), and then uses all other nodes.
 *
 * Other output values to look at: <P>
 *	System Pfail = 0.793   (0.586, 0.959)
 *	Objective = 1.58
 *	
 *	System Pfail = 0.793   (0.586, 0.959)
 *	Survivability = 9.5   (3.0, 15.2)
 *	Remote Traffic = 575   (457, 755)
 *	hamming = NA
 *
 *
 * @param sd The proposed solution
 * @return true iff proposed solution satisfies success conditions (see above)
 */
public static boolean
testCaseA1_2(SystemDesign sd) {

	// Ensure "FWD-B" has no agents on it

	// Find host id for the node...
	//
	int nodeIDFWDB = -1;
	for (int i=1; i<=sd.numnode; i++) {
		if (sd.nodeinfo[i].name.equals("FWD-B")) 
			nodeIDFWDB = i;
		}
	if (nodeIDFWDB == -1) {
		SystemDesign.logError("Verify error: can't find node FWD-B?!");
		return false;
		}
	SystemDesign.logInfo("FWD-B host ID: " + nodeIDFWDB);

	// ...now make sure no annealing host is him.
	//
	// (also count agents per node, so we can count zero-agent nodes)
	//
	int agentCountPerNode[] = new int[sd.numnode+1];
	for (int i=1; i<=sd.numfunction; i++) {
		if (sd.agent[i].annealhostid == nodeIDFWDB) {
			SystemDesign.logError("FAILS: Node FWD-B is used by agent #" + i);
			return false;	
			}
		// also accumulate count of agents per node
		agentCountPerNode[sd.agent[i].annealhostid]++;
		}

	int nodesWithAgents = 0;
	for (int i=1; i<=sd.numnode; i++) {
		if (sd.nodeinfo[i].type == Node.TYPE_SERVER) {
			if (agentCountPerNode[i] > 0) {
				nodesWithAgents++;
				}
			SystemDesign.logInfo("node " + i + " has " + agentCountPerNode[i] + " agents");
			}
		else {
			SystemDesign.logInfo("(node " + i + " is the router)");		
			}
		}
	SystemDesign.logInfo("nodesWithAgents: " + nodesWithAgents);
	if (nodesWithAgents != 6) {
		SystemDesign.logError("WRONG NUMBER OF NODES IN USE: " + nodesWithAgents);
		return false;
		}
	

	return true;
	}


/**
 * Verify that the given SystemDesign is a proper solution 
 *  to test case A1, scenario #3. <P>
 * (see en1.1-testplan-072103) <P>
 *
 * @param sd The proposed solution
 * @return true iff proposed solution satisfies success conditions (see above)
 */
public static boolean
testCaseA1_3(SystemDesign sd) {

	if (sd.isInfoEnabled()) {

		SystemDesign.logInfo("NOT SURE HOW TO VERIFY CASE 3; EYEBALL THE NUMBERS?");
		SystemDesign.logInfo("  \"All Nodes Used, balanced according to pfail(inod) * numagents(inod)\"");
		SystemDesign.logInfo("  ie, nodes 1 and 6 should have fewer agents");
		}
	
	// display total number of hosts per node
	// nodes 1 and 6 should have fewer agents
	int agentCountPerNode[] = new int[sd.numnode+1];
	for (int i=1; i<=sd.numfunction; i++) {
		agentCountPerNode[sd.agent[i].annealhostid]++;
		}

//	int nodesWithAgents = 0;
	for (int i=1; i<=sd.numnode; i++) {
		if (sd.nodeinfo[i].type == Node.TYPE_SERVER) {
//			if (agentCountPerNode[i] > 0) {
//				nodesWithAgents++;
//				}
			SystemDesign.logInfo("node " + i + " has " + agentCountPerNode[i] + " agents");
			}
		else {
			SystemDesign.logInfo("(node " + i + " is the router)");		
			}
		}
//	SystemDesign.logInfo("nodesWithAgents: " + nodesWithAgents);

	return true;
	}


/**
 * Verify that the given SystemDesign is a proper solution 
 *  to test case A1, scenario #4. <P>
 * (see en1.1-testplan-072103) <P>
 *
 * @param sd The proposed solution
 * @return true iff proposed solution satisfies success conditions (see above)
 */
public static boolean
testCaseA1_4(SystemDesign sd) {

	if (sd.isInfoEnabled()) {
		SystemDesign.logInfo("TEST CASE 4 -> USING TEST CASE 1 FOR VERIFICATION....");
		SystemDesign.logInfo("NOT SURE HOW TO VERIFY CASE 4; EYEBALL THE NUMBERS?");
		}
	return testCaseA1_1(sd);
	}


/**
 * Verify that the given SystemDesign is a proper solution
 *  to test case A1, scenario #7. <P>
 * (see en1.1-testplan-072103) <P>
 * 
	blah blah blah
 *
 * @param sd 				The proposed solution
 * @param requestedHIDs		array of original host ids to compare to
 * @param successThreshold	percentage (>0, <=100) threshold for success
 * 
 * @return true iff proposed solution satisfies success conditions (see above)
 */
public static boolean
testCaseA1_7(SystemDesign sd, int[] requestedHIDs, int successThreshold) {

	// Solution must still satisfy test case #1!
	//
	if (testCaseA1_1(sd) == false) {
		return false;
		}

	int hits = 0;	// hammging=anneal?

	int agentCountPerNode[] = new int[sd.numnode+1];
	for (int i=1; i<=sd.numfunction; i++) {
		
//		SystemDesign.logInfo("host f[" + i + "]=" + sd.agent[i].annealhostid +
//								", reqested=" + requestedHIDs[i]);

		if (sd.agent[i].annealhostid  == requestedHIDs[i])
			hits++;

		// also accumulate count of agents per node
		agentCountPerNode[sd.agent[i].annealhostid]++;
		}

	int percentReused = 100*hits/sd.numfunction;

	SystemDesign.logInfo(" *** satisfied " + hits + " of " + sd.numfunction + " (" + percentReused + "%)");
	
	return (percentReused >= successThreshold);
	}


/**
 * return 1-based array of hamming IDs for each hamming test case (7-12).
 * 
 * @param testCase
 * @param numagents
 * @return
 */
private static int[]
hammingIDsforTestCase(SystemDesign sd, int testCase) {

	int[] result = null;

	switch (testCase) {

		// For case 7, eturn the annealing host IDs created in a previous run of test case 1
		//
		case 7:

			result = new int[sd.numfunction+1];

			StringTokenizer lines = new StringTokenizer(testCase1HammingInfo, "\n");
			for (int i=1;i<=sd.numfunction; i++) {
				String line = lines.nextToken();
				StringTokenizer tokens = new StringTokenizer(line, " \t\n");
				int index = Integer.parseInt(tokens.nextToken());
				if (index != i) {
					if (sd.isErrorEnabled()) 
						SystemDesign.logError("hammingIDsforTestCase: bad data for " + testCase);
					return null;					
					}
				tokens.nextToken(); // toss the agent name
				result[i] = Integer.parseInt(tokens.nextToken());
				
				}

				// Set an agent's hamming node to...
				//  something other than what it really should be.
				//
				// Need to relax the pass/fail standard if you keep this in.
				//
				if (false) {
					if (sd.isWarnEnabled()) 
						SystemDesign.logWarn("(OK) hammingIDsforTestCase: TESTING SLEDGEHAMMER TWEAK...");
					corruptHammingInfoCase7(result);
					}
				
			break; // case 7

		default:
			if (sd.isErrorEnabled()) 
				SystemDesign.logError("hammingIDsforTestCase: Unsupported test case: " + testCase);
			break;  // default
		}

	return result;
	}


/**
 * Tweak the host mapping so that the first 10 agents want to be on nodes that no longer exist.
 * Specifically, zet to what???
 * 
 * 25AUG03 - ??? - MB says the real solution (a workaround) 
 * is to create an unused/unusable node to place the hammered agents on.
 *
 * @param data
 */
private static void
corruptHammingInfoCase7(int[] data) {
	
	for (int i=1; i<=10; i++)
		data[i] = 1;
	}


// Data for hammingIDsforTestCase(7)
// Output from a run of test case 1.
//
	private static String testCase1HammingInfo = 
		"1 1-37-ARBN 7 \n"+
		"2 501-FSB 2 \n"+
		"3 2-37-ARBN 2\n"+
		"4 16-ENGBN 3\n"+
		"5 1-BDE-1-AD 7\n"+
		"6 2-3-FABN 2\n"+
		"7 1-36-INFBN 4\n"+
		"8 Enclave2ScalabilityManager 5\n"+
		"9 1-35-ARBN 3\n"+
		"10 47-FSB 7\n"+
		"11 1-6-INFBN 3\n"+
		"12 4-27-FABN 2\n"+
		"13 2-6-INFBN 5\n"+
		"14 40-ENGBN 2\n"+
		"15 2-BDE-1-AD 7\n"+
		"16 16-CSG 3\n"+
		"17 26-SSCO 4\n"+
		"18 3-BDE-1-AD 7\n"+
		"19 485-CSB 7\n"+
		"20 4-1-FABN 4\n"+
		"21 AVNBDE-1-AD 2\n"+
		"22 592-ORDCO 7\n"+
		"23 77-MAINTCO 2\n"+
		"24 2-70-ARBN 3\n"+
		"25 588-MAINTCO 2\n"+
		"26 596-MAINTCO 4\n"+
		"27 1-501-AVNBN 4\n"+
		"28 238-POL-TRKCO 4\n"+
		"29 18-MAINTBN 7\n"+
		"30 125-FSB 3\n"+
		"31 226-MAINTCO 7\n"+
		"32 263-FLDSVC-CO 4\n"+
		"33 102-POL-SUPPLYCO 4\n"+
		"34 594-MDM-TRKCO 4\n"+
		"35 106-TCBN 4\n"+
		"36 127-DASB 7\n"+
		"37 70-ENGBN 3\n"+
		"38 2-501-AVNBN 7\n"+
		"39 1-41-INFBN 3\n"+
		"40 372-CGO-TRANSCO 3\n"+
		"41 1-1-CAVSQDN 7\n"+
		"42 1-13-ARBN 7\n"+
		"43 227-SUPPLYCO 4\n"+
		"44 15-PLS-TRKCO 2\n"+
		"45 69-CHEMCO 7\n"+
		"46 DIVARTY-1-AD 4\n"+
		"47 1-AD 3\n"+
		"48 DISCOM-1-AD 7\n"+
		"49 501-MPCO 7\n"+
		"50 123-MSB 4\n"+
		"51 1-94-FABN 7\n"+
		"52 501-MIBN-CEWI 4\n"+
		"53 1-4-ADABN 3\n"+
		"54 25-FABTRY-TGTACQ 3\n"+
		"55 141-SIGBN 4\n"+
		"56 1-AD-DIV 3";




//	************************************** OBSOLETE 
//	************************************** OBSOLETE 
//	************************************** OBSOLETE 


/**

	Create and run a simple (simplest?) test:	
	   Two identical servers - NodeA and NodeB
	   Two identical agents - AgentA and AgentB
	   Only one agent will fit on a node (each uses 75% of a nodes memory, 75% of CPU)

	 **** Doesn't automatically perform check on result - you gotta eyeball it.

**/
public boolean
doTestA(SystemDesign sd) {

	SystemDesign.logInfo("Running test 'testA'....");

   // Create some Node data
   //
   sd.numnode = 3;
   sd.nodeinfo[1] = new Node("NodeA",  Node.TYPE_SERVER, 1, 1, 100);

   SystemDesign.logWarn("  **** NODE[1].NAME #1: " + sd.nodeinfo[1].name);
   SystemDesign.logWarn("  **** NODE[1].MEMORY #1: " + sd.nodeinfo[1].memory);

   sd.nodeinfo[2] = new Node("Router", Node.TYPE_ROUTER, 1, 1,  50);
   sd.nodeinfo[3] = new Node("NodeB",  Node.TYPE_SERVER, 1, 1, 100);


   // Create some Link data
   //
   sd.numlink = 4;
   sd.linkinfo[1] = new Link("A->Router", Link.LINK_TYPE_WAN, 1, 2);
   sd.linkinfo[2] = new Link("Router->A", Link.LINK_TYPE_WAN, 2, 1);
   sd.linkinfo[3] = new Link("B->Router", Link.LINK_TYPE_WAN, 2, 3);
   sd.linkinfo[4] = new Link("Router->B", Link.LINK_TYPE_WAN, 3, 2);


   // Create some "function" (agent) data
   //
   sd.numfunction = 2;

   // Agent A
   //
   sd.xnfunction[1] = new Function("AgentA");
   sd.xnfunction[1].felig[1] = 1;			// eligible on this node where it:
   sd.xnfunction[1].cpurate[1] = 75;		// 	uses 75% CPU
   sd.xnfunction[1].memory[1] = 75;		// 	uses 75% RAM 

   sd.xnfunction[1].felig[3] = 1;			// eligible on this node where it:
   sd.xnfunction[1].cpurate[3] = 75;		// uses 75% CPU
   sd.xnfunction[1].memory[3] = 75;		// uses 75% RAM 

   sd.xnfunction[1].fcall[2].sendmsgrate = 100f;	// 1 talks to 2

   // Agent B
   //
   sd.xnfunction[2] = new Function("AgentB");
   sd.xnfunction[2].felig[1] = 1;			// eligible on this node where it:
   sd.xnfunction[2].cpurate[1] = 75;		//  uses 75% CPU
   sd.xnfunction[2].memory[1] = 75;		//  uses 75% RAM

   sd.xnfunction[2].felig[3] = 1;			// eligible on this node where it:
   sd.xnfunction[2].cpurate[3] = 75;		//  uses 75% CPU
   sd.xnfunction[2].memory[3] = 75;		//  uses 75% RAM

   sd.xnfunction[2].fcall[1].sendmsgrate = 100f;	// and 2 talks to 1


   // "objective function" data - not sure about this.
   sd.objfnjobset.numobjfn = 2;
   sd.objfnjobset.initfn[1] = 1;	// agents are only grouped with themselves
   sd.objfnjobset.initfn[2] = 2;

   // nor exactly what an Agent is, as opposed to a function
   sd.agent[1] = new Agent();
   sd.agent[1].currenthostid = 1;
   sd.agent[1].testhostid = 1;

   sd.agent[2] = new Agent();
   sd.agent[2].currenthostid = 1;
   sd.agent[2].testhostid = 1;


	if (sd.runConfig() == false) {
		return false;
		}

	// Test for success/failure - how?

	return true;
	} // doTestA



/**
	Process EN input files {baseName}.nodedef, etc.
**/
public boolean
doTestB(SystemDesign sd, String baseName) {

	SystemDesign.logInfo("SystemDesign.testB: Loading '" + baseName + "' data files....");

	// Load the data
	//
	RnR.readNodes(sd, baseName + ".nodedef");
	RnR.readLinks(sd, baseName + ".linkdef");
	RnR.readFunctions(sd, baseName + ".functiondef");

//	if (debugData_) {
//		SystemDesign.logInfo("SystemDesign.testB: debugData_ true, NOT annleaing or testing!");
//		return true;
//	}

	// Run the configuration.
	//
	sd.runConfig();

	// Return the test results, success or failure.
	//
	return verifyTestB(sd);
	}


/**
 * 
 * Verify the results of TestB.
 * Ensure that:
 *      - "Agent8" and "Agent9" are together on one of the nodes;
 *      - There is just one agent on each of the other nodes.
 * 
**/
public boolean 
verifyTestB(SystemDesign sd) {

	int[] agentsPerHost = new int[sd.numnode+1];
 
	if (sd.isDebugEnabled())
		SystemDesign.logDebug(".verifyTestB: Agent/Host annealing assignments:");
  
	for (int i=1; i<=sd.numfunction; i++) {
		if (sd.isDebugEnabled())
			SystemDesign.logDebug("  f " + i + ": " + sd.xnfunction[i].name + " -> Host "
				 + sd.agent[i].annealhostid);
		agentsPerHost[sd.agent[i].annealhostid]++;
		}

	int numberOfHostsWithTwoAgents = 0;
	int indexOfHostWithTwoAgents = 0;
	for (int i=1; i<=sd.numnode; i++) {
		if (sd.nodeinfo[i].type == Node.TYPE_SERVER) {

			int agentsThisHost = agentsPerHost[i];

			// Cannnot be fewer than one agent on each host...
			//
			if (agentsThisHost < 1) {
				SystemDesign.logWarn(".verifyTestB: Fails test: <1 agent on node " + i);
				return false;
			}

			// ...nor more than two agents...
			//
			else if (agentsThisHost > 2) {
				SystemDesign.logWarn(".verifyTestB: Fails test: >2 agents on node " + i);
				return false;
			}

			// ...and there should be only one node with exactly two agents. Count 'em.
			else
			if (agentsThisHost == 2) {
				numberOfHostsWithTwoAgents++;
				indexOfHostWithTwoAgents = i;
			}

		} // server
	}
	if (numberOfHostsWithTwoAgents != 1) {
		SystemDesign.logWarn(".verifyTestB: Fails test: Number of nodes with two agents: " + numberOfHostsWithTwoAgents + " (should be 1)!");
		return false;
	}

	// Ensure that Agent8 and Agent9 are together on host[indexOfHostWithTwoAgents].
	//
	for (int i=1; i<=sd.numfunction; i++) {
		if ("Agent8".equals(sd.xnfunction[i].name)  && "Agent9".equals(sd.xnfunction[i].name)) {
			if (sd.agent[i].annealhostid != indexOfHostWithTwoAgents) {
				SystemDesign.logWarn(".verifyTestB: Fails test: " + sd.xnfunction[i].name 
						+ " isn't on node " + indexOfHostWithTwoAgents);
				return false;
			}
		}
	}

//	   FYI/debug:
//		logInfo("SystemDesign.testB: calls to TraceFunctionForm.agentRun: " + TraceFunctionForm.calls_);


		SystemDesign.logInfo(".verifyTestB: Passes test 'TestB'!");

	return true;
	}


/**
	Big test, checks for correctness of result.
**/
public boolean
doTestC(SystemDesign sd, String baseName) {

	SystemDesign.logInfo("SystemDesign.testC: Loading '" + baseName + "' data files....");

	// Load the data
	//
	RnR.readNodes(sd, baseName + ".nodedef");
	RnR.readLinks(sd, baseName + ".linkdef");
	RnR.readFunctions(sd, baseName + ".functiondef");

	// Run the configuration.
	//
	sd.runConfig(30);

	// Display the laydown.
	//
	SystemDesign.logInfo("SystemDesign.testC: Agent/Host annealing assignments:");
	for (int i=1; i<=sd.numfunction; i++) {
		SystemDesign.logInfo("  f " + i + ": " + sd.xnfunction[i].name + " -> Host " + sd.agent[i].annealhostid);
	}


	// All the rest of this is testing the results!
	//
	// 
	int[] agentsPerHost = new int[sd.numnode+1];
	SystemDesign.logInfo("SystemDesign.testB: Agent/Host annealing assignments:");

	for (int i=1; i<=sd.numfunction; i++) {
		SystemDesign.logInfo("  f " + i + ": " + sd.xnfunction[i].name + " -> Host " + sd.agent[i].annealhostid);
		agentsPerHost[sd.agent[i].annealhostid]++;
	}

	int numberOfHostsWithTwoAgents = 0;
	int indexOfHostWithTwoAgents = 0;
	for (int i=1; i<=sd.numnode; i++) {
		if (sd.nodeinfo[i].type == Node.TYPE_SERVER) {

			int agentsThisHost = agentsPerHost[i];

			// Cannnot be fewer than one agent on each host...
			//
			if (agentsThisHost < 1) {
				SystemDesign.logWarn("SystemDesign.testB: Fails test: <1 agent on node " + i);
				return false;
			}

			// ...nor more than two agents...
			//
			else if (agentsThisHost > 2) {
				SystemDesign.logWarn("SystemDesign.testB: Fails test: >2 agents on node " + i);
				return false;
			}

			// ...and there should be only one node with exactly two agents. Count 'em.
			else
			if (agentsThisHost == 2) {
				numberOfHostsWithTwoAgents++;
				indexOfHostWithTwoAgents = i;
			}

		} // server
	}
	if (numberOfHostsWithTwoAgents != 1) {
		SystemDesign.logWarn("SystemDesign.testB: Fails test: Number of nodes with two agents: " 
				+ numberOfHostsWithTwoAgents + " (should be 1)!");
		return false;
	}

	// Ensure that Agent8 and Agent9 are together on host[indexOfHostWithTwoAgents].
	//
	for (int i=1; i<=sd.numfunction; i++) {
		if ("Agent8".equals(sd.xnfunction[i].name)  && "Agent9".equals(sd.xnfunction[i].name)) {
			if (sd.agent[i].annealhostid != indexOfHostWithTwoAgents) {
				SystemDesign.logWarn("SystemDesign.testB: Fails test: " + sd.xnfunction[i].name 
						+ " isn't on node " + indexOfHostWithTwoAgents);
				return false;
			}
		}
	}

	SystemDesign.logWarn("SystemDesign.testC: Passing 'TestC' without really testing!");
	return true;
	} // doTestC



private ByteArrayInputStream
readInputString(String xml) {

//	String xml = getTestDataA1AsString();
	ByteArrayInputStream bais = null;
	try {
		bais = new ByteArrayInputStream(xml.getBytes("US-ASCII"));
		}
	catch (java.io.UnsupportedEncodingException uee) {
		SystemDesign.logError("CSParser.testXMLInputStream: IO exception on encoding:\n", uee);
		}
	return bais;
	}


/**
 * An XML string representing a CougaarSociety embodying the "a1" test set data.
 */
private static String
testDataA1AsString_ =

	"	<CougaarSociety identifier=\"A1_test_data\">" +
	"		<CougaarNode name=\"FWD-B\">" +
	"			<Attribute name=\"ProbabilityOfFailure\" value=\"0.8\"/>" +
	"			<Attribute name=\"CPU\" value=\"100.0\"/>" +
	"			<Attribute name=\"Memory\" value=\"1024\"/>" +
	"		</CougaarNode>" +
	"		<CougaarNode name=\"FWD-F\">" +
	"			<Attribute name=\"ProbabilityOfFailure\" value=\"0.1\"/>" +
	"			<Attribute name=\"CPU\" value=\"100.0\"/>" +
	"			<Attribute name=\"Memory\" value=\"512\"/>" +
	"			<CougaarAgent name=\"1-37-ARBN\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"47-FSB\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-6-INFBN\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"26-SSCO\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"2-3-FABN\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"4-27-FABN\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"2-6-INFBN\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"40-ENGBN\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"AVNBDE-1-AD\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"592-ORDCO\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"2-70-ARBN\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"588-MAINTCO\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"2-37-ARBN\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"16-ENGBN\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-36-INFBN\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"2-BDE-1-AD\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"15-PLS-TRKCO\">" +
	"				<Requirement name=\"CPU\" value=\"2.5\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"		</CougaarNode>" +
	"		<CougaarNode name=\"FWD-E\">" +
	"			<Attribute name=\"ProbabilityOfFailure\" value=\"0.2\"/>" +
	"			<Attribute name=\"CPU\" value=\"100.0\"/>" +
	"			<Attribute name=\"Memory\" value=\"512\"/>" +
	"			<CougaarAgent name=\"77-MAINTCO\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-35-ARBN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"501-FSB\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"596-MAINTCO\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"2-501-AVNBN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-41-INFBN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"3-BDE-1-AD\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-BDE-1-AD\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"25-FABTRY-TGTACQ\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-1-CAVSQDN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"141-SIGBN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"106-TCBN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"		</CougaarNode>" +
	"		<CougaarNode name=\"FWD-C\">" +
	"			<Attribute name=\"ProbabilityOfFailure\" value=\"0.2\"/>" +
	"			<Attribute name=\"CPU\" value=\"100.0\"/>" +
	"			<Attribute name=\"Memory\" value=\"512\"/>" +
	"			<CougaarAgent name=\"69-CHEMCO\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-AD\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"DIVARTY-1-AD\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"18-MAINTBN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"501-MPCO\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"226-MAINTCO\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"16-CSG\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-94-FABN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"372-CGO-TRANSCO\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"485-CSB\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-13-ARBN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-AD-DIV\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"		</CougaarNode>" +
	"		<CougaarNode name=\"ENCLAVE2-MANAGER\">" +
	"			<Attribute name=\"ProbabilityOfFailure\" value=\"0.1\"/>" +
	"			<Attribute name=\"CPU\" value=\"100.0\"/>" +
	"			<Attribute name=\"Memory\" value=\"512\"/>" +
	"			<CougaarAgent name=\"Enclave2ScalabilityManager\">" +
	"				<Requirement name=\"CPU\" value=\"50.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"256.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"238-POL-TRKCO\">" +
	"				<Requirement name=\"CPU\" value=\"50.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"		</CougaarNode>" +
	"		<CougaarNode name=\"FWD-A\">" +
	"			<Attribute name=\"ProbabilityOfFailure\" value=\"0.5\"/>" +
	"			<Attribute name=\"CPU\" value=\"100.0\"/>" +
	"			<Attribute name=\"Memory\" value=\"1024\"/>" +
	"		</CougaarNode>" +
	"		<CougaarNode name=\"FWD-D\">" +
	"			<Attribute name=\"ProbabilityOfFailure\" value=\"0.2\"/>" +
	"			<Attribute name=\"CPU\" value=\"100.0\"/>" +
	"			<Attribute name=\"Memory\" value=\"512\"/>" +
	"			<CougaarAgent name=\"127-DASB\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"70-ENGBN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"DISCOM-1-AD\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"125-FSB\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-501-AVNBN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"123-MSB\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"263-FLDSVC-CO\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"102-POL-SUPPLYCO\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"501-MIBN-CEWI\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"594-MDM-TRKCO\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"1-4-ADABN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"4-1-FABN\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"			<CougaarAgent name=\"227-SUPPLYCO\">" +
	"				<Requirement name=\"CPU\" value=\"5.0\"/>" +
	"				<Requirement name=\"Memory\" value=\"30.0\"/>" +
	"			</CougaarAgent>" +
	"		</CougaarNode>" +
	"	</CougaarSociety>";


} // SystemDesignTest



