// vb2jUtils.java
// (c)2003 The Boeing Company; All rights reserved.
//
package com.boeing.pw.mct.vb2j;


/**
	VisualBasic-to-Java
	(c)2003 The Boeing Company; All rights reserved.
	@author robert.e.cranfill@boeing.com

	Utils to implement various VB functions.
	Util class can't be instantiated.
**/
public abstract class VB2JUtils {


/**
	Emulate VB's "Int" function.
**/
public static int
Int(double f) {
	return (int)Math.floor(f);
	}


/**
	Return a random number 0 <= n < 1.0
	Math.random cast to a float CAN return 1.0, so filter it.
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


} // VB2JUtils
