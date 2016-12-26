package system;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by ghe10 on 10/14/16.
 */
public class IntegratedSystem extends tools{
    public static Hashtable<String, Node> NodeTable = new Hashtable<String, Node>();
    public static Hashtable<String, ActionNode> ActionTable = new Hashtable<String, ActionNode>();
    public static Hashtable<String, Node> DeadTable = new Hashtable<String, Node>();
    public static int [] Results = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    public static FileSystem fileSystem = new FileSystem(NodeTable);
    private static Node Introducer;
    private int pingSendingPort = 20000;   /** ping sending, ack receiving **/
    private int ackSendingPort = 25000;  /** ack sending, ping receiving **/

    IntegratedSystem(){
        super(NodeTable, ActionTable, DeadTable, fileSystem);
        //fileSystem = new FileSystem(SelfIp, NodeTable); // the definition of this system variable is in tools
        Introducer = new Node("172.22.148.194", 40000, "unknown");//10.0.0.205
        join();
    }

    void join(){
        String result = "";
        byte [] buf;
        long tmp = 100;
        if(NodeTable.containsKey(SelfIp)){
            System.out.println("we don't need to join");
            return;
        }
        else{
            //Node myself = new Node(SelfIp, 40000, "log.log");
            //addNode(SelfIp, myself);
            updateNodeTable(SelfIp, 40000, new Date().getTime());
            try{
                /** add local message to result and send it out **/
                if(!SelfIp.equals(Introducer.Ip)){
                    result = result + Introducer.Ip + " " + Introducer.Port + " " + tmp + " " + "0" + " " + "1" + " ";
                    result = result + NodeTable.get(SelfIp).Ip + " "
                            + NodeTable.get(SelfIp).Port + " " + NodeTable.get(SelfIp).bornTime
                            + " " + "2"+ " " + "0" + " ";
                    System.out.println(result);
                    buf = result.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(buf, buf.length,
                            InetAddress.getByName(Introducer.Ip), ackSendingPort);
                    DatagramSocket initSocket = new DatagramSocket();
                    initSocket.send(sendPacket);
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean check(String pattern){
        String [] cmd = pattern.split(" ");
        int length = cmd.length;
        if(length < 2 || length > 3)
            return false;
        if(cmd[0].equals("grep"))
            return true;
        else
            return false;
    }

    private int count(int size){
        int res = 0;
        for(int i = 0; i < size; i++){
            if(Results[i] != -1){
                res = res + Results[i];
                Results[i] = -1;
            }
        }
        //System.out.println(res);
        return res;
    }

    public static void main(String[] args){

        int RequestCount = 0;
        int t_out = 100;  /** time out for failure detection **/
        IntegratedSystem ItSystem = new IntegratedSystem();
        //System.out.print(ItSystem.getLocalHostIP());
        String pattern = null;
        Scanner in = new Scanner(System.in);
        Listen listener = new Listen(ItSystem.NodeTable.get(ItSystem.SelfIp).Port);
        PingSender sender = new PingSender(ItSystem.NodeTable, ItSystem.ActionTable, t_out, Introducer, ItSystem.DeadTable, ItSystem.fileSystem);
        PingReceiver receiver = new PingReceiver(t_out, ItSystem.NodeTable, ItSystem.ActionTable, ItSystem.DeadTable, ItSystem.fileSystem); // 0 is loss prob
        LinkedList<Thread> list = new LinkedList<Thread>();
        Thread s1 = new Thread(listener);
        Thread sReceiver = new Thread(receiver);
        s1.start();
        sReceiver.start();
        Timer timer = new Timer();
        timer.schedule(sender, 500, 500);
        //ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
        //service.scheduleAtFixedRate(sender, 0, 1000, TimeUnit.MILLISECONDS);
        while(true){
            System.out.println("If you need log, please input your command");
            pattern = in.nextLine();
            if(pattern.equals("list")){
                Enumeration<Node> elements = NodeTable.elements();
                while(elements.hasMoreElements()) {
                    Node targetNode = elements.nextElement();
                    System.out.println(targetNode.Ip + " " + targetNode.bornTime + " ");
                }
                Enumeration<ActionNode> elements1 = ActionTable.elements();
                while(elements.hasMoreElements()) {
                    ActionNode targetNode = elements1.nextElement();
                    System.out.println(targetNode.IP + " " + targetNode.actionTime + " " + ItSystem.actions[targetNode.Action]);
                }
                continue;
            }
            else if(pattern.equals("join")){
                ItSystem.join();
                receiver.suicide = false;
                sender.suicide = false;
                continue;
            }
            else if(pattern.equals("leave")){
                sender.suicide = true;
                receiver.suicide = true;
                continue;
            }
            else if(pattern.equals("store")){
                ItSystem.fileSystem.printLocalFile();
                continue;
            }
            else if(pattern.equals("selfId")){
                System.out.println(ItSystem.SelfIp + " " +ItSystem.NodeTable.get(ItSystem.SelfIp).bornTime);
                continue;
            }
            else{
                String [] commands = pattern.split(" ");
                if(commands.length == 2){
                    if(commands[0].equals("put")){
                        ItSystem.fileSystem.putFile(commands[1], commands[1]);
                        continue;
                    }
                    else if(commands[0].equals("delete")){
                        ItSystem.fileSystem.deleteFile(commands[1]);
                        continue;
                    }
                    else if(commands[0].equals("request")){
                        ItSystem.fileSystem.request(commands[1], commands[1]);
                        continue;
                    }
                    else if(commands[0].equals("ls")){
                        ItSystem.fileSystem.ls(commands[1]);
                        continue;
                    }
                }
                else if(commands.length == 3){
                    if(commands[0].equals("put")){
                        ItSystem.fileSystem.putFile(commands[1], commands[2]);
                        continue;
                    }
                    else if(commands[0].equals("request")){
                        ItSystem.fileSystem.request(commands[1], commands[2]);
                    }
                }
            }
            if(!ItSystem.check(pattern)){
                System.out.println("invalid command !");
                continue;
            }
            System.out.println("Start to grep file");
            list.clear();
            Enumeration<Node> elements = NodeTable.elements();
            RequestCount = 0;
            while(elements.hasMoreElements()) {
                Node targetNode = elements.nextElement();
                if(targetNode.Ip.equals(ItSystem.SelfIp)){
                    System.out.println("self search");
                    ItSystem.selfSearch(pattern);
                    ItSystem.Results[RequestCount] = ItSystem.selfCount(pattern);
                    RequestCount++;
                    continue;
                }
                else{
                    RequestCount++;
                    Request request = new Request(targetNode, ItSystem.Results, RequestCount, pattern);
                    Thread s2 = new Thread(request);
                    s2.start();
                    list.add(s2);
                }
            }
			/* wait until all the request threads finish */
            for(int i = 0; i < RequestCount - 1; i++){
                try {
                    list.get(i).join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("We have " + String.valueOf(ItSystem.count(RequestCount)) + " related logs in total");
        }
    }

}
