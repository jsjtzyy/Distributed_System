package system;

import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.Hashtable;
import java.util.Random;

/**
 * Created by ghe10 on 10/14/16.
 */
public class PingSender extends tools implements Runnable{
    DatagramSocket sendSocket;
    private int pingSendingPort = 20000;   /** ping sending, ack receiving **/
    private int ackSendingPort = 25000;  /** ack sending, ping receiving **/
    private byte[] sendBuffer;
    private byte[] ACK;
    private int threshold = 6; /** if we have send this log 10 times, stop sending it **/
    public boolean suicide = false;
    public boolean isWorking = true;
    public double lost_prob = 0;
    private Node Introducer;
    int t_out = 300;

    public PingSender(Hashtable<String, Node> nodeTable, Hashtable<String, ActionNode> actionTable,
                      int t, Node n, Hashtable<String, Node> deadTable, FileSystem f, Scheduler s){
        super(nodeTable, actionTable, deadTable, f,  s);
        sendBuffer = new byte[1024];
        ACK = new byte[1024];
        t_out = t;
        Introducer = new Node("172.22.148.194", 40000, "unknown");
        try {
            sendSocket = new DatagramSocket(pingSendingPort);
            sendSocket.setSoTimeout(t_out);
        } catch (IOException e){
            System.out.println("exception in ping sender" + e);
        }
    }

    private void Suicide(){
        /** leave **/
        NodeTable.clear();
        ActionTable.clear();
        DeadTable.clear();
    }

    public void send(String targetIp, String pingMessage){
        InetAddress address;
        double p = Math.random();
        if( p < lost_prob){
            return;
        }
        //System.out.println(" we send ping to " + targetIp);
        try{
            address = InetAddress.getByName(targetIp);
            sendBuffer = pingMessage.getBytes();
            sendSocket.setSoTimeout(t_out);
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, ackSendingPort);
            //System.out.println("Sender send message to " + address + " " + ackSendingPort);
            sendSocket.send(sendPacket);
        } catch(UnknownHostException e){
            System.out.println("can't find ip of ping target in PingSender " + e);
        } catch(SocketException e1){
            System.out.println(" Set socket timeout failed in PingSender" + e1);
        } catch(IOException e2){
            System.out.println("send round failed exception in PingSender" + e2);
        }
    }


    public void run() {
        //System.out.println("send start at" + new Date().getTime());
        int ping_id = 0;
        String pingMessage = null;
        Node target = null;
        if(suicide){
            if(isWorking == false){
                //System.out.println("this PingSender thread is dead " + new Date().getTime());
                return;
            }
            isWorking = false;
            System.out.println("this PingSender thread will stop");
            Suicide();
            return;
        }
        else {
            isWorking = true;
        }
        target = getPingTarget();
        if(target == null ){
            if(SelfIp != Introducer.Ip) {
                target = Introducer;
            }
            else {
                //System.out.println("we have no peer to ping");
                return;
            }
        }
        pingMessage = buildMessage(target, target.Ip);
        /** wait for ACK **/
        DatagramPacket receivePacket = new DatagramPacket(ACK, ACK.length);
        send(target.Ip, pingMessage);
        //System.out.println("send finish");
        try{
            sendSocket.receive(receivePacket);
            // try to see if we received anything or not
            //System.out.println(" received data, start to read data");
            String receivedAck = new String(ACK);
            updateList(receivedAck.split(" "));  // dead lock or some stop here
        } catch(SocketTimeoutException e){
            updateAction(target.Ip, target.Port, target.bornTime, 1);
            deleteNode(target.Ip);
        } catch(IOException e){
            System.out.println("receive failed exception in PingSender" + e);
        }
        updateDeadCount();
       // System.out.println("send ends at" + new Date().getTime());
    }
}
