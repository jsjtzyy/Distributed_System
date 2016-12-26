package system;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.Date;

/* this is the main class of the system */
public class IntegratedSystem {
	/* node list that represents the system message of each VM */
	public static ArrayList<Node> NodeList = new ArrayList<Node>();
	public static ArrayList<Node> DeadList = new ArrayList<Node>();
	public static ArrayList<ActionNode> ActionList = new ArrayList<ActionNode>();
	/* system number of this VM */
	private static int SystemNum = -1;
	/* grep results from each machine */
	public static int [] Results = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
	private static Node Introducer;
	IntegratedSystem(){

	}

	/* read a txt file to init the system message */
	void init(){
		// this path is for system data
		//String systemPath = "systems.txt";
		String s = null;
		String [] info = null;
		String ip = getLocalHostIP();
		Node myself;
		Introducer = new Node(-1, "172.22.148.194", 40000, "unknown");
		join();

	}

	void readFile(){
		String systemPath = "systems.log";
		String s = null;
		String [] info = null;
		String ip = getLocalHostIP();
		FileOutputStream fileClear;
		BufferedReader input;
		synchronized (this) {
			try {
				input = new BufferedReader(new FileReader(systemPath));
				// try to join the system
				//s = input.readLine();
				while ((s = input.readLine()) != null) {
					System.out.println(s);
					info = s.split(" ");
					//int ttt = NodeList.size();
					for(int i = 0; i < 3; i++){
						System.out.println(info[i]);
					}
					Node node = new Node(NodeList.size(), info[0], Integer.parseInt(info[1]), Long.parseLong(info[2]), "unknown");
					if (!ip.equals(info[0])) {
						NodeList.add(node); // myself is added outside this func
						System.out.println(" I have machine with ip " + info[0]);
					}
				}
				// next we clear systems.log
				fileClear = new FileOutputStream(systemPath);
				s = "";
				fileClear.write(s.getBytes());
			} catch (FileNotFoundException e) {
				//e.printStackTrace();
				System.out.println("We don't have system file now");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	void join(){
		String ip = getLocalHostIP();
		String result = "";
		byte [] buf;
		long tmp = 100;
		boolean exist = false;
		boolean godExist = false;
		for(int i = 0; i < NodeList.size(); i++){
			if (NodeList.get(i).getIp().equals(ip)){
				exist = true;
				//break;
			}
			else if(NodeList.get(i).getIp().equals(Introducer.getIp())){
				System.out.println("we don't need to join");
				return;
			}
		}
		if(!exist){
			Node myself = new Node(1, ip, 40000, "log.log");
			synchronized(this) {
				NodeList.add(myself);
			}
			if(ip.equals(Introducer.getIp())){
				readFile();
			}
			SystemNum = 1;
		}
		try{
			/** add local message to result and send it out **/
			if(!ip.equals(Introducer.getIp())){
				result = result + Introducer.getIp() + " " + Introducer.getPort() + " " + tmp + " " + "0" + " " + "1" + " ";
				result = result + NodeList.get(SystemNum - 1).getIp() + " "
						+ NodeList.get(SystemNum - 1).getPort() + " " + NodeList.get(SystemNum - 1).bornTime
						+ " " + 2 + " ";
				System.out.println(result);
				buf = result.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(buf, buf.length,
						InetAddress.getByName(Introducer.getIp()), 25000);
				DatagramSocket initSocket = new DatagramSocket();
				initSocket.send(sendPacket);
			}
		}catch (IOException e) {
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

	public void writeLog(int action, Node n){
		FileOutputStream fileOuts;
		String ip = getLocalHostIP();
		//synchronized (this) {
		try{
			fileOuts = new FileOutputStream("log.log", true); // true represents write
			String out = "";
				/*for(int i = 0; i < NodeList.size(); i++){
					//if(NodeList.get(i).getIp().equals(ip))
					//	continue;
					String out = "";
					out = out + new Date().getTime() + " " + NodeList.get(i).getIp() + " " + NodeList.get(i).bornTime + "\n";
					fileOuts.write(out.getBytes());
				}*/
			out += new Date().getTime();
			if(action == 2) out += " add ";
			else if(action == 0) out += " leave ";
			else if(action < 0) out += " update ";
			else out += " fail ";
			out = out + n.getIp() +" " + n.bornTime + "\n";
			fileOuts.write(out.getBytes());

		}catch(FileNotFoundException e){
			System.out.println("Itsystem can't find file to write " + e);
		}catch (IOException e1){
			System.out.println("Itsystem can't write due to IOexception " + e1);
		}
		//}
	}

	/* main function */
	@SuppressWarnings("static-access")   // NodeList should be visited in a static way
	public static void main(String[] args){
		int count = 0;
		int t_out = 100;  /** time out for failure detection **/
		int receivePort = 20000; /** port to receive UDP packet **/
		IntegratedSystem ItSystem = new IntegratedSystem();
		ItSystem.init();
		String pattern = null;
		Scanner in = new Scanner(System.in);
		Listen listener = new Listen(ItSystem.NodeList.get(ItSystem.SystemNum - 1).getPort(), ItSystem.NodeList.get(ItSystem.SystemNum - 1).getPath());
		PingSender sender = new PingSender(ItSystem.NodeList, ItSystem.ActionList, ItSystem.SystemNum - 1, t_out, Introducer, ItSystem.DeadList);
		PingReceiver receiver = new PingReceiver(ItSystem.NodeList, ItSystem.ActionList, t_out, ItSystem.SystemNum - 1, 0, ItSystem.DeadList); // 0 is loss prob
		LinkedList<Thread> list = new LinkedList<Thread>();
		Thread s1 = new Thread(listener);
		Thread sReceiver = new Thread(receiver);
		s1.start();
		sReceiver.start();
		Timer timer = new Timer();
		timer.schedule(sender, 500, 500);

		while(true){
			System.out.println("If you need log, please input your command");
			pattern = in.nextLine();
			if(pattern.equals("list")){
				for(int i = 0; i < NodeList.size(); i++){
					System.out.println("Node " + ItSystem.NodeList.get(i).getNodeId() + " is " + ItSystem.NodeList.get(i).alive + " " + ItSystem.NodeList.get(i).getIp()
							+ " " + ItSystem.NodeList.get(i).bornTime);
				}
				/*for(int i = 0; i < ActionList.size(); i++){
					System.out.println("action " + i + " is " + ItSystem.ActionList.get(i).IP + " " + ItSystem.ActionList.get(i).actionTime
							+ " " + ItSystem.ActionList.get(i).findTime + " " + "count = " + ItSystem.ActionList.get(i).count + " "
							+  ItSystem.ActionList.get(i).Action + " action : 0 leave ; 1 fail ; 2 join ");
				}*/
				continue;
			}
			else if(pattern.equals("leave")){
				sender.suicide = true;
				receiver.suicide = true;
				ItSystem.writeLog(0, NodeList.get(ItSystem.SystemNum - 1));
				System.out.println("leave success");
				continue;
			}
			else if(pattern.equals("join")){
				ItSystem.join();
				receiver.suicide = false;
				sender.suicide = false;
				System.out.println("try to join system");
				continue;
			}
			else if(pattern.equals("prob")){
				pattern = in.nextLine();
				float tmp = Float.valueOf(pattern);
				sender.lost_prob = tmp;
				receiver.lost_prob = tmp;
				System.out.println("we set lost prob to " + tmp);
				continue;
			}
			else if(pattern.equals("selfid")){
				System.out.println(NodeList.get(ItSystem.SystemNum - 1).getIp() + " " + NodeList.get(ItSystem.SystemNum - 1).bornTime);
			    continue;
			}
			if(!check(pattern)){
				System.out.println("invalid command !");
				continue;
			}
			System.out.println("Start to grep file");
			count = 0;
			list.clear();
			for(int i = 0; i < ItSystem.NodeList.size(); i++){
				System.out.println("id is " + (i+1));
				if(ItSystem.NodeList.get(i).getIp().equals(ItSystem.NodeList.get(SystemNum - 1).getIp())){
					System.out.println("self search");
					ItSystem.selfSearch(pattern);
					continue;
				}
				else if(ItSystem.NodeList.get(i).isAlive() == false){
					//System.out.println("we have a failed machine in grep: " +  (i + 1));
					//continue;
				}
				System.out.println("send grep to machine: " +  (i + 1));
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
					//ItSystem.NodeList.get(i).alive = false;
				}
			}
			System.out.println("We have " + String.valueOf(ItSystem.count()) + " related logs in total");
		}
	}
}
