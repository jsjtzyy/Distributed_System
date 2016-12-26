package system;

import java.util.Comparator;
import java.util.Date;

/* this class defines the data structure for a node and relative messages */
public class Node {
	private int NodeId;
	private String Ip;
	private int Port;
    private String Path;
    private int UDPPort;
   // public boolean join;
    public boolean alive; // if a node failed , this will be set to false
    //public boolean isleave;
    public long bornTime;
    public int count; // there might be an overflow of it
    
    public Node(){
    	alive = true;
    }
    
    public Node(int nodeId, String ip, int port, String path){
    	NodeId = nodeId;
    	Ip = ip;
    	Port = port;
        UDPPort = 20000;
    	Path = path;
        //join = true;
    	alive = true;
        //isleave = false; // determine whether node is regular leave,  functions when the node is not alive
        count = 0;
        bornTime = new Date().getTime();
    }

    public Node(int nodeId, String ip, int port, long time_join, String path){
        NodeId = nodeId;
        Ip = ip;
        Port = port;
        UDPPort = 20000;
        Path = path;
        //join = true;
        alive = true;
        //isleave = false; // determine whether node is regular leave,  functions when the node is not alive
        count = 0;
        bornTime = time_join;
    }
    public int getUDPPort() {return UDPPort;}

    public int getNodeId(){
    	return NodeId;
    }
    
    public String getIp(){
    	return Ip;
    }
    
    public int getPort(){
    	return Port;
    }
    
    public String getPath(){
    	return Path;
    }
    
    public boolean isAlive(){
    	return alive;
    }




}
