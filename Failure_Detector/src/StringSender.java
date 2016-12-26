package system;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.FileSystemException;

import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* this class is for greped data transmission */
public class StringSender implements Runnable{
	private Socket socket;
	private static String filePath;
	private String pattern;
	private int BufferSize = 4096;
	
	public StringSender(Socket s, String path, String p){
		socket = s;
		filePath = path;
		pattern = p;
	}
	
	/* search and find the required data from local vm.log */
	public static void selfSearch(String pattern){
	    Runtime runtime = Runtime.getRuntime();
		pattern = pattern + " " + filePath;
		String [] cmd = pattern.split(" ");
		FileOutputStream fileOuts;
		try{
		    Process p = runtime.exec(cmd);
		    BufferedInputStream in = new BufferedInputStream(p.getInputStream());
		    BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
		    fileOuts = new FileOutputStream("sendLog.log");
		    String line = null;
		    while((line = inBr.readLine()) != null){
		       fileOuts.write((line).getBytes());
		    }
		    if(p.waitFor() != 0){
		       if(p.exitValue() != 0){
		          if(p.exitValue() == 1){ 
		              //System.err.println("self execute failed");
		          }
		       }
		    }
		    inBr.close();
	        in.close();
	   }catch (Exception e){
		  e.printStackTrace();
	   }
	}
	
	/* send the greped results */
	public void send(int num){
		int length = 0, l = 0;
		DataOutputStream fouts = null;
		FileInputStream fins = null;
		File file = new File("sendLog.log");
		byte[] sendByte = new byte[BufferSize];
		
		try {
			fins = new FileInputStream(file);
		    fouts = new DataOutputStream(socket.getOutputStream()); 
		    fouts.writeInt(num);
		    while((length = fins.read(sendByte, 0, sendByte.length)) > 0){
		    	fouts.write(sendByte, 0, length);
				l += 1;
		    }
		    System.out.println("send bytes" + l);
		} catch (FileSystemException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("DataStream creation failed");
			e.printStackTrace();
		} finally{
		if(fins != null)
			try {
				fins.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		if(fouts != null)
			try {
				fouts.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		if(socket != null)
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/* thread run function */
	public void run(){
	   // int count = 0;
	   System.out.println("stringSender starts");
		Runtime runtime = Runtime.getRuntime();
	    String line = null;
	    int num = 0;
	    String [] c = pattern.split(" ");
	    String [] cmd = {"grep","-c", c[c.length - 1], filePath};
	    selfSearch(pattern);
	    try {
			Process p = runtime.exec(cmd);
	        BufferedInputStream in = new BufferedInputStream(p.getInputStream());
	        BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
            line = inBr.readLine();
            num = Integer.parseInt(line);
            //System.out.println(num);
			send(num);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// this function is directly written by Java regular expressions
	/*public void run(){
		System.out.println("Start to send strings");
		int count = 0;
		File file = new File(filePath);
		BufferedReader reader = null;
		String tmpSource = null;
		StringBuilder source = new StringBuilder();
		String result;
		Pattern p = Pattern.compile(pattern);
		Matcher matcher = null;
		try{
			reader = new BufferedReader(new FileReader(file));
			System.out.println("read file in stringer sender");
			int line = 0;
			while((tmpSource = reader.readLine()) != null){
				source.append(tmpSource);
				line = line + 1;

			}
		}catch (IOException e1) {
			System.out.println("exception in StringSender");
			System.out.println(filePath);
			System.err.print(e1);
		}
		matcher = p.matcher(source.toString());
		System.out.println("start matching");
		try {
			//System.out.println("match and send in string sender");
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			while(matcher.find()){
				result = matcher.group();
				System.out.println(result);
				out.writeUTF(result + "\n");
				count = count + 1;
			}
			// here we use an empty string as transmission end
			out.writeUTF("");
			out.close();
			System.out.println("match and transmission finished, we have " + count + " lines matched and transmitted");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	
    /* this main is only used for self test */	
    /*public static void main(String args[]){
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
	}*/
}
