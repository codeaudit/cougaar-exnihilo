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
    Link
    Based on ExNihilo VB code as of 30April02
		
    <PRE>
		Type linktype
			name As String * 30
			node1 As Integer
			node2 As Integer
			'limits on info = numnodemax*numpropertymax
			'for numnodemax = 30, numrolelistmax = 50, then
			'In this model, the limit on links is numlinkmax rather than numnodemax*numnodemax. This helps a little.
			'The TOTAL memory requirement of Linkinfo.info(ilink).* must account for the numnode*numnode possible links
			'sizoflinkinfo_info= numnodemax*numnodemax*(numnodemax*numpropertymax)
			'which in this model gives 30*30*(size(Linkinfo.info)) =900*(306120 ) bytes
			background As Single
			bandwidth As Single
			costbase As Single
			costperkb As Single
			costpermonth As Single
			hops As Integer
			latency As Single
			mtu As Single 'max transmission unit in Bytes
			packetmax As Long
			protocol As Integer  'custom=0, atm=1, Ethernet=2, IEEE 802.3=3, Token Ring (IEEE 802.5)=4 , FDDI (RFC 1390)=5
			protocoloverhead As Integer 'protocol overhead in bytes
			pfail As Single
			text As String * 400 'five 80 char strings of text description per link...hmmm.
			
			'type = ALL/Other=0, Hub=1, WAN=2
			'type = 3 for continuation links used to draw non-planar graphs
			'a continuation link is an infinite bandwidth zero latency link
   			type As Integer
			
			saveposition As Integer
			left As Integer
			top  As Integer
			linecolor As Long
			linewidth As Integer
			iconcolor As Long
			
			'run time link data:
			traffic As Single
			'linkused=0 if link not used in current state, linkused=1 if used
			linkused As Boolean
		End Type
    </PRE>

	@version "1.1"

    @author robert.e.cranfill@boeing.com

**/
public class Link {

// Contstants

//    'type = ALL/Other=0, LAN=1, WAN=2
//    'type = 5 for continuation links used to draw non-planar graphs
public static final int LINK_TYPE_OTHER = 0;
public static final int LINK_TYPE_LAN   = 1;
public static final int LINK_TYPE_WAN   = 2;
public static final int LINK_TYPE_CONT  = 3;

//    protocol As Integer  'custom=0, atm=1, Ethernet=2, IEEE 802.3=3, 
//                         ' Token Ring (IEEE 802.5)=4 , FDDI (RFC 1390)=5
public static final int LINK_PROTOCOL_CUSTOM    = 0;
public static final int LINK_PROTOCOL_ATM       = 1;
public static final int LINK_PROTOCOL_ETHERNET  = 2;
public static final int LINK_PROTOCOL_IEEE      = 3;
public static final int LINK_PROTOCOL_TOKEN     = 4;
public static final int LINK_PROTOCOL_FDDI      = 5;


public String 	name;
public int 		node1;
public int 		node2;
public float 	background;
public float 	bandwidth;
public float 	costbase;
public float 	costperkb;
public float 	costpermonth;
public int 		hops;
public float 	latency;
public float 	mtu;
public long 	packetmax;
public int 		protocol;
public int 		protocoloverhead;
public float 	pfail;
public String 	text = "";
public int 		type;
public int 		saveposition;
public int 		left;
public int 		top;
public long 	linecolor;
public int 		linewidth = 1;
public long 	iconcolor;
public float 	traffic;
public boolean 	linkused;

public boolean	enabled; 	// 1.1

/**
	Zero-arg constructor.
**/
public Link() {

}


/**
	Nice constructor - set reasonable or necessary values.
**/
public Link(String name, int linkType, int fromNode, int toNode) {

    this.name = name;
    type = linkType;
    node1 = fromNode;
    node2 = toNode;
    
    // useful defaults
    protocol = LINK_PROTOCOL_ETHERNET;
    bandwidth = 10E6f;     // in bps, so 10E6f = 10Mbps
 
    latency = 1E-6f;  // per MB 15July
 
    mtu = bandwidth * 100;    // if zero, causes div/0 errors in annealing step
    
    }


public String
toString() {
    return "[LinkType: '" + name + "'; " + node1 + "-" + node2 + "]";
    }

} // Link


