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


/**
	Port of "tracefunctionform.frm" - trace functions, including important method "agentRun".

	@version 18jun03 - fixed "integration violations"
	@version aug'03 - fixed call-by-reference problems

	@version EN2.09 - 
	@author robert.e.cranfill@boeing.com
**/
public class TraceFunctionForm {

public int	calls_ = 0;  // rec - for performance testing

private SystemDesign    systemDesign_;

//rec: unused: private int imsgcolorsearch;                                            // {VB2J [909]}	Dim imsgcolorsearch As Integer
//rec: unused: private int imsgcoloropt;                                               // {VB2J [910]}	Dim imsgcoloropt As Integer

// rec: unused: private float distlist[][] = new float[2][101];                       // {VB2J [912]}	Dim distlist(1, 100) As Single

// rec: unused: private final static int qdim = 100;                                           // {VB2J [914]}	Const qdim = 100
//rec: unused: private int iqfirst; int iqlast;                                        // {VB2J [915]}	Dim iqfirst As Integer, iqlast As Integer
//rec: unused: private int qarray[] = new int[qdim+1];                                 // {VB2J [916]}	Dim qarray(qdim) As Integer
//rec: unused: private int iqfirsttemp; int iqlasttemp;                                // {VB2J [917]}	Dim iqfirsttemp As Integer, iqlasttemp As Integer
//rec: unused: private int qarraytemp[] = new int[qdim+1];                             // {VB2J [918]}	Dim qarraytemp(qdim) As Integer


// rec - 'junk' array not used?
//private static final int junkmax = 10000;                                      // {VB2J [921]}	Const junkmax = 10000
//private static int junk[] = new int[junkmax+1];                                // {VB2J [922]}	Dim junk(junkmax) As Integer


   // {VB2J [924]}	'vb data types:
   // {VB2J [925]}	' integer 2 bytes
   // {VB2J [926]}	' long 4 bytes
   // {VB2J [927]}	' single 4 bytes
   // {VB2J [928]}	' double 8 bytes

   // {VB2J [930]}	' visual c++ data types:
   // {VB2J [931]}	'help > reference > c/c++ language and c++ libraries > c language reference > declarations and types
   // {VB2J [932]}	'    > declarations and types > storage of basic types (link on page)

   // {VB2J [934]}	' char 1 byte
   // {VB2J [935]}	' short 2 bytes
   // {VB2J [936]}	' int 4 bytes
   // {VB2J [937]}	' long 4 bytes
   // {VB2J [938]}	' float 4 bytes
   // {VB2J [939]}	' double 8 bytes
   // {VB2J [940]}	' long double 8 bytes


