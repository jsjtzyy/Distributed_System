package system;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by ghe10 on 10/14/16.
 */
public class Request implements Runnable{
    private int Port;
    private int index;
    private String Ip;
    private String pattern;
    private Node node;
    private int []Results;


    public Request(Node n, int []res, int requestNum, String p){
        pattern = p;
        index = requestNum;
        node  = n;
        Results = res;
        Port = n.Port;
        Ip = n.Ip;
    }

    /* a run function that sends request and receives data */
    public void run(){
        StringReceiver strReceiver = null;
        try{
            Socket socket = new Socket(Ip, Port);
            DataOutputStream s = new DataOutputStream(socket.getOutputStream());
            s.writeUTF(pattern);
            strReceiver = new StringReceiver(socket, node, Results, index);
            strReceiver.receive();
        }catch(UnknownHostException ex1){
            System.err.println(ex1);
        }catch(IOException ex2){
            System.err.println(ex2);
        }
    }

}
