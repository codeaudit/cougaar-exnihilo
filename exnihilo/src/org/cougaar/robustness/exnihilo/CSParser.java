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

import org.cougaar.scalability.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

import java.io.*;


/**
    Parse the XML representing a CougaarSociety, and create a CougaarSociety from it. <BR>
    The inverse of the method CougaarSociety.toXML() <BR>
  
    @author robert.e.cranfill@boeing.com
    @version 1.1 - moved from obsolete 'exnihilo2002' package
    @version 1.0 - July 2002

**/
public class CSParser {


// This can be changed via our setEncodingMethod() method.
//
private String  encodingMethod_ = "US-ASCII";

public boolean echoStruct_ = false; // display the parsed structure?

/**
 * Create a parser with 'echo structure' flag == false.
 */
public CSParser() {
	}


/**
 * Create a parser with given value for 'echo structure' flag.
 */
public CSParser(boolean echoStructure) {
	super();
	echoStruct_ = echoStructure;
	}


public static final void
main(String[] args) {

    CSParser pcs = new CSParser();

    if (args.length == 1) {
        try {
            pcs.parseCSXML(new FileInputStream(args[0]));
            }
        catch (FileNotFoundException fnfe) {
            SystemDesign.logError("CSParser.main: can't read input file?\n", fnfe);
            }
        }
    else {
        pcs.parseCSXML(pcs.testXMLInputStream());
        }
    }


/**
    'setter' for encoding method used to get input stream from input string. <BR>
    Used by the String.getBytes() method to convert to a ByteArrayInputStream in
    our societyFromXML() method. Seemingly not useful, but here it is.
**/
public void
setEncodingMethod(String encoding) {

    encodingMethod_ = encoding;
    }


/**
    Parse the given XML into a CougaarSociety object hierarchy. <BR>
    Return null on failure.
**/
public CougaarSociety
societyFromXML(String xml) {

    ByteArrayInputStream bais = null;
    try {
        bais = new ByteArrayInputStream(xml.getBytes(encodingMethod_));
        }
    catch (java.io.UnsupportedEncodingException uee) {
        SystemDesign.logError("", uee);
        return null;
        }

    return parseCSXML(bais);
    }


/**
    Given the input stream, parse it. <BR>
    @return null on failure.
**/
public CougaarSociety
parseCSXML(InputStream xmlIn) {
    
    // Create a DocumentBuilderFactory and options on it.
    //
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);    // *gotta* do this, they say
    dbf.setIgnoringComments(false);
    dbf.setIgnoringElementContentWhitespace(true);  // we don't care about WS
    dbf.setCoalescing(false);
    dbf.setExpandEntityReferences(false);

    // Create a DocumentBuilder
    //
    DocumentBuilder db = null;
    try {
        db = dbf.newDocumentBuilder();
        }
    catch (javax.xml.parsers.ParserConfigurationException pce) {
		SystemDesign.logError("CSParser.parseCSXML: Parser error on newDocumentBuilder:\n", pce);
        return null;
        }

    // Parse the input file to get a Document object.
    //
    Document doc = null;
    try {
        doc = db.parse(xmlIn);
        }
    catch (org.xml.sax.SAXException se) {
		SystemDesign.logError("CSParser.parseCSXML: SAX exception on parse:\n", se);
		return null;
        }
    catch (IOException ioe) {
		SystemDesign.logError("CSParser.parseCSXML: IO exception on parse:\n", ioe);
		return null;
       }

    CougaarSociety result = createSocFromDoc(doc);

//  SystemDesign.logDebug("");
//	SystemDesign.logDebug("CougaarSociety XML is:\n" + result.toXML());
//	SystemDesign.logDebug("end CougaarSociety XML");
//	SystemDesign.logDebug("");

    return result;
    }


