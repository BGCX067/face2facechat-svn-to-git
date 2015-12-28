/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package main;


import java.io.*;
import java.awt.*;
import java.net.*;
import java.awt.event.*;
import java.util.Vector;

import javax.media.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.Format;
import javax.media.format.FormatChangeEvent;
import javax.media.control.BufferControl;



public class client implements ReceiveStreamListener, SessionListener,
	ControllerListener
{
    String sessions[] = null;
    RTPManager mgrs[] = null;
    Vector playerWindows = null;
  int port[]=null;
    boolean dataReceived = false;
    Object dataSync = new Object();


    public client(String sessions[],int port[]) {
	this.sessions = sessions;
        this.port=port;
    }

    protected boolean initialize() {

        try {
	    InetAddress ipAddr;
	    SessionAddress localAddr = new SessionAddress();
	    SessionAddress destAddr;

	    mgrs = new RTPManager[sessions.length];
	    playerWindows = new Vector();




	    for (int i = 0; i < sessions.length; i++) {




		System.err.println("  - Open RTP session for: addr: " + sessions[i]);

		mgrs[i] = (RTPManager) RTPManager.newInstance();
		mgrs[i].addSessionListener(this);
		mgrs[i].addReceiveStreamListener(this);

		ipAddr = InetAddress.getByName(sessions[i]);

		if( ipAddr.isMulticastAddress()) {

		    localAddr= new SessionAddress( ipAddr,
						   port[i],
						   1);
		    destAddr = new SessionAddress( ipAddr,
						   port[i],
						   1);
		} else {
		    localAddr= new SessionAddress( InetAddress.getLocalHost(),
			  		           port[i]);
                    destAddr = new SessionAddress( ipAddr,port[i]);
		}

		mgrs[i].initialize( localAddr);


BufferControl bc = (BufferControl)mgrs[i].getControl("javax.media.control.BufferControl");
		if (bc != null)
		    bc.setBufferLength(350);

    		mgrs[i].addTarget(destAddr);

	    }

        } catch (Exception e){
            System.err.println("Cannot create the RTP Session: " + e.getMessage());
            return false;
        }



	long then = System.currentTimeMillis();
	long waitingPeriod = 30000;

	try{
	    synchronized (dataSync) {
		while (!dataReceived &&
			System.currentTimeMillis() - then < waitingPeriod) {
		    if (!dataReceived)
			System.err.println("  - Waiting for RTP data to arrive...");
		    dataSync.wait(1000);
		}
	    }
	} catch (Exception e) {}

	if (!dataReceived) {
	    System.err.println("No RTP data was received.");
	    close();
	    return false;
	}

        return true;
    }


    public boolean isDone() {
	return playerWindows.size() == 0;
    }



    protected void close() {

	for (int i = 0; i < playerWindows.size(); i++) {
	    try {
		((PlayerWindow)playerWindows.elementAt(i)).close();
	    } catch (Exception e) {}
	}

	playerWindows.removeAllElements();


	for (int i = 0; i < mgrs.length; i++) {
	    if (mgrs[i] != null) {
                mgrs[i].removeTargets( "Closing session from Main");
                mgrs[i].dispose();
		mgrs[i] = null;
	    }
	}
    }


    PlayerWindow find(Player p) {
	for (int i = 0; i < playerWindows.size(); i++) {
	    PlayerWindow pw = (PlayerWindow)playerWindows.elementAt(i);
	    if (pw.player == p)
		return pw;
	}
	return null;
    }


    PlayerWindow find(ReceiveStream strm) {
	for (int i = 0; i < playerWindows.size(); i++) {
	    PlayerWindow pw = (PlayerWindow)playerWindows.elementAt(i);
	    if (pw.stream == strm)
		return pw;
	}
	return null;
    }



    public synchronized void update(SessionEvent evt) {
	if (evt instanceof NewParticipantEvent) {
	    Participant p = ((NewParticipantEvent)evt).getParticipant();
	    System.err.println("  - A new participant had just joined: " + p.getCNAME());
	}
    }



    public synchronized void update( ReceiveStreamEvent evt) {

	RTPManager mgr = (RTPManager)evt.getSource();
	Participant participant = evt.getParticipant();	// could be null.
	ReceiveStream stream = evt.getReceiveStream();  // could be null.

	if (evt instanceof RemotePayloadChangeEvent) {

	    System.err.println("  - Received an RTP PayloadChangeEvent.");
	    System.err.println("Sorry, cannot handle payload change.");
	    System.exit(0);

	}

	else if (evt instanceof NewReceiveStreamEvent) {

	    try {
		stream = ((NewReceiveStreamEvent)evt).getReceiveStream();
		DataSource ds = stream.getDataSource();


		RTPControl ctl = (RTPControl)ds.getControl("javax.media.rtp.RTPControl");
		if (ctl != null){
		    System.err.println("  - Recevied new RTP stream: " + ctl.getFormat());
		} else
		    System.err.println("  - Recevied new RTP stream");

		if (participant == null)
		    System.err.println("      The sender of this stream had yet to be identified.");
		else {
		    System.err.println("      The stream comes from: " + participant.getCNAME());
		}


		Player p = javax.media.Manager.createPlayer(ds);
		if (p == null)
		    return;

		p.addControllerListener(this);
		p.realize();
		PlayerWindow pw = new PlayerWindow(p, stream);
		playerWindows.addElement(pw);


		synchronized (dataSync) {
		    dataReceived = true;
		    dataSync.notifyAll();
		}

	    } catch (Exception e) {
		System.err.println("NewReceiveStreamEvent exception " + e.getMessage());
		return;
	    }

	}

	else if (evt instanceof StreamMappedEvent) {

	     if (stream != null && stream.getDataSource() != null) {
		DataSource ds = stream.getDataSource();

		RTPControl ctl = (RTPControl)ds.getControl("javax.media.rtp.RTPControl");
		System.err.println("  - The previously unidentified stream ");
		if (ctl != null)
		    System.err.println("      " + ctl.getFormat());
		System.err.println("      had now been identified as sent by: " + participant.getCNAME());
	     }
	}

	else if (evt instanceof ByeEvent) {

	     System.err.println("  - Got \"bye\" from: " + participant.getCNAME());
	     PlayerWindow pw = find(stream);
	     if (pw != null) {
		pw.close();
		playerWindows.removeElement(pw);
	     }
	}

    }



    public synchronized void controllerUpdate(ControllerEvent ce) {

	Player p = (Player)ce.getSourceController();

	if (p == null)
	    return;


	if (ce instanceof RealizeCompleteEvent) {
	    PlayerWindow pw = find(p);
	    if (pw == null) {
		// Some strange happened.
		System.err.println("Internal error!");
		System.exit(-1);
	    }
	    pw.initialize();
	    pw.setVisible(true);
	    p.start();
	}

	if (ce instanceof ControllerErrorEvent) {
	    p.removeControllerListener(this);
	    PlayerWindow pw = find(p);
	    if (pw != null) {
		pw.close();
		playerWindows.removeElement(pw);
	    }
	    System.err.println("Main internal error: " + ce);
	}

    }





    class PlayerWindow extends Frame {

	Player player;
	ReceiveStream stream;

	PlayerWindow(Player p, ReceiveStream strm) {
	    player = p;
	    stream = strm;
	}

	public void initialize() {
	    add(new PlayerPanel(player));
	}

	public void close() {
	    player.close();
	    setVisible(false);
	    dispose();
	}

	public void addNotify() {
	    super.addNotify();
	    pack();
	}
    }


    class PlayerPanel extends Panel {

	Component vc, cc;

	PlayerPanel(Player p) {
	    setLayout(new BorderLayout());
	    if ((vc = p.getVisualComponent()) != null)
		add("Center", vc);
	    if ((cc = p.getControlPanelComponent()) != null)
		add("South", cc);
	}

	public Dimension getPreferredSize() {
	    int w = 0, h = 0;
	    if (vc != null) {
		Dimension size = vc.getPreferredSize();
		w = size.width;
		h = size.height;
	    }
	    if (cc != null) {
		Dimension size = cc.getPreferredSize();
		if (w == 0)
		    w = size.width;
		h += size.height;
	    }
	    if (w < 160)
		w = 160;
	    return new Dimension(w, h);
	}
    }


   



}




