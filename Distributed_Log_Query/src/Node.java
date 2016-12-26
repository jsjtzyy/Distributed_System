package system;
/* this class defines the data structure for a node and relative messages */
public class Node {
	private int NodeId;
	private String Ip;
	private int Port;
    private String Path;
    public boolean alive; // if a node failed , this will be set to false
    
    public Node(){
    	alive = true;
    }
    
    public Node(int nodeId, String ip, int port, String path){
    	NodeId = nodeId;
    	Ip = ip;
    	Port = port;
    	Path = path;
    	alive = true;
    }
    
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
