package system;

import java.net.*;
import java.util.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Created by ghe10 on 9/25/16.
 */
public class PingSender extends TimerTask {
    DatagramSocket sendSocket;
    public ArrayList<Node> NodeList;
    public ArrayList<Node> DeadList;
    public ArrayList<ActionNode> ActionList;
    public boolean suicide;
    public boolean isWorking;
    private int time;
    private int dead; /** represents dead machine number **/
    private int t_out = 100;
    private int removeDeadTime = 31;
    /** in ping sender, we need to send ping to others' ping receiving port : ackPort
     * and listen at receivePort
     * **/
    private int pingSendingPort = 20000;   /** ping sending, ack receiving **/
    private int ackSendingPort = 25000;  /** ack sending, ping receiving **/
    private int selfId;
    private byte[] sendBuffer;
    private byte[] ACK;
    private int threshold = 6; /** if we have send this log 10 times, stop sending it **/
    private Node Introducer;
    public double lost_prob = 0;

    public PingSender(ArrayList<Node> list, ArrayList<ActionNode> actionList, int id, int t, Node n, ArrayList<Node> deadList){
        NodeList = list;
        ActionList = actionList;
        DeadList = deadList;
        Introducer = n;
        selfId = id;
        suicide = false;
        isWorking = true;
        sendBuffer = new byte[1024];
        ACK = new byte[1024];
        t_out = t;
        dead = 0;
        try {
            sendSocket = new DatagramSocket(pingSendingPort);
            sendSocket.setSoTimeout(t_out);
        } catch (IOException e){
            System.out.println("exception in ping sender" + e);
        }
    }

