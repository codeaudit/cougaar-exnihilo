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
	MetroTimer - timer for the "Metropolis" simulated annealing algorithm. <P>
	
	This will tweak the system temp (SystemDesign.annealtemp) from start to end, 
	over the specified number of seconds, in the given number of chunks, *logarithmically*.
	Such as, if seconds=60 and chunks=5, there will be 5 12-second chunks. <P>

	@author robert.e.cranfill@boeing.com
**/
public class MetroTimer implements Runnable {

private MetroParams 	metroParams_;	// 1.1
private AnnealForm      annealForm_;
private SystemDesign    systemDesign_;

private float           startLog_;
private float           stopLog_;
private int             seconds_;
private int             chunks_;

private boolean die_;


/**
	Create a new timer.
**/
public
MetroTimer(MetroParams metroParams, AnnealForm annForm, SystemDesign sdf, float annealingStartTemp, 
            float annealingStopTemp, int seconds, int chunks) {

	metroParams_	= metroParams;
    annealForm_     = annForm;
    systemDesign_   = sdf;

	die_ = false;

	startLog_	= (float)Math.log(annealingStartTemp);
	stopLog_ 	= (float)Math.log(annealingStopTemp);
	seconds_ 	= seconds;
	chunks_ 	= chunks;

	if (systemDesign_.isInfoEnabled()) {
		SystemDesign.logInfo("MetroTimer:");
		SystemDesign.logInfo("   Tstart: " + annealingStartTemp + " (e^" + startLog_ + ")");
		SystemDesign.logInfo("    Tstop: " + annealingStopTemp + " (e^" + stopLog_ + ")");
		SystemDesign.logInfo("  seconds: " + seconds_);
		SystemDesign.logInfo("   chunks: " + chunks_);
		SystemDesign.logInfo("");
		}

	} // constructor


/**
	Required by 'Runnable' interface. <BR>
	This needs to be re-done with Cougaar Threads??? <BR>
**/
public void 
run() {

	int sleepTime = 1000 * seconds_ / chunks_;	// milliseconds
	float tempLog = startLog_;
	float tempLogIncrement = (stopLog_ - startLog_) / (chunks_-1);

	// This is just FYI, not used in solving.
	long timerStarted = System.currentTimeMillis();

	for (int i=0; i<chunks_; i++)  {

		// Set the new system annealing temperature.
		//
		systemDesign_.annealtemp = (float)Math.exp(tempLog);

		// update objective functions??? 1.1
		metroParams_.updateDepenedentVars();

		// also just FYI
        int percent = Math.round((1.0f-(float)i/(float)chunks_)*100.0f);
		if (systemDesign_.isInfoEnabled())
			SystemDesign.logInfo("MetroTimer.run: set annealing temp= " + systemDesign_.annealtemp + " (e^" + tempLog + "; " + percent + "%)");

		try {
			Thread.sleep(sleepTime);
			}
		catch (InterruptedException tie) {
			if (systemDesign_.isErrorEnabled())
				SystemDesign.logError("MetroTimer.run: InterruptedException: " + tie.getMessage(), tie);
			}
		catch (Exception e) {
			if (systemDesign_.isErrorEnabled())
				SystemDesign.logError("MetroTimer.run: Exception: " + e.getMessage(), e);
			}

		if (die_) {
			if (systemDesign_.isInfoEnabled()) {
				SystemDesign.logInfo("MetroTimer: has been told to stop; annealing temp is " + systemDesign_.annealtemp);
				SystemDesign.logInfo("MetroTimer: ran for " + (System.currentTimeMillis() - timerStarted) + " ms.");
				}
			break;
			}

		tempLog += tempLogIncrement;
		}

	if (!die_) {
		if (systemDesign_.isInfoEnabled()) {
			SystemDesign.logInfo("MetroTimer: target temperature " + systemDesign_.annealtemp + " reached!");
			SystemDesign.logInfo("MetroTimer: ran for " + (System.currentTimeMillis() - timerStarted) + " ms.");
			SystemDesign.logInfo("");
			}

		// Tell SystemDesign and AnnealForm that they are done!
		//
		systemDesign_.iteranel = false;			// autoannealflag = False
		annealForm_.iterate = false;			// need public setter for this ???

	//	autoannealCheck.Value = 0
	//	annealForm.iterateCheck.Value = 0
	//	annealForm.writeagentsCommand.Value = True

		}
	
	} // run


/**
	Public method to stop us nicely.
**/
public void
stopPlease() {
	die_ = true;
	}


} // class MetroTimer
