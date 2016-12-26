package system;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Hashtable;

/**
 * Created by ghe10 on 10/14/16.
 */
/** in ping sender, we need to send ping to others' ping receiving port : ackPort
 * and listen at receivePort
 *  in ping receiver, we need to receive ping from ackPort
 * **/
public class PingReceiver implements Runnable{
    private int t_out = 100;
    private byte[] Buffer;
    private int pingSendingPort = 20000;   /** ping sending, ack receiving **/
    private int ackSendingPort = 25000;  /** ack sending, ping receiving **/
    public boolean suicide;
    public float lost_prob = 0;
    public Hashtable<String, Node> NodeTable; //<String, Node>
    public Hashtable<String, ActionNode> ActionTable;
    public Hashtable<String, Node> DeadTable;
    public FileSystem fileSystem;

    public PingReceiver(int t, Hashtable<String, Node> nodeTable, Hashtable<String, ActionNode> actionTable,
                        Hashtable<String, Node> deadTable, FileSystem f){
        Buffer = new byte[1024];
        t_out = t;
        suicide = false;
        NodeTable = nodeTable;
        ActionTable = actionTable;
        DeadTable = deadTable;
        fileSystem = f;
    }
    public void run(){
        System.out.println(" start to receive data");
        try{
            DatagramSocket ackSocket = new DatagramSocket(ackSendingPort);
            while(true){
                Buffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(Buffer, Buffer.length);
                ackSocket.receive(receivePacket);
                //System.out.println("we receive a ping from " + receivePacket.getAddress());
                if(suicide == true){
                    continue;
                }
                //String data = new String(Buffer);
                //System.out.println(data);
                PingHandler handler = new PingHandler(receivePacket, Buffer, t_out, ackSocket, lost_prob,
                        NodeTable, ActionTable, DeadTable, fileSystem);
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