                                                                               // {VB2J [944]}	'void __export  __fortran allpairpath(float *dist, float *pathdist, int numnode)
                                                                               // {VB2J [945]}	'void __declspec( dllexport )  __stdcall mmcqueue(float lambda, float mu, int c, float *w, float *lq)

// rec - removed
// VBJ2_XLATE_FAILURE; // Keyword after 'Private' isn't 'Sub'.                    // {VB2J [947]}	Private Declare Sub mmcqueue_dll Lib "c:\exnihilo\c\exnihilo\exnihilo.dll" Alias "_mmcqueue@20" (ByVal lambda As Single, ByVal mu As Single, ByVal c As Integer, w As Single, lq As Single)


private boolean drawSearchPartialSolution; // was drawsearchpartialsolutionOption.Value - perhaps not used (GUI)?
private boolean drawOptPartialSolution; // was drawoptpartialsolutionOption - ditto
private boolean drawOptFullSolution; // was drawoptfullsolutionOption - "
private boolean drawMsgPathOpt; // was drawmsgpathoptCheck - "
// EN2.09 - unused: private boolean drawMsgPathAnneal; // was drawmsgpathannealCheck - "

private boolean haveExudedWarning320_ = false;

/**
 * Constructor - hold on to our systemDesign form
 */
public TraceFunctionForm(SystemDesign sd) {
    systemDesign_ = sd;
	}




/**
 * agentRun - Pretty darned important method, called 50% of the time, so optimizing here might be good.
 * 
 * @version aug'03 - fixed call-by-reference problem with SystemDesign.routeMsgTime()
 * 
 */
public void 
agentRun() {                                  // {VB2J [950]}	Private Sub agentrunCommand_Click()

	calls_++;

//	logger_.info("TraceFunctionForm.agentRun entered.");

  float msgsize, msgfreq; // unused: callfreqx;                                          // {VB2J [952]}	Dim msgsize As Single, msgfreq As Single, callfreqx As Single
//unused:  float fcallx, mu, lambda, w, lq;                                            // {VB2J [953]}	Dim fcallx As Single, mu As Single, lambda As Single, w As Single, lq As Single
//unused:  int pathnode1, pathnode2;                                                    // {VB2J [954]}	Dim pathnode1 As Integer, pathnode2 As Integer

  // rec - removed loop vars
//  int inod, inod2;                                                             // {VB2J [955]}	Dim inod As Integer, inod2 As Integer

// rec - GUI: all unused
//  int drawmsgpathsearch = 0, drawmsgpathopt = 0;                                       // {VB2J [956]}	Dim drawmsgpathsearch As Integer, drawmsgpathopt As Integer
//  int donotdrawmsgpath = 0;                                                        // {VB2J [957]}	Dim donotdrawmsgpath As Integer
//  long drawmsgcolorsearch = 0, drawmsgcoloropt = 0;                                    // {VB2J [958]}	Dim drawmsgcolorsearch As Long, drawmsgcoloropt As Long
//  int drawmsgwidthsearch = 0, drawmsgwidthopt = 0;                                     // {VB2J [959]}	Dim drawmsgwidthsearch As Integer, drawmsgwidthopt As Integer
//  int drawmsgstylesearch = 0, drawmsgstyleopt = 0;                                     // {VB2J [960]}	Dim drawmsgstylesearch As Integer, drawmsgstyleopt As Integer
//  int drawmsgtypesearch = 0, drawmsgtypeopt = 0;                                       // {VB2J [961]}	Dim drawmsgtypesearch As Integer, drawmsgtypeopt As Integer

// rec - msgsent was int, but used as bool. changed to bool.
  boolean msgsent = false;                                                                 // {VB2J [962]}	Dim msgsent As Integer
// rec - never used:  int msgtype = 0;                                                                 // {VB2J [963]}	Dim msgtype As Integer
  float distx = 0, distxt = 0;                                                        // {VB2J [964]}	Dim distx As Single, distxt As Single
  boolean specifiedpath = false;   // rec - was int, used as bool. changed to bool.             // {VB2J [965]}	Dim specifiedpath As Integer

//unused:  int inodtest;                                                                // {VB2J [967]}	Dim inodtest As Integer
//unused:  int iqdata;                                                                  // {VB2J [968]}	Dim iqdata As Integer
//unused:  int iqmode;                                                                  // {VB2J [969]}	Dim iqmode As Integer
  String node1, node2;                                                         // {VB2J [970]}	Dim node1 As String, node2 As String
//unused:  int reclen;                                                                  // {VB2J [971]}	Dim reclen As Integer

//unused:  int /* rec - loop var: ifn, */ ifncall;                                                            // {VB2J [973]}	Dim ifn As Integer, ifncall As Integer

//unused:  float threadtime;                                                           // {VB2J [975]}	Dim threadtime As Single

//unused:  boolean failedstatemsg;                                                      // {VB2J [977]}	Dim failedstatemsg As Boolean

// the messages this exudes are many and seemingly pointless....
  boolean infomsg = false;                                                             // {VB2J [978]}	Dim infomsg As Boolean
//unused:  boolean fatalmsg;                                                            // {VB2J [979]}	Dim fatalmsg As Boolean

                                                                               // {VB2J [981]}	'pathsave(ipath,inode) is the list of nodes on path number ipath in reverse order
                                                                               // {VB2J [982]}	'this is needed to paint full solutions at the end of the increment
                                                                               // {VB2J [983]}	'Dim pathsave(numnodemax2, numnodemax) As Integer

/**
	rec - removed GUI code

  if (failedstatemsgCheck.Value == 1)  {                                       // {VB2J [987]}	If (failedstatemsgCheck.Value = 1) Then
    failedstatemsg = true;                                                     // {VB2J [988]}	   failedstatemsg = true
    } else {                                                                   // {VB2J [989]}	Else
    failedstatemsg = false;                                                    // {VB2J [990]}	   failedstatemsg = false
    }                                                                          // {VB2J [991]}	End If

  if (infomsgCheck.Value == 1)  {                                              // {VB2J [993]}	If (infomsgCheck.Value = 1) Then
    infomsg = true;                                                            // {VB2J [994]}	   infomsg = true
    } else {                                                                   // {VB2J [995]}	Else
    infomsg = false;                                                           // {VB2J [996]}	   infomsg = false
    }                                                                          // {VB2J [997]}	End If

  if (fatalmsgCheck.Value == 1)  {                                             // {VB2J [999]}	If (fatalmsgCheck.Value = 1) Then
    fatalmsg = true;                                                           // {VB2J [1000]}	   fatalmsg = true
    } else {                                                                   // {VB2J [1001]}	Else
    fatalmsg = false;                                                          // {VB2J [1002]}	   fatalmsg = false
    }                                                                          // {VB2J [1003]}	End If



  if (drawmsgtypeoptgraphicsOption.Value)  {                                   // {VB2J [1007]}	If (drawmsgtypeoptgraphicsOption.Value) Then
    drawmsgtypeopt = 0;                                                        // {VB2J [1008]}	   drawmsgtypeopt = 0
    } else {                                                                   // {VB2J [1009]}	Else
    drawmsgtypeopt = 1;                                                        // {VB2J [1010]}	   drawmsgtypeopt = 1
    }                                                                          // {VB2J [1011]}	End If

  if (drawmsgtypesearchgraphicsOption.Value)  {                                // {VB2J [1013]}	If (drawmsgtypesearchgraphicsOption.Value) Then
    drawmsgtypesearch = 0;                                                     // {VB2J [1014]}	   drawmsgtypesearch = 0
    } else {                                                                   // {VB2J [1015]}	Else
    drawmsgtypesearch = 1;                                                     // {VB2J [1016]}	   drawmsgtypesearch = 1
    }                                                                          // {VB2J [1017]}	End If

  if (drawmsgtypeannealgraphicsOption.Value)  {                                // {VB2J [1019]}	If (drawmsgtypeannealgraphicsOption.Value) Then
    drawmsgtypeanneal = 0;                                                     // {VB2J [1020]}	   drawmsgtypeanneal = 0
    } else {                                                                   // {VB2J [1021]}	Else
    drawmsgtypeanneal = 1;                                                     // {VB2J [1022]}	   drawmsgtypeanneal = 1
    }                                                                          // {VB2J [1023]}	End If


  donotdrawmsgpath = 0;                                                        // {VB2J [1026]}	donotdrawmsgpath = 0

  drawmsgpathsearch = drawmsgpathsearchCheck.Value;                            // {VB2J [1028]}	drawmsgpathsearch = drawmsgpathsearchCheck.Value
  drawmsgpathopt = drawmsgpathoptCheck.Value;                                  // {VB2J [1029]}	drawmsgpathopt = drawmsgpathoptCheck.Value
  drawmsgpathanneal = drawmsgpathannealCheck.Value;                            // {VB2J [1030]}	drawmsgpathanneal = drawmsgpathannealCheck.Value

  drawmsgcolorsearch = msgcolorsearchLabel.BackColor;                          // {VB2J [1032]}	drawmsgcolorsearch = msgcolorsearchLabel.BackColor
  drawmsgcoloropt = msgcoloroptLabel.BackColor;                                // {VB2J [1033]}	drawmsgcoloropt = msgcoloroptLabel.BackColor
  drawmsgcoloranneal = msgcolorannealLabel.BackColor;                          // {VB2J [1034]}	drawmsgcoloranneal = msgcolorannealLabel.BackColor

  drawmsgwidthsearch = msgwidthsearchCombo.ListIndex + 1;                      // {VB2J [1036]}	drawmsgwidthsearch = msgwidthsearchCombo.ListIndex + 1
  drawmsgwidthopt = msgwidthoptCombo.ListIndex + 1;                            // {VB2J [1037]}	drawmsgwidthopt = msgwidthoptCombo.ListIndex + 1
  drawmsgwidthanneal = msgwidthannealCombo.ListIndex + 1;                      // {VB2J [1038]}	drawmsgwidthanneal = msgwidthannealCombo.ListIndex + 1

  drawmsgstylesearch = msgstylesearchCombo.ListIndex;                          // {VB2J [1040]}	drawmsgstylesearch = msgstylesearchCombo.ListIndex
  drawmsgstyleopt = msgstyleoptCombo.ListIndex;                                // {VB2J [1041]}	drawmsgstyleopt = msgstyleoptCombo.ListIndex
  drawmsgstyleanneal = msgstyleannealCombo.ListIndex;                          // {VB2J [1042]}	drawmsgstyleanneal = msgstyleannealCombo.ListIndex

*/ // rec - end removed GUI code

  specifiedpath = false;                                                           // {VB2J [1044]}	specifiedpath = 0
  
  // rec - decl
  int ipath = 0;                                                                   // {VB2J [1045]}	ipath = 0


/* rec - removed GUI code

                                                                               // {VB2J [1049]}	   'erase previous line icons:
                                                                               // {VB2J [1050]}	   'new add to trace
                                                                               // {VB2J [1051]}	   'If (drawmsgtypeopt = 1) Then
  if ((drawmsgtypeopt == 1 & drawmsgpathoptCheck.Value == 1) | 
      (drawmsgtypesearch == 1 & drawmsgpathsearchCheck.Value == 1) |
      (drawmsgtypeanneal == 1 & drawmsgpathannealCheck.Value == 1))  { // {VB2J [1054]}	   If ((drawmsgtypeopt = 1 And drawmsgpathoptCheck.Value = 1) Or    (drawmsgtypesearch = 1 And drawmsgpathsearchCheck.Value = 1) Or    (drawmsgtypeanneal = 1 And drawmsgpathannealCheck.Value = 1)) Then
    for (int ilink=1; ilink<=numlink; ilink++) {                               // {VB2J [1055]}	      For ilink = 1 To numlink

      xxx = systemdesign.Lineicon[ilink].Visible;                              // {VB2J [1057]}	         xxx = systemdesign.Lineicon(ilink).Visible
      systemdesign.Lineicon[ilink].Visible = false;                            // {VB2J [1058]}	         systemdesign.Lineicon(ilink).Visible = false
      xxx = systemdesign.Lineicon[ilink].Visible;                              // {VB2J [1059]}	         xxx = systemdesign.Lineicon(ilink).Visible
      }                                                                        // {VB2J [1060]}	      Next ilink
    }                                                                          // {VB2J [1061]}	   End If

  DoEvents(); //W FUNC;                                                        // {VB2J [1063]}	   DoEvents

  DoEvents(); //W FUNC;                                                        // {VB2J [1070]}	DoEvents

*/ // rec - end removed GUI code


  for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                                 // {VB2J [1079]}	For ilink = 1 To numlink
    systemDesign_.linkinfo[ilink].traffic = 0;                                               // {VB2J [1080]}	   linkinfo(ilink).traffic = 0
    systemDesign_.linkinfo[ilink].linkused = false;                                          // {VB2J [1081]}	   linkinfo(ilink).linkused = false
    }                                                                          // {VB2J [1082]}	Next ilink

