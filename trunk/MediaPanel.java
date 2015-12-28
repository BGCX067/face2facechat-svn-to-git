/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;
import java.net.URL;
import javax.media.CannotRealizeException;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.naming.ldap.ManageReferralControl;
import javax.swing.JPanel;

/**
 *
 * @author Lucy
 */
public class MediaPanel extends JPanel {
    Player mediaPlayer=null;
    public MediaPanel(URL mediaUrl){
        setLayout(new BorderLayout());
        Manager.setHint(Manager.LIGHTWEIGHT_RENDERER, true);
        try{
             mediaPlayer= Manager.createRealizedPlayer(mediaUrl);
            Component video=mediaPlayer.getVisualComponent();
            Component controls = mediaPlayer.getControlPanelComponent();
            if(video!=null){
                add(video,BorderLayout.CENTER);
            }
                if(controls!=null){
                add(controls,BorderLayout.SOUTH);

            }
            mediaPlayer.start();
        }
        catch(NoPlayerException e){
            System.err.println("No media Player Found");
        }
        catch(CannotRealizeException e){
            System.err.println("Could not realise the media player");
        }
        catch(IOException e){
            System.err.println("Error Reading from the source");
        }
    }
    public void close()
    {
        mediaPlayer.stop();
    }

}
