/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Lucy
 */
public class SListener {
    public SListener(){
        try{
            ServerSocket server = new ServerSocket(41050);
        while(true){
        Socket connection = null;

        try{
            connection = server.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));


            String input;
            input=in.readLine();
            String out1 = connection.getInetAddress().toString();
            out1=out1.substring(1);
            System.out.print(input);
            String ipn[]=new String[1];
            ipn[0]=out1;
            if(input.equals("hello")){
                
                Accept acc = new Accept(ipn,"hi");
                acc.setVisible(true);

                
            }
             if(input.equals("bye")){
                 new Bye().setVisible(true);
                  try{
            Thread.currentThread().sleep(3000);
        }catch(InterruptedException ie){}
                 System.exit(-1);

             }
         if(input.equals("hello1")){

                Accept acc = new Accept(ipn,"hi1");
                acc.setVisible(true);


            }
            String ipsc[]=new String[4];
            int i=0;
            if(input.equals("hi")){
                ipsc[i]=out1;
               i++;
                 
	}

             if(input.equals("hi1")){String ipc[]=null;
                 if(i==0){ i=1;   ipc=new String[i]; ipc[i-1]=out1; }
                 else {  ipc=new String[i];
                      for(int k=0;k<i;k++){ ipc[k]=ipsc[k]; }
             ipc[i]=out1;
             i++; }
             String ip[]=new String[i*2];
             int port[]=new int [i*2];
             int j=0;
             for(int h=0;h<i;h++){ ip[j]=ipc[h]; port[j]=400; j++; ip[j]=ipc[h]; port[j]=800; j++;}
client as=new client(ip,port);
	if (!as.initialize()) {
	    System.err.println("Failed to initialize the sessions.");
	    System.exit(-1);}
            try {
	    while (!as.isDone())
		Thread.sleep(1000);
	} catch (Exception e) {}

	System.err.println("Exiting Main");
	}
           

	


            }
            
        
        catch(IOException e){

        }
        finally{
            try{


            if(connection!=null) connection.close();
            }
            catch(IOException e){

            }

        }
        }

        }
        catch(IOException e){

            }

    }

}
