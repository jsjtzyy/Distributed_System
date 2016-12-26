package system;

import java.io.*;
import java.net.InetAddress;
import java.util.*;

/**
 * Created by ghe10 on 10/14/16.
 */
public class tools extends TimerTask{
    public Hashtable<String, Node> NodeTable; //<String, Node>
    public Hashtable<String, ActionNode> ActionTable;
    public Hashtable<String, Node> DeadTable;
    public String[] actions;
    public String SelfIp;
    public String LogPath;
    public String SendLogPath;
    public int threshold = 10;
    public int removeDeadTime = 30;
    public FileSystem fileSystem;

    tools() {
        SelfIp = getLocalHostIP();
        LogPath = "log.log";
        SendLogPath = "sendLog.log";
        actions = new String[]{" leave ", " fail ", " join"};
    }

    tools(Hashtable nodeTable, Hashtable actionTable, Hashtable deadTable) {
        //System.out.println("build function in tools");
        NodeTable = nodeTable;
        ActionTable = actionTable;
        DeadTable = deadTable;
        SelfIp = getLocalHostIP();
        LogPath = "log.log";
        SendLogPath = "sendLog.log";
        //fileSystem = new FileSystem(SelfIp, NodeTable);
        //System.out.println(NodeTable == null);
    }

    // this function is just for ItSystem
    tools(Hashtable nodeTable, Hashtable actionTable, Hashtable deadTable , FileSystem f) {
        //System.out.println("build function in tools");
        NodeTable = nodeTable;
        ActionTable = actionTable;
        DeadTable = deadTable;
        SelfIp = getLocalHostIP();
        LogPath = "log.log";
        SendLogPath = "sendLog.log";
        fileSystem = f;
        //System.out.println(fileSystem == null);
    }

    // this is for timertask
    public void run(){

    }