    public String getLocalHostIP() {
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

    /** the following three functions are used to judge whether sb has just dead **/
    private boolean isJustDead(String ip){
        int length = DeadList.size();
        if(length == 0) return false;
        for(int i = 0; i < DeadList.size(); i++){
            if(DeadList.get(i).getIp().equals(ip)){
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


    private int getPayloadId(int max){
        Random random = new Random();
        int num = -1;
        while(num < 0){
            num = random.nextInt(max);
        }
        //System.out.println("payload id = " + num);
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

    /** targetIp targetPort num_of_payload pay_load pay_load
     * pay_load: IP port isalive isleave**/
    private String buildPingMessage(int SystemId){
        String result = "";
        result = result + NodeList.get(SystemId).getIp() + " "
                + NodeList.get(SystemId).getPort() + " " + NodeList.get(SystemId).bornTime + " ";
        /** message from NodeList **/
        int i = 0;
        if(NodeList.size() == 0){
            result = result + "0" + " ";
        }
        else {
            int payLoad1 = getPayloadId(NodeList.size());
            result = result + "1" + " ";
            result = result + NodeList.get(payLoad1).getIp() + " "
                    + NodeList.get(payLoad1).getPort() + " " + NodeList.get(payLoad1).bornTime + " ";
        }

        /** message from ActionList **/
        if(ActionList.size() == 0){
            result = result + "0" + " ";
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

    void deleteAction(String ip){
        return;
        /*synchronized (this){
            for(int i = 0; i < ActionList.size() && i >= 0 ; i++){
                if(ActionList.get(i).IP.equals(ip)){
                    ActionList.remove(i);
                    i--;
                }
            }
        }*/
    }
    
    private void showLists(){
        for(int i = 0; i < NodeList.size(); i++){
            System.out.println("Node " + i + " is " + NodeList.get(i).alive + " " + NodeList.get(i).getIp()
                    + " " + NodeList.get(i).bornTime);
        }
        /*for(int i = 0; i < ActionList.size(); i++){
            System.out.println("action " + i + " is " + ActionList.get(i).IP + " " + ActionList.get(i).actionTime
                    + " " + "count = " + ActionList.get(i).count + " " + ActionList.get(i).Action + " action : 0 leave ; 1 fail ; 2 join ");
        }*/
        //writeLog();
    }

    private void read_modify(){
        /** read ACK and modify our member list **/
        String ack = new String(ACK);
        String [] data = ack.split(" ");
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
                            exist = true;
                            break;
                        }
                    }
                }
            }
            /** what if we have a new user :)  here we add an unknown user to the NodeList, but I suppose it is wrong..**/
            if (!exist) {
                synchronized(this) {
                    Node tmp = new Node(NodeList.size() + 1, ip, port, time_join, "unknown");
                    if(isJustDead(ip) == false) {
                        NodeList.add(tmp);
                        writeLog(2, tmp);
                        showLists();
                    }
                }
                //System.out.println(" Ping sender find a new node ");
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
                ActionNode n = new ActionNode(ip, port, action, time_join);
                ActionList.add(n);
            }
            //System.out.println("we add an action in PingHandler");
            if(action == 0){
                for(int i = 0; i < NodeList.size(); i++){
                    if(ip.equals(NodeList.get(i).getIp())  && !ip.equals(NodeList.get(selfId).getIp())){ //&& NodeList.get(i).bornTime == time_join
                        synchronized (this){
                            writeLog(0, NodeList.get(i));
                            addDead(NodeList.get(i));
                            NodeList.remove(i);
                        }
                        showLists();
                        break;
                    }
                }
                System.out.println("we add an leave action in PingHandler");

            }
            else if(action == 1){
                for(int i = 0; i < NodeList.size(); i++){
                    if(ip.equals(NodeList.get(i).getIp()) && !ip.equals(NodeList.get(selfId).getIp())){// && NodeList.get(i).bornTime == time_join
                        synchronized (this){
                            writeLog(1, NodeList.get(i));
                            addDead(NodeList.get(i));
                            NodeList.remove(i);
                        }
                        showLists();
                        break;
                    }
                }
                System.out.println("we add a fail action in PingHandler");

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
                    System.out.println("we add a join action in PingHandler");
                    synchronized(this) {
                        Node newNode = new Node(NodeList.size() + 1, ip, port, time_join, "unknown");
                        if(isJustDead(ip) == false) {
                            NodeList.add(newNode);
                            writeLog(2, newNode);
                            deleteAction(ip);
                            System.out.println("PingSender add new node through action according to ACK");
                            showLists();
                        }
                    }

                }
            }
        }
        //System.out.println("update finished");
    }

    private int getPingId(int exist_id){
        Random random = new Random();
        int num = -1;
        if(NodeList.size() <= 1) return -1;
        if(NodeList.size() == 2 && exist_id != -1 && exist_id != selfId) return -1;
        while(num < 0  || num == selfId || num == exist_id) {
            num = random.nextInt(NodeList.size());
        }
        return num;
    }

    public void send(int ping_id, String pingMessage){
        InetAddress address;
        double p = Math.random();
        if( p < lost_prob){
            return;
        }
        try{
            if(ping_id == -1 && NodeList.size() == 1){ // send to boss
                address = InetAddress.getByName(Introducer.getIp());
                //System.out.println("ping boss");
            }
            else{
                address = InetAddress.getByName(NodeList.get(ping_id).getIp());
            }
            sendBuffer = pingMessage.getBytes();
            sendSocket.setSoTimeout(t_out);
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, ackSendingPort);
            //System.out.println("Sender send message to " + address + " " + ackSendingPort);
            sendSocket.send(sendPacket);
        } catch(UnknownHostException e){
            System.out.println("can't find ip of ping target in PingSender " + e);
        } catch(SocketException e1){
            System.out.println(" Set socket timeout failed in PingSender" + e1);
        } catch(IOException e2){
            System.out.println("send round failed exception in PingSender" + e2);
        }
    }

