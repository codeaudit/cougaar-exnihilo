// vb2j.java
// (c)2003 The Boeing Company; All rights reserved.
//
package com.boeing.pw.mct.vb2j;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
	VisualBasic-to-Java
	(c)2003 The Boeing Company; All rights reserved.

	@version 18jun03 - fixed some "integration violations"
	@author robert.e.cranfill@boeing.com

**/
public class VB2J {

public static final boolean FORCE_COMPILE_ERROR_ON_TRANSLATION_FAILURE = true;

public static final String	VB2J_COMMENT_STRING = "//{VB2J} ";
public static final String	TRANS_FAILURE_COMMENT_STRING_SILENT = "//{VB2J-FAIL} ";
public static final String	TRANS_FAILURE_COMMENT_STRING_ERROR  = "VBJ2_XLATE_FAILURE;";
//public static final String	TRANS_FAILURE_COMMENT_STRING_ERROR  = "!!{VB2J-FAIL} ";		<-- THIS CAUSES 3 ERRORS FOR ONE. NO GOOD.


// Blanks that we'll use to pad source line comments out to standard column. Length of this is the col the comments go to.
private static final String SOURCE_COMMENT_PAD_80 = "                                                                               "; // 79 blanks
// private static final String SOURCE_COMMENT_PAD_60 = "                                                           "; // 59 blanks

private static Logger logger_ = Logger.getLogger("com.boeing.pw.mct.vb2j");

private String 	package_;
private String 	class_;

private int		indentLevel_;
private String	inputFilename_;
private int		linesProcessed_;
private int		fatals_;
private int		warnings_;
private int		gotos_;	// count of non-supported 'goto's in source.
private int		currentLineNumber_;

// this is getting complicated....
private ArrayList	possibleFnNames_ = new ArrayList();


/**
	Constructor - need input filename, output package and class names.
**/
public VB2J(String inFile, String packageName, String className) {
	
	inputFilename_ = inFile;
	package_ = packageName;
	class_ = className;

	indentLevel_ = 0;
	linesProcessed_ = 0;
	fatals_ = 0;
	warnings_ = 0;
	gotos_= 0;
}


/**
	Given the VB code in inputstream "is", convert to Java.
**/
public void
convert(LineNumberReader lnr, PrintStream os) {

	os.println(VB2J_COMMENT_STRING);
	os.println(VB2J_COMMENT_STRING + "Input file: " + inputFilename_ + ".frm");
	os.println(VB2J_COMMENT_STRING + (new java.util.Date()));
	os.println(VB2J_COMMENT_STRING);
	os.println();

	// package
	if ("".equals(package_)==false) {
		os.println("package " + package_ + ";");
	os.println();
	}

	// class
	os.println("public class " + class_ + " extends com.boeing.pw.mct.vb2j.VBConvertedCode {");
	os.println();

	// Skip the form-related stuff, only convert code.
	//
	skipFrmHeader(lnr);

	String sourceLine = null;
	String processLine = "";
	try {
		while ((sourceLine = lnr.readLine()) != null) {

			// Skip "Attribute ", which is really part of the form header
			//
			if (sourceLine.startsWith("Attribute ")) {
				continue;
			}

			if (linesProcessed_ == 0)
				logger_.info("First non-header line is: " + sourceLine);

			currentLineNumber_ = lnr.getLineNumber();
			linesProcessed_++;
			if (sourceLine.trim().equals("")) {	// don't annotate blank lines, just pass 'em thru.
				os.println();
			}
			else {
				if (sourceLine.endsWith("_")) {
					processLine += sourceLine.substring(0, sourceLine.length()-1);
				}
				else {

					processLine += sourceLine;
					String indentedLine = currentIndent() + translateLine(processLine.trim());	// process a trimmed input line
	//				os.println(indentedLine + "\t// {VB2J [" + currentLineNumber_ + "]}\t" + sourceLine);
					os.println(indentedLine + padTo(indentedLine) + "// {VB2J [" + currentLineNumber_ + "]}\t" + processLine);
					processLine = "";
				}
			}
		}
	}
	catch (Exception e) {
		e.printStackTrace();
	}

	// Tidy up

	// Close the class
	os.println("} // class " + class_);
	os.println("/*");
	os.println("  End translated code for class " + class_);
	os.println("     Lines processed: " + linesProcessed_);
	os.println("              Fatals: " + fatals_);
	os.println("            Warnings: " + warnings_);
	os.println("               GoTos: " + gotos_);
	
	os.println();
	os.println("  Possible functions: ");
	os.println(arrayListToString(possibleFnNames_));
	os.println();

	os.println("*/");

	os.flush();

	logger_.info("-----------------------");
	logger_.info("Lines processed: " + linesProcessed_);
	logger_.info("         Fatals: " + fatals_);
	logger_.info("       Warnings: " + warnings_);
	logger_.info("          GoTos: " + gotos_);
	logger_.info("-----------------------");


}	// convert


/**
	Given the indicated string, return a string of blanks to pad it to the length of SOURCE_COMMENT_PAD.
**/
private String
padTo(String leftSide) {
	
	try {
		return SOURCE_COMMENT_PAD_80.substring(leftSide.length());
	}
	catch (StringIndexOutOfBoundsException sioobe) {
		return " ";
	}
}


/**
	 Skip the form-related stuff, only convert code.
	 Just track "Begin"s and "End"s until we aren't nested any more.
	 - For "begin", match any line that starts with "<whitespace>Begin<space>"
	   For "end", match only lines that are just "<whitespace>Begin"
**/
private void
skipFrmHeader(LineNumberReader lnr) {

	int indent = 0;
	boolean gotBegin = false;
	String inLine;
	try {
		while ((inLine = lnr.readLine()) != null) {

			if (lnr.getLineNumber() == 1) {
				if (inLine.equalsIgnoreCase("VERSION 5.00") == false) {
					logger_.severe("Warning: Wrong VB source version? found: " + inLine);
					warnings_++;
				}
			}
			else
			if (inLine.trim().startsWith("Begin ")) {
				gotBegin = true;
				indent++;
				// System.out.println(">" + indent + " @ " + lnr.getLineNumber());
			}
			else 
			if (inLine.trim().equalsIgnoreCase("End")) {
				indent--;
				// System.out.println("<" + indent + " @ " + lnr.getLineNumber());
			}

			// If we're back to indent zero, we're done (almost)
			//
			if (gotBegin && indent == 0) {
				logger_.info("Skipped " + lnr.getLineNumber() + " lines of header.");
				return;
			}
		}
	}
	catch (IOException ioe) {
		ioe.printStackTrace();
	}
}


/**
	Return a string that's the proper amount of indent.
	Uses indentLevel_
**/
private String
currentIndent() {

	StringBuffer sb = new StringBuffer();
	for (int i=0; i<indentLevel_; i++) {
		sb.append("  ");	// or use tabs?
	}
	return sb.toString();
}


/**
	Translate one line of VB code.
**/
private String
translateLine(String line) {

	ArrayList tokens = tokenizeToArrayList(line, " \t");
	
	// Blank line?
	//
	if (tokens.size() == 0) {
		return "";
	}

	String tokenOne = (String)tokens.get(0);

	// Dim?
	//
	if (tokenOne.equalsIgnoreCase("Dim")) {
		return doDim(tokens);
	}

	// Const?
	//
	if (tokenOne.equalsIgnoreCase("Const")) {
		return doConst(tokens);
	}

	// If?
	if (tokenOne.equalsIgnoreCase("If")) {
		return doIf("if ", tokens);
	}

	// ElseIf?
	if (tokenOne.equalsIgnoreCase("ElseIf")) {
		indentLevel_--;	// doIf will increment this, incorrectly.
		return doIf("} else if ", tokens);
	}

	// End X?
	if (tokenOne.equalsIgnoreCase("End")) {
		return doEnd( /* tokens */ );
	}

	// Private Sub?
	if (tokenOne.equalsIgnoreCase("Private")) {
		return doPrivateSub(tokens);
	}

	// Sub?
	if (tokenOne.equalsIgnoreCase("Sub")) {
		return doSub(tokens);
	}

	// For?
	if (tokenOne.equalsIgnoreCase("For")) {
		return doFor(tokens);
	}

	// Next?
	if (tokenOne.equalsIgnoreCase("Next")) {
		return doNext( /* tokens */ );
	}

	// Exit?
	if (tokenOne.equalsIgnoreCase("Exit")) {
		return doExit(tokens);
	}

	// Else?
	if (tokenOne.equalsIgnoreCase("Else")) {
		return doElse(tokens);
	}

	// Do?
	if (tokenOne.equalsIgnoreCase("Do")) {
		return doDo(tokens);
	}

	// Loop?
	if (tokenOne.equalsIgnoreCase("Loop")) {
		return doLoop(tokens);
	}

	// GoTo?
	if (tokenOne.equalsIgnoreCase("GoTo")) {
		return doGoTo( /* tokens */ );
	}

	// A label?
	if (tokenOne.endsWith(":")) {
		return doLabel(tokens);
	}

	// Assignment?
	//
	String tryAssignment = tryAssignment(tokens);
	if (tryAssignment != null) {
		return tryAssignment;
	}

	if (tokens.size() == 1) {
		return tokenOne + "(); //W FUNC;";	// Warning: we're assuming this is a function call.
	}
	
	// If we didn't know what to do with it, 
	// return a string that the compiler will choke on.
	//
	fatals_++;
	return failureString() + " //F UNK";	// Unknown source statment
}



/**
	See if this is an assignment statement.
	Not sure how to do this 100%.... 
	for now, just check for a "=" token somewhere in the middle.... :/

**/
private String
tryAssignment(ArrayList tokens) {

	boolean hasEqual = false;
	
	for (int i=1; i<tokens.size()-1; i++) {
		if (((String)tokens.get(i)).equals("=")) {
			hasEqual = true;
			break;
		}
	}
	
	if (!hasEqual)
		return null;


	// Anything we're not sure of, we'll report.
	//
	String flags = "";

	/*
		Left side could be
			x
			x(y)		// could be array or function call, like "array(func(3))", but I don't see that in the code...

		Right side could be
			x + y
			x(y)		// could be array or function call
	*/

	// Re-build the line as single string, for ease of manipulation (such as it is)
	//
	StringBuffer line_sb = new StringBuffer();
	for (int i=0; i<tokens.size(); i++) {
		if (i>0)
			line_sb.append(" ");
		line_sb.append((String)tokens.get(i));
	}
	String result = line_sb.toString();

	// As a first stab, just change all "(" to "[" and ")" to "]".
	//
	if (result.indexOf('(') > -1) {
		flags += " ARRAY/FN";
		result = result.replace('(', '[').replace(')', ']');
		warnings_++;
	}

	return result + ";" + (flags.equals("") ? "" : (" //W" +flags));
}


/**
	GoTo
	NOT SUPPORTED! Must be re-coded by hand, as Java doesn't have a goto! 
	
	Emit an error that will cause just one Java compilation error (or none, depending on failureString())
**/
private String
doGoTo(/* unused: ArrayList tokens */) {

	gotos_++;
	return failureString() + " //F GOTO";
}


/**
	Do
	Only supports "Do" and "Do While True" - that's all the test cases have!
	(no "Do While (x < y)")

**/
private String
doDo(ArrayList tokens) {

	if (tokens.size() != 1 & tokens.size() != 3) {
		logger_.warning("doDo: Wrong number of tokens for 'Do' on line number " + currentLineNumber_);
		fatals_++;
		return failureString() + " // Wrong number of tokens for 'Do'.";
	}

	indentLevel_++;

	return "while (true) {";
}


/**
	A label
		Like
			"foo:"

**/
private String
doLabel(ArrayList tokens) {

	if (tokens.size() != 1) {
		logger_.warning("doLabel: Wrong number of tokens for label on line number " + currentLineNumber_);
		fatals_++;
		return failureString() + " // Wrong number of tokens for label.";
	}

	return (String)tokens.get(0);	// is the same in Java!
}


/**
	Loop
	Just terminate the block.
**/
private String
doLoop(ArrayList tokens) {

	if (tokens.size() != 1) {
		logger_.warning("doLoop: Wrong number of tokens for 'Loop' on line number " + currentLineNumber_);
		fatals_++;
		return failureString() + " // Wrong number of tokens for 'Loop'.";
	}

	indentLevel_--;

	return "}";
}


/**
	Exit
	Exit Sub, Exit For, Exit Do
	
		- Exit Sub
			assume "void" method (a correct assumption so far; we don't support anything else)
			
		- Exit For
		
		- Exit Do
		
**/
private String
doExit(ArrayList tokens) {

	if (tokens.size() != 2) {
		logger_.warning("doExit: Wrong number of tokens for 'Exit' on line number " + currentLineNumber_);
		fatals_++;
		return failureString() + " // Wrong number of tokens for 'Exit'.";
	}

	String type = (String)tokens.get(1);
	if (type.equalsIgnoreCase("Sub")) {
		return "return;";
	}
	else
	if (type.equalsIgnoreCase("For")) {
		return "break;";
	}
	else
	if (type.equalsIgnoreCase("Do")) {
		return "break;";
	}

	logger_.warning("doExit: Unknown 'Exit' variant' on line number " + currentLineNumber_);
	fatals_++;
	return failureString() + " // Unknown 'Exit' variant.";
}


/**
	Else
	
**/
private String
doElse(ArrayList tokens) {

	if (tokens.size() != 1) {
		logger_.warning("doElse: Wrong number of tokens for 'Else' on line number " + currentLineNumber_);
		fatals_++;
		return failureString() + " // Wrong number of tokens for 'Else'.";
	}

	return "} else { ";
}


/**
	For
	   For ifn = x - 1 To j - 2
	Assumptions:
		- loop var is local, and an int (safe assumption, eh?)

	Handle increments other than +1???
**/
private String
doFor(ArrayList tokens) {

	if (tokens.size() < 6) {
		logger_.warning("doFor: Too few tokens for 'For' on line number " + currentLineNumber_);
		fatals_++;
		return failureString() + " // Too few tokens for 'For'.";
	}

	indentLevel_++;

	String varName = (String)tokens.get(1);
	String start = "";
	String end = "";
	boolean gotStart = false;
	for (int i=3; i<tokens.size(); i++) {
		String token = (String)tokens.get(i);
		if (token.equalsIgnoreCase("To")) {
			gotStart = true;
		}
		else
		if (gotStart == false) {
			start += token;
		}
		else {
			end += token;
		}
	}

	return "for (int " + varName + "=" + start + "; " + varName + "<=" + end + "; " + varName + "++) {";
}


/**
	Next
	   Next ifn 
	Assumptions:
		- loops are always nested correctly, explicitly terminated
**/
private String
doNext( /* ArrayList tokens */) {

	indentLevel_--;
	return "} ";
}


/**
	"Private Sub" WITH NO PARAMS is only variation supported so far.
**/
private String
doPrivateSub(ArrayList tokens) {

	String tokenTwo = (String)tokens.get(1);
	if (tokenTwo.equalsIgnoreCase("Sub") == false) {
		logger_.warning("doPrivateSub: Keyword after 'Private' isn't 'Sub' on line number " + currentLineNumber_);
		fatals_++;
		return failureString() + " // Keyword after 'Private' isn't 'Sub'.";
	}

	if (tokens.size() < 3) {
		logger_.warning("doPrivateSub: Wrong number of tokens for 'Private Sub' on line number " + currentLineNumber_);
		fatals_++;
		return failureString() + " // Wrong number of tokens for 'Private Sub'.";
	}
	tokens.remove(0);
	return doSub(tokens);
}


/**
	"Sub" WITH NO PARAMS is only variation supported so far
	This also processes "Private Sub"'s params.
	Input: "Sub" Token, followed by whatever else was there.
**/
private String
doSub(ArrayList tokens) {

	if (tokens.size() < 2) {
		logger_.warning("doPrivateSub: Wrong number of tokens for 'Sub' on line number " + currentLineNumber_);
		fatals_++;
		return failureString() + " // Wrong number of tokens for 'Sub'.";
	}
	indentLevel_++;
	
	possibleFnNames_.add(tokens.get(1));

	return "private static void " + tokens.get(1) + " {";
}


/**
	End
		End If
		End Sub

	So far, all these can be taken care of with just a "}"

**/
private String 
doEnd( /* ArrayList tokens */ ) {
	
	indentLevel_--;
	return "}";
}


/**
	If or ElseIf
	Copy all tokens except the last, presumably "Then" - should we check???
	Translate operators from VB to Java.
**/
private String 
doIf(String startString, ArrayList tokens) {

	String condExp = "";

	for (int i=1; i<tokens.size()-1; i++) {

		String next = (String)tokens.get(i);
		if (next.equals("="))
			condExp += "== ";
		else
		if (next.equalsIgnoreCase("And"))	// use non-short circuit operators?
			condExp += "& ";
		else
		if (next.equalsIgnoreCase("Or"))
			condExp += "| ";
		else
		if (next.equalsIgnoreCase("<>"))
			condExp += "!= ";
		else
		if (next.equalsIgnoreCase("Not"))
			condExp += " !";
		else
		if (next.equalsIgnoreCase("(Not"))	// special case needs to be pretty!
			condExp += "( ! ";
		else
			condExp += next + " ";
	}

	// only add "(" and ")" if needed.
	//
	if (condExp.substring(0,1).equals("(") == false) {
		condExp = "(" + condExp + ")";
	}

	indentLevel_++;

	return startString + condExp + " {";
}


/**
	"Const"
		"Const tempMax = 1000" -> "static final int tempMax = 1000;" ???
**/
private String
doConst(ArrayList tokens) {
	
	return "static final int " + tokens.get(1) + 
			" " + tokens.get(2) + " " + tokens.get(3) + ";";
}


/**
	"Dim"
	Handles simple types:
		"Dim foo As Integer" 	-> "int foo;"
		"Dim foo As Single" 	-> "int foo;"	???
		"Dim foo As Boolean" 	-> "boolean foo;"	- better to use Boolean?
		"Dim foo As String" 	-> "String foo;"	- does NOT handle String(40), for example

	Handles arrays up to two dimensions:
	(notice that we add one to each array dim, so we can use 1-based arrays)
		Dim foo(10) As Integer		-> "int foo[] = new int[11];"
		Dim foo(10, 2) As Integer	-> "int foo[][] = new int[11, 3];"

	Handles multiple Dim statments, ie, 
		"Dim foo As Integer, fum as Integer"  -> "int foo; int fum;", all on one line
	but only if each component is NOT an array declaration

**/
private String
doDim(ArrayList tokens) {

	// Parse into "dim phrases"
	// - break into sets of "V As T" terms
	//
	ArrayList dpa = new ArrayList();
	String result = "";
	for (int i=1; i<tokens.size(); i++) {

		String token = (String)(tokens.get(i));
		dpa.add(token);
		if ("As".equalsIgnoreCase(token)) {
			i++;
			token = (String)(tokens.get(i));	// "T"
			if (token.endsWith(",")) {			// multi-Dim line
				token = token.substring(0, token.length()-1);
			}
			dpa.add(token);
			result += processDimPhrase(dpa);
			dpa = new ArrayList();
		}
	}
	return result;
}


/**
	Process one "dim phrase", which is one of:
		Var "As" Type
		Var(x) "As" Type
		Var(x, y) "As" Type		- note that this will get tokenized funny

	Note that output array sizes are increased by 1 so we can use VB's 1-based indexing.

	Input: set of tokens for ONE Dim phrase, NOT including the "Dim"
**/
private String
processDimPhrase(ArrayList tokens) {

	// Data type - last token
	//
	String vbType = (String)tokens.get(tokens.size()-1);
	String javaType = null;
	if (vbType.equalsIgnoreCase("Integer")) {
		javaType = "int";
	}
	else
	if (vbType.equalsIgnoreCase("Single")) {
		javaType = "double";
	}
	else
	if (vbType.equalsIgnoreCase("Boolean")) {
		javaType = "boolean";
	}
	else
	if (vbType.equalsIgnoreCase("String")) {
		javaType = "String";
	}
	else
	if (vbType.equalsIgnoreCase("String")) {
		javaType = "String";
	}

	if (javaType == null) {
		fatals_++;
		return failureString();
	}

	// Variable name; fix array index and max size, if any.
	//
	String varName = (String)tokens.get(0);
	if (tokens.size() > 3) {	// such as ["Var(x,", "y)", "As", "Type"]
		varName += (String)tokens.get(1);
	}

	int leftP = varName.indexOf("(");
	if (leftP > -1) {
		String rest = varName.substring(leftP+1);
		int rightP = rest.indexOf(")") + leftP + 2;
		if (rightP == -1) {
			logger_.severe("Un-matched parens in Dim statement?! on line number " + currentLineNumber_);
			fatals_++;
			return "";
		}

		// could be "n", or "n, m"
		String arraySizeStr = varName.substring(leftP+1, rightP-1);
		varName = varName.substring(0, leftP);	// "Var"
		int comma = arraySizeStr.indexOf(",");
		if (comma == -1) {	// single dimension
			varName += "[] = new " + javaType + "[";
			try {
				int arraySize = Integer.parseInt(arraySizeStr);
				varName += (arraySize+1) + "]";
			}
			catch (NumberFormatException nfe) { // symbolic dimension?
				 varName += arraySizeStr + "+1]";
			}
		}
		else { // >1 dimension - only handle 2, for now!
			varName += "[][] = new " + javaType + "[";
			try {
				int arraySize1 = Integer.parseInt(arraySizeStr.substring(0, comma));
				int arraySize2 = Integer.parseInt(arraySizeStr.substring(comma+1));
				varName += (arraySize1+1) + "][" + (arraySize2+1) + "]";
			}
			catch (NumberFormatException nfe) { // symbolic dimension?
				String as1 = arraySizeStr.substring(0, comma);
				String as2 = arraySizeStr.substring(comma+1);
				varName += as1 + "+1][" + as2 + "+1]";
			}
		}
	}
	
	return javaType + " " + varName + ";";
}


public static void
main(String args[]) {

	setUpLogger();

	logger_.info("VB2J 1.0a; robert.e.cranfill@boeing.com; (c)2003 The Boeing Company");

	if (args.length < 2) {
		showUsage();
		return;
	}
	
	// Prep the input
	//
	if (args[0].equals("-i") == false) {
		showUsage();
		return;
	}

	LineNumberReader lnr = null;
	String inFileName = args[1] + ".frm";
	try {
		lnr = new LineNumberReader(new FileReader(inFileName));
	}
	catch (FileNotFoundException fnfe) {
		logger_.severe("Can't open input file '" + inFileName + "'!");
		return;
	}

	// Prep the output
	// By default, output to System.out
	//
	PrintStream outStream = System.out;
	if (args.length > 2) {
		if (args.length != 4) {
			showUsage();
			return;
		}
		if (args[2].equals("-o") == false) {
			showUsage();
			return;
		}
		try {
			outStream = new PrintStream(new FileOutputStream(args[3]));
		}
		catch (FileNotFoundException fnfe) {
			logger_.severe("Can't open output file '" + args[3] + "'!");
			fnfe.printStackTrace();
		}
	}


	if (outStream == System.out)
		System.out.println("\n"+"---------------- output code ----------------");
	VB2J vb2j = new VB2J(args[1], ""/* "com.boeing.pw.mct.vb2jTest" */, args[1]);
	vb2j.convert(lnr, outStream);
	if (outStream == System.out)
		System.out.println("-------------- end output code --------------");

} // main


/**
	Create the log4j logger.
**/
private static void
setUpLogger() {

	// Set up a logger.
	//
	ConsoleHandler ch = new ConsoleHandler();
    ch.setLevel(Level.ALL); // must do this or it will never display less than INFO
    ch.setFormatter(new SimpleLogFormatter());   // SimpleLogFormatter is ours
    logger_.addHandler(ch);
    logger_.setUseParentHandlers(false);    // we don't want ConsoleHandler's usual output, just ours
    logger_.setLevel(Level.FINEST); // INFO is the usual

}


/**
	Tell 'em what we want.
**/
private static void
showUsage() {

	logger_.info("usage: java com.boeing.pw.mct.vb2j.VB2J -i {infile} -o {outfile}");
} // showUsage


/**
	Tokenize all non-comment, uh, tokens, and return an ArrayList of them.

	Such as
		Dim threadcall(numfunctionmax) As Integer ' Holds max functions
	becomes
		returnval[0] = "Dim"
		returnval[1] = "threadcall(numfunctionmax)"
		returnval[2] = "As"
		returnval[3] = "Integer"

	Note that this doens't quite do what you'd want for
		Dim X(10, 20) As Integer
	but we deal with that in the "Dim" processing code.

**/
private ArrayList // of String
tokenizeToArrayList(String line, String tokens) {

	StringTokenizer st = new StringTokenizer(line, tokens);
	ArrayList result = new ArrayList();
	while (st.hasMoreTokens()) {
		String token = (String)st.nextToken();
		if (token.startsWith("'"))
			break;
		result.add(token);
	}
	return result;
}


/**
	Form the appropriate failure string, depending on the failure option.
**/
private String
failureString() {

	if (FORCE_COMPILE_ERROR_ON_TRANSLATION_FAILURE) {
		return TRANS_FAILURE_COMMENT_STRING_ERROR;
	}
	else {
		return TRANS_FAILURE_COMMENT_STRING_SILENT;
	}
}


/**
	Prettyprint an ArrayList of Strings, each item on a new line.
**/
private String
arrayListToString(ArrayList al) {
	
	StringBuffer sb = new StringBuffer();
	for (int i=0; i<al.size(); i++) {
		sb.append((String)al.get(i) + "\n");
	}
	return sb.toString();
}


} // VB2J
