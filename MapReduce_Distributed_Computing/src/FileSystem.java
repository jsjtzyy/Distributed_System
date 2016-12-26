package system;

import java.io.*;
import java.net.*;
import java.util.*;


/**
 * Created by Administrator on 2016/11/1.
 */
public class FileSystem {
    public Hashtable<String, Set<String>> IpToFile = new Hashtable<>(); // ip -> file list
    public Hashtable<String, Set<String>> FileToIp = new Hashtable<>(); // file -> ip list
    public Hashtable<String, Long> DeletedFiles = new Hashtable<>(); // just need to consider those delete commands, not failures, failures will be solved when SWIM works
    public Hashtable<String, Long> FileToTime = new Hashtable<>(); // file -> time added
    public Hashtable<String, Node> NodeTable;
    public static File logFile;
    String selfIp;
    String LogPath = "log.log";
    String globalPath; // file directory of DFS
    String FileLogPath = "FileLog.log";
    //int fileTcpPort; // for file transmission
    int udpPort = 10000;
    int udpReceivePort = 11000;
    DatagramSocket udpSocket, udpReceiveSocket;
    //ServerSocket tcpListenSocket;

    /** init a file system **/
    public FileSystem(Hashtable<String, Node> table){
        selfIp = getLocalHostIP();
        //fileTcpPort = port;
        NodeTable = table;
        String cmd = "rm -rf ../sdfs";
        logFile = new File(LogPath);
        //System.out.println(cmd);
        try{
            // clear the sdfs folder
            Process pro = Runtime.getRuntime().exec(cmd);
            pro.waitFor();
            InputStream in = pro.getInputStream();
            BufferedReader read = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while((line = read.readLine())!=null){
                System.out.println(line);
            }
            // make folder in case it doesn't exist
            pro = Runtime.getRuntime().exec("mkdir ../sdfs");
            pro.waitFor();
            in = pro.getInputStream();
            read = new BufferedReader(new InputStreamReader(in));
            line = null;
            // print execute mes
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

    /** write log with sync**/
    public static void write(String log){
        FileOutputStream os;
        log = log + '\n';
        synchronized (logFile){
            try {
                os = new FileOutputStream(logFile, true);
                os.write(log.getBytes());
            }
            catch (java.io.IOException e){
                System.out.println(e);
            }
        }
    }

    /** get local ip **/
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

    /*public void write(String log){
        // try to call a function in tools
        tools.writeLog(log);
    }*/

    /** write filesystem log **/
    public void writeFileLog(String fileName, String action) {
        String out = "";
        out += new Date().getTime();
        // build mes
        out = out +  selfIp + " " + action + " " + fileName;
        write(out);
        //System.out.println("write log");
        /*FileOutputStream fileOuts;
        try {
            synchronized(this) {
                fileOuts = new FileOutputStream(FileLogPath, true);
                String out = "";
                out += new Date().getTime();
                out = out +  selfIp + " " + action + " " + fileName + "\n";

                //fileOuts.write(out.getBytes());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Itsystem can't find file to write " + e);
        } catch (IOException e1) {
            System.out.println("Itsystem can't write due to IOexception " + e1);
        }*/
        //System.out.println("write log finish");
    }

    /** check if we have a specific file **/
    public boolean containsFile(String ip, String fileName){
        if(IpToFile.containsKey(ip) == false) return false;
        Set<String> set = IpToFile.get(ip);
        for (String name : set){
            if(name.equals(fileName)) return true;
        }
        return false;
    }

    /** delete a dile in sdfs **/
    public boolean deletedFile(String fileName, long time){
        if(DeletedFiles.containsKey(fileName) && DeletedFiles.get(fileName) == time){
            return true;
        }
        return false;
    }

    /** change local file hashtable according to update message**/
    public void enhanceTables(String ip, String fileName, long time){ // add <ip, file>
        if(!NodeTable.containsKey(ip)) return;
        synchronized (this){
            // check if this ip is already in the hashtable
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
                // directly put
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

    /** add replicas and local sdfs file**/
    public void putFile(String fileName, String newName){ // put file, send two replica
        long time = new Date().getTime();
        copyFile cp = new copyFile(fileName, newName);
        Thread s1 = new Thread(cp);
        s1.start();
        System.out.println("put to global finish");
        // change the file table
        enhanceTables(selfIp, newName, time);
        long start=System.currentTimeMillis();
        System.out.println("start time: " + start);
        //for(int i = 0; i < 2; i++){
        // add replica
        addOneReplica add = new addOneReplica(fileName, newName, FileToTime.get(newName), 0, 2);
        Thread s2 = new Thread(add);
        s2.start();
        //}
    }

    /** delete a file in local sdfs folder */
    public void deleteFileOnThisNode(String fileName){
        // this function will delete file fileName in this node and sll fileNames in both two tables
        String s = "../sdfs/" + fileName;
        File f = new File(s);
        //check if we have the file
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

    /** delete a file in sdfs **/
    public void deleteFile(String fileName){ // delete local file, tell others to delete
        //check if we have the file
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

    /** tell to put, not used now **/
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

    /** tell other nodes a file is added **/
    public void tellToAdd(String ip, String fileName, long time, ArrayList<String> added_ips){
        // tell a node we have just add a file to this SDFS
        int size = added_ips.size();
        String s =  "add" + " "  + fileName + " " + time + " " + size + " ";
        for(int i = 0; i < size; i++){
            s = s + added_ips.get(i) + " ";
        }
        byte []  buf = s.getBytes();
        try{
            // tell other nodes to add
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

    /** add replicas to sdfs**/
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

        /** run function for add replica **/
        public void run() {
            synchronized (addOneReplica.class) {
                // this is called by updateList in tools, thus we shouldn't lock NodeTable, it is already locked
                String ip1 = null, ip2 = null;
                ArrayList<String> ips = new ArrayList<>();
                ips.add(selfIp);
                int count = 0;
                // check if vaild
                if (NodeTable.size() < 3) {
                    System.out.println("we don't have enough nodes to save replica");
                    return;
                }
                ips.add(selfIp);
                //System.out.println("we have enough nodes");
                // this part is modified on 2016/11/18
                Set<String> set = NodeTable.keySet();
                LinkedList<String> list = new LinkedList<String>(set);
                Collections.shuffle(list);
                // add replicas
                for (String s : list) {
                    //System.out.println("consider ip " + s);
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
                    //System.out.println(s + "is not suitable");
                }
                /*for (String ip : ips) {
                    //enhanceTables(ip, newName, time);
                    tellToPut(ip, newName, time);
                }*/
                // tell others the added file message
                for (String ip : set){
                    tellToAdd(ip, newName, time, ips);
                }
                if (count < num) {
                    System.out.println("we are short of suitable nodes, thus no enough replica");
                }
            }
        }
    }

    /** update file system for a detected failure **/
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
                    //System.out.println("start thread for new replica");
                    if(ips.size() == 2) {
                        //System.out.println("try to add one replica");
                        addOneReplica add = new addOneReplica(fileName, fileName, FileToTime.get(fileName), 2);
                        Timer timer = new Timer();
                        timer.schedule(add, 3000);
                    }
                    else if(ips.size() == 1){
                        //System.out.println("try to add two replica");
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

    /** show what is saved locally **/
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


    /** build a message for file**/
    public String fileMessage(){
        // do we need lock for  read table
        String res = "", targetFile = "";
        Set<String> ips;
        int size = 0, i = 0;
        //Enumeration<String> elements;
        Set<String> set;
        if (FileToIp.isEmpty()){
            // no file
            // System.out.println("no file");
            res = res + "0" + " ";
            return res;
        }
        else{
            // we have file messages to send
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
        // return file message
        return res;
    }

    /** request a file **/
    public void request(String sdfsName, String saveName){
        try {
            requestFile req = new requestFile(sdfsName, saveName);
            Thread s = new Thread(req);
            s.start();
            s.join();
        }catch(java.lang.InterruptedException e){
            System.out.println(e);
        }
    }

    /** check if a file exists **/
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
    public static void sendFile(String IPaddr, String oldName, String newName, int mode){
        // mode = 0 put; mode = 1 request; mode = 2 add replica when failure; mode = 3 send map data and file
        // change yingyiz2 to ghe10 if necessary
        String cmd = null;
        File f;
        if(mode == 0) {
            f = new File("../local/" + oldName);
            System.out.println("file stste : " + f.exists());
            cmd = "scp -r ../local/" + oldName + " yingyiz2@" + IPaddr + ":sdfs/" + newName;
        }
        else if(mode == 1){
            f = new File("../sdfs/" + oldName);
            System.out.println("file stste : " + f.exists());
            cmd = "scp -r ../sdfs/"+ oldName + " yingyiz2@" + IPaddr + ":local/" + newName;
        }
        else if(mode == 2){
            f = new File("../sdfs/" + oldName);
            System.out.println("file stste : " + f.exists());
            cmd = "scp -r ../sdfs/"+ oldName + " yingyiz2@" + IPaddr + ":sdfs/" + newName;
        }
        else if(mode == 3){
            f = new File("../local/" + oldName);
            System.out.println("file stste : " + f.exists());
            cmd = "scp -r ../local/"+ oldName + " yingyiz2@" + IPaddr + ":local/" + newName;
        }
        else if(mode == 4){
            f = new File("../intermediate/" + oldName);
            System.out.println("file stste : " + f.exists());
            // send message back for merge and next step
            cmd = "scp -r ../intermediate/"+ oldName + " yingyiz2@" + IPaddr + ":intermediate/" + newName;
        }
        else if(mode == 5){
            f = new File("../local/" + oldName);
            System.out.println("file stste : " + f.exists());
            cmd = "scp -r ../local/"+ oldName + " yingyiz2@" + IPaddr + ":MP4/" + newName;
        }
        //System.out.println(oldName);
        //System.out.println(IPaddr);
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
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("send file error");
        }
        long end = System.currentTimeMillis();
        //System.out.println("end time: " + end);
        //System.out.println("Send file complete");
    }

    /** this class is used to copy file to sdfs**/
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
                // execute system command
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
            //System.out.println("Copy file complete");
        }
    }


    /** this part is for multi-thread **************************************************/
    public class requestFile implements Runnable {
        public String sdfsName;
        public String saveName;
        public DatagramSocket delaySocket;
        /** init a request **/
        public requestFile(String sdfsN, String saveN){
            sdfsName = sdfsN;
            saveName = saveN;
            /*try {
                delaySocket = new DatagramSocket(14000);
                delaySocket.setSoTimeout(500);
            }
            catch(java.net.SocketException e){
                e.printStackTrace();
            }*/
            try {
                Thread.sleep(150);
            } catch(java.lang.InterruptedException e){
                System.out.println(e);
            }
        }

        /** thread for requesting file **/
        public void run() {
            boolean received  = false;
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
                    // build new packet
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), udpReceivePort);
                    udpSocket.send(packet);
                    System.out.println("start waiting");
                    try {
                        Thread.sleep(500);
                    } catch(java.lang.InterruptedException e){
                        System.out.println(e);
                    }
                    File file = new File("../local/"+ saveName);
                    if (file.exists()){
                        System.out.println("receive success");
                        received = true;
                        //System.out.println("waiting ends");
                        break;
                    }

                }
                catch (java.io.IOException e1){
                    e1.printStackTrace();
                }
            }
            if(!received){
                System.out.println("file doesn't reach within timeout");
            }
        }
    }

    /** listen command to delete file **/
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

        /** run function for a file system delete file thread **/
        public void run(){
            System.out.println("start add delete listener");
            while (true) {
                Buffer = new byte[1024];
                String fileName = "", saveName = "", ip = "", tmp = "";
                long time = 0;
                DatagramPacket receivePacket = new DatagramPacket(Buffer, Buffer.length);
                try {
                    // receive commands
                    udpReceiveSocket.receive(receivePacket);
                    tmp = new String(Buffer);
                    //System.out.println("we receive a command " + tmp);
                    String [] data = tmp.split(" ");
                    // check for each state
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
                    // add new file
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
