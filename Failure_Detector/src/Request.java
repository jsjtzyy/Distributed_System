package system;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

//import static system.IntegratedSystem.getLocalHostIP;

/* this class sends out grep request and prepares for receiving */  
public class Request implements Runnable{
	private int Port = 12345;
	private int index;
	private String Ip;
	private boolean strMode = false;
	private String pattern;
	private Node node;
	private int []Results;
		

	public Request(Node n, int []res, int requestNum, String p){
		pattern = p;
		strMode = true;
		index = requestNum;
		node  = n;
		Results = res;
		Port = n.getPort();
		Ip = n.getIp();
	}

	/* get local IP */
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

	/* a run function that sends request and receives data */
   public void run(){
	   StringReceiver strReceiver = null;
	   try{		   
		   Socket socket = new Socket(Ip, Port);
		   //SocketAddress address = new InetSocketAddress(Ip, Port);
		   //socket.connect(address, 5000);
		   DataOutputStream s = new DataOutputStream(socket.getOutputStream());
		   if(strMode){
			   s.writeUTF("1");
			   s.writeUTF(pattern);
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
