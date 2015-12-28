/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package main;

/**
 *
 * @author Gourav
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package server;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package sound;
import java.awt.*;
import javax.media.*;
import javax.media.ControllerListener;
import java.net.InetAddress;
import java.io.*;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;
import javax.media.format.*;
import javax.media.control.TrackControl;
import javax.media.control.QualityControl;
import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;
import com.sun.media.rtp.*;
//import javax.media.create


/**
 *
 * @author Gourav
 */
public class serverv {


    MediaLocator audio,video;
    String ipaddress[];
    int portBase;
    Processor processor=null;
    RTPManager rtpMgrs[];
    DataSource dataOutput=null;
    public serverv(String ipAddress[],String pb)
    {
        this.audio=new MediaLocator("javasound://0");
        this.video=new MediaLocator("vfw://0");
        this.ipaddress=ipAddress;
        this.portBase=Integer.valueOf(pb).intValue();

    }
    //starts transmission
public synchronized String start()
{
    String result=null;
    //  (not created yet)
    result=createProcessor();
    if(result!=null) return result;
    //(not yet created)
    result=createTransmitter();
  if(result!=null)
  {
      processor.close();
      processor=null;
      return result;
  }
    processor.start();
    return null;

}
public void stop(){
    synchronized(this){
        if(processor!=null){
            processor.stop();
            processor.close();
            processor=null;
            for(int i=0;i<rtpMgrs.length;i++)
            {
                rtpMgrs[i].removeTargets("Session ended");
                rtpMgrs[i].dispose();
            }

        }
    }
}

//now create the media processor
private String createProcessor()
{
    if(video==null || audio==null)
        return "locator is null";
    DataSource vds,ads,ds=null;
    DataSource clone;
    try{
        //vds=javax.media.Manager.createDataSource(video);
        ads=javax.media.Manager.createDataSource(video);
       //DataSource darray[]=new DataSource[2];
        //darray[0]=vds;
        //darray[1]=ads;
        //ds=javax.media.Manager.createMergingDataSource(darray);
        ds=ads;
        //ds=ads;
//ds=ads;
    }
    catch(Exception e){ System.out.println("cannot create datasource");}
    FileTypeDescriptor outputType = new FileTypeDescriptor(FileTypeDescriptor.MSVIDEO);

		// setup output video and audio data format
		Format outputFormat[] = new Format[2];
		outputFormat[0] = new VideoFormat(VideoFormat.INDEO50);
		outputFormat[1] = new AudioFormat(AudioFormat.GSM_MS /* LINEAR */);

		// create processor
		ProcessorModel processorModel = new ProcessorModel(ds, outputFormat, outputType);
 /*  Format formats[] = new Format[2];
formats[0] = new AudioFormat(AudioFormat.IMA4);
formats[1] = new VideoFormat(VideoFormat.INDEO50);
FileTypeDescriptor outputType =
new FileTypeDescriptor(FileTypeDescriptor.RAW);*/
    try {
	    processor = javax.media.Manager.createProcessor(ds);
	} catch (Exception npe) {
	    return "Couldn't create processor";
	}
boolean res=waitForState(processor,Processor.Configured);
if(res==false) return "couldnt realize processor";
   // setJPEGQuality(processor,0.5f);
    //dataOutput=processor.getDataOutput();
TrackControl [] tracks=processor.getTrackControls();
if(tracks==null || tracks.length<1)
    return "couldn,t find tracks in processor";
ContentDescriptor cd=new ContentDescriptor(ContentDescriptor.RAW_RTP);
processor.setContentDescriptor(cd);
Format supported[];
Format chosen;
boolean atLeastOneTrack=false;
for(int i=0;i<tracks.length;i++)
{Format format=tracks[i].getFormat();
 if(tracks[i].isEnabled()){
     supported=tracks[i].getSupportedFormats();
     if(supported.length>0){
         if(supported[0] instanceof VideoFormat)
         {
             //chosen=checkForVideoSizes(tracks[i].getFormat(),supported[0]);
             chosen=supported[0];
         }
         else
         {
             chosen=supported[0];
         }
         tracks[i].setFormat(chosen);
         atLeastOneTrack=true;
     }
     else tracks[i].setEnabled(false);

 }
 else tracks[i].setEnabled(false);
}
if(!atLeastOneTrack)
    return ("could not set any of the track");
res=waitForState(processor,Controller.Realized);
if(res==false )
    return "couldnt realize processor";
//
//setJPEGQuality(processor,0.5f);
    //
dataOutput=processor.getDataOutput();
return null;
}
//create rtp manager
public  String createTransmitter()
{
    PushBufferDataSource pbds=(PushBufferDataSource)dataOutput;
    PushBufferStream pbss[]=pbds.getStreams();
    rtpMgrs=new RTPManager[pbss.length];
    SessionAddress localAddr,destAddr;
    InetAddress ipAddr;
    SendStream sendStream;
    int port;
    SourceDescription srcDesList[];
    for(int i=0;i<pbss.length;i++)
    {
        try{
            rtpMgrs[i]=RTPManager.newInstance();
            port=portBase+2*i;
            localAddr=new SessionAddress(InetAddress.getLocalHost(),port);
            rtpMgrs[i].initialize(localAddr);
            for(int k=0;k<ipaddress.length;k++)
                 rtpMgrs[i].addTarget(new SessionAddress(InetAddress.getByName(ipaddress[k]),port));
            sendStream=rtpMgrs[i].createSendStream(dataOutput, i);
            sendStream.start();

        } catch(Exception e) { return e.getMessage(); }

    }
    return null;

}
//CHECK FOR SIZE
Format checkForVideoSizes(Format original,Format supported)
{
    int width,height;
    Dimension size=((VideoFormat)original).getSize();
    Format jpegFmt=new Format(VideoFormat.JPEG_RTP);
    Format h263Fmt=new Format(VideoFormat.H263_RTP);
    if(supported.matches(jpegFmt))
    {
        width=(size.width%8==0 ?size.width:(int)(size.width/8)*8);
        height=(size.height%8==0?size.height:(int)(size.height/8)*8);

    }
    else if(supported.matches(h263Fmt))
    {
        if(size.width<128){ width=128; height=96; }
        else if(size.width<176){ width=176; height=144; }
        else { width=352; height=288; }
    }
    else
    {
        return supported;
    }
    return(new VideoFormat(null,new Dimension(width,height),Format.NOT_SPECIFIED,null,Format.NOT_SPECIFIED)).intersects(supported);
    }


void setJPEGQuality(Player p,float val)
{
Control cs[]=p.getControls();
QualityControl qc=null;
VideoFormat jpegFmt=new VideoFormat(VideoFormat.JPEG);
for(int i=0;i<cs.length;i++)
{
    if(cs[i] instanceof QualityControl && cs[i] instanceof Owned)
    {
        Object owner=((Owned)cs[i]).getOwner();
        if(owner instanceof Codec)
        {
            Format fmts[]=((Codec)owner).getSupportedOutputFormats(null);
            for(int j=0;j<fmts.length;j++){
                if(fmts[j].matches(jpegFmt)){
                    qc=(QualityControl)cs[i];
                    qc.setQuality(val);
                    break;
                }
            }
        }
        if(qc!=null)  break;
    }
}
}

Integer stateLock=new Integer(0);
boolean failed=false;
Integer getStateLock()
{
    return stateLock;
}
void setFailed()
{
    failed=true;

}
public synchronized boolean waitForState(Processor p,int state)
{
    p.addControllerListener(new StateListener());
    failed=false;
    if(state==Processor.Configured){ p.configure(); }
    else if(state==Processor.Realized) {p.realize();}
    while(p.getState()<state &&  !failed) {
        synchronized (getStateLock()){
            try
            { getStateLock().wait();

            }catch(InterruptedException ie){ return false; }
        }

        }
if(failed) return false;
else return true;
}
//inner classes
class StateListener implements ControllerListener{
    public void controllerUpdate(ControllerEvent ce)
    {
        if(ce instanceof ControllerClosedEvent)
            setFailed();
        if(ce instanceof ControllerEvent){
            synchronized(getStateLock()){
                getStateLock().notifyAll();
            }
        }
    }
}
/**
     * @param args the command line arguments
     */
  
    

}

