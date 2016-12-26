package system;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

/**
 * Created by ghe10 on 9/25/16.
 * Receive ping and start a ping handler thread
 * this class serves as the door of a system
 */
public class PingReceiver implements Runnable{
    private int SystemId;
    private int t_out = 100;
    private byte[] Buffer;
    public ArrayList<Node> NodeList;
    public ArrayList<Node> DeadList;
    public ArrayList<ActionNode> ActionList;
    /** in ping sender, we need to send ping to others' ping receiving port : ackPort
     * and listen at receivePort
     *  in ping receiver, we need to receive ping from ackPort
     * **/
    private int pingSendingPort = 20000;   /** ping sending, ack receiving **/
    private int ackSendingPort = 25000;  /** ack sending, ping receiving **/
    public boolean suicide;
    public float lost_prob = 0;

    public PingReceiver(ArrayList<Node> list, ArrayList<ActionNode> actionList, int t, int id,
                        float prob, ArrayList<Node> deadList){
        NodeList = list;
        ActionList = actionList;
        Buffer = new byte[1024];
        t_out = t;
        SystemId = id;
        suicide = false;
        lost_prob = prob;
        DeadList = deadList;
    }

    public void run(){
        System.out.println(" start to receive data");
        try{
            DatagramSocket ackSocket = new DatagramSocket(ackSendingPort);
            while(true){
                //Buffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(Buffer, Buffer.length);
                ackSocket.receive(receivePacket);
                if(suicide == true){
                    continue;
                }
                PingHandler handler = new PingHandler(receivePacket, Buffer, t_out, NodeList, ActionList, SystemId, ackSocket, lost_prob, DeadList);
                Thread s = new Thread(handler);
                s.start();
            }
        }catch(SocketException e){
            System.out.println("Exception in PingReceiver" + e);
        }catch(IOException e1){
            System.out.println("Exception in PingReceiver" + e1);
        }
    }
}
