/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package main;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 *
 * @author Lucy
 */
public class Connection {
      public Connection(String ip,String text){
          OutputStreamWriter out = null;

            try{
                Socket http = new Socket(ip, 41050);
                OutputStream raw = http.getOutputStream();
                OutputStream buffered =  new BufferedOutputStream(raw);
                out= new OutputStreamWriter(buffered, "ASCII");
                out.write(text);

            }
            catch(Exception e){
                System.err.println(e);

            }
            finally{
                try
                {
                    out.close();

                }
                catch(Exception e){

                }
            }

      }

}
