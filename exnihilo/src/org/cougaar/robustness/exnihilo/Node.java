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
	By-hand port of "nodetype" type, from systemdesignx.bas.

    <PRE>
    Original VB code:
        Type nodetype
            name As String * 40
            
            costbase As Single
            costbasepermonth As Single
            costpercpuproc As Single
            costpercpusec As Single
            
            costperdisk As Single 'new
            costpermbmemory As Single 'new
            
            costpermbstorage As Single
            costpermbtraffic As Single
            
            cpubackground As Single
            cpucount As Integer
            cpucountmax As Integer
            cpuutilmax As Single
              
            diskcount As Integer   'new
            diskcountmax As Integer   'new
            diskiobackground As Single
            diskiomax As Single
            diskiomaxreads As Single   'new
            diskiomaxwrites As Single   'new
            diskiomaxreadswrites As Single   'new
            disklatency As Single   'new
            disksize As Single
            
            jipstocpusecpersec As Single
            
            memory As Integer
            memorymax As Integer 'new
            msgforward As Integer
            
            pfail As Single
            
            packetratemax As Single
            
            'type = ALL/Other=0, Server=1, Router=2, LAN = 3, Net Sublayer = 4
            'type = 5 for link continuation nodes used to draw non-planar graphs
            'a link continuation node should have msgforward=1
            type As Integer
            
            'runtime variables: Not stored in project files:
            cpuutil As Single 'runtime utilization
            fcalltot(numfunctionmax) As Single
            
            memoryused As Single
            
            packetrate As Single
            
            'nodeused=0 if node not used in current state, nodeused=1 if used
            nodeused As Boolean


			//   'marc en 1.1 smoothing
			numagents As Integer  'number of agents on a server at runtime
			agentlist(numfunctionmax) As Integer 'list of agents on server at runtime
   
			//   'marc en 1.1 soft constraints
			enabled As Boolean  'true if server/router/hub is available for service, false otherwise
   
			//   'marc en 1.2 performance map
			//   protected As Boolean  'component will not be attacked in building performance map


            'for routers, nodeinfo(inod).nextnode(inodx) is the next hop along path from inod to inodx,
            'for a packet arriving at inod, destined for inodx
            nextnode(numnodemax) As Integer
            
            iconfile As String * 80 'name of the bitmap file for the object
            
            hardwarecost As Single  'total hardware fixed cost for current state
            fcallcost As Single 'total cost of all function calls

            text As String * 400 'five 80 char strings of text description per link...hmmm.
        End Type
    </PRE>

    @author robert.e.cranfill@boeing.com

    @version 27Mar03 - removed 95% CPU limit.

**/
public class Node {

// Haven't converted the code to use these symbols, but....
//
// 'type = ALL/Other=0, Server=1, Router=2, LAN = 3, Net Sublayer = 4
// 'type = 5 for link continuation nodes used to draw non-planar graphs
public static final int TYPE_OTHER  = 0;
public static final int TYPE_SERVER = 1;
public static final int TYPE_ROUTER = 2;
public static final int TYPE_LAN    = 3;
public static final int TYPE_NETSUB = 4;
public static final int TYPE_LINK   = 5;


public String 	name;

public float	costbase;
public float	costbasepermonth;
public float	costpercpuproc;
public float	costpercpusec;

public float	costperdisk;
public float	costpermbmemory;

public float	costpermbstorage;
public float	costpermbtraffic;

public float	cpubackground;   // a percentage, ie, 0 <= x <= 100
public int		cpucount;
public int		cpucountmax;
public float	cpuutilmax; // a percentage, ie, 0 <= x <= 100

public int		diskcount;
public int		diskcountmax;
public float	diskiobackground;
public float	diskiomax;
public float	diskiomaxreads;
public float	diskiomaxwrites;
public float	diskiomaxreadswrites;
public float	disklatency;
public float	disksize;

public float	jipstocpusecpersec;

public int		memory;
public int		memorymax;
public int		msgforward;

public float	pfail;

public float	packetratemax;

public int		type;

public float    cpuutil;
public float[]  fcalltot = new float[SystemDesign.numfunctionmax+1];
public float    memoryused;
public float    packetrate;

public boolean  nodeused;

public int 		numagents; // 1.1
public int		agentlist[] = new int [SystemDesign.numfunctionmax+1]; // 1.1
public boolean	enabled; // 1.1
public boolean	isProtected; // 1.1 (1.2?)

public int      nextnode[] = new int[SystemDesign.numnodemax+1];

public String   iconfile;

public float    hardwarecost;
public float    fcallcost;

public String   text;



/**
	Constructor
    Sets any fields that are *required* by solver (AFAIK)
**/
public Node() {

	// What's required to make things not blow up? 
    // Not *all* of these are necessary...
    //
	costpercpuproc = 1.0f;
	costpercpusec = 1.0f;
	costperdisk = 1.0f;
	costpermbmemory = 1.0f;
	costpermbstorage = 1.0f;
	costpermbtraffic = 1.0f;

	costbase = 1.0f;
	costbasepermonth = 1.0f;
 
    cpubackground   = 0f;   // a percentage, ie, 0 <= x <= 100
    cpuutilmax      = 100f; // a percentage, ie, 0 <= x <= 100
    cpucount        = 1;
    cpucountmax     = 1;

/*
    diskcount = 1;
    diskcountmax = 1;
    diskiobackground = 1.0f;
    diskiomax = 1.0f;
    diskiomaxreads = 1.0f;
    diskiomaxwrites = 1.0f;
    diskiomaxreadswrites = 1.0f;
    disklatency = 1.0f;
    disksize = 1.0f;
*/


	// 1.1
	enabled = true;
	
} // constructor ()


/**
	
    As of 02May02, must specifcy >0 costbase, costbasepermonth

    Automatically sets msgforward_=1 for NODE_TYPE_ROUTER
    
**/
public Node(String name, int nodeType, float costbase, float costbasepermonth, int memory) {

	this();

    this.name = name;
    type = nodeType;

    if (costbase <= 0) {
		// should log this error?
		costbase = 0.001f;
	}
	else {
		this.costbase = costbase;
	}
	
    if (costbasepermonth <= 0) {
		// should log this error?
		costbasepermonth = 0.01f;
	}
	else {
		this.costbasepermonth = costbasepermonth;
	}
 
    this.memory = memory;
    memorymax = memory;

    if (nodeType == TYPE_ROUTER) {
        msgforward = 1;
        packetratemax = 1E6f;
        pfail = 0.001f;
        
        /* 2002 code had this. why? ???
        memory  = 0;
        cpuutil = 0;
        */

        }
    else {
        msgforward = 0;
        packetratemax = 1E3f;
        pfail = 0.01f;
        }

    } // nice constructor


} // class Node
