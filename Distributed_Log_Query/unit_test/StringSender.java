package system;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// this class is for string transmission
public class StringSender implements Runnable{
	private Socket socket;
	private String filePath;
	private String pattern;
	
	public StringSender(Socket s, String path, String p){
		socket = s;
		filePath = path;
		pattern = p;
	}
	
	// this function is for UNIX system only, since Runtime.getRuntime() returns windows in windows system
	public void run(){
	    int count = 0;
		Runtime runtime = Runtime.getRuntime();
		//System.out.println("Start to send strings");
		pattern = pattern + " " + filePath;
		//System.out.println(filePath);
		String [] cmd = pattern.split(" ");
		try{
			Process p = runtime.exec(cmd);
	        BufferedInputStream in = new BufferedInputStream(p.getInputStream());
	        BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
	        String line = null;
	        try {
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());	
				System.out.println("string sender build DataOutputStream success");
		        while((line = inBr.readLine()) != null){
		        	out.writeUTF(line);
		        	count = count + 1;
		        }
		        System.out.println("send " + Integer.toString(count) + " lines");
		        // here we use an empty string as file end
		        out.writeUTF("");
		        out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	        if(p.waitFor() != 0){
	           if(p.exitValue() != 0){
	               if(p.exitValue() == 1) System.err.println("execute failed");
	               }
	        }
	        inBr.close();
	        in.close();	
		}catch (Exception e){
	        e.printStackTrace();
		}
	}

	
	
	public static void main(String args[]){
		
	    Runtime runtime = Runtime.getRuntime();
	    String [] cmd = {"grep" ,"INFO", "D:/DS_project/DebugTest1.log"};

	    try{
	        Process p = runtime.exec(cmd);
	        BufferedInputStream in = new BufferedInputStream(p.getInputStream());
	        BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
	        String line = null;
	        while((line = inBr.readLine()) != null){
	            System.out.println(line);
	        }
	        if(p.waitFor() != 0){
	           if(p.exitValue() != 0){
	               if(p.exitValue() == 1) System.err.println("execute failed");
	               }
	        }
	        inBr.close();
	        in.close();
	    }catch (Exception e){
	        e.printStackTrace();
	    }
	}
}
