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
	MetroParams - converted from metrparmex.frm by VB2J;
	Implements the "Metropolis" simulated annealing algorithm.
	
	First module I've aggressively ported in a non-reversible manner! Woo-hoo!

	@version EN2.20 - changed some initial annealing temps.
	@version 1.1
		- support for hamming, etc
		- de-static-ed buncha vars
	@version 18Jun03 - fixed "integration violations"
	@author robert.e.cranfill@boeing.com
**/
public class MetroParams {


// For auto-annealing
private MetroTimer 	metroTimer_;	
private Thread 		metroThread_;

private AnnealForm      annealForm_; // for callback from MetroTimer
private int             annealTime_; // specified in constructor
private SystemDesign    systemDesign_;

// {VB2J [994]}	'Option Explicit - I wish!

private boolean 	metrparmload;                                                          // {VB2J [996]}	Dim metrparmload As Integer - rec changed to boolean
private boolean 	autoannealflag;                                                        // {VB2J [997]}	Dim autoannealflag As Boolean

private int 	autoannealcounter;                                                      // {VB2J [999]}	Dim autoannealcounter As Integer
private float 	autoannealtempmax;                                                      // {VB2J [1000]}	Dim autoannealtempmax As Single
private float 	autoannealtempmin;                                                      // {VB2J [1001]}	Dim autoannealtempmin As Single
private float 	autoannealtempmaxscrollvalue;                                           // {VB2J [1002]}	Dim autoannealtempmaxscrollvalue As Single
private float 	autoannealtempminscrollvalue;                                           // {VB2J [1003]}	Dim autoannealtempminscrollvalue As Single
// rec - changed to int:
private int	 	autoannealtime;                                                         // {VB2J [1004]}	Dim autoannealtime As Single


// rec - vars to represent/back state of GUI components that we don't have
private float	annealtempScroll_max = 1000f;	// instead of annealtempScroll.max, etc
private float	annealtempScroll_min = 0f;
private float	annealtempScroll_Value = 0f;

private float	responsetimescroll_max = 1000f;
private float	responsetimescroll_min = 0f;
private float	responsetimescroll_Value = 0f;

private float	pfailureScroll_max = 1000f;
private float	pfailureScroll_min = 0f;
private float	pfailureScroll_Value = 0f;

// directly use the vars in SystemDesign - 1.1 aug'03 ???
//private float	hammingScroll_max = 1000f;
//private float	hammingScroll_min = 0f;
//private float	hammingScroll_Value = 0f;

// EN2.09 - similarly, no "scroll value" vars here for load bal

public boolean	pfailureCheck = false;	// checkbox surrogates
public boolean	pfailuresurviveCheck = false;
public boolean	remotetrafficCheck = false;
public boolean	hammingCheck = false;
public boolean	loadbalCheck = false;

private float fixedcostvalue = 0f;
private float monthlycostvalue = 0f;
// unused: private static float pfailurevalue = 0f;
private float responsetimevalue = 0f;

private float remotetrafficvalue; // 'messaging mods
private float pfailuresurvivevalue; // 'marc EN 1.1 smoothing
private float hammingvalue; // 'marc EN 1.1 hamming

private float hammingscale, hammingave;


/**
 * Constructor needs the AnnealForm that's calling us, 
 * so we can notify it when we're done, and the annealing time to use.
 * 
 * @param annealForm: for callback from MetroTimer
 * @param annealTime: number of seconds to do the anneal in.
 */
public 
MetroParams(AnnealForm annealForm, SystemDesign sdf, int annealTime) {
	
    annealForm_     = annealForm;
    systemDesign_   = sdf;
    annealTime_     = annealTime;
    
    // 1.1 this is a funky spaghetti interaction... fix???
    if (sdf.solveWithSurvivability_)
		pfailuresurviveCheck = true;
	else 
	if (sdf.solveWithTraffic_)
		remotetrafficCheck = true;
	else
		pfailureCheck = true;

    // Do VB init stuff.
    //
    this.formLoad();
	}


/**
	rec - Stop the annealing timer.
**/
public void
stopTimer() {
	
	SystemDesign.logInfo("MetroParams.stopTimer: Killing annealing timer.");
	
	if (metroTimer_ != null) {
       metroTimer_.stopPlease();
	   metroTimer_ = null;
    }
	metroThread_ = null;
} // stopTimer



/**
 * Update "objective function" for pFail
 */
private void
pfailureScroll_Change_WHAT() {

	updateStatelistScale(MetroParams.UPDATE_STATELIST_SCALE_PFAIL);
	}


/**
 * Update "objective function" for remote traffic
 */
private void
remotetrafficScroll_Change() {

	// 22sept03
	float remotetrafficscalemax = 1000 / ((systemDesign_.remotetrafficmax - systemDesign_.remotetrafficmin) / 2); //   'en 1.1 change scale
	float remotetrafficscalemin = 0.00001f / ((systemDesign_.remotetrafficmax - systemDesign_.remotetrafficmin) / 2); //  'en 1.1 change scale
	float scalelogmin = (float)Math.log(remotetrafficscalemin);
	float scalelogmax = (float)Math.log(remotetrafficscalemax);
	float remotetrafficscalelog = scalelogmin 
				+ (scalelogmax - scalelogmin) 
				* (systemDesign_.remotetraffic - systemDesign_.remotetrafficmin) 
				/ (systemDesign_.remotetrafficmax - systemDesign_.remotetrafficmin);

	if (remotetrafficCheck) {
		systemDesign_.remotetrafficscale = RnR.Exp(remotetrafficscalelog);
//	   remotetrafficscale = remotetrafficscalex
//	   scinumformat remotetrafficscale, remotetrafficscaleouttext
//	   remotetrafficscaleText.Text = remotetrafficscaleouttext
//	   remotetrafficvalue = remotetrafficaveText.Text * remotetrafficscale
//	   scinumformat remotetrafficvalue, remotetrafficvalueouttext
		}
	else {
		systemDesign_.remotetrafficscale = 0;
//	   remotetrafficvalueouttext = "0"
//	   remotetrafficvalue = 0
//	   scinumformat remotetrafficvalue, remotetrafficvalueouttext
		}


	updateStatelistScale(MetroParams.UPDATE_STATELIST_SCALE_REMOTE_TRAFFIC);
	}


/**
 * Update "objective function" for pFail/survive (aka load balancing or smoothing)
 *  'en 1.1 smoothing
 */
private void
pfailuresurviveScroll_Change() {

	float pfailuresurvivescalemax =  10000f / ((systemDesign_.pfailuresurvivemax - systemDesign_.pfailuresurvivemin) / 2f);
	float pfailuresurvivescalemin = 0.0001f / ((systemDesign_.pfailuresurvivemax - systemDesign_.pfailuresurvivemin) / 2f);
	float scalelogmin = (float)Math.log(pfailuresurvivescalemin);
	float scalelogmax = (float)Math.log(pfailuresurvivescalemin);

	float pfailuresurvivescalelog = scalelogmin
				+ (scalelogmax - scalelogmin) 
				  * (systemDesign_.pfailuresurvive - systemDesign_.pfailuresurvivemin) 
				  / (systemDesign_.pfailuresurvivemax - systemDesign_.pfailuresurvivemin);

	if (pfailuresurviveCheck) {
		systemDesign_.pfailuresurvivescale = RnR.Exp(pfailuresurvivescalelog);
		systemDesign_.pfailuresurvive = systemDesign_.pfailuresurviveave * systemDesign_.pfailuresurvivescale;
		}
	else {
		systemDesign_.hammingscale = 0;
		systemDesign_.hamming = 0;
		}

	updateStatelistScale(MetroParams.UPDATE_STATELIST_SCALE_PFAIL_SURVIVE);
	}


/**
 * Update "objective function" for hamming
 *  'en 1.1 hamming
 */
private void
hammingScroll_Change() {
	
	float hammingscalemax =  10000f / ((systemDesign_.hammingmax - systemDesign_.hammingmin) / 2f);
	float hammingscalemin = 0.0001f / ((systemDesign_.hammingmax - systemDesign_.hammingmin) / 2f);
	float scalelogmin = (float)Math.log(hammingscalemin);
	float scalelogmax = (float)Math.log(hammingscalemax);

	// 'remotetrafficscalex = remotetrafficscalemin + (remotetrafficscalemax - remotetrafficscalemin) * (remotetrafficscroll.Value - remotetrafficscroll.min) / (remotetrafficscroll.max - remotetrafficscroll.min)

	float hammingscalelog = scalelogmin
				+ (scalelogmax - scalelogmin) 
				  * (systemDesign_.hamming - systemDesign_.hammingmin) 
				  / (systemDesign_.hammingmax - systemDesign_.hammingmin);

	if (hammingCheck) {
		systemDesign_.hammingscale = RnR.Exp(hammingscalelog);
		systemDesign_.hamming = hammingave * hammingscale;
		}
	else {
		systemDesign_.hammingscale = 0;
		systemDesign_.hamming = 0;
		}


	updateStatelistScale(MetroParams.UPDATE_STATELIST_SCALE_HAMMING);
	}


//	We don't need any of these:
//
// private void
// fixedcostscroll_Change() {
//	 }
//
// private void
// monthlycostscroll_Change() {
//	 }
//
// private void
// responsetimescroll_Change() {
//	 }
//



/**
 * Based on 
 * 	Private Sub fixedcostscrollCommand_Click()
 * renamed better...
 * 
 */
public void
updateDepenedentVars() {

//	if (systemDesign_.darparun == false) {
//		fixedcostscroll_Change();
//		monthlycostscroll_Change();
//		responsetimescroll_Change();
//		}

	pfailureScroll_Change();

	remotetrafficScroll_Change(); //  'messaging mods

	// 'en 1.1 smoothing
	pfailuresurviveScroll_Change();

	// 'en 1.1 hamming
	hammingScroll_Change();

	} // updateDepenedentVars



// values for param to updateStatelistScale
//
public static final int	UPDATE_STATELIST_SCALE_PFAIL 		  = 1;
public static final int	UPDATE_STATELIST_SCALE_PFAIL_SURVIVE  = 2;
public static final int	UPDATE_STATELIST_SCALE_REMOTE_TRAFFIC = 3;
public static final int	UPDATE_STATELIST_SCALE_HAMMING 		  = 4;

/**
 *
 	'since scale has changed, we need to update the objective function for all saved states:
	'For istate = 0 To state.numfstatecallsave
	'
	'note that there is a natural bug here, and not yet fixed.
	'under the new scale, the items in statelist(istate).* are
	'probably not the best found solutions under the new scale,
	'but they are kept in the list of best solutions statelist(*)
	'for convenience. We really need to polish this up a bit.

 * @param thing
 */
public void
updateStatelistScale(int updateScaleType) {

	for (int istate=0; istate<=systemDesign_.numstatelist; istate++) {

		switch (updateScaleType) {

			case UPDATE_STATELIST_SCALE_PFAIL:
				systemDesign_.statelist[istate].pfailurescale = systemDesign_.pfailurescale;
				break;

			case UPDATE_STATELIST_SCALE_PFAIL_SURVIVE:
				systemDesign_.statelist[istate].pfailuresurvivescale = systemDesign_.pfailuresurvivescale;
				break;

			case UPDATE_STATELIST_SCALE_REMOTE_TRAFFIC:
				systemDesign_.statelist[istate].remotetrafficscale = systemDesign_.remotetrafficscale;
				systemDesign_.statelist[istate].pfailurescale = 0;	// 22sept03	
				break;

			case UPDATE_STATELIST_SCALE_HAMMING:
				systemDesign_.statelist[istate].hammingscale = systemDesign_.hammingscale;
				break;
			}

		float objx = systemDesign_.statelist[istate].fixedcost * systemDesign_.statelist[istate].fixedcostscale;
		objx += systemDesign_.statelist[istate].monthlycost * systemDesign_.statelist[istate].monthlycostscale;
		objx += (1 - systemDesign_.statelist[istate].psuccess) * systemDesign_.statelist[istate].pfailurescale;
		objx += systemDesign_.statelist[istate].responsetime * systemDesign_.statelist[istate].responsetimescale;
		objx += systemDesign_.statelist[istate].remotetraffic * systemDesign_.statelist[istate].remotetrafficscale;  // 'messaging mods
		objx += systemDesign_.statelist[istate].pfailuresurvive * systemDesign_.statelist[istate].pfailuresurvivescale; // 'marc EN 1.1 smoothing
		objx += systemDesign_.statelist[istate].hamming * systemDesign_.statelist[istate].hammingscale; // 'marc EN 1.1 hamming
		systemDesign_.statelist[istate].e = objx;
		} // Next istate

	} // updateStatelistScale


/**
	Adjust various dependent vars

	This doesn't happen dynamically, like it does in the interactive VB version,
	but we do call this once to set things up

**/
private void 
annealtempScroll_Change() {                                // {VB2J [1005]}	Private Sub annealtempScroll_Change()
                                                                               // {VB2J [1007]}	'new stuff
// rec - some of these duplicate vars in SystemDesign - intended scope ???
// unused:  float responsetimemin; float responsetimemax;                               // {VB2J [1009]}	   Dim responsetimemin As Single, responsetimemax As Single
// unused:  float monthlycostpertimemin; float monthlycostpertimemax;                   // {VB2J [1010]}	   Dim monthlycostpertimemin As Single, monthlycostpertimemax As Single
// unused:  float monthlycostscalemin; float monthlycostscalemax;                       // {VB2J [1011]}	   Dim monthlycostscalemin As Single, monthlycostscalemax As Single
// unused:  float monthlycostscalex; float monthlycostscale;                            // {VB2J [1012]}	   Dim monthlycostscalex As Single, monthlycostscale As Single

// unused:  float responsetimevalue;                                                    // {VB2J [1014]}	   Dim responsetimevalue As Single

  float annealtempx;                                                          // {VB2J [1015]}	   Dim annealtempx As Single
  float scalelogmin = (float)Math.log(0.000000001f);                                  // {VB2J [1019]}	   scalelogmin = Log(0.000000001)    '1.e-9
  float scalelogmax = (float)Math.log(1000000f);                                      // {VB2J [1020]}	   scalelogmax = Log(1000000)       '1.e6

// {VB2J [1022]}	   'monthlycostscalex = monthlycostscalemin + (monthlycostscalemax - monthlycostscalemin) * (monthlycostscroll.Value - monthlycostscroll.min) / (monthlycostscroll.max - monthlycostscroll.min)

  float annealtemplog = scalelogmin + 
  			(scalelogmax - scalelogmin) * 
			(annealtempScroll_Value - annealtempScroll_min) 
			/ (annealtempScroll_max - annealtempScroll_min);  // {VB2J [1024]}	   annealtemplog = scalelogmin + (scalelogmax - scalelogmin) * (annealtempScroll.Value - annealtempScroll.min) / (annealtempScroll.max - annealtempScroll.min)

  annealtempx = (float)Math.exp(annealtemplog);                                // {VB2J [1026]}	   annealtempx = Exp(annealtemplog)
  systemDesign_.annealtemp = annealtempx;                                                    // {VB2J [1027]}	   annealtemp = annealtempx

  SystemDesign.logInfo("MetroParams.annealtempScroll_Change: annealtemp set to " + systemDesign_.annealtemp);


// rec - GUI
//  VBJ2_XLATE_FAILURE; //F UNK                                                  // {VB2J [1028]}	   scinumformat annealtemp, annealtempouttext
//  annealtempText.text = annealtempouttext;                                     // {VB2J [1030]}	   annealtempText.text = annealtempouttext
//  DoEvents(); //W FUNC;                                                        // {VB2J [1032]}	   DoEvents

  }                                                                            // {VB2J [1034]}	End Sub
// annealtempScroll_Change



/**
	AutoAnneal - the whole reason we're here!
**/
public void 
autoAnneal() {                                // {VB2J [1048]}	Private Sub autoannealCommand_Click()

	float annealtempx = 200;				// EN2.20 - changed from 1000

	autoannealtempmax = annealtempx;
	autoannealtempmin = 0.00002f;			// EN2.20 - changed from 0.0001

 	SystemDesign.logInfo("");
	SystemDesign.logInfo("MetroParams.autoAnneal creating timer...");

	// Start a timer that lowers the system temp as we anneal.
	//
	// This 'chunks' thing is rather different from Marc's way, but it seems to be fine.
	//
	metroTimer_ = new MetroTimer(this, annealForm_, systemDesign_, 
                                 annealtempx, autoannealtempmin, 
                                 autoannealtime, 32);	
	metroThread_ = new Thread(metroTimer_);
	metroThread_.start();

	float scalelogmin = (float)Math.log(0.000000001);                                  // {VB2J [1070]}	   scalelogmin = Log(0.000000001)    '1.e-9
	float scalelogmax = (float)Math.log(1000000);                                      // {VB2J [1071]}	   scalelogmax = Log(1000000)       '1.e6


	autoannealtempmaxscrollvalue = annealtempScroll_min +
  			((float)Math.log(autoannealtempmax) - scalelogmin) * 
			(annealtempScroll_max - annealtempScroll_min) / 
			(scalelogmax - scalelogmin);
	autoannealtempminscrollvalue = annealtempScroll_min + 
  			((float)Math.log(autoannealtempmin) - scalelogmin) * 
			(annealtempScroll_max - annealtempScroll_min) / 
			(scalelogmax - scalelogmin);

	annealtempScroll_Value = autoannealtempmaxscrollvalue;                       // {VB2J [1079]}	   annealtempScroll.Value = autoannealtempmaxscrollvalue

	autoannealflag = true;                                                       // {VB2J [1082]}	   autoannealflag = true
	autoannealcounter = 0;                                                       // {VB2J [1084]}	   autoannealcounter = 0

// rec - update dependent vars
	annealtempScroll_Change(); //W FUNC;                                         // {VB2J [1086]}	   annealtempScroll_Change

//  DoEvents(); //W FUNC;                                                        // {VB2J [1087]}	   DoEvents


  }                                                                            // {VB2J [1114]}	End Sub
  // autoAnneal


/**
	VB's equivalent of constructor, sorta.
**/
private void 
formLoad() {                                              // {VB2J [1224]}	Private Sub Form_Load()

	metrparmload = true;                                                         // {VB2J [1225]}	   metrparmload = true

	setDefaults();                                             // {VB2J [1228]}	   setdefaultsCommand.Value = true

// rec - GUI
//  fixedcostvalueText.text = 0;                                                 // {VB2J [1230]}	   fixedcostvalueText.text = 0
//  monthlycostvalueText.text = 0;                                               // {VB2J [1231]}	   monthlycostvalueText.text = 0
//  pfailurevalueText.text = 0;                                                  // {VB2J [1232]}	   pfailurevalueText.text = 0
//  responsetimevalueText.text = 0;                                              // {VB2J [1233]}	   responsetimevalueText.text = 0

//  fixedcostscroll_Change(); //W FUNC;                                          // {VB2J [1235]}	   fixedcostscroll_Change
//  monthlycostscroll_Change(); //W FUNC;                                        // {VB2J [1236]}	   monthlycostscroll_Change
//  pfailureScroll_Change(); //W FUNC;                                           // {VB2J [1237]}	   pfailureScroll_Change
//  responsetimescroll_Change(); //W FUNC;                                       // {VB2J [1238]}	   responsetimescroll_Change

	autoannealcounter = 0;                                                       // {VB2J [1240]}	   autoannealcounter = 0

// rec - GUI
//  autoannealtimeText.text = 270;                                               // {VB2J [1242]}	   autoannealtimeText.text = 270


//  autoannealtime = 270;
    SystemDesign.logInfo("MetroParams.formLoad: Setting auto-anneal time to " + annealTime_ + " seconds.");
	autoannealtime = annealTime_;


// rec
//  DoEvents(); //W FUNC;                                                        // {VB2J [1244]}	   DoEvents

  }                                                                            // {VB2J [1246]}	End Sub
// formLoad


/**
	Set some dependent vars.
	This doesn't happen dyncamically, like it does in the interactive VB version,
	but we do call this once to set things up.
**/
private void 
pfailureScroll_Change() {                                  // {VB2J [1471]}	Private Sub pfailureScroll_Change()

  float objx;                                                                 // {VB2J [1472]}	   Dim objx As Single
  float pfailurevalue;                                                        // {VB2J [1474]}	   Dim pfailurevalue As Single

  // rec - decl*2
  float pfailurescalemax = 10000f / systemDesign_.pfailureave;            // {VB2J [1476]}	   pfailurescalemax = 10000 / Val(pfailureaveText.text)
  float pfailurescalemin = 0.0001f / systemDesign_.pfailureave;           // {VB2J [1477]}	   pfailurescalemin = 0.0001 / Val(pfailureaveText.text)

  // rec - decl*3
  float scalelogmin = (float)Math.log(pfailurescalemin);                             // {VB2J [1479]}	   scalelogmin = Log(pfailurescalemin)
  float scalelogmax = (float)Math.log(pfailurescalemax);                             // {VB2J [1480]}	   scalelogmax = Log(pfailurescalemax)
  float pfailurescalelog = scalelogmin + 
							(scalelogmax - scalelogmin) * 
							(pfailureScroll_Value - pfailureScroll_min) / 
							(pfailureScroll_max - pfailureScroll_min);  // {VB2J [1482]}	   pfailurescalelog = scalelogmin + (scalelogmax - scalelogmin) * (pfailureScroll.Value - pfailureScroll.min) / (pfailureScroll.max - pfailureScroll.min)

  if (pfailureCheck)  {                                             // {VB2J [1484]}	   If (pfailureCheck.Value = 1) Then

	  float pfailurescalex = (float)Math.exp(pfailurescalelog);                        // {VB2J [1485]}	      pfailurescalex = Exp(pfailurescalelog)
      systemDesign_.pfailurescale = pfailurescalex;                                            // {VB2J [1486]}	      pfailurescale = pfailurescalex
 
	  // rec - GUI
	  //F UNK                                                // {VB2J [1487]}	      scinumformat pfailurescale, pfailurescaleouttext
      // pfailurescaleText.text = pfailurescaleouttext;                             // {VB2J [1488]}	      pfailurescaleText.text = pfailurescaleouttext
      pfailurevalue = systemDesign_.pfailureave * systemDesign_.pfailurescale;                      // {VB2J [1489]}	      pfailurevalue = pfailureaveText.text * pfailurescale
      //F UNK                                                // {VB2J [1490]}	      scinumformat pfailurevalue, pfailurevalueouttext
    } else {                                                                   // {VB2J [1491]}	   Else
      systemDesign_.pfailurescale = 0;                                                         // {VB2J [1492]}	      pfailurescale = 0
	  
	  // rec - GUI
      // pfailurevalueouttext = "0";                                                // {VB2J [1493]}	      pfailurevalueouttext = "0"
      pfailurevalue = 0;                                                         // {VB2J [1494]}	      pfailurevalue = 0
      //F UNK                                                // {VB2J [1495]}	      scinumformat pfailurevalue, pfailurevalueouttext
    }                                                                          // {VB2J [1496]}	   End If

  // rec - GUI
  // failurevalue = pfailurevalueouttext;                               // {VB2J [1498]}	   pfailurevalueText.text = pfailurevalueouttext

                                                                               // {VB2J [1500]}	   'update objective function text box:
  objx  = fixedcostvalue;                             // {VB2J [1501]}	   objx = Val(fixedcostvalueText.text)
  objx += monthlycostvalue;                    // {VB2J [1502]}	   objx = objx + Val(monthlycostvalueText.text)
  objx += pfailurevalue;                       // {VB2J [1503]}	   objx = objx + Val(pfailurevalueText.text)
  objx += responsetimevalue;                   // {VB2J [1504]}	   objx = objx + Val(responsetimevalueText.text)

// 1.1
   objx += remotetrafficvalue; // 'messaging mods
   objx += pfailuresurvivevalue; // 'marc EN 1.1 smoothing
   objx += hammingvalue; // 'marc EN 1.1 hamming


  // rec - GUI
  //F UNK                                                  // {VB2J [1506]}	   scinumformat objx, objectivexouttext
  // objectiveText.text = objectivexouttext;                                      // {VB2J [1508]}	   objectiveText.text = objectivexouttext

   // {VB2J [1511]}	   'since scale has changed, we need to update the objective function for all saved states:
   // (rec - next comment was here, apropos of?...)
   // {VB2J [1512]}	   'For istate = 0 To state.numfstatecallsave
//
//  rec - replaced with call to this handy one-place-fits-all method
	updateStatelistScale(UPDATE_STATELIST_SCALE_PFAIL);
//
//  for (int istate=0; istate<=systemDesign_.numstatelist; istate++) {                         // {VB2J [1513]}	   For istate = 0 To numstatelist
//    systemDesign_.statelist[istate].pfailurescale = systemDesign_.pfailurescale;               // {VB2J [1514]}	      statelist(istate).pfailurescale = pfailurescale
//
//    objx = systemDesign_.statelist[istate].fixedcost * systemDesign_.statelist[istate].fixedcostscale;  // {VB2J [1516]}	      objx = statelist(istate).fixedcost * statelist(istate).fixedcostscale
//    objx += systemDesign_.statelist[istate].monthlycost * systemDesign_.statelist[istate].monthlycostscale;  // {VB2J [1517]}	      objx = objx + statelist(istate).monthlycost * statelist(istate).monthlycostscale
//    objx += (1 - systemDesign_.statelist[istate].psuccess) * systemDesign_.statelist[istate].pfailurescale;  // {VB2J [1518]}	      objx = objx + (1 - statelist(istate).psuccess) * statelist(istate).pfailurescale 'added 8/12/01 to try to fix the subspace bug with probability
//    objx += systemDesign_.statelist[istate].responsetime * systemDesign_.statelist[istate].responsetimescale;  // {VB2J [1519]}	      objx = objx + statelist(istate).responsetime * statelist(istate).responsetimescale
//
//// 1.1
//	objx += systemDesign_.statelist[istate].remotetraffic * systemDesign_.statelist[istate].remotetrafficscale; // 'messaging mods
//  	objx += systemDesign_.statelist[istate].pfailuresurvive * systemDesign_.statelist[istate].pfailuresurvivescale; // 'marc EN 1.1 smoothing
//	objx += systemDesign_.statelist[istate].hamming * systemDesign_.statelist[istate].hammingscale; // 'marc EN 1.1 hamming
// 
//    systemDesign_.statelist[istate].e = objx;                                    // {VB2J [1520]}	      statelist(istate).e = objx
//    }                                                                          // {VB2J [1521]}	   Next istate


//  DoEvents(); //W FUNC;                                                        // {VB2J [1524]}	DoEvents

  }                                                                            // {VB2J [1527]}	End Sub
// pfailureScroll_Change



/**
	Set default values.
**/
private void 
setDefaults() {                               // {VB2J [1608]}	Private Sub setdefaultsCommand_Click()

  systemDesign_.responsetimemin = 0.1f;                                                       // {VB2J [1610]}	   responsetimemin = 0.1
  systemDesign_.responsetimemax = 600f;                                                       // {VB2J [1611]}	   responsetimemax = 600 'ten minutes
  systemDesign_.responsetimeave = 1f;                                                         // {VB2J [1612]}	   responsetimeave = 1 ' 1 second

  systemDesign_.fixedcostmin = 100f;                                                          // {VB2J [1614]}	   fixedcostmin = 100
  systemDesign_.fixedcostmax = 80000000f;                                                     // {VB2J [1615]}	   fixedcostmax = 80000000 ' $80 million
  systemDesign_.fixedcostave = 15000f;                                                        // {VB2J [1616]}	   fixedcostave = 15000

  systemDesign_.monthlycostmin = 30f;                                                         // {VB2J [1618]}	   monthlycostmin = 30 'rent from tabnet
  systemDesign_.monthlycostmax = 100000f;                                                     // {VB2J [1619]}	   monthlycostmax = 100000 '$100k/month - 10 employees
  systemDesign_.monthlycostave = 100f;                                                        // {VB2J [1620]}	   monthlycostave = 100

  systemDesign_.pfailuremin = 0.001f;                                                         // {VB2J [1623]}	   pfailuremin = 0.001
  systemDesign_.pfailuremax = 0.9999f;                                                        // {VB2J [1624]}	   pfailuremax = 0.9999


// rec - GUI
//  responsetimeminText.text = responsetimemin;                                  // {VB2J [1627]}	   responsetimeminText.text = responsetimemin
//  responsetimemaxText.text = responsetimemax;                                  // {VB2J [1628]}	   responsetimemaxText.text = responsetimemax

//  fixedcostminText.text = fixedcostmin;                                        // {VB2J [1630]}	   fixedcostminText.text = fixedcostmin
//  fixedcostmaxText.text = fixedcostmax;                                        // {VB2J [1631]}	   fixedcostmaxText.text = fixedcostmax

//  monthlycostminText.text = monthlycostmin;                                    // {VB2J [1633]}	   monthlycostminText.text = monthlycostmin
//  monthlycostmaxText.text = monthlycostmax;                                    // {VB2J [1634]}	   monthlycostmaxText.text = monthlycostmax

//  pfailureminText.text = pfailuremin;                                          // {VB2J [1636]}	   pfailureminText.text = pfailuremin
//  pfailuremaxText.text = pfailuremax;                                          // {VB2J [1637]}	   pfailuremaxText.text = pfailuremax



//  fixedcostscroll.Value = fixedcostscroll.min;                                 // {VB2J [1641]}	   fixedcostscroll.Value = fixedcostscroll.min
//  monthlycostscroll.Value = monthlycostscroll.min;                             // {VB2J [1642]}	   monthlycostscroll.Value = monthlycostscroll.min
//  pfailureScroll.Value = pfailureScroll.min;                                   // {VB2J [1643]}	   pfailureScroll.Value = pfailureScroll.min

                                                                               // {VB2J [1645]}	   'set the responsetimescale such that the value is approximately equal to one:

																			   
  // rec - decl*3
  float scalelogmin = (float)Math.log(0.00001f);                                      // {VB2J [1646]}	   scalelogmin = Log(0.00001)
  float scalelogmax = (float)Math.log(100000f);                                       // {VB2J [1647]}	   scalelogmax = Log(100000)
  float rx = -scalelogmin * (responsetimescroll_max - responsetimescroll_min) / (scalelogmax - scalelogmin);  // {VB2J [1649]}	   rx = -scalelogmin * (responsetimescroll.max - responsetimescroll.min) / (scalelogmax - scalelogmin)
  responsetimescroll_Value = rx;                                               // {VB2J [1650]}	   responsetimescroll.Value = rx

                                                                               // {VB2J [1652]}	   'set pfailurescale so value is approximately equal to one:
  scalelogmin = (float)Math.log(0.00001f);                                      // {VB2J [1653]}	   scalelogmin = Log(0.00001)
  scalelogmax = (float)Math.log(100000f);                                       // {VB2J [1654]}	   scalelogmax = Log(100000)

  rx = -scalelogmin * (pfailureScroll_max - pfailureScroll_min) / (scalelogmax - scalelogmin);  // {VB2J [1656]}	   rx = -scalelogmin * (pfailureScroll.max - pfailureScroll.min) / (scalelogmax - scalelogmin)

  // rec - GUI sim
  pfailureScroll_Value = rx;                                                   // {VB2J [1657]}	   pfailureScroll.Value = rx
  pfailureScroll_Value = pfailureScroll_min + (pfailureScroll_max - pfailureScroll_min) / 2;  // {VB2J [1659]}	   pfailureScroll.Value = pfailureScroll.min + (pfailureScroll.max - pfailureScroll.min) / 2



   // {VB2J [1664]}	'   'set fixedcostscale so value is approximately equal to one:
   // {VB2J [1665]}	'   scalelogmin = Log(fixedcostscalemin)
   // {VB2J [1666]}	'   scalelogmax = Log(fixedcostscalemax)
   // {VB2J [1667]}	'
   // {VB2J [1668]}	'   rx = -scalelogmin * (fixedcostscroll.max - fixedcostscroll.min) / (scalelogmax - scalelogmin)
   // {VB2J [1669]}	'   fixedcostscroll.Value = rx
   // {VB2J [1670]}	'
   // {VB2J [1671]}	'   'set monthlycostscale so value is approximately equal to one:
   // {VB2J [1672]}	'   scalelogmin = Log(monthlycostscalemin)
   // {VB2J [1673]}	'   scalelogmax = Log(monthlycostscalemax)
   // {VB2J [1674]}	'
   // {VB2J [1675]}	'   rx = -scalelogmin * (monthlycostscroll.max - monthlycostscroll.min) / (scalelogmax - scalelogmin)
   // {VB2J [1676]}	'   monthlycostscroll.Value = rx



// rec - no cost, responsetime calcs

//  fixedcostscroll_Value = fixedcostscroll.min + (fixedcostscroll.max - fixedcostscroll.min) / 2;  // {VB2J [1678]}	   fixedcostscroll.Value = fixedcostscroll.min + (fixedcostscroll.max - fixedcostscroll.min) / 2
//  monthlycostscroll_Value = monthlycostscroll.min + (monthlycostscroll.max - monthlycostscroll.min) / 2;  // {VB2J [1679]}	   monthlycostscroll.Value = monthlycostscroll.min + (monthlycostscroll.max - monthlycostscroll.min) / 2
                                                                               // {VB2J [1682]}	   'responsetimescroll.Value = responsetimescroll.max
//  fixedcostscroll_Change(); //W FUNC;                                          // {VB2J [1684]}	   fixedcostscroll_Change
//  monthlycostscroll_Change(); //W FUNC;                                        // {VB2J [1685]}	   monthlycostscroll_Change

  pfailureScroll_Change(); //W FUNC;                                           // {VB2J [1686]}	   pfailureScroll_Change

  updateDepenedentVars();
  
//  responsetimescroll_Change(); //W FUNC;                                       // {VB2J [1687]}	   responsetimescroll_Change

  annealtempScroll_Value = annealtempScroll_max;                               // {VB2J [1689]}	   annealtempScroll.Value = annealtempScroll.max
  annealtempScroll_Change(); //W FUNC;                                         // {VB2J [1690]}	   annealtempScroll_Change

//  DoEvents(); //W FUNC;                                                        // {VB2J [1692]}	   DoEvents



  }                                                                            // {VB2J [1696]}	End Sub
// setDefaults



} // MetroParams
