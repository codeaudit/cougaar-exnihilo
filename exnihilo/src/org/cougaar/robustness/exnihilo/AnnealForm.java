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

import java.io.PrintWriter;
import java.io.Writer;


/**
	Port of annealform.frm.
	@author robert.e.cranfill@boeing.com

	@version EN2.20 - (VB "en 2.18") Several mods to improve remote messaging.
	@version EN2.09 - many changes, as noted
	@version 29April04 - Avoid NPE in getRandomState line 3538 (nee); checks for null, exits gRS if so.	
	@version 17Mar04 - In pursuit of NPEs in getRandomState(); also tidied up some System.out stuff.
	@version 18June03 - fixed "integration violations"
	@version 30April03 - incorporating latest ENVB changes.
	@version "1.1" - incorporating EN 1.1 changes
		- getRandomState completely redone
**/
public class AnnealForm {

public static final int		DEFAULT_ANNEALING_TIME_SEC = 60;

private final SystemDesign  systemDesign_;
private MetroParams         metroParams_ = null;
private TraceFunctionForm   traceFunctionForm_;

// rec - Instance vars created in refactoring.
// 15may03 - unused? private int nodeeventtemp[]     = new int[SystemDesign.numnodemax+1];
// 28aug3 - tidy - private int threadcall[]        = new int[SystemDesign.numfunctionmax+1]; // {VB2J [1080]}     Dim threadcall(numfunctionmax) As Integer
// EN2.09: unused: private int	threadcall[]	= new int[SystemDesign.numfunctionmax+1]; // {VB2J [1418]}     Dim threadcall(numfunctionmax) As Integer

// EN2.20 - added totfncall
private float  totfncall[] = new float[SystemDesign.numfunctionmax+1]; 

private int 	activenodes[]	= new int[SystemDesign.numnodemax+1];
// EN2.09: unused: private int 	inactivenodes[]	= new int[SystemDesign.numnodemax+1];	// 30april03
private float 	nodeutiltemp[]	= new float[SystemDesign.numnodemax+1];
private float 	nodememtemp[]	= new float[SystemDesign.numnodemax+1];

// EN2.09: 'en 2.01 load balancing cleanup. Delete this line
// private int		agentlisttemp[]	= new int[SystemDesign.numfunctionmax+1];	// 30april03

private float  	nodeutiltemp2[]	= new float[SystemDesign.numnodemax+1]; // 'en 1.1 speedup
private float  	nodememtemp2[]	= new float[SystemDesign.numnodemax+1]; // 'en 1.1 speedup
private int		nodelisttemp[]	= new int[SystemDesign.numnodemax+1];


// output vals for findSystemPower method
private float
	fsp_totcpuavail, /* // EN2.09: unused: fsp_totcpurequired, */ 
	fsp_totcpurequiredmin, fsp_totcpurequired25, fsp_totcpurequired50, fsp_totcpurequired75, fsp_totcpurequired100, 
	fsp_totmemoryavail, /* // EN2.09: unused: fsp_totmemoryrequired, */ 
	fsp_totmemoryrequiredmin, fsp_totmemoryrequired25, fsp_totmemoryrequired50, fsp_totmemoryrequired75, fsp_totmemoryrequired100;


// default annealing time; can be changed via 'setAnnealingTime'.
//
private int                 annealingTime_ = DEFAULT_ANNEALING_TIME_SEC; // in seconds

//EN2.09: unused: private static final int qdim = 100;                                                   // {VB2J [1060]}	Const qdim = 100

/* rec - unused: private int iqfirst; */ 
//EN2.09: unused: private int iqlast;                                                       // {VB2J [1061]}	Dim iqfirst As Integer, iqlast As Integer
// rec - unused: private int qarray[] = new int[qdim+1];                                                // {VB2J [1062]}	Dim qarray(qdim) As Integer
// rec - unused: private static final int junkmax = 10000;                                              // {VB2J [1065]}	Const junkmax = 10000
// rec - unused: private int junk[][] = new int[junkmax+1][2+1];                                        // {VB2J [1066]}	Dim junk(junkmax, 2) As Integer
// rec - unused: private int fstatecalltemp[][] = new int[SystemDesign.numfunctionmax+1][SystemDesign.numnodemax+1];              // {VB2J [1068]}	Dim fstatecalltemp(numfunctionmax, numnodemax) As Integer

private int iter; int iterk; int iterm;                                                // {VB2J [1070]}	Dim iter As Integer, iterk As Integer, iterm As Integer

// rec - added for iteration count
private long longIter_ = 0;


// rec - GUI: private boolean drawincrement;                                                         // {VB2J [1072]}	Dim drawincrement As Boolean

// rec - unused: private int drawmsgpathoptChecksave;                                                   // {VB2J [1074]}	Dim drawmsgpathoptChecksave As Integer
// rec - unused: private int drawmsgpathsearchChecksave;                                                // {VB2J [1075]}	Dim drawmsgpathsearchChecksave As Integer
// rec - unused: private int drawmsgpathannealchecksave;                                                // {VB2J [1076]}	Dim drawmsgpathannealchecksave As Integer

// 30April03
private boolean	initiallaydownflag;			//   Dim initiallaydownflag As Boolean

// rec - These were GUI components, now instance vars
public boolean randomAgentSeed = true; // was RandomAgentSeedCheck
public boolean iterate = false; // was iterateCheck
public boolean lockstate = false;

private int numsurvivelist;

// ***********************************************

private boolean writebestsolution = true;	// ???

// EN2.09 
boolean binarysearchsoftconstraints = false;
boolean loadbalconstraints = false;

// For test output
long iterations_ = 0;


/**
 * Constructor takes the SystemDesign form we're instantiated by, so we can access its instance vars. 
 */
public
AnnealForm(SystemDesign systemDesign) {

    systemDesign_ = systemDesign;
    traceFunctionForm_ = new TraceFunctionForm(systemDesign_);

    // Do this object's init stuff.
    //
    this.formLoad();
	}


/**
	doInitialLaydown

	@version 1.1	rxbh; This is now a private method; initialLaydown() is the public thing to call.
**/
private void
doInitialLaydown() {

	// Check project?? nahh....
//	if (systemDesign_.checkProject() == false) {
//		// blow chow
//		}

	// rec - FYI
	long initialStart = System.currentTimeMillis();
	if (systemDesign_.isInfoEnabled())
		SystemDesign.logInfo("doInitialLaydown: Finding initial solution....");


	initializeArchive();
	randomAgentSeed = false; // 1.1
	initiallaydownflag = true;

//		'marc en 1.1 speedup
	systemDesign_.solverwarmup = true;

	// changed to 100 somewhere <1.1?
	//
	for (int i=1; i<=100; i++) {
		annealingStep();
		if (systemDesign_.state.status)  {
			if (systemDesign_.isInfoEnabled()) {
				SystemDesign.logInfo("doInitialLaydown: found after " + i + " iterations, " + 
				                     (System.currentTimeMillis()-initialStart) + " milliseconds.");
				}
			break;
			}
		}

//		'marc en 1.1 speedup
	systemDesign_.solverwarmup = false;

	if (systemDesign_.state.status == false)  {
		if (systemDesign_.isWarnEnabled())
			SystemDesign.logWarn("doInitialLaydown: no solution found in 100 iterations.");
		}

	if (systemDesign_.isInfoEnabled())
		SystemDesign.logInfo("doInitialLaydown: continuing....");


// creation of MetroParams now is in initialLaydown() method
// aug03

//	// rec - reworked - some MetroParams stuff unnecessary
//	//
//	metroParams_ = new MetroParams(this, systemDesign_, annealingTime_);
//
//		metroParams_.fixedcost = false;
//		metroParams_.monthlycost = false;
//		metroParams_.responsetime = false;
	metroParams_.autoAnneal();

	// rec - Set global that causes iteration to happen, and begin annealing.
	//
	iterate = true;
	annealingStep();

// don't kill the metroParams, we may use again
//	metroParams_ = null;

	} //	   doInitialLaydown


/**
 * @version EN2.09 - updated extensively
 */
public void 
initialLaydown() {                            // {VB2J [7]}	Private Sub initiallaydownCommand_Click()

// replaced by "fsp_" globals
//	   {VB2J [8]}	   'Dim totcpuavail As Single, totcpurequired As Single, totcpurequiredmin As Single, 
//	   totcpurequired25 As Single, totcpurequired50 As Single, totcpurequired75 As Single, 
//	   totcpurequired100 As Single, totmemoryavail As Single, totmemoryrequired As Single, 
//	   totmemoryrequiredmin As Single, totmemoryrequired25 As Single, totmemoryrequired50 As Single, 
//	   totmemoryrequired75 As Single, totmemoryrequired100 As Single
//

	boolean nodeusedtemp[] = new boolean[SystemDesign.numnodemax+1];                          // {VB2J [9]}	   Dim nodeusedtemp(numnodemax) As Boolean 'marc EN 1.1 smoothing

	float cpufactorlist[] = new float[101];
	float memoryfactorlist[] = new float[101]; // {VB2J [11]}	   Dim cpufactorlist(100) As Single, memoryfactorlist(100) As Single
	   
	   
// rec - test, unused:  boolean foundsoftsolution = false;                                                   // {VB2J [14]}	   foundsoftsolution = false
  boolean beginbinarysearch = false;                                                   // {VB2J [15]}	   beginbinarysearch = false

// rec - added
	float cpufactor, cpufactormin=0, cpufactormax=0, memoryfactor, memoryfactormin=0, memoryfactormax=0;
	float xcpufactornew, xmemoryfactornew;
	float dcpufactor, dmemoryfactor;
	float dcpupctmaxmin, dmemorypctmaxmin;

	// rec - reworked - some MetroParams stuff unnecessary
	//
	metroParams_ = new MetroParams(this, systemDesign_, annealingTime_);


	// 1.1 - hack frickin' hack....
	// none of the following VB code ('cept in one place, not the one we need) sets this...
	// (perhaps cuz the VB control is clicked by the user?)
	//
	if (systemDesign_.blendpfailhamming) {
		metroParams_.hammingCheck = true;
		}

    int numruns;
   
	// EN2.09
	// 'en 2.01 load balancing, constraints on load balancing
	// 'en 2.01 this whole routine has been modified
   if (!binarysearchsoftconstraints) {

      // 'a single step is needed to initialize the metrparmex form
      annealingStep();
      
      // 'en 2.01 load balancing soft constraints
      if (systemDesign_.blendpfailsurvive || 
          systemDesign_.blendpfailmessaging || 
          systemDesign_.blendpfailhamming || 
          systemDesign_.blendpfailloadbal) {

         metroParams_.pfailureCheck = true;
         metroParams_.pfailuresurviveCheck = false;
         metroParams_.remotetrafficCheck = false;
         metroParams_.hammingCheck = false;
         metroParams_.loadbalCheck = false;
       	}
      
      if (systemDesign_.blendloadbalmessaging) {
          metroParams_.pfailureCheck = false;
          metroParams_.pfailuresurviveCheck = false;
          metroParams_.remotetrafficCheck = false;
          metroParams_.hammingCheck = false;
          metroParams_.loadbalCheck = true;
	  	}
      
      numruns = 0;

      // 'first increment
      initializeArchive();
      numruns++;
      doInitialLaydown();

      loadbalconstraints = true;
      
      if ((systemDesign_.numstatelist > 0) && 
          (!systemDesign_.blendpfailsurvive) && 
          (!systemDesign_.blendpfailmessaging) && 
          (!systemDesign_.blendpfailhamming) && 
          (!systemDesign_.blendpfailloadbal) && 
          (!systemDesign_.blendloadbalmessaging) ) {
        
         // 'case of a single pass strategy that has succeeded
        return;
      	}
      
      if (systemDesign_.numstatelist <= 0) {

         // 'first pass was a failure
         // 'switch to load balancing strategy, if not already done
         // 'All multipass strategies are converted to load balancing of cpu/memory
         
        loadbalconstraints = false;
         
        if (systemDesign_.loadbalscale <= 0) {
            // 'switch strategy to load balancing
            metroParams_.pfailureCheck = false;
            metroParams_.pfailuresurviveCheck = false;
            metroParams_.remotetrafficCheck = false;
            metroParams_.hammingCheck = false;
            metroParams_.loadbalCheck = true;
         	}
     
        initializeArchive();
        numruns++;
        doInitialLaydown();
		return;
		}

	}

   // 'run is a multipass strategy, with first pass successful, so continue processing second pass

   else { //  'marc en 2.01 case where binarysearchsoftconstraints = true
	 // end EN2.09


	  for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                                   // {VB2J [17]}	   For ifn = 1 To numfunction
		for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [18]}	      For inod = 1 To numnode
		  // {VB2J [19]}	         'xnfunctionfirstpass(ifn).felig(inod) = xnfunction(ifn).felig(inod)
		  systemDesign_.xnfunctionfirstpass[ifn].cpurate[inod] = systemDesign_.xnfunction[ifn].cpurate[inod];              // {VB2J [20]}	         xnfunctionfirstpass(ifn).cpurate(inod) = xnfunction(ifn).cpurate(inod)
		  systemDesign_.xnfunctionfirstpass[ifn].memory[inod] = systemDesign_.xnfunction[ifn].memory[inod];              // {VB2J [21]}	         xnfunctionfirstpass(ifn).memory(inod) = xnfunction(ifn).memory(inod)
		  }                                                                        // {VB2J [22]}	      Next inod
		}                                                                          // {VB2J [23]}	   Next ifn


//	Original VB set param values by side-effect; we have to use globals (with "fcp_" prefix)
//	   {VB2J [26]}	   findsystempower 
//							totcpuavail, totcpurequired, totcpurequiredmin, 
//							totcpurequired25, totcpurequired50, totcpurequired75, totcpurequired100, 
//							totmemoryavail, totmemoryrequired, 
//							totmemoryrequiredmin, totmemoryrequired25, totmemoryrequired50, totmemoryrequired75, totmemoryrequired100
//
	findSystemPower();

	systemDesign_.cpufactor = fsp_totcpuavail / fsp_totcpurequiredmin;                                 // {VB2J [28]}	   cpufactor = totcpuavail / totcpurequiredmin
	systemDesign_.memoryfactor = fsp_totmemoryavail / fsp_totmemoryrequiredmin;                        // {VB2J [29]}	   memoryfactor = totmemoryavail / totmemoryrequiredmin

	  if (systemDesign_.cpufactor < 1)  {                                                        // {VB2J [31]}	   If (cpufactor < 1) Then
		for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                                 // {VB2J [32]}	      For ifn = 1 To numfunction
		  if (systemDesign_.xnfunction[ifn].objfn)  {                                            // {VB2J [33]}	         If (xnfunction(ifn).objfn) Then
			for (int inod=1; inod<=systemDesign_.numnode; inod++) {                              // {VB2J [34]}	            For inod = 1 To numnode
			  systemDesign_.xnfunction[ifn].cpurate[inod] = systemDesign_.xnfunction[ifn].cpurate[inod] * systemDesign_.cpufactor;              // {VB2J [35]}	               xnfunction(ifn).cpurate(inod) = xnfunction(ifn).cpurate(inod) * cpufactor
			  }                                                                    // {VB2J [36]}	            Next inod
			}                                                                      // {VB2J [37]}	         End If
		  }                                                                        // {VB2J [38]}	      Next ifn
		} else {                                                                   // {VB2J [39]}	   Else
		    systemDesign_.cpufactor = 1;                                                             // {VB2J [40]}	      cpufactor = 1
		}                                                                          // {VB2J [41]}	   End If

	  if (systemDesign_.memoryfactor < 1)  {                                                     // {VB2J [43]}	   If (memoryfactor < 1) Then
		for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                                 // {VB2J [44]}	      For ifn = 1 To numfunction
		  if (systemDesign_.xnfunction[ifn].objfn)  {                                            // {VB2J [45]}	         If (xnfunction(ifn).objfn) Then
			for (int inod=1; inod<=systemDesign_.numnode; inod++) {                              // {VB2J [46]}	            For inod = 1 To numnode
			  systemDesign_.xnfunction[ifn].memory[inod] = systemDesign_.xnfunction[ifn].memory[inod] * systemDesign_.memoryfactor;              // {VB2J [47]}	               xnfunction(ifn).memory(inod) = xnfunction(ifn).memory(inod) * memoryfactor
			  }                                                                    // {VB2J [48]}	            Next inod
			}                                                                      // {VB2J [49]}	         End If
		  }                                                                        // {VB2J [50]}	      Next ifn
		} else {                                                                   // {VB2J [51]}	   Else
		systemDesign_.memoryfactor = 1;                                                          // {VB2J [52]}	      memoryfactor = 1
		}                                                                          // {VB2J [53]}	   End If

	  int ilist = 1;                                                                   // {VB2J [56]}	   ilist = 1
	  cpufactorlist[ilist] = systemDesign_.cpufactor;                                            // {VB2J [57]}	   cpufactorlist(ilist) = cpufactor
	  memoryfactorlist[ilist] = systemDesign_.memoryfactor;                                      // {VB2J [58]}	   memoryfactorlist(ilist) = memoryfactor

//	   {VB2J [61]}	   'a single step is needed to initialize the metrparmex form
	  annealingStep();                                           // {VB2J [62]}	   annealingstepCommand.Value = true


	  if (systemDesign_.blendpfailsurvive  
	   || systemDesign_.blendpfailmessaging 
	   || systemDesign_.blendpfailhamming)  { 										// {VB2J [64]}	   If (blendpfailsurviveCheck.Value = 1 Or blendpfailmessagingCheck.Value = 1 Or blendpfailhammingCheck.Value = 1) Then

		metroParams_.pfailureCheck = true;                                        // {VB2J [65]}	      metrparmex.pfailureCheck.Value = 1
		metroParams_.pfailuresurviveCheck = false;                                 // {VB2J [66]}	      metrparmex.pfailuresurviveCheck.Value = 0
		metroParams_.remotetrafficCheck = false;                                   // {VB2J [67]}	      metrparmex.remotetrafficCheck.Value = 0
		metroParams_.hammingCheck = false;                                         // {VB2J [68]}	      metrparmex.hammingCheck.Value = 0

//		  DoEvents();         ;                                                      // {VB2J [69]}	      DoEvents
		}                                                                          // {VB2J [70]}	   End If

	  numruns = 0;                                                                 // {VB2J [72]}	   numruns = 0


//	   {VB2J [73]}	   'first increment
	  while (true) {                                                               // {VB2J [74]}	   Do While (true)

		initializeArchive();                          // {VB2J [75]}	      annealForm.initializearchiveCommand.Value = true
//	   {VB2J [76]}	      'en 1.1 soft constraints
	   systemDesign_.dcpupctmaxmin = Integer.MAX_VALUE; // 9E+19;                                                     // {VB2J [77]}	      dcpupctmaxmin = 9E+19
	   systemDesign_.dmemorypctmaxmin = Integer.MAX_VALUE; // 9E+19;                                                  // {VB2J [78]}	      dmemorypctmaxmin = 9E+19

		numruns++;                                                     // {VB2J [80]}	      numruns = numruns + 1
//		  DoEvents();         ;                                                      // {VB2J [81]}	      DoEvents
		doInitialLaydown();                                                        // {VB2J [82]}	      doInitialLaydown (false)
//		  runcountLabel.Caption = numruns;                                           // {VB2J [83]}	      runcountLabel.Caption = numruns
//		  DoEvents();         ;                                                      // {VB2J [84]}	      DoEvents


		if (systemDesign_.numstatelist <= 0)  {                                                  // {VB2J [87]}	      If (numstatelist <= 0) Then

//	   {VB2J [89]}	         'marc en 1.1 soft constraints
//	   {VB2J [90]}	         'add a 10% buffer to the last projection (e.g., dcpupctmaxmin * 1.1)
	   xcpufactornew = (float)(1.0 / (1.0 + systemDesign_.dcpupctmaxmin * 1.37));                        // {VB2J [91]}	         xcpufactornew = 1# / (1# + dcpupctmaxmin * 1.37)
	   xmemoryfactornew = (float)(1.0 / (1.0 + systemDesign_.dmemorypctmaxmin * 1.37));                  // {VB2J [92]}	         xmemoryfactornew = 1# / (1# + dmemorypctmaxmin * 1.37)

	   systemDesign_.cpufactor    = xcpufactornew * systemDesign_.cpufactor;                                   // {VB2J [94]}	         cpufactor = xcpufactornew * cpufactor
	   systemDesign_.memoryfactor = xmemoryfactornew * systemDesign_.memoryfactor;                          // {VB2J [95]}	         memoryfactor = xmemoryfactornew * memoryfactor

// EN2.09 - removed
////	   {VB2J [98]}	   'en 1.1 bug check
//		  if (xmemoryfactornew == 0)  {                                            // {VB2J [99]}	   If (xmemoryfactornew = 0) Then
//			if (systemDesign_.isErrorEnabled())
//				SystemDesign.logError("skewed MEMORY FACTOR=0 #1");                                    // {VB2J [100]}	      MsgBox "screwed MEMORY FACTOR = 0"
//			}                                                                      // {VB2J [101]}	   End If

		  for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                               // {VB2J [104]}	         For ifn = 1 To numfunction
			if (systemDesign_.xnfunction[ifn].objfn)  {                                          // {VB2J [105]}	            If (xnfunction(ifn).objfn) Then
			  for (int inod=1; inod<=systemDesign_.numnode; inod++) {                            // {VB2J [106]}	               For inod = 1 To numnode
				systemDesign_.xnfunction[ifn].cpurate[inod] = systemDesign_.xnfunction[ifn].cpurate[inod] * xcpufactornew;              // {VB2J [107]}	                  xnfunction(ifn).cpurate(inod) = xnfunction(ifn).cpurate(inod) * xcpufactornew
				systemDesign_.xnfunction[ifn].memory[inod] = systemDesign_.xnfunction[ifn].memory[inod] * xmemoryfactornew;              // {VB2J [108]}	                  xnfunction(ifn).memory(inod) = xnfunction(ifn).memory(inod) * xmemoryfactornew
				}                                                                  // {VB2J [109]}	               Next inod
			  }                                                                    // {VB2J [110]}	            End If
			}                                                                      // {VB2J [111]}	         Next ifn

		  ilist = numruns + 1;                                                     // {VB2J [113]}	         ilist = numruns + 1
		  cpufactorlist[ilist] = systemDesign_.cpufactor;                                        // {VB2J [114]}	         cpufactorlist(ilist) = cpufactor
		  memoryfactorlist[ilist] = systemDesign_.memoryfactor;                                  // {VB2J [115]}	         memoryfactorlist(ilist) = memoryfactor

		  } else {                                                                 // {VB2J [118]}	      Else

// {VB2J [119]}	         'case where numstatelist > 0. 'solution is ok, so take a step back and verify with longer search time:

		  if (numruns == 1)  break;                                           // {VB2J [121]}	         If (numruns = 1) Then Exit Do

			int numlist = numruns;                                                     // {VB2J [123]}	         numlist = numruns
			cpufactormin = systemDesign_.cpufactor;                                              // {VB2J [126]}	         cpufactormin = cpufactor 'absolute lower bound for cpufactor with working solution on fast search
			memoryfactormin = systemDesign_.memoryfactor;                                        // {VB2J [127]}	         memoryfactormin = memoryfactor

// uh, don't these get stepped on in the next few lines?
			xcpufactornew = (float)(1.0 / (1.0 + systemDesign_.dcpupctmaxmin * 1.1));                       // {VB2J [129]}	         xcpufactornew = 1# / (1# + dcpupctmaxmin * 1.1)
			xmemoryfactornew = (float)(1.0 / (1.0 + systemDesign_.dmemorypctmaxmin * 1.1));                 // {VB2J [130]}	         xmemoryfactornew = 1# / (1# + dmemorypctmaxmin * 1.1)

			xcpufactornew = cpufactorlist[numlist];                                // {VB2J [132]}	         xcpufactornew = cpufactorlist(numlist)
			xmemoryfactornew = memoryfactorlist[numlist];                          // {VB2J [133]}	         xmemoryfactornew = memoryfactorlist(numlist)

			if (xcpufactornew-1 > -0.00001 && xmemoryfactornew-1 > -0.00001)  {    // {VB2J [136]}	         If (xcpufactornew - 1 > -0.00001 And xmemoryfactornew - 1 > -0.00001) Then
			  break;                                                               // {VB2J [137]}	            Exit Do
			  }                                                                    // {VB2J [138]}	         End If

			ilist = numlist - 1;                                                   // {VB2J [141]}	         ilist = numlist - 1 'we will work backwards through the search list history

// EN2.09 - removed		
////	   {VB2J [146]}	   'en 1.1 bug check
//			if (memoryfactorlist[ilist] == 0)  {                                   // {VB2J [147]}	   If (memoryfactorlist(ilist) = 0) Then
//				if (systemDesign_.isErrorEnabled())
//			  		SystemDesign.logError("skewed MEMORY FACTOR=0 #2");                                  // {VB2J [148]}	      MsgBox "screwed MEMORY FACTOR = 0"
//			  }                                                                    // {VB2J [149]}	   End If



			for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                             // {VB2J [153]}	         For ifn = 1 To numfunction
			  if (systemDesign_.xnfunction[ifn].objfn)  {                                        // {VB2J [154]}	            If (xnfunction(ifn).objfn) Then
				for (int inod=1; inod<=systemDesign_.numnode; inod++) {                          // {VB2J [155]}	               For inod = 1 To numnode
				  systemDesign_.xnfunction[ifn].cpurate[inod] = systemDesign_.xnfunctionfirstpass[ifn].cpurate[inod] * cpufactorlist[ilist];              // {VB2J [156]}	                  xnfunction(ifn).cpurate(inod) = xnfunctionfirstpass(ifn).cpurate(inod) * cpufactorlist(ilist)
				  systemDesign_.xnfunction[ifn].memory[inod] = systemDesign_.xnfunctionfirstpass[ifn].memory[inod] * memoryfactorlist[ilist];              // {VB2J [157]}	                  xnfunction(ifn).memory(inod) = xnfunctionfirstpass(ifn).memory(inod) * memoryfactorlist(ilist)
				  }                                                                // {VB2J [158]}	               Next inod
				}                                                                  // {VB2J [159]}	            End If
			  }                                                                    // {VB2J [160]}	         Next ifn


																				   // {VB2J [163]}	         'cpufactor = cpufactor / xcpufactornew
																				   // {VB2J [164]}	         'memoryfactor = memoryfactor / xmemoryfactornew
			systemDesign_.cpufactor = cpufactorlist[ilist];                                      // {VB2J [165]}	         cpufactor = cpufactorlist(ilist)
			systemDesign_.memoryfactor = memoryfactorlist[ilist];                                // {VB2J [166]}	         memoryfactor = memoryfactorlist(ilist)

			initializeArchive();                      // {VB2J [170]}	         annealForm.initializearchiveCommand.Value = true
//	   {VB2J [171]}	         'en 1.1 soft constraints
	   systemDesign_.dcpupctmaxmin = Integer.MAX_VALUE; // 9E+19;                                                 // {VB2J [172]}	         dcpupctmaxmin = 9E+19
	   systemDesign_.dmemorypctmaxmin = Integer.MAX_VALUE; // 9E+19;                                              // {VB2J [173]}	         dmemorypctmaxmin = 9E+19

// hurk!
//			metroParams_.annealtime = 270;                              // {VB2J [175]}	         metrparmex.autoannealtimeText.Text = 270  'make it a long run

			numruns++;                                                 // {VB2J [177]}	         numruns = numruns + 1
//			DoEvents();         ;                                                  // {VB2J [178]}	         DoEvents
			doInitialLaydown();                                                    // {VB2J [179]}	         doInitialLaydown (false)
//			runcountLabel.Caption = numruns;                                       // {VB2J [180]}	         runcountLabel.Caption = numruns
//			DoEvents();         ;                                                  // {VB2J [181]}	         DoEvents


// {VB2J [186]}	'''''''''''''''
// {VB2J [187]}	''''''''' start of section on post-success search
// {VB2J [188]}	'''''''''''''''

			if (systemDesign_.numstatelist <= 0)  {                                              // {VB2J [190]}	         If (numstatelist <= 0) Then

//	   {VB2J [191]}	            'this case was a failure after a previous successful solution and backtrack
//	   {VB2J [192]}	            'the failure on backtrack, with a long search time, means we need to reverse again,
//	   {VB2J [193]}	            'and continue reducing, with current NON-solution used to set an absolute min

				cpufactormax = systemDesign_.cpufactor;                                            // {VB2J [194]}	            cpufactormax = cpufactor
				memoryfactormax = systemDesign_.memoryfactor;                                      // {VB2J [195]}	            memoryfactormax = memoryfactor
				
				beginbinarysearch = true;                                            // {VB2J [197]}	            beginbinarysearch = true
				break;                                                               // {VB2J [198]}	            Exit Do

				} else {                                                           // {VB2J [297]}	         Else

//	   {VB2J [298]}	            'this case was a success after a previous successful solution and backtrack with long runtime
//	   {VB2J [299]}	            'the success on backtrack means we need to backtrack further. Backup until we fail.

				cpufactormin = systemDesign_.cpufactor;                                          // {VB2J [301]}	            cpufactormin = cpufactor
				memoryfactormin = systemDesign_.memoryfactor;                                    // {VB2J [302]}	            memoryfactormin = memoryfactor


				while (true) {                                                     // {VB2J [305]}	            Do While (true)
				  ilist--;                                               // {VB2J [306]}	               ilist = ilist - 1

				  if (ilist < 1)  {                                                // {VB2J [308]}	               If (ilist < 1) Then
					beginbinarysearch = false;                                     // {VB2J [309]}	                  beginbinarysearch = false
					break;                                                         // {VB2J [310]}	                  Exit Do
					}                                                              // {VB2J [311]}	               End If

				  xcpufactornew = cpufactorlist[ilist];                            // {VB2J [313]}	               xcpufactornew = cpufactorlist(ilist)
				  xmemoryfactornew = memoryfactorlist[ilist];                      // {VB2J [314]}	               xmemoryfactornew = memoryfactorlist(ilist)

				  systemDesign_.cpufactor /= xcpufactornew;                           // {VB2J [316]}	               cpufactor = cpufactor / xcpufactornew
				  systemDesign_.memoryfactor /= xmemoryfactornew;                  // {VB2J [317]}	               memoryfactor = memoryfactor / xmemoryfactornew

																				   // {VB2J [319]}	               'cpufactor = (cpufactormax - cpufactormin) / 2#
																				   // {VB2J [320]}	               'memoryfactor = (memoryfactormax - memoryfactormin) / 2#

// EN2.09 - removed
//// {VB2J [325]}	   'en 1.1 bug check
//				  if (systemDesign_.memoryfactor == 0)  {                                        // {VB2J [326]}	   If (memoryfactor = 0) Then
//					if (systemDesign_.isErrorEnabled())
//						SystemDesign.logError("skewed MEMORY FACTOR=0 #3");                            // {VB2J [327]}	      MsgBox "screwed MEMORY FACTOR = 0"
//					}                                                              // {VB2J [328]}	   End If

				  for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                       // {VB2J [333]}	               For ifn = 1 To numfunction
					if (systemDesign_.xnfunction[ifn].objfn)  {                                  // {VB2J [334]}	                  If (xnfunction(ifn).objfn) Then
					  for (int inod=1; inod<=systemDesign_.numnode; inod++) {                    // {VB2J [335]}	                     For inod = 1 To numnode
						systemDesign_.xnfunction[ifn].cpurate[inod] 
							= systemDesign_.xnfunctionfirstpass[ifn].cpurate[inod] * systemDesign_.cpufactor;              // {VB2J [336]}	                        xnfunction(ifn).cpurate(inod) = xnfunctionfirstpass(ifn).cpurate(inod) * cpufactor
						systemDesign_.xnfunction[ifn].memory[inod] 
							= systemDesign_.xnfunctionfirstpass[ifn].memory[inod] * systemDesign_.memoryfactor;              // {VB2J [337]}	                        xnfunction(ifn).memory(inod) = xnfunctionfirstpass(ifn).memory(inod) * memoryfactor
						}                                                          // {VB2J [338]}	                     Next inod
					  }                                                            // {VB2J [339]}	                  End If
					}                                                              // {VB2J [340]}	               Next ifn

				  initializeArchive();                // {VB2J [342]}	               annealForm.initializearchiveCommand.Value = true
																				   // {VB2J [343]}	               'en 1.1 soft constraints
					systemDesign_.dcpupctmaxmin = Integer.MAX_VALUE; // 9E+19;                                           // {VB2J [344]}	               dcpupctmaxmin = 9E+19
					systemDesign_.dmemorypctmaxmin = Integer.MAX_VALUE; //  9E+19;                                        // {VB2J [345]}	               dmemorypctmaxmin = 9E+19

//	hurk!!!
//				  metroParams_.annealtime = 270;                        // {VB2J [347]}	               metrparmex.autoannealtimeText.Text = 270

				  numruns++;                                           // {VB2J [349]}	               numruns = numruns + 1
//					DoEvents();         ;                                            // {VB2J [350]}	               DoEvents
				  doInitialLaydown();                                              // {VB2J [351]}	               doInitialLaydown (false)
//					runcountLabel.Caption = numruns;                                 // {VB2J [352]}	               runcountLabel.Caption = numruns
//					DoEvents();         ;                                            // {VB2J [353]}	               DoEvents

				  if (systemDesign_.numstatelist <= 0)  {                                        // {VB2J [355]}	               If (numstatelist <= 0) Then
// {VB2J [356]}	                  'failed state, so we can begin the binary search
				   cpufactormax = systemDesign_.cpufactor;                                      // {VB2J [357]}	                  cpufactormax = cpufactor
				   memoryfactormax = systemDesign_.memoryfactor;                                // {VB2J [358]}	                  memoryfactormax = memoryfactor
					beginbinarysearch = true;                                      // {VB2J [359]}	                  beginbinarysearch = true
					break;                                                         // {VB2J [360]}	                  Exit Do
					}                                                              // {VB2J [361]}	               End If

				  cpufactormin = systemDesign_.cpufactor;                                        // {VB2J [363]}	               cpufactormin = cpufactor
				  memoryfactormin = systemDesign_.memoryfactor;                                  // {VB2J [364]}	               memoryfactormin = memoryfactor
				  }                                                                // {VB2J [366]}	            Loop
				}                                                                  // {VB2J [369]}	         End If



//	   {VB2J [370]}	'''''''''''''''
//	   {VB2J [371]}	''''''''' end of section on post-success search
//	   {VB2J [372]}	'''''''''''''''

			  break;                                                               // {VB2J [379]}	         Exit Do
			  }                                                                    // {VB2J [380]}	      End If
			} // while true                                                                     // {VB2J [381]}	   Loop

//	   {VB2J [387]}	   'binary search section for soft constraints:

		  if (beginbinarysearch)  {                                                // {VB2J [390]}	   If (beginbinarysearch) Then

			cpufactor = (float)((cpufactormax + cpufactormin) / 2.0);                        // {VB2J [392]}	      cpufactor = (cpufactormax + cpufactormin) / 2#
			memoryfactor = (float)((memoryfactormax + memoryfactormin) / 2.0);               // {VB2J [393]}	      memoryfactor = (memoryfactormax + memoryfactormin) / 2#

			for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                             // {VB2J [395]}	      For ifn = 1 To numfunction
			  if (systemDesign_.xnfunction[ifn].objfn)  {                                        // {VB2J [396]}	         If (xnfunction(ifn).objfn) Then
				for (int inod=1; inod<=systemDesign_.numnode; inod++) {                          // {VB2J [397]}	            For inod = 1 To numnode
				  systemDesign_.xnfunction[ifn].cpurate[inod] = systemDesign_.xnfunctionfirstpass[ifn].cpurate[inod] * cpufactor;              // {VB2J [398]}	               xnfunction(ifn).cpurate(inod) = xnfunctionfirstpass(ifn).cpurate(inod) * cpufactor
				  systemDesign_.xnfunction[ifn].memory[inod] = systemDesign_.xnfunctionfirstpass[ifn].memory[inod] * memoryfactor;              // {VB2J [399]}	               xnfunction(ifn).memory(inod) = xnfunctionfirstpass(ifn).memory(inod) * memoryfactor
				  }                                                                // {VB2J [400]}	            Next inod
				}                                                                  // {VB2J [401]}	         End If
			  }                                                                    // {VB2J [402]}	      Next ifn

// {VB2J [404]}	      'cpufactormin and cpufactormax are set, so begin binary search:
// {VB2J [406]}	      'test for convergence:

   dcpufactor = (cpufactormax - cpufactormin) / cpufactor;                // {VB2J [407]}	      dcpufactor = (cpufactormax - cpufactormin) / cpufactor
   dmemoryfactor = (memoryfactormax - memoryfactormin) / memoryfactor;              // {VB2J [408]}	      dmemoryfactor = (memoryfactormax - memoryfactormin) / memoryfactor

			while (true) {                                                         // {VB2J [410]}	      Do While (true)
			  if (dcpufactor < systemDesign_.memoryoverloadbinarysearchtolerance 
			  	&& dmemoryfactor < systemDesign_.memoryoverloadbinarysearchtolerance)  { // {VB2J [411]}	         If (dcpufactor < memoryoverloadbinarysearchtolerance And dmemoryfactor < memoryoverloadbinarysearchtolerance) Then

// {VB2J [412]}	            'set cpufactor = cpufactormin and rerun solver
				initializeArchive();                  // {VB2J [413]}	            annealForm.initializearchiveCommand.Value = true

// {VB2J [414]}	            'en 1.1 soft constraints
   systemDesign_.dcpupctmaxmin = Integer.MAX_VALUE; // 9E+19;                                             // {VB2J [415]}	            dcpupctmaxmin = 9E+19
   systemDesign_.dmemorypctmaxmin = Integer.MAX_VALUE; // 9E+19;                                          // {VB2J [416]}	            dmemorypctmaxmin = 9E+19

// {VB2J [418]}	            'add a small (0.99) buffer to ensure (?) a successful solution
				cpufactor = (float)(cpufactormin * 0.99);                                   // {VB2J [419]}	            cpufactor = cpufactormin * 0.99
				memoryfactor = (float)(memoryfactormin * 0.99);                             // {VB2J [420]}	            memoryfactor = memoryfactormin * 0.99

				for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                         // {VB2J [422]}	            For ifn = 1 To numfunction
				  if (systemDesign_.xnfunction[ifn].objfn)  {                                    // {VB2J [423]}	               If (xnfunction(ifn).objfn) Then
					for (int inod=1; inod<=systemDesign_.numnode; inod++) {                      // {VB2J [424]}	                  For inod = 1 To numnode
					  systemDesign_.xnfunction[ifn].cpurate[inod] = systemDesign_.xnfunctionfirstpass[ifn].cpurate[inod] * cpufactor;              // {VB2J [425]}	                     xnfunction(ifn).cpurate(inod) = xnfunctionfirstpass(ifn).cpurate(inod) * cpufactor
					  systemDesign_.xnfunction[ifn].memory[inod] = systemDesign_.xnfunctionfirstpass[ifn].memory[inod] * memoryfactor;              // {VB2J [426]}	                     xnfunction(ifn).memory(inod) = xnfunctionfirstpass(ifn).memory(inod) * memoryfactor
					  }                                                            // {VB2J [427]}	                  Next inod
					}                                                              // {VB2J [428]}	               End If
				  }                                                                // {VB2J [429]}	            Next ifn

				numruns++;                                             // {VB2J [431]}	            numruns = numruns + 1
//				  DoEvents();         ;                                              // {VB2J [432]}	            DoEvents
				doInitialLaydown();                                                // {VB2J [433]}	            doInitialLaydown (false)
//				  runcountLabel.Caption = numruns;                                   // {VB2J [434]}	            runcountLabel.Caption = numruns
//				  DoEvents();         ;                                              // {VB2J [435]}	            DoEvents


				if (systemDesign_.numstatelist <= 0)  {                                          // {VB2J [438]}	            If (numstatelist <= 0) Then
				  if (systemDesign_.isInfoEnabled())
				  	SystemDesign.logInfo( "this should never happen... unless we are on the brittle edge of finding a solution");                                              // {VB2J [439]}	               MsgBox "this should never happen... unless we are on the brittle edge of finding a solution"
				  }                                                                // {VB2J [440]}	            End If

//	   {VB2J [442]}	            'foundsoftsolution = true
				break;                                                             // {VB2J [443]}	            Exit Do
				}                                                                  // {VB2J [444]}	         End If

			  initializeArchive();                    // {VB2J [446]}	         annealForm.initializearchiveCommand.Value = true
	 // {VB2J [447]}	         'en 1.1 soft constraints
			  dcpupctmaxmin = Integer.MAX_VALUE; // 9E+19;                                               // {VB2J [448]}	         dcpupctmaxmin = 9E+19
			  dmemorypctmaxmin = Integer.MAX_VALUE; // 9E+19;                                            // {VB2J [449]}	         dmemorypctmaxmin = 9E+19

			  numruns++;                                               // {VB2J [451]}	         numruns = numruns + 1
//				DoEvents();         ;                                                // {VB2J [452]}	         DoEvents
			  doInitialLaydown();                                                  // {VB2J [453]}	         doInitialLaydown (false)
//				runcountLabel.Caption = numruns;                                     // {VB2J [454]}	         runcountLabel.Caption = numruns
//				DoEvents();         ;                                                // {VB2J [455]}	         DoEvents

			  if (systemDesign_.numstatelist <= 0)  {                                            // {VB2J [457]}	         If (numstatelist <= 0) Then
// {VB2J [458]}	            'failed solution
				cpufactormax = cpufactor;                                          // {VB2J [459]}	            cpufactormax = cpufactor
				memoryfactormax = memoryfactor;                                    // {VB2J [460]}	            memoryfactormax = memoryfactor
				} else {                                                           // {VB2J [461]}	         Else
// {VB2J [462]}	            'successful solution
				cpufactormin = cpufactor;                                          // {VB2J [463]}	            cpufactormin = cpufactor
				memoryfactormin = memoryfactor;                                    // {VB2J [464]}	            memoryfactormin = memoryfactor
				}                                                                  // {VB2J [465]}	         End If

			  cpufactor = (float)((cpufactormax + cpufactormin) / 2.0);                      // {VB2J [467]}	         cpufactor = (cpufactormax + cpufactormin) / 2#
			  memoryfactor = (float)((memoryfactormax + memoryfactormin) / 2.0);             // {VB2J [468]}	         memoryfactor = (memoryfactormax + memoryfactormin) / 2#

			  for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                           // {VB2J [470]}	         For ifn = 1 To numfunction
				if (systemDesign_.xnfunction[ifn].objfn)  {                                      // {VB2J [471]}	            If (xnfunction(ifn).objfn) Then
				  for (int inod=1; inod<=systemDesign_.numnode; inod++) {                        // {VB2J [472]}	               For inod = 1 To numnode
					systemDesign_.xnfunction[ifn].cpurate[inod] = systemDesign_.xnfunctionfirstpass[ifn].cpurate[inod] * cpufactor;              // {VB2J [473]}	                  xnfunction(ifn).cpurate(inod) = xnfunctionfirstpass(ifn).cpurate(inod) * cpufactor
					systemDesign_.xnfunction[ifn].memory[inod] = systemDesign_.xnfunctionfirstpass[ifn].memory[inod] * memoryfactor;              // {VB2J [474]}	                  xnfunction(ifn).memory(inod) = xnfunctionfirstpass(ifn).memory(inod) * memoryfactor
					}                                                              // {VB2J [475]}	               Next inod
				  }                                                                // {VB2J [476]}	            End If
				}                                                                  // {VB2J [477]}	         Next ifn

			  dcpufactor = (cpufactormax - cpufactormin) / cpufactor;              // {VB2J [479]}	         dcpufactor = (cpufactormax - cpufactormin) / cpufactor
			  dmemoryfactor = (memoryfactormax - memoryfactormin) / memoryfactor;              // {VB2J [480]}	         dmemoryfactor = (memoryfactormax - memoryfactormin) / memoryfactor
			  }                                                                    // {VB2J [481]}	      Loop

																				   // {VB2J [483]}	      'If (foundsoftsolution) Then Exit Do
			}                                                                      // {VB2J [484]}	   End If

   } // EN2.09 - 'end of case for binary search for soft constraints

// EN2.09 -  'marc en 2.01 load balancing soft constraints

//	   {VB2J [497]}	   'marc EN 1.1 smoothing
		  if (	   systemDesign_.blendpfailsurvive 
				|| systemDesign_.blendpfailmessaging 
				|| systemDesign_.blendpfailhamming
				|| systemDesign_.blendpfailloadbal )  { // EN2.09  // {VB2J [498]}	   If (blendpfailsurviveCheck.Value = 1 Or blendpfailmessagingCheck.Value = 1 Or blendpfailhammingCheck.Value = 1) Then

			if (systemDesign_.blendpfailsurvivefirstpass)  {                                     // {VB2J [499]}	      If (blendpfailsurvivefirstpass) Then

				systemDesign_.blendpfailsurvivefirstpass = false;                                  // {VB2J [500]}	         blendpfailsurvivefirstpass = false

//	   {VB2J [501]}	         'loop over all nodes, and reset eligibility matrix to reflect nodes actually used in first pass
//	   {VB2J [502]}	         'check if this is a successful solution in first pass

int istatesave = 0;
			  if (writebestsolution)  {                 // {VB2J [504]}	         If (annealForm.writebestsolutionCheck.Value = 1) Then
				float emin = Float.MAX_VALUE; // 9E+19;                                                      // {VB2J [505]}	            emin = 9E+19
				for (int istate=0; istate<=systemDesign_.numstatelist; istate++) {               // {VB2J [506]}	            For istate = 0 To numstatelist
				  if (systemDesign_.statelist[istate].e < emin)  {                               // {VB2J [507]}	               If (statelist(istate).e < emin) Then
					emin = systemDesign_.statelist[istate].e;                                    // {VB2J [508]}	                  emin = statelist(istate).e
					istatesave = istate;                                           // {VB2J [509]}	                  istatesave = istate
					}                                                              // {VB2J [510]}	               End If
				  }                                                                // {VB2J [511]}	            Next istate
				} else {                                                           // {VB2J [512]}	         Else
				istatesave = 0;                                                    // {VB2J [513]}	            istatesave = 0
				}                                                                  // {VB2J [514]}	         End If

//	   {VB2J [516]}	         'check kludge for minimum number of active nodes

			  if (systemDesign_.numstatelist > 0)  {                                             // {VB2J [518]}	         If (numstatelist > 0) Then
				for (int inod=1; inod<=systemDesign_.numnode; inod++) {                          // {VB2J [519]}	            For inod = 1 To numnode
				  nodeusedtemp[inod] = false;                                      // {VB2J [520]}	               nodeusedtemp(inod) = false
				  }                                                                // {VB2J [521]}	            Next inod

				for (int ifnx=1; ifnx<=systemDesign_.numfunction; ifnx++) {                      // {VB2J [523]}	            For ifnx = 1 To numfunction
//	   {VB2J [524]}	               'Marc EN 1.1 5/8/03 report best solution mod
//	   {VB2J [525]}	               'nodeusedtemp(agent(ifnx).annealhostid) = true
				  nodeusedtemp[systemDesign_.agent[ifnx].savedhostid[istatesave]] = true;              // {VB2J [526]}	               nodeusedtemp(agent(ifnx).savedhostid(istatesave)) = true
				  }                                                                // {VB2J [527]}	            Next ifnx

				int numnodesusedtemp = 0;                                              // {VB2J [529]}	            numnodesusedtemp = 0

				for (int inod=1; inod<=systemDesign_.numnode; inod++) {                          // {VB2J [531]}	            For inod = 1 To numnode
				  if (nodeusedtemp[inod])  {                                       // {VB2J [532]}	               If (nodeusedtemp(inod)) Then
					numnodesusedtemp++;                       // {VB2J [533]}	                  numnodesusedtemp = numnodesusedtemp + 1
					}                                                              // {VB2J [534]}	               End If
				  }                                                                // {VB2J [535]}	            Next inod

				if (numnodesusedtemp < systemDesign_.minultralognodes)  {                        // {VB2J [537]}	            If (numnodesusedtemp < minultralognodes) Then
				  systemDesign_.numstatelist = 0;                                                // {VB2J [538]}	               numstatelist = 0
				  }                                                                // {VB2J [539]}	            End If
				}                                                                  // {VB2J [541]}	         End If

// {VB2J [544]}	         'if numstatelist = 0, then use the soft constraints code (later mod)
			  if (systemDesign_.numstatelist == 0)  {                                            // {VB2J [545]}	         If (numstatelist = 0) Then

//				  VBJ2_XLATE_FAILURE;                                                // {VB2J [546]}	            MsgBox "failed to find a solution"
//				  Beep();         ;                                                  // {VB2J [547]}	            Beep
//				  Beep();         ;                                                  // {VB2J [548]}	            Beep
//				  Beep();         ;                                                  // {VB2J [549]}	            Beep

				if (systemDesign_.isErrorEnabled())
					SystemDesign.logError("Failed to find a solution!");

				return;                                                            // {VB2J [550]}	            Exit Sub
				}                                                                  // {VB2J [551]}	         End If

			  for (int inod=1; inod<=systemDesign_.numnode; inod++) {                            // {VB2J [553]}	         For inod = 1 To numnode
				if ( ! nodeusedtemp[inod]  && systemDesign_.nodeinfo[inod].type == 1)  {    // EN2.09                               // {VB2J [554]}	            If (Not nodeusedtemp(inod)) Then

		              // EN2.09 - 'en 2.01 bug fix
				    systemDesign_.nodeinfo[inod].enabled = false; // 'a bit redundant, but needed. Cleanup should eliminate the change in function eligibility

				    for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                       // {VB2J [555]}	               For ifn = 1 To numfunction
					systemDesign_.xnfunction[ifn].felig[inod] = 0;                               // {VB2J [556]}	                  xnfunction(ifn).felig(inod) = 0
					}                                                              // {VB2J [557]}	               Next ifn
				  }                                                                // {VB2J [558]}	            End If
				}                                                                  // {VB2J [559]}	         Next inod
			  }                                                                    // {VB2J [561]}	      End If
			}                                                                      // {VB2J [562]}	   End If


		   // EN2.09
			if (systemDesign_.blendloadbalmessaging) {
				if (systemDesign_.blendloadbalmessagingfirstpass) {
				    systemDesign_.blendloadbalmessagingfirstpass = false;
					// 'find min and max values of loadbal from first pass
				    systemDesign_.loadbalminfirstpass = systemDesign_.loadbalsamplemin;
				    systemDesign_.loadbalmaxfirstpass = systemDesign_.loadbalsamplemax;
					}
				}
		      
		   
		   
// {VB2J [567]}	   'second increment

	if (systemDesign_.blendpfailsurvive)  {                                // {VB2J [568]}	   If (blendpfailsurviveCheck.Value = 1) Then

		if (systemDesign_.isInfoEnabled())
			SystemDesign.logInfo("Doing 'second increment': blend pfail/survive");

	      	// EN2.09 - 'Marc en 2.01 load balancing soft constraints
			metroParams_.loadbalCheck = false;
	      
			metroParams_.pfailureCheck = false;                                    // {VB2J [569]}	      metrparmex.pfailureCheck.Value = 0
			metroParams_.pfailuresurviveCheck = true;                             // {VB2J [570]}	      metrparmex.pfailuresurviveCheck.Value = 1
			metroParams_.remotetrafficCheck = false;                               // {VB2J [571]}	      metrparmex.remotetrafficCheck.Value = 0
//			  DoEvents();         ;                                                  // {VB2J [572]}	      DoEvents
			initializeArchive();                      // {VB2J [573]}	      annealForm.initializearchiveCommand.Value = true
			numruns++;                                                 // {VB2J [575]}	      numruns = numruns + 1

//			  DoEvents();         ;                                                  // {VB2J [576]}	      DoEvents
			doInitialLaydown();                                                    // {VB2J [577]}	      doInitialLaydown (false)
//			  runcountLabel.Caption = numruns;                                       // {VB2J [578]}	      runcountLabel.Caption = numruns
//			  DoEvents();         ;                                                  // {VB2J [579]}	      DoEvents
			}                                                                      // {VB2J [580]}	   End If


// {VB2J [583]}	   'third increment

		  if (systemDesign_.blendpfailmessaging)  {                              // {VB2J [584]}	   If (blendpfailmessagingCheck.Value = 1) Then

			if (systemDesign_.isInfoEnabled())
				SystemDesign.logInfo("Doing 'third increment': blend pfail/messaging");

			metroParams_.pfailureCheck = false;                                    // {VB2J [585]}	      metrparmex.pfailureCheck.Value = 0
			metroParams_.pfailuresurviveCheck = false;                             // {VB2J [586]}	      metrparmex.pfailuresurviveCheck.Value = 0
			metroParams_.remotetrafficCheck = true;                               // {VB2J [587]}	      metrparmex.remotetrafficCheck.Value = 1
//			  DoEvents();         ;                                                  // {VB2J [588]}	      DoEvents

			initializeArchive();                      // {VB2J [589]}	      annealForm.initializearchiveCommand.Value = true

			numruns++;                                                 // {VB2J [591]}	      numruns = numruns + 1
//			  DoEvents();         ;                                                  // {VB2J [592]}	      DoEvents
			doInitialLaydown();                                                    // {VB2J [593]}	      doInitialLaydown (false)
//			  runcountLabel.Caption = numruns;                                       // {VB2J [594]}	      runcountLabel.Caption = numruns
//			  DoEvents();         ;                                                  // {VB2J [595]}	      DoEvents
			}                                                                      // {VB2J [596]}	   End If


// {VB2J [599]}	   'fourth increment
		  if (systemDesign_.blendpfailhamming)  {                                // {VB2J [600]}	   If (blendpfailhammingCheck.Value = 1) Then

			if (systemDesign_.isInfoEnabled())
				SystemDesign.logInfo("Doing 'fourth increment': blend pfail/hamming");

// rec - aug'03 - seems to be necessary to do this; 
// first pass clears this flag, and nothing else ever re-sets it ???
//
if (systemDesign_.isWarnEnabled())
	SystemDesign.logWarn("(OK) re-setting hammingCheck flag!");
metroParams_.hammingCheck = true;
			

			metroParams_.pfailureCheck = false;                                    // {VB2J [601]}	      metrparmex.pfailureCheck.Value = 0
			metroParams_.pfailuresurviveCheck = false;                             // {VB2J [602]}	      metrparmex.pfailuresurviveCheck.Value = 0
			metroParams_.remotetrafficCheck = true;                               // {VB2J [603]}	      metrparmex.remotetrafficCheck.Value = 1
//			  DoEvents();         ;                                                  // {VB2J [604]}	      DoEvents

		    // EN2.09 - 'Marc en 2.01 load balancing soft constraints
			metroParams_.loadbalCheck = false;

			initializeArchive();                      		// {VB2J [605]}	      annealForm.initializearchiveCommand.Value = true
			numruns++;                                                 // {VB2J [607]}	      numruns = numruns + 1
//			  DoEvents();         ;                                                  // {VB2J [608]}	      DoEvents
			doInitialLaydown ();                                              // {VB2J [609]}	      doInitialLaydown (false)
//			  runcountLabel.Caption = numruns;                                       // {VB2J [610]}	      runcountLabel.Caption = numruns
//			  DoEvents();         ;                                                  // {VB2J [611]}	      DoEvents
			}                                                                      // {VB2J [612]}	   End If

		  // EN2.09
		   // 'Marc en 2.01 load balancing soft constraints
		   // 'CPU/Memory Load Balancing increment
		   if (systemDesign_.blendpfailloadbal) {
				metroParams_.pfailureCheck = false;
				metroParams_.pfailuresurviveCheck = false;
				metroParams_.remotetrafficCheck = false;
				metroParams_.loadbalCheck = true;
				initializeArchive();
				numruns++;
				doInitialLaydown();
		   		}
		   // 'marc en 2.02 blend load balancing and messaging
		   if (systemDesign_.blendloadbalmessaging) {

		       metroParams_.pfailureCheck = false;
		       metroParams_.pfailuresurviveCheck = false;
		       metroParams_.remotetrafficCheck = true;
		       metroParams_.loadbalCheck = false;

		       initializeArchive();

		       numruns++;
		       doInitialLaydown();
		   }
		    
		  
//		 EN2.09: unused: 		  return;                                                                  // {VB2J [615]}	Exit Sub

		  
// rec - NOT REACHABLE as per original code
//
// {VB2J [617]}	   'fifth increment - repeat of second increment
//
//		  if (systemDesign_.blendpfailsurvive)  {                                // {VB2J [618]}	   If (blendpfailsurviveCheck.Value = 1) Then
//			metroParams_.pfailureCheck = false;                                    // {VB2J [619]}	      metrparmex.pfailureCheck.Value = 0
//			metroParams_.pfailuresurviveCheck = true;                             // {VB2J [620]}	      metrparmex.pfailuresurviveCheck.Value = 1
//			metroParams_.remotetrafficCheck = false;                               // {VB2J [621]}	      metrparmex.remotetrafficCheck.Value = 0
////			  DoEvents();         ;                                                  // {VB2J [622]}	      DoEvents
//
//			initializeArchive();                      // {VB2J [623]}	      annealForm.initializearchiveCommand.Value = true
//			numruns++;                                                 // {VB2J [625]}	      numruns = numruns + 1
////			  DoEvents();         ;                                                  // {VB2J [626]}	      DoEvents
//			doInitialLaydown();                                                    // {VB2J [627]}	      doInitialLaydown (false)
////			  runcountLabel.Caption = numruns;                                       // {VB2J [628]}	      runcountLabel.Caption = numruns
////			  DoEvents();         ;                                                  // {VB2J [629]}	      DoEvents
//			}                                                                      // {VB2J [630]}	   End If


}     //	   intialLaydown                                                                   // {VB2J [633]}	End Sub


/**
 * 'Private Sub initializearchiveCommand_Click() marc en 1.1 smoothing
 * @version 1.1
 */
private void
initializeArchive() {

   systemDesign_.state.numfstatecallsave = 0;
   systemDesign_.numstatelist = 0;
	for (int ilink = 1; ilink<=systemDesign_.numlink; ilink++) {
		systemDesign_.plink[ilink] = 0;
		systemDesign_.linkgenepoolstep[ilink] = false;
	   }
	}


/**
 * 
 * @version 1.1
 */
private void
annealingStep() { 

	float pfailuresurvivex; // rec - unused:  pfailuresurvivetmpmax;
	
//	{VB2J [6]}	// new 1.1 code

// rec - unused:   int iqdata;                                                                  // {VB2J [9]}	   Dim iqdata As Integer
// rec - unused:   int iqmode;                                                                  // {VB2J [10]}	   Dim iqmode As Integer
// rec - loop var:  int inod;                                                                    // {VB2J [11]}	   Dim inod As Integer
// rec - loop var:   int ifn;
// rec - loop var:	int ifncall;                                                         // {VB2J [13]}	   Dim ifn As Integer, ifncall As Integer

// rec - GUI:   String recfname;String recnodname;                                           // {VB2J [15]}	   Dim recfname As String, recnodname As String
// rec - GUI:   int reclen;                                                                  // {VB2J [16]}	   Dim reclen As Integer

// rec - unused:   int nodeeventtemp[] = new int[SystemDesign.numnodemax+1];                                 // {VB2J [18]}	   Dim nodeeventtemp(numnodemax) As Integer

   float objx = 0;                                                                 // {VB2J [20]}	   Dim objx As Single


   systemDesign_.fixedcostsamplemin = Float.MAX_VALUE; // should use Integer.MAX ??? was 9000000000#;                                            // {VB2J [24]}	   fixedcostsamplemin = 9000000000#
   systemDesign_.fixedcostsamplemax = -Float.MAX_VALUE; // -9000000000#;                                           // {VB2J [25]}	   fixedcostsamplemax = -9000000000#

   systemDesign_.monthlycostsamplemin = Float.MAX_VALUE; // 9000000000#;                                          // {VB2J [27]}	   monthlycostsamplemin = 9000000000#
   systemDesign_.monthlycostsamplemax = -Float.MAX_VALUE; // -9000000000#;                                         // {VB2J [28]}	   monthlycostsamplemax = -9000000000#

   systemDesign_.pfailuresamplemin = Float.MAX_VALUE; // 9000000000#;                                             // {VB2J [30]}	   pfailuresamplemin = 9000000000#
   systemDesign_.pfailuresamplemax = -Float.MAX_VALUE; // -9000000000#;                                            // {VB2J [31]}	   pfailuresamplemax = -9000000000#

   systemDesign_.responsetimesamplemin = Float.MAX_VALUE; // 9000000000#;                                         // {VB2J [33]}	   responsetimesamplemin = 9000000000#
   systemDesign_.responsetimesamplemax = -Float.MAX_VALUE; // -9000000000#;                                        // {VB2J [34]}	   responsetimesamplemax = -9000000000#

// {VB2J [37]}	   'messaging mods
	systemDesign_.remotetrafficsamplemin = Float.MAX_VALUE; // 9E+19;                                              // {VB2J [38]}	   remotetrafficsamplemin = 9E+19
	systemDesign_.remotetrafficsamplemax = -Float.MAX_VALUE; // -9E+19;                                             // {VB2J [39]}	   remotetrafficsamplemax = -9E+19

// {VB2J [41]}	   'marc EN 1.1 smoothing
	systemDesign_.pfailuresurvivesamplemin = Float.MAX_VALUE; // 9E+19;                                            // {VB2J [42]}	   pfailuresurvivesamplemin = 9E+19
	systemDesign_.pfailuresurvivesamplemax = -Float.MAX_VALUE; // -9E+19;                                           // {VB2J [43]}	   pfailuresurvivesamplemax = -9E+19

// {VB2J [45]}	   'marc EN 1.1 hamming
	systemDesign_.hammingsamplemin = Float.MAX_VALUE; // 9E+19;                                                    // {VB2J [46]}	   hammingsamplemin = 9E+19
	systemDesign_.hammingsamplemax = -Float.MAX_VALUE; // -9E+19;                                                   // {VB2J [47]}	   hammingsamplemax = -9E+19

//    'en 2.0 load balancing
	systemDesign_.loadbalsamplemin =  Float.MAX_VALUE; //  9E+19
	systemDesign_.loadbalsamplemax = -Float.MAX_VALUE; // -9E+19


	// {VB2J [52]}	'linkid(inod,jnod) = ilink, where ilink is the physical link number between inod and jnod
	// {VB2J [53]}	'This assumes NO MORE THAN ONE LINK between physical nodes.
	// {VB2J [54]}	'If link(inod,jnod)=0, then there is no physical link between nodes
	// {VB2J [55]}	'Global linkid(numnodemax, numnodemax) As Integer

																				// {VB2J [57]}	   'linkid is used in the routing code, to associate a physical link with nodenear,nodefar during the dykstra search																	// {VB2J [58]}	   'This is only needed for routing with cost and failure contributions to the routing search
   for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                    // {VB2J [59]}	   For inod = 1 To numnode
	 for (int jnod=1; jnod<=systemDesign_.numnode; jnod++) {                                  // {VB2J [60]}	   For jnod = 1 To numnode
	   systemDesign_.linkid[inod][jnod] = 0;                                                  // {VB2J [61]}	      linkid(inod, jnod) = 0
	   }                                                                        // {VB2J [62]}	   Next jnod
	 }                                                                          // {VB2J [63]}	   Next inod


   for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                                 // {VB2J [66]}	   For ilink = 1 To numlink
	 systemDesign_.linkid[systemDesign_.linkinfo[ilink].node1][systemDesign_.linkinfo[ilink].node2] = ilink;              // {VB2J [67]}	      linkid(linkinfo(ilink).node1, linkinfo(ilink).node2) = ilink
																				// {VB2J [68]}	      'correction for LAN links:
	 if (systemDesign_.linkinfo[ilink].type == 1)  {                                          // {VB2J [69]}	      If (linkinfo(ilink).type = 1) Then
	   systemDesign_.linkid[systemDesign_.linkinfo[ilink].node2][systemDesign_.linkinfo[ilink].node1] = ilink;              // {VB2J [70]}	         linkid(linkinfo(ilink).node2, linkinfo(ilink).node1) = ilink
	   }                                                                        // {VB2J [71]}	      End If
	 }                                                                          // {VB2J [72]}	   Next ilink


   if (iterate == true)  {                                              // {VB2J [76]}	   If (iterateCheck.Value = 1) Then
	systemDesign_.iteranel = true;                                                           // {VB2J [77]}	      iteranel = True
	 }                                                                          // {VB2J [78]}	   End If

   if (systemDesign_.darparun)  {                                    // {VB2J [81]}	   If (darparun) Then
	 systemDesign_.objfnjobset.numobjfn = 0;                         // {VB2J [82]}	      objfnjobset.numobjfn = 0
	 for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {        // {VB2J [84]}	      For ifn = 1 To numfunction
	   if (systemDesign_.xnfunction[ifn].objfn)  {                   // {VB2J [85]}	         If (xnfunction(ifn).objfn) Then
		 systemDesign_.objfnjobset.numobjfn++;                       // {VB2J [86]}	            objfnjobset.numobjfn = objfnjobset.numobjfn + 1
		 systemDesign_.objfnjobset.initfn[systemDesign_.objfnjobset.numobjfn] = ifn;          // {VB2J [87]}	            objfnjobset.initfn(objfnjobset.numobjfn) = ifn
		 }                                                          // {VB2J [88]}	         End If
	   }                                                            // {VB2J [89]}	      Next ifn
	 }                                                              // {VB2J [90]}	   End If

   
   // 'en 2.05 cleanup: delete this entropy section
//
//	systemDesign_.entropy.numentropysamples = 0;                    // {VB2J [93]}	   entropy.numentropysamples = 0
//	systemDesign_.entropy.ientropyobjsamplelast = 0;                // {VB2J [94]}	   entropy.ientropyobjsamplelast = 0
//	systemDesign_.entropy.entropydataready = false;                 // {VB2J [95]}	   entropy.entropydataready = False

// rec - unused:	int ientropy = 0;                                               // {VB2J [96]}	   ientropy = 0

																	// {VB2J [99]}	   'marc EN 1.1 smoothing
   if (systemDesign_.blendpfailsurvive)  {                          // {VB2J [100]}	   If (blendpfailsurvive) Then
  
	// {VB2J [101]}	      'store original eligibility matrix for second pass
	// {VB2J [102]}	      'initialize blendpfailsurvivefirstpass = True in routine systemdesignx(Main)
	// {VB2J [103]}	      'ROB: make sure you look at routine systemdesignx(Main)
	// {VB2J [104]}	      'set blendpfailsurvivefirstpass = false in annealform(writeagentsCommand_Click)
  
	 if (systemDesign_.blendpfailsurvivefirstpass)  {                                         // {VB2J [105]}	      If (blendpfailsurvivefirstpass) Then
	   for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                               // {VB2J [106]}	         For ifn = 1 To numfunction
		 for (int inod=1; inod<=systemDesign_.numnode; inod++) {                              // {VB2J [107]}	            For inod = 1 To numnode
			systemDesign_.xnfunctionfirstpass[ifn].felig[inod] = systemDesign_.xnfunction[ifn].felig[inod];              // {VB2J [108]}	               xnfunctionfirstpass(ifn).felig(inod) = xnfunction(ifn).felig(inod)
		   }                                                                    // {VB2J [109]}	            Next inod
		 }                                                                      // {VB2J [110]}	         Next ifn
	   }                                                                        // {VB2J [111]}	      End If
	 }                                                                          // {VB2J [112]}	   End If



// rec - re-worked goto
//
startanneal:                                                        	  // {VB2J [117]}	startanneal:

   while (true) {

// {VB2J [119]}	   'for each function ifn, load the list of nodes for which that function is eligible:
// {VB2J [120]}	   'note that the nodal availability can change during a run, if user updates nodal properties

																		  // {VB2J [122]}	   'real time node failure fix: execute this section of code after any time that nodal availability changes:

   for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {               // {VB2J [124]}	   For ifn = 1 To numfunction
	 int j = 0;                                                           // {VB2J [125]}	      j = 0
	 for (int inod=1; inod<=systemDesign_.numnode; inod++) {              // {VB2J [126]}	      For inod = 1 To numnode
	   if (systemDesign_.nodeinfo[inod].enabled)  {                       // {VB2J [127]}	         If (nodeinfo(inod).enabled) Then 'marc en 1.1 soft constraints
		 if (systemDesign_.xnfunction[ifn].felig[inod] == 1)  {           // {VB2J [128]}	            If (xnfunction(ifn).felig(inod) = 1) Then
		   j++;                                                           // {VB2J [129]}	               j = j + 1
		   systemDesign_.xnfunction[ifn].nodelist[j] = inod;              // {VB2J [130]}	               xnfunction(ifn).nodelist(j) = inod
		   }                                                              // {VB2J [131]}	            End If
		 }                                                                // {VB2J [132]}	         End If
	   }                                                                  // {VB2J [133]}	      Next inod

	// {VB2J [135]}	      'we store the number of elements in the eligibility list xnfunction(ifn).nodelist(*) in the
	// {VB2J [136]}	      'array element xnfunction(ifn).nodelist(0)

	 systemDesign_.xnfunction[ifn].nodelist[0] = j;                                           // {VB2J [138]}	      xnfunction(ifn).nodelist(0) = j
	 }                                                                          // {VB2J [139]}	   Next ifn

   if (systemDesign_.darparun 
   && (initiallaydownflag || randomAgentSeed))  {  // {VB2J [144]}	   If (darparun And (initiallaydownflag Or (RandomAgentSeedCheck.Value = 1))) Then 'marc en 1.1 smoothing

	 for (int ifnx=1; ifnx<=systemDesign_.objfnjobset.numobjfn; ifnx++) {     // {VB2J [145]}	      For ifnx = 1 To objfnjobset.numobjfn
	   int ifn = systemDesign_.objfnjobset.initfn[ifnx];                      // {VB2J [146]}	         ifn = objfnjobset.initfn(ifnx)
	   int inodrunx = RnR.Int(systemDesign_.xnfunction[ifn].nodelist[0] * RnR.Rnd() + 1);                 // {VB2J [147]}	         inodrunx = Int((xnfunction(ifn).nodelist(0)) * Rnd + 1)
																				// {VB2J [148]}	         'agent(ifn).testhostid = inodrunx
		systemDesign_.agent[ifn].testhostid = systemDesign_.xnfunction[ifn].nodelist[inodrunx];              // {VB2J [149]}	         agent(ifn).testhostid = xnfunction(ifn).nodelist(inodrunx)
	   }                                                                        // {VB2J [150]}	      Next ifnx

	 initiallaydownflag = false;            // {VB2J [152]}	      initiallaydownflag = False
	 randomAgentSeed = false;               // {VB2J [153]}	      RandomAgentSeedCheck.Value = 0  'marc en 1.1 smoothing
	 }                                      // {VB2J [154]}	   End If


   if (lockstate)  {                                            // {VB2J [161]}	   If (lockstateCheck.Value = 1) Then

       ;	// make PMD happy!										// {VB2J [162]}	      'readstateCommand.Value = True

// 'en 2.03 cleanup: delete this entire elseif (subthreadopt) block

       //	 } else if (systemDesign_.subthreadopt)  {                                                // {VB2J [163]}	   ElseIf (subthreadopt) Then
//
//	     //																				// {VB2J [164]}	      'en 1.x future. Rob, Ignore this section
////	 iter++;                                                           // {VB2J [165]}	      iter = iter + 1
////
////	 if (iter + 1000 * iterk > systemDesign_.maxannealingincrements)  {                      // {VB2J [167]}	      If (iter + 1000# * iterk > maxannealingincrements) Then
////	   iterate = false;                                                  // {VB2J [168]}	         iterateCheck.Value = 0
////	   systemDesign_.iteranel = false;                                                        // {VB2J [169]}	         iteranel = False
////	   }                                                                        // {VB2J [170]}	      End If
////
////	 if (iter >= 1000)  {                                                       // {VB2J [172]}	      If (iter >= 1000) Then
////	   iter = 0;                                                                // {VB2J [173]}	         iter = 0
////	   iterk++;                                                       // {VB2J [174]}	         iterk = iterk + 1
////// rec - GUI	   incrementLabel.Caption = iterk;                                          // {VB2J [175]}	         incrementLabel.Caption = iterk
////  //     DoEvents();                                                              // {VB2J [176]}	         DoEvents
////	   }                                                                        // {VB2J [177]}	      End If
////
////	 getRandomThread();                                                         // {VB2J [179]}	      getrandomthread
//

   } else {                                                                   // {VB2J [180]}	   Else
	 iter++;                                                           // {VB2J [181]}	      iter = iter + 1

	 if (iter + 1000 * iterk > systemDesign_.maxannealingincrements)  {                      // {VB2J [183]}	      If (iter + 1000# * iterk > maxannealingincrements) Then
	   iterate = false;                                                  // {VB2J [184]}	         iterateCheck.Value = 0
	   systemDesign_.iteranel = false;                                                        // {VB2J [185]}	         iteranel = False
	   }                                                                        // {VB2J [186]}	      End If

	 if (iter >= 1000)  {                                                       // {VB2J [188]}	      If (iter >= 1000) Then
	   iter = 0;                                                                // {VB2J [189]}	         iter = 0
	   iterk++;                                                       // {VB2J [190]}	         iterk = iterk + 1
// rec - GUI	   incrementLabel.Caption = iterk;                                          // {VB2J [191]}	         incrementLabel.Caption = iterk
  //     DoEvents();                                                              // {VB2J [192]}	         DoEvents
	   }                                                                        // {VB2J [193]}	      End If

	 if ( ! systemDesign_.state.status || systemDesign_.darparun)  {                                         // {VB2J [195]}	      If (Not State.status Or darparun) Then
																				// {VB2J [196]}	         'pick a random state
	   getRandomState();                                                        // {VB2J [197]}	         getrandomstate
	   }                                                                        // {VB2J [198]}	      End If
	 }                                                                          // {VB2J [199]}	   End If 'end of If (lockstateCheck.Value <> 1) Then

	// {VB2J [208]}	''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [209]}	''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [210]}	''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [211]}	''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [212]}	''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [213]}	''''''''''''''''''''''''''''''''''''''''''''''''''''''

   evaluateobjective:                                                           // {VB2J [217]}	evaluateobjective:

// rec - removed GUI code
//
//   if (iterate == true)  {                                              // {VB2J [220]}	   If (iterateCheck.Value = 1) Then
//	 pausetime = Val[screenrefreshText.Text];                                   // {VB2J [221]}	      pausetime = Val(screenrefreshText.Text)   ' Set duration.
//	 if (Timer > starttime + pausetime)  {                                      // {VB2J [222]}	      If (Timer > starttime + pausetime) Then
//	   starttime = Timer;                                                       // {VB2J [223]}	         starttime = Timer
//	   tracefunctionForm.drawmsgpathoptCheck.Value = drawmsgpathoptChecksave;   // {VB2J [224]}	         tracefunctionForm.drawmsgpathoptCheck.Value = drawmsgpathoptChecksave
//	   tracefunctionForm.drawmsgpathsearchCheck.Value = drawmsgpathsearchChecksave; // {VB2J [225]}	         tracefunctionForm.drawmsgpathsearchCheck.Value = drawmsgpathsearchChecksave
//	   tracefunctionForm.drawmsgpathannealCheck.Value = drawmsgpathannealchecksave; // {VB2J [226]}	         tracefunctionForm.drawmsgpathannealCheck.Value = drawmsgpathannealchecksave
//	   drawincrement = true;                                                    // {VB2J [227]}	         drawincrement = True
//	   } else {                                                                 // {VB2J [228]}	      Else
//	   tracefunctionForm.drawmsgpathoptCheck.Value = 0;                         // {VB2J [229]}	         tracefunctionForm.drawmsgpathoptCheck.Value = 0
//	   tracefunctionForm.drawmsgpathsearchCheck.Value = 0;                      // {VB2J [230]}	         tracefunctionForm.drawmsgpathsearchCheck.Value = 0
//	   tracefunctionForm.drawmsgpathannealCheck.Value = 0;                      // {VB2J [231]}	         tracefunctionForm.drawmsgpathannealCheck.Value = 0
//	   drawincrement = false;                                                   // {VB2J [232]}	         drawincrement = False  'new 7/23/01
//	   }                                                                        // {VB2J [233]}	      End If
//	 }                                                                          // {VB2J [234]}	   End If
//
   // DoEvents();                                                                  // {VB2J [236]}	   DoEvents

   if ( ! systemDesign_.darparun)  {                                                          // {VB2J [238]}	   If (Not darparun) Then
	 ; // hey PMD!																			// {VB2J [239]}	      'tracefunctionForm.definedstateCommand.Value = True
	 } else {                                                                   // {VB2J [240]}	   Else
		traceFunctionForm_.agentRun();                            // {VB2J [241]}	      tracefunctionForm.agentrunCommand.Value = True
	 }                                                                          // {VB2J [242]}	   End If

   // DoEvents();                                                                  // {VB2J [244]}	   DoEvents

	// {VB2J [247]}	   'find required state function-node assignments:
	// {VB2J [249]}	   'state.fstatecall(ifnrun, inodcall)

   if ( ! systemDesign_.darparun || systemDesign_.state.status)  {                                           // {VB2J [256]}	   If (Not darparun Or State.status) Then

																				// {VB2J [258]}	      'determine the required nodes to support this set of functions and state:
	 int inodcount = 0;                                                             // {VB2J [259]}	      inodcount = 0

	 for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [261]}	      For inod = 1 To numnode
	   for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                               // {VB2J [262]}	         For ifn = 1 To numfunction
		 if (systemDesign_.nodeinfo[inod].fcalltot[ifn] > 0)  {                               // {VB2J [263]}	            If (nodeinfo(inod).fcalltot(ifn) > 0) Then
		   inodcount++;                                           // {VB2J [264]}	               inodcount = inodcount + 1
		   systemDesign_.requirednodes[inodcount] = inod;                                     // {VB2J [265]}	               requirednodes(inodcount) = inod
		   break;                                                               // {VB2J [266]}	               Exit For 'ANY function burn means this node is used
		   }                                                                    // {VB2J [267]}	            End If
		 }                                                                      // {VB2J [268]}	         Next ifn
	   }                                                                        // {VB2J [269]}	      Next inod

	systemDesign_.requirednodes[0] = inodcount;                                              // {VB2J [271]}	      requirednodes(0) = inodcount

// rec - GUI
//	 if (drawincrement || (iterate == false))  {                          // {VB2J [273]}	      If (drawincrement Or (iterateCheck.Value = 0)) Then
//	   requirednodeCombo.Clear();                                               // {VB2J [274]}	         requirednodeCombo.Clear
//	   for (int ireqnod=1; ireqnod<=requirednodes(0); ireqnod++) {              // {VB2J [275]}	         For ireqnod = 1 To requirednodes(0)
//		 inod = requirednodes[ireqnod];                                         // {VB2J [276]}	            inod = requirednodes(ireqnod)
////		 VBJ2_XLATE_FAILURE;                                                    // {VB2J [277]}	            requirednodeCombo.AddItem nodeinfo(inod).name
//		 }                                                                      // {VB2J [278]}	         Next ireqnod
//	   requirednodeCombo.ListIndex = 0;                                         // {VB2J [279]}	         requirednodeCombo.ListIndex = 0
//	   // DoEvents();                                                              // {VB2J [280]}	         DoEvents
//	   }                                                                        // {VB2J [281]}	      End If

	 int inodroutercount = 0;                                                       // {VB2J [283]}	      inodroutercount = 0
	 int inodlancount = 0;                                                          // {VB2J [284]}	      inodlancount = 0

	 for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [286]}	      For inod = 1 To numnode
	   if ((systemDesign_.nodeinfo[inod].type == Node.TYPE_ROUTER) 
	    && (systemDesign_.nodeinfo[inod].packetrate > 0))  {     // {VB2J [287]}	         If ((nodeinfo(inod).type = 2) And (nodeinfo(inod).packetrate > 0)) Then 'router
		 inodroutercount++;                                 // {VB2J [288]}	            inodroutercount = inodroutercount + 1
		 systemDesign_.requiredrouters[inodroutercount] = inod;                               // {VB2J [289]}	            requiredrouters(inodroutercount) = inod
		 }                                                                      // {VB2J [290]}	         End If

	   if ((systemDesign_.nodeinfo[inod].type == Node.TYPE_LAN) 
	    && (systemDesign_.nodeinfo[inod].packetrate > 0))  {     // {VB2J [292]}	         If ((nodeinfo(inod).type = 3) And (nodeinfo(inod).packetrate > 0)) Then 'LAN
		 inodlancount++;                                       // {VB2J [293]}	            inodlancount = inodlancount + 1
		 systemDesign_.requiredlans[inodlancount] = inod;                                     // {VB2J [294]}	            requiredlans(inodlancount) = inod
		 }                                                                      // {VB2J [295]}	         End If
	   }                                                                        // {VB2J [296]}	      Next inod

	systemDesign_.requiredrouters[0] = inodroutercount;                                      // {VB2J [298]}	      requiredrouters(0) = inodroutercount
	systemDesign_.requiredlans[0] = inodlancount;                                            // {VB2J [299]}	      requiredlans(0) = inodlancount

	// {VB2J [302]}	   ''
	// {VB2J [303]}	   ''
	// {VB2J [304]}	   ' print cost
	// {VB2J [305]}	   '
	// {VB2J [306]}	   ' get total fixed cost

	systemDesign_.fixedcost = 0;                                                             // {VB2J [310]}	      fixedcost = 0

	 if ( ! systemDesign_.darparun)  {                                                        // {VB2J [312]}	      If (Not darparun) Then
	   for (int ireqnod=1; ireqnod<=systemDesign_.requirednodes[0]; ireqnod++) {              // {VB2J [313]}	         For ireqnod = 1 To requirednodes(0)
		 int inod = systemDesign_.requirednodes[ireqnod];                                         // {VB2J [314]}	            inod = requirednodes(ireqnod)
																				// {VB2J [315]}	            'find current fixed hardware costs:
		 float nodcostx = systemDesign_.getnodefixedhardwarecost(inod);                             // {VB2J [316]}	            nodcostx = getnodefixedhardwarecost(inod)
		 nodcostx += systemDesign_.getnodefixedsoftwarecost(inod);                  // {VB2J [317]}	            nodcostx = nodcostx + getnodefixedsoftwarecost(inod)
		 systemDesign_.fixedcost += nodcostx;                                      // {VB2J [318]}	            fixedcost = fixedcost + nodcostx
		 }                                                                      // {VB2J [319]}	         Next ireqnod


																				// {VB2J [322]}	         'router costs:
	   for (int ireqnod=1; ireqnod<=systemDesign_.requiredrouters[0]; ireqnod++) {            // {VB2J [323]}	         For ireqnod = 1 To requiredrouters(0)
		 int inod = systemDesign_.requiredrouters[ireqnod];                                       // {VB2J [324]}	            inod = requiredrouters(ireqnod)
																				// {VB2J [325]}	            'find current fixed hardware costs:
																				// {VB2J [326]}	            'don't need to add getnodefixedsoftwarecost for lans and routers
		 float nodcostx = systemDesign_.getnodefixedhardwarecost(inod);                             // {VB2J [327]}	            nodcostx = getnodefixedhardwarecost(inod)
		 systemDesign_.fixedcost += nodcostx;                                      // {VB2J [328]}	            fixedcost = fixedcost + nodcostx
		 }                                                                      // {VB2J [329]}	         Next ireqnod

																				// {VB2J [331]}	         'LAN costs:
	   for (int ireqnod=1; ireqnod<=systemDesign_.requiredlans[0]; ireqnod++) {               // {VB2J [332]}	         For ireqnod = 1 To requiredlans(0)
		 int inod = systemDesign_.requiredlans[ireqnod];                                          // {VB2J [333]}	            inod = requiredlans(ireqnod)
																				// {VB2J [334]}	            'find current fixed hardware costs:
																				// {VB2J [335]}	            'don't need to add getnodefixedsoftwarecost for lans and routers
		 float nodcostx = systemDesign_.getnodefixedhardwarecost(inod);                             // {VB2J [336]}	            nodcostx = getnodefixedhardwarecost(inod)
		 systemDesign_.fixedcost += nodcostx;                                      // {VB2J [337]}	            fixedcost = fixedcost + nodcostx
		 }                                                                      // {VB2J [338]}	         Next ireqnod
	   }                                                                        // {VB2J [339]}	      End If 'end of if (not darparun)


	// {VB2J [341]}	      'fixedcost = initial costs: NODES: LANS, Routers, Servers
	// {VB2J [345]}	   '''

	systemDesign_.monthlycost = 0;                                                           // {VB2J [348]}	      monthlycost = 0

	 if ( ! systemDesign_.darparun)  {                                                        // {VB2J [350]}	      If (Not darparun) Then
	   for (int ireqnod=1; ireqnod<=systemDesign_.requirednodes[0]; ireqnod++) {              // {VB2J [351]}	         For ireqnod = 1 To requirednodes(0)
		 int inod = systemDesign_.requirednodes[ireqnod];                                         // {VB2J [352]}	            inod = requirednodes(ireqnod)
																				// {VB2J [353]}	            'find current fixed hardware costs:
		 float nodcostx = systemDesign_.getnodemonthlyhardwarecost(inod);                           // {VB2J [354]}	            nodcostx = getnodemonthlyhardwarecost(inod)
		 nodcostx += systemDesign_.getnodemonthlysoftwarecost(inod);                // {VB2J [355]}	            nodcostx = nodcostx + getnodemonthlysoftwarecost(inod)
		 systemDesign_.monthlycost += nodcostx;                                  // {VB2J [356]}	            monthlycost = monthlycost + nodcostx
		 }                                                                      // {VB2J [357]}	         Next ireqnod
	   }                                                                        // {VB2J [358]}	      End If
	 }                                                                          // {VB2J [359]}	   End If 'end if not darparun or state.status


   if (systemDesign_.state.status)  {                                                         // {VB2J [371]}	   If (State.status) Then
																				// {VB2J [372]}	      'update costs for used links:

	 if ( ! systemDesign_.darparun)  {                                                        // {VB2J [374]}	      If (Not darparun) Then
	   for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                             // {VB2J [375]}	         For ilink = 1 To numlink
		 if (systemDesign_.linkinfo[ilink].linkused)  {                                       // {VB2J [376]}	            If (linkinfo(ilink).linkused) Then
			systemDesign_.fixedcost += systemDesign_.linkinfo[ilink].costbase;                    // {VB2J [377]}	               fixedcost = fixedcost + linkinfo(ilink).costbase
			systemDesign_.monthlycost += systemDesign_.linkinfo[ilink].costpermonth;              // {VB2J [378]}	               monthlycost = monthlycost + linkinfo(ilink).costpermonth
		   }                                                                    // {VB2J [379]}	            End If
		 }                                                                      // {VB2J [380]}	         Next ilink
	   }                                                                        // {VB2J [381]}	      End If

	systemDesign_.monthlycostave = 0.99f * systemDesign_.monthlycostave + 0.01f * systemDesign_.monthlycost;               // {VB2J [383]}	      monthlycostave = 0.99 * monthlycostave + 0.01 * monthlycost
	systemDesign_.fixedcostave = 0.99f * systemDesign_.fixedcostave + 0.01f * systemDesign_.fixedcost;                     // {VB2J [384]}	      fixedcostave = 0.99 * fixedcostave + 0.01 * fixedcost
	systemDesign_.responsetimeave = 0.99f * systemDesign_.responsetimeave + 0.01f * systemDesign_.ftracetime;              // {VB2J [385]}	      responsetimeave = 0.99 * responsetimeave + 0.01 * ftracetime

																				// {VB2J [387]}	      'determine state probability:
	 float psuccess = 1;                                                              // {VB2J [388]}	      psuccess = 1
	 for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [389]}	      For inod = 1 To numnode
	   if (systemDesign_.nodeinfo[inod].nodeused)  {                                          // {VB2J [390]}	         If (nodeinfo(inod).nodeused) Then
		 psuccess *= (1 - systemDesign_.nodeinfo[inod].pfail);                      // {VB2J [391]}	            psuccess = psuccess * (1 - nodeinfo(inod).pfail)
		 }                                                                      // {VB2J [392]}	         End If
	   }                                                                        // {VB2J [393]}	      Next inod
	 for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                               // {VB2J [394]}	      For ilink = 1 To numlink
	   if (systemDesign_.linkinfo[ilink].linkused)  {                                         // {VB2J [395]}	         If (linkinfo(ilink).linkused) Then
		 psuccess *= (1 - systemDesign_.linkinfo[ilink].pfail);                     // {VB2J [396]}	            psuccess = psuccess * (1 - linkinfo(ilink).pfail)
		 }                                                                      // {VB2J [397]}	         End If
	   }                                                                        // {VB2J [398]}	      Next ilink


	systemDesign_.pfailureave = 0.99f * systemDesign_.pfailureave + 0.01f * (1 - psuccess);                  // {VB2J [401]}	      pfailureave = 0.99 * pfailureave + 0.01 * (1 - psuccess)


	// {VB2J [404]}	'      fixedcostsamplemin = 9000000000#
	// {VB2J [405]}	'      fixedcostsamplemax = -9000000000#
	// {VB2J [406]}	'
	// {VB2J [407]}	'      monthlycostsamplemin = 9000000000#
	// {VB2J [408]}	'      monthlycostsamplemax = -9000000000#
	// {VB2J [409]}	'
	// {VB2J [410]}	'      pfailuresamplemin = 9000000000#
	// {VB2J [411]}	'      pfailuresamplemax = -9000000000#
	// {VB2J [412]}	'
	// {VB2J [413]}	'      responsetimesamplemin = 9000000000#
	// {VB2J [414]}	'      responsetimesamplemax = -9000000000#

	// {VB2J [418]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [419]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [420]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [421]}	      ' messaging mods         '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [422]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [423]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [424]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

																				// {VB2J [426]}	      'find inter-host traffic

	systemDesign_.remotetraffic = 0;                                                        // {VB2J [428]}	      remotetraffic = 0#

	 for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {               // {VB2J [430]}	      For ifnjob = 1 To objfnjobset.numobjfn
	   for (int jfnjob=1; jfnjob<=systemDesign_.objfnjobset.numobjfn; jfnjob++) {             // {VB2J [431]}	      For jfnjob = 1 To objfnjobset.numobjfn
		 int ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                                     // {VB2J [432]}	         ifnx = objfnjobset.initfn(ifnjob)
		 int jfnx = systemDesign_.objfnjobset.initfn[jfnjob];                                     // {VB2J [433]}	         jfnx = objfnjobset.initfn(jfnjob)
		 int inod = systemDesign_.agent[ifnx].testhostid;                                         // {VB2J [434]}	         inod = agent(ifnx).testhostid
		 int jnod = systemDesign_.agent[jfnx].testhostid;                                         // {VB2J [435]}	         jnod = agent(jfnx).testhostid
		 if (inod != jnod)  {                                                   // {VB2J [436]}	         If (inod <> jnod) Then
			systemDesign_.remotetraffic += systemDesign_.xnfunction[ifnx].fcall[jfnx].sendmsgrate;              // {VB2J [437]}	            remotetraffic = remotetraffic + xnfunction(ifnx).fcall(jfnx).sendmsgrate
		   }                                                                    // {VB2J [438]}	         End If
		 }                                                                      // {VB2J [439]}	      Next jfnjob
	   }                                                                        // {VB2J [440]}	      Next ifnjob

	systemDesign_.remotetrafficave = 0.99f * systemDesign_.remotetrafficave + 0.01f * systemDesign_.remotetraffic;         // {VB2J [442]}	      remotetrafficave = 0.99 * remotetrafficave + 0.01 * remotetraffic

	// {VB2J [444]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [445]}	      '
	// {VB2J [446]}	      ' end of messaging mods
	// {VB2J [447]}	      '
	// {VB2J [448]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''


	// {VB2J [455]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [456]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [457]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [458]}	      ' marc EN 1.1 smoothing  '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [459]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [460]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [461]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

	// {VB2J [463]}	      'find pfailuresurvive:
	// {VB2J [464]}	      ' Minimize { Max(inod?system) [ Pfail(inod) * NumAgents(inod)] }

	systemDesign_.pfailuresurvive = 0;                                                       // {VB2J [466]}	      pfailuresurvive = 0

																				// {VB2J [468]}	      'find nodeinfo(inod).numagents:
	 for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [469]}	      For inod = 1 To numnode
		systemDesign_.nodeinfo[inod].numagents = 0;                                            // {VB2J [470]}	         nodeinfo(inod).numagents = 0
	   }                                                                        // {VB2J [471]}	      Next inod

	 for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {               // {VB2J [473]}	      For ifnjob = 1 To objfnjobset.numobjfn
	   int ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                                       // {VB2J [474]}	         ifnx = objfnjobset.initfn(ifnjob)
	   int inod = systemDesign_.agent[ifnx].testhostid;                                           // {VB2J [475]}	         inod = agent(ifnx).testhostid
	   systemDesign_.nodeinfo[inod].numagents++;                 // {VB2J [476]}	         nodeinfo(inod).numagents = nodeinfo(inod).numagents + 1
	   }                                                                        // {VB2J [477]}	      Next ifnjob

	 for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [479]}	      For inod = 1 To numnode
		pfailuresurvivex = systemDesign_.nodeinfo[inod].pfail * systemDesign_.nodeinfo[inod].numagents;              // {VB2J [480]}	         pfailuresurvivex = nodeinfo(inod).pfail * nodeinfo(inod).numagents
	   if (pfailuresurvivex > systemDesign_.pfailuresurvive)  {                               // {VB2J [481]}	         If (pfailuresurvivex > pfailuresurvive) Then
		 systemDesign_.pfailuresurvive = pfailuresurvivex;                                    // {VB2J [482]}	            pfailuresurvive = pfailuresurvivex
		 }                                                                      // {VB2J [483]}	         End If
	   }                                                                        // {VB2J [484]}	      Next inod

	systemDesign_.pfailuresurviveave = 0.99f * systemDesign_.pfailuresurviveave + 0.01f * systemDesign_.pfailuresurvive;   // {VB2J [486]}	      pfailuresurviveave = 0.99 * pfailuresurviveave + 0.01 * pfailuresurvive

	// {VB2J [488]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [489]}	      '
	// {VB2J [490]}	      ' end of marc EN 1.1 smoothing
	// {VB2J [491]}	      '
	// {VB2J [492]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''


	// {VB2J [499]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [500]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [501]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [502]}	      ' marc EN 1.1 hamming    '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [503]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [504]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [505]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

	// {VB2J [507]}	      'find hamming:

	 systemDesign_.hamming = 0;                                                               // {VB2J [509]}	      hamming = 0

	 for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {               // {VB2J [511]}	      For ifnjob = 1 To objfnjobset.numobjfn
	   int ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                                       // {VB2J [512]}	         ifnx = objfnjobset.initfn(ifnjob)
	   int inod = systemDesign_.agent[ifnx].testhostid;                                           // {VB2J [513]}	         inod = agent(ifnx).testhostid
	   int inodh = systemDesign_.agent[ifnx].hamminghostid;                                       // {VB2J [514]}	         inodh = agent(ifnx).hamminghostid
	   if (inod != inodh)  {                                                    // {VB2J [515]}	         If (inod <> inodh) Then
		 systemDesign_.hamming++;                                                 // {VB2J [516]}	            hamming = hamming + 1
		 }                                                                      // {VB2J [517]}	         End If
	   }                                                                        // {VB2J [518]}	      Next ifnjob

	systemDesign_.hammingave = 0.99f * systemDesign_.hammingave + 0.01f * systemDesign_.hamming;                           // {VB2J [520]}	      hammingave = 0.99 * hammingave + 0.01 * hamming

	// {VB2J [522]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [523]}	      '
	// {VB2J [524]}	      ' end of marc EN 1.1 hamming
	// {VB2J [525]}	      '
	// {VB2J [526]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

// block add for EN2.09
	systemDesign_.loadbal = 0;
    
    for (int inod1=1; inod1<=systemDesign_.numnode; inod1++) {
       for (int inod2=inod1+1; inod2<=systemDesign_.numnode; inod2++) {
          if ((systemDesign_.nodeinfo[inod1].type == 1 && systemDesign_.nodeinfo[inod1].enabled) && 
              (systemDesign_.nodeinfo[inod2].type == 1 && systemDesign_.nodeinfo[inod2].enabled)) {
             float utilcpu1 = systemDesign_.nodeinfo[inod1].cpuutil + systemDesign_.nodeinfo[inod1].cpubackground / 100;
             float utilcpu2 = systemDesign_.nodeinfo[inod2].cpuutil + systemDesign_.nodeinfo[inod2].cpubackground / 100;
             float utilmem1 = (systemDesign_.nodeinfo[inod1].memoryused / systemDesign_.nodeinfo[inod1].memory);
             float utilmem2 = (systemDesign_.nodeinfo[inod2].memoryused / systemDesign_.nodeinfo[inod2].memory);
 
             systemDesign_.loadbal += systemDesign_.loadbalcpumemratio * (utilcpu1 - utilcpu2) * (utilcpu1 - utilcpu2);
             systemDesign_.loadbal += (1.0 - systemDesign_.loadbalcpumemratio) * (utilmem1 - utilmem2)* (utilmem1 - utilmem2);
          } // End If
       } // Next inod2
    } // Next inod1
    
    systemDesign_.loadbal = (float)Math.sqrt(systemDesign_.loadbal);
    
    systemDesign_.loadbalave = 0.99f * systemDesign_.loadbalave + 0.01f * systemDesign_.loadbal;
    
// end block add for EN2.09

	
	
																				// {VB2J [536]}	      'find min/max of all samples for cost, responsetime, failure:

	 if ( ! systemDesign_.darparun)  {                                                        // {VB2J [538]}	      If (Not darparun) Then
	   if (systemDesign_.fixedcost > systemDesign_.fixedcostsamplemax)  {                                   // {VB2J [539]}	         If (fixedcost > fixedcostsamplemax) Then
		systemDesign_.fixedcostsamplemax = systemDesign_.fixedcost;                                        // {VB2J [540]}	            fixedcostsamplemax = fixedcost
		 }                                                                      // {VB2J [541]}	         End If

	   if (systemDesign_.fixedcost < systemDesign_.fixedcostsamplemin)  {                                   // {VB2J [543]}	         If (fixedcost < fixedcostsamplemin) Then
		systemDesign_.fixedcostsamplemin = systemDesign_.fixedcost;                                        // {VB2J [544]}	            fixedcostsamplemin = fixedcost
		 }                                                                      // {VB2J [545]}	         End If

	   if (systemDesign_.monthlycost < systemDesign_.monthlycostsamplemin)  {                               // {VB2J [547]}	         If (monthlycost < monthlycostsamplemin) Then
		systemDesign_.monthlycostsamplemin = systemDesign_.monthlycost;                                    // {VB2J [548]}	            monthlycostsamplemin = monthlycost
		 }                                                                      // {VB2J [549]}	         End If

	   if (systemDesign_.monthlycost > systemDesign_.monthlycostsamplemax)  {                               // {VB2J [551]}	         If (monthlycost > monthlycostsamplemax) Then
		systemDesign_.monthlycostsamplemax = systemDesign_.monthlycost;                                    // {VB2J [552]}	            monthlycostsamplemax = monthlycost
		 }                                                                      // {VB2J [553]}	         End If

	   if (systemDesign_.ftracetime < systemDesign_.responsetimesamplemin)  {                               // {VB2J [555]}	         If (ftracetime < responsetimesamplemin) Then
		systemDesign_.responsetimesamplemin = systemDesign_.ftracetime;                                    // {VB2J [556]}	            responsetimesamplemin = ftracetime
		 }                                                                      // {VB2J [557]}	         End If

	   if (systemDesign_.ftracetime > systemDesign_.responsetimesamplemax)  {                               // {VB2J [559]}	         If (ftracetime > responsetimesamplemax) Then
		systemDesign_.responsetimesamplemax = systemDesign_.ftracetime;                                    // {VB2J [560]}	            responsetimesamplemax = ftracetime
		 }                                                                      // {VB2J [561]}	         End If

	   }                                                                        // {VB2J [563]}	      End If

	 if ((1 - psuccess) < systemDesign_.pfailuresamplemin)  {                                 // {VB2J [565]}	      If ((1 - psuccess) < pfailuresamplemin) Then
		systemDesign_.pfailuresamplemin = 1 - psuccess;                                        // {VB2J [566]}	         pfailuresamplemin = 1 - psuccess
	   }                                                                        // {VB2J [567]}	      End If

	 if ((1 - psuccess) > systemDesign_.pfailuresamplemax)  {                                 // {VB2J [569]}	      If ((1 - psuccess) > pfailuresamplemax) Then
		systemDesign_.pfailuresamplemax = 1 - psuccess;                                        // {VB2J [570]}	         pfailuresamplemax = 1 - psuccess
	   }                                                                        // {VB2J [571]}	      End If


	// {VB2J [576]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [577]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [578]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [579]}	      ' messaging mods         '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [580]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [581]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [582]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

	 if (systemDesign_.remotetraffic < systemDesign_.remotetrafficsamplemin)  {                             // {VB2J [584]}	      If (remotetraffic < remotetrafficsamplemin) Then
		systemDesign_.remotetrafficsamplemin = systemDesign_.remotetraffic;                                  // {VB2J [585]}	         remotetrafficsamplemin = remotetraffic
	   }                                                                        // {VB2J [586]}	      End If

	 if (systemDesign_.remotetraffic > systemDesign_.remotetrafficsamplemax)  {                             // {VB2J [588]}	      If (remotetraffic > remotetrafficsamplemax) Then
		systemDesign_.remotetrafficsamplemax = systemDesign_.remotetraffic;                                  // {VB2J [589]}	         remotetrafficsamplemax = remotetraffic
	   }                                                                        // {VB2J [590]}	      End If

	// {VB2J [592]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [593]}	      '
	// {VB2J [594]}	      ' end of messaging mods
	// {VB2J [595]}	      '
	// {VB2J [596]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''


	// {VB2J [601]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [602]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [603]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [604]}	      ' marc EN 1.1 smoothing  '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [605]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [606]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [607]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

	 if (systemDesign_.pfailuresurvive < systemDesign_.pfailuresurvivesamplemin)  {                         // {VB2J [609]}	      If (pfailuresurvive < pfailuresurvivesamplemin) Then
		systemDesign_.pfailuresurvivesamplemin = systemDesign_.pfailuresurvive;                              // {VB2J [610]}	         pfailuresurvivesamplemin = pfailuresurvive
	   }                                                                        // {VB2J [611]}	      End If

	 if (systemDesign_.pfailuresurvive > systemDesign_.pfailuresurvivesamplemax)  {                         // {VB2J [613]}	      If (pfailuresurvive > pfailuresurvivesamplemax) Then
		systemDesign_.pfailuresurvivesamplemax = systemDesign_.pfailuresurvive;                              // {VB2J [614]}	         pfailuresurvivesamplemax = pfailuresurvive
	   }                                                                        // {VB2J [615]}	      End If

	// {VB2J [617]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [618]}	      '
	// {VB2J [619]}	      ' end of marc EN 1.1 smoothing
	// {VB2J [620]}	      '
	// {VB2J [621]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''


	// {VB2J [630]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [631]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [632]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [633]}	      ' marc EN 1.1 hamming    '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [634]}	      '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [635]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [636]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

	 if (systemDesign_.hamming < systemDesign_.hammingsamplemin)  {                                         // {VB2J [638]}	      If (hamming < hammingsamplemin) Then
		systemDesign_.hammingsamplemin = systemDesign_.hamming;                                              // {VB2J [639]}	         hammingsamplemin = hamming
	   }                                                                        // {VB2J [640]}	      End If

	 if (systemDesign_.hamming > systemDesign_.hammingsamplemax)  {                                         // {VB2J [642]}	      If (hamming > hammingsamplemax) Then
		systemDesign_.hammingsamplemax = systemDesign_.hamming;                                              // {VB2J [643]}	         hammingsamplemax = hamming
	   }                                                                        // {VB2J [644]}	      End If

	// {VB2J [646]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
	// {VB2J [647]}	      '
	// {VB2J [648]}	      ' end of marc EN 1.1 hamming
	// {VB2J [649]}	      '
	// {VB2J [650]}	      ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

	 
// begin EN2.09 insert
//     ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//     ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//     '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//     ' marc EN 2.0 load balancing    ''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//     '                        '''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//     ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//     ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

     if (systemDesign_.loadbal < systemDesign_.loadbalsamplemin) {
         systemDesign_.loadbalsamplemin = systemDesign_.loadbal;
   }

     if (systemDesign_.loadbal > systemDesign_.loadbalsamplemax) {
         systemDesign_.loadbalsamplemax = systemDesign_.loadbal;
   }

//     ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//     '
//     ' end of marc EN 2.0 load balancing
//     '
//     ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//   end EN2.09 insert
	 
     
	 

	// {VB2J [660]}	      'The constraint violations should have no contribution to the objective function
	// {VB2J [661]}	      'when fixedcostcheck, monthlycostcheck, pfailurecheck, or responsetimecheck are not checked
	// {VB2J [662]}	      'in form metrparmex. This applies to both the min and max constraint violations

	// {VB2J [665]}	      'marc en 1.1 smoothing and hamming: add test for remotetraffic, pfailuresurvive, and hamming:
   
	 if (  ((systemDesign_.fixedcostscale > 0) && (systemDesign_.fixedcost > systemDesign_.fixedcostmax)) 
	 	|| ((systemDesign_.monthlycostscale > 0) && (systemDesign_.monthlycost > systemDesign_.monthlycostmax)) 
	 	|| ((systemDesign_.pfailurescale > 0) && (1 - psuccess) > systemDesign_.pfailuremax) 
	 	|| ((systemDesign_.responsetimescale > 0) && (systemDesign_.ftracetime > systemDesign_.responsetimemax)) 
	 	|| ((systemDesign_.remotetrafficscale > 0) && (systemDesign_.remotetraffic > systemDesign_.remotetrafficmax)) 
	 	|| ((systemDesign_.pfailuresurvivescale > 0) && (systemDesign_.pfailuresurvive > systemDesign_.pfailuresurvivemax)) 
	 	|| ((systemDesign_.hammingscale > 0) && (systemDesign_.hamming > systemDesign_.hammingmax))
	 	|| ((systemDesign_.loadbalscale > 0) && (systemDesign_.loadbal > systemDesign_.loadbalmax)) // EN2.09

	 ) { // {VB2J [672]}	      If (((fixedcostscale > 0#) And (fixedcost > fixedcostmax)) Or           ((monthlycostscale > 0#) And (monthlycost > monthlycostmax)) Or           ((pfailurescale > 0#) And (1 - psuccess) > pfailuremax) Or           ((responsetimescale > 0#) And (ftracetime > responsetimemax)) Or           ((remotetrafficscale > 0#) And (remotetraffic > remotetrafficmax)) Or           ((pfailuresurvivescale > 0#) And (pfailuresurvive > pfailuresurvivemax)) Or           ((hammingscale > 0#) And (hamming > hammingmax))) Then

// {VB2J [674]}	         'suggested fix 2/12/02: set some other type of failure flag, rather than state.status, for constraint
	   objx = Float.MAX_VALUE;                                                      // {VB2J [675]}	         objx = 9999000000#
	   } else {                                                                 // {VB2J [676]}	      Else
// {VB2J [677]}	         'objx = value of objective function this increment
	   objx = 0;                                                               // {VB2J [678]}	         objx = 0#

// not DARPA
//	   if ( ! systemDesign_.darparun)  {                                                      // {VB2J [680]}	         If (Not darparun) Then
//		 if (systemDesign_.fixedcost > systemDesign_.fixedcostmin)  {                                       // {VB2J [681]}	            If (fixedcost > fixedcostmin) Then
//		   objx += systemDesign_.fixedcost * systemDesign_.fixedcostscale;                            // {VB2J [682]}	               objx = objx + fixedcost * fixedcostscale
//		   } else {                                                             // {VB2J [683]}	            Else
//		   objx += systemDesign_.fixedcostmin * systemDesign_.fixedcostscale;                         // {VB2J [684]}	               objx = objx + fixedcostmin * fixedcostscale
//		   }                                                                    // {VB2J [685]}	            End If
//
//		 if (systemDesign_.monthlycost > systemDesign_.monthlycostmin)  {                                   // {VB2J [687]}	            If (monthlycost > monthlycostmin) Then
//		   objx += systemDesign_.monthlycost * systemDesign_.monthlycostscale;                        // {VB2J [688]}	               objx = objx + monthlycost * monthlycostscale
//		   } else {   
//		
//		// rec - was    	                                                          // {VB2J [689]}	            Else
//		//	objx += systemDesign_.monthlymin * systemDesign_.monthlycostscale;                         // {VB2J [690]}	               objx = objx + monthlymin * monthlycostscale
//			objx += systemDesign_.monthlycostmin * systemDesign_.monthlycostscale;                         // {VB2J [690]}	               objx = objx + monthlymin * monthlycostscale
//		   }                                                                    // {VB2J [691]}	            End If
//
//		 if (systemDesign_.ftracetime > systemDesign_.responsetimemin)  {                                   // {VB2J [693]}	            If (ftracetime > responsetimemin) Then
//		   objx += systemDesign_.ftracetime * systemDesign_.responsetimescale;                        // {VB2J [694]}	               objx = objx + ftracetime * responsetimescale
//		   } else {                                                             // {VB2J [695]}	            Else
//		   objx += systemDesign_.responsetimemin * systemDesign_.responsetimescale;                   // {VB2J [696]}	               objx = objx + responsetimemin * responsetimescale
//		   }                                                                    // {VB2J [697]}	            End If
//		 }                                                                      // {VB2J [698]}	         End If


	// {VB2J [701]}	         'messaging mods
	   objx += systemDesign_.remotetraffic * systemDesign_.remotetrafficscale;                        // {VB2J [702]}	         objx = objx + remotetraffic * remotetrafficscale
	// {VB2J [703]}	         'end of messaging mods

	   
// EN2.09
// 'marc en 2.02 blend load balancing and messaging
       if (systemDesign_.blendloadbalmessaging && ! systemDesign_.blendloadbalmessagingfirstpass) {
          float dloadbalfirstpass = systemDesign_.loadbalmaxfirstpass - systemDesign_.loadbalminfirstpass;
          float dl = systemDesign_.loadbal - (systemDesign_.loadbalminfirstpass + 0.1f * dloadbalfirstpass);
          if (dl > 0) {
             objx *= Math.exp(2.0f * dl / dloadbalfirstpass); // 'nominal max out at exp(1.8)
          }
       }
       
	// {VB2J [705]}	         'marc EN 1.1 smoothing
	   objx += systemDesign_.pfailuresurvive * systemDesign_.pfailuresurvivescale;                    // {VB2J [706]}	         objx = objx + pfailuresurvive * pfailuresurvivescale
	// {VB2J [707]}	         'end of marc EN 1.1 smoothing

	// {VB2J [709]}	         'marc EN 1.1 hamming
	   objx += systemDesign_.hamming * systemDesign_.hammingscale;                                    // {VB2J [710]}	         objx = objx + hamming * hammingscale
	// {VB2J [711]}	         'end of marc EN 1.1 hamming

	   
       // EN2.09 'en 2.0 load balancing
       objx += systemDesign_.loadbal * systemDesign_.loadbalscale;
       
       

	   if ((1 - psuccess) > systemDesign_.pfailuremin)  {                                     // {VB2J [714]}	         If ((1 - psuccess) > pfailuremin) Then
		 objx = objx + (1 - psuccess) * systemDesign_.pfailurescale;                          // {VB2J [715]}	            objx = objx + (1 - psuccess) * pfailurescale
		 } else {                                                               // {VB2J [716]}	         Else
		 objx = objx + systemDesign_.pfailuremin * systemDesign_.pfailurescale;                             // {VB2J [717]}	            objx = objx + pfailuremin * pfailurescale
		 }                                                                      // {VB2J [718]}	         End If

		systemDesign_.objectiveave = 0.99f * systemDesign_.objectiveave + 0.01f * objx;                        // {VB2J [720]}	         objectiveave = 0.99 * objectiveave + 0.01 * objx

	   }                                                                        // {VB2J [722]}	      End If
	 }                                                                          // {VB2J [723]}	   End If


   if (systemDesign_.darparun && systemDesign_.state.status)  {                                              // {VB2J [728]}	   If (darparun And State.status) Then
	 if ((systemDesign_.requirednodes[0] < systemDesign_.minultralognodes) 
	   && (systemDesign_.annealtemp < 1))  {           // {VB2J [729]}	      If ((requirednodes(0) < minultralognodes) And (annealtemp < 1)) Then
																				// {VB2J [730]}	         'State.status = False
	   objx = (systemDesign_.minultralognodes - systemDesign_.requirednodes[0] + 1) 
	   			* objx / systemDesign_.annealtemp;              // {VB2J [731]}	         objx = (minultralognodes - requirednodes(0) + 1) * objx / annealtemp
	   }                                                                        // {VB2J [732]}	      End If
	 }                                                                          // {VB2J [733]}	   End If

	// {VB2J [749]}	   'save this successful state for later perturbation

	// {VB2J [751]}	   'NOTE: this does not store the saved states in order of solution quality (objx)
	// {VB2J [752]}	   'For example, statelist(1).e is NOT the best solution (lowest objx).
	// {VB2J [753]}	   'Storing in sorted order of goodness would require an extra sort of the list
	// {VB2J [754]}	   'for each step of the iteration (OUCH). The stored solutions
	// {VB2J [755]}	   'Statelist(1).e to Statelist(numsavedstatesmax).e is simply an unsorted list of the
	// {VB2J [756]}	   'best numsavedstatesmax solutions.


   if (systemDesign_.state.status)  {                                                         // {VB2J [759]}	   If (State.status) Then
																				// {VB2J [760]}	      'see if this state is better than any saved state:
																				// {VB2J [761]}	      'If (state.numfstatecallsave = 0) Then
	 if (systemDesign_.numstatelist == 0)  {                                                  // {VB2J [762]}	      If (numstatelist = 0) Then
																				// {VB2J [763]}	         'store this as the initial annealing state

		systemDesign_.statelist[0].e = objx;                                         // {VB2J [765]}	         statelist(0).e = objx
		systemDesign_.statelist[0].fixedcost = systemDesign_.fixedcost;              // {VB2J [766]}	         statelist(0).fixedcost = fixedcost
		systemDesign_.statelist[0].monthlycost = systemDesign_.monthlycost;          // {VB2J [767]}	         statelist(0).monthlycost = monthlycost
		systemDesign_.statelist[0].responsetime = systemDesign_.ftracetime;          // {VB2J [768]}	         statelist(0).responsetime = ftracetime
		systemDesign_.statelist[0].pfailure = 1 - systemDesign_.psuccess;            // {VB2J [769]}	         statelist(0).pfailure = 1 - psuccess
		systemDesign_.statelist[0].psuccess = systemDesign_.psuccess;                // {VB2J [770]}	         statelist(0).psuccess = psuccess
// {VB2J [771]}	         'messaging mods
		systemDesign_.statelist[0].remotetraffic = systemDesign_.remotetraffic;      // {VB2J [772]}	         statelist(0).remotetraffic = remotetraffic
// {VB2J [773]}	         'marc EN 1.1 smoothing
		systemDesign_.statelist[0].pfailuresurvive = systemDesign_.pfailuresurvive;  // {VB2J [774]}	         statelist(0).pfailuresurvive = pfailuresurvive
// {VB2J [775]}	         'marc EN 1.1 hamming
		systemDesign_.statelist[0].hamming = systemDesign_.hamming;                  // {VB2J [776]}	         statelist(0).hamming = hamming

        // EN2.09
		systemDesign_.statelist[0].loadbal = systemDesign_.loadbal;
   
		
		systemDesign_.statelist[0].fixedcostscale = systemDesign_.fixedcostscale;    // {VB2J [778]}	         statelist(0).fixedcostscale = fixedcostscale
		systemDesign_.statelist[0].monthlycostscale = systemDesign_.monthlycostscale;// {VB2J [779]}	         statelist(0).monthlycostscale = monthlycostscale
		systemDesign_.statelist[0].pfailurescale = systemDesign_.pfailurescale;      // {VB2J [780]}	         statelist(0).pfailurescale = pfailurescale
		systemDesign_.statelist[0].responsetimescale = systemDesign_.responsetimescale; // {VB2J [781]}	         statelist(0).responsetimescale = responsetimescale
// {VB2J [782]}	         'messaging mods
		systemDesign_.statelist[0].remotetrafficscale = systemDesign_.remotetrafficscale;      // {VB2J [783]}	         statelist(0).remotetrafficscale = remotetrafficscale
// {VB2J [784]}	         'marc EN 1.1 smoothing
		systemDesign_.statelist[0].pfailuresurvivescale = systemDesign_.pfailuresurvivescale;  // {VB2J [785]}	         statelist(0).pfailuresurvivescale = pfailuresurvivescale
// {VB2J [786]}	         'marc EN 1.1 hamming
		systemDesign_.statelist[0].hammingscale = systemDesign_.hammingscale;                  // {VB2J [787]}	         statelist(0).hammingscale = hammingscale

        // EN2.09
		systemDesign_.statelist[0].loadbalscale = systemDesign_.loadbalscale;
        
		systemDesign_.statelist[0].temp = systemDesign_.annealtemp;                            // {VB2J [789]}	         statelist(0).temp = annealtemp

	   for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                // {VB2J [791]}	         For inod = 1 To numnode
		 if (systemDesign_.nodeinfo[inod].nodeused)  {                                        // {VB2J [792]}	            If (nodeinfo(inod).nodeused) Then
			systemDesign_.statelist[0].nodeused[inod] = true;                                  // {VB2J [793]}	               statelist(0).nodeused(inod) = True
		   } else {                                                             // {VB2J [794]}	            Else
			systemDesign_.statelist[0].nodeused[inod] = false;                                 // {VB2J [795]}	               statelist(0).nodeused(inod) = False
		   }                                                                    // {VB2J [796]}	            End If
		 }                                                                      // {VB2J [797]}	         Next inod

	   for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                             // {VB2J [799]}	         For ilink = 1 To numlink
		 if (systemDesign_.linkinfo[ilink].linkused)  {                                       // {VB2J [800]}	            If (linkinfo(ilink).linkused) Then
			systemDesign_.statelist[0].linkused[ilink] = true;                                 // {VB2J [801]}	               statelist(0).linkused(ilink) = True
		   } else {                                                             // {VB2J [802]}	            Else
			systemDesign_.statelist[0].linkused[ilink] = false;                                // {VB2J [803]}	               statelist(0).linkused(ilink) = False
		   }                                                                    // {VB2J [804]}	            End If
		 }                                                                      // {VB2J [805]}	         Next ilink


	   if (systemDesign_.darparun)  {                                                         // {VB2J [808]}	         If (darparun) Then
		 for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {           // {VB2J [809]}	            For ifnjob = 1 To objfnjobset.numobjfn
		   int ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                                   // {VB2J [810]}	               ifnx = objfnjobset.initfn(ifnjob)
		   systemDesign_.agent[ifnx].annealhostid = systemDesign_.agent[ifnx].testhostid;                   // {VB2J [811]}	               agent(ifnx).annealhostid = agent(ifnx).testhostid
		   systemDesign_.agent[ifnx].savedhostid[0] = systemDesign_.agent[ifnx].testhostid;                 // {VB2J [812]}	               agent(ifnx).savedhostid(0) = agent(ifnx).testhostid
		   }                                                                    // {VB2J [813]}	            Next ifnjob
		 }                                                                      // {VB2J [814]}	         End If


	   for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                // {VB2J [817]}	         For inod = 1 To numnode
		 for (int ifnx=1; ifnx<=systemDesign_.numfunction; ifnx++) {                          // {VB2J [818]}	            For ifnx = 1 To numfunction
			systemDesign_.statelist[0].fstatecallsave[ifnx][inod] = systemDesign_.state.fstatecall[ifnx][inod];              // {VB2J [819]}	               statelist(0).fstatecallsave(ifnx, inod) = State.fstatecall(ifnx, inod)
		   }                                                                    // {VB2J [820]}	            Next ifnx
		 }                                                                      // {VB2J [821]}	         Next inod


	// {VB2J [825]}	         'store this as the first saved state
	// {VB2J [826]}	         'state.numfstatecallsave = 1
	systemDesign_.numstatelist = 1;                                                        // {VB2J [827]}	         numstatelist = 1

	// {VB2J [830]}	         'speedup: Just set statelist(1) = statelist(0) for case numstatelist=0-->1

	systemDesign_.statelist[1].e = objx;                                                   // {VB2J [832]}	         statelist(1).e = objx
	systemDesign_.statelist[1].fixedcost = systemDesign_.fixedcost;                                      // {VB2J [833]}	         statelist(1).fixedcost = fixedcost
	systemDesign_.statelist[1].monthlycost = systemDesign_.monthlycost;                                  // {VB2J [834]}	         statelist(1).monthlycost = monthlycost
	systemDesign_.statelist[1].responsetime = systemDesign_.ftracetime;                                  // {VB2J [835]}	         statelist(1).responsetime = ftracetime
	systemDesign_.statelist[1].pfailure = 1 - systemDesign_.psuccess;                                    // {VB2J [836]}	         statelist(1).pfailure = 1 - psuccess
	  systemDesign_.statelist[1].psuccess = systemDesign_.psuccess;                                        // {VB2J [837]}	         statelist(1).psuccess = psuccess

	// {VB2J [839]}	         'messaging mods
	systemDesign_.statelist[1].remotetraffic = systemDesign_.remotetraffic;                              // {VB2J [840]}	         statelist(1).remotetraffic = remotetraffic
	// {VB2J [841]}	         'marc EN 1.1 smoothing
	systemDesign_.statelist[1].pfailuresurvive = systemDesign_.pfailuresurvive;                          // {VB2J [842]}	         statelist(1).pfailuresurvive = pfailuresurvive
	// {VB2J [843]}	         'marc EN 1.1 hamming
	systemDesign_.statelist[1].hamming = systemDesign_.hamming;                                          // {VB2J [844]}	         statelist(1).hamming = hamming

    // EN2.09
	systemDesign_.statelist[1].loadbal = systemDesign_.loadbal;

	
	systemDesign_.statelist[1].fixedcostscale = systemDesign_.fixedcostscale;                            // {VB2J [846]}	         statelist(1).fixedcostscale = fixedcostscale
	systemDesign_.statelist[1].monthlycostscale = systemDesign_.monthlycostscale;                        // {VB2J [847]}	         statelist(1).monthlycostscale = monthlycostscale
	systemDesign_.statelist[1].pfailurescale = systemDesign_.pfailurescale;                              // {VB2J [848]}	         statelist(1).pfailurescale = pfailurescale
	systemDesign_.statelist[1].responsetimescale = systemDesign_.responsetimescale;                      // {VB2J [849]}	         statelist(1).responsetimescale = responsetimescale

	// {VB2J [851]}	         'messaging mods
	systemDesign_.statelist[1].remotetrafficscale = systemDesign_.remotetrafficscale;                    // {VB2J [852]}	         statelist(1).remotetrafficscale = remotetrafficscale
	// {VB2J [853]}	         'marc EN 1.1 smoothing
	systemDesign_.statelist[1].pfailuresurvivescale = systemDesign_.pfailuresurvivescale;                // {VB2J [854]}	         statelist(1).pfailuresurvivescale = pfailuresurvivescale
	// {VB2J [855]}	         'marc EN 1.1 hamming
	systemDesign_.statelist[1].hammingscale = systemDesign_.hammingscale;                                // {VB2J [856]}	         statelist(1).hammingscale = hammingscale

    // EN2.09
	systemDesign_.statelist[1].loadbalscale = systemDesign_.loadbalscale;

	   systemDesign_.statelist[1].temp = systemDesign_.annealtemp;                                          // {VB2J [858]}	         statelist(1).temp = annealtemp



	   for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                // {VB2J [862]}	         For inod = 1 To numnode
		 if (systemDesign_.nodeinfo[inod].nodeused)  {                                        // {VB2J [863]}	            If (nodeinfo(inod).nodeused) Then
			systemDesign_.statelist[1].nodeused[inod] = true;                                  // {VB2J [864]}	               statelist(1).nodeused(inod) = True
		   } else {                                                             // {VB2J [865]}	            Else
			systemDesign_.statelist[1].nodeused[inod] = false;                                 // {VB2J [866]}	               statelist(1).nodeused(inod) = False
		   }                                                                    // {VB2J [867]}	            End If
		 }                                                                      // {VB2J [868]}	         Next inod

	   for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                             // {VB2J [870]}	         For ilink = 1 To numlink
// rec - should have been 'ilink', not 'inod'
		 if (systemDesign_.linkinfo[ilink].linkused)  {                                        // {VB2J [871]}	            If (linkinfo(inod).linkused) Then
			systemDesign_.statelist[1].linkused[ilink] = true;                                 // {VB2J [872]}	               statelist(1).linkused(ilink) = True
		   } else {                                                             // {VB2J [873]}	            Else
			systemDesign_.statelist[1].linkused[ilink] = false;                                // {VB2J [874]}	               statelist(1).linkused(ilink) = False
		   }                                                                    // {VB2J [875]}	            End If
		 }                                                                      // {VB2J [876]}	         Next ilink


	   if (systemDesign_.darparun)  {                                                         // {VB2J [880]}	         If (darparun) Then
		 for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {           // {VB2J [881]}	            For ifnjob = 1 To objfnjobset.numobjfn
		   int ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                                   // {VB2J [882]}	               ifnx = objfnjobset.initfn(ifnjob)
		   systemDesign_.agent[ifnx].savedhostid[1] = systemDesign_.agent[ifnx].testhostid;                 // {VB2J [883]}	               agent(ifnx).savedhostid(1) = agent(ifnx).testhostid
		   }                                                                    // {VB2J [884]}	            Next ifnjob
		 }                                                                      // {VB2J [885]}	         End If

	   for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                // {VB2J [889]}	         For inod = 1 To numnode
		 for (int ifnx=1; ifnx<=systemDesign_.numfunction; ifnx++) {                          // {VB2J [890]}	            For ifnx = 1 To numfunction
			systemDesign_.statelist[1].fstatecallsave[ifnx][inod] = systemDesign_.state.fstatecall[ifnx][inod];              // {VB2J [891]}	               statelist(1).fstatecallsave(ifnx, inod) = State.fstatecall(ifnx, inod)
		   }                                                                    // {VB2J [892]}	            Next ifnx
		 }                                                                      // {VB2J [893]}	         Next inod


	   } else {                                                                 // {VB2J [898]}	      Else 'numstatelist > 0
	// {VB2J [899]}	         'first see if it is equal to any saved state:
	// {VB2J [900]}	         'skip update if this is the same state as the state objx
	   int iposmaxsave = -1;                                                        // {VB2J [901]}	         iposmaxsave = -1
	   float objmax = -Float.MAX_VALUE; // -9999900000;                                                   // {VB2J [902]}	         objmax = -9999900000#
// EN2.09: unused:	   boolean ifoundequal = false;                                                     // {VB2J [903]}	         ifoundequal = False

	   boolean updateannealstateflag = false;                                           // {VB2J [905]}	         updateannealstateflag = False 'en 1.1 cleanup

	   for (int i=1; i<=systemDesign_.numstatelist; i++) {                                    // {VB2J [907]}	         For i = 1 To numstatelist
		 if (systemDesign_.statelist[i].e > objmax)  {                                        // {VB2J [908]}	            If (statelist(i).e > objmax) Then
		   iposmaxsave = i;                                                     // {VB2J [909]}	               iposmaxsave = i
		   objmax = systemDesign_.statelist[i].e;                                             // {VB2J [910]}	               objmax = statelist(i).e
		   }                                                                    // {VB2J [911]}	            End If

	// {VB2J [913]}	            'first see if it is the same basic node set:

		 boolean statenotequalflag = false;                                             // {VB2J [915]}	            statenotequalflag = False 'en 1.1 cleanup

		 if ( ! systemDesign_.darparun)  {                                                    // {VB2J [917]}	            If (Not darparun) Then
		   for (int inod=1; inod<=systemDesign_.numnode; inod++) {                            // {VB2J [918]}	               For inod = 1 To numnode
			 for (int ifnx=1; ifnx<=systemDesign_.numfunction; ifnx++) {                      // {VB2J [919]}	                  For ifnx = 1 To numfunction

	// {VB2J [921]}	                     'if (nodeused(inod) > 0
	// {VB2J [922]}	                     'if there is a positive function use on these nodes,
	// {VB2J [923]}	                     'then include this as a valid option

			   if (systemDesign_.state.fstatecall[ifnx][inod] != 
			   		systemDesign_.statelist[i].fstatecallsave[ifnx][inod])  { // {VB2J [925]}	                     If ((State.fstatecall(ifnx, inod) <> statelist(i).fstatecallsave(ifnx, inod))) Then
	// {VB2J [926]}	                        'and nodeused(inod) > 0 and linkused(ilink) > 0
	// {VB2J [927]}	                        'some states in the state matrix are unused.

				 if (systemDesign_.nodeinfo[inod].nodeused == true 
				  || systemDesign_.statelist[i].nodeused[inod] == true)  { // {VB2J [929]}	                        If (nodeinfo(inod).nodeused = True Or statelist(i).nodeused(inod) = True) Then
	// {VB2J [930]}	                           'flag it as a new state
				   statenotequalflag = true;                                    // {VB2J [931]}	                           statenotequalflag = True  'en 1.1 cleanup
				   break;                                                       // {VB2J [932]}	                           Exit For
																				// {VB2J [933]}	                           'GoTo statenotequal
				   }                                                            // {VB2J [934]}	                        End If
				 }                                                              // {VB2J [935]}	                     End If
			   }                                                                // {VB2J [936]}	                  Next ifnx

			 iterations_ = iterk*1000  + iter;	// update this before return - 19sept04

			 if (statenotequalflag) return;                                // {VB2J [938]}	                  If (statenotequalflag) Then Exit For
			   }                                                                // {VB2J [939]}	               Next inod
	// {VB2J [940]}	               'state matrix checks of state.fstatecall = statelist(i).fstatecallsave, so check fixedcost, etc:

			 } else {                                                           // {VB2J [942]}	            Else 'darparun = true

			 for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {       // {VB2J [944]}	               For ifnjob = 1 To objfnjobset.numobjfn
			   int ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                               // {VB2J [945]}	                  ifnx = objfnjobset.initfn(ifnjob)
			   int inod = systemDesign_.agent[ifnx].testhostid;                                   // {VB2J [946]}	                  inod = agent(ifnx).testhostid

			   if (systemDesign_.agent[ifnx].testhostid != systemDesign_.agent[ifnx].savedhostid[i])  {     // {VB2J [948]}	                  If (agent(ifnx).testhostid <> agent(ifnx).savedhostid(i)) Then
				 if (systemDesign_.nodeinfo[inod].nodeused == true 
				  || systemDesign_.statelist[i].nodeused[inod])  { // {VB2J [949]}	                     If (nodeinfo(inod).nodeused = True Or statelist(i).nodeused(inod)) Then
				   statenotequalflag = true;                                    // {VB2J [950]}	                        statenotequalflag = True  'en 1.1 cleanup
				   break;                                                       // {VB2J [951]}	                        Exit For
																				// {VB2J [952]}	                        'GoTo statenotequal
				   }                                                            // {VB2J [953]}	                     End If
				 }                                                              // {VB2J [954]}	                  End If
			   }                                                                // {VB2J [955]}	               Next ifnjob

			 }                                                                  // {VB2J [957]}	            End If


	// {VB2J [960]}	               'If ((state.fixedcost(i) = fixedcost) And state.monthlycost(i) = monthlycost And state.responsetime(i) = ftracetime) Then
	// {VB2J [961]}	               'If (statelist(i).fixedcost <> fixedcost) Then statenotequalflag = True
	// {VB2J [962]}	               'If (statelist(i).monthlycost <> monthlycost) Then statenotequalflag = True
	// {VB2J [963]}	               'If (statelist(i).pfailure <> 1 - psuccess) Then statenotequalflag = True
	// {VB2J [964]}	               'If (statelist(i).responsetime <> ftracetime) Then statenotequalflag = True

		   if ( ! statenotequalflag)  {                                         // {VB2J [966]}	            If (Not statenotequalflag) Then

// EN2.09: unused:			 ifoundequal = true;                                                // {VB2J [968]}	               ifoundequal = True
		    
	// {VB2J [969]}	               'this is a previous solution, with updated scale variables
	// {VB2J [970]}	               'now just update the objective "scale" variables
	systemDesign_.statelist[i].fixedcostscale = systemDesign_.fixedcostscale;                      // {VB2J [971]}	               statelist(i).fixedcostscale = fixedcostscale
	systemDesign_.statelist[i].monthlycostscale = systemDesign_.monthlycostscale;                  // {VB2J [972]}	               statelist(i).monthlycostscale = monthlycostscale
	systemDesign_.statelist[i].pfailurescale = systemDesign_.pfailurescale;                        // {VB2J [973]}	               statelist(i).pfailurescale = pfailurescale
	systemDesign_.statelist[i].responsetimescale = systemDesign_.responsetimescale;                // {VB2J [974]}	               statelist(i).responsetimescale = responsetimescale

	// {VB2J [976]}	               'messaging mods
	systemDesign_.statelist[i].remotetrafficscale = systemDesign_.remotetrafficscale;              // {VB2J [977]}	               statelist(i).remotetrafficscale = remotetrafficscale
	// {VB2J [978]}	               'marc EN 1.1 smoothing
	systemDesign_.statelist[i].pfailuresurvivescale = systemDesign_.pfailuresurvivescale;              // {VB2J [979]}	               statelist(i).pfailuresurvivescale = pfailuresurvivescale
	// {VB2J [980]}	               'marc EN 1.1 hamming
	systemDesign_.statelist[i].hammingscale = systemDesign_.hammingscale;                          // {VB2J [981]}	               statelist(i).hammingscale = hammingscale

    // EN2.09 - 'EN 2.0 load balancing
	systemDesign_.statelist[i].loadbalscale = systemDesign_.loadbalscale;

	systemDesign_.statelist[i].e = objx;                                             // {VB2J [983]}	               statelist(i).e = objx

// rec - unused
//			 iposequalsave = i;                                                 // {VB2J [985]}	               iposequalsave = i

			 updateannealstateflag = true;                                      // {VB2J [987]}	               updateannealstateflag = True 'en 1.1 cleanup
			 break;                                                             // {VB2J [988]}	               Exit For
																				// {VB2J [989]}	               'GoTo updateannealstate
			 }                                                                  // {VB2J [990]}	            End If

	// {VB2J [992]}	            'update the stored costscale, temp etc parameters for this state:
		   }                                                                    // {VB2J [993]}	         Next i


		 if ( ! updateannealstateflag)  {                                       // {VB2J [997]}	         If (Not updateannealstateflag) Then
	// {VB2J [998]}	            'by this point, there are no equal states, so look for a better state:
// EN2.09: unused:		   boolean ifoundbetter = false;                                                // {VB2J [999]}	            ifoundbetter = False
		   if (systemDesign_.numstatelist < SystemDesign.numsavedstatesmax)  {                             // {VB2J [1000]}	            If (numstatelist < numsavedstatesmax) Then
	// {VB2J [1001]}	               'room for new state, so add it to list:
// EN2.09: unused:			 ifoundbetter = true;                                               // {VB2J [1002]}	               ifoundbetter = True
																				// {VB2J [1003]}	               'iposbettersave = iposmaxsave
																				// {VB2J [1004]}	               'state.numfstatecallsave = state.numfstatecallsave + 1
			systemDesign_.numstatelist++;                                   // {VB2J [1005]}	               numstatelist = numstatelist + 1

																				// {VB2J [1007]}	               'iposx = state.numfstatecallsave
			 int iposx = systemDesign_.numstatelist;                                              // {VB2J [1008]}	               iposx = numstatelist

			 if (systemDesign_.darparun)  {                                                   // {VB2J [1010]}	               If (darparun) Then
			   for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {     // {VB2J [1011]}	                  For ifnjob = 1 To objfnjobset.numobjfn
				 int ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                             // {VB2J [1012]}	                     ifnx = objfnjobset.initfn(ifnjob)
				systemDesign_.agent[ifnx].savedhostid[iposx] = systemDesign_.agent[ifnx].testhostid;              // {VB2J [1013]}	                     agent(ifnx).savedhostid(iposx) = agent(ifnx).testhostid
				 }                                                              // {VB2J [1014]}	                  Next ifnjob
			   }                                                                // {VB2J [1015]}	               End If

			 for (int inod=1; inod<=systemDesign_.numnode; inod++) {                          // {VB2J [1017]}	               For inod = 1 To numnode
			   for (int ifnx=1; ifnx<=systemDesign_.numfunction; ifnx++) {                    // {VB2J [1018]}	                  For ifnx = 1 To numfunction
				systemDesign_.statelist[iposx].fstatecallsave[ifnx][inod] = systemDesign_.state.fstatecall[ifnx][inod];              // {VB2J [1019]}	                     statelist(iposx).fstatecallsave(ifnx, inod) = State.fstatecall(ifnx, inod)
				 }                                                              // {VB2J [1020]}	                  Next ifnx
			   }                                                                // {VB2J [1021]}	               Next inod


			systemDesign_.statelist[iposx].e = objx;                                         // {VB2J [1026]}	               statelist(iposx).e = objx
			systemDesign_.statelist[iposx].fixedcost = systemDesign_.fixedcost;                            // {VB2J [1027]}	               statelist(iposx).fixedcost = fixedcost
			systemDesign_.statelist[iposx].monthlycost = systemDesign_.monthlycost;                        // {VB2J [1028]}	               statelist(iposx).monthlycost = monthlycost
			systemDesign_.statelist[iposx].responsetime = systemDesign_.ftracetime;                        // {VB2J [1029]}	               statelist(iposx).responsetime = ftracetime
			systemDesign_.statelist[iposx].pfailure = 1 - systemDesign_.psuccess;                          // {VB2J [1030]}	               statelist(iposx).pfailure = 1 - psuccess
			systemDesign_.statelist[iposx].psuccess = systemDesign_.psuccess;                              // {VB2J [1031]}	               statelist(iposx).psuccess = psuccess

	// {VB2J [1033]}	               'messaging mods
	systemDesign_.statelist[iposx].remotetraffic = systemDesign_.remotetraffic;                    // {VB2J [1034]}	               statelist(iposx).remotetraffic = remotetraffic

	// {VB2J [1036]}	               'marc EN 1.1 smoothing
	systemDesign_.statelist[iposx].pfailuresurvive = systemDesign_.pfailuresurvive;                // {VB2J [1037]}	               statelist(iposx).pfailuresurvive = pfailuresurvive

	// {VB2J [1039]}	               'marc EN 1.1 hamming
	systemDesign_.statelist[iposx].hamming = systemDesign_.hamming;                                // {VB2J [1040]}	               statelist(iposx).hamming = hamming


	systemDesign_.statelist[iposx].fixedcostscale = systemDesign_.fixedcostscale;                  // {VB2J [1043]}	               statelist(iposx).fixedcostscale = fixedcostscale
	systemDesign_.statelist[iposx].monthlycostscale = systemDesign_.monthlycostscale;              // {VB2J [1044]}	               statelist(iposx).monthlycostscale = monthlycostscale
	systemDesign_.statelist[iposx].pfailurescale = systemDesign_.pfailurescale;                    // {VB2J [1045]}	               statelist(iposx).pfailurescale = pfailurescale
	systemDesign_.statelist[iposx].responsetimescale = systemDesign_.responsetimescale;              // {VB2J [1046]}	               statelist(iposx).responsetimescale = responsetimescale

	// {VB2J [1048]}	               'messaging mods
	systemDesign_.statelist[iposx].remotetrafficscale = systemDesign_.remotetrafficscale;              // {VB2J [1049]}	               statelist(iposx).remotetrafficscale = remotetrafficscale

	// {VB2J [1051]}	               'marc en 1.1 smoothing
	systemDesign_.statelist[iposx].pfailuresurvivescale = systemDesign_.pfailuresurvivescale;              // {VB2J [1052]}	               statelist(iposx).pfailuresurvivescale = pfailuresurvivescale

	// {VB2J [1054]}	               'marc en 1.1 hamming
	systemDesign_.statelist[iposx].hammingscale = systemDesign_.hammingscale;                      // {VB2J [1055]}	               statelist(iposx).hammingscale = hammingscale
 
	// EN2.09 - 'EN 2.0 load balancing
	systemDesign_.statelist[iposx].loadbalscale = systemDesign_.loadbalscale;

	
	systemDesign_.statelist[iposx].temp = systemDesign_.annealtemp;                                // {VB2J [1057]}	               statelist(iposx).temp = annealtemp

			 for (int inod=1; inod<=systemDesign_.numnode; inod++) {                          // {VB2J [1059]}	               For inod = 1 To numnode
				systemDesign_.statelist[iposx].nodeused[inod] = systemDesign_.nodeinfo[inod].nodeused;              // {VB2J [1060]}	                  statelist(iposx).nodeused(inod) = nodeinfo(inod).nodeused
			   }                                                                // {VB2J [1061]}	               Next inod

			 for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                       // {VB2J [1063]}	               For ilink = 1 To numlink
				systemDesign_.statelist[iposx].linkused[ilink] = systemDesign_.linkinfo[ilink].linkused;              // {VB2J [1064]}	                  statelist(iposx).linkused(ilink) = linkinfo(ilink).linkused
			   }                                                                // {VB2J [1065]}	               Next ilink
			 } else {                                                           // {VB2J [1066]}	            Else
	// {VB2J [1067]}	               'no equal states and list full, so check worst state
		 if (objx < objmax)  {                                              // {VB2J [1068]}	               If (objx < objmax) Then 'new best state at bottom of quality list
	// {VB2J [1069]}	                  'iposmaxsave is the position of the WORST previously stored state
			   int iposx = iposmaxsave;                                             // {VB2J [1070]}	                  iposx = iposmaxsave
// EN2.09: unused:			   ifoundbetter = true;                                             // {VB2J [1071]}	                  ifoundbetter = True

			   if (systemDesign_.darparun)  {                                                 // {VB2J [1073]}	                  If (darparun) Then
				 for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {   // {VB2J [1074]}	                     For ifnjob = 1 To objfnjobset.numobjfn
				   int ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                           // {VB2J [1075]}	                        ifnx = objfnjobset.initfn(ifnjob)
				   systemDesign_.agent[ifnx].savedhostid[iposx] = systemDesign_.agent[ifnx].testhostid;              // {VB2J [1076]}	                        agent(ifnx).savedhostid(iposx) = agent(ifnx).testhostid
				   }                                                            // {VB2J [1077]}	                     Next ifnjob
				 }                                                              // {VB2J [1078]}	                  End If

			   for (int inod=1; inod<=systemDesign_.numnode; inod++) {                        // {VB2J [1080]}	                  For inod = 1 To numnode
				 for (int ifnx=1; ifnx<=systemDesign_.numfunction; ifnx++) {                  // {VB2J [1081]}	                     For ifnx = 1 To numfunction
					systemDesign_.statelist[iposx].fstatecallsave[ifnx][inod] = systemDesign_.state.fstatecall[ifnx][inod];              // {VB2J [1082]}	                        statelist(iposx).fstatecallsave(ifnx, inod) = State.fstatecall(ifnx, inod)
				   }                                                            // {VB2J [1083]}	                     Next ifnx
				 }                                                              // {VB2J [1084]}	                  Next inod

	systemDesign_.statelist[iposx].e = objx;                                       // {VB2J [1087]}	                  statelist(iposx).e = objx
	systemDesign_.statelist[iposx].fixedcost = systemDesign_.fixedcost;                          // {VB2J [1088]}	                  statelist(iposx).fixedcost = fixedcost
	systemDesign_.statelist[iposx].monthlycost = systemDesign_.monthlycost;                      // {VB2J [1089]}	                  statelist(iposx).monthlycost = monthlycost
	systemDesign_.statelist[iposx].responsetime = systemDesign_.ftracetime;                      // {VB2J [1090]}	                  statelist(iposx).responsetime = ftracetime
	systemDesign_.statelist[iposx].pfailure = 1 - systemDesign_.psuccess;                        // {VB2J [1091]}	                  statelist(iposx).pfailure = 1 - psuccess
	systemDesign_.statelist[iposx].psuccess = systemDesign_.psuccess;                            // {VB2J [1092]}	                  statelist(iposx).psuccess = psuccess

	// {VB2J [1094]}	                  'messaging mods
	systemDesign_.statelist[iposx].remotetraffic = systemDesign_.remotetraffic;                  // {VB2J [1095]}	                  statelist(iposx).remotetraffic = remotetraffic

	// {VB2J [1097]}	                  'marc en 1.1 smoothing
	systemDesign_.statelist[iposx].pfailuresurvive = systemDesign_.pfailuresurvive;              // {VB2J [1098]}	                  statelist(iposx).pfailuresurvive = pfailuresurvive

	// {VB2J [1100]}	                  'marc en 1.1 hamming
	systemDesign_.statelist[iposx].hamming = systemDesign_.hamming;                              // {VB2J [1101]}	                  statelist(iposx).hamming = hamming

	// EN2.09 - 'EN 2.0 load balancing (VB line 4666)
	systemDesign_.statelist[iposx].loadbal = systemDesign_.loadbal;

	systemDesign_.statelist[iposx].fixedcostscale = systemDesign_.fixedcostscale;                // {VB2J [1104]}	                  statelist(iposx).fixedcostscale = fixedcostscale
	systemDesign_.statelist[iposx].monthlycostscale = systemDesign_.monthlycostscale;              // {VB2J [1105]}	                  statelist(iposx).monthlycostscale = monthlycostscale
	systemDesign_.statelist[iposx].pfailurescale = systemDesign_.pfailurescale;                  // {VB2J [1106]}	                  statelist(iposx).pfailurescale = pfailurescale
	systemDesign_.statelist[iposx].responsetimescale = systemDesign_.responsetimescale;              // {VB2J [1107]}	                  statelist(iposx).responsetimescale = responsetimescale

	// {VB2J [1109]}	                  'messaging mods
	systemDesign_.statelist[iposx].remotetrafficscale = systemDesign_.remotetrafficscale;              // {VB2J [1110]}	                  statelist(iposx).remotetrafficscale = remotetrafficscale

	// {VB2J [1112]}	                  'marc en 1.1 smoothing
	systemDesign_.statelist[iposx].pfailuresurvivescale = systemDesign_.pfailuresurvivescale;              // {VB2J [1113]}	                  statelist(iposx).pfailuresurvivescale = pfailuresurvivescale

	// {VB2J [1115]}	                  'marc en 1.1 hamming
	systemDesign_.statelist[iposx].hammingscale = systemDesign_.hammingscale;                    // {VB2J [1116]}	                  statelist(iposx).hammingscale = hammingscale

	// EN2.09 - 'EN 2.0 load balancing
	systemDesign_.statelist[iposx].loadbalscale = systemDesign_.loadbalscale;

	systemDesign_.statelist[iposx].temp = systemDesign_.annealtemp;                              // {VB2J [1119]}	                  statelist(iposx).temp = annealtemp

			   for (int inod=1; inod<=systemDesign_.numnode; inod++) {                        // {VB2J [1121]}	                  For inod = 1 To numnode
				systemDesign_.statelist[iposx].nodeused[inod] = systemDesign_.nodeinfo[inod].nodeused;              // {VB2J [1122]}	                     statelist(iposx).nodeused(inod) = nodeinfo(inod).nodeused
				 }                                                              // {VB2J [1123]}	                  Next inod

			   for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                     // {VB2J [1125]}	                  For ilink = 1 To numlink
				systemDesign_.statelist[iposx].linkused[ilink] = systemDesign_.linkinfo[ilink].linkused;              // {VB2J [1126]}	                     statelist(iposx).linkused(ilink) = linkinfo(ilink).linkused
				 }                                                              // {VB2J [1127]}	                  Next ilink

			   }                                                                // {VB2J [1130]}	               End If
			 }                                                                  // {VB2J [1131]}	            End If
		   }                                                                    // {VB2J [1132]}	         End If
		 }                                                                      // {VB2J [1133]}	      End If

	// {VB2J [1152]}	'updateannealstate:

	// {VB2J [1154]}	      'BUG: for numstatelist=0, then objx = statelist(0).e, so de=0, and no statechange possible

	float de = objx - systemDesign_.statelist[0].e;                                              // {VB2J [1157]}	      de = objx - statelist(0).e
	float t = systemDesign_.annealtemp;                                                          // {VB2J [1158]}	      t = annealtemp

	   boolean changeannealstate = false;                                               // {VB2J [1160]}	      changeannealstate = False
	   if (de > 0 && de < Float.MAX_VALUE /* 70000000 */ )  {                                          // {VB2J [1161]}	      If (de > 0 And de < 70000000#) Then  'added 8/12/01
		 float rndx = RnR.Rnd();                                                            // {VB2J [1162]}	         rndx = Rnd
		float exptest = 0;                                                          // {VB2J [1163]}	         exptest = 0#

//		   VBJ2_XLATE_FAILURE;                                                    // {VB2J [1164]}	         On Error GoTo 0

		 exptest = RnR.Exp(-de / t);                                                // {VB2J [1165]}	         exptest = Exp(-de / t)
		 if (exptest > rndx)  {                                                 // {VB2J [1166]}	         If (exptest > rndx) Then
		   changeannealstate = true;                                            // {VB2J [1167]}	            changeannealstate = True
		   } else {                                                             // {VB2J [1168]}	         Else
		   changeannealstate = false;                                           // {VB2J [1169]}	            changeannealstate = False
		   }                                                                    // {VB2J [1170]}	         End If

		 } else if (systemDesign_.numstatelist == 1)  {                                       // {VB2J [1172]}	      ElseIf (numstatelist = 1) Then
		 changeannealstate = true;                                              // {VB2J [1173]}	         changeannealstate = True

																				// {VB2J [1175]}	      'en 1.1 bugs: comment out following line, and add new random tests for de = 0
																				// {VB2J [1176]}	      'ElseIf (de = 0 Or de > 800000000#) Then 'new 8/12/01

		 } else if (de > Float.MAX_VALUE /* 800000000 */ )  {                                         // {VB2J [1178]}	      ElseIf (de > 800000000#) Then 'en 1.1 bugs
		 changeannealstate = false;                                             // {VB2J [1179]}	         changeannealstate = False
		 } else if (de == 0)  {                                                 // {VB2J [1180]}	      ElseIf (de = 0) Then  'en 1.1 bugs
		 if (RnR.Rnd() > 0.5)  {                                                      // {VB2J [1181]}	         If (Rnd > 0.5) Then 'en 1.1 bugs
		   changeannealstate = false;                                           // {VB2J [1182]}	            changeannealstate = False 'en 1.1 bugs
		   } else {                                                             // {VB2J [1183]}	         Else 'en 1.1 bugs
		   changeannealstate = true;                                            // {VB2J [1184]}	            changeannealstate = True 'en 1.1 bugs
		   }                                                                    // {VB2J [1185]}	         End If 'en 1.1 bugs
		 } else {                                                               // {VB2J [1186]}	      Else
		 changeannealstate = true;                                              // {VB2J [1187]}	         changeannealstate = True
		 }                                                                      // {VB2J [1188]}	      End If

	   if (changeannealstate)  {                                                // {VB2J [1190]}	      If (changeannealstate) Then

		systemDesign_.statelist[0].e = objx;                                                 // {VB2J [1192]}	         statelist(0).e = objx
		systemDesign_.statelist[0].fixedcost = systemDesign_.fixedcost;                                    // {VB2J [1193]}	         statelist(0).fixedcost = fixedcost
		systemDesign_.statelist[0].monthlycost = systemDesign_.monthlycost;                                // {VB2J [1194]}	         statelist(0).monthlycost = monthlycost
		systemDesign_.statelist[0].responsetime = systemDesign_.ftracetime;                                // {VB2J [1195]}	         statelist(0).responsetime = ftracetime

		systemDesign_.statelist[0].fixedcostscale = systemDesign_.fixedcostscale;                          // {VB2J [1197]}	         statelist(0).fixedcostscale = fixedcostscale
		systemDesign_.statelist[0].monthlycostscale = systemDesign_.monthlycostscale;                      // {VB2J [1198]}	         statelist(0).monthlycostscale = monthlycostscale
		systemDesign_.statelist[0].pfailurescale = systemDesign_.pfailurescale;                            // {VB2J [1199]}	         statelist(0).pfailurescale = pfailurescale
		systemDesign_.statelist[0].responsetimescale = systemDesign_.responsetimescale;                    // {VB2J [1200]}	         statelist(0).responsetimescale = responsetimescale

		systemDesign_.statelist[0].psuccess = systemDesign_.psuccess;                                      // {VB2J [1202]}	         statelist(0).psuccess = psuccess
		systemDesign_.statelist[0].pfailure = 1 - systemDesign_.psuccess;                                  // {VB2J [1203]}	         statelist(0).pfailure = 1 - psuccess

	// {VB2J [1205]}	         'messaging mods
		systemDesign_.statelist[0].remotetraffic = systemDesign_.remotetraffic;                            // {VB2J [1206]}	         statelist(0).remotetraffic = remotetraffic

	// {VB2J [1208]}	         'marc en 1.1 smoothing
		systemDesign_.statelist[0].pfailuresurvive = systemDesign_.pfailuresurvive;                        // {VB2J [1209]}	         statelist(0).pfailuresurvive = pfailuresurvive

	// {VB2J [1211]}	         'marc en 1.1 hamming
		systemDesign_.statelist[0].hamming = systemDesign_.hamming;                                        // {VB2J [1212]}	         statelist(0).hamming = hamming

     // EN2.09 -  'EN 2.0 load balancing
		systemDesign_.statelist[0].loadbal = systemDesign_.loadbal;

		systemDesign_.statelist[0].temp = systemDesign_.annealtemp;                                        // {VB2J [1216]}	         statelist(0).temp = annealtemp

		systemDesign_.statelist[0].numpath = systemDesign_.numpath;                                        // {VB2J [1218]}	         statelist(0).numpath = numpath
	// {VB2J [1219]}	         'state.numpathnode(0) = numpathnode

		 for (int ipath=1; ipath<=systemDesign_.statelist[0].numpath; ipath++) {              // {VB2J [1221]}	         For ipath = 1 To statelist(0).numpath
			systemDesign_.statelist[0].numpathnode[ipath] = systemDesign_.pathsave[ipath][0];                // {VB2J [1222]}	            statelist(0).numpathnode(ipath) = pathsave(ipath, 0)

			// 'en 2.04 statelistpath removal: delete the For ipathnod loop
//		for (int ipathnod=1; ipathnod<=systemDesign_.statelist[0].numpathnode[ipath]; ipathnod++) { // {VB2J [1223]}	            For ipathnod = 1 To statelist(0).numpathnode(ipath)
//																				// {VB2J [1224]}	               'marc EN 1.1 5/10/03 change statelist to statelistpath, report best solution mod
//			systemDesign_.statelistpath[0][ipath][ipathnod] = systemDesign_.pathsave[ipath][ipathnod];              // {VB2J [1225]}	               statelistpath(0, ipath, ipathnod) = pathsave(ipath, ipathnod)
//																				// {VB2J [1226]}	               'statelist(0).path(ipath, ipathnod) = pathsave(ipath, ipathnod)
//																				// {VB2J [1227]}	               'state.pathedge(0, ipathnod) = pathedge(ipathnod)
//			 }                                                                  // {VB2J [1228]}	            Next ipathnod

		 }                                                                    // {VB2J [1229]}	         Next ipath


		 if (systemDesign_.darparun)  {                                                       // {VB2J [1232]}	         If (darparun) Then
		   for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {         // {VB2J [1233]}	            For ifnjob = 1 To objfnjobset.numobjfn
			 int ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                                 // {VB2J [1234]}	               ifnx = objfnjobset.initfn(ifnjob)
			systemDesign_.agent[ifnx].annealhostid = systemDesign_.agent[ifnx].testhostid;                 // {VB2J [1235]}	               agent(ifnx).annealhostid = agent(ifnx).testhostid
			systemDesign_.agent[ifnx].savedhostid[0] = systemDesign_.agent[ifnx].testhostid;               // {VB2J [1236]}	               agent(ifnx).savedhostid(0) = agent(ifnx).testhostid
			 }                                                                  // {VB2J [1237]}	            Next ifnjob

		   for (int inod=1; inod<=systemDesign_.numnode; inod++) {                            // {VB2J [1239]}	            For inod = 1 To numnode
			systemDesign_.statelist[0].utiltot[inod] = systemDesign_.nodeinfo[inod].cpuutil + systemDesign_.nodeinfo[inod].cpubackground / 100;              // {VB2J [1240]}	               statelist(0).utiltot(inod) = nodeinfo(inod).cpuutil + nodeinfo(inod).cpubackground / 100
			systemDesign_.statelist[0].memtot[inod] = systemDesign_.nodeinfo[inod].memoryused;             // {VB2J [1241]}	               statelist(0).memtot(inod) = nodeinfo(inod).memoryused
			 }                                                                  // {VB2J [1242]}	            Next inod
		   }                                                                    // {VB2J [1243]}	         End If


		 for (int inod=1; inod<=systemDesign_.numnode; inod++) {                              // {VB2J [1246]}	         For inod = 1 To numnode
		   for (int ifnx=1; ifnx<=systemDesign_.numfunction; ifnx++) {                        // {VB2J [1247]}	            For ifnx = 1 To numfunction
			systemDesign_.statelist[0].fstatecallsave[ifnx][inod] = systemDesign_.state.fstatecall[ifnx][inod];              // {VB2J [1248]}	               statelist(0).fstatecallsave(ifnx, inod) = State.fstatecall(ifnx, inod)
			 }                                                                  // {VB2J [1249]}	            Next ifnx
		   }                                                                    // {VB2J [1250]}	         Next inod


																				// {VB2J [1253]}	         'determine probabilities:

																				// {VB2J [1255]}	         'associate the internodal path with a defined link, and set colors for data and data requests:
																				// {VB2J [1256]}	         'probx = nodeinfo(1).pfail

// rec - unused:		 int probx = 1;                                                            // {VB2J [1258]}	         probx = 1#

		 for (int inod=1; inod<=systemDesign_.numnode; inod++) {                              // {VB2J [1260]}	         For inod = 1 To numnode
			systemDesign_.nodeinfo[inod].nodeused = false;                                     // {VB2J [1261]}	            nodeinfo(inod).nodeused = False
		   }                                                                    // {VB2J [1262]}	         Next inod

		 for (int ipath=1; ipath<=systemDesign_.statelist[0].numpath; ipath++) {              // {VB2J [1264]}	         For ipath = 1 To statelist(0).numpath
			int pathnodx1 = systemDesign_.pathsave[ipath][1];                                      // {VB2J [1265]}	            pathnodx1 = pathsave(ipath, 1)
			systemDesign_.nodeinfo[pathnodx1].nodeused = true;                                 // {VB2J [1266]}	            nodeinfo(pathnodx1).nodeused = True

		   int numpathnode = systemDesign_.pathsave[ipath][0];                                    // {VB2J [1268]}	            numpathnode = pathsave(ipath, 0)
		   for (int inodpath=1; inodpath<=numpathnode-1; inodpath++) {          // {VB2J [1269]}	            For inodpath = 1 To numpathnode - 1
			 pathnodx1 = systemDesign_.pathsave[ipath][inodpath];                             // {VB2J [1270]}	               pathnodx1 = pathsave(ipath, inodpath)
			 int pathnodx2 = systemDesign_.pathsave[ipath][inodpath + 1];                         // {VB2J [1271]}	               pathnodx2 = pathsave(ipath, inodpath + 1)

			 int ilinksav = -1;                                                     // {VB2J [1273]}	               ilinksav = -1

			 for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                       // {VB2J [1275]}	               For ilink = 1 To numlink
			   int linknode1 = systemDesign_.linkinfo[ilink].node1;                               // {VB2J [1276]}	                  linknode1 = linkinfo(ilink).node1
			   int linknode2 = systemDesign_.linkinfo[ilink].node2;                               // {VB2J [1277]}	                  linknode2 = linkinfo(ilink).node2
			   if ((linknode1 == pathnodx2 && linknode2 == pathnodx1) 
			    || (linknode1 == pathnodx1 && linknode2 == pathnodx2))  { // {VB2J [1278]}	                  If ((linknode1 = pathnodx2 And linknode2 = pathnodx1) Or (linknode1 = pathnodx1 And linknode2 = pathnodx2)) Then
				 ilinksav = ilink;                                              // {VB2J [1279]}	                     ilinksav = ilink
				 break;                                                         // {VB2J [1280]}	                     Exit For
				 }                                                              // {VB2J [1281]}	                  End If
			   }                                                                // {VB2J [1282]}	               Next ilink

			systemDesign_.nodeinfo[pathnodx2].nodeused = true;                               // {VB2J [1284]}	               nodeinfo(pathnodx2).nodeused = True
			 systemDesign_.linkinfo[ilinksav].linkused = true;                                // {VB2J [1285]}	               linkinfo(ilinksav).linkused = True
			 }                                                                  // {VB2J [1286]}	            Next inodpath
		   }                                                                    // {VB2J [1287]}	         Next ipath

		 for (int inod=1; inod<=systemDesign_.numnode; inod++) {                              // {VB2J [1289]}	         For inod = 1 To numnode
			systemDesign_.statelist[0].nodeused[inod] = systemDesign_.nodeinfo[inod].nodeused;               // {VB2J [1290]}	            statelist(0).nodeused(inod) = nodeinfo(inod).nodeused
		   }                                                                    // {VB2J [1291]}	         Next inod

		 for (int ind=1; ind<=systemDesign_.requirednodes[0]; ind++) {                        // {VB2J [1293]}	         For ind = 1 To requirednodes(0)
		   int inod = systemDesign_.requirednodes[ind];                                           // {VB2J [1294]}	            inod = requirednodes(ind)
		   systemDesign_.statelist[0].nodeused[inod] = true;                                  // {VB2J [1295]}	            statelist(0).nodeused(inod) = True
		   }                                                                    // {VB2J [1296]}	         Next ind

		 for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                           // {VB2J [1298]}	         For ilink = 1 To numlink
			systemDesign_.statelist[0].linkused[ilink] = systemDesign_.linkinfo[ilink].linkused;             // {VB2J [1299]}	            statelist(0).linkused(ilink) = linkinfo(ilink).linkused
		   }                                                                    // {VB2J [1300]}	         Next ilink

		 }                                                                      // {VB2J [1302]}	      End If

	   
// 'en 2.05 cleanup: delete this entropy section
//	   
//	   if (systemDesign_.entropyrun)  {                                                       // {VB2J [1314]}	      If (entropyrun) Then
//																				// {VB2J [1315]}	         'en 2.x future: Rob, ignore this section
//																				// {VB2J [1316]}	         'abelianized shannon space:
//
//		systemDesign_.entropy.ientropyobjsamplelast++;     // {VB2J [1318]}	         entropy.ientropyobjsamplelast = entropy.ientropyobjsamplelast + 1
//
//		 if (systemDesign_.entropy.ientropyobjsamplelast >= SystemDesign.entropyobjsampledim)  {           // {VB2J [1320]}	         If (entropy.ientropyobjsamplelast >= entropyobjsampledim) Then
//			systemDesign_.entropy.entropydataready = true;                                     // {VB2J [1321]}	            entropy.entropydataready = True
//		   if (systemDesign_.entropy.ientropyobjsamplelast > SystemDesign.entropyobjsampledim)  {          // {VB2J [1322]}	            If (entropy.ientropyobjsamplelast > entropyobjsampledim) Then
//			systemDesign_.entropy.ientropyobjsamplelast = 1;                                 // {VB2J [1323]}	               entropy.ientropyobjsamplelast = 1
//			 }                                                                  // {VB2J [1324]}	            End If
//		   }                                                                    // {VB2J [1325]}	         End If
//
//		systemDesign_.entropy.objlist[systemDesign_.entropy.ientropyobjsamplelast] = systemDesign_.statelist[0].e;              // {VB2J [1327]}	         entropy.objlist(entropy.ientropyobjsamplelast) = statelist(0).e
//		systemDesign_.entropy.obj2list[systemDesign_.entropy.ientropyobjsamplelast] = systemDesign_.statelist[0].e * systemDesign_.statelist[0].e;              // {VB2J [1328]}	         entropy.obj2list(entropy.ientropyobjsamplelast) = statelist(0).e * statelist(0).e
//
//		 if (systemDesign_.entropy.entropydataready)  {                                       // {VB2J [1330]}	         If (entropy.entropydataready) Then
//		   float objsum = 0;                                                          // {VB2J [1331]}	            objsum = 0
//		   float obj2sum = 0;                                                         // {VB2J [1332]}	            obj2sum = 0
//		   for (int i=1; i<=SystemDesign.entropyobjsampledim; i++) {                         // {VB2J [1333]}	            For i = 1 To entropyobjsampledim
//			 objsum += systemDesign_.entropy.objlist[i];                              // {VB2J [1334]}	               objsum = objsum + entropy.objlist(i)
//			 obj2sum += systemDesign_.entropy.obj2list[i];                           // {VB2J [1335]}	               obj2sum = obj2sum + entropy.obj2list(i)
//			 }                                                                  // {VB2J [1336]}	            Next i
//
//			float objentropyave = objsum / SystemDesign.entropyobjsampledim;                        // {VB2J [1338]}	            objentropyave = objsum / entropyobjsampledim
//		   float obj2entropyave = obj2sum / SystemDesign.entropyobjsampledim;                      // {VB2J [1339]}	            obj2entropyave = obj2sum / entropyobjsampledim
//
//		   systemDesign_.entropy.numentropysamples++;           // {VB2J [1341]}	            entropy.numentropysamples = entropy.numentropysamples + 1
//
//		   systemDesign_.entropy.c[systemDesign_.entropy.numentropysamples] =
//		   	 (obj2entropyave - objentropyave * objentropyave) / (systemDesign_.annealtemp * systemDesign_.annealtemp);              // {VB2J [1343]}	            entropy.c(entropy.numentropysamples) = (obj2entropyave - objentropyave * objentropyave) / (annealtemp * annealtemp)
//		   systemDesign_.entropy.temp[systemDesign_.entropy.numentropysamples] = systemDesign_.annealtemp;                // {VB2J [1344]}	            entropy.temp(entropy.numentropysamples) = annealtemp
//		   }                                                                    // {VB2J [1345]}	         End If
//
//		 }                                                                      // {VB2J [1347]}	      End If    'end of entropy


//
//	// {VB2J [1357]}	      'en 2.x future: Rob, ignore this section. In general for 1.x, ignore anything with gene pool or entropy
//	// {VB2J [1358]}	      'update gene pool:
//	// {VB2J [1359]}	      '******************
//	   gensamplecount++;                                     // {VB2J [1360]}	      gensamplecount = gensamplecount + 1
//
//	   for (int ilink=1; ilink<=numlink; ilink++) {                             // {VB2J [1362]}	      For ilink = 1 To numlink
//		 if (statelist(0).linkused[ilink])  {                                   // {VB2J [1363]}	         If (statelist(0).linkused(ilink)) Then
//		   linkcount[ilink]++;                             // {VB2J [1364]}	           linkcount(ilink) = linkcount(ilink) + 1
//		   linkgenepool[ilink] = true;                                          // {VB2J [1365]}	           linkgenepool(ilink) = True
//		   linkgenepoolstep[ilink] = true;                                      // {VB2J [1366]}	           linkgenepoolstep(ilink) = True
//		   }                                                                    // {VB2J [1367]}	         End If
//		 }                                                                      // {VB2J [1368]}	      Next ilink
// 
//	// {VB2J [1369]}	      'END of genetic stuff !
//	// {VB2J [1370]}	      '**********************


	   }                                                                        // {VB2J [1372]}	   End If

// rec - removed GUI stuff
//
//
//	 if (drawincrement || (iterate == false))  {                          // {VB2J [1380]}	   If (drawincrement Or (iterateCheck.Value = 0)) Then
//
// ... removed totally!
//
//	   metrparmex.hammingannealText.Text = statelist[0].hamming;                // {VB2J [1575]}	      metrparmex.hammingannealText.Text = statelist(0).hamming   'marc en 1.1 hamming
//
//	   // DoEvents();                                                              // {VB2J [1577]}	      DoEvents
//	   }                                                                        // {VB2J [1578]}	   End If 'end of If (drawincrement Or (iterateCheck.Value = 0))


	// {VB2J [1584]}	'''''''''''''''''''''''''''''''''''''''''
	// {VB2J [1585]}	'''''''''''''''''''''''''''''''''''''''''
	// {VB2J [1586]}	'''''''''''''''''''''''''''''''''''''''''
	// {VB2J [1587]}	'''''''''''''''''''''''''''''''''''''''''
	// {VB2J [1588]}	'''''''''''''''''''''''''''''''''''''''''
	// {VB2J [1589]}	'''''''''''''''''''''''''''''''''''''''''

	// {VB2J [1592]}	   'generate a list of all function calls resulting from an initial call
	// {VB2J [1593]}	   'to the active function

// rec - pointless
//	 if ( ! systemDesign_.darparun)  {                                                        // {VB2J [1595]}	   If (Not darparun) Then
//	   }                                                                        // {VB2J [1596]}	   End If 'end of if (not darparun)

// rec - GUI & not DARPA - why bother? :)
//
//	 if ( ! systemDesign_.darparun)  {                                                        // {VB2J [1600]}	   If (Not darparun) Then
//	   if (drawincrement || (iterate == false))  {                        // {VB2J [1601]}	      If (drawincrement Or (iterateCheck.Value = 0)) Then
//		 failedcallsCombo.Clear();                                              // {VB2J [1602]}	         failedcallsCombo.Clear
//
//		 for (int icallevt=1; icallevt<=numfcallevent; icallevt++) {            // {VB2J [1604]}	         For icallevt = 1 To numfcallevent
//		   if (fcallevent(icallevt).status == 0)  {                             // {VB2J [1605]}	            If (fcallevent(icallevt).status = 0) Then
//			 VBJ2_XLATE_FAILURE;                                                // {VB2J [1606]}	               lenstr xnfunction(fcallevent(icallevt).srcfn).name, recfname, reclen
//			 VBJ2_XLATE_FAILURE;                                                // {VB2J [1607]}	               lenstr nodeinfo(fcallevent(icallevt).srcnod).name, recnodname, reclen
//			 rec = recnodname & "::" & recfname & " ==> ";                      // {VB2J [1608]}	               rec = recnodname & "::" & recfname & " ==> "
//																				// {VB2J [1609]}	               'rec = nodeinfo(fcallevent(icallevt).srcnod).name & "::" & xnfunction(fcallevent(icallevt).srcfn).name & "==>"
//
//			 VBJ2_XLATE_FAILURE;                                                // {VB2J [1611]}	               lenstr xnfunction(fcallevent(icallevt).targfn).name, recfname, reclen
//			 VBJ2_XLATE_FAILURE;                                                // {VB2J [1612]}	               lenstr nodeinfo(fcallevent(icallevt).targnod).name, recnodname, reclen
//			 rec = rec & recnodname & "::" & recfname;                          // {VB2J [1613]}	               rec = rec & recnodname & "::" & recfname
//																				// {VB2J [1614]}	               'rec = rec & nodeinfo(fcallevent(icallevt).targnod).name & "::" & xnfunction(fcallevent(icallevt).targfn).name
//			 VBJ2_XLATE_FAILURE;                                                // {VB2J [1615]}	               failedcallsCombo.AddItem rec
//			 }                                                                  // {VB2J [1616]}	            End If
//		   }                                                                    // {VB2J [1617]}	         Next icallevt
//
//		 if (failedcallsCombo.ListCount > 0)  {                                 // {VB2J [1619]}	         If (failedcallsCombo.ListCount > 0) Then
//		   failedcallsCombo.ListIndex = 0;                                      // {VB2J [1620]}	            failedcallsCombo.ListIndex = 0
//		   }                                                                    // {VB2J [1621]}	         End If
//		 }                                                                      // {VB2J [1622]}	      End If
//	   }                                                                        // {VB2J [1623]}	   End If
//

							// rec - uh?
							// {VB2J [1625]}	'the junk(i) array is a list of ALL functions spawned by the original call to activetracefunction
							// {VB2J [1626]}	'Every function in this array must be defined on AT LEAST one node


// rec - GUI
//	 if (drawincrement || (iterate == false))  {                          // {VB2J [1629]}	   If (drawincrement Or (iterateCheck.Value = 0)) Then
//	   drawincrement = false;                                                   // {VB2J [1630]}	      drawincrement = False
//	   // DoEvents();                                                              // {VB2J [1631]}	      DoEvents
//	   }                                                                        // {VB2J [1632]}	   End If


	// {VB2J [1639]}	'''''''''''''''''''''''''''''''''''''''''
	// {VB2J [1640]}	'''''''''''''''''''''''''''''''''''''''''
	// {VB2J [1641]}	'''''''''''''''''''''''''''''''''''''''''
	// {VB2J [1642]}	'''''''''''''''''''''''''''''''''''''''''
	// {VB2J [1643]}	'''''''''''''''''''''''''''''''''''''''''
	// {VB2J [1644]}	'''''''''''''''''''''''''''''''''''''''''


// rec - re-worked goto
//
//	 if (iterate == true)  {                                            // {VB2J [1648]}	   If (iterateCheck.Value = 1) Then
//
//// rec - GUI																// {VB2J [1649]}	      'new 4/4/03
////	   for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                // {VB2J [1650]}	      For inod = 1 To numnode
////		 systemdesign_.node(inod).ZOrder();                                      // {VB2J [1651]}	         systemdesign.node(inod).ZOrder
////		 }                                                                      // {VB2J [1652]}	      Next inod
//
//	   // VBJ2_XLATE_FAILURE; //F GOTO                                             // {VB2J [1655]}	      GoTo startanneal
//      
//	   }                                                                        // {VB2J [1656]}	   End If

	if (iterate == false) {
		break startanneal;
		}
} // end while(true) startanneal loop


	systemDesign_.iteranel = false;                                                          // {VB2J [1658]}	   iteranel = False

	iterations_ = iterk*1000  + iter;	// update this before return - 19sept04

}    // annealingStep                                                                    // {VB2J [1661]}	End Sub


/**
 * Stop the threaded timer.
 */
public boolean
stopThreads() {

    if (metroParams_ != null) {
        metroParams_.stopTimer();
    	}
    return true;
	}


/**
 * Our own iteration counter, for advertising purposes.
 */
public long
getIterationCount() {
    return longIter_;
	}


/**
	// 'marc en 1.1 soft constraints
 * 
 * rec - set output values via globals, not side-effect
**/
public void
findSystemPower() {
//		float totcpuavail, float totcpurequired, float totcpurequiredmin, float totcpurequired25,
//		float totcpurequired50, float totcpurequired75, float totcpurequired100, float totmemoryavail, 
//		float totmemoryrequired, float totmemoryrequiredmin, float totmemoryrequired25, float totmemoryrequired50,
//		float totmemoryrequired75, float totmemoryrequired100) {


boolean nodeused[] = new boolean[SystemDesign.numnodemax+1];
int 	numlistlong;	// int suffices
float 	cpulist[] = new float[SystemDesign.numnodemax+1];
int		cpulistid[] = new int[SystemDesign.numnodemax+1];	// rec - was long, using int for index to arrays
float 	memlist[] = new float[SystemDesign.numnodemax+1];
int 	memlistid[] = new int[SystemDesign.numnodemax+1];	// rec - was long, using int for index to arrays

// 'loop over all functions, and compute their required mean cpu and memory
	
	fsp_totcpuavail = 0;
	float totcpuburn = 0;
	float totcpuburnmin = 0;
	fsp_totcpurequiredmin = 0;
	fsp_totcpurequired25 = 0;
	fsp_totcpurequired50 = 0;
	fsp_totcpurequired75 = 0;
	fsp_totcpurequired100 = 0;
	
	fsp_totmemoryavail = 0;
	float totmemoryburn = 0;
	float totmemoryburnmin = 0;
	fsp_totmemoryrequiredmin = 0;
	fsp_totmemoryrequired25 = 0;
	fsp_totmemoryrequired50 = 0;
	fsp_totmemoryrequired75 = 0;
	fsp_totmemoryrequired100 = 0;

	float cpuaveburnrate = 0;	// rec
	float memoryaveburnrate = 0;
	int numcpuburn = 0;
	float memorymin = 0;
	float cpumin = 0;

	for (int inod=1; inod<=systemDesign_.numnode; inod++) {
		nodeused[inod] = false;
		}

	for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {
		if (systemDesign_.xnfunction[ifn].objfn) {
		  cpuaveburnrate = 0;
		  memoryaveburnrate = 0;
		  numcpuburn = 0;
		  memorymin = Float.MAX_VALUE; // 9E+19f;
		  cpumin = Float.MAX_VALUE; // 9E+19f;

	  for (int inod=1; inod<=systemDesign_.numnode; inod++) {

		if (systemDesign_.xnfunction[ifn].felig[inod] == 1) {
			cpuaveburnrate    += systemDesign_.xnfunction[ifn].cpurate[inod];
			memoryaveburnrate += systemDesign_.xnfunction[ifn].memory[inod];
			numcpuburn++;
			nodeused[inod] = true;
			if (systemDesign_.xnfunction[ifn].memory[inod] < memorymin) {
			   memorymin = systemDesign_.xnfunction[ifn].memory[inod];
		 		} // End If
			if (systemDesign_.xnfunction[ifn].cpurate[inod] < cpumin) {
			   cpumin = systemDesign_.xnfunction[ifn].cpurate[inod];
		 		} // End If
            
			cpulistid[numcpuburn] = inod;
			cpulist[numcpuburn] = systemDesign_.xnfunction[ifn].cpurate[inod];
			memlistid[numcpuburn] = inod;
			memlist[numcpuburn] = systemDesign_.xnfunction[ifn].memory[inod];
			} //  End If

		} // Next inod
      
		cpuaveburnrate /= numcpuburn;
		memoryaveburnrate /= numcpuburn;
		totcpuburn += cpuaveburnrate;
		totmemoryburn += memoryaveburnrate;
		
		totcpuburnmin += cpumin;
		totmemoryburnmin += memorymin;
	
	// 'links are sorted from lowest cpu or memory to highest
		numlistlong = numcpuburn;
	
		quickSort_single_index(cpulist, cpulistid, 1, numlistlong);
		quickSort_single_index(memlist, memlistid, 1, numlistlong);
	
		int ipct25  = (int)(0.25 * numcpuburn + 1);
		int ipct50  = (int)(0.50 * numcpuburn + 1);
		int ipct75  = (int)(0.75 * numcpuburn + 1);
		int ipct100 = numcpuburn;
		int ind25  = cpulistid[ipct25];
		int ind50  = cpulistid[ipct50];
		int ind75  = cpulistid[ipct75];
		int ind100 = cpulistid[ipct100];
		
		fsp_totcpurequired25  += systemDesign_.xnfunction[ifn].cpurate[ind25];
		fsp_totcpurequired50  += systemDesign_.xnfunction[ifn].cpurate[ind50];
		fsp_totcpurequired75  += systemDesign_.xnfunction[ifn].cpurate[ind75];
		fsp_totcpurequired100 += systemDesign_.xnfunction[ifn].cpurate[ind100];
	 
		ind25  = memlistid[ipct25];
		ind50  = memlistid[ipct50];
		ind75  = memlistid[ipct75];
		ind100 = memlistid[ipct100];
		fsp_totmemoryrequired25  += systemDesign_.xnfunction[ifn].memory[ind25];
		fsp_totmemoryrequired50  += systemDesign_.xnfunction[ifn].memory[ind50];
		fsp_totmemoryrequired75  += systemDesign_.xnfunction[ifn].memory[ind75];
		fsp_totmemoryrequired100 += systemDesign_.xnfunction[ifn].memory[ind100];
		} // End If
	} // Next ifn
	
// EN2.09: unused:	fsp_totcpurequired = totcpuburn;
	fsp_totcpurequiredmin = totcpuburnmin;
	
// EN2.09: unused:	fsp_totmemoryrequired = totmemoryburn;
	fsp_totmemoryrequiredmin = totmemoryburnmin;

	for (int inod = 1; inod <= systemDesign_.numnode; inod++) {
		if (nodeused[inod] && systemDesign_.nodeinfo[inod].enabled) {
			fsp_totcpuavail    += 1.0 - systemDesign_.nodeinfo[inod].cpubackground / 100.0;
			fsp_totmemoryavail += systemDesign_.nodeinfo[inod].memory;
			}//  End If
		} // Next inod

	} // End Sub - findSystemPower



/**
 * 
	Port of
		Public Sub QuickSort_single_index(
					ByRef sortarray() As Single, 
					ByRef sortarray_index() As Long, 
					ByVal First As Long, 
					ByVal Last As Long)

	For Java, use int instead of Long (for array indices)

	rec: here's some test code:

		// VB doesn't use zeroth element
		float data[] = new float[] {-1f, 1.0f, 3.0f, 2.0f, 5.0f, 4.0f}; // not in order
		int indices[] = new int[] {-1, 1, 2, 3, 4, 5};
		quickSort_single_index(data, indices, 1, 5);
		System.out.println(" test of QuickSort_single_index");	
		System.out.println(" first field should be 1,3,2,5,4; second field should be in order");	
		for (int i=1; i<=5; i++)
			System.out.println("sorted: " + indices[i] + "=" + data[i]);	


 * @param sortarray			data values to be sorted
 * @param sortarray_index	original indices, so we know what came from where
 * @param first				starting index to sort
 * @param last				ending index to sort
 * 
 * @version 1.1
 */
private void
quickSort_single_index(float[] sortarray, int[] sortarray_index, int first, int last) {

float 	temp;
int 	temp_index;
float 	list_Separator;
int 	low;
int 	high;
  
	low = first;
	high = last;
	list_Separator = sortarray[(first + last) / 2];

	while (true) {

		while (sortarray[low] < list_Separator) {
			low++;
			}

		while (sortarray[high] > list_Separator) {
			high--;
			}

		if (low <= high) {
			temp = sortarray[low];
			temp_index = sortarray_index[low];
			sortarray[low] = sortarray[high];
			sortarray_index[low] = sortarray_index[high];
			sortarray[high] = temp;
			sortarray_index[high] = temp_index;
			low++;
			high--;
			}
		
		if (low > high)
			break;

		} // while true

	if (first < high) {
		quickSort_single_index(sortarray, sortarray_index, first, high);
		}

	if (low < last) {
		quickSort_single_index(sortarray, sortarray_index, low, last);
		}

	} // QuickSort_single_index


/**
 * 	Port of QuickSort_single_twoindex
 * 
 * @param sortarray
 * @param sortarray_index
 * @param sortarray_index2
 * @param first
 * @param last
 */
private void
quickSort_single_twoindex(float[] sortarray, int[] sortarray_index, int[] sortarray_index2, int first, int last) {

float 	temp;
int 	temp_index, temp_index2;
float 	list_Separator;
int 	low;
int 	high;
  
	low = first;
	high = last;
	list_Separator = sortarray[(first + last) / 2];

	while (true) {

		while (sortarray[low] < list_Separator) {
			low++;
			}

		while (sortarray[high] > list_Separator) {
			high--;
			}

		if (low <= high) {
			temp = sortarray[low];
			temp_index = sortarray_index[low];
			temp_index2 = sortarray_index2[low];
			sortarray[low] = sortarray[high];
			sortarray_index[low] = sortarray_index[high];
			sortarray_index2[low] = sortarray_index2[high];
			sortarray[high] = temp;
			sortarray_index[high] = temp_index;
			sortarray_index2[high] = temp_index2;
			low++;
			high--;
			}
		
		if (low > high)
			break;

		} // while true

if (first < high) {
	quickSort_single_index(sortarray, sortarray_index, first, high);
	}

if (low < last) {
	quickSort_single_index(sortarray, sortarray_index, low, last);
	}

} // QuickSort_single_index



/**
 * Completely re-done for 1.1
 * 
 * @version 17Mar04 - Trying to prevent the NPE
 * @version 1.1
 */
public void
getRandomState() {             // {VB2J [6]}	Sub getrandomstate()

	// these little ints are used all over the place, so define 'em here...
	boolean foundinod = false, foundnewnode = false, agentmoved = false;
	int inodsave=1, ifn=0, ifx=0, ifnx=0, inod=0, inod1=0, inod2=0, inodx=0, 
	    inodnew=0, inodnew2=0, inodrunx=0, inodnewsave=0;
	int ifnx1=0, ifnx2=0;
	int ifn1=0, ifn2=0, indx2=0;


//logDebug("  AnnealForm.getRandomState() 1 ");
//logDebug("  AnnealForm.getRandomState() systemDesign_.darparun " + systemDesign_.darparun);

// zero-out rather than re-allocate
// 	  int threadcall[] = new int[SystemDesign.numfunctionmax+1];                                // {VB2J [7]}	   Dim threadcall(numfunctionmax) As Integer
	for (int i=1; i<=SystemDesign.numfunctionmax; i++) {
// EN2.09: unused: threadcall[i] = 0;
//		agentlisttemp[i] = 0;	// see below
	    totfncall[i] = 0f;	// EN2.20
	    }

	  boolean checkmobility = false;   // ???                                                    // {VB2J [8]}	   Dim checkmobility  As Boolean
//
// simply zero these out, rather than re-creating; it's a hair faster
//

//int activenodes[] = new int[SystemDesign.numnodemax+1];                                   // {VB2J [10]}	   Dim activenodes(numnodemax) As Integer
//int inactivenodes[] = new int[SystemDesign.numnodemax+1];                                 // {VB2J [11]}	   Dim inactivenodes(numnodemax) As Integer
//double nodeutiltemp[] = new double[SystemDesign.numnodemax+1];                            // {VB2J [15]}	   Dim nodeutiltemp(numnodemax) As Single
//double nodememtemp[] = new double[SystemDesign.numnodemax+1];                             // {VB2J [16]}	   Dim nodememtemp(numnodemax) As Single
//double nodeutiltemp2[] = new double[SystemDesign.numnodemax+1];                           // {VB2J [17]}	   Dim nodeutiltemp2(numnodemax) As Single  'en 1.1 speedup
//double nodememtemp2[] = new double[SystemDesign.numnodemax+1];                            // {VB2J [18]}	   Dim nodememtemp2(numnodemax) As Single  'en 1.1 speedup
//
//  int nodelisttemp[] = new int[SystemDesign.numnodemax+1];                                  // {VB2J [24]}	   Dim nodelisttemp(numnodemax) As Integer
	for (int i=1; i<=SystemDesign.numnodemax; i++) {
//	  EN2.09: unused: 		activenodes[i] = 0;
//	  EN2.09: unused: 		inactivenodes[i] = 0;
		nodeutiltemp[i] = 0;
		nodememtemp[i] = 0;
		nodeutiltemp2[i] = 0;
		nodememtemp2[i] = 0;
		nodelisttemp[i] = 0;
		}


	  int numactivenodes;                                                          // {VB2J [12]}	   Dim numactivenodes As Integer
// EN2.09: unused:	  int numinactivenodes;                                                        // {VB2J [13]}	   Dim numinactivenodes As Integer

// EN2.09 - removed: just init, above
//	  int agentlisttemp[] = new int[SystemDesign.numfunctionmax+1];                             // {VB2J [20]}	   Dim agentlisttemp(numfunctionmax) As Integer

  // 'en 2.01 load balancing cleanup. Delete this line
  // int numagentlisttemp;                                                        // {VB2J [22]}	   Dim numagentlisttemp As Integer


// {VB2J [26]}	   'en 1.1 general:
	int highrisknodes[] = new int[SystemDesign.numnodemax+1];                                 // {VB2J [27]}	   Dim highrisknodes(numnodemax) As Integer
	int numlistlong;      // int suffices                                                    // {VB2J [28]}	   Dim numlistlong As Long

	// EN2.21 - 16Sept04 - from "numnodemax" to "numnodemaxchoose2" - 3 occurances
	float pfailuresurvivelist[] = new float[SystemDesign.numnodemaxchoose2+1];                     // {VB2J [29]}	   Dim pfailuresurvivelist(numnodemax) As Single
	int pfailuresurvivelistid[] = new int[SystemDesign.numnodemaxchoose2+1];                                                          // {VB2J [30]}	   Dim pfailuresurvivelistid(numnodemax) As Long

	//  'en 2.0 load balancing
	int pfailuresurvivelistid2[] = new int[SystemDesign.numnodemaxchoose2+1];

// EN2.09: unused:	float pfailuresurvivelisttemp[] = new float[SystemDesign.numnodemax+1];                 // {VB2J [31]}	   Dim pfailuresurvivelisttemp(numnodemax) As Single
	float pfailuresurvivex;float pfailuresurvivetmpmax;                        // {VB2J [32]}	   Dim pfailuresurvivex As Single, pfailuresurvivetmpmax As Single
	int usednodestemp[] = new int[SystemDesign.numnodemax+1];                                 // {VB2J [33]}	   Dim usednodestemp(numnodemax) As Integer
	int unusednodestemp[] = new int[SystemDesign.numnodemax+1];                               // {VB2J [34]}	   Dim unusednodestemp(numnodemax) As Integer

// {VB2J [36]}	   'en 1.1 smoothing/messaging
	float totmessageifn[] = new float[SystemDesign.numnodemax+1];                           // {VB2J [37]}	   Dim totmessageifn(numnodemax) As Single
	int totmessageifnid[] = new int[SystemDesign.numnodemax+1];                         // {VB2J [38]}	   Dim totmessageifnid(numnodemax) As Long

// {VB2J [40]}	   'en 1.1 hamming
	int objfnjobsetlist[] = new int[SystemDesign.numfunctionmax+1];							   // {VB2J [41]}	   Dim objfnjobsetlist(numfunctionmax)
boolean objfnjobsetlistHasBeenInited = false;	// rec - aug'03 bugfix

// {VB2J [44]}	   'for each node, loop over all functions in requiredfunctions(*), and for each possible function
// {VB2J [45]}	   'call to requiredfunctions(ifnx) from node inod, pick a random node for execution of the function ifnx
// {VB2J [47]}	   'there is potential speedup here, since some nodes (like routers), will make NO function calls


	  if (systemDesign_.darparun)  {                                                             // {VB2J [49]}	   If (darparun) Then

		checkmobility = false;                                                     // {VB2J [51]}	      checkmobility = False

		if (systemDesign_.numstatelist > 0)  {                                                   // {VB2J [53]}	      If (numstatelist > 0) Then
		  for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {             // {VB2J [54]}	         For ifnjob = 1 To objfnjobset.numobjfn
			ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                     				   // {VB2J [55]}	            ifnx = objfnjobset.initfn(ifnjob)
			systemDesign_.agent[ifnx].testhostid = systemDesign_.agent[ifnx].annealhostid;				       // {VB2J [56]}	            agent(ifnx).testhostid = agent(ifnx).annealhostid
			}                                                                      // {VB2J [57]}	         Next ifnjob
		  }                                                                        // {VB2J [58]}	      End If


   // {VB2J [61]}	      'en 1.1 general mods:
   // {VB2J [62]}	      'find current loading of servers:
		for (inodx=1; inodx<=systemDesign_.numnode; inodx++) {                               // {VB2J [63]}	      For inodx = 1 To numnode
		  nodeutiltemp[inodx] = 0; 				                                   // {VB2J [64]}	         nodeutiltemp(inodx) = 0
		  nodememtemp[inodx] = 0;				                                   // {VB2J [65]}	         nodememtemp(inodx) = 0
		  }                                                                        // {VB2J [66]}	      Next inodx

		for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {               // {VB2J [68]}	      For ifnjob = 1 To objfnjobset.numobjfn
		  ifnx = systemDesign_.objfnjobset.initfn[ifnjob]; 	                        		   // {VB2J [69]}	         ifnx = objfnjobset.initfn(ifnjob)
		  inodx = systemDesign_.agent[ifnx].testhostid; 			                               // {VB2J [70]}	         inodx = agent(ifnx).testhostid
		  if (systemDesign_.nodeinfo[inodx].type == 1)  {                                        // {VB2J [71]}	         If (nodeinfo(inodx).type = 1) Then
			nodeutiltemp[inodx] = nodeutiltemp[inodx] + systemDesign_.xnfunction[ifnx].cpurate[inodx] / systemDesign_.nodeinfo[inodx].cpucount;  // {VB2J [72]}	            nodeutiltemp(inodx) = nodeutiltemp(inodx) + xnfunction(ifnx).cpurate(inodx) / nodeinfo(inodx).cpucount
			nodememtemp[inodx] = nodememtemp[inodx] + systemDesign_.xnfunction[ifnx].memory[inodx]; // {VB2J [73]}	            nodememtemp(inodx) = nodememtemp(inodx) + xnfunction(ifnx).memory(inodx)
			}                                                                      // {VB2J [74]}	         End If
		  }                                                                        // {VB2J [75]}	      Next ifnjob

// {VB2J [77]}	      'find nodeinfo(inod).numagents:
		for (inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [78]}	      For inod = 1 To numnode
			systemDesign_.nodeinfo[inod].numagents = 0;                                            // {VB2J [79]}	         nodeinfo(inod).numagents = 0
		  pfailuresurvivelist[inod] = 0;                                           // {VB2J [80]}	         pfailuresurvivelist(inod) = 0
		  }                                                                        // {VB2J [81]}	      Next inod

		for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {               // {VB2J [83]}	      For ifnjob = 1 To objfnjobset.numobjfn
		  ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                                       // {VB2J [84]}	         ifnx = objfnjobset.initfn(ifnjob)
		  inod = systemDesign_.agent[ifnx].testhostid;                                           // {VB2J [85]}	         inod = agent(ifnx).testhostid
		  systemDesign_.nodeinfo[inod].numagents = systemDesign_.nodeinfo[inod].numagents + 1;                 // {VB2J [86]}	         nodeinfo(inod).numagents = nodeinfo(inod).numagents + 1
		  systemDesign_.nodeinfo[inod].agentlist[systemDesign_.nodeinfo[inod].numagents] = ifnx;               // {VB2J [87]}	         nodeinfo(inod).agentlist(nodeinfo(inod).numagents) = ifnx
		  }                                                                        // {VB2J [88]}	      Next ifnjob

		int numusednodestemp = 0;                                                      // {VB2J [91]}	      numusednodestemp = 0
		int numunusednodestemp = 0;                                                    // {VB2J [92]}	      numunusednodestemp = 0

		for (inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [94]}	      For inod = 1 To numnode
		  if (systemDesign_.nodeinfo[inod].numagents > 0)  {                                     // {VB2J [95]}	         If (nodeinfo(inod).numagents > 0) Then
			numusednodestemp++;                               // {VB2J [96]}	            numusednodestemp = numusednodestemp + 1
			usednodestemp[numusednodestemp] = inod;                                // {VB2J [97]}	            usednodestemp(numusednodestemp) = inod
			} else {                                                               // {VB2J [98]}	         Else
			if (systemDesign_.nodeinfo[inod].type == 1 && systemDesign_.nodeinfo[inod].enabled)  {              // {VB2J [99]}	            If (nodeinfo(inod).type = 1 And nodeinfo(inod).enabled) Then
			  numunusednodestemp++;                         // {VB2J [100]}	               numunusednodestemp = numunusednodestemp + 1
			  unusednodestemp[numunusednodestemp] = inod;                          // {VB2J [101]}	               unusednodestemp(numunusednodestemp) = inod
			  }                                                                    // {VB2J [102]}	            End If
			}                                                                      // {VB2J [103]}	         End If
		  }                                                                        // {VB2J [104]}	      Next inod

		usednodestemp[0] = numusednodestemp;                                       // {VB2J [106]}	      usednodestemp(0) = numusednodestemp
		unusednodestemp[0] = numunusednodestemp;                                   // {VB2J [107]}	      unusednodestemp(0) = numunusednodestemp

		for (inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [109]}	      For inod = 1 To numnode
		  pfailuresurvivelist[inod] = systemDesign_.nodeinfo[inod].pfail * systemDesign_.nodeinfo[inod].numagents;              // {VB2J [110]}	         pfailuresurvivelist(inod) = nodeinfo(inod).pfail * nodeinfo(inod).numagents
		  pfailuresurvivelistid[inod] = inod;                                      // {VB2J [111]}	         pfailuresurvivelistid(inod) = inod
		  }                                                                        // {VB2J [112]}	      Next inod

 // {VB2J [114]}	      'randomize the agent lists

		for (inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [116]}	      For inod = 1 To numnode
		  if (systemDesign_.nodeinfo[inod].numagents == 2 && RnR.Rnd() > 0.5)  {                        // {VB2J [117]}	         If (nodeinfo(inod).numagents = 2 And Rnd > 0.5) Then
			int xnfunctiontmp = systemDesign_.nodeinfo[inod].agentlist[1];                           // {VB2J [118]}	            xnfunctiontmp = nodeinfo(inod).agentlist(1)
			systemDesign_.nodeinfo[inod].agentlist[1] = xnfunctiontmp;                           // {VB2J [120]}	            nodeinfo(inod).agentlist(2) = xnfunctiontmp
			} else if (systemDesign_.nodeinfo[inod].numagents > 2)  {                            // {VB2J [121]}	         ElseIf (nodeinfo(inod).numagents > 2) Then
			

			for (int i=1; i<=systemDesign_.nodeinfo[inod].numagents; i++) {                      // {VB2J [122]}	            For i = 1 To nodeinfo(inod).numagents
																				   // {VB2J [123]}	               'pick two agents and flip the order in the list
			  ifnx1 = RnR.Int(systemDesign_.nodeinfo[inod].numagents * RnR.Rnd() + 1);                     // {VB2J [124]}	               ifnx1 = Int(nodeinfo(inod).numagents * Rnd + 1)
			  ifnx2 = ifnx1;                                                       // {VB2J [125]}	               ifnx2 = ifnx1

			  while (ifnx2 == ifnx1) {  										         // {VB2J [127]}	               Do While (ifnx2 = ifnx1)
			  	ifnx2 = RnR.Int(systemDesign_.nodeinfo[inod].numagents * RnR.Rnd() + 1);                   // {VB2J [128]}	                  ifnx2 = Int(nodeinfo(inod).numagents * Rnd + 1)
			  }                                                                    // {VB2J [129]}	               Loop

			ifn1 = systemDesign_.nodeinfo[inod].agentlist[ifnx1];                                // {VB2J [131]}	               ifn1 = nodeinfo(inod).agentlist(ifnx1)
			ifn2 = systemDesign_.nodeinfo[inod].agentlist[ifnx2];                                // {VB2J [132]}	               ifn2 = nodeinfo(inod).agentlist(ifnx2)

			systemDesign_.nodeinfo[inod].agentlist[ifnx1] = ifn2;                                // {VB2J [134]}	               nodeinfo(inod).agentlist(ifnx1) = ifn2
			systemDesign_.nodeinfo[inod].agentlist[ifnx2] = ifn1;                                // {VB2J [135]}	               nodeinfo(inod).agentlist(ifnx2) = ifn1
			}                                                                      // {VB2J [136]}	            Next i
		  }                                                                        // {VB2J [137]}	         End If
		}                                                                          // {VB2J [138]}	      Next inod


//	logDebug("  AnnealForm.getRandomState() 2 ");

// {VB2J [140]}	      'randomize the node lists:

	  if (usednodestemp[0] == 2 && RnR.Rnd() > 0.5)  {                                    // {VB2J [142]}	      If (usednodestemp(0) = 2 And Rnd > 0.5) Then
		int xnodtemp = usednodestemp[1];                                               // {VB2J [143]}	         xnodtemp = usednodestemp(1)
		usednodestemp[1] = usednodestemp[2];                                       // {VB2J [144]}	         usednodestemp(1) = usednodestemp(2)
		usednodestemp[2] = xnodtemp;                                               // {VB2J [145]}	         usednodestemp(2) = xnodtemp
		} else if (usednodestemp[0] > 2)  {                                        // {VB2J [146]}	      ElseIf (usednodestemp(0) > 2) Then
		for (int i=1; i<=usednodestemp[0]; i++) {                                  // {VB2J [147]}	         For i = 1 To usednodestemp(0)
		  int ind1 = RnR.Int(usednodestemp[0] * RnR.Rnd() + 1);                                  // {VB2J [148]}	            ind1 = Int(usednodestemp(0) * Rnd + 1)
		  int ind2 = ind1;                                                             // {VB2J [149]}	            ind2 = ind1

		  while (ind2 == ind1) { // Wrong number of tokens for 'Do'.                  // {VB2J [151]}	            Do While (ind2 = ind1)
			  ind2 = RnR.Int(usednodestemp[0] * RnR.Rnd() + 1);                                  // {VB2J [152]}	               ind2 = Int(usednodestemp(0) * Rnd + 1)
			  }                                                                        // {VB2J [153]}	            Loop

		inod1 = usednodestemp[ind1];                                               // {VB2J [155]}	            inod1 = usednodestemp(ind1)
		inod2 = usednodestemp[ind2];                                               // {VB2J [156]}	            inod2 = usednodestemp(ind2)

		usednodestemp[ind1] = inod2;                                               // {VB2J [158]}	            usednodestemp(ind1) = inod2
		usednodestemp[ind2] = inod1;                                               // {VB2J [159]}	            usednodestemp(ind2) = inod1
		}                                                                          // {VB2J [160]}	         Next i
	  }                                                                            // {VB2J [161]}	      End If

	if (unusednodestemp[0] == 2 && RnR.Rnd() > 0.5)  {                                    // {VB2J [163]}	      If (unusednodestemp(0) = 2 And Rnd > 0.5) Then
	  int xnodtemp = unusednodestemp[1];                                               // {VB2J [164]}	         xnodtemp = unusednodestemp(1)
	  unusednodestemp[1] = unusednodestemp[2];                                     // {VB2J [165]}	         unusednodestemp(1) = unusednodestemp(2)
	  unusednodestemp[2] = xnodtemp;                                               // {VB2J [166]}	         unusednodestemp(2) = xnodtemp
	  } else if (unusednodestemp[0] > 2)  {                                        // {VB2J [167]}	      ElseIf (unusednodestemp(0) > 2) Then
	  
	  int ind1, ind2;
	  for (int i=1; i<=unusednodestemp[0]; i++) {                                  // {VB2J [168]}	         For i = 1 To unusednodestemp(0)
		ind1 = RnR.Int(unusednodestemp[0] * RnR.Rnd() + 1);                                  // {VB2J [169]}	            ind1 = Int(unusednodestemp(0) * Rnd + 1)
		ind2 = ind1;                                                               // {VB2J [170]}	            ind2 = ind1

		while (ind2 == ind1) {                    									// {VB2J [172]}	            Do While (ind2 = ind1)
			ind2 = RnR.Int(unusednodestemp[0] * RnR.Rnd() + 1);                              // {VB2J [173]}	               ind2 = Int(unusednodestemp(0) * Rnd + 1)
		}                                                                          // {VB2J [174]}	            Loop

	  inod1 = unusednodestemp[ind1];                                               // {VB2J [176]}	            inod1 = unusednodestemp(ind1)
	  inod2 = unusednodestemp[ind2];                                               // {VB2J [177]}	            inod2 = unusednodestemp(ind2)

	  unusednodestemp[ind1] = inod2;                                               // {VB2J [179]}	            unusednodestemp(ind1) = inod2
	  unusednodestemp[ind2] = inod1;                                               // {VB2J [180]}	            unusednodestemp(ind2) = inod1
	  }                                                                            // {VB2J [181]}	         Next i
	}                                                                              // {VB2J [182]}	      End If

																				   // {VB2J [185]}	      'randomize the functions in the objective function job set

	if (systemDesign_.hammingscale > 0)  {                                         // {VB2J [187]}	      If (hammingscale > 0#) Then
		for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) { // {VB2J [188]}	         For ifnjob = 1 To objfnjobset.numobjfn
	  		ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                           // {VB2J [189]}	            ifnx = objfnjobset.initfn(ifnjob)
	  		objfnjobsetlist[ifnjob] = ifnx;                                            // {VB2J [190]}	            objfnjobsetlist(ifnjob) = ifnx

			// 17Mar04 - Objective functions should never be zero, but sometimes I'm seeing that, so warn.
			if (ifnx == 0) {
				if (systemDesign_.isWarnEnabled())
					SystemDesign.logWarn("AnnealForm.getRandomState: ifnx=0, ifnjob=" + ifnjob);
					}

			}                                                                            // {VB2J [191]}	         Next ifnjob
		objfnjobsetlistHasBeenInited = true;

		if (systemDesign_.objfnjobset.numobjfn == 2 && RnR.Rnd() > 0.5)  {                                  // {VB2J [193]}	         If (objfnjobset.numobjfn = 2 And Rnd > 0.5) Then
		  int xtemp = objfnjobsetlist[1];                                                  // {VB2J [194]}	            xtemp = objfnjobsetlist(1)
		  objfnjobsetlist[1] = objfnjobsetlist[2];                                     // {VB2J [195]}	            objfnjobsetlist(1) = objfnjobsetlist(2)
		  objfnjobsetlist[2] = xtemp;                                                  // {VB2J [196]}	            objfnjobsetlist(2) = xtemp
	
		// 17Mar04
		 if (objfnjobsetlist[1] == 0) {
			if (systemDesign_.isWarnEnabled())
				SystemDesign.logWarn("AnnealForm.getRandomState: objfnjobsetlist[1] == 0");
			}
		if (objfnjobsetlist[2] == 0) {
			if (systemDesign_.isWarnEnabled())
				SystemDesign.logWarn("AnnealForm.getRandomState: objfnjobsetlist[2] == 0");
			}

	  } else {                                                                     // {VB2J [197]}	         Else

	  for (int i=1; i<=systemDesign_.objfnjobset.numobjfn; i++) {                                // {VB2J [198]}	            For i = 1 To objfnjobset.numobjfn
// {VB2J [199]}	               'pick two agents and flip the order in the list
		ifnx1 = RnR.Int(systemDesign_.objfnjobset.numobjfn * RnR.Rnd() + 1);                               // {VB2J [200]}	               ifnx1 = Int(objfnjobset.numobjfn * Rnd + 1)
		ifnx2 = ifnx1;                                                             // {VB2J [201]}	               ifnx2 = ifnx1

		while (ifnx2 == ifnx1) {                 // {VB2J [203]}	               Do While (ifnx2 = ifnx1)
			ifnx2 = RnR.Int(systemDesign_.objfnjobset.numobjfn * RnR.Rnd() + 1);                               // {VB2J [204]}	                  ifnx2 = Int(objfnjobset.numobjfn * Rnd + 1)
			}                                                                          // {VB2J [205]}	               Loop

	  	ifn1 = objfnjobsetlist[ifnx1];                                               // {VB2J [207]}	               ifn1 = objfnjobsetlist(ifnx1)
		ifn2 = objfnjobsetlist[ifnx2];                                               // {VB2J [208]}	               ifn2 = objfnjobsetlist(ifnx2)
	
		objfnjobsetlist[ifnx1] = ifn2;                                               // {VB2J [210]}	               objfnjobsetlist(ifnx1) = ifn2
		objfnjobsetlist[ifnx2] = ifn1;                                               // {VB2J [211]}	               objfnjobsetlist(ifnx2) = ifn1
	
		  // 17mar04 - Objective functions should never be zero, but sometimes I'm seeing that, so warn.
		if (ifn1 == 0 || ifn2 == 0) {
			if (systemDesign_.isWarnEnabled()) {
				SystemDesign.logWarn("AnnealForm.getRandomState: 2:");
				SystemDesign.logWarn(" ifn2=" + ifn2 + " ifnx2=" + ifnx2);
				SystemDesign.logWarn(" objfnjobsetlist[ifnx2]=" + objfnjobsetlist[ifnx2]);
				}
			}


	  }                                                                            // {VB2J [212]}	            Next i
	}                                                                              // {VB2J [213]}	         End If
	}                                                                              // {VB2J [214]}	      End If


//	logDebug("  AnnealForm.getRandomState() 3 checkmobility " + checkmobility);


// GoTo target label moved to Java-compatible labeled-while
//
startmainloop:

	while (checkmobility == false) {                       // {VB2J [217]}	      Do While (Not checkmobility)  'marc en 1.1 speedup

//		logDebug("  AnnealForm.getRandomState() 4 minultralognodes " + systemDesign_.minultralognodes);

//startmainloop:                                                                 // {VB2J [219]}	startmainloop:

// {VB2J [221]}	'rob: ignore this section. I need to fix the minultralognodes problem
//		         rec - 13aug - fixed, removed 'true' constant
//
	if ((systemDesign_.numstatelist > 0) 
	    && (usednodestemp[0] < systemDesign_.minultralognodes) 
	    && (RnR.Rnd() < 0.6))  { // {VB2J [222]}	         If (True And (numstatelist > 0) And (usednodestemp(0) < minultralognodes) And (Rnd < 0.6)) Then
			
//logDebug("  AnnealForm.getRandomState() case 5 ");

			
																				   // {VB2J [223]}	            'move a function from a random used node to a random unused node
	for (int inodsrcx=1; inodsrcx<=usednodestemp[0]; inodsrcx++) {                 // {VB2J [224]}	            For inodsrcx = 1 To usednodestemp(0)
		int inodsrc = usednodestemp[inodsrcx];                                             // {VB2J [225]}	               inodsrc = usednodestemp(inodsrcx)
		for (ifnx=1; ifnx<=systemDesign_.nodeinfo[inodsrc].numagents; ifnx++) {                  // {VB2J [226]}	               For ifnx = 1 To nodeinfo(inodsrc).numagents
		  ifn = systemDesign_.nodeinfo[inodsrc].agentlist[ifnx];                                     // {VB2J [227]}	                  ifn = nodeinfo(inodsrc).agentlist(ifnx)
		  for (int inodnewx=1; inodnewx<=unusednodestemp[0]; inodnewx++) {             // {VB2J [228]}	                  For inodnewx = 1 To unusednodestemp(0)
			inodnew = usednodestemp[inodnewx];                                         // {VB2J [229]}	                     inodnew = usednodestemp(inodnewx)
			if (systemDesign_.xnfunction[ifn].felig[inodnew] == 1)  {                                // {VB2J [230]}	                     If (xnfunction(ifn).felig(inodnew) = 1) Then
				systemDesign_.agent[ifn].testhostid = inodnew;                                         // {VB2J [231]}	                        agent(ifn).testhostid = inodnew
			  checkmobility = true;                                                    // {VB2J [232]}	                        checkmobility = True
			  break;                                                                   // {VB2J [233]}	                        Exit For
			  }                                                                        // {VB2J [234]}	                     End If
			}                                                                          // {VB2J [235]}	                  Next inodnewx
	
			if (checkmobility) break;                                              // {VB2J [237]}	                  If (checkmobility) Then Exit For
		}                                                                          // {VB2J [238]}	               Next ifnx
		
		if (checkmobility) break;                                              // {VB2J [240]}	               If (checkmobility) Then Exit For
	}                                                                          // {VB2J [241]}	            Next inodsrcx


	  } else if ((RnR.Rnd() < 0.5) && (systemDesign_.numstatelist > 0) 
	  				&& (systemDesign_.pfailurescale > 0))  {       // {VB2J [245]}	         ElseIf ((Rnd < 0.5) And (numstatelist > 0) And (pfailurescale > 0#)) Then   'marc en 1.1 smoothing
																				   // {VB2J [246]}	            'pick a node inod, and try to move all agents off of the node
//logDebug("  AnnealForm.getRandomState() case 6 ");

		boolean foundstate = false;                                                          // {VB2J [248]}	            foundstate = False

	  for (int indx=1; indx<=usednodestemp[0]; indx++) {                           // {VB2J [250]}	            For indx = 1 To usednodestemp(0)
		inod = usednodestemp[indx];                                                // {VB2J [251]}	               inod = usednodestemp(indx)

		// {VB2J [252]}	 'check that host inod can be fully remapped
		for (ifnx=1; ifnx<=systemDesign_.nodeinfo[inod].numagents; ifnx++) {                 // {VB2J [253]}	               For ifnx = 1 To nodeinfo(inod).numagents
		  ifn = systemDesign_.nodeinfo[inod].agentlist[ifnx];                                    // {VB2J [254]}	                  ifn = nodeinfo(inod).agentlist(ifnx)
// 15Jun04(former)line 2841 - site of NPE for bug #13609
			if (ifn == 0) {
				if (systemDesign_.isErrorEnabled())
					SystemDesign.logError("Preemptive fix (2) of ifn=0 error; breaking loop; no state found?");
				break;
				}
		  if (systemDesign_.xnfunction[ifn].nodelist[0] == 1)  {                                 // {VB2J [255]}	                  If (xnfunction(ifn).nodelist(0) = 1) Then
			// {VB2J [256]}	                     'this node cannot be fully remapped to other servers
			break;                                                                 // {VB2J [257]}	                     Exit For
			} else if (systemDesign_.xnfunction[ifn].nodelist[0] > 1 && ifnx == systemDesign_.nodeinfo[inod].numagents)  { // {VB2J [258]}	                  ElseIf (xnfunction(ifn).nodelist(0) > 1 And ifnx = nodeinfo(inod).numagents) Then
			foundstate = true;                                                     // {VB2J [259]}	                     foundstate = True
			}                                                                      // {VB2J [260]}	                  End If
		  }                                                                        // {VB2J [261]}	               Next ifnx

		if (foundstate) break;                                               // {VB2J [263]}	               If (foundstate) Then Exit For
	 }                                                                        // {VB2J [264]}	            Next indx

		if ( ! foundstate) continue startmainloop;                                 // {VB2J [266]}	            If (Not foundstate) Then GoTo startmainloop

		  float rndtest = RnR.Rnd();                                                  // {VB2J [269]}	            rndtest = Rnd

		  if (rndtest < 0.4)  {                                                    // {VB2J [271]}	            If (rndtest < 0.4) Then

			// {VB2J [272]}	               'move all agents on inod to other current active hosts
			for (inodx=1; inodx<=systemDesign_.numnode; inodx++) {                           // {VB2J [274]}	               For inodx = 1 To numnode
			  nodeutiltemp[inodx] = systemDesign_.statelist[0].utiltot[inodx];                   // {VB2J [275]}	                  nodeutiltemp(inodx) = statelist(0).utiltot(inodx)
			  nodememtemp[inodx] = systemDesign_.statelist[0].memtot[inodx];                     // {VB2J [276]}	                  nodememtemp(inodx) = statelist(0).memtot(inodx)
			  }                                                                    // {VB2J [277]}	               Next inodx

			int ind = 0;                                                               // {VB2J [279]}	               ind = 0
			for (inod2=1; inod2<=systemDesign_.numnode; inod2++) {                           // {VB2J [280]}	               For inod2 = 1 To numnode

			// {VB2J [281]}	 'marc en 1.1 comments: activenodes(*) is a list of all actively used hosts, EXCLUDING node inod
			  for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {         // {VB2J [282]}	                  For ifnjob = 1 To objfnjobset.numobjfn
				ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                                 // {VB2J [283]}	                     ifnx = objfnjobset.initfn(ifnjob)
				inodx = systemDesign_.agent[ifnx].testhostid;                                    // {VB2J [284]}	                     inodx = agent(ifnx).testhostid
				if (inodx == inod2 && inodx != inod)  {                             // {VB2J [285]}	                     If (inodx = inod2 And inodx <> inod) Then
				  ind++;                                                   // {VB2J [286]}	                        ind = ind + 1
				  activenodes[ind] = inod2;                                        // {VB2J [287]}	                        activenodes(ind) = inod2
				  break;                                                           // {VB2J [288]}	                        Exit For
				  }                                                                // {VB2J [289]}	                     End If
				}                                                                  // {VB2J [290]}	                  Next ifnjob
			  }                                                                    // {VB2J [291]}	               Next inod2

			numactivenodes = ind;                                                  // {VB2J [293]}	               numactivenodes = ind

			if (numactivenodes == 0) continue startmainloop;                      // {VB2J [295]}	               If (numactivenodes = 0) Then GoTo startmainloop

 // {VB2J [297]}	               'loop over all functions on inod, and test to remove all agents from a server

			  for (int indx=1; indx<=usednodestemp[0]; indx++) {                   // {VB2J [299]}	               For indx = 1 To usednodestemp(0)
				inod = usednodestemp[indx];                                        // {VB2J [300]}	                  inod = usednodestemp(indx)

// This doesn't seem to happen any more. ???
//
//	if (inod == 0) {
//		if (systemDesign_.isErrorEnabled())
//			SystemDesign.logError("AnnealForm.getRandomState: inod==0 error about to happen?");
//		}


// {VB2J [301]}	                  'check that host inod can be fully remapped
				for (ifnx=1; ifnx<=systemDesign_.nodeinfo[inod].numagents; ifnx++) {         // {VB2J [302]}	                  For ifnx = 1 To nodeinfo(inod).numagents
				  ifn = systemDesign_.nodeinfo[inod].agentlist[ifnx];                            // {VB2J [303]}	                     ifn = nodeinfo(inod).agentlist(ifnx)

				  foundnewnode = false;                                            // {VB2J [305]}	                     foundnewnode = False
// {VB2J [306]}	                     'loop over activenodes, trying to find fits:
				  for (int indxnew=1; indxnew<=usednodestemp[0]; indxnew++) {      // {VB2J [307]}	                     For indxnew = 1 To usednodestemp(0)
					inodnew = usednodestemp[indxnew];                              // {VB2J [308]}	                        inodnew = usednodestemp(indxnew)
					if (inodnew != inod)  {                                        // {VB2J [309]}	                        If (inodnew <> inod) Then
					  if (systemDesign_.xnfunction[ifn].felig[inodnew] == 1)  {                  // {VB2J [310]}	                           If (xnfunction(ifn).felig(inodnew) = 1) Then
						if ((nodeutiltemp[inodnew] + systemDesign_.xnfunction[ifn].cpurate[inodnew] / systemDesign_.nodeinfo[inodnew].cpucount + systemDesign_.nodeinfo[inodnew].cpubackground / 100
									 <= systemDesign_.nodeinfo[inodnew].cpuutilmax / 100) 
						 && (nodememtemp[inodnew] + systemDesign_.xnfunction[ifn].memory[inodnew] <= systemDesign_.nodeinfo[inodnew].memory))  { // {VB2J [312]}	                              If ((nodeutiltemp(inodnew) + xnfunction(ifn).cpurate(inodnew) / nodeinfo(inodnew).cpucount + nodeinfo(inodnew).cpubackground / 100# <= nodeinfo(inodnew).cpuutilmax / 100#)                                  And (nodememtemp(inodnew) + xnfunction(ifn).memory(inodnew) <= nodeinfo(inodnew).memory)) Then

						  nodeutiltemp[inodnew] = nodeutiltemp[inodnew] + systemDesign_.xnfunction[ifn].cpurate[inodnew] / systemDesign_.nodeinfo[inodnew].cpucount;              // {VB2J [314]}	                                 nodeutiltemp(inodnew) = nodeutiltemp(inodnew) + xnfunction(ifn).cpurate(inodnew) / nodeinfo(inodnew).cpucount
						  nodememtemp[inodnew] = nodememtemp[inodnew] + systemDesign_.xnfunction[ifn].memory[inodnew];              // {VB2J [315]}	                                 nodememtemp(inodnew) = nodememtemp(inodnew) + xnfunction(ifn).memory(inodnew)
																				   // {VB2J [316]}	                                 'agent(ifnx).testhostid = inodnew
						  foundnewnode = true;                                     // {VB2J [317]}	                                 foundnewnode = True
						  break;                                                   // {VB2J [318]}	                                 Exit For
						  }                                                        // {VB2J [319]}	                              End If
						}                                                          // {VB2J [320]}	                           End If
					  }                                                            // {VB2J [321]}	                        End If
					}                                                              // {VB2J [322]}	                     Next indxnew

				  if ( ! foundnewnode) break;                                // {VB2J [324]}	                     If (Not foundnewnode) Then Exit For

					if (ifnx == systemDesign_.nodeinfo[inod].numagents)  {                       // {VB2J [326]}	                     If (ifnx = nodeinfo(inod).numagents) Then
					  foundinod = true;                                            // {VB2J [327]}	                        foundinod = True
					  inodsave = inod;                                             // {VB2J [328]}	                        inodsave = inod
					  }                                                            // {VB2J [329]}	                     End If
					}                                                              // {VB2J [330]}	                  Next ifnx

				  if (foundinod) break;                                      // {VB2J [332]}	                  If (foundinod) Then Exit For
					}                                                              // {VB2J [333]}	               Next indx

				// {VB2J [335]}	               'now remove all agents from node inodsave
				  inod = inodsave;                                                 // {VB2J [336]}	               inod = inodsave


			// It seems this error does not occur any more,
			//  but just in case, this code seems to fix it - aug'03 - ???
			//
			if (inod == 0) {
				if (systemDesign_.isErrorEnabled())
					SystemDesign.logError("Preemptive fix of inod=0 error; setting inod=1");
				inod = 1; // ???
				}
			else {
				if (systemDesign_.nodeinfo[inod] == null) {
					if (systemDesign_.isErrorEnabled())
						SystemDesign.logError("Preemptive fix of inod=0 error FAILED on node" + inod);
						// we'll get an NPE next....
					}
				}
			// end fixed error check

				  for (ifnx=1; ifnx<=systemDesign_.nodeinfo[inod].numagents; ifnx++) {       // {VB2J [338]}	               For ifnx = 1 To nodeinfo(inod).numagents

					ifn = systemDesign_.nodeinfo[inod].agentlist[ifnx];                          // {VB2J [339]}	                  ifn = nodeinfo(inod).agentlist(ifnx)

// {VB2J [341]}	                  'loop over activenodes, trying to find fits:
					for (int indxnew=1; indxnew<=usednodestemp[0]; indxnew++) {    // {VB2J [342]}	                  For indxnew = 1 To usednodestemp(0)
					  inodnew = usednodestemp[indxnew];                            // {VB2J [343]}	                     inodnew = usednodestemp(indxnew)
					  if (inodnew != inod)  {                                      // {VB2J [344]}	                     If (inodnew <> inod) Then
						if (systemDesign_.xnfunction[ifn].felig[inodnew] == 1)  {                // {VB2J [345]}	                        If (xnfunction(ifn).felig(inodnew) = 1) Then
						  if ((nodeutiltemp[inodnew] 
						 		+ systemDesign_.xnfunction[ifn].cpurate[inodnew] / systemDesign_.nodeinfo[inodnew].cpucount 
						  		+ systemDesign_.nodeinfo[inodnew].cpubackground / 100 
						  		<= (systemDesign_.nodeinfo[inodnew].cpuutilmax / 100)) 
						  	  && (nodememtemp[inodnew] + systemDesign_.xnfunction[ifn].memory[inodnew] 
						  	    <= systemDesign_.nodeinfo[inodnew].memory))  { // {VB2J [347]}	                           If ((nodeutiltemp(inodnew) + xnfunction(ifn).cpurate(inodnew) / nodeinfo(inodnew).cpucount + nodeinfo(inodnew).cpubackground / 100# <= nodeinfo(inodnew).cpuutilmax / 100#)                               And (nodememtemp(inodnew) + xnfunction(ifn).memory(inodnew) <= nodeinfo(inodnew).memory)) Then

							nodeutiltemp[inodnew] = nodeutiltemp[inodnew] + systemDesign_.xnfunction[ifn].cpurate[inodnew] / systemDesign_.nodeinfo[inodnew].cpucount;              // {VB2J [349]}	                              nodeutiltemp(inodnew) = nodeutiltemp(inodnew) + xnfunction(ifn).cpurate(inodnew) / nodeinfo(inodnew).cpucount
							nodememtemp[inodnew] = nodememtemp[inodnew] + systemDesign_.xnfunction[ifn].memory[inodnew];              // {VB2J [350]}	                              nodememtemp(inodnew) = nodememtemp(inodnew) + xnfunction(ifn).memory(inodnew)
							systemDesign_.agent[ifn].testhostid = inodnew;                       // {VB2J [351]}	                              agent(ifn).testhostid = inodnew
							foundnewnode = true;                                   // {VB2J [352]}	                              foundnewnode = True
							break;                                                 // {VB2J [353]}	                              Exit For
							}                                                      // {VB2J [354]}	                           End If
						  }                                                        // {VB2J [355]}	                        End If
						}                                                          // {VB2J [356]}	                     End If
					  }                                                            // {VB2J [357]}	                  Next indxnew
					}                                                              // {VB2J [358]}	               Next ifnx

				  checkmobility = true;                                            // {VB2J [360]}	               checkmobility = True


																				   // {VB2J [363]}	            'ElseIf (rndtest < 0.7) Then '         If (rndtest < 0.5) Then
				  } else if (rndtest < 0.9)  {                                     // {VB2J [364]}	            ElseIf (rndtest < 0.9) Then 'marc en 1.1 speedup: change 0.7 to 0.9

 // {VB2J [365]}	               'try to move all agents from random active node to random inactive node plus active nodes
 // {VB2J [366]}	               'pick one with lowest pfail

 // {VB2J [368]}	               'Unload all agents from one of the servers, and try to transfer all agents to one of
 // {VB2J [369]}	               'the empty servers (randomly weighted towards low pfail) plus the servers that are
 // {VB2J [370]}	               'currently in use. Two modes: greedy mode that unloads highest pfail and loads lowest pfail,
 // {VB2J [371]}	               'and pure random mode that unloads and loads any pfail host.

				  for (inodx=1; inodx<=systemDesign_.numnode; inodx++) {                     // {VB2J [373]}	               For inodx = 1 To numnode
					nodeutiltemp2[inodx] = nodeutiltemp[inodx];                    // {VB2J [374]}	                  nodeutiltemp2(inodx) = nodeutiltemp(inodx)
					nodememtemp2[inodx] = nodememtemp[inodx];                      // {VB2J [375]}	                  nodememtemp2(inodx) = nodememtemp(inodx)
					}                                                              // {VB2J [376]}	               Next inodx

				  if (RnR.Rnd() < 0.99)  {                                               // {VB2J [378]}	               If (Rnd < 0.99) Then
																				   // {VB2J [379]}	                  'dumb move, first test, then execute the move
					for (int indx=1; indx<=usednodestemp[0]; indx++) {             // {VB2J [380]}	                  For indx = 1 To usednodestemp(0)
					  inod = usednodestemp[indx];                                  // {VB2J [381]}	                     inod = usednodestemp(indx)

																				   // {VB2J [383]}	                     'try to load all agents off of inod, and onto one other unused node, and possibly other used nodes
																				   // {VB2J [384]}	                     'select an unused node from list:
					  for (int indxnew=1; indxnew<=unusednodestemp[0]; indxnew++) {// {VB2J [385]}	                     For indxnew = 1 To unusednodestemp(0)
						inodnew = unusednodestemp[indxnew];                        // {VB2J [386]}	                        inodnew = unusednodestemp(indxnew)

						for (ifnx=1; ifnx<=systemDesign_.nodeinfo[inod].numagents; ifnx++) { // {VB2J [388]}	                        For ifnx = 1 To nodeinfo(inod).numagents
						  ifn = systemDesign_.nodeinfo[inod].agentlist[ifnx];                    // {VB2J [389]}	                           ifn = nodeinfo(inod).agentlist(ifnx)
																				   // {VB2J [390]}	                           'first try to move to selected unused node, then to remaining used nodes

						  agentmoved = false;                                      // {VB2J [392]}	                           agentmoved = False

						  if ((systemDesign_.xnfunction[ifn].felig[inodnew] == 1) 
						  	&& ((nodeutiltemp2[inodnew] + systemDesign_.xnfunction[ifn].cpurate[inodnew] / systemDesign_.nodeinfo[inodnew].cpucount + systemDesign_.nodeinfo[inodnew].cpubackground / 100 
						        < systemDesign_.nodeinfo[inodnew].cpuutilmax / 100)) 
						       && (nodememtemp2[inodnew] + systemDesign_.xnfunction[ifn].memory[inodnew] < systemDesign_.nodeinfo[inodnew].memory))  { // {VB2J [396]}	                           If ((xnfunction(ifn).felig(inodnew) = 1)                               And ((nodeutiltemp2(inodnew) + xnfunction(ifn).cpurate(inodnew) / nodeinfo(inodnew).cpucount + nodeinfo(inodnew).cpubackground / 100# < nodeinfo(inodnew).cpuutilmax / 100#))                               And (nodememtemp2(inodnew) + xnfunction(ifn).memory(inodnew) < nodeinfo(inodnew).memory)) Then

							nodeutiltemp2[inodnew] = nodeutiltemp2[inodnew] + systemDesign_.xnfunction[ifn].cpurate[inodnew] / systemDesign_.nodeinfo[inodnew].cpucount;              // {VB2J [398]}	                              nodeutiltemp2(inodnew) = nodeutiltemp2(inodnew) + xnfunction(ifn).cpurate(inodnew) / nodeinfo(inodnew).cpucount
							nodememtemp2[inodnew] = nodememtemp2[inodnew] + systemDesign_.xnfunction[ifn].memory[inodnew];              // {VB2J [399]}	                              nodememtemp2(inodnew) = nodememtemp2(inodnew) + xnfunction(ifn).memory(inodnew)
																				   // {VB2J [400]}	                              'agent(ifn).testhostid = inodnew
							agentmoved = true;                                     // {VB2J [401]}	                              agentmoved = True
							} else {                                               // {VB2J [402]}	                           Else
																				   // {VB2J [403]}	                              'now loop over used nodes, and try to fit the function somewhere
							for (int indxnew2=1; indxnew2<=usednodestemp[0]; indxnew2++) { // {VB2J [404]}	                              For indxnew2 = 1 To usednodestemp(0)
							  inodnew2 = usednodestemp[indxnew2];                  // {VB2J [405]}	                                 inodnew2 = usednodestemp(indxnew2)
							  if (inodnew2 != inod)  {                             // {VB2J [406]}	                                 If (inodnew2 <> inod) Then
								if ((systemDesign_.xnfunction[ifn].felig[inodnew2] == 1) 
								     && ((nodeutiltemp2[inodnew2] + systemDesign_.xnfunction[ifn].cpurate[inodnew2] / systemDesign_.nodeinfo[inodnew2].cpucount + systemDesign_.nodeinfo[inodnew2].cpubackground / 100
								         < systemDesign_.nodeinfo[inodnew2].cpuutilmax / 100)) 
								     && (nodememtemp2[inodnew2] + systemDesign_.xnfunction[ifn].memory[inodnew2] < systemDesign_.nodeinfo[inodnew2].memory))  { 
								   // {VB2J [409]}	  If ((xnfunction(ifn).felig(inodnew2) = 1) And ((nodeutiltemp2(inodnew2) + xnfunction(ifn).cpurate(inodnew2) / nodeinfo(inodnew2).cpucount + nodeinfo(inodnew2).cpubackground / 100# < nodeinfo(inodnew2).cpuutilmax / 100#))                                        And (nodememtemp2(inodnew2) + xnfunction(ifn).memory(inodnew2) < nodeinfo(inodnew2).memory)) Then

								  nodeutiltemp2[inodnew2] = nodeutiltemp2[inodnew2] + systemDesign_.xnfunction[ifn].cpurate[inodnew2] / systemDesign_.nodeinfo[inodnew2].cpucount;              // {VB2J [411]}	                                       nodeutiltemp2(inodnew2) = nodeutiltemp2(inodnew2) + xnfunction(ifn).cpurate(inodnew2) / nodeinfo(inodnew2).cpucount
								  nodememtemp2[inodnew2] = nodememtemp2[inodnew2] + systemDesign_.xnfunction[ifn].memory[inodnew2];              // {VB2J [412]}	                                       nodememtemp2(inodnew2) = nodememtemp2(inodnew2) + xnfunction(ifn).memory(inodnew2)
																				   // {VB2J [413]}	                                       'agent(ifn).testhostid = inodnew2
								  agentmoved = true;                               // {VB2J [414]}	                                       agentmoved = True
								  break;                                           // {VB2J [415]}	                                       Exit For
								  }                                                // {VB2J [416]}	                                    End If
								}                                                  // {VB2J [417]}	                                 End If
							  }                                                    // {VB2J [418]}	                              Next indxnew2
							}                                                      // {VB2J [419]}	                           End If

						  if ( ! agentmoved)  {                                    // {VB2J [421]}	                           If (Not agentmoved) Then
 // {VB2J [422]}	                              'node inod cannot be emptied onto inodnew plus inodnew2(*)
 // {VB2J [423]}	                              'pick another inodnew
							break;                                                 // {VB2J [424]}	                              Exit For
							}                                                      // {VB2J [425]}	                           End If
						  }                                                        // {VB2J [426]}	                        Next ifnx

						if (agentmoved)  {                                         // {VB2J [428]}	                        If (agentmoved) Then
 // {VB2J [429]}	                           'success on last step of above loop
						  inodsave = inod;                                         // {VB2J [430]}	                           inodsave = inod
						  inodnewsave = inodnew;                                   // {VB2J [431]}	                           inodnewsave = inodnew
						  break;                                                   // {VB2J [432]}	                           Exit For
						  }                                                        // {VB2J [433]}	                        End If
						}                                                          // {VB2J [434]}	                     Next indxnew

					  if (agentmoved)  {                                           // {VB2J [436]}	                     If (agentmoved) Then
						break;                                                     // {VB2J [437]}	                        Exit For
						}                                                          // {VB2J [438]}	                     End If
					  }                                                            // {VB2J [439]}	                  Next indx

					if ( ! agentmoved) continue startmainloop;                     // {VB2J [441]}	                  If (Not agentmoved) Then GoTo startmainloop 'this is a wasted move...

					  inod = inodsave;                                             // {VB2J [443]}	                  inod = inodsave
					  inodnew = inodnewsave;                                       // {VB2J [444]}	                  inodnew = inodnewsave

					  for (ifnx=1; ifnx<=systemDesign_.nodeinfo[inod].numagents; ifnx++) {   // {VB2J [446]}	                  For ifnx = 1 To nodeinfo(inod).numagents
						ifn = systemDesign_.nodeinfo[inod].agentlist[ifnx];                      // {VB2J [447]}	                     ifn = nodeinfo(inod).agentlist(ifnx)
																				   // {VB2J [448]}	                     'first try to move to selected unused node, then to remaining used nodes

						agentmoved = false;                                        // {VB2J [450]}	                     agentmoved = False

						if ((systemDesign_.xnfunction[ifn].felig[inodnew] == 1) 
						&& ((nodeutiltemp[inodnew] + systemDesign_.xnfunction[ifn].cpurate[inodnew] / systemDesign_.nodeinfo[inodnew].cpucount +
								systemDesign_.nodeinfo[inodnew].cpubackground / 100 < systemDesign_.nodeinfo[inodnew].cpuutilmax / 100)) 
									 && (nodememtemp[inodnew] + systemDesign_.xnfunction[ifn].memory[inodnew] < systemDesign_.nodeinfo[inodnew].memory))  { // {VB2J [454]}	                     If ((xnfunction(ifn).felig(inodnew) = 1)                         And ((nodeutiltemp(inodnew) + xnfunction(ifn).cpurate(inodnew) / nodeinfo(inodnew).cpucount + nodeinfo(inodnew).cpubackground / 100# < nodeinfo(inodnew).cpuutilmax / 100#))                         And (nodememtemp(inodnew) + xnfunction(ifn).memory(inodnew) < nodeinfo(inodnew).memory)) Then

						  nodeutiltemp[inodnew] = nodeutiltemp[inodnew] + systemDesign_.xnfunction[ifn].cpurate[inodnew] / systemDesign_.nodeinfo[inodnew].cpucount;              // {VB2J [456]}	                        nodeutiltemp(inodnew) = nodeutiltemp(inodnew) + xnfunction(ifn).cpurate(inodnew) / nodeinfo(inodnew).cpucount
						  nodememtemp[inodnew] = nodememtemp[inodnew] + systemDesign_.xnfunction[ifn].memory[inodnew];              // {VB2J [457]}	                        nodememtemp(inodnew) = nodememtemp(inodnew) + xnfunction(ifn).memory(inodnew)
						  systemDesign_.agent[ifn].testhostid = inodnew;                         // {VB2J [458]}	                        agent(ifn).testhostid = inodnew
						  agentmoved = true;                                       // {VB2J [459]}	                        agentmoved = True
						  } else {                                                 // {VB2J [460]}	                     Else
																				   // {VB2J [461]}	                        'now loop over used nodes, and try to fit the function somewhere
						  for (int indxnew2=1; indxnew2<=usednodestemp[0]; indxnew2++) { // {VB2J [462]}	                        For indxnew2 = 1 To usednodestemp(0)
							inodnew2 = usednodestemp[indxnew2];                    // {VB2J [463]}	                           inodnew2 = usednodestemp(indxnew2)

							if ((systemDesign_.xnfunction[ifn].felig[inodnew2] == 1) 
								&& ((nodeutiltemp[inodnew2] + systemDesign_.xnfunction[ifn].cpurate[inodnew2] /
										 systemDesign_.nodeinfo[inodnew2].cpucount + systemDesign_.nodeinfo[inodnew2].cpubackground / 100 
									< systemDesign_.nodeinfo[inodnew2].cpuutilmax / 100)) 
								&& (nodememtemp[inodnew2] + systemDesign_.xnfunction[ifn].memory[inodnew2] < systemDesign_.nodeinfo[inodnew2].memory))  { // {VB2J [467]}	                           If ((xnfunction[ifn].felig(inodnew2) = 1)                               And ((nodeutiltemp(inodnew2) + xnfunction(ifn).cpurate(inodnew2) / nodeinfo(inodnew2).cpucount + nodeinfo(inodnew2).cpubackground / 100# < nodeinfo(inodnew2).cpuutilmax / 100#))                               And (nodememtemp(inodnew2) + xnfunction(ifn).memory(inodnew2) < nodeinfo(inodnew2).memory)) Then

							  nodeutiltemp[inodnew2] = nodeutiltemp[inodnew2] + systemDesign_.xnfunction[ifn].cpurate[inodnew2] / systemDesign_.nodeinfo[inodnew2].cpucount;              // {VB2J [469]}	                              nodeutiltemp(inodnew2) = nodeutiltemp(inodnew2) + xnfunction(ifn).cpurate(inodnew2) / nodeinfo(inodnew2).cpucount
							  nodememtemp[inodnew2] = nodememtemp[inodnew2] + systemDesign_.xnfunction[ifn].memory[inodnew2];              // {VB2J [470]}	                              nodememtemp(inodnew2) = nodememtemp(inodnew2) + xnfunction(ifn).memory(inodnew2)
							  systemDesign_.agent[ifn].testhostid = inodnew2;                    // {VB2J [471]}	                              agent(ifn).testhostid = inodnew2
							  agentmoved = true;                                   // {VB2J [472]}	                              agentmoved = True
							  break;                                               // {VB2J [473]}	                              Exit For
							  }                                                    // {VB2J [474]}	                           End If

							}                                                      // {VB2J [476]}	                        Next indxnew2
						  }                                                        // {VB2J [477]}	                     End If

						if ( ! agentmoved)  {                                      // {VB2J [479]}	                     If (Not agentmoved) Then
// {VB2J [480]}	                        'node inod cannot be emptied onto inodnew plus inodnew2(*)
// {VB2J [481]}	                        'pick another inodnew
						  break;                                                   // {VB2J [482]}	                        Exit For
						  }                                                        // {VB2J [483]}	                     End If
						}                                                          // {VB2J [484]}	                  Next ifnx

					  if (agentmoved)  {                                           // {VB2J [486]}	                  If (agentmoved) Then
 // {VB2J [487]}	                     'success on last step of above loop
						inodsave = inod;                                           // {VB2J [488]}	                     inodsave = inod
						inodnewsave = inodnew;                                     // {VB2J [489]}	                     inodnewsave = inodnew
						}                                                          // {VB2J [490]}	                  End If

					  } else {                                                     // {VB2J [492]}	               Else   ' else for initial If (Rnd < 0.5) section
// {VB2J [493]}	                  'do a smart move
					  }                                                            // {VB2J [494]}	               End If
					checkmobility = true;                                          // {VB2J [496]}	               checkmobility = True

																				   // {VB2J [500]}	            'Else 'rndtest > 0.7
					} else {                                                       // {VB2J [501]}	            Else 'rndtest > 0.9 (marc en 1.1 comments
																				   // {VB2J [502]}	               'move all agents on node inod to other eligible nodes in system

					for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {   // {VB2J [504]}	               For ifnjob = 1 To objfnjobset.numobjfn
					  ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                           // {VB2J [505]}	                  ifnx = objfnjobset.initfn(ifnjob)
					  inodx = systemDesign_.agent[ifnx].annealhostid;                            // {VB2J [506]}	                  inodx = agent(ifnx).annealhostid
					  if (inodx == inod)  {                                        // {VB2J [507]}	                  If (inodx = inod) Then
						if (systemDesign_.xnfunction[ifnx].nodelist[0] > 1)  {                   // {VB2J [508]}	                     If (xnfunction(ifnx).nodelist(0) > 1) Then
																				   // {VB2J [509]}	                        'pick a random starting point in list:
						  int inodranstart = RnR.Int(systemDesign_.xnfunction[ifnx].nodelist[0] * RnR.Rnd() + 1);              // {VB2J [510]}	                        inodranstart = Int(xnfunction(ifnx).nodelist(0) * Rnd + 1)

						  for (int indx=inodranstart; indx<=systemDesign_.xnfunction[ifnx].nodelist[0]; indx++) { // {VB2J [512]}	                        For indx = inodranstart To xnfunction(ifnx).nodelist(0)
							inodnew = systemDesign_.xnfunction[ifnx].nodelist[indx];             // {VB2J [513]}	                           inodnew = xnfunction(ifnx).nodelist(indx)
							if (inodnew != inod)  {                                // {VB2J [514]}	                           If (inodnew <> inod) Then
								systemDesign_.agent[ifnx].testhostid = inodnew;                    // {VB2J [515]}	                              agent(ifnx).testhostid = inodnew
							  checkmobility = true;                                // {VB2J [516]}	                              checkmobility = True
							  }                                                    // {VB2J [517]}	                           End If
							}                                                      // {VB2J [518]}	                        Next indx

						  for (int indx=1; indx<=inodranstart-1; indx++) {         // {VB2J [520]}	                        For indx = 1 To inodranstart - 1
							inodnew = systemDesign_.xnfunction[ifnx].nodelist[indx];             // {VB2J [521]}	                           inodnew = xnfunction(ifnx).nodelist(indx)
							if (inodnew != inod)  {                                // {VB2J [522]}	                           If (inodnew <> inod) Then
								systemDesign_.agent[ifnx].testhostid = inodnew;                    // {VB2J [523]}	                              agent(ifnx).testhostid = inodnew
							  checkmobility = true;                                // {VB2J [524]}	                              checkmobility = True
							  }                                                    // {VB2J [525]}	                           End If
							}                                                      // {VB2J [526]}	                        Next indx
						  }                                                        // {VB2J [527]}	                     End If
						}                                                          // {VB2J [529]}	                  End If
					  }                                                            // {VB2J [530]}	               Next ifnjob
					}                                                              // {VB2J [531]}	            End If

				  } else {                                                         // {VB2J [535]}	         Else ' else for If (Rnd < 0.5 And numstatelist > 0 And pfailurescale > 0#) Then
																				   // {VB2J [536]}	            'move a function
//logDebug("  AnnealForm.getRandomState() case 7 ");

				  float rndtest = RnR.Rnd();                                                   // {VB2J [538]}	            rndtest = Rnd
				  if (rndtest < 0.1)  {                                            // {VB2J [540]}	            If (rndtest < 0.1) Then
																				   // {VB2J [541]}	               'dumb move

					ifnx = RnR.Int(systemDesign_.objfnjobset.numobjfn * RnR.Rnd() + 1);                  // {VB2J [543]}	               ifnx = Int((objfnjobset.numobjfn) * Rnd + 1)
					ifn = systemDesign_.objfnjobset.initfn[ifnx];                                // {VB2J [544]}	               ifn = objfnjobset.initfn(ifnx)

					if (systemDesign_.xnfunction[ifn].nodelist[0] > 1)  {                        // {VB2J [546]}	               If (xnfunction(ifn).nodelist(0) > 1) Then
					  for (int icount=1; icount<=systemDesign_.xnfunction[ifn].nodelist[0]; icount++) { // {VB2J [547]}	                  For icount = 1 To xnfunction(ifn).nodelist(0)
						inodrunx = RnR.Int(systemDesign_.xnfunction[ifn].nodelist[0] * RnR.Rnd() + 1);              // {VB2J [548]}	                     inodrunx = Int((xnfunction(ifn).nodelist(0)) * Rnd + 1)
						inod = systemDesign_.xnfunction[ifn].nodelist[inodrunx];                 // {VB2J [549]}	                     inod = xnfunction(ifn).nodelist(inodrunx)

						if (inod != systemDesign_.agent[ifn].testhostid)  {                      // {VB2J [551]}	                     If (inod <> agent(ifn).testhostid) Then
							systemDesign_.agent[ifn].testhostid = inod;                            // {VB2J [552]}	                        agent(ifn).testhostid = inod
						  checkmobility = true;                                    // {VB2J [553]}	                        checkmobility = True
						  break;                                                   // {VB2J [554]}	                        Exit For
						  }                                                        // {VB2J [555]}	                     End If
						}                                                          // {VB2J [556]}	                  Next icount
					  } else {                                                     // {VB2J [557]}	               Else
					  inod = systemDesign_.xnfunction[ifn].nodelist[1];                          // {VB2J [558]}	                  inod = xnfunction(ifn).nodelist(1)
					  systemDesign_.agent[ifn].testhostid = inod;                                // {VB2J [559]}	                  agent(ifn).testhostid = inod
					  checkmobility = true;                                        // {VB2J [560]}	                  checkmobility = True
					  }                                                            // {VB2J [561]}	               End If

																				   // {VB2J [564]}	            'Else  'marc en 1.1 smoothing

					} else if (rndtest < 0.5)  {                                   // {VB2J [566]}	            ElseIf (rndtest < 0.5) Then 'marc en 1.1 smoothing (change else to elseif (rndtest...)
																				   // {VB2J [567]}	               'do a smart function move

					int ind = 0;                                                       // {VB2J [569]}	               ind = 0

					for (inodx=1; inodx<=systemDesign_.numnode; inodx++) {                   // {VB2J [571]}	               For inodx = 1 To numnode
					  if ((nodeutiltemp[inodx] + systemDesign_.nodeinfo[inodx].cpubackground / 100 > systemDesign_.nodeinfo[inodx].cpuutilmax / 100) 
					   || (nodememtemp[inodx] > systemDesign_.nodeinfo[inodx].memory))  { // {VB2J [573]}	                  If ((nodeutiltemp(inodx) + nodeinfo(inodx).cpubackground / 100# > nodeinfo(inodx).cpuutilmax / 100#)                      Or (nodememtemp(inodx) > nodeinfo(inodx).memory)) Then

						ind++;                                             // {VB2J [575]}	                     ind = ind + 1
						systemDesign_.nodeoverload[ind].nodenum = inodx;                         // {VB2J [576]}	                     nodeoverload(ind).nodenum = inodx
						systemDesign_.nodeoverload[ind].cpuoverload = (float)nodeutiltemp[inodx] + systemDesign_.nodeinfo[inodx].cpubackground / 100 - systemDesign_.nodeinfo[inodx].cpuutilmax / 100;              // {VB2J [577]}	                     nodeoverload(ind).cpuoverload = nodeutiltemp(inodx) + nodeinfo(inodx).cpubackground / 100# - nodeinfo(inodx).cpuutilmax / 100#
						systemDesign_.nodeoverload[ind].memoverload = (float)nodememtemp[inodx] - systemDesign_.nodeinfo[inodx].memory;              // {VB2J [578]}	                     nodeoverload(ind).memoverload = nodememtemp(inodx) - nodeinfo(inodx).memory
						}                                                          // {VB2J [579]}	                  End If
					  }                                                            // {VB2J [580]}	               Next inodx

					systemDesign_.numnodeoverload = ind;                                         // {VB2J [582]}	               numnodeoverload = ind

					if (systemDesign_.numnodeoverload == 0)  {                                   // {VB2J [584]}	               If (numnodeoverload = 0) Then
					  continue startmainloop;                             		   // {VB2J [585]}	                  GoTo startmainloop
					  }                                                            // {VB2J [586]}	               End If

// {VB2J [588]}	               'move a random function off of a random overloaded node
					int inodranx = RnR.Int(systemDesign_.numnodeoverload * RnR.Rnd() + 1);                       // {VB2J [589]}	               inodranx = Int(numnodeoverload * Rnd + 1)
					inod = systemDesign_.nodeoverload[inodranx].nodenum;                         // {VB2J [590]}	               inod = nodeoverload(inodranx).nodenum

//  'en 2.01 load balancing cleanup: Rob: Delete following 10 lines:
//
// // {VB2J [592]}	               'build a list of agents on this server
//
//					int i = 0;                                                         // {VB2J [594]}	               i = 0
//					for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {   // {VB2J [595]}	               For ifnjob = 1 To objfnjobset.numobjfn
//					  ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                           // {VB2J [596]}	                  ifnx = objfnjobset.initfn(ifnjob)
//					  inodx = systemDesign_.agent[ifnx].testhostid;                              // {VB2J [597]}	                  inodx = agent(ifnx).testhostid
//					  if (inodx == inod)  {                                        // {VB2J [598]}	                  If (inodx = inod) Then
//						i++;                                                 // {VB2J [599]}	                     i = i + 1
////'en 2.01 load balancing cleanup. Delete this line
//					// agentlisttemp[i] = ifnx;                                   // {VB2J [600]}	                     agentlisttemp(i) = ifnx
//						}                                                          // {VB2J [601]}	                  End If
//					  }                                                            // {VB2J [602]}	               Next ifnjob
//
//					numagentlisttemp = i;                                          // {VB2J [604]}	               numagentlisttemp = i

					
// {VB2J [606]}	               'find an agent to unload
					for (ifnx=1; ifnx<=systemDesign_.nodeinfo[inod].numagents; ifnx++) {     // {VB2J [607]}	               For ifnx = 1 To nodeinfo(inod).numagents
					  ifn = systemDesign_.nodeinfo[inod].agentlist[ifnx];                        // {VB2J [608]}	                  ifn = nodeinfo(inod).agentlist(ifnx)

// {VB2J [610]}	                  'move to a server that is not overloaded
					  for (inod2=1; inod2<=systemDesign_.numnode; inod2++) {                 // {VB2J [612]}	                  For inod2 = 1 To numnode
						if (inod2 != inod && systemDesign_.nodeinfo[inod2].enabled)  {            // {VB2J [613]}	                     If (inod2 <> inod And nodeinfo(inod2).enabled) Then
						  if ((systemDesign_.nodeinfo[inod2].type == 1) && (systemDesign_.xnfunction[ifn].felig[inod2] == 1))  { // {VB2J [614]}	                        If ((nodeinfo(inod2).type = 1) And (xnfunction(ifn).felig(inod2) = 1)) Then
							if ((nodeutiltemp[inod2] + systemDesign_.xnfunction[ifn].cpurate[inod2]/systemDesign_.nodeinfo[inod2].cpucount + systemDesign_.nodeinfo[inod2].cpubackground/100 
							   < systemDesign_.nodeinfo[inod2].cpuutilmax / 100) 
							   && (nodememtemp[inod2] + systemDesign_.xnfunction[ifn].memory[inod2] < systemDesign_.nodeinfo[inod2].memory))  { 	// {VB2J [616]}	  If ( (nodeutiltemp(inod2) + xnfunction(ifn).cpurate(inod2) / nodeinfo(inod2).cpucount + nodeinfo(inod2).cpubackground / 100# < nodeinfo(inod2).cpuutilmax / 100#)	And (nodememtemp(inod2) + xnfunction(ifn).memory(inod2) < nodeinfo(inod2).memory)) Then

								systemDesign_.agent[ifn].testhostid = inod2;                       // {VB2J [618]}	                              agent(ifn).testhostid = inod2
							  checkmobility = true;                                // {VB2J [619]}	                              checkmobility = True
							  break;                                               // {VB2J [620]}	                              Exit For
							  }                                                    // {VB2J [621]}	                           End If
							}                                                      // {VB2J [622]}	                        End If
						  }                                                        // {VB2J [623]}	                     End If
						}                                                          // {VB2J [624]}	                  Next inod2

					  if (checkmobility) break;		                              // {VB2J [626]}	                  If (checkmobility) Then Exit For
					}                                                         	 // {VB2J [627]}	               Next ifnx

					  if (systemDesign_.numstatelist == 0)  {                                    // {VB2J [629]}	               If (numstatelist = 0) Then
 // {VB2J [630]}	                  'find minimum and average nodal violation:

// unused: float nodeoverloadave = 0;                                       // {VB2J [632]}	                  nodeoverloadave = 0
// unused: float nodeoverloadmax = 0;                                       // {VB2J [633]}	                  nodeoverloadmax = 0
float dcpu=0, dcpupct=0, dcputot=0, dmemorytot=0, dcpupctmax=0; 
float dmem=0, dmemorypct=0;

						for (int indx=1; indx<=systemDesign_.numnodeoverload; indx++) {          // {VB2J [635]}	                  For indx = 1 To numnodeoverload
						  inodx = systemDesign_.nodeoverload[indx].nodenum;                      // {VB2J [636]}	                     inodx = nodeoverload(indx).nodenum

						  dcpu = (float)(nodeutiltemp[inodx] + systemDesign_.nodeinfo[inodx].cpubackground / 100 - systemDesign_.nodeinfo[inodx].cpuutilmax / 100);              // {VB2J [638]}	                     dcpu = nodeutiltemp(inodx) + nodeinfo(inodx).cpubackground / 100# - nodeinfo(inodx).cpuutilmax / 100#
						  dcpupct = dcpu / systemDesign_.nodeinfo[inodx].cpucount;               // {VB2J [639]}	                     dcpupct = dcpu / nodeinfo(inodx).cpucount
						  dcputot = dcputot + dcpu;                                // {VB2J [640]}	                     dcputot = dcputot + dcpu
						  dmem = (float)(nodememtemp[inodx] - systemDesign_.nodeinfo[inodx].memory);              // {VB2J [641]}	                     dmem = nodememtemp(inodx) - nodeinfo(inodx).memory
						  dmemorypct = dmem / systemDesign_.nodeinfo[inodx].memory;              // {VB2J [642]}	                     dmemorypct = dmem / nodeinfo(inodx).memory
						  dmemorytot = dmemorytot + dmem;                          // {VB2J [643]}	                     dmemorytot = dmemorytot + dmem

						  if (dcpupct > dcpupctmax)  {                             // {VB2J [645]}	                     If (dcpupct > dcpupctmax) Then
							dcpupctmax = dcpupct;                                  // {VB2J [646]}	                        dcpupctmax = dcpupct
							}                                                      // {VB2J [647]}	                     End If

						  if (dmemorypct > systemDesign_.dmemorypctmax)  {                       // {VB2J [649]}	                     If (dmemorypct > dmemorypctmax) Then
							systemDesign_.dmemorypctmax = dmemorypct;                            // {VB2J [650]}	                        dmemorypctmax = dmemorypct
							}                                                      // {VB2J [651]}	                     End If

						  }                                                        // {VB2J [653]}	                  Next indx

// rec - unused:				float dcpupctave = dcputot / systemDesign_.nodeinfo[inodx].cpucount;              // {VB2J [655]}	                  dcpupctave = dcputot / nodeinfo(inodx).cpucount
// rec - unused:				float dmemorypctave = dmemorytot / systemDesign_.nodeinfo[inodx].memory;              // {VB2J [656]}	                  dmemorypctave = dmemorytot / nodeinfo(inodx).memory

						if (dcpupctmax > 0)  {                                     // {VB2J [658]}	                  If (dcpupctmax > 0) Then
						  if (dcpupctmax < systemDesign_.dcpupctmaxmin)  {                       // {VB2J [659]}	                     If (dcpupctmax < dcpupctmaxmin) Then
							systemDesign_.dcpupctmaxmin = dcpupctmax;                            // {VB2J [660]}	                        dcpupctmaxmin = dcpupctmax
							}                                                      // {VB2J [661]}	                     End If
						  } else if (dcpupctmax == 0 && systemDesign_.dmemorypctmax == 0)  {      // {VB2J [662]}	                  ElseIf (dcpupctmax = 0 And dmemorypctmax = 0) Then
						  if (dcpupctmax < systemDesign_.dcpupctmaxmin)  {                       // {VB2J [663]}	                     If (dcpupctmax < dcpupctmaxmin) Then
							systemDesign_.dcpupctmaxmin = dcpupctmax;                            // {VB2J [664]}	                        dcpupctmaxmin = dcpupctmax
							}                                                      // {VB2J [665]}	                     End If
						  }                                                        // {VB2J [666]}	                  End If

						if (systemDesign_.dmemorypctmax > 0)  {                                  // {VB2J [668]}	                  If (dmemorypctmax > 0) Then
						  if (systemDesign_.dmemorypctmax < systemDesign_.dmemorypctmaxmin)  {                 // {VB2J [669]}	                     If (dmemorypctmax < dmemorypctmaxmin) Then
							systemDesign_.dmemorypctmaxmin = systemDesign_.dmemorypctmax;                      // {VB2J [670]}	                        dmemorypctmaxmin = dmemorypctmax
							}                                                      // {VB2J [671]}	                     End If
						  } else if (systemDesign_.dmemorypctmax == 0 && dcpupctmax == 0)  {      // {VB2J [672]}	                  ElseIf (dmemorypctmax = 0 And dcpupctmax = 0) Then
						  if (systemDesign_.dmemorypctmax < systemDesign_.dmemorypctmaxmin)  {                 // {VB2J [673]}	                     If (dmemorypctmax < dmemorypctmaxmin) Then
							systemDesign_.dmemorypctmaxmin = systemDesign_.dmemorypctmax;                      // {VB2J [674]}	                        dmemorypctmaxmin = dmemorypctmax
							}                                                      // {VB2J [675]}	                     End If
						  }                                                        // {VB2J [676]}	                  End If

						}                                                          // {VB2J [678]}	               End If


// {VB2J [681]}	            ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
// {VB2J [682]}	            '''''''''''''''           ''''''''''''''''''''''''''''''''
// {VB2J [683]}	            ''''''''''''''' SMOOTHING ''''''''''''''''''''''''''''''''
// {VB2J [684]}	            '''''''''''''''           ''''''''''''''''''''''''''''''''
// {VB2J [685]}	            ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

					  } else if (rndtest < 0.7 && systemDesign_.pfailuresurvivescale > 0)  {     // {VB2J [687]}	            ElseIf (rndtest < 0.7 And pfailuresurvivescale > 0#) Then 'marc en 1.1 smoothing added
																				   // {VB2J [688]}	               'smooth pfailure with survivability (min(max(pfailure(inod)*numagents(inod))))

		int ilist = 0; //  'en 2.01 bug fix
					      
					  for (inod=1; inod<=systemDesign_.numnode; inod++) {                    // {VB2J [690]}	               For inod = 1 To numnode

		                    ilist++; // 'en 2.01 bug fix
		                    
						    //  pfailuresurvivelist[inod] = systemDesign_.nodeinfo[inod].pfail * systemDesign_.nodeinfo[inod].numagents;              // {VB2J [691]}	                  pfailuresurvivelist(inod) = nodeinfo(inod).pfail * nodeinfo(inod).numagents
						    //  pfailuresurvivelistid[inod] = inod;                        // {VB2J [692]}	                  pfailuresurvivelistid(inod) = inod

						      pfailuresurvivelist[ilist] = systemDesign_.nodeinfo[inod].pfail * systemDesign_.nodeinfo[inod].numagents;              // {VB2J [691]}	                  pfailuresurvivelist(inod) = nodeinfo(inod).pfail * nodeinfo(inod).numagents
						      pfailuresurvivelistid[ilist] = inod;                        // {VB2J [692]}	                  pfailuresurvivelistid(inod) = inod
}                                                          // {VB2J [693]}	               Next inod

						// 'en 2.01 bug fix														   // {VB2J [695]}	               'links are sorted from lowest pfailuresurvive to highest
					  	// numlistlong = systemDesign_.numnode;                                       // {VB2J [696]}	               numlistlong = numnode
					  	numlistlong = ilist;
					  
					  	quickSort_single_index(pfailuresurvivelist, pfailuresurvivelistid, 1, numlistlong);                                          // {VB2J [697]}	               QuickSort_single_index pfailuresurvivelist(), pfailuresurvivelistid(), 1, numlistlong

						//	 'en 2.01 bug fix		
					  	// int indx = pfailuresurvivelistid[systemDesign_.numnode];                       // {VB2J [699]}	               indx = pfailuresurvivelistid(numnode)
					  	int indx = pfailuresurvivelistid[numlistlong];                       // {VB2J [699]}               indx = pfailuresurvivelistid(numnode)
		
					  pfailuresurvivetmpmax = systemDesign_.nodeinfo[indx].pfail * systemDesign_.nodeinfo[indx].numagents;              // {VB2J [700]}	               pfailuresurvivetmpmax = nodeinfo(indx).pfail * nodeinfo(indx).numagents

					  if (pfailuresurvivetmpmax == 0)  {                           // {VB2J [702]}	               If (pfailuresurvivetmpmax = 0) Then
// {VB2J [703]}	                  'can only happen when all hosts that have have numagents > 0 have pfail = 0
						continue startmainloop;                                    // {VB2J [704]}	                  GoTo startmainloop
						}                                                          // {VB2J [705]}	               End If

					  numsurvivelist = 1;                                          // {VB2J [707]}	               numsurvivelist = 1
					  highrisknodes[numsurvivelist] = indx;                        // {VB2J [708]}	               highrisknodes(numsurvivelist) = indx

					  // 'en 2.01 bug fix
					  // for (ilist=systemDesign_.numnode-1; ilist>=1; ilist--) {         // {VB2J [710]}	               For ilist = numnode - 1 To 1 Step -1
					  for (ilist=numlistlong-1; ilist>=1; ilist--) {         // {VB2J [710]}	               For ilist = numnode - 1 To 1 Step -1

				      	indx = pfailuresurvivelistid[ilist];                       // {VB2J [711]}	                  indx = pfailuresurvivelistid(ilist)
				      	pfailuresurvivex = systemDesign_.nodeinfo[indx].pfail * systemDesign_.nodeinfo[indx].numagents;              // {VB2J [712]}	                  pfailuresurvivex = nodeinfo(indx).pfail * nodeinfo(indx).numagents
				      	if (pfailuresurvivex == pfailuresurvivetmpmax)  {          // {VB2J [713]}	                  If (pfailuresurvivex = pfailuresurvivetmpmax) Then
																			   // {VB2J [714]}	                     'pfailuresurvivetmpmax = pfailuresurvivex
						  numsurvivelist++;                     					// {VB2J [715]}	                     numsurvivelist = numsurvivelist + 1
						  highrisknodes[numsurvivelist] = indx;                    // {VB2J [716]}	                     highrisknodes(numsurvivelist) = indx
						  } else {                                                 // {VB2J [717]}	                  Else
							  break;                                                   // {VB2J [718]}	                     Exit For
							  }                                                        // {VB2J [719]}	                  End If
						}                                                          // {VB2J [720]}	               Next ilist

// rec - unused:					float pfailuresurvivehigh = pfailuresurvivetmpmax;                 // {VB2J [722]}	               pfailuresurvivehigh = pfailuresurvivetmpmax

					  // 'en 2.01 bug fix
					  // if (numsurvivelist == systemDesign_.numnode)  {                            // {VB2J [724]}	               If (numsurvivelist = numnode) Then
					  if (numsurvivelist == numlistlong)  {                            // {VB2J [724]}	               If (numsurvivelist = numnode) Then
// {VB2J [725]}	                  'all nodes have same high pfailuresurvive.
						continue startmainloop; 		                           // {VB2J [726]}	                  GoTo startmainloop
						}                                                          // {VB2J [727]}	               End If

					  
// {VB2J [729]}	               'now loop over high risk nodes, and move them to low risk nodes, greedy
					  for (indx=1; indx<=numsurvivelist; indx++) {             // {VB2J [730]}	               For indx = 1 To numsurvivelist
						inodx = highrisknodes[indx];                               // {VB2J [731]}	                  inodx = highrisknodes(indx)

// {VB2J [733]}	                  'find lowest pfailuresurvive, and move
// {VB2J [734]}	                  'sort the nodes by pfailuresurvive:

// {VB2J [736]}	                  'pick a random function to remove from host
// {VB2J [737]}	                  'Rnd returns a random number less than one, but greater than or equal to zero:

						int ifnranx = RnR.Int(systemDesign_.nodeinfo[inodx].numagents * RnR.Rnd() + 1);              // {VB2J [739]}	                  ifnranx = Int(nodeinfo(inodx).numagents * Rnd + 1)
						int ifnran = systemDesign_.nodeinfo[inodx].agentlist[ifnranx];               // {VB2J [740]}	                  ifnran = nodeinfo(inodx).agentlist(ifnranx)
						int ifnmove = ifnran;                                          // {VB2J [741]}	                  ifnmove = ifnran

						agentmoved = false;                                        // {VB2J [743]}	                  agentmoved = False
						
						// en 2.01 bug fix
						// for (indx2=1; indx2<=systemDesign_.numnode-numsurvivelist; indx2++) { // {VB2J [745]}	                  For indx2 = 1 To numnode - numsurvivelist
						for (indx2=1; indx2<=numlistlong-numsurvivelist; indx2++) { // {VB2J [745]}	                  For indx2 = 1 To numnode - numsurvivelist

						    int indtarg = pfailuresurvivelistid[indx2];                  // {VB2J [746]}	                     indtarg = pfailuresurvivelistid(indx2)

// check indtarg == 0 ??

						  if (systemDesign_.xnfunction[ifnmove].felig[indtarg] == 1)  {          // {VB2J [748]}	                     If (xnfunction(ifnmove).felig(indtarg) = 1) Then
							float pfailuresurvivetarg = (systemDesign_.nodeinfo[indtarg].numagents + 1) * systemDesign_.nodeinfo[indtarg].pfail;              // {VB2J [749]}	                        pfailuresurvivetarg = (nodeinfo(indtarg).numagents + 1) * nodeinfo(indtarg).pfail
// rec - unused:			float pfailuresurvivehighnew = (systemDesign_.nodeinfo[inodx].numagents - 1) * systemDesign_.nodeinfo[inodx].pfail;              // {VB2J [750]}	                        pfailuresurvivehighnew = (nodeinfo(inodx).numagents - 1) * nodeinfo(inodx).pfail
							float pfailuresurvivehighorig = systemDesign_.nodeinfo[inodx].numagents * systemDesign_.nodeinfo[inodx].pfail;              // {VB2J [751]}	                        pfailuresurvivehighorig = nodeinfo(inodx).numagents * nodeinfo(inodx).pfail
							if (pfailuresurvivetarg < pfailuresurvivehighorig)  {  // {VB2J [752]}	                        If (pfailuresurvivetarg < pfailuresurvivehighorig) Then
																				   // {VB2J [753]}	                           'nodeutiltemp(inodx) = nodeutiltemp(inodx) + xnfunction(ifnx).cpurate(inodx) / nodeinfo(inodx).cpucount
																				   // {VB2J [754]}	                           'nodememtemp(inodx) = nodememtemp(inodx) + xnfunction(ifnx).memory(inodx)
							  float nodeutiltarg = nodeutiltemp[indtarg] + systemDesign_.xnfunction[ifnmove].cpurate[indtarg] / systemDesign_.nodeinfo[indtarg].cpucount + systemDesign_.nodeinfo[indtarg].cpubackground / 100;              // {VB2J [755]}	                           nodeutiltarg = nodeutiltemp(indtarg) + xnfunction(ifnmove).cpurate(indtarg) / nodeinfo(indtarg).cpucount + nodeinfo(indtarg).cpubackground / 100#
							  float nodememtarg = nodememtemp[indtarg] + systemDesign_.xnfunction[ifnmove].memory[indtarg];              // {VB2J [756]}	                           nodememtarg = nodememtemp(indtarg) + xnfunction(ifnmove).memory(indtarg)

							  if (nodeutiltarg <= systemDesign_.nodeinfo[indtarg].cpuutilmax / 100 
							    && nodememtarg <= systemDesign_.nodeinfo[indtarg].memorymax)  { // {VB2J [758]}	                           If (nodeutiltarg <= nodeinfo(indtarg).cpuutilmax / 100# And nodememtarg <= nodeinfo(indtarg).memorymax) Then
								systemDesign_.agent[ifnmove].testhostid = indtarg;               // {VB2J [759]}	                              agent(ifnmove).testhostid = indtarg
								agentmoved = true;                                 // {VB2J [760]}	                              agentmoved = True
								systemDesign_.nodeinfo[inodx].numagents = systemDesign_.nodeinfo[inodx].numagents - 1;              // {VB2J [761]}	                              nodeinfo(inodx).numagents = nodeinfo(inodx).numagents - 1
								systemDesign_.nodeinfo[indtarg].numagents = systemDesign_.nodeinfo[indtarg].numagents + 1;              // {VB2J [762]}	                              nodeinfo(indtarg).numagents = nodeinfo(indtarg).numagents + 1
																				   // {VB2J [763]}	                              'update utilizations and pfailuresurvive etc:
								nodeutiltemp[indtarg] = nodeutiltarg;              // {VB2J [764]}	                              nodeutiltemp(indtarg) = nodeutiltarg
								nodememtemp[indtarg] = nodememtarg;                // {VB2J [765]}	                              nodememtemp(indtarg) = nodememtarg
								nodeutiltemp[inodx] = nodeutiltemp[inodx] - systemDesign_.xnfunction[ifnmove].cpurate[inodx] / systemDesign_.nodeinfo[inodx].cpucount;              // {VB2J [766]}	                              nodeutiltemp(inodx) = nodeutiltemp(inodx) - xnfunction(ifnmove).cpurate(inodx) / nodeinfo(inodx).cpucount
								nodememtemp[inodx] = nodememtemp[inodx] - systemDesign_.xnfunction[ifnmove].memory[inodx];              // {VB2J [767]}	                              nodememtemp(inodx) = nodememtemp(inodx) - xnfunction(ifnmove).memory(inodx)
								break;                                             // {VB2J [768]}	                              Exit For
								}                                                  // {VB2J [769]}	                           End If
							  }                                                    // {VB2J [770]}	                        End If
							}                                                      // {VB2J [771]}	                     End If
						  }                                                        // {VB2J [772]}	                  Next indx2

						if ( ! agentmoved)  {                                      // {VB2J [774]}	                  If (Not agentmoved) Then
																				   // {VB2J [775]}	                     'loop over remaining agents on server from randomly chosen point to end of list
						  for (int ifnrx=ifnranx+1; ifnrx<=systemDesign_.nodeinfo[inodx].numagents; ifnrx++) { // {VB2J [776]}	                     For ifnrx = ifnranx + 1 To nodeinfo(inodx).numagents
							ifnmove = systemDesign_.nodeinfo[inodx].agentlist[ifnrx];              // {VB2J [777]}	                        ifnmove = nodeinfo(inodx).agentlist(ifnrx)
																				   // {VB2J [778]}	                        'loop over nodes with lowest survivability
							
							// en 2.01 bug fix
							// for (indx2=1; indx2<=systemDesign_.numnode-numsurvivelist; indx2++) { // {VB2J [779]}	                        For indx2 = 1 To numnode - numsurvivelist
							for (indx2=1; indx2<=numlistlong-numsurvivelist; indx2++) { // {VB2J [779]}	                        For indx2 = 1 To numnode - numsurvivelist
							  int indtarg = pfailuresurvivelistid[indx2];              // {VB2J [780]}	                           indtarg = pfailuresurvivelistid(indx2)

							  if (systemDesign_.xnfunction[ifnmove].felig[indtarg] == 1)  {      // {VB2J [782]}	                           If (xnfunction(ifnmove).felig(indtarg) = 1) Then
								float pfailuresurvivetarg = (systemDesign_.nodeinfo[indtarg].numagents + 1) * systemDesign_.nodeinfo[indtarg].pfail;              // {VB2J [783]}	                              pfailuresurvivetarg = (nodeinfo(indtarg).numagents + 1) * nodeinfo(indtarg).pfail
// rec - unused:				float pfailuresurvivehighnew = (systemDesign_.nodeinfo[inodx].numagents - 1) * systemDesign_.nodeinfo[inodx].pfail;              // {VB2J [784]}	                              pfailuresurvivehighnew = (nodeinfo(inodx).numagents - 1) * nodeinfo(inodx).pfail
								float pfailuresurvivehighorig = systemDesign_.nodeinfo[inodx].numagents * systemDesign_.nodeinfo[inodx].pfail;              // {VB2J [785]}	                              pfailuresurvivehighorig = nodeinfo(inodx).numagents * nodeinfo(inodx).pfail

								if (pfailuresurvivetarg < pfailuresurvivehighorig)  { // {VB2J [787]}	                              If (pfailuresurvivetarg < pfailuresurvivehighorig) Then
								  float nodeutiltarg = nodeutiltemp[indtarg] + systemDesign_.xnfunction[ifnmove].cpurate[indtarg] / systemDesign_.nodeinfo[indtarg].cpucount + systemDesign_.nodeinfo[indtarg].cpubackground / 100;              // {VB2J [788]}	                                 nodeutiltarg = nodeutiltemp(indtarg) + xnfunction(ifnmove).cpurate(indtarg) / nodeinfo(indtarg).cpucount + nodeinfo(indtarg).cpubackground / 100#
								  float nodememtarg = nodememtemp[indtarg] + systemDesign_.xnfunction[ifnmove].memory[indtarg];              // {VB2J [789]}	                                 nodememtarg = nodememtemp(indtarg) + xnfunction(ifnmove).memory(indtarg)

								  if (nodeutiltarg <= systemDesign_.nodeinfo[indtarg].cpuutilmax / 100 
								    && nodememtarg <= systemDesign_.nodeinfo[indtarg].memorymax)  { 	// {VB2J [791]}	   If (nodeutiltarg <= nodeinfo(indtarg).cpuutilmax / 100# And nodememtarg <= nodeinfo(indtarg).memorymax) Then
									systemDesign_.agent[ifnmove].testhostid = indtarg;              // {VB2J [792]}	                                    agent(ifnmove).testhostid = indtarg
									agentmoved = true;                             // {VB2J [793]}	                                    agentmoved = True
									systemDesign_.nodeinfo[inodx].numagents--;              // {VB2J [794]}	                                    nodeinfo(inodx).numagents = nodeinfo(inodx).numagents - 1
									systemDesign_.nodeinfo[indtarg].numagents++;              // {VB2J [795]}	                                    nodeinfo(indtarg).numagents = nodeinfo(indtarg).numagents + 1
																				   // {VB2J [796]}	                                    'update utilizations and pfailuresurvive etc:
									nodeutiltemp[indtarg] = nodeutiltarg;              // {VB2J [797]}	                                    nodeutiltemp(indtarg) = nodeutiltarg
									nodememtemp[indtarg] = nodememtarg;              // {VB2J [798]}	                                    nodememtemp(indtarg) = nodememtarg
									nodeutiltemp[inodx] = nodeutiltemp[inodx] - systemDesign_.xnfunction[ifnmove].cpurate[inodx] / systemDesign_.nodeinfo[inodx].cpucount;              // {VB2J [799]}	                                    nodeutiltemp(inodx) = nodeutiltemp(inodx) - xnfunction(ifnmove).cpurate(inodx) / nodeinfo(inodx).cpucount
									nodememtemp[inodx] = nodememtemp[inodx] -systemDesign_. xnfunction[ifnmove].memory[inodx];              // {VB2J [800]}	                                    nodememtemp(inodx) = nodememtemp(inodx) - xnfunction(ifnmove).memory(inodx)
									break;                                         // {VB2J [801]}	                                    Exit For
									}                                              // {VB2J [802]}	                                 End If
								  }                                                // {VB2J [803]}	                              End If
								}                                                  // {VB2J [804]}	                           End If
							  }                                                    // {VB2J [805]}	                        Next indx2
							if (agentmoved) break;		                           // {VB2J [806]}	                        If (agentmoved) Then Exit For
							  }                                                    // {VB2J [807]}	                     Next ifnrx
							}                                                      // {VB2J [808]}	                  End If

						  if ( ! agentmoved)  {                                    // {VB2J [810]}	                  If (Not agentmoved) Then
																				   // {VB2J [811]}	                     'loop over remaining agents on server from randomly chosen point to end of list
							for (int ifnrx=1; ifnrx<=ifnranx-1; ifnrx++) {         // {VB2J [812]}	                     For ifnrx = 1 To ifnranx - 1
							  ifnmove = systemDesign_.nodeinfo[inodx].agentlist[ifnrx];              // {VB2J [813]}	                        ifnmove = nodeinfo(inodx).agentlist(ifnrx)
																				   // {VB2J [814]}	                        'loop over nodes with lowest survivability

							  // 'en 2.01 bug fix
							  // for (indx2=1; indx2<=systemDesign_.numnode-numsurvivelist; indx2++) { // {VB2J [816]}	                        For indx2 = 1 To numnode - numsurvivelist
							  	for (indx2=1; indx2<=numlistlong-numsurvivelist; indx2++) { // {VB2J [816]}	                        For indx2 = 1 To numnode - numsurvivelist
								int indtarg = pfailuresurvivelistid[indx2];              // {VB2J [817]}	                           indtarg = pfailuresurvivelistid(indx2)

								if (systemDesign_.xnfunction[ifnran].felig[indtarg] == 1)  {     // {VB2J [819]}	                           If (xnfunction(ifnran).felig(indtarg) = 1) Then
								  float pfailuresurvivetarg = (systemDesign_.nodeinfo[indtarg].numagents + 1) * systemDesign_.nodeinfo[indtarg].pfail;              // {VB2J [820]}	                              pfailuresurvivetarg = (nodeinfo(indtarg).numagents + 1) * nodeinfo(indtarg).pfail
//	rec - unused:					float pfailuresurvivehighnew = (systemDesign_.nodeinfo[inodx].numagents - 1) * systemDesign_.nodeinfo[inodx].pfail;              // {VB2J [821]}	                              pfailuresurvivehighnew = (nodeinfo(inodx).numagents - 1) * nodeinfo(inodx).pfail
								  float pfailuresurvivehighorig = systemDesign_.nodeinfo[inodx].numagents * systemDesign_.nodeinfo[inodx].pfail;              // {VB2J [822]}	                              pfailuresurvivehighorig = nodeinfo(inodx).numagents * nodeinfo(inodx).pfail

								  if (pfailuresurvivetarg < pfailuresurvivehighorig)  { // {VB2J [824]}	                              If (pfailuresurvivetarg < pfailuresurvivehighorig) Then
									float nodeutiltarg = nodeutiltemp[indtarg] + systemDesign_.xnfunction[ifnmove].cpurate[indtarg] / systemDesign_.nodeinfo[indtarg].cpucount + systemDesign_.nodeinfo[indtarg].cpubackground / 100;              // {VB2J [825]}	                                 nodeutiltarg = nodeutiltemp(indtarg) + xnfunction(ifnmove).cpurate(indtarg) / nodeinfo(indtarg).cpucount + nodeinfo(indtarg).cpubackground / 100#
									float nodememtarg = nodememtemp[indtarg] + systemDesign_.xnfunction[ifnmove].memory[indtarg];              // {VB2J [826]}	                                 nodememtarg = nodememtemp(indtarg) + xnfunction(ifnmove).memory(indtarg)

									if (nodeutiltarg <= systemDesign_.nodeinfo[indtarg].cpuutilmax / 100 
									  && nodememtarg <= systemDesign_.nodeinfo[indtarg].memorymax)  { // {VB2J [828]}	                                 If (nodeutiltarg <= nodeinfo(indtarg).cpuutilmax / 100# And nodememtarg <= nodeinfo(indtarg).memorymax) Then
									  systemDesign_.agent[ifnran].testhostid = indtarg;              // {VB2J [829]}	                                    agent(ifnran).testhostid = indtarg
									  agentmoved = true;                           // {VB2J [830]}	                                    agentmoved = True
									  systemDesign_.nodeinfo[inodx].numagents = systemDesign_.nodeinfo[inodx].numagents - 1;              // {VB2J [831]}	                                    nodeinfo(inodx).numagents = nodeinfo(inodx).numagents - 1
									  systemDesign_.nodeinfo[indtarg].numagents = systemDesign_.nodeinfo[indtarg].numagents + 1;              // {VB2J [832]}	                                    nodeinfo(indtarg).numagents = nodeinfo(indtarg).numagents + 1
																				   // {VB2J [833]}	                                    'update utilizations and pfailuresurvive etc:
									  nodeutiltemp[indtarg] = nodeutiltarg;              // {VB2J [834]}	                                    nodeutiltemp(indtarg) = nodeutiltarg
									  nodememtemp[indtarg] = nodememtarg;              // {VB2J [835]}	                                    nodememtemp(indtarg) = nodememtarg
									  nodeutiltemp[inodx] = nodeutiltemp[inodx] - systemDesign_.xnfunction[ifnmove].cpurate[inodx] / systemDesign_.nodeinfo[inodx].cpucount;              // {VB2J [836]}	                                    nodeutiltemp(inodx) = nodeutiltemp(inodx) - xnfunction(ifnmove).cpurate(inodx) / nodeinfo(inodx).cpucount
									  nodememtemp[inodx] = nodememtemp[inodx] - systemDesign_.xnfunction[ifnmove].memory[inodx];              // {VB2J [837]}	                                    nodememtemp(inodx) = nodememtemp(inodx) - xnfunction(ifnmove).memory(inodx)
									  break;                                       // {VB2J [838]}	                                    Exit For
									  }                                            // {VB2J [839]}	                                 End If
									}                                              // {VB2J [840]}	                              End If
								  }                                                // {VB2J [841]}	                           End If
								}                                                  // {VB2J [842]}	                        Next indx2
							  if (agentmoved) break;		                         // {VB2J [843]}	                        If (agentmoved) Then Exit For
								}                                                  // {VB2J [844]}	                     Next ifnrx
							  }                                                    // {VB2J [845]}	                  End If

							}                                                      // {VB2J [847]}	               Next indx

						  checkmobility = true;                                    // {VB2J [849]}	               checkmobility = True

// EN 2.09 line# 2777 						  

						  } else if (rndtest < 0.75 && systemDesign_.pfailuresurvivescale > 0)  { // {VB2J [854]}	            ElseIf (rndtest < 0.75 And pfailuresurvivescale > 0#) Then 'marc en 1.1 smoothing
// {VB2J [855]}	               'pick the server with lowest pfailuresurvive, and try to add a function to it
// {VB2J [856]}	               'try to offload the servers in order of highest pfailuresurvive

// {VB2J [858]}	               'find current loading of servers:
						  for (inodx=1; inodx<=systemDesign_.numnode; inodx++) {             // {VB2J [859]}	               For inodx = 1 To numnode
							nodeutiltemp[inodx] = 0;                               // {VB2J [860]}	                  nodeutiltemp(inodx) = 0
							nodememtemp[inodx] = 0;                                // {VB2J [861]}	                  nodememtemp(inodx) = 0
							}                                                      // {VB2J [862]}	               Next inodx

						  for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) { // {VB2J [864]}	               For ifnjob = 1 To objfnjobset.numobjfn
							ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                     // {VB2J [865]}	                  ifnx = objfnjobset.initfn(ifnjob)
							inodx = systemDesign_.agent[ifnx].testhostid;                        // {VB2J [866]}	                  inodx = agent(ifnx).testhostid
							nodeutiltemp[inodx] = nodeutiltemp[inodx] + systemDesign_.xnfunction[ifnx].cpurate[inodx] / systemDesign_.nodeinfo[inodx].cpucount;              // {VB2J [867]}	                  nodeutiltemp(inodx) = nodeutiltemp(inodx) + xnfunction(ifnx).cpurate(inodx) / nodeinfo(inodx).cpucount
							nodememtemp[inodx] = nodememtemp[inodx] + systemDesign_.xnfunction[ifnx].memory[inodx];              // {VB2J [868]}	                  nodememtemp(inodx) = nodememtemp(inodx) + xnfunction(ifnx).memory(inodx)
							}                                                      // {VB2J [869]}	               Next ifnjob

// {VB2J [871]}	               'find nodeinfo(inod).numagents:
						  for (inod=1; inod<=systemDesign_.numnode; inod++) {                // {VB2J [872]}	               For inod = 1 To numnode
							systemDesign_.nodeinfo[inod].numagents = 0;                          // {VB2J [873]}	                  nodeinfo(inod).numagents = 0
							pfailuresurvivelist[inod] = 0;                         // {VB2J [874]}	                  pfailuresurvivelist(inod) = 0
							}                                                      // {VB2J [875]}	               Next inod

						  for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) { // {VB2J [877]}	               For ifnjob = 1 To objfnjobset.numobjfn
							ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                     // {VB2J [878]}	                  ifnx = objfnjobset.initfn(ifnjob)
							inod = systemDesign_.agent[ifnx].testhostid;                         // {VB2J [879]}	                  inod = agent(ifnx).testhostid
							systemDesign_.nodeinfo[inod].numagents = systemDesign_.nodeinfo[inod].numagents + 1;              // {VB2J [880]}	                  nodeinfo(inod).numagents = nodeinfo(inod).numagents + 1
							systemDesign_.nodeinfo[inod].agentlist[systemDesign_.nodeinfo[inod].numagents] = ifnx;              // {VB2J [881]}	                  nodeinfo(inod).agentlist(nodeinfo(inod).numagents) = ifnx
							}                                                      // {VB2J [882]}	               Next ifnjob

						  int ilist = 0; // 'en 2.01 bug fix
						  for (inod=1; inod<=systemDesign_.numnode; inod++) {                // {VB2J [884]}	               For inod = 1 To numnode

						    ilist++; //  'en 2.01 bug fix

//						    pfailuresurvivelist[inod] = systemDesign_.nodeinfo[inod].pfail * systemDesign_.nodeinfo[inod].numagents;              // {VB2J [885]}	                  pfailuresurvivelist(inod) = nodeinfo(inod).pfail * nodeinfo(inod).numagents
//							pfailuresurvivelistid[inod] = inod;                    // {VB2J [886]}	                  pfailuresurvivelistid(inod) = inod
							pfailuresurvivelist[ilist] = systemDesign_.nodeinfo[inod].pfail * systemDesign_.nodeinfo[inod].numagents;              // {VB2J [885]}	                  pfailuresurvivelist(inod) = nodeinfo(inod).pfail * nodeinfo(inod).numagents
							pfailuresurvivelistid[ilist] = inod;                    // {VB2J [886]}	                  pfailuresurvivelistid(inod) = inod
							}                                                      // {VB2J [887]}	               Next inod

						  
// {VB2J [889]}	               'links are sorted from lowest pfailuresurvive to highest
						  // 'en 2.01 bug fix
						  // numlistlong = systemDesign_.numnode;                                   // {VB2J [890]}	               numlistlong = numnode
						  numlistlong = ilist;
			
					quickSort_single_index(pfailuresurvivelist, pfailuresurvivelistid, 1, numlistlong);    // {VB2J [891]}	               QuickSort_single_index pfailuresurvivelist(), pfailuresurvivelistid(), 1, numlistlong

					numsurvivelist = 0;                                      // {VB2J [893]}	               numsurvivelist = 0 'list of nodes that have max survivability value pfailuresurvivetmp
// {VB2J [894]}	               'we will remove one function from each member of the list, and move to the hosts with good survivability to improve our survivability

// EN2.09: unused:  float pfailuresurvivetmpmin = Float.MAX_VALUE; // 9E+19;                           // {VB2J [896]}	               pfailuresurvivetmpmin = 9E+19
// {VB2J [897]}	               'loop over the list pfailuresurviveid until you find a low risk node that can receive an agent from a higher risk node

						  agentmoved = false;                                      // {VB2J [899]}	               agentmoved = False

						  // 'en 2.01 bug fix
						  // for (int indxlow=1; indxlow<=systemDesign_.numnode; indxlow++) {       // {VB2J [901]}	               For indxlow = 1 To numnode
							 for (int indxlow=1; indxlow<=numlistlong; indxlow++) {       // {VB2J [901]}	               For indxlow = 1 To numnode
																				   // {VB2J [902]}	                  'we will move agents from the high risk node to the low risk node
																				   // {VB2J [903]}	                  'the low risk node is the target of the move
							int indtarg = pfailuresurvivelistid[indxlow];              // {VB2J [904]}	                  indtarg = pfailuresurvivelistid(indxlow)

																				   // {VB2J [906]}	                  'loop over the highest risk nodes
							
							//  'en 2.01 bug fix
							// for (int indxhi=systemDesign_.numnode; indxhi>=(indxlow+1); indxhi--) { // {VB2J [907]}	                  For indxhi = numnode To indxlow + 1 Step -1
							for (int indxhi=numlistlong; indxhi>=(indxlow+1); indxhi--) { // {VB2J [907]}	                  For indxhi = numnode To indxlow + 1 Step -1

							    int indsrc = pfailuresurvivelistid[indxhi];              // {VB2J [908]}	                     indsrc = pfailuresurvivelistid(indxhi)
// check indsrc == 0??

							  if ((systemDesign_.nodeinfo[indsrc].numagents == 0) 
							   || (systemDesign_.nodeinfo[indsrc].type != 1))  { // {VB2J [909]}	                     If ((nodeinfo(indsrc).numagents = 0) Or (nodeinfo(indsrc).type <> 1)) Then
								break;                                             // {VB2J [910]}	                        Exit For
								}                                                  // {VB2J [911]}	                     End If

	// 29April04 - assuming it's this line that throws the NPE, check it
	if (systemDesign_ == null  ||
		systemDesign_.nodeinfo[indsrc] == null ||
		systemDesign_.nodeinfo[indtarg] == null) {
			
		if (systemDesign_.isWarnEnabled()) {
			SystemDesign.logWarn("AnnealForm.getRandomState(): NPE averted?");
			SystemDesign.logWarn("AnnealForm.getRandomState():  indsrc=" + indsrc + "; systemDesign_.nodeinfo[indsrc]=" + systemDesign_.nodeinfo[indsrc]);
			SystemDesign.logWarn("AnnealForm.getRandomState():  indtarg=" + indtarg + "; systemDesign_.nodeinfo[indtarg]=" + systemDesign_.nodeinfo[indtarg]);
			}
		break;
		} // 29April04 null checks

// {VB2J [912]}	                     'list is sorted, so exit if done
							  if (systemDesign_.nodeinfo[indsrc].numagents * systemDesign_.nodeinfo[indsrc].pfail < systemDesign_.nodeinfo[indtarg].numagents * systemDesign_.nodeinfo[indtarg].pfail)  { // {VB2J [913]}	                     If (nodeinfo(indsrc).numagents * nodeinfo(indsrc).pfail < nodeinfo(indtarg).numagents * nodeinfo(indtarg).pfail) Then
// {VB2J [914]}	                        'can't move agents off of node indxlow, so try a new indxlow

						// this was line 3538 29April04; source of NPE - in a break?!?
						//
								break;                                             // {VB2J [915]}	                        Exit For
								}                                                  // {VB2J [916]}	                     End If
// {VB2J [917]}	                     'see if any of the functions on indsource can be moved to indtarg
// {VB2J [918]}	                     'pick a random point in the list to start the search:
							  int ifnranx = RnR.Int(systemDesign_.nodeinfo[indsrc].numagents * RnR.Rnd()+ 1);              // {VB2J [919]}	                     ifnranx = Int(nodeinfo(indsrc).numagents * Rnd + 1)
							  int ifnran = systemDesign_.nodeinfo[indsrc].agentlist[ifnranx];              // {VB2J [920]}	                     ifnran = nodeinfo(indsrc).agentlist(ifnranx)
							  int ifnmove = ifnran;                                    // {VB2J [921]}	                     ifnmove = ifnran
							  if (systemDesign_.xnfunction[ifnmove].felig[indtarg] == 1)  {      // {VB2J [922]}	                     If (xnfunction(ifnmove).felig(indtarg) = 1) Then
								float pfailuresurvivetarg = (systemDesign_.nodeinfo[indtarg].numagents + 1) *systemDesign_. nodeinfo[indtarg].pfail;              // {VB2J [923]}	                        pfailuresurvivetarg = (nodeinfo(indtarg).numagents + 1) * nodeinfo(indtarg).pfail
//	rec - unused:				float pfailuresurvivehighnew = (systemDesign_.nodeinfo[indsrc].numagents - 1) * systemDesign_.nodeinfo[indsrc].pfail;              // {VB2J [924]}	                        pfailuresurvivehighnew = (nodeinfo(indsrc).numagents - 1) * nodeinfo(indsrc).pfail
								float pfailuresurvivehighorig = systemDesign_.nodeinfo[indsrc].numagents * systemDesign_.nodeinfo[indsrc].pfail;              // {VB2J [925]}	                        pfailuresurvivehighorig = nodeinfo(indsrc).numagents * nodeinfo(indsrc).pfail
								if (pfailuresurvivetarg < pfailuresurvivehighorig)  { // {VB2J [926]}	                        If (pfailuresurvivetarg < pfailuresurvivehighorig) Then
								  double nodeutiltarg = nodeutiltemp[indtarg] + systemDesign_.xnfunction[ifnmove].cpurate[indtarg] / systemDesign_.nodeinfo[indtarg].cpucount + systemDesign_.nodeinfo[indtarg].cpubackground / 100;              // {VB2J [927]}	                           nodeutiltarg = nodeutiltemp(indtarg) + xnfunction(ifnmove).cpurate(indtarg) / nodeinfo(indtarg).cpucount + nodeinfo(indtarg).cpubackground / 100#
								  double nodememtarg = nodememtemp[indtarg] + systemDesign_.xnfunction[ifnmove].memory[indtarg];              // {VB2J [928]}	                           nodememtarg = nodememtemp(indtarg) + xnfunction(ifnmove).memory(indtarg)
								  if (nodeutiltarg <= systemDesign_.nodeinfo[indtarg].cpuutilmax / 100 
								    && nodememtarg <= systemDesign_.nodeinfo[indtarg].memorymax)  { // {VB2J [929]}	                           If (nodeutiltarg <= nodeinfo(indtarg).cpuutilmax / 100# And nodememtarg <= nodeinfo(indtarg).memorymax) Then
									systemDesign_.agent[ifnmove].testhostid = indtarg;              // {VB2J [930]}	                              agent(ifnmove).testhostid = indtarg
									agentmoved = true;                             // {VB2J [931]}	                              agentmoved = True
									systemDesign_.nodeinfo[indsrc].numagents--;              // {VB2J [932]}	                              nodeinfo(indsrc).numagents = nodeinfo(indsrc).numagents - 1
									systemDesign_.nodeinfo[indtarg].numagents++;              // {VB2J [933]}	                              nodeinfo(indtarg).numagents = nodeinfo(indtarg).numagents + 1
									return;                                        // {VB2J [934]}	                              Exit Sub
									}                                              // {VB2J [935]}	                           End If
								  }                                                // {VB2J [936]}	                        End If
								}                                                  // {VB2J [937]}	                     End If
																				   // {VB2J [938]}	                     'now loop over remaining functions on indsrc, and try to move one of them
							  for (int ifnrx=ifnranx+1; ifnrx<=systemDesign_.nodeinfo[indsrc].numagents; ifnrx++) { // {VB2J [939]}	                     For ifnrx = ifnranx + 1 To nodeinfo(indsrc).numagents
								ifnmove = systemDesign_.nodeinfo[indsrc].agentlist[ifnrx];              // {VB2J [940]}	                        ifnmove = nodeinfo(indsrc).agentlist(ifnrx)
								if (systemDesign_.xnfunction[ifnmove].felig[indtarg] == 1)  {    // {VB2J [941]}	                        If (xnfunction(ifnmove).felig(indtarg) = 1) Then
								  double pfailuresurvivetarg = (systemDesign_.nodeinfo[indtarg].numagents + 1) * systemDesign_.nodeinfo[indtarg].pfail;              // {VB2J [942]}	                           pfailuresurvivetarg = (nodeinfo(indtarg).numagents + 1) * nodeinfo(indtarg).pfail
// rec - unused:	  		double pfailuresurvivehighnew = (systemDesign_.nodeinfo[indsrc].numagents - 1) * systemDesign_.nodeinfo[indsrc].pfail;              // {VB2J [943]}	                           pfailuresurvivehighnew = (nodeinfo(indsrc).numagents - 1) * nodeinfo(indsrc).pfail
								  double pfailuresurvivehighorig = systemDesign_.nodeinfo[indsrc].numagents * systemDesign_.nodeinfo[indsrc].pfail;              // {VB2J [944]}	                           pfailuresurvivehighorig = nodeinfo(indsrc).numagents * nodeinfo(indsrc).pfail
								  if (pfailuresurvivetarg < pfailuresurvivehighorig)  { // {VB2J [945]}	                           If (pfailuresurvivetarg < pfailuresurvivehighorig) Then
									double nodeutiltarg = nodeutiltemp[indtarg] + systemDesign_.xnfunction[ifnmove].cpurate[indtarg] /systemDesign_. nodeinfo[indtarg].cpucount + systemDesign_.nodeinfo[indtarg].cpubackground / 100;              // {VB2J [946]}	                              nodeutiltarg = nodeutiltemp(indtarg) + xnfunction(ifnmove).cpurate(indtarg) / nodeinfo(indtarg).cpucount + nodeinfo(indtarg).cpubackground / 100#
									double nodememtarg = nodememtemp[indtarg] + systemDesign_.xnfunction[ifnmove].memory[indtarg];              // {VB2J [947]}	                              nodememtarg = nodememtemp(indtarg) + xnfunction(ifnmove).memory(indtarg)
									if (nodeutiltarg <= systemDesign_.nodeinfo[indtarg].cpuutilmax / 100 
									  && nodememtarg <= systemDesign_.nodeinfo[indtarg].memorymax)  { // {VB2J [948]}	                              If (nodeutiltarg <= nodeinfo(indtarg).cpuutilmax / 100# And nodememtarg <= nodeinfo(indtarg).memorymax) Then
									  systemDesign_.agent[ifnmove].testhostid = indtarg;              // {VB2J [949]}	                                 agent(ifnmove).testhostid = indtarg
									  agentmoved = true;                           // {VB2J [950]}	                                 agentmoved = True
									  systemDesign_.nodeinfo[indsrc].numagents--;              // {VB2J [951]}	                                 nodeinfo(indsrc).numagents = nodeinfo(indsrc).numagents - 1
									  systemDesign_.nodeinfo[indtarg].numagents++;              // {VB2J [952]}	                                 nodeinfo(indtarg).numagents = nodeinfo(indtarg).numagents + 1
									  return;                                      // {VB2J [953]}	                                 Exit Sub
									  }                                            // {VB2J [954]}	                              End If
									}                                              // {VB2J [955]}	                           End If
								  }                                                // {VB2J [956]}	                        End If
								}                                                  // {VB2J [957]}	                     Next ifnrx

							  for (int ifnrx=1; ifnrx<=ifnranx-1; ifnrx++) {       // {VB2J [959]}	                     For ifnrx = 1 To ifnranx - 1
								ifnmove = systemDesign_.nodeinfo[indsrc].agentlist[ifnrx];              // {VB2J [960]}	                        ifnmove = nodeinfo(indsrc).agentlist(ifnrx)
								if (systemDesign_.xnfunction[ifnmove].felig[indtarg] == 1)  {    // {VB2J [961]}	                        If (xnfunction(ifnmove).felig(indtarg) = 1) Then
								  double pfailuresurvivetarg = (systemDesign_.nodeinfo[indtarg].numagents + 1) * systemDesign_.nodeinfo[indtarg].pfail;              // {VB2J [962]}	                           pfailuresurvivetarg = (nodeinfo(indtarg).numagents + 1) * nodeinfo(indtarg).pfail
//	rec - unused:			  		double pfailuresurvivehighnew = (systemDesign_.nodeinfo[indsrc].numagents - 1) * systemDesign_.nodeinfo[indsrc].pfail;              // {VB2J [963]}	                           pfailuresurvivehighnew = (nodeinfo(indsrc).numagents - 1) * nodeinfo(indsrc).pfail
								  double pfailuresurvivehighorig = systemDesign_.nodeinfo[indsrc].numagents * systemDesign_.nodeinfo[indsrc].pfail;              // {VB2J [964]}	                           pfailuresurvivehighorig = nodeinfo(indsrc).numagents * nodeinfo(indsrc).pfail
								  if (pfailuresurvivetarg < pfailuresurvivehighorig)  { // {VB2J [965]}	                           If (pfailuresurvivetarg < pfailuresurvivehighorig) Then
									double nodeutiltarg = nodeutiltemp[indtarg] + systemDesign_.xnfunction[ifnmove].cpurate[indtarg] / systemDesign_.nodeinfo[indtarg].cpucount + systemDesign_.nodeinfo[indtarg].cpubackground / 100;              // {VB2J [966]}	                              nodeutiltarg = nodeutiltemp(indtarg) + xnfunction(ifnmove).cpurate(indtarg) / nodeinfo(indtarg).cpucount + nodeinfo(indtarg).cpubackground / 100#
									double nodememtarg = nodememtemp[indtarg] + systemDesign_.xnfunction[ifnmove].memory[indtarg];              // {VB2J [967]}	                              nodememtarg = nodememtemp(indtarg) + xnfunction(ifnmove).memory(indtarg)
									if (nodeutiltarg <= systemDesign_.nodeinfo[indtarg].cpuutilmax / 100 
									  && nodememtarg <= systemDesign_.nodeinfo[indtarg].memorymax)  { // {VB2J [968]}	                              If (nodeutiltarg <= nodeinfo(indtarg).cpuutilmax / 100# And nodememtarg <= nodeinfo(indtarg).memorymax) Then
										systemDesign_.agent[ifnmove].testhostid = indtarg;              // {VB2J [969]}	                                 agent(ifnmove).testhostid = indtarg
									  agentmoved = true;                           // {VB2J [970]}	                                 agentmoved = True
									  systemDesign_.nodeinfo[indsrc].numagents--;              // {VB2J [971]}	                                 nodeinfo(indsrc).numagents = nodeinfo(indsrc).numagents - 1
									  systemDesign_.nodeinfo[indtarg].numagents++;              // {VB2J [972]}	                                 nodeinfo(indtarg).numagents = nodeinfo(indtarg).numagents + 1
									  return;                                      // {VB2J [973]}	                                 Exit Sub
									  }                                            // {VB2J [974]}	                              End If
									}                                              // {VB2J [975]}	                           End If
								  }                                                // {VB2J [976]}	                        End If
								}                                                  // {VB2J [977]}	                     Next ifnrx
							  }                                                    // {VB2J [979]}	                  Next indxhi
							}                                                      // {VB2J [981]}	               Next indxlow



// EN2.20 - begin new code block
//
						  } else if (rndtest < 0.75 && systemDesign_.remotetrafficscale > 0)  {  // {VB2J [986]}	            ElseIf (rndtest < 0.85 And remotetrafficscale > 0#) Then 'marc en 1.1 smoothing w/messaging

				               if (RnR.Rnd() < 0.1) {

				                 int ifnmax = 0;	// ???

				                  for (ifn=1; ifn<=systemDesign_.numfunction; ifn++) {
				                     totfncall[ifn] = 0f;
				                  	} // Next ifn

				                  // 'calculate interhost messaging:
								for (ifn=1; ifn<=systemDesign_.numfunction; ifn++) {
									if (systemDesign_.xnfunction[ifn].objfn && systemDesign_.xnfunction[ifn].nodelist[0] > 1) {
										for (ifn2 = 1; ifn2 <= systemDesign_.numfunction; ifn2++) {
											if (ifn2 != ifn && systemDesign_.xnfunction[ifn2].objfn) {
												if (systemDesign_.agent[ifn].testhostid != systemDesign_.agent[ifn2].testhostid) {
													totfncall[ifn] += systemDesign_.xnfunction[ifn].fcall[ifn2].sendmsgrate;
													totfncall[ifn] += systemDesign_.xnfunction[ifn2].fcall[ifn].sendmsgrate;
													}
												}
											} // Next ifn2
										} //  End If
									} //  Next ifn

									  // 'calculate probabilities of selection for high interhost messaging function:
									float totfncalltot = 0f;
									for (ifn=1; ifn<=systemDesign_.numfunction; ifn++) {
										totfncalltot += totfncall[ifn];
										} // Next ifn
									  
									for (ifn = 1; ifn <= systemDesign_.numfunction; ifn++) {
										totfncall[ifn] = totfncall[ifn] / totfncalltot;
										} // Next ifn
									  
									for (ifn=2; ifn<=systemDesign_.numfunction; ifn++) {
										totfncall[ifn] += totfncall[ifn - 1];
										} // Next ifn

				                  float rndfn = RnR.Rnd();
				                  boolean ifoundfn = false;
									for (ifn2=1; ifn2<=systemDesign_.numfunction; ifn2++) {
										if (systemDesign_.xnfunction[ifn2].objfn) {
											ifnmax = ifn2;
											if (rndfn < totfncall[ifn2]) {
												ifn = ifn2;
												ifoundfn = true;
												break; // Exit For
												} // End If
											} //       End If
										} //    Next ifn2
					                  
				                  if (ifoundfn == false) {
				                     ifn = ifnmax;
				                  	} //     End If

				                  // 'en 2.18 end of new code block

				               }
				               else {

//						// EN2.20 - end new code block

				                  // 'en 2.18 this was the old code block, which is now the else case for the above mods
				                  // 'en 2.18 basically, paste the above code block with if statement on top of the code block below
																				   		// {VB2J [987]}	               'marc en 1.1 smoothing pfailure with remotetraffic

						  int ind = 0;                                                 // {VB2J [989]}	               ind = 0

						  for (inodx=1; inodx<=systemDesign_.numnode; inodx++) {             // {VB2J [991]}	               For inodx = 1 To numnode
							if ((nodeutiltemp[inodx] + systemDesign_.nodeinfo[inodx].cpubackground/100 > systemDesign_.nodeinfo[inodx].cpuutilmax/100) 
							 || (nodememtemp[inodx] > systemDesign_.nodeinfo[inodx].memory))  { // {VB2J [993]}	                  If ((nodeutiltemp(inodx) + nodeinfo(inodx).cpubackground / 100# > nodeinfo(inodx).cpuutilmax / 100#)                      Or (nodememtemp(inodx) > nodeinfo(inodx).memory)) Then

							  ind++;                                       // {VB2J [995]}	                     ind = ind + 1
							  systemDesign_.nodeoverload[ind].nodenum = inodx;                   // {VB2J [996]}	                     nodeoverload(ind).nodenum = inodx
							  systemDesign_.nodeoverload[ind].cpuoverload = (float)nodeutiltemp[inodx] + systemDesign_.nodeinfo[inodx].cpubackground / 100 - systemDesign_.nodeinfo[inodx].cpuutilmax / 100;              // {VB2J [997]}	                     nodeoverload(ind).cpuoverload = nodeutiltemp(inodx) + nodeinfo(inodx).cpubackground / 100# - nodeinfo(inodx).cpuutilmax / 100#
							  systemDesign_.nodeoverload[ind].memoverload = (float)nodememtemp[inodx] - systemDesign_.nodeinfo[inodx].memory;              // {VB2J [998]}	                     nodeoverload(ind).memoverload = nodememtemp(inodx) - nodeinfo(inodx).memory
							  }                                                    // {VB2J [999]}	                  End If
							}                                                      // {VB2J [1000]}	               Next inodx

						  systemDesign_.numnodeoverload = ind;                                   // {VB2J [1002]}	               numnodeoverload = ind

						  if (systemDesign_.numnodeoverload == 0)  {                             // {VB2J [1004]}	               If (numnodeoverload = 0) Then
							int indx = RnR.Int(usednodestemp[0] * RnR.Rnd() + 1);                // {VB2J [1005]}	                  indx = Int(usednodestemp(0) * Rnd + 1)
							inod = usednodestemp[indx];                            // {VB2J [1006]}	                  inod = usednodestemp(indx)
							} else {                                               // {VB2J [1007]}	               Else
																				   // {VB2J [1008]}	                  'move a random function off of this node
							inodrunx = RnR.Int(systemDesign_.numnodeoverload * RnR.Rnd() + 1);             // {VB2J [1009]}	                  inodrunx = Int(numnodeoverload * Rnd + 1)
							inod = systemDesign_.nodeoverload[inodrunx].nodenum;                 // {VB2J [1010]}	                  inod = nodeoverload(inodrunx).nodenum
							}                                                      // {VB2J [1011]}	               End If

						  ifx = RnR.Int(systemDesign_.nodeinfo[inod].numagents * RnR.Rnd() + 1);              // {VB2J [1013]}	               ifx = Int(nodeinfo(inod).numagents * Rnd + 1)
						  ifn = systemDesign_.nodeinfo[inod].agentlist[ifx];                     // {VB2J [1014]}	               ifn = nodeinfo(inod).agentlist(ifx)

						  } // EN2.20 end old code block

																				   // {VB2J [1016]}	               'move agent ifn from node inod to node inod2 and ifn2
						  if (systemDesign_.xnfunction[ifn].nodelist[0] > 1)  { // NPE 22sept03                 // {VB2J [1017]}	               If (xnfunction(ifn).nodelist(0) > 1) Then
							for (indx2=1; indx2<=systemDesign_.xnfunction[ifn].nodelist[0]; indx2++) { // {VB2J [1018]}	                  For indx2 = 1 To xnfunction(ifn).nodelist(0)
							  totmessageifn[indx2] = 0;                            // {VB2J [1019]}	                     totmessageifn(indx2) = 0
							  }                                                    // {VB2J [1020]}	                  Next indx2

							for (indx2=1; indx2<=systemDesign_.xnfunction[ifn].nodelist[0]; indx2++) { // {VB2J [1022]}	                  For indx2 = 1 To xnfunction(ifn).nodelist(0)
																				   // {VB2J [1023]}	                     'loop over each target node, and find target state remote traffic for each node candidate
							  inod2 = systemDesign_.xnfunction[ifn].nodelist[indx2];             // {VB2J [1024]}	                     inod2 = xnfunction(ifn).nodelist(indx2)
							  totmessageifnid[indx2] = inod2;                      // {VB2J [1025]}	                     totmessageifnid(indx2) = inod2
							  for (ifn2=1; ifn2<=systemDesign_.numfunction; ifn2++) {        // {VB2J [1026]}	                     For ifn2 = 1 To numfunction
								if (ifn2 != ifn && systemDesign_.xnfunction[ifn2].objfn)  {       // {VB2J [1027]}	                        If (ifn2 <> ifn And xnfunction(ifn2).objfn) Then
								  if (systemDesign_.agent[ifn2].testhostid != inod2)  {          // {VB2J [1028]}	                           If (agent(ifn2).testhostid <> inod2) Then
									totmessageifn[indx2] = totmessageifn[indx2] + systemDesign_.xnfunction[ifn].fcall[ifn2].sendmsgrate;              // {VB2J [1029]}	                              totmessageifn(indx2) = totmessageifn(indx2) + xnfunction(ifn).fcall(ifn2).sendmsgrate
									totmessageifn[indx2] = totmessageifn[indx2] + systemDesign_.xnfunction[ifn2].fcall[ifn].sendmsgrate;              // {VB2J [1030]}	                              totmessageifn(indx2) = totmessageifn(indx2) + xnfunction(ifn2).fcall(ifn).sendmsgrate
									}                                              // {VB2J [1031]}	                           End If
								  }                                                // {VB2J [1032]}	                        End If
								}                                                  // {VB2J [1033]}	                     Next ifn2
							  }                                                    // {VB2J [1034]}	                  Next indx2


								int inodlo = 0;

				                  // EN2.20 - 'en 2.18 improve remote messaging, add new code block below, with if statement, before old code block
				                  if (RnR.Rnd() < 0.05) {

				                     // 'move ifn to a server selected from a probabilistic list of servers with low interhost traffic
				                  
				                     int tottotmessageifnid = 0;
				                     for (indx2=1; indx2<=systemDesign_.xnfunction[ifn].nodelist[0]; indx2++) {
				                        tottotmessageifnid += totmessageifn[indx2];
				                     	}
				                  
				                     // 'target nodes with low messaging should be preferentially sampled
				                     for (indx2=1; indx2<=systemDesign_.xnfunction[ifn].nodelist[0]; indx2++) {
				                        // 'totmessageifn(indx2) = 1# - totmessageifn(indx2) / tottotmessageifnid
				                        totmessageifn[indx2] = (1 - totmessageifn[indx2] / tottotmessageifnid) / (systemDesign_.xnfunction[ifn].nodelist[0] - 1);
				                     	}

				                     for (indx2=2; indx2<=systemDesign_.xnfunction[ifn].nodelist[0]; indx2++) {
				                        totmessageifn[indx2] = totmessageifn[indx2] + totmessageifn[indx2 - 1];
				                     	}
				                  
				                     float rndnod = RnR.Rnd();
				                     boolean ifoundnod = false;
				                     for (indx2=1; indx2<=systemDesign_.xnfunction[ifn].nodelist[0]; indx2++) {
				                        if (rndnod < totmessageifn[indx2]) {
				                           inodlo = systemDesign_.xnfunction[ifn].nodelist[indx2];
				                           ifoundnod = true;
				                           break; // Exit For
				                        } // End If
				                     } // Next indx2
				                     
				                     if (ifoundnod == false) {
				                        inodlo = systemDesign_.xnfunction[ifn].nodelist[systemDesign_.xnfunction[ifn].nodelist[0]];
				                     }
				                     
				                     // 'en 2.18 end of new code block
				                     // 'en 2.18 this section below is the old code block

							}
							else {
																				   // {VB2J [1036]}	                  'links are sorted from lowest pfailuresurvive to highest
								numlistlong = systemDesign_.xnfunction[ifn].nodelist[0];             // {VB2J [1037]}	                  numlistlong = xnfunction(ifn).nodelist(0)
								quickSort_single_index(totmessageifn, totmessageifnid, 1, numlistlong);                                    // {VB2J [1038]}	                  QuickSort_single_index totmessageifn(), totmessageifnid(), 1, numlistlong
								inodlo = totmessageifnid[1];                           // {VB2J [1040]}	                  inodlo = totmessageifnid(1)

								} // end 2.20 new 'else' wrapping old code
							
							systemDesign_.agent[ifn].testhostid = inodlo;                        // {VB2J [1041]}	                  agent(ifn).testhostid = inodlo
							checkmobility = true;                                  // {VB2J [1042]}	                  checkmobility = True
							} else {                                               // {VB2J [1043]}	               Else
// {VB2J [1044]}	                  'marc en 1.1 comment if nodelist(0)=1, then only one server in list, no move possible
// {VB2J [1045]}	                  'checkmobility = True
							}                                                      // {VB2J [1046]}	               End If


						  } else if (rndtest < 0.95 && systemDesign_.hammingscale > 0) {        // {VB2J [1049]}	            ElseIf (rndtest < 0.95 And hammingscale > 0#) Then  'marc en 1.1 hamming
// {VB2J [1050]}	               'pick an agent and move it as close as possible to target state
// {VB2J [1052]}	               'find current loading of servers:
						  for (inodx=1; inodx<=systemDesign_.numnode; inodx++) {             // {VB2J [1053]}	               For inodx = 1 To numnode
							nodeutiltemp[inodx] = 0;                               // {VB2J [1054]}	                  nodeutiltemp(inodx) = 0
							nodememtemp[inodx] = 0;                                // {VB2J [1055]}	                  nodememtemp(inodx) = 0
							}                                                      // {VB2J [1056]}	               Next inodx

						  for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) { // {VB2J [1058]}	               For ifnjob = 1 To objfnjobset.numobjfn
							ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                     // {VB2J [1059]}	                  ifnx = objfnjobset.initfn(ifnjob)
							inodx = systemDesign_.agent[ifnx].testhostid;                        // {VB2J [1060]}	                  inodx = agent(ifnx).testhostid
							nodeutiltemp[inodx] = nodeutiltemp[inodx] + systemDesign_.xnfunction[ifnx].cpurate[inodx] / systemDesign_.nodeinfo[inodx].cpucount;              // {VB2J [1061]}	                  nodeutiltemp(inodx) = nodeutiltemp(inodx) + xnfunction(ifnx).cpurate(inodx) / nodeinfo(inodx).cpucount
							nodememtemp[inodx] = nodememtemp[inodx] + systemDesign_.xnfunction[ifnx].memory[inodx];              // {VB2J [1062]}	                  nodememtemp(inodx) = nodememtemp(inodx) + xnfunction(ifnx).memory(inodx)
							}                                                      // {VB2J [1063]}	               Next ifnjob

						  for (int ifnjobran=1; ifnjobran<=systemDesign_.objfnjobset.numobjfn; ifnjobran++) { // {VB2J [1065]}	               For ifnjobran = 1 To objfnjobset.numobjfn
							int ifnmove = objfnjobsetlist[ifnjobran];                  // {VB2J [1066]}	                  ifnmove = objfnjobsetlist(ifnjobran)

//
// rec - NPE here aug'03 -This error does still happen about one run in 100. ???
// - root cause (as far as I know) is that systemDesign_.hammingscale != 0, so 
//		objfnjobsetlist is all empty, causing an NPE when agent[0] is accessed.
// - solution is to reload objfnjobsetlist from objfnjobset.initfn.... correct ???
//
if (ifnmove == 0) {

	if (systemDesign_.isWarnEnabled()) {
		SystemDesign.logWarn("AnnealForm.getRandomState(): NPE possibility! ---------------------");
		SystemDesign.logWarn("AnnealForm.getRandomState():  ifnjobran=" + ifnjobran +	
														", objfnjobsetlistHasBeenInited: " + objfnjobsetlistHasBeenInited);
		}

// 17Mar04 - do this always?
	if (true  ||  objfnjobsetlistHasBeenInited == false) {

		if (systemDesign_.isWarnEnabled()) {
			SystemDesign.logWarn("AnnealForm.getRandomState(): systemDesign_.hammingscale: " + systemDesign_.hammingscale);
			SystemDesign.logWarn("AnnealForm.getRandomState(): Applying objfnjobsetlist fixup?");
			}

		// fix up - how???
		// fix up entire objfnjobsetlist, or fixing ifnmove suffice? let's try all....???
		//
		for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {
			objfnjobsetlist[ifnjob] = systemDesign_.objfnjobset.initfn[ifnjob];
			}
		ifnmove = objfnjobsetlist[ifnjobran];

		} // objfnjobsetlistHasBeenInited==f

	//	17Mar04 - if still zero, we're gonna die. let's not.
	//
	if (ifnmove == 0) {
		if (systemDesign_.isWarnEnabled()) {
			SystemDesign.logWarn("AnnealForm.getRandomState(): FIXUP failed; aborting to prevent NPE. OK?");
			}
		return;
		}

	} // ifnmove==0
//
// end fix for NPE (which would occur in the next executable line)
//

							int indtarg = systemDesign_.agent[ifnmove].hamminghostid;                // {VB2J [1068]}	                  indtarg = agent(ifnmove).hamminghostid
							int indsrc = systemDesign_.agent[ifnmove].testhostid;                    // {VB2J [1069]}	                  indsrc = agent(ifnmove).testhostid
							if (indtarg != indsrc && systemDesign_.nodeinfo[indtarg].enabled)  {  // {VB2J [1070]}	                  If (indtarg <> indsrc And nodeinfo(indtarg).enabled) Then
								float nodeutiltarg = nodeutiltemp[indtarg] + systemDesign_.xnfunction[ifnmove].cpurate[indtarg] / systemDesign_.nodeinfo[indtarg].cpucount + systemDesign_.nodeinfo[indtarg].cpubackground / 100;              // {VB2J [1071]}	                     nodeutiltarg = nodeutiltemp(indtarg) + xnfunction(ifnmove).cpurate(indtarg) / nodeinfo(indtarg).cpucount + nodeinfo(indtarg).cpubackground / 100#
								float nodememtarg = nodememtemp[indtarg] + systemDesign_.xnfunction[ifnmove].memory[indtarg];              // {VB2J [1072]}	                     nodememtarg = nodememtemp(indtarg) + xnfunction(ifnmove).memory(indtarg)
								float nodeutilsrc = nodeutiltemp[indsrc] - systemDesign_.xnfunction[ifnmove].cpurate[indsrc] / systemDesign_.nodeinfo[indsrc].cpucount + systemDesign_.nodeinfo[indsrc].cpubackground / 100;              // {VB2J [1073]}	                     nodeutilsrc = nodeutiltemp(indsrc) - xnfunction(ifnmove).cpurate(indsrc) / nodeinfo(indsrc).cpucount + nodeinfo(indsrc).cpubackground / 100#
								float nodememsrc = nodememtemp[indsrc] - systemDesign_.xnfunction[ifnmove].memory[indsrc];              // {VB2J [1074]}	                     nodememsrc = nodememtemp(indsrc) - xnfunction(ifnmove).memory(indsrc)

							  if (nodeutiltarg <= systemDesign_.nodeinfo[indtarg].cpuutilmax / 100 
							    && nodememtarg <= systemDesign_.nodeinfo[indtarg].memorymax)  { // {VB2J [1076]}	                     If (nodeutiltarg <= nodeinfo(indtarg).cpuutilmax / 100# And nodememtarg <= nodeinfo(indtarg).memorymax) Then
								systemDesign_.agent[ifnmove].testhostid = indtarg;               // {VB2J [1077]}	                        agent(ifnmove).testhostid = indtarg
								agentmoved = true;                                 // {VB2J [1078]}	                        agentmoved = True
								nodeutiltemp[indsrc] = nodeutilsrc;                // {VB2J [1079]}	                        nodeutiltemp(indsrc) = nodeutilsrc
								nodememtemp[indsrc] = nodememsrc;                  // {VB2J [1080]}	                        nodememtemp(indsrc) = nodememsrc
								nodeutiltemp[indtarg] = nodeutiltarg;              // {VB2J [1081]}	                        nodeutiltemp(indtarg) = nodeutiltarg
								nodememtemp[indtarg] = nodememtarg;                // {VB2J [1082]}	                        nodememtemp(indtarg) = nodememtarg
								}                                                  // {VB2J [1083]}	                     End If
							  }                                                    // {VB2J [1084]}	                  End If
							}                                                      // {VB2J [1085]}	               Next ifnjobran


						  return;                                                  // {VB2J [1087]}	               Exit Sub
						  
// block inserted for EN2.09 / 2.02
          
//				            ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//				            ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//				            ''''''''' en 2.0 load balancing  '''''''''''''''''''''''''
//				            ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//				            ''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

            // 'marc en 2.02 blend load balancing and messaging
            // 'ElseIf (rndtest < 0.98 And loadbalscale > 0#) Then  'marc en 2.0 load balancing
			}
           else if ((rndtest < 0.98 && systemDesign_.loadbalscale > 0) ||
                (rndtest < 0.98 && systemDesign_.blendloadbalmessaging && !systemDesign_.blendloadbalmessagingfirstpass)) {

//               'recall from annealingstepcommand that objective function is found as follows:
//               'For inod1 = 1 To numnode
//               '   For inod2 = inod1 + 1 To numnode
//               '      If ((nodeinfo(inod1).type = 1 And nodeinfo(inod1).enabled) And (nodeinfo(inod2).type = 1 And nodeinfo(inod2).enabled)) Then
//               '         utilcpu1 = 100# * nodeinfo(inod1).cpuutil + nodeinfo(inod1).cpubackground
//               '         utilcpu2 = 100# * nodeinfo(inod2).cpuutil + nodeinfo(inod2).cpubackground
//               '
//               '         utilmem1 = (nodeinfo(inod1).memoryused / nodeinfo(inod1).memory) * 100
//               '         utilmem2 = (nodeinfo(inod2).memoryused / nodeinfo(inod2).memory) * 100
//               '
//               '         loadbal = loadbal + loadbalcpumemratio * (utilcpu1 - utilcpu2) ^ 2
//               '         loadbal = loadbal + (1# - loadbalcpumemratio) * (utilmem1 - utilmem2) ^ 2
//               '      End If
//               '   Next inod2
//               'Next inod1
//               '
//               'loadbal = Sqr(loadbal)
               
               int ilist = 0;
               for (inod1=1; inod1<=systemDesign_.numnode; inod1++) {

                  // 'en 2.01 bug fix
                  // 'For inod2 = inod1 To numnode
                  for (inod2=inod1+1; inod2<=systemDesign_.numnode; inod2++) {
                     if ((systemDesign_.nodeinfo[inod1].type == 1) && 
                             systemDesign_.nodeinfo[inod1].enabled && 
                             (systemDesign_.nodeinfo[inod2].type == 1) 
                             && systemDesign_.nodeinfo[inod2].enabled) {

                        ilist++;                  
                        float utilcpu1 = nodeutiltemp[inod1] + systemDesign_.nodeinfo[inod1].cpubackground / 100;
                        float utilcpu2 = nodeutiltemp[inod2] + systemDesign_.nodeinfo[inod2].cpubackground / 100;
               
                        float utilmem1 = nodememtemp[inod1] / systemDesign_.nodeinfo[inod1].memory;
                        float utilmem2 = nodememtemp[inod2] / systemDesign_.nodeinfo[inod2].memory;
               
                        float loadbaltemp = systemDesign_.loadbalcpumemratio * (utilcpu1 - utilcpu2) * (utilcpu1 - utilcpu2);
                        loadbaltemp = loadbaltemp + (1 - systemDesign_.loadbalcpumemratio) * (utilmem1 - utilmem2) * (utilmem1 - utilmem2);
                        pfailuresurvivelist[ilist] = (float)Math.sqrt(loadbaltemp);

                        pfailuresurvivelistid[ilist] = inod1;
                        pfailuresurvivelistid2[ilist] = inod2;
                     } // End If
                  } // Next inod2
               } // Next inod1
               
               // 'links are sorted from lowest pfailuresurvive to highest
               numlistlong = ilist;
               quickSort_single_twoindex(pfailuresurvivelist, pfailuresurvivelistid, pfailuresurvivelistid2, 1, numlistlong);

               // 'indx1 = pfailuresurvivelistid(numlistlong)
               // 'indx2 = pfailuresurvivelistid2(numlistlong)
               pfailuresurvivetmpmax = pfailuresurvivelist[numlistlong];

               if (pfailuresurvivetmpmax == 0) {
                  // 'can only happen when all hosts have same utilization (perfect balance)
               		continue startmainloop;		                           // {VB2J [1117]}	                  GoTo startmainloop
               		}
  
               
               // 'loop over unbalanced node list and try to move agent from high util to low util
               // 'break out of loop after one move
               
               for (ilist=numlistlong; ilist>=1; ilist--) {

                  if (pfailuresurvivelist[ilist] == 0) { // 'actually should check for a small number like loadbalmin
                     checkmobility = false;
                     break;
                  	} // End If
 
                  int indx1 = pfailuresurvivelistid[ilist];
                  indx2 = pfailuresurvivelistid2[ilist];

                  // 'loop over agents on high util node, and try to move to low util node
                  // 'determine which host is high util:
                  float utilcpu1 = nodeutiltemp[indx1] + systemDesign_.nodeinfo[indx1].cpubackground / 100;
                  float utilcpu2 = nodeutiltemp[indx2] + systemDesign_.nodeinfo[indx2].cpubackground / 100;
                  float utilmem1 = nodememtemp[indx1] / systemDesign_.nodeinfo[indx1].memory;
                  float utilmem2 = nodememtemp[indx2] / systemDesign_.nodeinfo[indx2].memory;
                  float util1 = systemDesign_.loadbalcpumemratio * utilcpu1 * utilcpu1 + (1 - systemDesign_.loadbalcpumemratio) * utilmem1 * utilmem1;
                  float util2 = systemDesign_.loadbalcpumemratio * utilcpu2 * utilcpu2 + (1 - systemDesign_.loadbalcpumemratio) * utilmem2 * utilmem2;
                  
                  // rec - reworked
                  int inodlo = indx1;
                  int inodhi = indx2;
                  if (util1 > util2) {
                     inodhi = indx1;
                     inodlo = indx2;
                  	}
//                  else {
//                     inodhi = indx2;
//                     inodlo = indx1;
//                  	}

                  // 'agent lists are already randomized
                  for (int ifnxhi=1; ifnxhi<=systemDesign_.nodeinfo[inodhi].numagents; ifnxhi++) {
                     int ifnhi = systemDesign_.nodeinfo[inodhi].agentlist[ifnxhi];
                     
                     if (systemDesign_.xnfunction[ifnhi].felig[inodlo] == 1) { 
                         systemDesign_.agent[ifnhi].testhostid = inodlo;
                        checkmobility = true;
                        break; // Exit For
                     } // End If
                  } // Next ifnxhi
               
                  if (checkmobility) {
                      break; // Exit For
                  }
               } // Next ilist
                           
//				'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
//				'''''''''''''' end of load balancing mods  ''''''''''''''''''''''''''''''
//				'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
		            		            
// end block inserted for EN2.09 / 2.02

// line 3171 in 2.09 VB								  
						  } else {                                                 // {VB2J [1091]}	            Else

   // {VB2J [1092]}	            'ElseIf (rndtest < 0.98) Then  'marc en 1.1 smoothing w/messaging
   // {VB2J [1093]}	               'exchange functions between servers
   // {VB2J [1094]}	               'pick two random agents ifnran1 and ifnran2:

   // {VB2J [1096]}	               'build a list nodelisttemp of temporary eligible servers

						  int ilist = 0;                                               // {VB2J [1098]}	               ilist = 0
						  for (inod=1; inod<=systemDesign_.numnode; inod++) {                // {VB2J [1099]}	               For inod = 1 To numnode
							if (systemDesign_.nodeinfo[inod].enabled)  {                         // {VB2J [1100]}	                  If (nodeinfo(inod).enabled) Then
							  for (ifnx=1; ifnx<=systemDesign_.nodeinfo[inod].numagents; ifnx++) { // {VB2J [1101]}	                     For ifnx = 1 To nodeinfo(inod).numagents
								ifn = systemDesign_.nodeinfo[inod].agentlist[ifnx];              // {VB2J [1102]}	                        ifn = nodeinfo(inod).agentlist(ifnx)
								if (systemDesign_.xnfunction[ifn].nodelist[0] > 1)  {     // NPE 22sept03       // {VB2J [1103]}	                        If (xnfunction(ifn).nodelist(0) > 1) Then
								  ilist++;                               // {VB2J [1104]}	                           ilist = ilist + 1
								  nodelisttemp[ilist] = inod;                      // {VB2J [1105]}	                           nodelisttemp(ilist) = inod
								  break;                                           // {VB2J [1106]}	                           Exit For
								  }                                                // {VB2J [1107]}	                        End If
								}                                                  // {VB2J [1108]}	                     Next ifnx
							  }                                                    // {VB2J [1109]}	                  End If
						     } 												       // {VB2J [1110]}	               Next inod
						  nodelisttemp[0] = ilist;                                 // {VB2J [1112]}	               nodelisttemp(0) = ilist

// {VB2J [1114]}	               'make sure there is at least two servers with functions that can belong to two or more servers

						  if (nodelisttemp[0] < 2)  {                              // {VB2J [1116]}	               If (nodelisttemp(0) < 2) Then
							continue startmainloop;		                           // {VB2J [1117]}	                  GoTo startmainloop
							}                                                      // {VB2J [1118]}	               End If

int ilist1=0, ilist2=0;

// {VB2J [1120]}	               'randomize the node list:

			  for (ilist=1; ilist<=nodelisttemp[0]; ilist++) {     // {VB2J [1121]}	               For ilist = 1 To nodelisttemp(0)


				ilist1 = RnR.Int(RnR.Rnd() * nodelisttemp[0] + 1);               // {VB2J [1122]}	                  ilist1 = Int(Rnd * nodelisttemp(0) + 1)
				ilist2 = ilist1;                                       // {VB2J [1123]}	                  ilist2 = ilist1
				while (ilist2 == ilist1) { 								// {VB2J [1124]}	                  Do While (ilist2 = ilist1)
				  ilist2 = RnR.Int(RnR.Rnd() * nodelisttemp[0] + 1);               // {VB2J [1125]}	                     ilist2 = Int(Rnd * nodelisttemp(0) + 1)
				}                                                      // {VB2J [1126]}	                  Loop

			  int nodex1 = nodelisttemp[ilist1];                           // {VB2J [1128]}	                  nodex1 = nodelisttemp(ilist1)
			  int nodex2 = nodelisttemp[ilist2];                           // {VB2J [1129]}	                  nodex2 = nodelisttemp(ilist2)

			  nodelisttemp[ilist1] = nodex2;                           // {VB2J [1131]}	                  nodelisttemp(ilist1) = nodex2
			  nodelisttemp[ilist2] = nodex1;                           // {VB2J [1132]}	                  nodelisttemp(ilist2) = nodex1
			  }                                                        // {VB2J [1133]}	               Next ilist

		//  'en 2.01 load balancing bug fix
			  //for (ilist1=1; ilist1<=nodelisttemp[0]-1; ilist1++) {  // {VB2J [1135]}	               For ilist1 = 1 To nodelisttemp(0) - 1
		for (ilist1=1; ilist1<=nodelisttemp[0]; ilist1++) {  // {VB2J [1135]}	               For ilist1 = 1 To nodelisttemp(0) - 1

			int inode1 = nodelisttemp[ilist1];                           // {VB2J [1136]}	                  inode1 = nodelisttemp(ilist1)

			//  'en 2.01 load balancing bug fix
			// for (ilist2=ilist1+1; ilist2<=nodelisttemp[0]-1; ilist2++) { // {VB2J [1137]}	                  For ilist2 = ilist1 + 1 To nodelisttemp(0) - 1
			for (ilist2=ilist1+1; ilist2<=nodelisttemp[0]; ilist2++) { // {VB2J [1137]}	                  For ilist2 = ilist1 + 1 To nodelisttemp(0) - 1
				int inode2 = nodelisttemp[ilist2];                         // {VB2J [1138]}	                     inode2 = nodelisttemp(ilist2)

																	   // {VB2J [1140]}	                     'loop over all agents on inode1, and find one with eligibility on more than one server
				int if1start = RnR.Int(systemDesign_.nodeinfo[inode1].numagents * RnR.Rnd() + 1);              // {VB2J [1141]}	                     if1start = Int(nodeinfo(inode1).numagents * Rnd + 1)
				for (ifnx1=if1start; ifnx1<=systemDesign_.nodeinfo[inode1].numagents; ifnx1++) { // {VB2J [1142]}	                     For ifnx1 = if1start To nodeinfo(inode1).numagents
				  ifn1 = systemDesign_.nodeinfo[inode1].agentlist[ifnx1];              // {VB2J [1143]}	                        ifn1 = nodeinfo(inode1).agentlist(ifnx1)
				  if (systemDesign_.xnfunction[ifn1].felig[inode2] == 1)  {          // {VB2J [1144]}	                        If (xnfunction(ifn1).felig(inode2) = 1) Then
					int if2start = RnR.Int(systemDesign_.nodeinfo[inode2].numagents * RnR.Rnd() + 1);              // {VB2J [1145]}	                           if2start = Int(nodeinfo(inode2).numagents * Rnd + 1)
					for (ifnx2=if2start; ifnx2<=systemDesign_.nodeinfo[inode2].numagents; ifnx2++) { // {VB2J [1146]}	                           For ifnx2 = if2start To nodeinfo(inode2).numagents
					  ifn2 = systemDesign_.nodeinfo[inode2].agentlist[ifnx2];              // {VB2J [1147]}	                              ifn2 = nodeinfo(inode2).agentlist(ifnx2)
					  if (systemDesign_.xnfunction[ifn2].felig[inode1] == 1)  {      // {VB2J [1148]}	                              If (xnfunction(ifn2).felig(inode1) = 1) Then
						systemDesign_.agent[ifn1].testhostid = inode2;               // {VB2J [1149]}	                                 agent(ifn1).testhostid = inode2
						systemDesign_.agent[ifn2].testhostid = inode1;               // {VB2J [1150]}	                                 agent(ifn2).testhostid = inode1
						return;                                        // {VB2J [1151]}	                                 Exit Sub
						}                                              // {VB2J [1152]}	                              End If
					  }                                                // {VB2J [1153]}	                           Next ifnx2

					for (ifnx2=1; ifnx2<=if2start-1; ifnx2++) {    // {VB2J [1155]}	                           For ifnx2 = 1 To if2start - 1
					  ifn2 = systemDesign_.nodeinfo[inode2].agentlist[ifnx2];              // {VB2J [1156]}	                              ifn2 = nodeinfo(inode2).agentlist(ifnx2)
					  if (systemDesign_.xnfunction[ifn2].felig[inode1] == 1)  {      // {VB2J [1157]}	                              If (xnfunction(ifn2).felig(inode1) = 1) Then
						systemDesign_.agent[ifn1].testhostid = inode2;               // {VB2J [1158]}	                                 agent(ifn1).testhostid = inode2
						systemDesign_.agent[ifn2].testhostid = inode1;               // {VB2J [1159]}	                                 agent(ifn2).testhostid = inode1
						return;                                        // {VB2J [1160]}	                                 Exit Sub
						}                                              // {VB2J [1161]}	                              End If
					  }                                                // {VB2J [1162]}	                           Next ifnx2
					}                                                  // {VB2J [1163]}	                        End If
				  }                                                    // {VB2J [1164]}	                     Next ifnx1

				for (ifnx1=1; ifnx1<=if1start-1; ifnx1++) {        // {VB2J [1166]}	                     For ifnx1 = 1 To if1start - 1
				  ifn1 = systemDesign_.nodeinfo[inode1].agentlist[ifnx1];              // {VB2J [1167]}	                        ifn1 = nodeinfo(inode1).agentlist(ifnx1)
				  if (systemDesign_.xnfunction[ifn1].felig[inode2] == 1)  {          // {VB2J [1168]}	                        If (xnfunction(ifn1).felig(inode2) = 1) Then
					int if2start = RnR.Int(systemDesign_.nodeinfo[inode2].numagents * RnR.Rnd() + 1);              // {VB2J [1169]}	                           if2start = Int(nodeinfo(inode2).numagents * Rnd + 1)
					for (ifnx2=if2start; ifnx2<=systemDesign_.nodeinfo[inode2].numagents; ifnx2++) { // {VB2J [1170]}	                           For ifnx2 = if2start To nodeinfo(inode2).numagents
					  ifn2 = systemDesign_.nodeinfo[inode2].agentlist[ifnx2];              // {VB2J [1171]}	                              ifn2 = nodeinfo(inode2).agentlist(ifnx2)
					  if (systemDesign_.xnfunction[ifn2].felig[inode1] == 1)  {      // {VB2J [1172]}	                              If (xnfunction(ifn2).felig(inode1) = 1) Then
						systemDesign_.agent[ifn1].testhostid = inode2;               // {VB2J [1173]}	                                 agent(ifn1).testhostid = inode2
						systemDesign_.agent[ifn2].testhostid = inode1;               // {VB2J [1174]}	                                 agent(ifn2).testhostid = inode1
						return;                                        // {VB2J [1175]}	                                 Exit Sub
						}                                              // {VB2J [1176]}	                              End If
					  }                                                // {VB2J [1177]}	                           Next ifnx2

					for (ifnx2=1; ifnx2<=if2start-1; ifnx2++) {    // {VB2J [1179]}	                           For ifnx2 = 1 To if2start - 1
					  ifn2 = systemDesign_.nodeinfo[inode2].agentlist[ifnx2];              // {VB2J [1180]}	                              ifn2 = nodeinfo(inode2).agentlist(ifnx2)
					  if (systemDesign_.xnfunction[ifn2].felig[inode1] == 1)  {      // {VB2J [1181]}	                              If (xnfunction(ifn2).felig(inode1) = 1) Then
						systemDesign_.agent[ifn1].testhostid = inode2;               // {VB2J [1182]}	                                 agent(ifn1).testhostid = inode2
						systemDesign_.agent[ifn2].testhostid = inode1;               // {VB2J [1183]}	                                 agent(ifn2).testhostid = inode1
																	   // {VB2J [1184]}	                                 'en 1.1 cleanup
																	   // {VB2J [1185]}	                                 'Exit Sub
						checkmobility = true;                          // {VB2J [1186]}	                                 checkmobility = True
						break;                                         // {VB2J [1187]}	                                 Exit For
						}                                              // {VB2J [1188]}	                              End If
					  }                                                // {VB2J [1189]}	                           Next ifnx2

					if (checkmobility) break;            // {VB2J [1190]}	                           If (checkmobility) Then Exit For
					}                                    // {VB2J [1192]}	                        End If
				  }                                      // {VB2J [1193]}	                     Next ifnx1
			    if (checkmobility) break;                  // {VB2J [1194]}	                    If (checkmobility) Then Exit For
			    }                                          // {VB2J [1196]}	                  Next ilist2
		      if (checkmobility) break;                    // {VB2J [1197]}	                  If (checkmobility) Then Exit For
		      }                                            // {VB2J [1199]}	               Next ilist1
		    }                                              // {VB2J [1201]}	            End If
	      }                                                // {VB2J [1203]}	         End If
	    }                                                  // {VB2J [1205]}	      Loop
	  }                                                    // {VB2J [1207]}	   End If

//	if (systemDesign_.isDebugEnabled())
//		SystemDesign.logDebug("getRandomState out.");

	}    // {VB2J [1209]}	End Sub getRandomState


// /**
//	getRandomThread
//	
//	@version EN2.09: removed per "'en 2.03 cleanup: DELETE THIS ENTIRE ROUTINE"
//
// **/
//
//private void
//getRandomThread() {                                        // {VB2J [1416]}	Sub getrandomthread()
//
//
//// rec - refactored to be instance var
//// Note that this name collides with an existing 'threadcall[]'; rename 'threadcall2' - um, no, undo. 28aug03
////  int threadcall[] = new int[systemDesign_.numfunctionmax+1];                                  // {VB2J [1418]}	   Dim threadcall(numfunctionmax) As Integer
//    for (int i=0; i<=SystemDesign.numfunctionmax; i++) {
//        threadcall[i] = 0;
//    }
//
//systemDesign_.srcfn = systemDesign_.activetracefunction;                                                   // {VB2J [1419]}	   srcfn = activetracefunction
//
//// rec - decl
//int ifninit = 0;                                                                   // {VB2J [1421]}	   ifninit = 0
//
//for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                                     // {VB2J [1423]}	   For ifn = 1 To numfunction
//  if (systemDesign_.xnfunction[systemDesign_.srcfn].fcall[ifn].callfreq > 0 
//   || systemDesign_.xnfunction[systemDesign_.srcfn].fcall[ifn].callpct > 0)  { // {VB2J [1424]}	      If (xnfunction(srcfn).fcall(ifn).callfreq > 0 Or xnfunction(srcfn).fcall(ifn).callpct > 0) Then
//    ifninit++;                                                                 // {VB2J [1425]}	         ifninit = ifninit + 1
//    threadcall[ifninit] = ifn;                                     // {VB2J [1426]}	         threadcall(ifninit) = ifn
//    }                                                                          // {VB2J [1427]}	      End If
//  }                                                                            // {VB2J [1428]}	   Next ifn
//
//                                                                               // {VB2J [1430]}	   'ifninitcall(0) = total number of calls initially generated by first call to activetracefunction
//threadcall[0] = ifninit;                                           // {VB2J [1431]}	   threadcall(0) = ifninit
//
//                                                                               // {VB2J [1433]}	   'pick one of the initial calls at random:
//// rec - decl*2
//int numthreadcall = 1;                                                             // {VB2J [1435]}	   numthreadcall = 1
//int icallx = RnR.Int(ifninit * RnR.Rnd() + 1);    // {VB2J [1436]}	   icallx = Int(ifninit * com.boeing.pw.mct.vb2j.Utils.rnd() + 1)
//systemDesign_.thread[1].fcallid[numthreadcall] = threadcall[icallx];             // {VB2J [1437]}	   thread(1).fcallid(numthreadcall) = threadcall(icallx)
//
//while (true) {                                                                 // {VB2J [1439]}	   Do While true
//                                                                               // {VB2J [1440]}	      'loop over all calls made by current (initial) function:
//  // rec - decl
//  int fncurloop = systemDesign_.thread[1].fcallid[numthreadcall];                    // {VB2J [1441]}	      fncurloop = thread(1).fcallid(numthreadcall)
//  ifninit = 0;                                                                 // {VB2J [1442]}	      ifninit = 0
//  for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                                   // {VB2J [1443]}	      For ifn = 1 To numfunction
//    if (systemDesign_.xnfunction[fncurloop].fcall[ifn].callpct > 0)  {                       // {VB2J [1444]}	         If (xnfunction(fncurloop).fcall(ifn).callpct > 0) Then
//      ifninit++;                                                               // {VB2J [1445]}	            ifninit = ifninit + 1
//      threadcall[ifninit] = ifn;                                   // {VB2J [1446]}	            threadcall(ifninit) = ifn
//      }                                                                        // {VB2J [1447]}	         End If
//    }                                                                          // {VB2J [1448]}	      Next ifn
//
//  if (ifninit == 0)  {                                                         // {VB2J [1450]}	      If (ifninit = 0) Then
//    systemDesign_.thread[1].fcallid[0] = numthreadcall;                          // {VB2J [1451]}	         thread(1).fcallid(0) = numthreadcall
//    break;                                                                     // {VB2J [1452]}	         Exit Do
//    }                                                                          // {VB2J [1453]}	      End If
//
//  numthreadcall++;                                                             // {VB2J [1455]}	      numthreadcall = numthreadcall + 1
//  icallx = RnR.Int(ifninit * RnR.Rnd() + 1);  // {VB2J [1456]}	      icallx = Int(ifninit * com.boeing.pw.mct.vb2j.Utils.rnd() + 1)
//  systemDesign_.thread[1].fcallid[numthreadcall] = threadcall[icallx];           // {VB2J [1457]}	      thread(1).fcallid(numthreadcall) = threadcall(icallx)
//  }                                                                            // {VB2J [1458]}	   Loop
//
//                                                                               // {VB2J [1460]}	   'now pick a subthread
//// rec - decl*2
//int icallx1 = RnR.Int(numthreadcall * RnR.Rnd() + 1);  // {VB2J [1462]}	   icallx1 = Int(numthreadcall * com.boeing.pw.mct.vb2j.Utils.rnd() + 1)
//int icallx2 = RnR.Int(numthreadcall * RnR.Rnd() + 1);  // {VB2J [1463]}	   icallx2 = Int(numthreadcall * com.boeing.pw.mct.vb2j.Utils.rnd() + 1)
//
//                                                                               // {VB2J [1466]}	   'thread(0).fcallid(*) is a list of the selected subthread calls,
//                                                                               // {VB2J [1467]}	   'with thread(0).fcallid(0) = number of calls in selected subthread
//
//if (icallx1 == icallx2)  {                                                     // {VB2J [1469]}	   If (icallx1 = icallx2) Then
//  systemDesign_.thread[0].fcallid[0] = 1;                                        // {VB2J [1470]}	      thread(0).fcallid(0) = 1
//  } else if (icallx1 > icallx2)  {                                             // {VB2J [1471]}	   ElseIf (icallx1 > icallx2) Then
//  systemDesign_.thread[0].fcallid[0] = icallx1 - icallx2 + 1;                    // {VB2J [1472]}	      thread(0).fcallid(0) = icallx1 - icallx2 + 1
//  for (int ifn=icallx2; ifn<=icallx1; ifn++) {                                 // {VB2J [1473]}	      For ifn = icallx2 To icallx1
//    systemDesign_.thread[0].fcallid[ifn - icallx2 + 1] = systemDesign_.thread[1].fcallid[ifn]; // {VB2J [1474]}	         thread(0).fcallid(ifn - icallx2 + 1) = thread(1).fcallid(ifn)
//    }                                                                          // {VB2J [1475]}	      Next ifn
//  } else {                                                                     // {VB2J [1476]}	   Else
//  systemDesign_.thread[0].fcallid[0] = icallx2 - icallx1 + 1;                    // {VB2J [1477]}	      thread(0).fcallid(0) = icallx2 - icallx1 + 1
//  for (int ifn=icallx1; ifn<=icallx2; ifn++) {                                 // {VB2J [1478]}	      For ifn = icallx1 To icallx2
//    systemDesign_.thread[0].fcallid[ifn - icallx1 + 1] = systemDesign_.thread[1].fcallid[ifn]; // {VB2J [1479]}	         thread(0).fcallid(ifn - icallx1 + 1) = thread(1).fcallid(ifn)
//    }                                                                          // {VB2J [1480]}	      Next ifn
//  }                                                                            // {VB2J [1481]}	   End If
//
//                                                                               // {VB2J [1484]}	   'for each function call in the sample thread, we search for a new server assignment
//
//for (int ifnx=1; ifnx<=systemDesign_.thread[0].fcallid[0]; ifnx++) {                         // {VB2J [1486]}	   For ifnx = 1 To thread(0).fcallid(0)
//
//  // rec - decl
//  int ifn = systemDesign_.thread[0].fcallid[ifnx];                                   // {VB2J [1487]}	      ifn = thread(0).fcallid(ifnx)
//                                                                               // {VB2J [1488]}	      'search the node/function eligibility list:
//  if (ifn == systemDesign_.activetracefunction)  {                                           // {VB2J [1489]}	      If (ifn = activetracefunction) Then
//    for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [1490]}	         For inod = 1 To numnode
//      systemDesign_.state.fstatecall[ifn][inod] = systemDesign_.activetracenode;               // {VB2J [1491]}	            State.fstatecall(ifn, inod) = activetracenode
//      }                                                                        // {VB2J [1492]}	         Next inod
//    } else {                                                                   // {VB2J [1493]}	      Else
//    // rec - decl
//    int inodrunx = RnR.Int(systemDesign_.xnfunction[ifn].nodelist[0] * RnR.Rnd() + 1);  // {VB2J [1494]}	         inodrunx = Int((xnfunction(ifn).nodelist(0)) * com.boeing.pw.mct.vb2j.Utils.rnd() + 1)
//    for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                  // {VB2J [1495]}	         For inod = 1 To numnode
//      systemDesign_.state.fstatecall[ifn][inod] = systemDesign_.xnfunction[ifn].nodelist[inodrunx];  // {VB2J [1496]}	            State.fstatecall(ifn, inod) = xnfunction(ifn).nodelist(inodrunx)
//      }                                                                        // {VB2J [1497]}	         Next inod
//    }                                                                          // {VB2J [1498]}	      End If
//  }                                                                            // {VB2J [1499]}	   Next ifnx
//
//}                                                                              // {VB2J [1501]}	End Sub
//// getrandomthread



/**
	Form_Load
	Mostly GUI stuff, which has been removed.
	
	Sets activenodeindex, used elsewhere. For now, callers must set activenodeindex - ???

**/
private void
formLoad() {                                              // {VB2J [3043]}	Private Sub Form_Load()


int j; /* int ifn; */ /* int inod; */                                                     // {VB2J [3044]}	   Dim j As Integer, ifn As Integer, inod As Integer

iter = 0;                                                                      // {VB2J [3046]}	   iter = 0
iterk = 0;                                                                     // {VB2J [3047]}	   iterk = 0
iterm = 0;                                                                     // {VB2J [3048]}	   iterm = 0

// rec - for iteration count
longIter_ = 0;

// rec - Don't set this here, SystemDesign can init it, and our users will change it when they want (I guess)
// systemDesign_.iteranel = false;                                                              // {VB2J [3050]}	   iteranel = false

systemDesign_.annealtemp = 1.0f;                                                                // {VB2J [3052]}	   annealtemp = 1


// rec - removed GUI code
/*
incrementLabel.Caption = iterk;                                                // {VB2J [3055]}	   incrementLabel.Caption = iterk
drawincrement = true;                                                          // {VB2J [3056]}	   drawincrement = true
*/

systemDesign_.monthlycostave = 0f;                                                            // {VB2J [3058]}	   monthlycostave = 0
systemDesign_.fixedcostave = 0f;                                                              // {VB2J [3059]}	   fixedcostave = 0
systemDesign_.responsetimeave = 0f;                                                           // {VB2J [3060]}	   responsetimeave = 0

// rec - removed GUI code
/*
screenrefreshText.text = 4;                                                    // {VB2J [3062]}	   screenrefreshText.text = 4
*/

                                                                               // {VB2J [3064]}	   'putting this in form_activate allows user to change draw status by opening
                                                                               // {VB2J [3065]}	   'tracefunctionform, and changing draw options in the middle of a run

// rec - removed GUI code
/*
drawmsgpathoptChecksave = tracefunctionForm.drawmsgpathoptCheck.Value;         // {VB2J [3066]}	   drawmsgpathoptChecksave = tracefunctionForm.drawmsgpathoptCheck.Value
drawmsgpathsearchChecksave = tracefunctionForm.drawmsgpathsearchCheck.Value;   // {VB2J [3067]}	   drawmsgpathsearchChecksave = tracefunctionForm.drawmsgpathsearchCheck.Value
drawmsgpathannealchecksave = tracefunctionForm.drawmsgpathannealCheck.Value;   // {VB2J [3068]}	   drawmsgpathannealchecksave = tracefunctionForm.drawmsgpathannealCheck.Value

activenodeindex = tracefunctionForm.nodeCombo.ListIndex + 1;                   // {VB2J [3072]}	   activenodeindex = tracefunctionForm.nodeCombo.ListIndex + 1
*/

                                                                               // {VB2J [3077]}	   'for each function ifn, load the list of nodes for which that function is eligible:
                                                                               // {VB2J [3078]}	   'note that the nodal availability can change during a run, if user updates nodal properties

                                                                               // {VB2J [3080]}	   'bug: this is a duplication of annealingstepcommand_click
for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                                     // {VB2J [3081]}	   For ifn = 1 To numfunction
  j = 0;                                                                       // {VB2J [3082]}	      j = 0
  for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                    // {VB2J [3083]}	      For inod = 1 To numnode
    if (systemDesign_.xnfunction[ifn].felig[inod] == 1)  {                                   // {VB2J [3084]}	         If (xnfunction(ifn).felig(inod) = 1) Then
      j++;                                                               // {VB2J [3085]}	            j = j + 1
      systemDesign_.xnfunction[ifn].nodelist[j] = inod;                          // {VB2J [3086]}	            xnfunction(ifn).nodelist(j) = inod
      }                                                                        // {VB2J [3087]}	         End If
    }                                                                          // {VB2J [3088]}	      Next inod

  systemDesign_.xnfunction[ifn].nodelist[0] = j;                                 // {VB2J [3090]}	      xnfunction(ifn).nodelist(0) = j
  }                                                                            // {VB2J [3091]}	   Next ifn

                                                                               // {VB2J [3096]}	'load the nodecombo and functioncombo list boxes

// rec - removed GUI code
/*
for (int inod=1; inod<=numnode; inod++) {                                      // {VB2J [3098]}	   For inod = 1 To numnode
  VBJ2_XLATE_FAILURE;                                                          // {VB2J [3099]}	      nodeCombo.AddItem nodeinfo(inod).name
  }                                                                            // {VB2J [3100]}	   Next inod

if (darparun)  {                                                               // {VB2J [3102]}	   If (darparun) Then
  DarparunCheck.Value = 1;                                                     // {VB2J [3103]}	      DarparunCheck.Value = 1
  } else {                                                                     // {VB2J [3104]}	   Else
  DarparunCheck.Value = 0;                                                     // {VB2J [3105]}	      DarparunCheck.Value = 0
  }                                                                            // {VB2J [3106]}	   End If
*/

foundInodStart:	while (true) { // rec - reworked GoTo

if ( ! systemDesign_.darparun )  {                                                            // {VB2J [3110]}	   If (Not darparun) Then
                                                                               // {VB2J [3111]}	      'for default trace function, activetracefunction, find first function with non-zero calling frequency
// rec - unused
//  int inodstart = -1;                                                              // {VB2J [3112]}	      inodstart = -1
//  int ifnstart = -1;                                                               // {VB2J [3113]}	      ifnstart = -1


  for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                                   // {VB2J [3115]}	      For ifn = 1 To numfunction
    for (int ifncall=1; ifncall<=systemDesign_.numfunction; ifncall++) {                     // {VB2J [3116]}	         For ifncall = 1 To numfunction
      if (systemDesign_.xnfunction[ifn].fcall[ifncall].callfreq > 0)  {                      // {VB2J [3117]}	            If (xnfunction(ifn).fcall(ifncall).callfreq > 0) Then
                                                                               // {VB2J [3118]}	               'find first eligible server
//		rec - unused
//        inodstart = systemDesign_.xnfunction[ifn].nodelist[1];                   // {VB2J [3119]}	               inodstart = xnfunction(ifn).nodelist(1)
//        ifnstart = ifn;                                                        // {VB2J [3120]}	               ifnstart = ifn

        break foundInodStart; //F GOTO                                           // {VB2J [3121]}	               GoTo foundinodstart
        }                                                                      // {VB2J [3122]}	            End If
      }                                                                        // {VB2J [3123]}	         Next ifncall
    }                                                                          // {VB2J [3124]}	      Next ifn

   String msgrec = "AnnealForm.formLoad: ";           // {VB2J [3126]}	      msgrec = "FATAL EX NIHILO ERROR:" + Chr(10) + Chr(10)
   msgrec = msgrec + "Unable to find initial function call with positive calling frequency."; // {VB2J [3127]}	      msgrec = msgrec + "Unable to find initial function call with positive calling frequency"
   SystemDesign.logWarn(msgrec);                                                          // {VB2J [3128]}	      MsgBox msgrec
  }                                                                            // {VB2J [3129]}	      End
  break foundInodStart;

} // while(true) - foundInodStart
//foundinodstart:                                                                // {VB2J [3130]}	foundinodstart:


/* rec - removed GUI code

nodeCombo.ListIndex = inodstart - 1;                                           // {VB2J [3132]}	      nodeCombo.ListIndex = inodstart - 1

if (functionCombo.ListCount > 0)  {                                            // {VB2J [3134]}	      If (functionCombo.ListCount > 0) Then
  functionCombo.ListIndex = 0;                                                 // {VB2J [3135]}	         functionCombo.ListIndex = 0
  } else {                                                                     // {VB2J [3136]}	      Else
  functionCombo.ListIndex = -1;                                                // {VB2J [3137]}	         functionCombo.ListIndex = -1
  }                                                                            // {VB2J [3138]}	      End If

}                                                                              // {VB2J [3140]}	   End If


VBJ2_XLATE_FAILURE;                                                            // {VB2J [3142]}	   statelistCombo.AddItem "Annealing"

for (int istate=1; istate<=numsavedstatesmax; istate++) {                      // {VB2J [3144]}	   For istate = 1 To numsavedstatesmax
  VBJ2_XLATE_FAILURE;                                                          // {VB2J [3145]}	      statelistCombo.AddItem istate
}                                                                              // {VB2J [3146]}	   Next istate

                                                                               // {VB2J [3149]}	'   statelistCombo.AddItem "1"
                                                                               // {VB2J [3150]}	'   statelistCombo.AddItem "2"
                                                                               // {VB2J [3151]}	'   statelistCombo.AddItem "3"
                                                                               // {VB2J [3152]}	'   statelistCombo.AddItem "4"
                                                                               // {VB2J [3153]}	'   statelistCombo.AddItem "5"

statelistCombo.ListIndex = 0;                                                  // {VB2J [3155]}	   statelistCombo.ListIndex = 0

SSTab1.Tab = 1;                                                                // {VB2J [3157]}	   SSTab1.Tab = 1

*/
}                                                                              // {VB2J [3164]}	End Sub
// Form_Load



/**
	Output the agent/node mapping -
    used to be to the "agents.state" file (later generalized to {project}.state)
    but now only writes to the logger; perhaps should output to a PrintStream?

    writeAgentState() actually writes to a file.

	Don't know how/if this'll be used yet, except for debug/info. 
	(it's not - EN4J uses static methods in class RnR to Read-n-Rite)

**/
public void
writeAgents_NOT_USED() {                               // {VB2J [3886]}	Private Sub writeagentsCommand_Click()

	SystemDesign.logInfo("");
	SystemDesign.logInfo("writeAgents: agent state:");

// 18jun03 - unused
//	String statefile;                                                              // {VB2J [3887]}	   Dim statefile As String

	if (systemDesign_.iteranel)  {                                                // {VB2J [3889]}	   If (iterateCheck.Value = 1) Then
		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3890]}	      MsgBox "Cannot read state during iteration"
		SystemDesign.logWarn("writeAgents: Cannot read state during iteration");
		return;                                                                        // {VB2J [3891]}	      Exit Sub
	}                                                                              // {VB2J [3892]}	   End If

																				   // {VB2J [3894]}	   'filename = App.path & "\agents.state"
	// iloc = InStr[1, nodedeffilename, ".nodedef"];                      // {VB2J [3895]}	   iloc = InStr(1, nodedeffilename, ".nodedef")
	// statefile = Mid[nodedeffilename, 1, iloc];                         // {VB2J [3896]}	   statefile = Mid(nodedeffilename, 1, iloc)
	// filename = App.path & "\" & statefile & "state";                               // {VB2J [3897]}	   filename = App.path & "\" & statefile & "state"
	// filenum = FreeFile;                                                            // {VB2J [3898]}	   filenum = FreeFile
	// VBJ2_XLATE_FAILURE;                                                            // {VB2J [3900]}	   Open filename For Output As #filenum
	
	
																				   // {VB2J [3903]}	   'Failure
	if (systemDesign_.numstatelist == 0)  {                                                      // {VB2J [3904]}	   If (numstatelist = 0) Then
		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3905]}	      Print #filenum, 1
		SystemDesign.logInfo("writeAgents: no solution state: pFail=1");
	} else {                                                                       // {VB2J [3906]}	   Else
		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3907]}	      Print #filenum, statelist(0).pfailure
		SystemDesign.logInfo("writeAgents: pFail=" + systemDesign_.statelist[0].pfailure);
	}                                                                              // {VB2J [3908]}	   End If
	
																				   // {VB2J [3910]}	   'NODES:
	//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3911]}	   Print #filenum, numnode
	SystemDesign.logInfo("writeAgents: numnode=" + systemDesign_.numnode);
																				   // {VB2J [3912]}	   'Line Input #filenum, record
																				   // {VB2J [3913]}	   'If (Val(record) <> numnode) Then
																				   // {VB2J [3914]}	   '   MsgBox "number of nodes in state is not equal to current project"
																				   // {VB2J [3915]}	   '   End
																				   // {VB2J [3916]}	   'End If
	
	for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                      // {VB2J [3918]}	   For inod = 1 To numnode

		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3919]}	      Print #filenum, nodeinfo(inod).cpubackground
		SystemDesign.logInfo("writeAgents: nodeinfo[" + inod + "].cpubackground=" + systemDesign_.nodeinfo[inod].cpubackground);

																					   // {VB2J [3920]}	      'Line Input #filenum, record
																					   // {VB2J [3921]}	      'nodeinfo(ifnx).cpubackground = Val(record)
		
		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3922]}	      Print #filenum, nodeinfo(inod).cpuutilmax
		SystemDesign.logInfo("writeAgents: nodeinfo[" + inod + "].cpuutilmax=" + systemDesign_.nodeinfo[inod].cpuutilmax);

																					   // {VB2J [3923]}	      'Line Input #filenum, record
																					   // {VB2J [3924]}	      'nodeinfo(ifnx).cpuutilmax = Val(record)
		
		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3925]}	      Print #filenum, nodeinfo(inod).jipstocpusecpersec
		SystemDesign.logInfo("writeAgents: nodeinfo[" + inod + "].jipstocpusecpersec=" + systemDesign_.nodeinfo[inod].jipstocpusecpersec);

																					   // {VB2J [3926]}	      'Line Input #filenum, record
																					   // {VB2J [3927]}	      'nodeinfo(ifnx).jipstocpusecpersec = Val(record)
		
		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3929]}	      Print #filenum, nodeinfo(inod).memory
		SystemDesign.logInfo("writeAgents: nodeinfo[" + inod + "].memory=" + systemDesign_.nodeinfo[inod].memory);

																					   // {VB2J [3930]}	      'Line Input #filenum, record
																					   // {VB2J [3931]}	      'nodeinfo(ifnx).memory = Val(record)
		
		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3933]}	      Print #filenum, nodeinfo(inod).pfail
		SystemDesign.logInfo("writeAgents: nodeinfo[" + inod + "].pfail=" + systemDesign_.nodeinfo[inod].pfail);

																					   // {VB2J [3934]}	      'Line Input #filenum, record
																					   // {VB2J [3935]}	      'nodeinfo(ifnx).pfail = Val(record)
	
	}                                                                              // {VB2J [3937]}	   Next inod
	
	for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                                   // {VB2J [3939]}	   For ilink = 1 To numlink
				
		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3940]}	      Print #filenum, linkinfo(ilink).bandwidth
		SystemDesign.logInfo("writeAgents: linkinfo[" + ilink + "].bandwidth=" + systemDesign_.linkinfo[ilink].bandwidth);

																					   // {VB2J [3941]}	      'Line Input #filenum, record
																					   // {VB2J [3942]}	      'linkinfo(ilink).bandwidth = record
				
		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3943]}	      Print #filenum, linkinfo(ilink).background
		SystemDesign.logInfo("writeAgents: linkinfo[" + ilink + "].background=" + systemDesign_.linkinfo[ilink].background);

																					   // {VB2J [3944]}	      'Line Input #filenum, record
																					   // {VB2J [3945]}	      'linkinfo(ilink).background = record
		
				
		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3947]}	      Print #filenum, linkinfo(ilink).pfail
		SystemDesign.logInfo("writeAgents: linkinfo[" + ilink + "].pfail=" + systemDesign_.linkinfo[ilink].pfail);

																					   // {VB2J [3948]}	      'Line Input #filenum, record
																					   // {VB2J [3949]}	      'linkinfo(ilink).pfail = record
	}                                                                              // {VB2J [3950]}	   Next ilink
	
	
																				   // {VB2J [3953]}	   'AGENT:
	
	//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3955]}	   Print #filenum, objfnjobset.numobjfn
	SystemDesign.logInfo("writeAgents: objfnjobset.numobjfn=" + systemDesign_.objfnjobset.numobjfn);

																				   // {VB2J [3956]}	   'Line Input #filenum, record
																				   // {VB2J [3957]}	   'objfnjobset.numobjfn = record
	
	for (int ifnx=1; ifnx<=systemDesign_.objfnjobset.numobjfn; ifnx++) {                         // {VB2J [3959]}	   For ifnx = 1 To objfnjobset.numobjfn

		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3960]}	      Print #filenum, objfnjobset.initfn(ifnx)
		SystemDesign.logInfo("writeAgents: objfnjobset.initfn[" + ifnx + "]=" + systemDesign_.objfnjobset.initfn[ifnx]);

																					   // {VB2J [3961]}	      'Line Input #filenum, record
																					   // {VB2J [3962]}	      'objfnjobset.initfn = record
	}                                                                              // {VB2J [3963]}	   Next ifnx
	
	
	
	//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3967]}	   Print #filenum, numfunction
	SystemDesign.logInfo("writeAgents: numfunction=" + systemDesign_.numfunction);
	
																				   // {VB2J [3969]}	   'Line Input #filenum, numfunctionx
																				   // {VB2J [3970]}	   'If (numfunctionx <> numfunction) Then
																				   // {VB2J [3971]}	   '   MsgBox "Number of functions in state file is not equal to number of functions in current run"
																				   // {VB2J [3972]}	   '   End
																				   // {VB2J [3973]}	   'End If
	
	for (int ifnx=1; ifnx<=systemDesign_.numfunction; ifnx++) {                                  // {VB2J [3975]}	   For ifnx = 1 To numfunction
	
		
		if (systemDesign_.numstatelist == 0)  {                                                      // {VB2J [3978]}	      If (numstatelist = 0) Then

			//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3979]}	         Print #filenum, 0
			SystemDesign.logInfo("writeAgents: numstatelist = 0");
		} else {                                                                       // {VB2J [3980]}	      Else

			//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3981]}	         Print #filenum, agent(ifnx).annealhostid
			SystemDesign.logInfo("writeAgents: agent[" + ifnx + "].annealhostid=" + systemDesign_.agent[ifnx].annealhostid);
																						   // {VB2J [3982]}	         'Line Input #filenum, record
																						   // {VB2J [3983]}	         'agent(ifnx).testhostid = Val(record)
		}                                                                              // {VB2J [3984]}	      End If

		
		//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3988]}	      Print #filenum, xnfunction(ifnx).ifgroup
		SystemDesign.logInfo("writeAgents: xnfunction[" + ifnx + "].ifgroup=" + systemDesign_.xnfunction[ifnx].ifgroup);

																					   // {VB2J [3989]}	      'Line Input #filenum, record
																					   // {VB2J [3990]}	      'xnfunction(ifnx).ifgroup = Val(record)
		
		for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                      // {VB2J [3992]}	      For inod = 1 To numnode

			//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3993]}	         Print #filenum, xnfunction(ifnx).cpurate(inod)
			SystemDesign.logInfo("writeAgents: xnfunction[" + ifnx + "].cpurate[" + inod + "]=" + systemDesign_.xnfunction[ifnx].cpurate[inod]);

																						   // {VB2J [3994]}	         'Line Input #filenum, record
																						   // {VB2J [3995]}	         'xnfunction(ifnx).cpurate(inod) = Val(record)

			// VBJ2_XLATE_FAILURE;                                                            // {VB2J [3996]}	         Print #filenum, xnfunction(ifnx).felig(inod)
			SystemDesign.logInfo("writeAgents: xnfunction[" + ifnx + "].felig[" + inod + "]=" + systemDesign_.xnfunction[ifnx].felig[inod]);

																						   // {VB2J [3997]}	         'Line Input #filenum, record
																						   // {VB2J [3998]}	         'xnfunction(ifnx).felig(inod) = Val(record)

			//VBJ2_XLATE_FAILURE;                                                            // {VB2J [3999]}	         Print #filenum, xnfunction(ifnx).memory(inod)
			SystemDesign.logInfo("writeAgents: xnfunction[" + ifnx + "].memory[" + inod + "]=" + systemDesign_.xnfunction[ifnx].memory[inod]);

																						   // {VB2J [4000]}	         'Line Input #filenum, record
																						   // {VB2J [4001]}	         'xnfunction(ifnx).memory(inod) = Val(record)
		}                                                                              // {VB2J [4002]}	      Next inod
		
		
		for (int ifnx2=1; ifnx2<=systemDesign_.numfunction; ifnx2++) {                               // {VB2J [4005]}	      For ifnx2 = 1 To numfunction

			//VBJ2_XLATE_FAILURE;                                                            // {VB2J [4006]}	         Print #filenum, xnfunction(ifnx).fcall(ifnx2).sendmsgrate
			if (systemDesign_.isInfoEnabled())
				SystemDesign.logInfo("writeAgents: xnfunction[" + ifnx + "].fcall[" + ifnx2 + "].sendmsgrate=" 
										+ systemDesign_.xnfunction[ifnx].fcall[ifnx2].sendmsgrate);

																						   // {VB2J [4007]}	         'Line Input #filenum, record
																						   // {VB2J [4008]}	         'xnfunction(ifnx).fcall(ifnx2).sendmsgrate = Val(record)
		}                                                                              // {VB2J [4009]}	      Next ifnx2
	
	}                                                                              // {VB2J [4011]}	   Next ifnx

	//VBJ2_XLATE_FAILURE;                                                            // {VB2J [4013]}	   Close #filenum
	
	
	if (systemDesign_.numstatelist == 0)  {                                                      // {VB2J [4016]}	   If (numstatelist = 0) Then
                                                         
		if (systemDesign_.isWarnEnabled())
			SystemDesign.logWarn("writeAgents: No valid solution found."); 				// {VB2J [4017]}	      MsgBox "Fatal Error: No Valid Solution Has Been Found!"
		}                                                                              // {VB2J [4018]}	   End If
	
	}                                                                              // {VB2J [4022]}	End Sub
// writeagents - was writeagentsCommand_Click



/*
	writeAgentState
	Taken from ExNihilo for 2002, last mod 14July
	Re-worked for this version.

	algorithm ("::" denotes writing that data item)

        Open file "agents.state"

        :: pfail (ignored coming into EN)
        :: numnodes (number of nodes)

        For inod = 1 To numnodes
            :: nodeinfo(inod).cpubackground
            :: nodeinfo(inod).cpuutilmax
            :: nodeinfo(inod).jipstocpusecpersec
            :: nodeinfo(inod).memory
            :: nodeinfo(inod).pfail
        Next inod

        For ilink = 1 To numlink
            :: linkinfo(ilink).bandwidth
            :: linkinfo(ilink).background
            :: linkinfo(ilink).pfail
        Next ilink

        :: objfnjobset.numobjfn

        For ifnx = 1 To numfunction
            :: objfnjobset.initfn(ifnx)
        Next ifnx

        :: numfunction                 // mod - july '02, moved this here from inside the following loop
        For ifnx = 1 To numfunction

            :: agent(ifnx).currenthostid
            :: xnfunction(ifnx).ifgroup

            For inod = 1 To numnode
                :: xnfunction(ifnx).cpurate(inod)
                :: xnfunction(ifnx).felig(inod)
                :: xnfunction(ifnx).memory(inod)
            Next inod

            For ifnx2 = 1 To numfunction
                :: xnfunction(ifnx).fcall(ifnx2).sendmsgrate
            Next ifnx2

        Next ifnx

*/
public boolean
writeAgentState(Writer w,
                Function[] functions, int numfunction,
				Link[] links, int numlink,
				Node[] nodes, int numnode,
				Agent[] agents,
				ObjFnJobSet objfn,
				boolean sendComments
				) {


    SystemDesign.logInfo("writeAgentState:");
    SystemDesign.logInfo(" numfunction: " + numfunction);
    SystemDesign.logInfo("     numlink: " + numlink);
    SystemDesign.logInfo("     numnode: " + numnode);
    SystemDesign.logInfo("");


    // Create "objective function" data.
    // For us, the initial configuration is that
    // each agent's obj function is itself - as per MB 15July
    //
//    ObjFnJobSet objfnjobset = new ObjFnJobSet();
//    for (int ifnx=1; ifnx<=numfunction; ifnx++)
//        objfnjobset.initfn[ifnx] = ifnx;

/*
    // 'find host containing each agent:
    //
    Agent agents[] = new Agent[numfunction + 1];
    for (int ia = 1; ia<=numfunction; ia++)
        agents[ia] = new Agent(0);

    // this does not end up ensuring that all currenthostid_ are valid! (host<>0)
    //
    for (int ifnx = 1; ifnx <= numfunction; ifnx++) {
        for (int inod = 1; inod <= numnode; inod++) {
            if (nodes[inod].fcalltot_[ifnx] > 0)
                agents[ifnx].currenthostid_ = inod;
            } // inod
        } // ifnx
*/


    // Write to the given Writer.
    //
    // (alg) Open file "agents.state"
    PrintWriter pw = new PrintWriter(w);


    // (alg) :: pfail
    printWithOptionalComments(pw, "0", sendComments,
                              "pfail (ignored input for EN) - File created " + new java.util.Date());

    // (alg) :: numnode (number of nodes)
    printWithOptionalComments(pw, ""+numnode, sendComments, "number of nodes");

    // (alg) For inod = 1 To numnode
    for (int inod = 1; inod <= numnode; inod++) {

        // (alg)      :: nodeinfo(inod).cpubackground
        // (alg)      :: nodeinfo(inod).cpuutilmax
        // (alg)      :: nodeinfo(inod).jipstocpusecpersec
        // (alg)      :: nodeinfo(inod).memory
        // (alg)      :: nodeinfo(inod).pfail
        printWithOptionalComments(pw, nodes[inod].cpubackground      , sendComments, "node["+inod+"].cpubackground");
        printWithOptionalComments(pw, nodes[inod].cpuutilmax         , sendComments, "node["+inod+"].cpuutilmax");
        printWithOptionalComments(pw, nodes[inod].jipstocpusecpersec , sendComments, "node["+inod+"].jipstocpusecpersec");
        printWithOptionalComments(pw, nodes[inod].memory             , sendComments, "node["+inod+"].memory");
        printWithOptionalComments(pw, nodes[inod].pfail              , sendComments, "node["+inod+"].pfail");

        }// (alg)  Next inod

    // (alg)  For ilink = 1 To numlink
    for (int ilink = 1; ilink <= numlink; ilink++) {

        // (alg)      :: linkinfo(ilink).bandwidth
        // (alg)      :: linkinfo(ilink).background
        // (alg)      :: linkinfo(ilink).pfail
        printWithOptionalComments(pw, links[ilink].bandwidth , sendComments, "links["+ilink+"].bandwidth");
        printWithOptionalComments(pw, links[ilink].background, sendComments, "links["+ilink+"].background");
        printWithOptionalComments(pw, links[ilink].pfail     , sendComments, "links["+ilink+"].pfail");

        }   // (alg)  Next ilin

    // (alg)  :: objfnjobset.numobjfn
    printWithOptionalComments(pw, objfn.numobjfn, sendComments, "objfnjobset.numobjfn");

    // (alg)  For ifnx = 1 To numfunction
    for (int ifnx = 1; ifnx <= numfunction; ifnx++) {

        // (alg)      :: objfnjobset.initfn(ifnx)
        printWithOptionalComments(pw, objfn.initfn[ifnx], sendComments, "objfnjobset.initfn["+ifnx+"]");
        }   // (alg)  Next ifnx

    // (alg)      :: numfunction       - july '02 - moved out of loop
    printWithOptionalComments(pw, numfunction, sendComments, "numfunction");

    // (alg)  For ifnx = 1 To numfunction
    for (int ifnx = 1; ifnx <= numfunction; ifnx++) {

        // (alg)      :: agent(ifnx).currenthostid
        // (alg)      :: xnfunction(ifnx).ifgroup
        printWithOptionalComments(pw, agents[ifnx].currenthostid,  sendComments, "agent["+ifnx+"].currenthostid");
        printWithOptionalComments(pw, functions[ifnx].ifgroup,     sendComments, "functions["+ifnx+"].ifgroup");

        // (alg)      For inod = 1 To numnode
        for (int inod = 1; inod <= numnode; inod++) {

            // (alg)          :: xnfunction(ifnx).cpurate(inod)
            // (alg)          :: xnfunction(ifnx).felig(inod)
            // (alg)          :: xnfunction(ifnx).memory(inod)
            printWithOptionalComments(pw, functions[ifnx].cpurate[inod], sendComments, "functions["+ifnx+"].cpurate["+inod+"]");
            printWithOptionalComments(pw, functions[ifnx].felig[inod],   sendComments, "functions["+ifnx+"].felig["+inod+"]");
            printWithOptionalComments(pw, functions[ifnx].memory[inod] , sendComments, "functions["+ifnx+"].memory["+inod+"]");
            }   // (alg)      Next inod

        // (alg)      For ifnx2 = 1 To numfunction
        for (int ifnx2 = 1; ifnx2 <= numfunction; ifnx2++) {

            // (alg)          :: xnfunction(ifnx).fcall(ifnx2).sendmsgrate
            printWithOptionalComments(pw, functions[ifnx].fcall[ifnx2].sendmsgrate, sendComments, "functions["+ifnx+"].fcall["+ifnx2+"].sendmsgrate");
            }   // (alg)      Next ifnx2
        }   // (alg)  Next ifnx

    pw.flush();

    SystemDesign.logInfo("");
    SystemDesign.logInfo("writeAgentState done!");

    return true;
    }   // writeAgentState


/**
    Three versions of this little semi-useful method
**/
private static void
printWithOptionalComments(PrintWriter pw, String toPrint, boolean showComments, String comment) {

    pw.print(toPrint + (showComments?("\t# " + comment):"") + "\r\n");
    }

private static void
printWithOptionalComments(PrintWriter pw, int toPrint, boolean showComments, String comment) {

    printWithOptionalComments(pw, ""+toPrint, showComments, comment);
    }

private static void
printWithOptionalComments(PrintWriter pw, float toPrint, boolean showComments, String comment) {

    printWithOptionalComments(pw, ""+toPrint, showComments, comment);
    }


/**
 * setter for annealingTime_
 */
public int
getAnnealingTime() {
    return annealingTime_;
	}


/**
 * getter for annealingTime_
 */
public void
setAnnealingTime(int at) {
    annealingTime_ = at;
	}


} // AnnealForm