  for (int inod=1; inod<=systemDesign_.numnode; inod++) {                                    // {VB2J [1084]}	For inod = 1 To numnode

// rec - common failure here... why ???
if (systemDesign_.nodeinfo[inod] == null) {
    SystemDesign.logError("agentRun: nodinfo[" + inod + "] is null!");
    SystemDesign.logError("agentRun: systemDesign_.numnode=" + systemDesign_.numnode);        
    // uh, should probably return here... or ???
	}

    systemDesign_.nodeinfo[inod].cpuutil = 0;                                                // {VB2J [1085]}	   nodeinfo(inod).cpuutil = 0

    systemDesign_.nodeinfo[inod].packetrate = 0;                                             // {VB2J [1086]}	   nodeinfo(inod).packetrate = 0
    systemDesign_.nodeinfo[inod].nodeused = false;                                           // {VB2J [1087]}	   nodeinfo(inod).nodeused = false

    systemDesign_.nodeinfo[inod].memoryused = 0;                                             // {VB2J [1089]}	   nodeinfo(inod).memoryused = 0

    for (int ifn=1; ifn<=systemDesign_.numfunction; ifn++) {                                 // {VB2J [1091]}	   For ifn = 1 To numfunction
      systemDesign_.nodeinfo[inod].fcalltot[ifn] = 0;                                        // {VB2J [1092]}	      nodeinfo(inod).fcalltot(ifn) = 0
      }                                                                        // {VB2J [1093]}	   Next ifn
    }                                                                          // {VB2J [1094]}	Next inod

  systemDesign_.ftracetime = 0;                                                              // {VB2J [1096]}	ftracetime = 0

  systemDesign_.numnodeevent = 1;                                                            // {VB2J [1099]}	numnodeevent = 1

                                                                               // {VB2J [1108]}	''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
                                                                               // {VB2J [1109]}	'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
                                                                               // {VB2J [1110]}	'''''''''''''''''''''''''''''''''''''''''''''''''''''
                                                                               // {VB2J [1111]}	''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

                                                                               // {VB2J [1114]}	'loop over all agents in objective function task set:

                                                                               // {VB2J [1116]}	'For ifn = 1 To objfnjobset.numobjfn
                                                                               // {VB2J [1117]}	'   For ifnx = 1 To numfunction
                                                                               // {VB2J [1118]}	'      Line Input #filenum, record
                                                                               // {VB2J [1119]}	'      objfnjobset.initfn(ifnx) = record
                                                                               // {VB2J [1120]}	'   Next ifnx
                                                                               // {VB2J [1121]}	'Next ifn

