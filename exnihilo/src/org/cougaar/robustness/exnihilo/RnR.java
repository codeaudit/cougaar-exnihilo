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

import java.io.*;


/**
	Originally "Reading 'n' wRiting", now a collection of misc util methods.

	@version 1.1 - moved VB emulation methods here from VB2JUtils (obsolete)
	@version 18Jun03 - fixed "integration violations"
	@author robert.e.cranfill@boeing.com
**/
public class RnR {



/**
	Emulate VB's "Int" function.
**/
public static int
Int(double f) {
	return (int)Math.floor(f);
	}


/**
	Return a random number 0 <= n < 1.0 <BR>
	Math.random cast to a float can return 1.0, 
	but we want strictly <1.0, so don't return that.

	@version 1.1
**/
public static float
Rnd() {
	float result = (float)Math.random();
	while (result == 1.0) {
		result = (float)Math.random();
		}
	return result;
	}


/**
	Return e^x
**/
public static float
Exp(float x) {
	return (float)Math.exp(x);
	}


/**
	Read a .nodedef file into the fields of the given SystemDesign object. <BR>
	You must call readNodes first, as that sets the system node count, numnode.
	
	@version 1.1
**/
public static boolean 
readNodes(SystemDesign systemDesign, String pFileName) { 
            // {VB2J [867]}  Public Function readNodes(ByVal pFileName As String) As Boolean

	LineNumberReader in = null;
	try {
		in = new LineNumberReader(new FileReader(pFileName));
	}
	catch (Exception e) {
		e.printStackTrace();
		return false;
	}

	try {

//		1.1
		String line = null;	
		line = in.readLine();	//  "Node Definition:" or "EN VERSION"
		float noderevlev = 1.0f;
		if (line.startsWith("EN VERSION")) {
			noderevlev = Float.parseFloat(line.substring(11));
			SystemDesign.logDebug(" >>> EN node file version " + noderevlev);

			// skip two lines
			in.readLine();
			in.readLine();
			}

		in.readLine();					   		// {VB2J [884]}     'Print #filenum, "----------------"
		in.readLine();							// {VB2J [886]}     'Print #filenum, ""
		in.readLine();							// {VB2J [888]}     'Print #filenum, "List of Defined Nodes:"
		in.readLine();							// {VB2J [890]}     'Print #filenum, "Total number of defined nodes:"
		systemDesign.numnode = Integer.parseInt(in.readLine().trim());				// {VB2J [892]}     'Print #filenum, numnode
		for (int inod=1; inod<=systemDesign.numnode; inod++) {                                      // {VB2J [904]}     For inod = 1 To numnode
			in.readLine();								// {VB2J [905]}        'Print #filenum, "-------------------------------------------------------"
			in.readLine();								// {VB2J [907]}        'Print #filenum, "Node Number:"
			int inodx = Integer.parseInt(in.readLine().trim()); // {VB2J [909]}        'Print #filenum, inod

														// {VB2J [913]}        'if inodx is not equal to inod, there is a database error!
			if (inodx != inod)  {                       // {VB2J [914]}        If (inodx <> inod) Then
				SystemDesign.logError("RnR.readNodes: Error in node file for node " + inod + "; found index " + inodx);
				return false;
				}                                       // {VB2J [584]}     End If

			systemDesign.nodeinfo[inod] = new Node();

// We don't care about "node" data, it's GUI stuff.
//
    // but 'caption' is used for the node name!
		/* systemDesign.node[inod].Tag = */ 		in.readLine(); 						// {VB2J [929]}        node(inod).Tag = Val(record)
		/* systemDesign.node[inod].Caption =  */ 	String caption = in.readLine(); 		// {VB2J [932]}        node(inod).Caption = record

	// Don't parse this line, we don't use it, it's not worth the effort.
		in.readLine();									// {VB2J [933]}        'Print #filenum, node(inod).Top, node(inod).Left
		/* systemDesign.node[inod].top  = -1;  *///W ARRAY/FN              // {VB2J [936]}        node(inod).top = tokara(1)
		/* systemDesign.node[inod].left = -1;  *///W ARRAY/FN              // {VB2J [937]}        node(inod).left = tokara(2)

		/* systemDesign.node[inod].BackColor = */ Integer.parseInt(in.readLine().trim());                                           // {VB2J [940]}        node(inod).BackColor = Val(record)
		/* systemDesign.node[inod].ForeColor = */ Integer.parseInt(in.readLine().trim());                                            // {VB2J [943]}        node(inod).ForeColor = Val(record)

														   // {VB2J [946]}        'now load nodeinfo:
		systemDesign.nodeinfo[inod].name = caption.trim(); // was systemDesign.node[inod].Caption;                                      // {VB2J [947]}        nodeinfo(inod).name = node(inod).Caption

		systemDesign.nodeinfo[inod].costbase = Float.parseFloat(in.readLine().trim());                                         // {VB2J [953]}        nodeinfo(inod).costbase = Val(record)
		systemDesign.nodeinfo[inod].costbasepermonth = Float.parseFloat(in.readLine().trim());                                 // {VB2J [956]}        nodeinfo(inod).costbasepermonth = Val(record)
		systemDesign.nodeinfo[inod].costpercpuproc = Float.parseFloat(in.readLine().trim());                                   // {VB2J [959]}        nodeinfo(inod).costpercpuproc = Val(record)
		systemDesign.nodeinfo[inod].costpercpusec = Float.parseFloat(in.readLine().trim());                                    // {VB2J [962]}        nodeinfo(inod).costpercpusec = Val(record)
		systemDesign.nodeinfo[inod].costperdisk = Float.parseFloat(in.readLine().trim());                                      // {VB2J [966]}        nodeinfo(inod).costperdisk = Val(record)
		systemDesign.nodeinfo[inod].costpermbmemory = Float.parseFloat(in.readLine().trim());                                  // {VB2J [969]}        nodeinfo(inod).costpermbmemory = Val(record)
		systemDesign.nodeinfo[inod].costpermbstorage = Float.parseFloat(in.readLine().trim());                                 // {VB2J [975]}        nodeinfo(inod).costpermbstorage = Val(record)
		systemDesign.nodeinfo[inod].costpermbtraffic = Float.parseFloat(in.readLine().trim());                                 // {VB2J [978]}        nodeinfo(inod).costpermbtraffic = Val(record)
		systemDesign.nodeinfo[inod].cpubackground = Float.parseFloat(in.readLine().trim());                                    // {VB2J [983]}        nodeinfo(inod).cpubackground = Val(record)
		systemDesign.nodeinfo[inod].cpucount = Integer.parseInt(in.readLine().trim());                                         // {VB2J [986]}        nodeinfo(inod).cpucount = Val(record)
		systemDesign.nodeinfo[inod].cpucountmax = Integer.parseInt(in.readLine().trim());                                      // {VB2J [989]}        nodeinfo(inod).cpucountmax = Val(record)
		systemDesign.nodeinfo[inod].cpuutilmax = Float.parseFloat(in.readLine().trim());                                       // {VB2J [992]}        nodeinfo(inod).cpuutilmax = Val(record)
		systemDesign.nodeinfo[inod].diskcount = Integer.parseInt(in.readLine().trim());                                        // {VB2J [998]}        nodeinfo(inod).diskcount = Val(record)
		systemDesign.nodeinfo[inod].diskcountmax = Integer.parseInt(in.readLine().trim());                                     // {VB2J [1001]}       nodeinfo(inod).diskcountmax = Val(record)
		systemDesign.nodeinfo[inod].diskiobackground = Float.parseFloat(in.readLine().trim());                                 // {VB2J [1005]}       nodeinfo(inod).diskiobackground = Val(record)
		systemDesign.nodeinfo[inod].diskiomax = Float.parseFloat(in.readLine().trim());                                        // {VB2J [1008]}       nodeinfo(inod).diskiomax = Val(record)
		systemDesign.nodeinfo[inod].diskiomaxreads = Float.parseFloat(in.readLine().trim());                                   // {VB2J [1014]}       nodeinfo(inod).diskiomaxreads = Val(record)
		systemDesign.nodeinfo[inod].diskiomaxwrites = Float.parseFloat(in.readLine().trim());                                  // {VB2J [1017]}       nodeinfo(inod).diskiomaxwrites = Val(record)
		systemDesign.nodeinfo[inod].diskiomaxreadswrites = Float.parseFloat(in.readLine().trim());                             // {VB2J [1020]}       nodeinfo(inod).diskiomaxreadswrites = Val(record)
		systemDesign.nodeinfo[inod].disklatency = Float.parseFloat(in.readLine().trim());                                      // {VB2J [1023]}       nodeinfo(inod).disklatency = Val(record)
		systemDesign.nodeinfo[inod].disksize = Float.parseFloat(in.readLine().trim());                                         // {VB2J [1029]}       nodeinfo(inod).disksize = Val(record)

//		'marc en 1.1 and en 1.2: smoothing and performance map
//		'Print #filenum, nodeinfo(inod).enabled 'marc en 1.1. soft constraints, add nodeinfo(inod).enabled
//		'Print #filenum, nodeinfo(inod).protected 'marc en 1.2 performance map, add nodeinfo(inod).protected
		if (noderevlev > 1.0) {
			systemDesign.nodeinfo[inod].enabled = new Boolean(in.readLine().trim()).booleanValue();
			// 'en 1.2 performance map
			systemDesign.nodeinfo[inod].isProtected = new Boolean(in.readLine().trim()).booleanValue();
			}


		systemDesign.nodeinfo[inod].memory = Integer.parseInt(in.readLine().trim());                                           // {VB2J [1035]}       nodeinfo(inod).memory = Val(record)
		systemDesign.nodeinfo[inod].memorymax = Integer.parseInt(in.readLine().trim());                                        // {VB2J [1039]}       nodeinfo(inod).memorymax = Val(record)
		systemDesign.nodeinfo[inod].msgforward = Integer.parseInt(in.readLine().trim());                                       // {VB2J [1044]}       nodeinfo(inod).msgforward = Val(record)
		systemDesign.nodeinfo[inod].packetratemax = Float.parseFloat(in.readLine().trim());                                    // {VB2J [1049]}       nodeinfo(inod).packetratemax = Val(record)
		systemDesign.nodeinfo[inod].pfail = Float.parseFloat(in.readLine().trim());                                            // {VB2J [1054]}       nodeinfo(inod).pfail = Val(record)
		systemDesign.nodeinfo[inod].type = Integer.parseInt(in.readLine().trim());                                             // {VB2J [1057]}       nodeinfo(inod).type = Val(record)
		}                                                                              // {VB2J [1060]}    Next inod
	}
	catch (IOException ioe) {
		SystemDesign.logError("RnR.readNodes: I/O exception reading file '" + pFileName + "' on line #" + in.getLineNumber());
		ioe.printStackTrace();
	}
	catch (NumberFormatException nfe) {
		SystemDesign.logError("RnR.readNodes: Number format exception parsing file '" + pFileName + "' on line #" + in.getLineNumber());
		nfe.printStackTrace();
	}

	SystemDesign.logDebug("RnR.readNodes: OK");

	return true;                                                              // {VB2J [1116]}     readNodes = True    ' cranfill
}                                                                         // {VB2J [1118]} End Function
// readNodes


/**
	Read a .linkdef file into the fields of the given SystemDesign object.
	@version 1.1
**/
public static boolean 
readLinks(SystemDesign systemDesign, String pFileName) {
                                // {VB2J [549]} Public Function readLinks(ByVal pFileName As String) As Boolean

	LineNumberReader in = null;
	try {
		in = new LineNumberReader(new FileReader(pFileName));
	}
	catch (Exception e) {
		e.printStackTrace();
		return false;
	}

	try {


//		1.1
		String line = null;	
		line = in.readLine();	//  "----------------" or "EN VERSION"
		float linkrevlev = 1.0f;
		if (line.startsWith("EN VERSION")) {
			linkrevlev = Float.parseFloat(line.substring(11));
			SystemDesign.logDebug(" >>> EN link file version " + linkrevlev);

			// skip two lines
			in.readLine();
			in.readLine();
			}

		in.readLine();										// {VB2J [565]}     'Print #filenum, "-------------------------------------------------------"
		in.readLine();										// {VB2J [567]}     'Print #filenum, ""
		in.readLine();										// {VB2J [569]}     'Print #filenum, "List of Defined Links:"
		in.readLine();										// {VB2J [571]}     'Print #filenum, "Total number of defined Links:"
		systemDesign.numlink = Integer.parseInt(in.readLine().trim());			// {VB2J [573]}     'Print #filenum, numlink
		in.readLine();										// {VB2J [576]}     'Print #filenum, "Total number of defined nodes:"
		int numnodex = Integer.parseInt(in.readLine().trim()); // {VB2J [578]}     'Print #filenum, numnode
		if (numnodex != systemDesign.numnode)  {                                                    // {VB2J [581]}     If (numnodex <> numnode) Then
			SystemDesign.logError("RnR.readLinks: Number of nodes defined in link file not equal to number of nodes in current project.");
			return false;
			}                                                                            // {VB2J [584]}     End If
	
		  for (int ilink=1; ilink<=systemDesign.numlink; ilink++) {                                   // {VB2J [587]}     For ilink = 1 To numlink
			in.readLine();// {VB2J [589]}        Line Input #filenum, record
			in.readLine();// {VB2J [591]}        Line Input #filenum, record
			in.readLine();// {VB2J [593]}        Line Input #filenum, record
			
			systemDesign.linkinfo[ilink].name = in.readLine().trim();                                          // {VB2J [596]}        linkinfo(ilink).name = record
			systemDesign.linkinfo[ilink].node1 = Integer.parseInt(in.readLine().trim());                                         // {VB2J [599]}        linkinfo(ilink).node1 = Val(record)
			systemDesign.linkinfo[ilink].node2 = Integer.parseInt(in.readLine().trim());                                         // {VB2J [602]}        linkinfo(ilink).node2 = Val(record)
			
			systemDesign.linkinfo[ilink].left = Integer.parseInt(in.readLine().trim());                                          // {VB2J [608]}        linkinfo(ilink).left = Val(record)
			systemDesign.linkinfo[ilink].top = Integer.parseInt(in.readLine().trim());                                           // {VB2J [611]}        linkinfo(ilink).top = Val(record)
			systemDesign.linkinfo[ilink].saveposition = Integer.parseInt(in.readLine().trim());                                  // {VB2J [614]}        linkinfo(ilink).saveposition = Val(record)
			systemDesign.linkinfo[ilink].linecolor = (long)Float.parseFloat(in.readLine().trim());                               // {VB2J [619]}        linkinfo(ilink).linecolor = Val(record)
			systemDesign.linkinfo[ilink].linewidth = Integer.parseInt(in.readLine().trim());                                     // {VB2J [622]}        linkinfo(ilink).linewidth = Val(record)
			systemDesign.linkinfo[ilink].background = Float.parseFloat(in.readLine().trim());                                    // {VB2J [627]}        linkinfo(ilink).background = Val(record)
			systemDesign.linkinfo[ilink].bandwidth = Float.parseFloat(in.readLine().trim());                                     // {VB2J [630]}        linkinfo(ilink).bandwidth = Val(record)
			systemDesign.linkinfo[ilink].costbase = Float.parseFloat(in.readLine().trim());                                      // {VB2J [633]}        linkinfo(ilink).costbase = Val(record)
			systemDesign.linkinfo[ilink].costperkb = Float.parseFloat(in.readLine().trim());                                     // {VB2J [636]}        linkinfo(ilink).costperkb = Val(record)
			systemDesign.linkinfo[ilink].costpermonth = Float.parseFloat(in.readLine().trim());                                  // {VB2J [639]}        linkinfo(ilink).costpermonth = Val(record)
			systemDesign.linkinfo[ilink].hops = Integer.parseInt(in.readLine().trim());                                          // {VB2J [642]}        linkinfo(ilink).hops = Val(record)
			systemDesign.linkinfo[ilink].latency = Float.parseFloat(in.readLine().trim());                                       // {VB2J [645]}        linkinfo(ilink).latency = Val(record)
			systemDesign.linkinfo[ilink].mtu = Float.parseFloat(in.readLine().trim());                                           // {VB2J [648]}        linkinfo(ilink).mtu = Val(record)
			systemDesign.linkinfo[ilink].packetmax = (long)Float.parseFloat(in.readLine().trim());                               // {VB2J [651]}        linkinfo(ilink).packetmax = Val(record)
			systemDesign.linkinfo[ilink].pfail = Float.parseFloat(in.readLine().trim());                                         // {VB2J [654]}        linkinfo(ilink).pfail = Val(record)
			systemDesign.linkinfo[ilink].protocol = Integer.parseInt(in.readLine().trim());                                      // {VB2J [657]}        linkinfo(ilink).protocol = Val(record)
			systemDesign.linkinfo[ilink].protocoloverhead = Integer.parseInt(in.readLine().trim());                              // {VB2J [660]}        linkinfo(ilink).protocoloverhead = Val(record)
			systemDesign.linkinfo[ilink].type = Integer.parseInt(in.readLine().trim());                                          // {VB2J [663]}        linkinfo(ilink).type = Val(record)
		
			String record = in.readLine();
			while (record.equals("(End-_-_of-_-_text!)") == false) {                    // {VB2J [671]}        While (record <> "(End-_-_of-_-_text!)")
			  systemDesign.linkinfo[ilink].text += ("\n" + record);                                  // {VB2J [672]}           recx = recx & Chr$(13) & Chr$(10) & record
			  record = in.readLine();
			}                                                           // {VB2J [674]}        Wend
			// systemDesign.linkinfo[ilink].text = recx;                                                 // {VB2J [676]}        linkinfo(ilink).text = recx
		}                                                                            // {VB2J [698]}     Next ilink
	}
	catch (IOException ioe) {
		SystemDesign.logError("RnR.readLinks: I/O exception reading file '" + pFileName + "' on line #" + in.getLineNumber());
		ioe.printStackTrace();
	}
	catch (NumberFormatException nfe) {
		SystemDesign.logError("RnR.readLinks: Number format exception parsing file '" + pFileName + "' on line #" + in.getLineNumber());
		nfe.printStackTrace();
	}

  return true;                          // {VB2J [703]}      readLinks = True ' cranfill
}                                       // {VB2J [705]}  End Function
// readLinks


/**
	Read a .functiondef file into the fields of the given SystemDesign object.

	Also set objfnjobset ???

	@version 1.1
**/
public static boolean 
readFunctions(SystemDesign systemDesign, String pFileName) { 
                // {VB2J [708]}  Public Function readFunctions(ByVal pFileName) As Boolean

	LineNumberReader in = null;
	try {
		in = new LineNumberReader(new FileReader(pFileName));
	}
	catch (Exception e) {
		e.printStackTrace();
		return false;
	}

	// rec - In the VB, each input line was commented with the corresponding VB line that output that data item.
	// Since that's more meaningful than the "Line Input #filenum, record" line that read it, 
	// I've kept the 'Print' code as the comment.
	//
	try {
		in.readLine();					// {VB2J [720]}     'Print #filenum, "-------------------------------------------------------"
        in.readLine();					// {VB2J [722]}     'Print #filenum, "-------------------------------------------------------"
        in.readLine();					// {VB2J [724]}     'Print #filenum, ""
        in.readLine();					// {VB2J [726]}     'Print #filenum, "List of Defined Functions:"
        in.readLine();					// {VB2J [728]}     'Print #filenum, "Total number of defined functions:"
		systemDesign.numfunction = Integer.parseInt(in.readLine().trim());		// {VB2J [730]}     'Print #filenum, numfunction
        in.readLine();					// {VB2J [742]}     'Print #filenum, "Total number of defined nodes:"

		int numnodex = Integer.parseInt(in.readLine().trim());	// {VB2J [744]}     'Print #filenum, numnode
		if (numnodex != systemDesign.numnode)  {    // {VB2J [747]}     If (numnodex <> numnode) Then
			SystemDesign.logError("RnR.readFunctions: Error in function file; number of nodes is " + numnodex + ", expected " + systemDesign.numnode);
			return false;
		}       // {VB2J [750]}     End If

		for (int ifn=1; ifn<=systemDesign.numfunction; ifn++) {    // {VB2J [755]}     For ifn = 1 To numfunction

			in.readLine();					// {VB2J [756]}        'Print #filenum, "-------------------------------------------------------"
			in.readLine();					// {VB2J [758]}        'Print #filenum, "Function Number:"

			// rec - unlike the other readX methods, we don't check that the index in the file match what we are expecting. Whatever.
			in.readLine();																// {VB2J [760]}        'Print #filenum, ifn

			systemDesign.xnfunction[ifn].name = in.readLine().trim();  						// {VB2J [762]}        'Print #filenum, xnfunction(ifn).name
			systemDesign.xnfunction[ifn].ifgroup = Integer.parseInt(in.readLine().trim());		// {VB2J [765]}        'Print #filenum, xnfunction(ifn).ifgroup
			systemDesign.xnfunction[ifn].floadtype = Integer.parseInt(in.readLine().trim());   // {VB2J [768]}        'Print #filenum, xnfunction(ifn).floadtype
			in.readLine();					 											// {VB2J [773]}        'Print #filenum, "Objective Function Flag:"
			systemDesign.xnfunction[ifn].objfn = (Boolean.valueOf(in.readLine().trim())).booleanValue();   	// {VB2J [776]}        'Print #filenum, xnfunction(ifn).objfn
			
			for (int inod=1; inod<=systemDesign.numnode; inod++) {          // {VB2J [780]}        For inod = 1 To numnode
				systemDesign.xnfunction[ifn].costpercall[inod] = Float.parseFloat(in.readLine().trim());          	// {VB2J [781]}           'Print #filenum, xnfunction(ifn).costpercall(inod)
				systemDesign.xnfunction[ifn].costpernode[inod] = Float.parseFloat(in.readLine().trim());       	// {VB2J [784]}           'Print #filenum, xnfunction(ifn).costpernode(inod)
				systemDesign.xnfunction[ifn].costpermonth[inod] = Float.parseFloat(in.readLine().trim());    		// {VB2J [787]}           'Print #filenum, xnfunction(ifn).costpermonth(inod)
				systemDesign.xnfunction[ifn].cpu[inod] = Float.parseFloat(in.readLine().trim()); 					// {VB2J [791]}           'Print #filenum, xnfunction(ifn).cpu(inod)
				systemDesign.xnfunction[ifn].cpurate[inod] = Float.parseFloat(in.readLine().trim());         		// {VB2J [794]}           'Print #filenum, xnfunction(ifn).cpurate(inod)
				systemDesign.xnfunction[ifn].memory[inod] = Float.parseFloat(in.readLine().trim());   				// {VB2J [797]}           'Print #filenum, xnfunction(ifn).memory(inod)
				systemDesign.xnfunction[ifn].diskiopercall[inod] = Float.parseFloat(in.readLine().trim());   		// {VB2J [801]}           'Print #filenum, xnfunction(ifn).diskiopercall(inod)
				systemDesign.xnfunction[ifn].diskspacereq[inod] = Float.parseFloat(in.readLine().trim());     		// {VB2J [804]}           'Print #filenum, xnfunction(ifn).diskspacereq(inod)
				systemDesign.xnfunction[ifn].felig[inod] = Integer.parseInt(in.readLine().trim());          		// {VB2J [807]}           'Print #filenum, xnfunction(ifn).felig(inod)
				systemDesign.xnfunction[ifn].flock[inod] = Integer.parseInt(in.readLine().trim());             	// {VB2J [810]}           'Print #filenum, xnfunction(ifn).flock(inod)
			}                                 	// {VB2J [813]}        Next inod

			for (int ifn2=1; ifn2<=systemDesign.numfunction; ifn2++) {                                  	// {VB2J [815]}        For ifn2 = 1 To numfunction
				systemDesign.xnfunction[ifn].fcall[ifn2].ifgroup = Integer.parseInt(in.readLine().trim());     	// {VB2J [816]}           'Print #filenum, xnfunction(ifn).fcall(ifn2).ifgroup
				systemDesign.xnfunction[ifn].fcall[ifn2].sendmsgsize = Float.parseFloat(in.readLine().trim()); 	// {VB2J [819]}           'Print #filenum, xnfunction(ifn).fcall(ifn2).sendmsgsize
				systemDesign.xnfunction[ifn].fcall[ifn2].recvmsgsize = Float.parseFloat(in.readLine().trim()); 	// {VB2J [822]}           'Print #filenum, xnfunction(ifn).fcall(ifn2).recvmsgsize
				systemDesign.xnfunction[ifn].fcall[ifn2].sendmsgrate = Float.parseFloat(in.readLine().trim()); 	// {VB2J [825]}           'Print #filenum, xnfunction(ifn).fcall(ifn2).sendmsgrate
				systemDesign.xnfunction[ifn].fcall[ifn2].callpct = Float.parseFloat(in.readLine().trim());     	// {VB2J [829]}           'Print #filenum, xnfunction(ifn).fcall(ifn2).callpct
				systemDesign.xnfunction[ifn].fcall[ifn2].callfreq = Float.parseFloat(in.readLine().trim());    	// {VB2J [832]}           'Print #filenum, xnfunction(ifn).fcall(ifn2).callfreq
			}                                  	// {VB2J [835]}        Next ifn2
		}                                     	// {VB2J [837]}     Next ifn

       // {VB2J [840]}     'write function groups:

       in.readLine();			// {VB2J [842]}     'Print #filenum, "-------------------------------------------------------"
       in.readLine();			// {VB2J [844]}     'Print #filenum, "-------------------------------------------------------"
       in.readLine();			// {VB2J [846]}     'Print #filenum, "List of Defined function groups"
       in.readLine();			// {VB2J [848]}     'Print #filenum, "Total number of defined groups:"

       systemDesign.numfgroup = Integer.parseInt(in.readLine().trim());        // {VB2J [850]}     'Print #filenum, numfgroup

	   for (int igp=1; igp<=systemDesign.numfgroup; igp++) {     	// {VB2J [854]}     For igp = 1 To numfgroup
			systemDesign.fgroup[igp] = in.readLine();       		// {VB2J [855]}        'Print #filenum, fgroup(igp)
		}                                                        	// {VB2J [858]}     Next igp
	}
	catch (IOException ioe) {
		SystemDesign.logError("RnR.readFunctions: I/O exception reading file '" + pFileName + "' on line #" + in.getLineNumber());
		SystemDesign.logError(" " + ioe);
	//	ioe.printStackTrace();
	}
	catch (NumberFormatException nfe) {
		SystemDesign.logError("RnR.readFunctions: Number format exception parsing file '" + pFileName + "' on line #" + in.getLineNumber());
		SystemDesign.logError(" " + nfe);
	//	nfe.printStackTrace();
	}

   return true;                   // {VB2J [863]}      readFunctions = True ' cranfill
}                                 // {VB2J [865]}  End Function
// readFunctions


/**
 * Write the node data. - stolen from 2002 code
 *   Note that in the original VB code, 
 *       "nodes" is the control array of VB 'Label' items, the visual representation of the node; 
 *       "nodeinfo" is the node data we care about. <P>
 * 
 * NOT COMPATIBLE WITH 1.1 YET
 */
public static void
saveNodes(String baseName, SystemDesign sd, boolean annotateData) {


    Node[] nodeinfo = sd.nodeinfo;

    PrintWriter pw = null;
    try {
        pw = new PrintWriter(new FileWriter(baseName + ".nodedef"));
    }
    catch (IOException ioe) {
        ioe.printStackTrace();
        return;
    }

    SystemDesign.logDebug("");
    SystemDesign.logDebug("processing nodes....");

    printWithOptionalComments(pw, "Node Definition:", false, "");
    printWithOptionalComments(pw, "----------------", false, "");
    printWithOptionalComments(pw, "", false, "");
    printWithOptionalComments(pw, "List of Defined Nodes:", false, "");
    printWithOptionalComments(pw, "Total number of defined nodes:", false, "");
    printWithOptionalComments(pw, sd.numnode, annotateData, "numnode");
    
    // Arrange the nodes in a circle around the router.
    //
    double radius = 250;
    double centerX = 320, centerY = 280;

    for (int inod=1; inod<=sd.numnode; inod++) {

        printWithOptionalComments(pw, "-------------------------------------------------------", false, "");
        printWithOptionalComments(pw, "Node Number:", false, "");
        printWithOptionalComments(pw, inod, annotateData, "inod");

        printWithOptionalComments(pw, "", false, "");                     // nodes[inod].Tag  - is stated to be unused
        printWithOptionalComments(pw, nodeinfo[inod].name, false, "");   // nodes[inod].Caption - use node 'name' (text, can't comment)
 
        // Here's the placement computation.
        // Node #1 at "12 o'clock" position, the rest clockwise from there.
        //
        int x = Math.round((float)centerX);
        int y = Math.round((float)centerY);
        if (nodeinfo[inod].type != Node.TYPE_ROUTER) {
            double theta = (inod-1) * 2 * Math.PI / (sd.numnode-1);
            x = Math.round((float)(centerX - radius*Math.sin(theta)));
            y = Math.round((float)(centerY - radius*Math.cos(theta)));
        //    SystemDesign.logDebug("screen loc node " + inod + " = (" + x + ", " + y + ")");
            }

        // can't put a comment with this one, because it's got two values
        printWithOptionalComments(pw, y + "    " + x, false, "icon top, left");    // nodes[inod].top, nodes[inod].left

        // Icon colors
        int color = 65535;   // yellow
        if (nodeinfo[inod].type == Node.TYPE_ROUTER) 
            color = 16764108; // a light blue
        printWithOptionalComments(pw, color, annotateData, "backcolor");   // nodes[inod].BackColor
        printWithOptionalComments(pw, "0", annotateData, "forecolor");     // nodes[inod].ForeColor

/* actual node properties
*/
        printWithOptionalComments(pw, nodeinfo[inod].costbase,         annotateData, "costbase");
        printWithOptionalComments(pw, nodeinfo[inod].costbasepermonth, annotateData, "costbasepermonth");
        printWithOptionalComments(pw, nodeinfo[inod].costpercpuproc,   annotateData, "costpercpuproc");
        printWithOptionalComments(pw, nodeinfo[inod].costpercpusec,    annotateData, "costpercpusec");
        printWithOptionalComments(pw, nodeinfo[inod].costperdisk,      annotateData, "costperdisk");
        printWithOptionalComments(pw, nodeinfo[inod].costpermbmemory,  annotateData, "costpermbmemory");
        printWithOptionalComments(pw, nodeinfo[inod].costpermbstorage, annotateData, "costpermbstorage");
        printWithOptionalComments(pw, nodeinfo[inod].costpermbtraffic, annotateData, "costpermbtraffic");

        printWithOptionalComments(pw, nodeinfo[inod].cpubackground,    annotateData, "cpubackground");
        printWithOptionalComments(pw, nodeinfo[inod].cpucount,         annotateData, "cpucount");
        printWithOptionalComments(pw, nodeinfo[inod].cpucountmax,      annotateData, "cpucountmax");
        printWithOptionalComments(pw, nodeinfo[inod].cpuutilmax,       annotateData, "cpuutilmax");

        printWithOptionalComments(pw, nodeinfo[inod].diskcount,            annotateData, "diskcount");
        printWithOptionalComments(pw, nodeinfo[inod].diskcountmax,         annotateData, "diskcountmax");
        printWithOptionalComments(pw, nodeinfo[inod].diskiobackground,     annotateData, "diskiobackground");
        printWithOptionalComments(pw, nodeinfo[inod].diskiomax,            annotateData, "diskiomax");
        printWithOptionalComments(pw, nodeinfo[inod].diskiomaxreads,       annotateData, "diskiomaxreads");
        printWithOptionalComments(pw, nodeinfo[inod].diskiomaxwrites,      annotateData, "diskiomaxwrites");
        printWithOptionalComments(pw, nodeinfo[inod].diskiomaxreadswrites, annotateData, "diskiomaxreadswrites");
        printWithOptionalComments(pw, nodeinfo[inod].disklatency,          annotateData, "disklatency");
        printWithOptionalComments(pw, nodeinfo[inod].disksize,             annotateData, "disksize");

        printWithOptionalComments(pw, nodeinfo[inod].memory,        annotateData, "memory");
        printWithOptionalComments(pw, nodeinfo[inod].memorymax,     annotateData, "memorymax");
        printWithOptionalComments(pw, nodeinfo[inod].msgforward,    annotateData, "msgforward");
        printWithOptionalComments(pw, nodeinfo[inod].packetratemax, annotateData, "packetratemax");
        printWithOptionalComments(pw, nodeinfo[inod].pfail,         annotateData, "pfail");
        printWithOptionalComments(pw, nodeinfo[inod].type,          annotateData, "type");

        } // Next inod

    pw.close();
    
    }   // saveNodes


/**
 * Ouput a .functiondef file. - stolen from 2002 code
 *   as per menusavefunctions_Click
 * 
 * NOT COMPATIBLE WITH 1.1 YET
 */
public static void
saveFunctions(String baseName, SystemDesign sd, boolean annotateData) {

	SystemDesign.logWarn(" saveFunctions NOT COMPATIBLE WITH 1.1 YET");


    Function[] functions = sd.xnfunction;
// 18jun03 - unused:    Link[] links = sd.linkinfo;
// 18jun03 - unused:     Node[] nodes = sd.nodeinfo;

    // ??? right ???
    int[] functionGroups = sd.ifninitcall;

    PrintWriter pw = null;
    try {
        pw = new PrintWriter(new FileWriter(baseName + ".functiondef"));
    }
    catch (IOException ioe) {
        ioe.printStackTrace();
        return;
    }

    SystemDesign.logDebug("processing functions....");

    printWithOptionalComments(pw, "-------------------------------------------------------", false, "");
    printWithOptionalComments(pw, "-------------------------------------------------------", false, "");
    printWithOptionalComments(pw, "", false, "");
    printWithOptionalComments(pw, "List of Defined Functions:", false, "");
    printWithOptionalComments(pw, "Total number of defined functions:", false, "");
    printWithOptionalComments(pw, sd.numfunction, false, "");
    printWithOptionalComments(pw, "Total number of defined nodes:", false, "");
    printWithOptionalComments(pw, sd.numnode, false, "");
    
    for (int ifn=1; ifn<=sd.numfunction; ifn++) {

        printWithOptionalComments(pw, "-------------------------------------------------------", false, "");
        printWithOptionalComments(pw, "Function Number:", false, "");
        printWithOptionalComments(pw, ifn, annotateData, "ifn");
        printWithOptionalComments(pw, functions[ifn].name, false, ""); // text, can't comment
        printWithOptionalComments(pw, functions[ifn].ifgroup, annotateData, "functions[ifn="+ifn+"].ifgroup");
        printWithOptionalComments(pw, functions[ifn].floadtype, annotateData, "functions["+ifn+"].floadtype");

        // 11July - added
        printWithOptionalComments(pw, "Objective Function Flag:", false, "");
        printWithOptionalComments(pw, "True", false, "");

        for (int inod=1; inod<=sd.numnode; inod++) {

            printWithOptionalComments(pw, functions[ifn].costpercall[inod],    annotateData, "functions["+ifn+"].costpercall[inod="+inod+"]");
            printWithOptionalComments(pw, functions[ifn].costpernode[inod],    annotateData, "functions["+ifn+"].costpernode["+inod+"]");
            printWithOptionalComments(pw, functions[ifn].costpermonth[inod],   annotateData, "functions["+ifn+"].costpermonth["+inod+"]");
 
            float cpuNorm = functions[ifn].cpu[inod] / 1.0f; // ??? sd.nodeinfo[inod].cpuutilmax; // was, essentially, / 1.0, before 15sept04
            
            // printWithOptionalComments(pw, functions[ifn].cpu[inod],     annotateData, "functions["+ifn+"].cpu["+inod+"]");
            printWithOptionalComments(pw, cpuNorm,					    annotateData, "functions["+ifn+"].cpu["+inod+"]");
            
            printWithOptionalComments(pw, functions[ifn].cpurate[inod], annotateData, "functions["+ifn+"].cpurate["+inod+"]");
            printWithOptionalComments(pw, functions[ifn].memory[inod],  annotateData, "functions["+ifn+"].memory["+inod+"]");
    
            printWithOptionalComments(pw, functions[ifn].diskiopercall[inod],  annotateData, "functions["+ifn+"].diskiopercall["+inod+"]");
            printWithOptionalComments(pw, functions[ifn].diskspacereq[inod],   annotateData, "functions["+ifn+"].diskspacereq["+inod+"]");
            printWithOptionalComments(pw, functions[ifn].felig[inod],          annotateData, "functions["+ifn+"].felig["+inod+"]");
            printWithOptionalComments(pw, functions[ifn].flock[inod],          annotateData, "functions["+ifn+"].flock["+inod+"]");
            } // Next inod

        for (int ifn2=1; ifn2<=sd.numfunction; ifn2++) {
            
            printWithOptionalComments(pw, functions[ifn].fcall[ifn2].ifgroup,     annotateData, "functions["+ifn+"].fcall[ifn2="+ifn2+"].ifgroup");
            printWithOptionalComments(pw, functions[ifn].fcall[ifn2].sendmsgsize, annotateData, "functions["+ifn+"].fcall["+ifn2+"].sendmsgsize");
            printWithOptionalComments(pw, functions[ifn].fcall[ifn2].recvmsgsize, annotateData, "functions["+ifn+"].fcall["+ifn2+"].recvmsgsize");
            printWithOptionalComments(pw, functions[ifn].fcall[ifn2].sendmsgrate, annotateData, "functions["+ifn+"].fcall["+ifn2+"].sendmsgrate");
            printWithOptionalComments(pw, functions[ifn].fcall[ifn2].callpct,     annotateData, "functions["+ifn+"].fcall["+ifn2+"].callpct");
            printWithOptionalComments(pw, functions[ifn].fcall[ifn2].callfreq,    annotateData, "functions["+ifn+"].fcall["+ifn2+"].callfreq");
 
            } // Next ifn2
           
        } // Next ifn


    // 'write function groups:
    int numfgroup = sd.numfgroup;

    printWithOptionalComments(pw, "-------------------------------------------------------", false, "");
    printWithOptionalComments(pw, "-------------------------------------------------------", false, "");
    printWithOptionalComments(pw, "List of Defined function groups", false, "");
    printWithOptionalComments(pw, "Total number of defined groups:", false, "");
    printWithOptionalComments(pw, numfgroup, false, "");
    for (int igp=1; igp<=numfgroup; igp++) {
        printWithOptionalComments(pw, functionGroups[igp], annotateData, "functionGroups " + igp);
        }

    pw.close();
    
    } // saveFunctions


/**
 * Write the links data. - stolen from 2002 code
 * 
 * NOT COMPATIBLE WITH 1.1 YET
 */
public static void
saveLinks(String baseName, SystemDesign sd, boolean annotateData) {

	SystemDesign.logWarn(" saveLinks NOT COMPATIBLE WITH 1.1 YET");

    Link[] linkinfo = sd.linkinfo;
//	18jun03 - unused:     Node[] nodeinfo = sd.nodeinfo;
    
    PrintWriter pw = null;
    try {
        pw = new PrintWriter(new FileWriter(baseName + ".linkdef"));
    }
    catch (IOException ioe) {
        ioe.printStackTrace();
        return;
    }

    SystemDesign.logDebug("processing links....");
    
    printWithOptionalComments(pw, "-------------------------------------------------------", false, "");
    printWithOptionalComments(pw, "-------------------------------------------------------", false, "");
    printWithOptionalComments(pw, "", false, "");
    printWithOptionalComments(pw, "List of Defined Links:",         false, "");
    printWithOptionalComments(pw, "Total number of defined Links:", false, "");
    printWithOptionalComments(pw, sd.numlink,                          false, "");  // no need to comment
    printWithOptionalComments(pw, "Total number of defined nodes:", false, "");  // even though there's no node data here!?
    printWithOptionalComments(pw, sd.numnode,                          false, "");  // no need to comment

    for (int ilink=1; ilink<=sd.numlink; ilink++) {

        printWithOptionalComments(pw, "-------------------------------------------------------", false, "");
        printWithOptionalComments(pw, "Link Number:", false, "");
        printWithOptionalComments(pw, ilink, false, "");
        printWithOptionalComments(pw, linkinfo[ilink].name,    false, "");     // text, can't comment
        printWithOptionalComments(pw, linkinfo[ilink].node1,   annotateData, "node1");
        printWithOptionalComments(pw, linkinfo[ilink].node2,   annotateData, "node2");

        // 'save link icon position for user defined link positions
        printWithOptionalComments(pw, linkinfo[ilink].left,        annotateData, "left");
        printWithOptionalComments(pw, linkinfo[ilink].top,         annotateData, "top");
        printWithOptionalComments(pw, linkinfo[ilink].saveposition, annotateData, "saveposition");
        printWithOptionalComments(pw, linkinfo[ilink].linecolor,   annotateData, "linecolor");
        printWithOptionalComments(pw, linkinfo[ilink].linewidth,   annotateData, "linewidth");

        printWithOptionalComments(pw, linkinfo[ilink].background,  annotateData, "background");
        printWithOptionalComments(pw, linkinfo[ilink].bandwidth,   annotateData, "bandwidth");
        printWithOptionalComments(pw, linkinfo[ilink].costbase,    annotateData, "costbase");
        printWithOptionalComments(pw, linkinfo[ilink].costperkb,   annotateData, "costperkb");
        printWithOptionalComments(pw, linkinfo[ilink].costpermonth, annotateData, "costpermonth");
        printWithOptionalComments(pw, linkinfo[ilink].hops,        annotateData, "hops");
        printWithOptionalComments(pw, linkinfo[ilink].latency,     annotateData, "latency");
        printWithOptionalComments(pw, linkinfo[ilink].mtu,         annotateData, "mtu");
        printWithOptionalComments(pw, linkinfo[ilink].packetmax,   annotateData, "packetmax");
        printWithOptionalComments(pw, linkinfo[ilink].pfail,       annotateData, "pfail");
        printWithOptionalComments(pw, linkinfo[ilink].protocol,    annotateData, "protocol");
        printWithOptionalComments(pw, linkinfo[ilink].protocoloverhead, annotateData, "protocoloverhead");
        printWithOptionalComments(pw, linkinfo[ilink].type,        annotateData, "type");
        printWithOptionalComments(pw, linkinfo[ilink].text,        false, "linkinfo[ilink].text");    // text, can't comment

        // 'the text string is followed by the "(End-_-_of-_-_text!)" string to flag the end of string
        printWithOptionalComments(pw, "(End-_-_of-_-_text!)", false, "");

        } // Next ilink

    pw.close();

    } // saveLinks


/**
 * Print a string with optional comments.
 * If the comment is empty, don't echo the comment start char ("#").
 */
private static void
printWithOptionalComments(PrintWriter pw, String toPrint, boolean showComments, String comment) {
    
    if (showComments) {
        if (comment == null || comment.equals("")) {
            pw.print(toPrint + "\r\n");
        }
        else {
            pw.print(toPrint + "\t# " + comment + "\r\n");
        }
    }
    else {
        pw.print(toPrint + "\r\n");
    }
}

/**
 * Print an int with optional comments.
 */
private static void
printWithOptionalComments(PrintWriter pw, int toPrint, boolean showComments, String comment) {
    
    printWithOptionalComments(pw, ""+toPrint, showComments, comment);
}

/**
 * Print a float with optional comments.
 */
private static void
printWithOptionalComments(PrintWriter pw, float toPrint, boolean showComments, String comment) {
    
    printWithOptionalComments(pw, ""+toPrint, showComments, comment);
}

/**
 * Print a boolean with optional comments.
 */
private static void
printWithOptionalComments(PrintWriter pw, boolean toPrint, boolean showComments, String comment) {
    
    printWithOptionalComments(pw, toPrint?"True":"False", showComments, comment);
}


/**
 * Not really a Readin' and 'ritin' thing, but...
 * Convenient method to retrieve the full stack trace from a given exception.
 */
public static String
getStackTrace(Throwable t) {

	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw, true);
	t.printStackTrace(pw);
	pw.flush();
	pw.close();
	return sw.toString();
	}


} // class RnR
