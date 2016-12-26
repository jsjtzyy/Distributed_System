package system;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/* this class stands for detecting grep requests from other VMs */
public class Listen implements Runnable{
	public int Port;
	private String Path;
	
	public Listen(int port, String path){
		this.Port = port;
		this.Path = path;
	}

	public void run(){
		int mode;
		String pattern = null;
		try{
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(Port);
			/* here we keep listening, when receive a request, start a new thread to answer */
			while(true){
				System.out.println("listening at " + Port);
				Socket request = serverSocket.accept();                
				DataInputStream s = new DataInputStream(request.getInputStream());
				mode = Integer.parseInt(s.readUTF());
				pattern = s.readUTF(); 
				/* new answer thread starts here */
                System.out.println("we have a grep request " + pattern);
				StringSender strSender = new StringSender(request, Path, pattern);
				Thread t = new Thread(strSender);
				t.start();
			}
		}catch(Exception e){
			System.out.println("Exception from Listen " + e.getMessage());
		}	
	}
}
