package system;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;

/**
 * Created by ghe10 on 9/25/16.
 * we need a handler to judge direct/indirect ping
 * if indirect, we need to send another ping to target
 * this is achieved by judge the target ip
 */
public class PingHandler implements Runnable{
    private DatagramPacket receivedPacket;
    private byte[] Buffer;
    private String ip;
    private int time_out = 100;
    public ArrayList<Node> NodeList;
    public ArrayList<Node> DeadList;
    ArrayList<ActionNode> ActionList;
    private int threshold = 6;
    private int removeDeadTime = 31;
    private int SystemId;
    /** in ping sender, we need to send ping to others' ping receiving port : ackPort
     * and listen at receivePort
     *
     *  in ping handler, we need to send indirect ping to ???? others' ping receiving port : ackPort
     *  and receive ACK from receivePort, and
     *
     *  in ping handler, we need to send direct ACK to receivePort
     * **/
    DatagramSocket ackSocket; // port ackSendingPort = 25000
    DatagramSocket indirectPingSocket;
    private int pingSendingPort = 20000;   /** ping sending, ack receiving **/
    private int ackSendingPort = 25000;  /** ack sending, ping receiving **/
    public double lost_prob = 0;

    public PingHandler(DatagramPacket packet, byte[] buf, int t_out,
                       ArrayList<Node> list, ArrayList<ActionNode> actionList, int id, DatagramSocket s1, double prob, ArrayList<Node> deadList){
        Buffer = buf.clone();
        receivedPacket = packet;
        time_out = t_out;
        ip = getLocalHostIP();
        NodeList = list;
        DeadList = deadList;
        ActionList = actionList;
        SystemId = id;
        ackSocket = s1;
        lost_prob = prob;
        try{
            indirectPingSocket = new DatagramSocket();
        }catch(SocketException e){
            System.out.println("PingHandler build socket failed " + e);
        }
    }

    /** the following three functions are used to judge whether sb has just dead **/
    private boolean isJustDead(String ip){
        int length = DeadList.size();
        if(length == 0) return false;
        for(int i = 0; i < DeadList.size(); i++){
            if(DeadList.get(i).getIp().equals(ip)){
                System.out.println("just dead " + DeadList.get(i).count);
                return true;
            }
        }
        return false;
    }

    private void updateDeadCount(){
        synchronized (this){
            for(int i = 0; i < DeadList.size(); i++){
                DeadList.get(i).count += 1;
                if(DeadList.get(i).count == removeDeadTime){
                    DeadList.remove(i);
                }
            }
        }
    }

    private void addDead(Node node){
        synchronized (this){
            for(int i = 0; i < DeadList.size(); i++){
                if(DeadList.get(i).getIp().equals(node.getIp())){
                    return;
                }
            }
            Node dead = new Node(-1, node.getIp(), node.getPort(), "dead");
            DeadList.add(dead);
        }
    }


    void writeMemList(){
        FileOutputStream fileOuts;
        String ip = getLocalHostIP();
        synchronized (this) {
            try{
                fileOuts = new FileOutputStream("systems.log"); // true represents write
                for(int i = 0; i < NodeList.size(); i++){
                    if(NodeList.get(i).getIp().equals(ip))
                        continue;
                    String out = "";
                    out = out + NodeList.get(i).getIp() + " " + NodeList.get(i).getPort() + " " + NodeList.get(i).bornTime + " " + "a" + "\n";
                    fileOuts.write(out.getBytes());
                    fileOuts.flush();
                    //System.out.println("writing...");
                }

            }catch(FileNotFoundException e){
                System.out.println("Itsystem can't find file to write " + e);
            }catch (IOException e1){
                System.out.println("Itsystem can't write due to IOexception " + e1);
            }

        }
    }

