/* 
 * <copyright>
 * Copyright 2004 The Boeing Company
 *   under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
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


import org.cougaar.core.component.Service;
import org.cougaar.util.log.Logger;


/**
 * A replacment for the Cougaar logger that just outputs to System.out.
 * So my regression code can run in a non-Cougaar environment.
 * Was this class really necessary? Perhaps not... tell me how! :)
 *
 *	This differs from the usual logger behavior in that 
 *  if the log level isn't appropriate, the print routines won't print.
 * 
 * 
 * @author robert.e.cranfill@boeing.com
 */
public class StdOutLogger implements Service, Logger {

private int currentLoggerLevel_;


public StdOutLogger() {
	currentLoggerLevel_ = Logger.INFO;
	}


public StdOutLogger(int initialLevel) {

	if (initialLevel < Logger.DETAIL || initialLevel > Logger.FATAL) {
		System.out.println("StdOutLogger new value out of bounds! Using 'Logger.INFO'");
		initialLevel = Logger.INFO;
		}
	currentLoggerLevel_ = initialLevel;
	}


public void setLogLevel(int newLevel) {

	if (newLevel < Logger.DETAIL || newLevel > Logger.FATAL) {
		System.out.println("StdOutLogger new value out of bounds! Using 'Logger.INFO'");
		newLevel = Logger.INFO;
		}
	currentLoggerLevel_ = newLevel;
	}


/**
 * What's this supposed to do??? (I'd guess print a dot, but what's "s"?
 * 
 */
public void printDot(String s) {
	}


public boolean isEnabledFor(int checkLevel) {

	if (currentLoggerLevel_ <= checkLevel)
		return true;
	else
		return false;
	}


/** 
 * FATAL
 */
public void fatal(String s) {
	fatal(s, null);
	}

public void fatal(String s, Throwable t) {

	if (this.isEnabledFor(Logger.FATAL) == false)
		return;
	System.out.println(" FATAL: " + s);
	if (t != null)
		t.printStackTrace();
	}

public boolean isFatalEnabled() {
	return currentLoggerLevel_ <= Logger.FATAL;
	}


/** 
 * DETAIL
 */
public void detail(String s) {
	detail(s, null);
	}

public void detail(String s, Throwable t) {

	if (this.isEnabledFor(Logger.DETAIL) == false)
		return;
	System.out.println("DETAIL: " + s);
	if (t != null)
		t.printStackTrace();
	}

public boolean isDetailEnabled() {
	return currentLoggerLevel_ <= Logger.DETAIL;
	}
	
	
/** 
 * ERROR
 */
public void error(String s) {
	error(s, null);
	}

public void error(String s, Throwable t) {

	if (this.isEnabledFor(Logger.ERROR) == false)
		return;
	System.out.println("ERROR: " + s);
	if (t != null)
		t.printStackTrace();
	}

public boolean isErrorEnabled() {
	return currentLoggerLevel_ <= Logger.ERROR;
	}


/** 
 * WARN
 */
public void warn(String s) {
	warn(s, null);
	}

public void warn(String s, Throwable t) {

	if (this.isEnabledFor(Logger.WARN) == false)
		return;
	System.out.println(" WARN: " + s);
	if (t != null)
		t.printStackTrace();
	}

public boolean isWarnEnabled() {
	return currentLoggerLevel_ <= Logger.WARN;
	}


/** 
 * INFO
 */
public void info(String s) {
	info(s, null);
	}

public void info(String s, Throwable t) {

	if (this.isEnabledFor(Logger.INFO) == false)
		return;
	System.out.println(" INFO: " + s);
	if (t != null)
		t.printStackTrace();
	}

public boolean isInfoEnabled() {
	return currentLoggerLevel_ <= Logger.INFO;
	}


/** 
 * DEBUG
 */
public void debug(String s) {
	debug(s, null);
	}
	
public void debug(String s, Throwable t) {
	
	if (this.isEnabledFor(Logger.DEBUG) == false)
		return;
	System.out.println("DEBUG: " + s);
	if (t != null)
		t.printStackTrace();
	}
	
public boolean isDebugEnabled() {
	return currentLoggerLevel_ <= Logger.DEBUG;
	}


/** 
 * SHOUT
 */
public void shout(String s) {
	shout(s, null);
	}
	
public void shout(String s, Throwable t) {
	
	if (this.isEnabledFor(Logger.SHOUT) == false)
		return;
	System.out.println("SHOUT: " + s);
	if (t != null)
		t.printStackTrace();
	}
	
public boolean isShoutEnabled() {
	return currentLoggerLevel_ <= Logger.SHOUT;
	}


/** 
 * LOG - I guess this level's always enabled?
 */
public void log(int i, String s) {
	log(i, s, null);
	}
public void log(int i, String s, Throwable t) {
	System.out.println("  LOG: " + s);
	if (t != null)
		t.printStackTrace();
	}


}	// StdOutLogger
