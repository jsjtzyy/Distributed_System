package system;

import java.io.IOException;
import java.net.*;
import java.util.Hashtable;


/**
 * Created by ghe10 on 10/14/16.
 */
/** in ping sender, we need to send ping to others' ping receiving port : ackPort
 * and listen at receivePort
 *
 *  in ping handler, we need to send indirect ping to ???? others' ping receiving port : ackPort
 *  and receive ACK from receivePort, and
 *
 *  in ping handler, we need to send direct ACK to receivePort
 * **/
public class PingHandler extends tools implements Runnable{
    private DatagramPacket receivedPacket;
    private byte[] Buffer;
    private int time_out = 200;
    private int SystemId;

    DatagramSocket ackSocket; // port ackSendingPort = 25000
    DatagramSocket indirectPingSocket;
    private int pingSendingPort = 20000;   /** ping sending, ack receiving **/
    private int ackSendingPort = 25000;  /** ack sending, ping receiving **/
    public double lost_prob = 0;

    public PingHandler(DatagramPacket packet, byte[] buf, int t_out, DatagramSocket s1, double prob,
                       Hashtable<String, Node> nodeTable, Hashtable<String, ActionNode> actionTable,
                       Hashtable<String, Node> deadTable, FileSystem f, Scheduler s){
        super(nodeTable, actionTable, deadTable, f, s);
        Buffer = buf.clone();
        receivedPacket = packet;
        time_out = t_out;
        ackSocket = s1;
        lost_prob = prob;
        try{
            indirectPingSocket = new DatagramSocket();
        }catch(SocketException e){
            System.out.println("PingHandler build socket failed " + e);
        }
    }

    private void sendACK(){
        byte [] ACKBuffer = new byte[1024];
        DatagramPacket ACK;
        String result = "";
        double p = Math.random();
        if( p < lost_prob){
            return;
        }
        /** sth more need to be done for ACK message **/
        String ii = receivedPacket.getAddress().toString();
        //System.out.println("before delete / " + NodeTable.containsKey(ii));
        ii = ii.substring(1);
        //System.out.println("ii = " + ii);
        //System.out.println(NodeTable.containsKey(ii));
        result = buildMessage(NodeTable.get(ii), ii);
        //System.out.println(" our ack is " + result);
        ACKBuffer = result.getBytes();
        try{
            ACK = new DatagramPacket(ACKBuffer, ACKBuffer.length, receivedPacket.getAddress(), receivedPacket.getPort());
            ackSocket.send(ACK);
            //System.out.println("we send ACK mess " + result);
        }catch(SocketException e){
            System.out.println(" PingHandler build new socket for answering direct ping failed");

        }catch(IOException e1){
            System.out.println(" PingHandler send ACK for answering direct ping failed");
        }
    }

    /*private void sendIndirectPing(String []data){ // receivedPacket is the indirect ping packet we receive
        double p = Math.random();
        if( p < lost_prob){
            return;
        }
        byte [] sendBuffer = new byte[1024];
        byte [] receivedACK = new byte[1024];
        DatagramPacket forwardPacket;
        DatagramPacket midPacket;
        DatagramPacket backwardPacket;
        InetAddress targetAddress, requestAddress;
        String send = "";
        updateList(data);
        for(int i = 0; i < data.length; i++){
            send += data[i];
            send += " ";
        }
        try{
            targetAddress = InetAddress.getByName(data[0]);
            sendBuffer = send.getBytes();
            indirectPingSocket.setSoTimeout(time_out);
            forwardPacket = new DatagramPacket(sendBuffer, sendBuffer.length, targetAddress, ackSendingPort);
            indirectPingSocket.send(forwardPacket);
            //receive ACK from indirect ping
            midPacket = new DatagramPacket(receivedACK, receivedACK.length);
            ackSocket.receive(midPacket);
            // update list again *
            updateList(receivedACK.toString().split(" "));
            //send this message back, here we don't change the ACK message
            backwardPacket = new DatagramPacket(receivedACK, receivedACK.length, receivedPacket.getAddress(), receivedPacket.getPort());
            ackSocket.send(backwardPacket);
            System.out.print("indirect ping succeed in PingHandler");

        }catch (SocketTimeoutException e){
            System.out.print("indirect ping time out");

        }catch(UnknownHostException e0){
            System.out.print("PingHandler finds an unknown address  " + data[0] + " " + e0);
        }catch(SocketException e1){
            System.out.print("PingHandler failed to build socket " + e1);
        }catch(IOException e2){
            System.out.print("PingHandler failed with IOException " + e2);
        }
    }*/

    /** here we need to handle the ping message **/
    public void run(){
        //System.out.println("handler start");
        String data[];
        String s = new String(Buffer);
        //System.out.println(s);
        data = s.split(" ");
        //showPing(data);
        //if(SelfIp.equals(data[0])){
            /** direct ping **/
        updateList(data);
        sendACK();
            //System.out.println("direct ping");

            //System.out.println(new String(Buffer));
        /*}
        else{
            System.out.println("indirect ping");
            sendIndirectPing(data);
            updateList(data);
        }*/
        //System.out.println("ping handler end");
    }
}