    public String getLocalHostIP() {
        String ip;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ip = addr.getHostAddress();
        } catch (Exception ex) {
            System.out.println("We can't find our own ip....");
            ip = "";
        }
        return ip;
    }

    /**
     * this part is for MP1 log grep------------------------------------------------------
     **/
    public int selfCount(String pattern){
        Runtime runtime = Runtime.getRuntime();
        String line = null;
        int num = 0;
        String [] c = pattern.split(" ");
        String [] cmd = {"grep","-c", c[c.length - 1], LogPath};
        try {
            Process p = runtime.exec(cmd);
            BufferedInputStream in = new BufferedInputStream(p.getInputStream());
            BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
            line = inBr.readLine();
            num = Integer.parseInt(line);
            System.out.println(num);
            return num;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return num;
    }

    public void selfSearch(String pattern) {
        Runtime runtime = Runtime.getRuntime();
        pattern = pattern + " " + LogPath;
        String[] cmd = pattern.split(" ");
        FileOutputStream fileOuts;
        try {
            Process p = runtime.exec(cmd);
            BufferedInputStream in = new BufferedInputStream(p.getInputStream());
            BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
            fileOuts = new FileOutputStream("sendLog.log");
            String line = null;
            while ((line = inBr.readLine()) != null) {
                fileOuts.write((line).getBytes());
            }
            if (p.waitFor() != 0) {
                if (p.exitValue() != 0) {
                    if (p.exitValue() == 1) {
                        System.err.println("self grep exit incorrect");
                    }
                }
            }
            inBr.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this part is for MP2 failure detector and MP3 file system----------------------------------------------
     **/
    public void showPing(String [] data) {
        if(data == null){
            System.out.println(" data is empty!");
            return;
        }
        for(int i = 0; i < data.length; i++){
            System.out.print(data[i] + " ");
        }
    }

    public void showLists() {
       // System.out.println("showLists");
        int num = 0;
        Enumeration<Node> elements = NodeTable.elements();
        while (elements.hasMoreElements()) {
            num++;
            Node n = elements.nextElement();
            System.out.println(num + " " + n.Ip + " " + n.bornTime);
        }
        //System.out.println("showLists finish");
    }

    public void writeLog(int action, Node n) {
        //System.out.println("write log");
        FileOutputStream fileOuts;
        try {
            synchronized(this) {
                fileOuts = new FileOutputStream(LogPath, true);
                String out = "";
                out += new Date().getTime();
                if (action == 2) out += " add ";
                else if (action == 1) out += " fail ";
                else if (action == 0) out += " leave ";
                else if (action < 0) out += " update ";
                out = out + n.Ip + " " + n.bornTime + "\n";
                fileOuts.write(out.getBytes());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Itsystem can't find file to write " + e);
        } catch (IOException e1) {
            System.out.println("Itsystem can't write due to IOexception " + e1);
        }
        //System.out.println("write log finish");
    }

    public void updateNodeTable(String ip, int port, long t){
        synchronized (NodeTable){
            if(NodeTable.containsKey(ip)){
                //NodeTable.get(ip).bornTime = NodeTable.get(ip).bornTime > t ? NodeTable.get(ip).bornTime : t;
                if(NodeTable.get(ip).bornTime < t) {
                    NodeTable.get(ip).bornTime = t;
                    writeLog(2, NodeTable.get(ip));
                    showLists();
                }
            }
            else{
                if(isJustDead(ip)) return;
                Node n = new Node(ip, port, t, "unknown");
                NodeTable.put(ip, n);
                System.out.println("add node " + ip + " " + t);
                writeLog(2, n);
                showLists();
            }
        }
    }

    public void deleteNode(String ip){
        synchronized (NodeTable){
            if(NodeTable.containsKey(ip)){
                writeLog(1, NodeTable.get(ip));
                DeadTable.put(ip, NodeTable.get(ip));
                NodeTable.remove(ip);
                fileSystem.updateForFailure(ip);
                showLists();
            }
        }
    }

    public void updateAction(String ip, int port, long time_join, int action){
        // action 0->leave 1->fail 2->join
        if(ip.equals(SelfIp) && action != 2){
            System.out.println("someone mistakes me as failed");
            return;
        }
        if(action <= 1){
            synchronized (ActionTable){ // just updated on 11 5
                /*if(NodeTable.containsKey(ip)){
                    DeadTable.put(ip, NodeTable.get(ip));
                    writeLog(action, NodeTable.get(ip));
                    NodeTable.remove(ip);
                    showLists();
                }*/
                deleteNode(ip);
                if(ActionTable.containsKey(ip)){
                    /******/
                    ActionNode n = ActionTable.get(ip);
                    n.Action = action;
                    n.actionTime = time_join;
                    ActionTable.put(ip, n);
                }
            }
        }
        else{ // join
            synchronized (this){
                if(NodeTable.containsKey(ip)){
                    //NodeTable.get(ip).bornTime = NodeTable.get(ip).bornTime > time_join ? NodeTable.get(ip).bornTime : time_join;
                    if(NodeTable.get(ip).bornTime < time_join) {
                        Node n = NodeTable.get(ip);
                        n.bornTime = time_join;
                        NodeTable.put(ip, n);
                        writeLog(2, NodeTable.get(ip));
                        showLists();
                    }
                }
                else{
                    Node n = new Node(ip, port, time_join, "unknown");
                    if(isJustDead(ip)) return;
                    NodeTable.put(ip, n);
                    writeLog(1, n);
                    showLists();
                }
            }
        }
    }

    public void updateList(String[] data) {
        // targetIp(0) targetPort(1) num_of_payload(2) pay_load(3,4,5,6,7) pay_load(8 - 12)
        // pay_load: IP port isjoin isalive isleave
        //System.out.println("update List");
        if(data.length < 5){
            System.out.println("len = " + data.length);
            System.out.println("data[0] = " + data[0]);
            System.out.println("we have a wrong data*******************************************************");
            return;
        }
        int num = Integer.valueOf(data[3]);
        String ip;
        int port;
        long time_join;
        int index = 0;
        if (num != 0) {
            // this part is for the first payLoad : an alive node in ACK's Nodelist
            //this might cause a new node added to our NodeList
            index = 4;
            ip = data[index];
            port = Integer.valueOf(data[index + 1]);
            time_join = Long.valueOf(data[index + 2]);
            int i = 0;
            updateNodeTable(ip, port, time_join);
        }
        /** next we handel action **/
        //int bias;
        if (num == 0) {
            num = Integer.valueOf(data[4]);
            index = 4;
        } else {
            num = Integer.valueOf(data[7]);
            index = 7;
        }
        if (num == 1) {
            ip = data[index + 1];
            port = Integer.valueOf(data[index + 2]);
            time_join = Long.valueOf(data[index + 3]);
            int action = Integer.valueOf(data[index + 4]);
            updateAction(ip, port, time_join, action);
            //index = index + 4;
        }
        /** next we deal with file **/
//        for(int k = 0; k < data.length; k++){
//            System.out.println(k + " " + data[k]);
//        }
       /* if(num == 0) index += 1;
        else index += 5;                   /////////////
        String fileName = "";
        long timeStamp = 0;
        num = Integer.valueOf(data[index]);
        if(num == 0) return;
        else {
            index++;
            fileName = data[index];
            index++;
            timeStamp = Long.valueOf(data[index]);
            index++;
            num = Integer.valueOf(data[index]); // number of ips that save this file
        }

        for(int i = 1; i <= num; i++){
            fileSystem.enhanceTables(data[index + i], fileName, timeStamp);
        }*/
       // System.out.println("update List finish");
    }

    public int getPayloadId(int max){
        Random random = new Random();
        int num = random.nextInt(max);
        return num;
    }

    public ActionNode getActionNode(){
        /** return the ActionNode with minimum count **/
        Enumeration<ActionNode> elements; //= ActionTable.elements();
        int min_count = 100,i = 0;
        String targetIp = "";
        ActionNode n;
        synchronized (ActionTable) {
            elements = ActionTable.elements();
            for (i = 0; i < ActionTable.size(); i++) {
                n = elements.nextElement();
                if (n.count < min_count) {
                    min_count = n.count;
                    targetIp = n.IP;
                }
            }
            if (min_count >= threshold) {
                return null;
            }
            ActionTable.get(targetIp).count++;
            return ActionTable.get(targetIp);
        }
    }

    public Node getNode(){
        Enumeration<Node> elements;
        synchronized (NodeTable) {
            int max = NodeTable.size();
            int index = getPayloadId(max);
            elements = NodeTable.elements();
            for (int i = 0; i < index; i++) {
                elements.nextElement();
            }
        }
        return elements.nextElement();
    }

    public Node getPingTarget(){
        Random random = new Random();
        Node n = null;
        int num = -1;
        Enumeration<Node> elements;
        if(NodeTable.size() <= 1) return null;
        synchronized (NodeTable) {  // find a node to ping
            while (true) {
                num = random.nextInt(NodeTable.size());
                elements = NodeTable.elements();
                for(int i = 0; i < num; i++){
                    elements.nextElement();
                }
                n = elements.nextElement();
                if(!n.Ip .equals(SelfIp)){
                    return n;
                }
            }
        }
    }

    public String buildMessage(Node target, String ip){
        // ip is for : we have a ping from A, but A is not in our list
        //System.out.println("build message " + mode);
        String result = "";
        if(target != null) {
            result = result + target.Ip + " " + target.Port + " " + target.bornTime + " ";
        }
        else{
            long tmp_t = 100;
            result = result + ip + " " + 40000 + " " + tmp_t + " ";
        }
        /** message from NodeList **/
        if(NodeTable.size() == 0){
            result = result + "0" + " ";
        }
        else {
            Node n = getNode();
            result = result + "1" + " ";
            result = result + n.Ip + " " + n.Port + " " + n.bornTime + " ";
        }
        /** message from ActionList **/
        if(ActionTable.size() == 0){
            result += "0" + " ";
        }
        else{
            ActionNode an = getActionNode();
            if(an != null) {
                result = result + "1" + " ";
                result = result + an.IP + " " + an.Port + " " + an.actionTime + " " + an.Action + " ";
            }
            else{
                result = result + "0" + " ";
            }
        }
        //System.out.println(fileSystem == null);
        //String filemes = fileSystem.fileMessage();
        //result = result + filemes;
        //System.out.println("the result we build is " + result);
        //System.out.println("build message " + mode + " finish");
        return result;
    }

    /** the following three functions are used to judge whether sb has just dead **/
    /*public void addDead(Node dead){
        synchronized (DeadTable){
            DeadTable.put(dead.Ip, dead);
        }
    }*/

    public boolean isJustDead(String ip){
        if(DeadTable.containsKey(ip)) return true;
        return false;
    }

    public void updateDeadCount(){
        int length;
        Node n;
        synchronized (DeadTable){
            length = DeadTable.size();
            Enumeration<Node> elements = DeadTable.elements();
            for(int i = 0; i < length; i++){
                n = elements.nextElement();
                n.count += 1;
                if(n.count == removeDeadTime){
                    DeadTable.remove(n.Ip);
                }
                else{
                    DeadTable.put(n.Ip,n);
                }
            }
        }
    }
}
