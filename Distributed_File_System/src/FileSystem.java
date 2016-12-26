package system;

import java.io.*;
import java.net.*;
import java.util.*;



/**
 * Created by Administrator on 2016/11/1.
 */
public class FileSystem {
    public Hashtable<String, Set<String>> IpToFile = new Hashtable<>();
    public Hashtable<String, Set<String>> FileToIp = new Hashtable<>();
    public Hashtable<String, Long> DeletedFiles = new Hashtable<>(); // just need to consider those delete commands, not failures, failures will be solved when SWIM works
    public Hashtable<String, Long> FileToTime = new Hashtable<>();
    public Hashtable<String, Node> NodeTable;
    String selfIp;
    String localPath;
    String globalPath; // file directory of DFS
    String FileLogPath = "FileLog.log";
    //int fileTcpPort; // for file transmission
    int udpPort = 10000;
    int udpReceivePort = 11000;
    DatagramSocket udpSocket, udpReceiveSocket;
    //ServerSocket tcpListenSocket;

    public FileSystem(Hashtable<String, Node> table){
        selfIp = getLocalHostIP();
        //fileTcpPort = port;
        NodeTable = table;
        String cmd = "rm -rf ../sdfs";
        //System.out.println(cmd);
        try{
            Process pro = Runtime.getRuntime().exec(cmd);
            pro.waitFor();
            InputStream in = pro.getInputStream();
            BufferedReader read = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while((line = read.readLine())!=null){
                System.out.println(line);
            }
            pro = Runtime.getRuntime().exec("mkdir ../sdfs");
            pro.waitFor();
            in = pro.getInputStream();
            read = new BufferedReader(new InputStreamReader(in));
            line = null;
            while((line = read.readLine())!=null){
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("sdfs folder clear up!");
        try {
            udpSocket = new DatagramSocket(udpPort);
            udpReceiveSocket = new DatagramSocket(udpReceivePort);
            //tcpListenSocket = new ServerSocket(fileTcpPort);
        }
        catch(java.net.SocketException e){
            System.err.println("build socket failed" + e);
        }
        ListenToDelete deleteListener = new ListenToDelete();
        Thread s = new Thread(deleteListener);
        s.start();
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

    public void writeFileLog(String fileName, String action) {
        //System.out.println("write log");
        FileOutputStream fileOuts;
        try {
            synchronized(this) {
                fileOuts = new FileOutputStream(FileLogPath, true);
                String out = "";
                out += new Date().getTime();
                out = out +  selfIp + " " + action + " " + fileName + "\n";
                fileOuts.write(out.getBytes());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Itsystem can't find file to write " + e);
        } catch (IOException e1) {
            System.out.println("Itsystem can't write due to IOexception " + e1);
        }
        //System.out.println("write log finish");
    }

    public boolean containsFile(String ip, String fileName){
        if(IpToFile.containsKey(ip) == false) return false;
        Set<String> set = IpToFile.get(ip);
        for (String name : set){
            if(name.equals(fileName)) return true;
        }
        return false;
    }

    public boolean deletedFile(String fileName, long time){
        if(DeletedFiles.containsKey(fileName) && DeletedFiles.get(fileName) == time){
            return true;
        }
        return false;
    }

    public void enhanceTables(String ip, String fileName, long time){ // add <ip, file>
        if(!NodeTable.containsKey(ip)) return;
        synchronized (this){
            if (IpToFile.containsKey(ip)) {
                Set<String> set = IpToFile.get(ip);
                if(set.contains(fileName) || deletedFile(fileName, time)){
                    return;
                }
                set.add(fileName);
                IpToFile.put(ip, set);
                FileToTime.put(fileName, time);
                System.out.println("add file " + fileName + " ip " + ip);
                writeFileLog(fileName, "add");
            }
            else{
                Set<String> tmp = new HashSet<String>();
                tmp.add(fileName);
                IpToFile.put(ip, tmp);
                FileToTime.put(fileName, time);
                System.out.println("add file " + fileName + " ip " + ip);
                writeFileLog(fileName, "add");
            }

            if(FileToIp.containsKey(fileName)){
                Set<String> set = FileToIp.get(fileName);
                if(set.contains(ip)){
                    return;
                }
                else{
                    set.add(ip);
                    FileToIp.put(fileName, set);
                }
            }
            else {
                Set<String> tmp = new HashSet<String>();
                tmp.add(ip);
                FileToIp.put(fileName, tmp);
            }
        }
    }

    public void putFile(String fileName, String newName){ // put file, send two replica
        long time = new Date().getTime();
        copyFile cp = new copyFile(fileName, newName);
        Thread s1 = new Thread(cp);
        s1.start();
        System.out.println("put to global finish");
        enhanceTables(selfIp, newName, time);
        long start=System.currentTimeMillis();
        System.out.println("start time: " + start);
        //tellToAdd(selfIp, newName, time, ArrayList<String> added_ips);
        //for(int i = 0; i < 2; i++){
        addOneReplica add = new addOneReplica(fileName, newName, FileToTime.get(newName), 0, 2);
        Thread s2 = new Thread(add);
        s2.start();
        //}
    }

    public void deleteFileOnThisNode(String fileName){
        // this function will delete file fileName in this node and sll fileNames in both two tables
        String s = "../sdfs/" + fileName;
        File f = new File(s);
        if(f.exists()){
            f.delete();
        }
        if(!FileToIp.containsKey(fileName)){
            System.out.println("no such file to delete");
            return;
        }
        Set<String> ips = FileToIp.get(fileName);

        synchronized (IpToFile) {
            for(String ip : ips) {
                Set<String> files = IpToFile.get(ip);
                files.remove(fileName);
                IpToFile.put(ip, files);
            }
        }
        synchronized ((FileToIp)){
            FileToIp.remove(fileName);
        }
        DeletedFiles.put(fileName, FileToTime.get(fileName));
        FileToTime.remove(fileName);
        writeFileLog(fileName, "delete");
    }

    public void deleteFile(String fileName){ // delete local file, tell others to delete
        if(!FileToIp.containsKey(fileName)){
            return;
        }
        Set<String> set = NodeTable.keySet(); //FileToIp.get(fileName);
        //int len = list.size();
        for(String ip : set){
            /** **/
            //String ip = list.get(i);
            String s =  "delete" + " "  + fileName + " " + FileToTime.get(fileName) + " ";
            byte []  buf = s.getBytes();
            try{
                // tell other nodes to delete
                System.out.println("tell machine " + ip + " to delete " + fileName);
                DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), udpReceivePort);
                udpSocket.send(p);
            }
            catch (java.net.UnknownHostException e){
                System.err.println("send delete mes failed " + e);
            }
            catch (java.io.IOException e1){
                System.err.println("send delete mes failed " + e1);
            }
        }
        deleteFileOnThisNode(fileName);
    }

    public void tellToPut(String ip, String fileName, long time){
        String s =  "put" + " "  + fileName + " " + time + " ";
        byte []  buf = s.getBytes();
        try{
            // tell other nodes to delete
            DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), udpReceivePort);
            udpSocket.send(p);
        }
        catch (java.net.UnknownHostException e){
            System.err.println("send put mes failed " + e);
        }
        catch (java.io.IOException e1){
            System.err.println("send put mes failed " + e1);
        }
    }

