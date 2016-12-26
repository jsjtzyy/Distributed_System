package system;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;


public class Request implements Runnable{
	private int Port = 12345;
	private int index;
	private String Ip;
	private boolean strMode = false;
	private String pattern;
	private Node node;
	private int []Results;
		
	/***** updated  2016 9 8 *****/
	public Request(Node n, int []res, int requestNum, String p){
		pattern = p;
		strMode = true;
		index = requestNum;
		node  = n;
		Results = res;
		Port = n.getPort();
		Ip = n.getIp();
	}
	
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
    
   //public static void request(String ip) throws IOException{
   public void run(){
	   StringReceiver strReceiver = null;
	   //System.out.println("Start...\n");
	   //System.out.println("Try to connect to" + Ip + " string mode is " + strMode);
	   //System.out.println(Thread.currentThread().getId());  // print thread id
	   try{		   
		   Socket socket = new Socket();
		   SocketAddress address = new InetSocketAddress(Ip, Port);
		   socket.connect(address, 2000);
		   DataOutputStream s = new DataOutputStream(socket.getOutputStream());
		   if(strMode){
			   //System.out.println("request string");
			   s.writeUTF("1");
			   s.writeUTF(pattern);
			   //System.out.println("prepare to receive");
			   strReceiver = new StringReceiver(socket, node, Results, index);
			   strReceiver.receive();
		   }
	   }catch(UnknownHostException ex1){
		   System.err.println(ex1);
	   }catch(IOException ex2){
		   System.err.println(ex2);
	   }
	   
   }
}