    void writeLog(int action, Node n){
        FileOutputStream fileOuts;
        try{
            fileOuts = new FileOutputStream("log.log", true); // true represents write
            String out = "";
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
    }

    private void showLists(){
        for(int i = 0; i < NodeList.size(); i++){
            System.out.println("Node " + i + " is " + NodeList.get(i).alive + " " + NodeList.get(i).getIp()
                    + " " + NodeList.get(i).bornTime);
        }
       /* for(int i = 0; i < ActionList.size(); i++){
            System.out.println("action " + i + " is " + ActionList.get(i).IP + " " + ActionList.get(i).actionTime
                    + " "  + "count = " + ActionList.get(i).count  + " " + ActionList.get(i).Action + " action : 0 leave ; 1 fail ; 2 join ");
        }*/
        //writeLog();
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

    private int findSystemId(){
        int res = 0;
        for(res = 0; res < NodeList.size(); res++){
            if(ip.equals(NodeList.get(res).getIp())){
                return res;
            }
        }
        return 0;
    }

    private int getPayloadId(int max){
        Random random = new Random();
        int num = -1;
        while(num < 0){
            num = random.nextInt(max);
        }
        return num;
    }

    private int getMinCount(){
        int index = 0, min_Count = 10000;
        if(ActionList.size() <= 1) return 0;
        for(int i = 0; i < ActionList.size(); i++){
            if(ActionList.get(i).count < min_Count){
                min_Count = ActionList.get(i).count;
                index = i;
            }
        }
        return index;
    }
    /**   num_of_payload pay_load pay_load
     * pay_load: IP isalive isleave**/
    private String buildACKMessage(){
        int SystemId = findSystemId();
        String result = "";
        result = result +  NodeList.get(SystemId).getIp() + " "
                + NodeList.get(SystemId).getPort() + " " + NodeList.get(SystemId).bornTime + " ";
        int i = 0;
        /** message from NodeList **/
        if(NodeList.size() == 0){
            result = result + "0" + " ";
        }
        else {
            int payLoad1 = getPayloadId(NodeList.size());
            result = result + "1" + " ";
            result = result + NodeList.get(payLoad1).getIp() + " "
                    + NodeList.get(payLoad1).getPort() + " " + NodeList.get(payLoad1).bornTime + " ";
            synchronized (this) {
                NodeList.get(payLoad1).count += 1;
            }
        }
        /** message from ActionList **/
        if(ActionList.size() == 0){
            result += "0" + " ";
            return result;
        }
        else{
            result = result + "1" + " ";
        }
        i = getMinCount();
        result = result + ActionList.get(i).IP + " " + ActionList.get(i).Port + " " + ActionList.get(i).actionTime
                + " " + ActionList.get(i).Action + " ";
        /** action : 0  1  2 **/
        synchronized(this){
            if(ActionList.get(i).count <= threshold){
                //ActionList.remove(i);
                ActionList.get(i).count += 1;
            }
        }
        return result;
    }


    boolean findAction(String ip, int action){
        for(int i = 0; i < ActionList.size(); i++){
            if(ip.equals(ActionList.get(i).IP) && action == ActionList.get(i).Action){
                return true;
            }
        }
        return false;
    }
    
    void deleteAction(String ip){
       /* synchronized (this){
            for(int i = 0; i < ActionList.size() && i >= 0 ; i++){
                if(ActionList.get(i).IP.equals(ip)){
                    ActionList.remove(i);
                    i--;
                }
            }
        }*/
        return;
    }

    /** targetIp(0) targetPort(1) num_of_payload(2) pay_load(3,4,5,6,7) pay_load(8 - 12)
     * pay_load: IP port isjoin isalive isleave**/
    private void updateList(String [] data){
        //System.out.println(data[3]);
        int num = Integer.valueOf(data[3]);
        String ip;
        int port;
        long time_join;
        boolean isalive = false, isleave = false;
        if(num != 0) {
            /** this part is for the first payLoad : an alive node in ACK's Nodelist
             * this might cause a new node added to our NodeList  **/
            int index = 4;
            ip = data[index];
            port = Integer.valueOf(data[index + 1]);
            time_join = Long.valueOf(data[index + 2]);
            int i = 0;
            boolean exist = false;
            for (i = 0; i < NodeList.size(); i++) {
                if (NodeList.get(i).getIp().equals(ip)) {
                    if(NodeList.get(i).bornTime == time_join){
                        exist = true;
                        break;
                    }
                    else if(NodeList.get(i).bornTime < time_join){
                        synchronized(this) {
                            NodeList.get(i).bornTime = time_join;
                            writeLog(-1, NodeList.get(i));
                        }
                        exist = true;
                        break;
                    }
                }
            }
            /** what if we have a new user :)  here we add an unknown user to the NodeList, but I suppose it is wrong..**/
            if (!exist) {
                synchronized(this) {
                    Node tmp = new Node(NodeList.size() + 1, ip, port, time_join, "unknown");
                    NodeList.add(tmp);
                    deleteAction(ip);
                    writeLog(2, tmp);
                }
                //System.out.println(" Ping sender find a new node " + ip + " " + time_join);
                showLists();
            }
        }
        /** next we handel action **/
        int bias;
        if(num == 0){
            num = Integer.valueOf(data[4]);
            bias = 4;
        }
        else{
            num = Integer.valueOf(data[7]);
            bias = 7;
        }
        if(num == 1){
            ip = data[bias + 1];
            port = Integer.valueOf(data[bias + 2]);
            time_join =Long.valueOf(data[bias + 3]);
            int action = Integer.valueOf(data[bias + 4]);
            if(findAction(ip, action)){
                //System.out.println("action exists");
                //System.out.println("update finished");
                return;
            }
            if(action != 2){
                synchronized(this) {
                    ActionNode n = new ActionNode(ip, port, action, time_join);
                    ActionList.add(n);
                }
            }
            //System.out.println("we add an action in PingHandler");
            if(action == 0){
                for(int i = 0; i < NodeList.size(); i++){
                    if(ip.equals(NodeList.get(i).getIp())  && !ip.equals(NodeList.get(SystemId).getIp())){ //  && NodeList.get(i).bornTime == time_join
                        //System.out.println("we find node with index " + i + " in nodelist leave");
                        synchronized (this){
                            writeLog(0, NodeList.get(i));
                            addDead(NodeList.get(i));
                            NodeList.remove(i);
                            showLists();
                        }
                        break;
                    }
                }
                System.out.println("we add a leave action in PingHandler");
            }
            else if(action == 1){
                for(int i = 0; i < NodeList.size(); i++){ // NodeList.get(i).bornTime == time_join &&
                    if(ip.equals(NodeList.get(i).getIp()) && !ip.equals(NodeList.get(SystemId).getIp())){
                        //System.out.println("we find node with index " + i + " in nodelist fail");
                        synchronized (this){
                            writeLog(1, NodeList.get(i));
                            addDead(NodeList.get(i));
                            NodeList.remove(i);
                            showLists();
                        }
                        break;
                    }
                }
                System.out.println("we add an fail action in PingHandler");
            }
            else if(action == 2){
                boolean exist = false;
                for(int i = 0; i < NodeList.size(); i++){
                    if(ip.equals(NodeList.get(i).getIp())){
                        if(time_join == NodeList.get(i).bornTime){
                            exist = true;
                        }
                        else if(NodeList.get(i).bornTime <= time_join){
                            synchronized(this) {
                                NodeList.get(i).bornTime = time_join;
                                writeLog(-1, NodeList.get(i));
                                exist = true;
                            }
                        }
                        break;
                    }
                }

                if(!exist){
                    /** if we have an add and the new node is not in our list, add it to our list **/
                    System.out.println("we add an join action in PingHandler");
                    synchronized(this) {
                        Node newNode = new Node(NodeList.size() + 1, ip, port, time_join, "unknown");
                        if(isJustDead(ip) == false) {
                            NodeList.add(newNode);
                            deleteAction(ip);
                            writeLog(2, newNode);
                            showLists();
                        }
                    
                    }
                    //System.out.println("PingSender add new node through action according to ACK");
                }
            }
        }
    }

    /** reveive a direct ping, send ACK back **/
    private void sendACK(){
        byte [] ACKBuffer = new byte[1024];
        DatagramPacket ACK;
        String result = "";
        double p = Math.random();
        if( p < lost_prob){
            return;
        }
        /** sth more need to be done for ACK message **/
        result = buildACKMessage();
        ACKBuffer = result.getBytes();
        try{
            ACK = new DatagramPacket(ACKBuffer, ACKBuffer.length, receivedPacket.getAddress(), receivedPacket.getPort());
            ackSocket.send(ACK);
        }catch(SocketException e){
            System.out.println(" PingHandler build new socket for answering direct ping failed");

        }catch(IOException e1){
            System.out.println(" PingHandler send ACK for answering direct ping failed");
        }
    }

    /** do we paddle some payload on the ACK, seems yes
     * A serious problem is that indirect ping may also reach both pinghandler 's listen and ping receiver
     * I think we need a new port for indirect ping send
     */

    private void sendIndirectPing(String []data){ // receivedPacket is the indirect ping packet we receive
        double p = Math.random();
        if( p < lost_prob){
            return;
        }
        byte [] sendBuffer = new byte[1024];
        byte [] receivedACK = new byte[1024];
        DatagramPacket forwardPacket;
        DatagramPacket midPacket;
        DatagramPacket backwardPacket;
        InetAddress targetAddress, requestAddress;
        String send = "";
        updateList(data);
        for(int i = 0; i < data.length; i++){
            send += data[i];
            send += " ";
        }
        try{
            targetAddress = InetAddress.getByName(data[0]);
            sendBuffer = send.getBytes();
            indirectPingSocket.setSoTimeout(time_out);
            forwardPacket = new DatagramPacket(sendBuffer, sendBuffer.length, targetAddress, ackSendingPort);
            indirectPingSocket.send(forwardPacket);
            /** receive ACK from indirect ping **/
            midPacket = new DatagramPacket(receivedACK, receivedACK.length);
            ackSocket.receive(midPacket);
            /** update list again **/
            updateList(receivedACK.toString().split(" "));
            /** send this message back, here we don't change the ACK message **/
            backwardPacket = new DatagramPacket(receivedACK, receivedACK.length, receivedPacket.getAddress(), receivedPacket.getPort());
            ackSocket.send(backwardPacket);
            System.out.print("indirect ping succeed in PingHandler");

        }catch (SocketTimeoutException e){
            System.out.print("indirect ping time out");

        }catch(UnknownHostException e0){
            System.out.print("PingHandler finds an unknown address  " + data[0] + " " + e0);
        }catch(SocketException e1){
            System.out.print("PingHandler failed to build socket " + e1);
        }catch(IOException e2){
            System.out.print("PingHandler failed with IOException " + e2);
        }
    }

    /** here we need to handle the ping message **/
    public void run(){
        String data[];
        data = new String(Buffer).split(" ");
        if(ip.equals(data[0])){
            /** direct ping **/
            //System.out.println("direct ping");
            sendACK();
            //System.out.println(new String(Buffer));
            updateList(data);
        }
        else{
            System.out.println("indirect ping");
            sendIndirectPing(data);
            updateList(data);
        }
        writeMemList();
    }
}