    public void tellToAdd(String ip, String fileName, long time, ArrayList<String> added_ips){
        // tell a node we have just add a file to this SDFS
        int size = added_ips.size();
        String s =  "add" + " "  + fileName + " " + time + " " + size + " ";
        for(int i = 0; i < size; i++){
            s = s + added_ips.get(i) + " ";
        }
        byte []  buf = s.getBytes();
        try{
            // tell other nodes to delete
            DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), udpReceivePort);
            udpSocket.send(p);
        }
        catch (java.net.UnknownHostException e){
            System.err.println("send add mes failed " + e);
        }
        catch (java.io.IOException e1){
            System.err.println("send add mes failed " + e1);
        }
    }
    // change this to a class
    class addOneReplica  extends TimerTask implements Runnable {
        public String oldName;
        public String newName;
        public long time;
        public int mode;
        public int num; // number of replica we need
        public addOneReplica(String on,String nn, long t, int m){
            oldName = on;
            newName = nn;
            time = t;
            mode = m;
            num = 1;
        }
        public addOneReplica(String on,String nn, long t, int m, int n){
            oldName = on;
            newName = nn;
            time = t;
            mode = m;
            num = n;
        }
        public void run() {
            synchronized (addOneReplica.class) {
                // this is called by updateList in tools, thus we shouldn't lock NodeTable, it is already locked
                String ip1 = null, ip2 = null;
                ArrayList<String> ips = new ArrayList<>();
                ips.add(selfIp);
                int count = 0;
                if (NodeTable.size() < 3) {
                    System.out.println("we don't have enough nodes to save replica");
                    return;
                }
                System.out.println("we have enough nodes");
                // this part is modified on 2016/11/18
                Set<String> set = NodeTable.keySet();
                LinkedList<String> list = new LinkedList<String>(set);
                Collections.shuffle(list);
                for (String s : list) {
                    System.out.println("consider ip " + s);
                    if (!containsFile(s, newName)) {
                        ip1 = s;
                        System.out.println("decide to add replica of " + newName + " on machine " + ip1);
                        enhanceTables(ip1, newName, time);
                        sendFile(ip1, oldName, newName, mode);// first send file, then tell them
                        count++;
                        ips.add(ip1);
                        if (count == num) {
                            break;
                        }
                    }
                    System.out.println(s + "is not suitable");
                }
                /*for (String ip : ips) {
                    //enhanceTables(ip, newName, time);
                    tellToPut(ip, newName, time);
                }*/
                for (String ip : set){
                    tellToAdd(ip, newName, time, ips);
                }
                if (count < num) {
                    System.out.println("we are short of suitable nodes, thus no enough replica");
                }
            }
        }
    }

    public void updateForFailure(String ip) {
        Set<String> files;
        String addMachine = null;
        synchronized (IpToFile) {
            if (!IpToFile.containsKey(ip)) {
                System.out.println("no file is related to " + ip);
                return;
            }
            files = IpToFile.get(ip);
            IpToFile.remove(ip); // update IpToFile
        }
        synchronized (FileToIp) {
            long start = System.currentTimeMillis();
            System.out.println("failure update start: " + start);
            for (String fileName : files) {
                Set<String> ips = FileToIp.get(fileName);
                ips.remove(ip); // remove this ip
                if(ips.size() == 0){
                    FileToIp.remove(fileName);
                    continue;
                }
                FileToIp.put(fileName, ips);
                //int myIndex = ips.indexOf(selfIp);
                if (!ips.contains(selfIp)) { // myself can't  help to add a replica
                    continue;
                } /////////////////////////////////////////////////// index out of bound
                //int theOtherIndex = (ips.indexOf(selfIp) + 1) % 2;
                String theOtherIp = "";
                boolean isMyJob = true;
                for(String otherIp : ips){
                    if(selfIp.compareTo(otherIp) < 0){
                        // my ip is not the largest remaining, it is not my task
                        isMyJob = false;
                        break;
                    }
                }
                if(isMyJob) {
                    // first half is for the situation where we have only two machines exist
                    //less than, equal, or greater than
                    System.out.println("start thread for new replica");
                    if(ips.size() == 2) {
                        System.out.println("try to add one replica");
                        addOneReplica add = new addOneReplica(fileName, fileName, FileToTime.get(fileName), 2);
                        Timer timer = new Timer();
                        timer.schedule(add, 3000);
                    }
                    else if(ips.size() == 1){
                        System.out.println("try to add two replica");
                        addOneReplica add = new addOneReplica(fileName, fileName, FileToTime.get(fileName), 2, 2); // add two replica
                        Timer timer = new Timer();
                        timer.schedule(add, 3000);
                    }
                    //Thread s = new Thread(add);
                    //s.start();
                    //Timer timer = new Timer();
                    //timer.schedule(add, 2000);
                }
                FileToIp.put(fileName, ips); // update FileToIp
            }
        }
    }


    public void printLocalFile(){
        Set<String> set;
        synchronized (IpToFile) {
            set = IpToFile.get(selfIp);
        }
        if(set == null){
            System.out.println("we never have file locally");
            return;
        }
        int len = set.size();
        if(len == 0) System.out.println("we have no file locally");
        for (String fileName : set) {
            System.out.println(fileName);
        }
    }



    public String fileMessage(){
        // do we need lock for  read table ???
        String res = "", targetFile = "";
        Set<String> ips;
        int size = 0, i = 0;
        //Enumeration<String> elements;
        Set<String> set;
        if (FileToIp.isEmpty()){
            // System.out.println("no file");
            res = res + "0" + " ";
            return res;
        }
        else{
            //System.out.println("we have files");
            res = res +  "1" + " ";
            size = FileToIp.size();
            Random random = new Random();
            int num = random.nextInt(size);
            //elements = FileToIp.elements();
            set = FileToIp.keySet();
            Iterator it = set.iterator();
            for(i = 0; i < num && it.hasNext(); i++){
                it.next();
            }
            if(!it.hasNext()){
                System.out.println("we don't have file for index " + num +" any more");
                return ("0" + " ");
            }
            targetFile = (String) it.next();
            //System.out.println("name of file : " + targetFile);
            ips = FileToIp.get(targetFile);
            res = res + targetFile + " " + FileToTime.get(targetFile) + " " + ips.size() + " ";
            for(String ip : ips){
                res = res + ip + " ";
            }
        }
        //System.out.println(res);
        return res;
    }

    public void request(String sdfsName, String saveName){
        requestFile req = new requestFile(sdfsName, saveName);
        Thread s = new Thread(req);
        s.start();
    }

    public void ls(String fileName){
        if(!FileToIp.containsKey(fileName)){
            System.out.println("we don't have such file");
            return;
        }
        System.out.println(fileName + " is saved at:");
        Set<String> s = FileToIp.get(fileName);
        for(String ip : s){
            System.out.println(ip);
        }
    }

    /** this part is for file transmission **************************************************/
    public static void sendFile(String IPaddr, String oldName, String newName, int mode){  // mode = 0 put; mode = 1 request; mode = 2 add replica when failure
        // change yingyiz2 to ghe10 if necessary
        String cmd = null;
        if(mode == 0) {
            cmd = "scp ../local/" + oldName + " ghe10@" + IPaddr + ":sdfs/" + newName;
        }
        else if(mode == 1){
            cmd = "scp ../sdfs/"+ oldName + " ghe10@" + IPaddr + ":local/" + newName;
        }
        else{
            cmd = "scp ../sdfs/"+ oldName + " ghe10@" + IPaddr + ":sdfs/" + newName;
        }
        //System.out.println(oldName);
        //System.out.println(IPaddr);
        System.out.println(cmd);
        try{
            Process pro = Runtime.getRuntime().exec(cmd);
            pro.waitFor();
            InputStream in = pro.getInputStream();
            BufferedReader read = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while((line = read.readLine())!=null){
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("send file error");
        }
        long end = System.currentTimeMillis();
        System.out.println("end time: " + end);
        System.out.println("Send file complete");
    }

    class copyFile implements Runnable {
        public String oldName;
        public String newName;
        public copyFile(String on, String nn){
            oldName = on;
            newName = nn;
        }
        public void run() {
            String cmd = "cp ../local/" + oldName + " ../sdfs/" + newName;
            try {
                Process pro = Runtime.getRuntime().exec(cmd);
                pro.waitFor();
                InputStream in = pro.getInputStream();
                BufferedReader read = new BufferedReader(new InputStreamReader(in));
                String line = null;
                while ((line = read.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Copy file complete");
        }
    }


    /** this part is for multi-thread **************************************************/
    public class requestFile implements Runnable {
        public String sdfsName;
        public String saveName;
        public DatagramSocket delaySocket;
        public requestFile(String sdfsN, String saveN){
            sdfsName = sdfsN;
            saveName = saveN;
            try {
                delaySocket = new DatagramSocket(14000);
                delaySocket.setSoTimeout(500);
            }
            catch(java.net.SocketException e){
                e.printStackTrace();
            }
        }
        public void run() {
            String request = "";
            byte[] buffer = new byte[1024];
            if (!FileToIp.containsKey(sdfsName)) {
                System.out.println("We don't have file " + sdfsName);
                return;
            }
            Set<String> ips = FileToIp.get(sdfsName);
            request = request + "request" + " " + selfIp + " " + sdfsName + " " + saveName + " ";
            buffer = request.getBytes();
            for (String ip : ips) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), udpReceivePort);
                    udpSocket.send(packet);
                    System.out.println("start waiting");
                    delaySocket.receive(packet);
                    System.out.println("waiting ends");
                }
                catch (SocketTimeoutException e){
                    File file = new File("../local/"+ saveName);
                    if (file.exists()){
                        System.out.println("receive success");
                        break;
                    }
                }
                catch (java.net.UnknownHostException e){
                    e.printStackTrace();
                }
                catch (java.io.IOException e1){
                    e1.printStackTrace();
                }
            }
        }
    }

    class ListenToDelete implements Runnable {
        // recive add and delete command
        byte [] Buffer;
        ListenToDelete(){
            Buffer = new byte[1024];
            /*try {
                DatagramSocket UdpSocket = new DatagramSocket(udpReceivePort);
            }
            catch(java.net.SocketException e){
                System.out.println("in ListenToDelete");
                e.printStackTrace();
            }*/
        }
        public void run(){
            System.out.println("start add delete listener");
            while (true) {
                Buffer = new byte[1024];
                String fileName = "", saveName = "", ip = "", tmp = "";
                long time = 0;
                DatagramPacket receivePacket = new DatagramPacket(Buffer, Buffer.length);
                try {
                    udpReceiveSocket.receive(receivePacket);
                    tmp = new String(Buffer);
                    System.out.println("we receive a command " + tmp);
                    String [] data = tmp.split(" ");
                    if(data[0].equals("delete")) {
                        fileName = data[1];
                        deleteFileOnThisNode(fileName);
                    }
                    else if(data[0].equals("put")){
                        fileName = data[1];
                        time = Long.valueOf(data[2]);
                        enhanceTables(selfIp, fileName, time);
                    }
                    else if(data[0].equals("request")){
                        ip = data[1];
                        fileName = data[2];
                        saveName = data[3];
                        System.out.println("receive request " + ip + " " + fileName);
                        sendFile(ip, fileName, saveName, 1);
                    }
                    else if(data[0].equals("add")){
                        // this is a multicasted mess for new input data
                        //String s =  "add" + " "  + fileName + " " + time + " " + size + " ";
                        fileName = data[1];
                        time = Long.valueOf(data[2]);
                        int size = Integer.valueOf(data[3]);
                        for(int i = 4; i < 4 + size; i++){
                            enhanceTables(data[i], fileName, time);
                        }
                    }
                }
                catch (java.io.IOException e){
                    System.out.println("in ListenToDelete");
                    e.printStackTrace();
                }
            }
        }
    }
}
