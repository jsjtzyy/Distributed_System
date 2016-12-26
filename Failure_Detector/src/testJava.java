package system;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by ghe10 on 9/26/16.
 */


public class testJava {
    public static String getLocalHostIP() {
        String ip;
        try {
             /* return local host */
            InetAddress addr = InetAddress.getLocalHost();
             /* return a string which represents ip */
            ip = addr.getHostAddress();
        } catch(Exception ex) {
            System.out.println("We can't find our own ip....");
            ip = "";
        }
        return ip;
    }

    public static void main(String args[]){

        String s = "you are a string " + true;
        System.out.println(s);
        byte [] buf = new byte[1000];
        buf = s.getBytes();
        /*System.out.println(buf);
        s = new String(buf);
        System.out.println(s);
        String ip = getLocalHostIP();
*/
        try {
            InetAddress address = InetAddress.getByName("127.0.0.250");
            System.out.println(address);
            DatagramSocket sendSocket = new DatagramSocket();
            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address , 30000);
            sendSocket.send(sendPacket);
        }catch(SocketException e){
            System.out.println(e);
        }catch (IOException e1){
            System.out.println(e1);
        }
    }
}
