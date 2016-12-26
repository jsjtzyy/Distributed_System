package system;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by ghe10 on 9/28/16.
 */
public class testJava1 {
    public static void main(String args[]){
        byte [] Buffer = new byte[1024];
        try {
            DatagramSocket receiveSocket = new DatagramSocket(30000);
            DatagramPacket receivePacket = new DatagramPacket(Buffer, Buffer.length);
            receiveSocket.receive(receivePacket);
            //receivePacket.
            System.out.println("in PingReceiver, buffer is " + (new String(Buffer)));
        }catch(IOException e){
            System.out.println("sb " + e);
        }
    }
}