/**
    Given a top-level Document node that looks like this: <BR>

    <PRE><TT>
        DOC: nodeName="#document"
            ELEM: nodeName="CougaarSociety"
                ATTR: nodeName="identifier" nodeValue="baseline"
            ELEM: nodeName="CougaarNode" 
                ATTR: nodeName="name"  nodeValue="ProbabilityOfFailure"
                ATTR: nodeName="value" nodeValue="0.2"
                ...
                ELEM: nodeName="Attribute" 
                    ATTR: nodeName="name"  nodeValue="CPU"
                    ATTR: nodeName="value" nodeValue="900"
                    ...
                    ELEM: nodeName="CougaarAgent" 
                        ATTR: nodeName="name"  nodeValue="ProbabilityOfFailure"
                        ATTR: nodeName="value" nodeValue="0.2"
                        ...
                        ELEM: nodeName="Requirement" 
                            ATTR: nodeName="name"  nodeValue="CPU"
                            ATTR: nodeName="value" nodeValue="50"
                            ...
    </TT></PRE>

    create the CougaarSociety it represents.
 
**/
private CougaarSociety
createSocFromDoc(org.w3c.dom.Node n) {

    CougaarSociety society = new CougaarSociety();

    // Top-level node should be DOCUMENT_NODE
    //
    if (n.getNodeType() != org.w3c.dom.Node.DOCUMENT_NODE) {
        SystemDesign.logError("createSoc: top node isn't DOCUMENT_NODE !");
        return null;
        }

    // First (and only) child ELEMENT_NODE should be a "CougaarSociety"
    //
	org.w3c.dom.Node socNode = null;
    for (org.w3c.dom.Node child = n.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            if (child.getNodeName().equals("CougaarSociety")) {
                socNode = child;
                break;
                }
            }
        }
    if (socNode == null) {
        SystemDesign.logError("createSoc: no first-level ELEMENT_NODE CougaarSociety!");
        return null;
        }

	// identifier - sept03
	String socID = getNamedAttr(socNode, "identifier");
	society.setIdentifier(socID);

    // All children in CougaarSociety are Node.ELEMENT_NODE CougaarNode
    //
    for (org.w3c.dom.Node child = socNode.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            if (child.getNodeName().equals("CougaarNode")) {

                String nodeName = getNamedAttr(child, "name");
                if (echoStruct_)SystemDesign.logDebug("CougaarNode: " + nodeName);
                CougaarNode cNode = society.newNode(nodeName);

                // All children of CougaarNode should be either
                //  Node.ELEMENT_NODE Attribute
                // or
                //  Node.ELEMENT_NODE CougaarAgent
                //
                for (org.w3c.dom.Node child2 = child.getFirstChild(); child2 != null; child2 = child2.getNextSibling()) {
                    
                    if (child2.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        if (child2.getNodeName().equals("Attribute")) {

                            String attrName  = getNamedAttr(child2, "name");
                            String attrValue = getNamedAttr(child2, "value");
                            if (echoStruct_)SystemDesign.logDebug(" Attribute: " + attrName + " = " + attrValue);
                            cNode.setAttribute(attrName, attrValue);
                            }    // Attribute
                        else 
                        if (child2.getNodeName().equals("CougaarAgent")) {

                            String agentName = getNamedAttr(child2, "name");
                            if (echoStruct_)SystemDesign.logDebug("  CougaarAgent: " + agentName);
                            CougaarAgent agent = cNode.newAgent(agentName);

                            // Set all the requirements for the agent
                            //
                            for (org.w3c.dom.Node child3 = child2.getFirstChild(); child3 != null; child3 = child3.getNextSibling()) {
                                if (child3.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE &&
                                    child3.getNodeName().equals("Requirement")) {

                                    String attrName  = getNamedAttr(child3, "name");
                                    String attrValue = getNamedAttr(child3, "value");
                                    if (echoStruct_)SystemDesign.logDebug("   Requirement: " + attrName + " = " + attrValue);
                                    agent.setRequirement(attrName, attrValue);
                                    } // Requirement
                                } // child3
                            }    // CougaarAgent
                        }
                    } // child2
                }
            }
        } // child

    return society;
    }   // createSocFromDoc


/**
    Find the attribute with a given nodeName, and return its nodeValue
**/
private String
getNamedAttr(org.w3c.dom.Node n, String attrName) {

    String result = null;
    NamedNodeMap atts = n.getAttributes();
    if (atts != null)
        result = atts.getNamedItem(attrName).getNodeValue();
    return result;
    }


