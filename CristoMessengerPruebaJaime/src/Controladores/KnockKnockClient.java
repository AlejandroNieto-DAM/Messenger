package Controladores;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author alejandronieto
 */

import Classes.EscuchaMensajes;
import Classes.RefrescarListaAmigos;
import Vista.CristoMessenger;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import java.util.Base64;
import java.util.ArrayList;

 
public class KnockKnockClient extends Thread{
    
    int portNumber;
    String hostName;
    String login;
    String pass;
    JFrame loginFrame;
    ClientProtocol protocol;
    CristoMessenger a;
    PrintWriter out;
    Socket kkSocket;
    BufferedReader in;
    
    
    LocalDateTime dateTime = LocalDateTime.now();
    
    RefrescarListaAmigos friendRefresh;
    EscuchaMensajes msgListener;
    
    Integer numeroMsgs = 0;
    int totalNumeroMensajes = 0;
    
    public KnockKnockClient(int port, String host, String login, String pass, JFrame frame) throws IOException{
        this.portNumber = port;
        this.hostName = host;
        this.login = login;
        this.pass = pass;
        this.loginFrame = frame;
        a = new CristoMessenger(this);
        protocol = new ClientProtocol(login, pass, a);
        kkSocket = new Socket(hostName, portNumber);
        out = new PrintWriter(kkSocket.getOutputStream(), true);
        in = new BufferedReader(
                new InputStreamReader(kkSocket.getInputStream()));
        
        friendRefresh = new RefrescarListaAmigos(this);
        msgListener = new EscuchaMensajes(in);

    }
    
    
    @Override
    public void run(){
 
        String fromServer = "";
        String fromUser = "";


        fromUser = protocol.processInput(null);
        out.println(fromUser);

        
        try {
            
            while((fromServer = in.readLine()) != null){

                if(fromServer.contains("LOGIN_CORRECT")){

                    protocol.processInput(fromServer);

                    try {
                        this.getPhoto();
                    } catch (IOException ex) {
                        Logger.getLogger(KnockKnockClient.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    this.a.loadPhoto();
                    this.loginFrame.setVisible(false);
                    this.a.setActualUser(login);
                    this.a.setVisible(true); 
                    //this.friendRefresh.run();
                    

                }

                if(fromServer.contains("MSGS")){
                    
                    System.out.println("Hemos entrao o k");
                    if(this.numeroMsgs == 0){
                        
                        protocol.processInput(fromServer);
                        this.numeroMsgs = protocol.getNumeroDeMensajes();
                        
                        System.out.println("Numero de msgs = " + this.numeroMsgs);
                        
                        if(this.numeroMsgs == 0){
                            out.println(protocol.getMessages());
                        } else {
                            out.println("PROTOCOLCRISTOMESSENGER1.0#" + dateTime + "#CLIENT#MSGS#OK_SEND!");
                        }
                        
                        //System.out.println("output del clinete --> " + protocol.getMessages());
                        
                    } else {
                        
                        if(numeroMsgs > 0){                        
                            protocol.leerMsgs(fromServer);
                            this.numeroMsgs--;
                            if(this.numeroMsgs == 0){

                                String theOutput = "PROTOCOLCRISTOMESSENGER1.0#" + dateTime + "#CLIENT#ALL_RECEIVED";
                                out.println(theOutput);
                                System.out.println("PERO MIRA QUE NUMERO --> " + this.numeroMsgs);

                                this.numeroMsgs = 1;
                                this.totalNumeroMensajes = 0;
                            }
                        } 
                       
                    }
                   
                   
                }
                
                if(fromServer.contains("CHAT")){
                    System.out.println("YEYO EN MI PUTA MADRE QUE FUNCIONA " + fromServer);
                }
                
                
                /*else {
                    try {
                        kkSocket.close();
                    } catch (IOException ex) {
                        Logger.getLogger(KnockKnockClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }*/
            }
            
        } catch (IOException ex) {
            Logger.getLogger(KnockKnockClient.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    

    
    public void getPhoto() throws IOException{
        String output = protocol.getPhoto();
        out.println(output);
        ArrayList<String> cadenas = new ArrayList();
        ArrayList<String> decodedBytes = new ArrayList();
        
        String fromServer = in.readLine();
        
        while(!(fromServer = in.readLine()).contains("ENDING_MULTIMEDIA_TRANSMISSION")){
            String datos[] = fromServer.split("#");
            cadenas.add(new String(datos[7]));
        }
        
        System.out.println("Img --> " + fromServer);
        for(String s : cadenas){
            decodedBytes.add(new String(Base64.getDecoder().decode(s.getBytes())));
        }
       
        
        File file = new File("userPhoto.jpg");
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        for (String s : decodedBytes) {
            for (char c : s.toCharArray()) {
                fos.write(c);
            }
        }
        fos.flush();
        fos.close();
    
    }
    
    
    public void getInput(String msg) throws IOException{
        //String msg =   in.readLine().contains("CHAT") ?  in.readLine() : "mal";
       // System.out.println(msg);
       this.processMsg(msg);
    }
    
    
    public void processMsg(String theInput) throws IOException{
        //TODO   
        protocol.processInput(theInput);  
    }
    
    public void refreshFriends() throws IOException{
        String[] friends = this.a.getFriends();
        int contadorF = 0;
        
        for(String friend : friends){
            contadorF++;
        }
        
        String[] refreshFriends = new String[contadorF];
        int contador2 = 0;
        for(String f : friends){
            String loginF  = f.substring(0, f.indexOf(" "));                            //TODO FALTA UN GET LOGIN
            String cadena = "PROTOCOLCRISTOMESSENGER1.0#" + dateTime + "#CLIENT#STATUS#" + login + "#" + loginF;
            out.println(cadena);
            String estado = this.protocol.friendStatus(in.readLine());
            refreshFriends[contador2] = loginF + " " + estado;
            contador2++;
        }
        
        this.a.setFriendsOf(refreshFriends);
    }
    
    
    public void sendMessage(String text) throws IOException{
        String output = protocol.sendMessage(text);
        out.println(output);
        //String fromServer = in.readLine();
        //System.out.println("");
    }
      
    public void getFriendStatus() throws IOException{
        String output = protocol.getFriendStatus();
        out.println(output);
        String fromServer = in.readLine();
        System.out.println("PERO MIRA QUE ESTADOOOOO " + fromServer);
        protocol.processInput(fromServer);
    }
    
    public void getFriendData() throws IOException{
        String output = protocol.getFriendData();
        out.println(output);
        String fromServer = in.readLine();
        protocol.processInput(fromServer);
    }
    
    public void getMessagesFrom() throws IOException{
        
        this.numeroMsgs = 0;
        this.totalNumeroMensajes = 0;
        
        String output =  protocol.getMessages();
        out.println(output);
             
    }
}
