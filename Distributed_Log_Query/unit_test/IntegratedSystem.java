package system;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

public class IntegratedSystem {
	public static ArrayList<Node> NodeList = new ArrayList<Node>();
	private static int SystemNum;
	public static int [] Results = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
	
	// read a txt file to init the system message
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
    
   private int count(){
	   int res = 0;
	   for(int i = 0; i < NodeList.size(); i++){
		   if(i != SystemNum - 1 && Results[i] != -1){
			   res = res + Results[i];
			   Results[i] = -1;
		   }
	   }
	   System.out.println("count func :" + res);
	   return res;
   }
    
   public static void selfSearch(String pattern){
	   Runtime runtime = Runtime.getRuntime();
	   String [] tmp = pattern.split(" ");
	   Results[SystemNum - 1] = 0;
	   for(int i = 1; i <= 7; ++i){
	        if(NodeList.get(i - 1).isAlive() == false) continue;
	        String [] cmd = {"grep","-c", tmp[tmp.length - 1], NodeList.get(i - 1).getPath()};
	        int count = 0;
	   try{
	        Process p = runtime.exec(cmd);
	        BufferedInputStream in = new BufferedInputStream(p.getInputStream());
	        BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
	        String line = null;
	        while((line = inBr.readLine()) != null){
	           count = Integer.parseInt(line);
	        }
	       if(p.waitFor() != 0){
	           if(p.exitValue() != 0){
	               if(p.exitValue() == 1) System.err.println("self execute failed");
			   }
	       }
	       inBr.close();
           in.close();
           Results[SystemNum - 1] += count;
       }catch (Exception e){
	       e.printStackTrace();
	   }
	   }
   }
   
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

	@SuppressWarnings("static-access")   // NodeList should be visited in a static way
	public static void main(String[] args){
		int numIn = -1;
		int count = 0;  // this count the request threads
		int expectedNum = 0, receivedNum = 0;
		IntegratedSystem ItSystem = new IntegratedSystem();
		ItSystem.init();
		String input = null;
		String pattern = null;
		LinkedList<Thread> list = new LinkedList<Thread>(); 

		File file = new File("pattern.txt");
		BufferedReader reader = null;
		try {
			FileWriter outputFile = new FileWriter("TestResult.txt");
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
            long startTime=System.currentTimeMillis();          //getting start time
            while((tempString = reader.readLine()) != null){
				pattern = "grep " + tempString;
				System.out.println("Start to grep file");
				// this for will be changed to a loop across nodes
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
				for(int i = 0; i < count; i++){  // wait for all the thread completed
					try {
						list.get(i).join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				for(int i = 0; i < ItSystem.NodeList.size(); i++){
					if(ItSystem.Results[i] == -1){
						System.out.println("machine " + String.valueOf(ItSystem.NodeList.get(i).getNodeId()) + " failed");
						ItSystem.NodeList.get(i).alive = false;
					}
				}
				expectedNum = ItSystem.Results[SystemNum - 1];
				receivedNum = ItSystem.count();
				if(expectedNum == receivedNum){
					outputFile.write(tempString + " expected: " + expectedNum + ";  received:" + receivedNum + ";  True\n");
				}else{
					outputFile.write(tempString + " expected: " + expectedNum + ";  received:" + receivedNum + ";  False\n");
				}
                outputFile.flush();
			}// end of while
			outputFile.close();
            long endTime=System.currentTimeMillis();                //getting stop time
            
            System.out.println("The unit test has completed! Please refer to TestResult.text");
            System.out.println("Total running time： "+(endTime-startTime)+"ms");
        } catch (IOException e) {
			e.printStackTrace();
		}finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}

	}
}