  for (int ifnjob=1; ifnjob<=systemDesign_.objfnjobset.numobjfn; ifnjob++) {                 // {VB2J [1128]}	For ifnjob = 1 To objfnjobset.numobjfn

	// rec - decl
	int ifnx = systemDesign_.objfnjobset.initfn[ifnjob];                                         // {VB2J [1129]}	   ifnx = objfnjobset.initfn(ifnjob)
                                                                               // {VB2J [1130]}	   'inod = agent(ifnx).currenthostid
                                                                               // {VB2J [1131]}	   'inod = State.fstatecall(ifnx, inod)
	// rec - decl
    int inod = systemDesign_.agent[ifnx].testhostid;                                             // {VB2J [1132]}	   inod = agent(ifnx).testhostid


// 14may03
if (false && ifnx == 8)
	if (systemDesign_.xnfunction[ifnx].felig[inod] == 0) {
		SystemDesign.logWarn("****** TFF: Invalid eligibility!!! ifnx=" + ifnx + "; inod=" + inod);
	}



                                                                               // {VB2J [1137]}	   'test node inod for utilization
// rec - removed test code
/*
    if (1 == 2)  {                                                             // {VB2J [1139]}	   If (1 = 2) Then
      xxx = xnfunction[ifnx].name;                                             // {VB2J [1140]}	      xxx = xnfunction(ifnx).name
      xxx = nodeinfo[inod].name;                                               // {VB2J [1141]}	      xxx = nodeinfo(inod).name
      xxx = xnfunction[ifnx2].name;                                            // {VB2J [1142]}	      xxx = xnfunction(ifnx2).name
      xxx = nodeinfo[inod2].name;                                              // {VB2J [1143]}	      xxx = nodeinfo(inod2).name
      }                                                                        // {VB2J [1144]}	   End If
*/ // rec - end removed test code

// 30apr03
//'9/9/02 eliminate the test on xnfunction(ifnx).cpurate(inod) > 0:
//   'If (xnfunction(ifnx).cpurate(inod) > 0) Then
//   
//    if (systemDesign_.xnfunction[ifnx].cpurate[inod] > 0)  {                                 // {VB2J [1146]}	   If (xnfunction(ifnx).cpurate(inod) > 0) Then
      if (100 * systemDesign_.xnfunction[ifnx].cpurate[inod] / systemDesign_.nodeinfo[inod].cpucount + 
	  	  systemDesign_.nodeinfo[inod].cpubackground + 
		  100 * systemDesign_.nodeinfo[inod].cpuutil 
		  		<= systemDesign_.nodeinfo[inod].cpuutilmax)  { // {VB2J [1147]}	      If (100 * xnfunction(ifnx).cpurate(inod) / nodeinfo(inod).cpucount + nodeinfo(inod).cpubackground + 100 * nodeinfo(inod).cpuutil <= nodeinfo(inod).cpuutilmax) Then
        systemDesign_.nodeinfo[inod].cpuutil += systemDesign_.xnfunction[ifnx].cpurate[inod] / systemDesign_.nodeinfo[inod].cpucount;              // {VB2J [1148]}	         nodeinfo(inod).cpuutil = nodeinfo(inod).cpuutil + xnfunction(ifnx).cpurate(inod) / nodeinfo(inod).cpucount ' + nodeinfo(inod).cpubackground
        systemDesign_.nodeinfo[inod].fcalltot[ifnx] = 1;                                     // {VB2J [1149]}	         nodeinfo(inod).fcalltot(ifnx) = 1
        systemDesign_.nodeinfo[inod].nodeused = true;                                        // {VB2J [1150]}	         nodeinfo(inod).nodeused = true
        } else {                                                               // {VB2J [1151]}	      Else
          systemDesign_.state.status = false;                                                  // {VB2J [1152]}	         State.status = false

        if (infomsg)  {                                                        // {VB2J [1154]}	         If (infomsg) Then
          SystemDesign.logInfo("TraceFunctionForm: Current Search Node Cannot Support CPU Requirements of Current Call:");              // {VB2J [1155]}	            recout = "INFORMATION:" & Chr(10) & Chr(10) & "Current Search Node Cannot Support CPU Requirements of Current Call:"
          SystemDesign.logInfo("  Current Node Name: " + systemDesign_.nodeinfo[inod].name);              // {VB2J [1156]}	            recout = recout & Chr(10) & Chr(10) & "Current Node Name: " & nodeinfo(inod).name
          SystemDesign.logInfo("  Current Function Name: " + systemDesign_.xnfunction[ifnx].name);              // {VB2J [1157]}	            recout = recout & Chr(10) & "Current Function Name: " & xnfunction(ifnx).name
          // {VB2J [1158]}	            MsgBox recout
          }                                                                    // {VB2J [1159]}	         End If

        return;                                                                // {VB2J [1161]}	         Exit Sub
        }                                                                      // {VB2J [1162]}	      End If
//      }                                                                        // {VB2J [1163]}	   End If


// rec - sometimes happens. when annealing solution not possible?
	if (systemDesign_.nodeinfo[inod] == null) {
    
        if (!haveExudedWarning320_) {
            SystemDesign.logWarn("TraceFunctionForm.runAgent: systemDesign_.nodeinfo[" + inod + "] is null!");
            SystemDesign.logWarn("TraceFunctionForm.runAgent: Probably indicates no solution (will verify this later...only warning once)");
        }
        haveExudedWarning320_ = true;
        return;
    }
    systemDesign_.nodeinfo[inod].memoryused += systemDesign_.xnfunction[ifnx].memory[inod];              // {VB2J [1166]}	   nodeinfo(inod).memoryused = nodeinfo(inod).memoryused + xnfunction(ifnx).memory(inod)

    if (systemDesign_.nodeinfo[inod].memoryused > systemDesign_.nodeinfo[inod].memory)  {                  // {VB2J [1168]}	   If (nodeinfo(inod).memoryused > nodeinfo(inod).memory) Then
      systemDesign_.state.status = false;                                                    // {VB2J [1169]}	      State.status = false

      if (infomsg)  {                                                          // {VB2J [1171]}	      If (infomsg) Then
        SystemDesign.logInfo("TraceFunctionForm: Current Search Node Cannot Support Memory Requirements of Current Call:");              // {VB2J [1172]}	         recout = "INFORMATION:" & Chr(10) & Chr(10) & "Current Search Node Cannot Support Memory Requirements of Current Call:"
        SystemDesign.logInfo("  Current Node Name: " + systemDesign_.nodeinfo[inod].name);              // {VB2J [1173]}	         recout = recout & Chr(10) & Chr(10) & "Current Node Name: " & nodeinfo(inod).name
        SystemDesign.logInfo("  Current Function Name: " + systemDesign_.xnfunction[ifnx].name);              // {VB2J [1174]}	         recout = recout & Chr(10) & "Current Function Name: " & xnfunction(ifnx).name
        // {VB2J [1175]}	         MsgBox recout
        }                                                                      // {VB2J [1176]}	      End If

      return;                                                                  // {VB2J [1178]}	      Exit Sub
      }                                                                        // {VB2J [1179]}	   End If


                                                                               // {VB2J [1182]}	   'test messaging
    for (int ifnx2=1; ifnx2<=systemDesign_.numfunction; ifnx2++) {                           // {VB2J [1183]}	   For ifnx2 = 1 To numfunction
      if (ifnx2 != ifnx)  {                                                    // {VB2J [1184]}	      If (ifnx2 <> ifnx) Then
		
		// rec - decl
        float msgrate = systemDesign_.xnfunction[ifnx].fcall[ifnx2].sendmsgrate;                   // {VB2J [1185]}	         msgrate = xnfunction(ifnx).fcall(ifnx2).sendmsgrate

                                                                               // {VB2J [1187]}	         'bug fixed 6/3/02
                                                                               // {VB2J [1188]}	         'If (msgrate > 0 And xnfunction(ifnx2).objfn) Then
                                                                               // {VB2J [1189]}	         'bug changed back to orig on 7/15/02
                                                                               // {VB2J [1190]}	         'If (xnfunction(ifnx).fcall(ifnx2).callpct > 0 And xnfunction(ifnx2).objfn) Then
        if (msgrate > 0 && systemDesign_.xnfunction[ifnx2].objfn)  {                          // {VB2J [1191]}	         If (msgrate > 0 And xnfunction(ifnx2).objfn) Then

          msgsize = msgrate;                                                   // {VB2J [1193]}	            msgsize = msgrate
          msgfreq = 1;                                                         // {VB2J [1194]}	            msgfreq = 1
                                                                               // {VB2J [1195]}	            'find message node
                                                                               // {VB2J [1196]}	            'inod2 = agent(ifnx2).currenthostid
                                                                               // {VB2J [1197]}	            'inod2 = State.fstatecall(ifnx2, inod)
		  // rec - decl
          int inod2 = systemDesign_.agent[ifnx2].testhostid;                                     // {VB2J [1198]}	            inod2 = agent(ifnx2).testhostid


		  // rec - decl
          boolean localhostlink = false;                                               // {VB2J [1201]}	            localhostlink = false

          if (inod == inod2)  {                                                // {VB2J [1203]}	            If (inod = inod2) Then
            distx = 0;                                                         // {VB2J [1204]}	               distx = 0
            distxt = 0;                                                        // {VB2J [1205]}	               distxt = 0
            msgsent = true;                                                    // {VB2J [1206]}	               msgsent = true
            localhostlink = true;                                              // {VB2J [1207]}	               localhostlink = true
            } else {                                                           // {VB2J [1208]}	            Else
            // rec - changed to boolean var 'drawSearchPartialSolution'
			if (drawSearchPartialSolution)  {                      // {VB2J [1209]}	               If (drawsearchpartialsolutionOption.Value) Then
                                                                               // {VB2J [1210]}	                  'Sub routemsgtimemenu(msgsize As Single, msgfreq as single, pathnode1 As Integer, pathnode2 As Integer, drawmsgpath As Integer, drawmsgcolor As Long, drawmsgwidth As Integer, drawmsgstyle As Integer, drawmsgtype As Integer, msgsent As Integer, msgtype As Integer, distx As Single, distxt As Single, specifiedpath As Integer)
              systemDesign_.routeMsgTime(
			  		msgsize, msgfreq, inod, inod2,  
					msgsent, 
					distx, distxt, specifiedpath);                             // {VB2J [1211]}	                  routemsgtimemenu msgsize, msgfreq, inod, inod2, drawmsgpathsearch, drawmsgcolorsearch, drawmsgwidthsearch, drawmsgstylesearch, drawmsgtypesearch, msgsent, msgtype, distx, distxt, specifiedpath

				// rec - get systemDesign_.routeMsgTime vars that are set by side-effect
				//
				msgsent = systemDesign_.routeMsgTime_msgsent;
				distx = systemDesign_.routeMsgTime_distx;
				distxt = systemDesign_.routeMsgTime_distxtime;


			} else {                                                           // {VB2J [1212]}	               Else
			  systemDesign_.routeMsgTime(
			  		msgsize, msgfreq, inod, inod2, 
					msgsent, 
					distx, distxt, specifiedpath);                             // {VB2J [1213]}	                  routemsgtimemenu msgsize, msgfreq, inod, inod2, donotdrawmsgpath, drawmsgcolorsearch, drawmsgwidthsearch, drawmsgstylesearch, drawmsgtypesearch, msgsent, msgtype, distx, distxt, specifiedpath

				// rec - get systemDesign_.routeMsgTime vars that are set by side-effect
				//
				msgsent = systemDesign_.routeMsgTime_msgsent;
				distx = systemDesign_.routeMsgTime_distx;
				distxt = systemDesign_.routeMsgTime_distxtime;

              }                                                                // {VB2J [1214]}	               End If
            }                                                                  // {VB2J [1215]}	            End If


                                                                               // {VB2J [1218]}	            'bug: msgsent = integer type, but works ok

		  // rec - added
// EN2.09 - unused: 		  boolean linkok;

          if (msgsent == true)  {                                              // {VB2J [1220]}	            If (msgsent = true) Then
// EN2.09 - unused:            linkok = true;                                                     // {VB2J [1221]}	               linkok = true
            } else {                                                           // {VB2J [1222]}	            Else
                                                                               // {VB2J [1223]}	               'If (fatalmsg) Then
                                                                               // {VB2J [1224]}	               '   MsgBox "FATAL ERROR:" & Chr(10) & Chr(10) & "No path for function call listed below:" & Chr(10) & Chr(10) & "Calling Function: " & xnfunction(srcfn).name & Chr(10) & "Calling Node: " & nodeinfo(pathnode1).name & Chr(10) & "Function Name: " & xnfunction(ifncall).name
                                                                               // {VB2J [1225]}	               'End If

// EN2.09 - unused:             linkok = false;                                                    // {VB2J [1227]}	               linkok = false
            systemDesign_.state.status = false;                                              // {VB2J [1228]}	               State.status = false

            if (infomsg)  {                                                    // {VB2J [1230]}	               If (infomsg) Then
              // VBJ2_XLATE_FAILURE; //F UNK                                      // {VB2J [1231]}	                  lenstr nodeinfo(inod).name, node1, reclen
              // VBJ2_XLATE_FAILURE; //F UNK                                      // {VB2J [1232]}	                  lenstr nodeinfo(inod2).name, node2, reclen
              SystemDesign.logWarn("TraceFunctionForm.runAgent: Cannot find path from node '" + systemDesign_.nodeinfo[inod].name + "' to node '" + systemDesign_.nodeinfo[inod2].name + "'"); //F UNK                                      // {VB2J [1233]}	                  MsgBox "INFORMATION:" & Chr(10) & Chr(10) & "Cannot find path from node """ & node1 & """ to node """ & node2 & """"
              }                                                                // {VB2J [1234]}	               End If

            return;                                                            // {VB2J [1236]}	               Exit Sub
            }                                                                  // {VB2J [1237]}	            End If


                                                                               // {VB2J [1240]}	      ''''''''''''''''''' messaging



          if ( ! localhostlink)  {                                             // {VB2J [1244]}	            If (Not localhostlink) Then
			
			// rec - changed to drawOptPartialSolution
            if (drawOptPartialSolution)  {                         // {VB2J [1245]}	               If (drawoptpartialsolutionOption.Value) Then

				systemDesign_.routeMsgTime(
			  		msgsize, msgfreq, inod, inod2, 
					msgsent, 
					distx, distxt, specifiedpath);                             // {VB2J [1246]}	                  routemsgtimemenu msgsize, msgfreq, inod, inod2, drawmsgpathopt, drawmsgcoloropt, drawmsgwidthopt, drawmsgstyleopt, drawmsgtypeopt, msgsent, msgtype, distx, distxt, specifiedpath


				// rec - get systemDesign_.routeMsgTime vars that are set by side-effect
				//
				msgsent = systemDesign_.routeMsgTime_msgsent;
				distx = systemDesign_.routeMsgTime_distx;
				distxt = systemDesign_.routeMsgTime_distxtime;



			  } else {                                                         // {VB2J [1247]}	               Else
                                                                               // {VB2J [1248]}	                  'store path segments in pathsave(ipath, inodpath)
                                                                               // {VB2J [1249]}	                  'draw collection of all successful paths after total solution is found.

              systemDesign_.routeMsgTime(
			  		msgsize, msgfreq, inod, inod2, 
					msgsent, 
					distx, distxt, specifiedpath);                             // {VB2J [1251]}	                  routemsgtimemenu msgsize, msgfreq, inod, inod2, donotdrawmsgpath, drawmsgcoloropt, drawmsgwidthopt, drawmsgstyleopt, drawmsgtypeopt, msgsent, msgtype, distx, distxt, specifiedpath

				// rec - get systemDesign_.routeMsgTime vars that are set by side-effect
				//
				msgsent = systemDesign_.routeMsgTime_msgsent;
				distx = systemDesign_.routeMsgTime_distx;
				distxt = systemDesign_.routeMsgTime_distxtime;


              ipath++;                                               // {VB2J [1253]}	                  ipath = ipath + 1
              systemDesign_.pathsave[ipath][0] = systemDesign_.numpathnode;                                // {VB2J [1254]}	                  pathsave(ipath, 0) = numpathnode
              for (int inodpath=1; inodpath<=systemDesign_.numpathnode; inodpath++) {        // {VB2J [1256]}	                  For inodpath = 1 To numpathnode
                systemDesign_.pathsave[ipath][inodpath] = systemDesign_.path[inodpath];                    // {VB2J [1257]}	                     pathsave(ipath, inodpath) = path(inodpath)
                }                                                              // {VB2J [1258]}	                  Next inodpath
              }                                                                // {VB2J [1260]}	               End If
                                                                               // {VB2J [1263]}	               'determine probabilities:
                                                                               // {VB2J [1265]}	               'associate the internodal path with a defined link, and set colors for data and data requests:
                                                                               // {VB2J [1266]}	               'probx = nodeinfo(1).pfail

			// rec - decl (var is unused, tho, so I commented it out)
            // float probx = 1f;                                                         // {VB2J [1268]}	               probx = 1

			// rec - decl
            int pathnodx1 = systemDesign_.path[1];                                               // {VB2J [1269]}	               pathnodx1 = path(1)

if (systemDesign_.nodeinfo[pathnodx1]==null) {
	SystemDesign.logWarn("systemDesign_.nodeinfo[" + pathnodx1 + "] is null!");
}
            systemDesign_.nodeinfo[pathnodx1].nodeused = true;                               // {VB2J [1270]}	               nodeinfo(pathnodx1).nodeused = true

            for (int inodpath=1; inodpath<=systemDesign_.numpathnode-1; inodpath++) {        // {VB2J [1272]}	               For inodpath = 1 To numpathnode - 1
              pathnodx1 = systemDesign_.path[inodpath];                                      // {VB2J [1273]}	                  pathnodx1 = path(inodpath)
 			  // rec - decl
              int pathnodx2 = systemDesign_.path[inodpath + 1];                                  // {VB2J [1274]}	                  pathnodx2 = path(inodpath + 1)

 			  // rec - decl
              int ilinksav = -1;                                                   // {VB2J [1276]}	                  ilinksav = -1

              for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                     // {VB2J [1278]}	                  For ilink = 1 To numlink
 			    // rec - decl*2
                int linknode1 = systemDesign_.linkinfo[ilink].node1;                             // {VB2J [1279]}	                     linknode1 = linkinfo(ilink).node1
                int linknode2 = systemDesign_.linkinfo[ilink].node2;                             // {VB2J [1280]}	                     linknode2 = linkinfo(ilink).node2
                if ((linknode1 == pathnodx2 && linknode2 == pathnodx1) || 
				    (linknode1 == pathnodx1 && linknode2 == pathnodx2))  { // {VB2J [1281]}	                     If ((linknode1 = pathnodx2 And linknode2 = pathnodx1) Or (linknode1 = pathnodx1 And linknode2 = pathnodx2)) Then
                  ilinksav = ilink;                                            // {VB2J [1282]}	                        ilinksav = ilink
                  break;                                                       // {VB2J [1283]}	                        Exit For
                  }                                                            // {VB2J [1284]}	                     End If
                }                                                              // {VB2J [1285]}	                  Next ilink

              systemDesign_.nodeinfo[pathnodx2].nodeused = true;                             // {VB2J [1287]}	                  nodeinfo(pathnodx2).nodeused = true
              systemDesign_.linkinfo[ilinksav].linkused = true;                              // {VB2J [1288]}	                  linkinfo(ilinksav).linkused = true
              }                                                                // {VB2J [1289]}	               Next inodpath

            }                                                                  // {VB2J [1291]}	            End If


// 30apr03
if (!systemDesign_.darparun) {

                                                                               // {VB2J [1295]}	            'update the node event list:
          systemDesign_.numnodeevent++;                                     // {VB2J [1296]}	            numnodeevent = numnodeevent + 1
          systemDesign_.nodeevent[systemDesign_.numnodeevent].node = inod2;                                // {VB2J [1297]}	            nodeevent(numnodeevent).node = inod2
          systemDesign_.nodeevent[systemDesign_.numnodeevent].callfreq = 1;                                // {VB2J [1298]}	            nodeevent(numnodeevent).callfreq = 1
          systemDesign_.nodeevent[systemDesign_.numnodeevent].cpu = systemDesign_.xnfunction[ifnx2].cpu[inod2] * msgfreq;              // {VB2J [1299]}	            nodeevent(numnodeevent).cpu = xnfunction(ifnx2).cpu(inod2) * msgfreq
          systemDesign_.nodeevent[systemDesign_.numnodeevent].msgsize = msgsize * msgfreq;                 // {VB2J [1300]}	            nodeevent(numnodeevent).msgsize = msgsize * msgfreq

          systemDesign_.nodeevent[systemDesign_.numnodeevent].memory = systemDesign_.xnfunction[ifnx2].memory[inod2];              // {VB2J [1302]}	            nodeevent(numnodeevent).memory = xnfunction(ifnx2).memory(inod2)

                                                                               // {VB2J [1304]}	            ' nodeevent(numnodeevent).ftrace.numftrace = 1
                                                                               // {VB2J [1305]}	            ' nodeevent(numnodeevent).ftrace.node(1) = srcnod
                                                                               // {VB2J [1306]}	            ' nodeevent(numnodeevent).ftrace.f(1) = srcfn
                                                                               // {VB2J [1307]}	            'the nodes in path(*) are all nodes along the path from calling function srcfn
                                                                               // {VB2J [1308]}	            ' on srcnod to called function ifncall on node inodopt, in reverse
                                                                               // {VB2J [1309]}	            'order, so path(1) = inodopt, and path(numpathnode) = srcnod

                                                                               // {VB2J [1311]}	            'ifn = 3 => user1
                                                                               // {VB2J [1312]}	            'ifn = 4 => w1
                                                                               // {VB2J [1313]}	            'ifn = 5 => wimage1

          systemDesign_.nodeevent[systemDesign_.numnodeevent].f = ifnx2;                                   // {VB2J [1315]}	            nodeevent(numnodeevent).f = ifnx2
          systemDesign_.nodeevent[systemDesign_.numnodeevent].fcalledby = ifnx;                            // {VB2J [1316]}	            nodeevent(numnodeevent).fcalledby = ifnx  'user initiated calls default to function 0

                                                                               // {VB2J [1318]}	      '      nodeevent(numnodeevent).fnodecalledby = srcnod  'user initiated calls default to node 0
          systemDesign_.nodeevent[systemDesign_.numnodeevent].fnodecalledby = inod;                        // {VB2J [1319]}	            nodeevent(numnodeevent).fnodecalledby = inod  'user initiated calls default to node 0

                                                                               // {VB2J [1321]}	'            nodeinfo(inod2).memoryused = nodeinfo(inod2).memoryused + xnfunction(ifnx2).memory(inod2)

} // !darparun 30apr03


// removed empty 'if'
//          if (systemDesign_.xnfunction[ifnx2].cpurate[inod2] > 0)  {                         // {VB2J [1325]}	            If (xnfunction(ifnx2).cpurate(inod2) > 0) Then
                                                                               // {VB2J [1326]}	               'nodeinfo(inod2).cpuutil = nodeinfo(inod2).cpuutil + xnfunction(ifnx2).cpurate(inod2) / nodeinfo(inod2).cpucount
                                                                               // {VB2J [1327]}	               'Utilization = sum of all function usage divided by number of processors per server
                                                                               // {VB2J [1328]}	               'nodeinfo(inodopt).fcalltot(ifncall) = nodeinfo(inodopt).fcalltot(ifncall) + 1
                                                                               // {VB2J [1329]}	               'nodeinfo(inod2).fcalltot(ifnx2) = 1
//            }                                                                  // {VB2J [1330]}	            End If




          if ( ! localhostlink)  {                                             // {VB2J [1339]}	            If (Not localhostlink) Then
            for (int inodpath=1; inodpath<=systemDesign_.numpathnode-1; inodpath++) {        // {VB2J [1340]}	               For inodpath = 1 To numpathnode - 1

//				30apr03
			if (!systemDesign_.darparun) {
              systemDesign_.nodeevent[systemDesign_.numnodeevent].ftrace.path[inodpath] = systemDesign_.path[systemDesign_.numpathnode + 1 - inodpath];              // {VB2J [1341]}	                  nodeevent(numnodeevent).ftrace.path(inodpath) = path(numpathnode + 1 - inodpath)
			}                                                                  // {VB2J [1342]}	                  'now loop over all links, and add events to update traffic on all links along path

              for (int ilink=1; ilink<=systemDesign_.numlink; ilink++) {                     // {VB2J [1344]}	                  For ilink = 1 To numlink

                if (systemDesign_.linkinfo[ilink].type != Link.LINK_TYPE_LAN)  {                              // {VB2J [1346]}	                     If (linkinfo(ilink).type <> 1) Then
                  if ((systemDesign_.linkinfo[ilink].node1 == systemDesign_.path[systemDesign_.numpathnode + 1 - inodpath]) &
				      (systemDesign_.linkinfo[ilink].node2 == systemDesign_.path[systemDesign_.numpathnode - inodpath]))  { // {VB2J [1347]}	                        If ((linkinfo(ilink).node1 = path(numpathnode + 1 - inodpath)) And (linkinfo(ilink).node2 = path(numpathnode - inodpath))) Then
                    systemDesign_.linkinfo[ilink].traffic += msgsize * msgfreq;              // {VB2J [1348]}	                           linkinfo(ilink).traffic = linkinfo(ilink).traffic + msgsize * msgfreq
                    if (systemDesign_.nodeinfo[systemDesign_.linkinfo[ilink].node2].type == Node.TYPE_ROUTER)  {          // {VB2J [1349]}	                           If (nodeinfo(linkinfo(ilink).node2).type = 2) Then 'router with incoming packets
                      systemDesign_.nodeinfo[systemDesign_.linkinfo[ilink].node2].packetrate +=
							(msgsize / systemDesign_.linkinfo[ilink].mtu) * msgfreq;              // {VB2J [1350]}	                              nodeinfo(linkinfo(ilink).node2).packetrate = nodeinfo(linkinfo(ilink).node2).packetrate + (msgsize / linkinfo(ilink).mtu) * msgfreq
                      }                                                        // {VB2J [1351]}	                           End If

                    break;                                                     // {VB2J [1353]}	                           Exit For
                    }                                                          // {VB2J [1354]}	                        End If
                  } else {                                                     // {VB2J [1355]}	                     Else  'LAN link so update all other associated links
                                                                               // {VB2J [1356]}	                        'Link type linkinfo(*).type = 1 for LAN links
                                                                               // {VB2J [1357]}	                        'Node type nodeinfo(linkinfo(ilink).node1).type = 3 is for LAN nodes
                                                                               // {VB2J [1358]}	                        ' that are at the starting position of the directed link traffic
                                                                               // {VB2J [1359]}	                        '
                                                                               // {VB2J [1360]}	                        'Node type nodeinfo(linkinfo(ilink).node2).type = 3 is for LAN nodes
                                                                               // {VB2J [1361]}	                        ' that are at the ending position of the directed link traffic


                  if ((systemDesign_.linkinfo[ilink].node1 == systemDesign_.path[systemDesign_.numpathnode + 1 - inodpath]) &&
				      (systemDesign_.linkinfo[ilink].node2 == systemDesign_.path[systemDesign_.numpathnode - inodpath]) || 
					  (systemDesign_.linkinfo[ilink].node2 == systemDesign_.path[systemDesign_.numpathnode + 1 - inodpath]) && 
					  (systemDesign_.linkinfo[ilink].node1 == systemDesign_.path[systemDesign_.numpathnode - inodpath]))  { // {VB2J [1367]}	                        If ((linkinfo(ilink).node1 = path(numpathnode + 1 - inodpath))                            And (linkinfo(ilink).node2 = path(numpathnode - inodpath))                            Or (linkinfo(ilink).node2 = path(numpathnode + 1 - inodpath))                            And (linkinfo(ilink).node1 = path(numpathnode - inodpath))) Then

                                                                               // {VB2J [1369]}	                           'nodeevent(numnodeevent).ftrace.path(inodpath) is the current internodal starting node
                                                                               // {VB2J [1370]}	                           'for the message path.
                                                                               // {VB2J [1371]}	                           'nodeevent(numnodeevent).ftrace.path(inodpath+1) is the current internodal ending node
                                                                               // {VB2J [1372]}	                           'for the message path.

                                                                               // {VB2J [1374]}	                           'IMPORTANT: Even though LAN links point from linkinfo(ilink).node1 to linkinfo(ilink).node2,
                                                                               // {VB2J [1375]}	                           'a path defined by the path(ix) may describe traffic in either direction.


                                                                               // {VB2J [1378]}	                           'since path(i) is path in reverse order, path(1) = finish, path(numpathnode) = start
                                                                               // {VB2J [1379]}	                           'then message path is from path(numpathnode + 1 - inodpath) to path(numpathnode  - inodpath)

                    if (systemDesign_.nodeinfo[systemDesign_.path[systemDesign_.numpathnode - inodpath]].type == Node.TYPE_LAN)  {   // {VB2J [1381]}	                           If (nodeinfo(path(numpathnode - inodpath)).type = 3) Then 'target node is lan node

// rec - removed test code
/*
                      if (1 == 2)  {                                           // {VB2J [1383]}	                              If (1 = 2) Then
                        X1 = nodeinfo[linkinfo[ilink].node1].name;             // {VB2J [1384]}	                                 X1 = nodeinfo(linkinfo(ilink).node1).name
                        X2 = nodeinfo[linkinfo[ilink].node2].name;             // {VB2J [1385]}	                                 X2 = nodeinfo(linkinfo(ilink).node2).name
                        XL = linkinfo[ilink].name;                             // {VB2J [1386]}	                                 XL = linkinfo(ilink).name
                        }                                                      // {VB2J [1387]}	                              End If
*/

                      systemDesign_.nodeinfo[systemDesign_.linkinfo[ilink].node2].packetrate +=  
					  		(msgsize / systemDesign_.linkinfo[ilink].mtu) * msgfreq;              // {VB2J [1389]}	                              nodeinfo(linkinfo(ilink).node2).packetrate = nodeinfo(linkinfo(ilink).node2).packetrate + (msgsize / linkinfo(ilink).mtu) * msgfreq

					  // rec - decl
                      int nodelan = systemDesign_.linkinfo[ilink].node2;                         // {VB2J [1391]}	                              nodelan = linkinfo(ilink).node2
                                                                               // {VB2J [1392]}	                              'update the LAN traffic to ALL lan connected nodes, even if they are not the target or receiver of the message
                                                                               // {VB2J [1393]}	                              'loop over all links, and set any LAN links with receiving nodes being a LAN node type link nodeinfo(linkinfonodetype = 3 to

                                                                               // {VB2J [1395]}	                              'if this is one server calling another server on the SAME LAN, then
                                                                               // {VB2J [1396]}	                              'we only need to add the LAN traffic once to each LAN link. Adding
                                                                               // {VB2J [1397]}	                              'twice is double counting for intra-LAN traffic  2/6/02

                      if (systemDesign_.path[3] == inod && systemDesign_.path[1] == inod2 && inodpath == 1 || systemDesign_.numpathnode != 3)  { // {VB2J [1399]}	                              If (path(3) = inod And path(1) = inod2 And inodpath = 1 Or numpathnode <> 3) Then
                        for (int jlink=1; jlink<=systemDesign_.numlink; jlink++) {           // {VB2J [1400]}	                                 For jlink = 1 To numlink
                          if (systemDesign_.linkinfo[jlink].node2 == nodelan)  {             // {VB2J [1401]}	                                    If (linkinfo(jlink).node2 = nodelan) Then
                            systemDesign_.linkinfo[jlink].traffic += msgsize * msgfreq;              // {VB2J [1402]}	                                       linkinfo(jlink).traffic = linkinfo(jlink).traffic + msgsize * msgfreq
                            }                                                  // {VB2J [1403]}	                                    End If
                          }                                                    // {VB2J [1404]}	                                 Next jlink
                        }                                                      // {VB2J [1405]}	                              End If
                      } else {                                                 // {VB2J [1406]}	                           Else
                                                                               // {VB2J [1407]}	                              'target node is not a lan node, so traffic flowing away from lan node
                      systemDesign_.nodeinfo[systemDesign_.linkinfo[ilink].node1].packetrate += 
					  		(msgsize / systemDesign_.linkinfo[ilink].mtu) * msgfreq;              // {VB2J [1408]}	                              nodeinfo(linkinfo(ilink).node1).packetrate = nodeinfo(linkinfo(ilink).node1).packetrate + (msgsize / linkinfo(ilink).mtu) * msgfreq
                      }                                                        // {VB2J [1409]}	                           End If

                    break;                                                     // {VB2J [1411]}	                           Exit For
                    }                                                          // {VB2J [1412]}	                        End If
                  }                                                            // {VB2J [1413]}	                     End If
                }                                                              // {VB2J [1414]}	                  Next ilink
              }                                                                // {VB2J [1415]}	               Next inodpath

// 30apr03
if (!systemDesign_.darparun) {
            systemDesign_.nodeevent[systemDesign_.numnodeevent].ftrace.path[systemDesign_.numpathnode] = systemDesign_.path[1];              // {VB2J [1416]}	               nodeevent(numnodeevent).ftrace.path(numpathnode) = path(1)
            systemDesign_.nodeevent[systemDesign_.numnodeevent].ftrace.numpathnode = systemDesign_.numpathnode;              // {VB2J [1418]}	               nodeevent(numnodeevent).ftrace.numpathnode = numpathnode
}

            }                                                                  // {VB2J [1419]}	            End If
          }                                                                    // {VB2J [1420]}	         End If
        }                                                                      // {VB2J [1421]}	      End If 'end if (ifnx2 <> ifnx)
      }                                                                        // {VB2J [1422]}	   Next ifnx2


    }                                                                          // {VB2J [1425]}	Next ifnjob

  systemDesign_.state.status = true;                                                         // {VB2J [1433]}	State.status = true

