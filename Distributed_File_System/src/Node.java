package system;

import java.util.Date;

/**
 * Created by ghe10 on 10/14/16.
 */
public class Node {
    public String Ip;
    public int Port;
    public String Path;
    public int UDPPort;
    public boolean alive; // if a node failed , this will be set to false
    public long bornTime;
    public int count; // there might be an overflow of it

    public Node(){
        alive = true;
    }

    public Node( String ip, int port, String path){
        Ip = ip;
        Port = port;
        UDPPort = 20000;
        Path = path;
        alive = true;
        count = 0;
        bornTime = new Date().getTime();
    }

    public Node(String ip, int port, long time_join, String path){
        Ip = ip;
        Port = port;
        UDPPort = 20000;
        Path = path;
        alive = true;
        count = 0;
        bornTime = time_join;
    }

}