/**
    Create test data.
**/
public InputStream
testXMLInputStream() {
    
    String xml = 
        "<?xml version=\"1.0\"?>" +
        "<!-- robert.e.cranfill@boeing.com: one node, two agents, that's it -->" +
        "" +
        "<CougaarSociety identifier=\"baseline\">" +
        "" +
        "    <CougaarNode name=\"FWD-F\">" +
        "        <Attribute name=\"ProbabilityOfFailure\" value=\"0.2\"/>" +
        "        <Attribute name=\"CPU\" value=\"900\"/>" +
        "        <Attribute name=\"Memory\" value=\"512\"/>" +
        "        <Attribute name=\"OperatingSystem\" value=\"LINUX\"/>" +
        "" +
        "        <CougaarAgent name=\"15-PLS-TRKCO\">" +
        "            <Requirement name=\"CPU\" value=\"50\"/>" +
        "            <Requirement name=\"Memory\" value=\"30\"/>" +
        "            <Requirement name=\"OperatingSystem\" value=\"LINUX\"/>" +
        "            " +
        "<!-- test of comment in middle -->" +
        "" +
        "            <Requirement name=\"BandwidthReceived_106-TCBN\" value=\"3\"/>" +
        "            <Requirement name=\"BandwidthSent_106-TCBN\" value=\"3\"/>" +
        "        </CougaarAgent>" +
        "" +
        "        <CougaarAgent name=\"106-TCBN\">" +
        "            <Requirement name=\"OperatingSystem\" value=\"LINUX\"/>" +
        "            <Requirement name=\"CPU\" value=\"50\"/>" +
        "            <Requirement name=\"Memory\" value=\"30\"/>" +
        "            " +
        "            <Requirement name=\"BandwidthSent_15-PLS-TRKCO\" value=\"3\"/>" +
        "            <Requirement name=\"BandwidthReceived_15-PLS-TRKCO\" value=\"3\"/>" +
        "        </CougaarAgent>" +
        "" +
        "    </CougaarNode>" +
        "    " +
        "    <CougaarNode name=\"FWD-G\">" +
        "        <Attribute name=\"ProbabilityOfFailure\" value=\"0.4\"/>" +
        "        <Attribute name=\"CPU\" value=\"100\"/>" +
        "        <Attribute name=\"Memory\" value=\"1024\"/>" +
        "        <Attribute name=\"OperatingSystem\" value=\"Windows\"/>" +
        "" +
        "        <CougaarAgent name=\"16-PLS-TRKCO2\">" +
        "            <Requirement name=\"CPU\" value=\"50\"/>" +
        "            <Requirement name=\"Memory\" value=\"30\"/>" +
        "            <Requirement name=\"OperatingSystem\" value=\"Windows\"/>" +
        "            <Requirement name=\"BandwidthReceived_106-TCBN\" value=\"3\"/>" +
        "            <Requirement name=\"BandwidthSent_106-TCBN\" value=\"3\"/>" +
        "        </CougaarAgent>" +
        "" +
        "        <CougaarAgent name=\"107-TCBN2\">" +
        "            <Requirement name=\"OperatingSystem\" value=\"LINUX\"/>" +
        "            <Requirement name=\"CPU\" value=\"50\"/>" +
        "            <Requirement name=\"Memory\" value=\"30\"/>" +
        "            <Requirement name=\"BandwidthSent_15-PLS-TRKCO\" value=\"3\"/>" +
        "            <Requirement name=\"BandwidthReceived_15-PLS-TRKCO\" value=\"3\"/>" +
        "        </CougaarAgent>" +
        "" +
        "    </CougaarNode>" +
        "    " +
        "</CougaarSociety>";
        
    ByteArrayInputStream bais = null;
    try {
        bais = new ByteArrayInputStream(xml.getBytes(encodingMethod_));
        }
    catch (java.io.UnsupportedEncodingException uee) {
		SystemDesign.logError("CSParser.testXMLInputStream: IO exception on encoding:\n", uee);
        }
    return bais;
    }   // testXMLInputStream
    

}