// rec - removed un-needed test (we just set this var on the previous line!)
//  if (systemDesign_.state.status)  {                                                         // {VB2J [1441]}	If (State.status) Then

    systemDesign_.numpath = ipath;                                                           // {VB2J [1442]}	   numpath = ipath

                                                                               // {VB2J [1444]}	'new add to trace
                                                                               // {VB2J [1445]}	   'If (drawoptfullsolutionOption.Value) Then
	// rec - changed to use local vars
    if (drawOptFullSolution && drawMsgPathOpt)  { // {VB2J [1446]}	   If (drawoptfullsolutionOption.Value And (drawmsgpathoptCheck.Value = 1)) Then

		// rec - there already is an "ipath" in this scope; changed to "ipath2" - ???
		for (int ipath2=1; ipath2<=systemDesign_.numpath; ipath2++) {                             // {VB2J [1447]}	      For ipath = 1 To numpath
          systemDesign_.numpathnode = systemDesign_.pathsave[ipath2][0];                                      // {VB2J [1448]}	         numpathnode = pathsave(ipath, 0)

          for (int inodpath=0; inodpath<=systemDesign_.numpathnode; inodpath++) {              // {VB2J [1450]}	         For inodpath = 0 To numpathnode 'store the numpathnode data in path(0)

		    // rec - changed to use more-local "ipath", now "ipath2" - ???
			systemDesign_.path[inodpath] = systemDesign_.pathsave[ipath2][inodpath];                          // {VB2J [1451]}	            path(inodpath) = pathsave(ipath, inodpath)
            }                                                                    // {VB2J [1452]}	         Next inodpath

                                                                               // {VB2J [1454]}	         'Sub drawmessagepath(numpathnode As Integer, drawmsgcolor As Long, drawmsgwidth As Integer, drawmsgstyle As Integer, drawmsgtype As Integer)
          // rec - GUI
		  // VBJ2_XLATE_FAILURE; //F UNK                                         // {VB2J [1455]}	         drawmessagepath numpathnode, drawmsgcoloropt, drawmsgwidthopt, drawmsgstyleopt, drawmsgtypeopt

        }                                                                      // {VB2J [1457]}	      Next ipath
      } 
   
    // EN2.09 - 'en 2.04 statelistpath removal: delete this entire elsif block (not used anyway)
