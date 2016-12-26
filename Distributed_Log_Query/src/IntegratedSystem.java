package system;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.io.FileOutputStream;

/* this is the main class of the system */
public class IntegratedSystem {
    /* node list that represents the system message of each VM */
	public static ArrayList<Node> NodeList = new ArrayList<Node>();
	/* system number of this VM */
	private static int SystemNum;
	/* grep results from each machine */
	public static int [] Results = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

	IntegratedSystem(){

	}
	
	/* read a txt file to init the system message */
	void init(){
		// this path is for system data
		String systemPath = "system.txt";
		String s = null;
		String [] info = null;
		String ip = getLocalHostIP();
		try {
			@SuppressWarnings("resource")
			BufferedReader input = new BufferedReader(new FileReader(systemPath)); 
			while((s = input.readLine()) != null){
				info = s.split(" ");
				Node node = new Node(Integer.parseInt(info[0]), info[1], Integer.parseInt(info[2]), info[3]);
				IntegratedSystem.NodeList.add(node);
				if(ip.equals(info[1])){
					SystemNum = Integer.parseInt(info[0]);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
    
   private int count(){
	   int res = 0;
	   for(int i = 0; i < NodeList.size(); i++){
		   if(Results[i] != -1){
			   res = res + Results[i];
			   Results[i] = -1;
		   }
	   }
	   //System.out.println(res);
	   return res;
   }
    
   /* this function deals with local grep */
   public static void selfSearch(String pattern){
	   FileOutputStream fileOuts;
	   Runtime runtime = Runtime.getRuntime();
	   String line = null;
	   int num = 0;
	   String [] c = pattern.split(" ");
	   String [] cmd = {"grep","-c", c[c.length - 1],NodeList.get(SystemNum - 1).getPath()};
	   String [] cmd1 = (pattern + " " + NodeList.get(SystemNum - 1).getPath()).split(" ");
	   try {
		   Process p = runtime.exec(cmd);
	       BufferedInputStream in = new BufferedInputStream(p.getInputStream());
	       BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
           line = inBr.readLine();
           num = Integer.parseInt(line);
           Results[SystemNum - 1] = num;
	       if(p.waitFor() != 0){
	           if(p.exitValue() != 0){
	               if(p.exitValue() == 1){ 
	                   //System.err.println("self execute failed");
	               }
	           }
	       }
	       inBr.close();
           in.close();
           
           //System.out.println("try to grep local file");
		   fileOuts = new FileOutputStream("../log" + SystemNum + ".log");
		   Process p1 = runtime.exec(cmd1);
		   in = new BufferedInputStream(p1.getInputStream());
		   inBr = new BufferedReader(new InputStreamReader(in));
		   
		   while((line = inBr.readLine()) != null){
	           fileOuts.write((line).getBytes());
	       }
	       if(p.waitFor() != 0){
	           if(p.exitValue() != 0){
	               if(p.exitValue() == 1){
	                   // System.err.println("self execute failed");
	               }
	           }
	       }
	       inBr.close();
           in.close();
		} catch (IOException e) {
			e.printStackTrace();
			Results[SystemNum - 1] = -1;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	   
   }
   
   /* 2016 9 10 check whether the input is correct */
   private static boolean check(String pattern){
       String [] cmd = pattern.split(" ");
       int length = cmd.length;
       if(length < 2 || length > 3)
           return false;
       if(cmd[0].equals("grep"))
           return true;
       else 
           return false;
   }

    /* main function */
	@SuppressWarnings("static-access")   // NodeList should be visited in a static way
	public static void main(String[] args){
		int count = 0; 
		IntegratedSystem ItSystem = new IntegratedSystem();
		ItSystem.init();
		String pattern = null;
        Scanner in = new Scanner(System.in);
		Listen listener = new Listen(ItSystem.NodeList.get(ItSystem.SystemNum - 1).getPort(), ItSystem.NodeList.get(ItSystem.SystemNum - 1).getPath());
		Thread s1 = new Thread(listener);
		LinkedList<Thread> list = new LinkedList<Thread>(); 
		s1.start();
		while(true){
			System.out.println("If you need log, please input your command");
            pattern = in.nextLine();
            if(!check(pattern)){
                System.out.println("invalid command !");
                continue;
            }
			System.out.println("Start to grep file");
			count = 0;
			list.clear();
			for(int i = 0; i < ItSystem.NodeList.size(); i++){
				if(ItSystem.NodeList.get(i).getNodeId() == ItSystem.SystemNum ){
					ItSystem.selfSearch(pattern);
					continue;
				}
				else if(ItSystem.NodeList.get(i).isAlive() == false){
					continue;
				}
				Request request = new Request(ItSystem.NodeList.get(i), ItSystem.Results, ItSystem.NodeList.get(i).getNodeId(), pattern);
				Thread s2 = new Thread(request);
				s2.start();
				list.add(s2);
				count += 1;
			}
			/* wait until all the request threads finish */
			for(int i = 0; i < count; i++){
				try {
					list.get(i).join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
				
			for(int i = 0; i < ItSystem.NodeList.size(); i++){
				if(ItSystem.Results[i] != -1){
					System.out.println("machine " + String.valueOf(ItSystem.NodeList.get(i).getNodeId()) + " have " + String.valueOf(ItSystem.Results[i]) + " related logs");
				}
				else{
					System.out.println("machine " + String.valueOf(ItSystem.NodeList.get(i).getNodeId()) + " failed");
					ItSystem.NodeList.get(i).alive = false;
				}
			}
			System.out.println("We have " + String.valueOf(ItSystem.count()) + " related logs in total");
		}
	}
}