    private void Suicide(){
        /** leave **/
        /*String payload = "";
        payload = payload + "0" + " ";
        payload = payload + "1" + " " + NodeList.get(selfId).getIp() + " "
                + NodeList.get(selfId).getPort() + " " + NodeList.get(selfId).bornTime + " "
                + "0" + " ";*/
        //if(NodeList.size() <= 1){
        //    return;
        //}
        /*for(int ping_id = 0; ping_id < NodeList.size(); ping_id++) {
            if(ping_id == selfId) continue;
            try {
                InetAddress address = InetAddress.getByName(NodeList.get(ping_id).getIp());
                String result = "";
                result = result + NodeList.get(ping_id).getIp() + " "
                        + NodeList.get(ping_id).getPort() + " " + NodeList.get(ping_id).bornTime + " ";
                result = result + payload;
                sendBuffer = result.getBytes();
                DatagramPacket leavePacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, ackSendingPort);
                sendSocket.send(leavePacket);
            } catch (UnknownHostException e) {
                System.out.println(" during suicide, PingSender have some problem " + e);
            } catch (IOException e1) {
                System.out.println(" during suicide, PingSender have some problem " + e1);
            }
        }*/
        NodeList.clear();
        ActionList.clear();
        DeadList.clear();
    }

    @Override
    public void run() {
        int ping_id = 0;
        String pingMessage = null;
        InetAddress address = null;
        /** set UDP socket timeout to 100 ms **/

        /** need to get system time here **/
        if(suicide){
            if(isWorking == false){
                return;
            }
            isWorking = false;
            System.out.println("this PingSender thread will stop");
            Suicide();
            return;
        }
        else {
            isWorking = true;
        }
        /** there is sth not good for this strategy, we need to modify dead when add a new node **/
        /** direct  ping **/
        ping_id = getPingId(-1);
        //System.out.println(" we have " + ping_id);
        if(ping_id == -1 || ping_id == selfId){
            /** this indicates that we have only ourself alive **/
            /*if(Introducer.getIp().equals(getLocalHostIP())){
                return;
            }
            
            String result = "";
            result = result + Introducer.getIp() + " "
                    + Introducer.getPort() + " " + Introducer.bornTime + " ";
            result = result + "1" + " ";
            result = result + NodeList.get(selfId).getIp() + " "
                    + NodeList.get(selfId).getPort() + " " + NodeList.get(selfId).bornTime + " ";
            result = result + "0" + " ";
            send(-1, result);*/
            updateDeadCount();
            return;
        }
        pingMessage = buildPingMessage(ping_id);
        send(ping_id, pingMessage);
        /** wait for ACK **/
        DatagramPacket receivePacket = new DatagramPacket(ACK, ACK.length);
        try{
            sendSocket.receive(receivePacket);
            //System.out.println(" ip = " + receivePacket.getAddress() + " port = " + receivePacket.getPort());
            read_modify();
        } catch(SocketTimeoutException e){
            /** indirect ping **/
            System.out.println("PingSender receive timeout");
            /** send another message **/
            if(NodeList.size() <= 2){ // we can't send a indirect ping
                System.out.println(" we don't have indirect ping choice, thus we regard node " + ping_id + " as failed");
                synchronized(this) {
                    ActionNode tmp_action = new ActionNode(NodeList.get(ping_id).getIp(), NodeList.get(ping_id).getPort(),
                            1, new Date().getTime());
                    ActionList.add(tmp_action);
                    writeLog(1, NodeList.get(ping_id));
                    addDead(NodeList.get(ping_id));
                    NodeList.remove(ping_id);
                }
                showLists();
                return;
            }
            int indirect_id = getPingId(ping_id);
            //System.out.print(indirect_id);
            send(ping_id, pingMessage);// the orginal send function is only suitable for direct ping , update 2016 9 29 9 32
            System.out.println("receive again");
            try {
                sendSocket.receive(receivePacket);
                read_modify();
            }catch(SocketTimeoutException e2){
                System.out.println(" machine " + selfId + " finds machine " + (ping_id) + " failed");
                if(ping_id >= NodeList.size()){
                    return;
                }
                synchronized(this){
                    ActionNode tmp_action = new ActionNode(NodeList.get(ping_id).getIp(), NodeList.get(ping_id).getPort(),
                            1, new Date().getTime());
                    ActionList.add(tmp_action);
                    writeLog(1, NodeList.get(ping_id));
                    addDead(NodeList.get(ping_id));
                    NodeList.remove(ping_id);
                }
                showLists();
            }
            catch (IOException e3){
                System.out.println("PingSender IO " + e3);
            }
        } catch(IOException e){
            System.out.println("receive failed exception in PingSender" + e);
        }
        updateDeadCount();
    }

}