//
//    else if (false && drawMsgPathAnneal)  {                      // {VB2J [1458]}	   ElseIf (false And drawmsgpathannealCheck.Value) Then
//      systemDesign_.numpath = systemDesign_.statelist[0].numpath;                                          // {VB2J [1459]}	      numpath = statelist(0).numpath
//
//	  // rec - there already is an "ipath" in this scope! use anyway
//      for (ipath=1; ipath<=systemDesign_.numpath; ipath++) {                             // {VB2J [1461]}	      For ipath = 1 To numpath
//        systemDesign_.numpathnode = systemDesign_.statelist[0].numpathnode[ipath];                         // {VB2J [1462]}	         numpathnode = statelist(0).numpathnode(ipath)
//
//        for (int inodpath=1; inodpath<=systemDesign_.numpathnode; inodpath++) {              // {VB2J [1464]}	         For inodpath = 1 To numpathnode
//
//		// 1.1
//		// 'marc EN 1.1 5/10/03 change statelist to statelistpath: report best solution mod
//		// 'path(inodpath) = statelist(0).path(ipath, inodpath)  'pathsave(ipath, inodpath)
//		//  systemDesign_.path[inodpath] = systemDesign_.statelist[0].path[ipath][inodpath];                 // {VB2J [1465]}	            path(inodpath) = statelist(0).path(ipath, inodpath)  'pathsave(ipath, inodpath)
// 
//		systemDesign_.path[inodpath] = systemDesign_.statelistpath[0][ipath][inodpath];  // (rec: original comment here wrong:) 'pathsave(ipath, inodpath)
// 
//          }                                                                    // {VB2J [1466]}	         Next inodpath
//
//                                                                               // {VB2J [1468]}	         'Sub drawmessagepath(numpathnode As Integer, drawmsgcolor As Long, drawmsgwidth As Integer, drawmsgstyle As Integer, drawmsgtype As Integer)
//
//		// rec - GUI
//		// VBJ2_XLATE_FAILURE; //F UNK                                            // {VB2J [1469]}	         drawmessagepath numpathnode, drawmsgcoloranneal, drawmsgwidthanneal, drawmsgstyleanneal, drawmsgtypeanneal
//
//        }                                                                      // {VB2J [1471]}	      Next ipath
//
//      }                                                                        // {VB2J [1473]}	   End If

// rec - end removed 'if' test
//    }                                                                          // {VB2J [1475]}	End If


  }                                                                            // {VB2J [1478]}	End Sub


} // TraceFunctionForm




