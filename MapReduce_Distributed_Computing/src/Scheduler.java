package system;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

/**
 * Created by Administrator on 2016/11/24.
 */
public class Scheduler implements Runnable{
    public FileSystem fileSystem;
    private String localPath = "../local/"; // this should be local file path
    private String intermediatePath = "../intermediate/";
    private LinkedList<MapTask> mapTaskQueue = new LinkedList<>(); // queue map when previous one not finished
    private Hashtable<String, String> MapResults = new Hashtable<>(); // <prefix_of_this_map, Key file>
    private LinkedList<String> splitedFiles = new LinkedList<>();
    private DatagramSocket assignSocket, receiveSocket, communicationSocket;
    // assign task, receive key, receive workers'map request
    private int schedulePort = 50000; // send schedule mes
    private int SchedulerListenPort = 62000;
    private int workerListenPort = 63000;
    private int communicationPort = 61000;
    private String schdeulerIp = "172.22.148.194";

    private int currentMapResults = 0; // records how many  maps have been finished in this map
    private int currentTotalMapNums = 0;
    public boolean isMaping = false;
    private LinkedList<String> detectedFailure = new LinkedList<>();
    private Hashtable<String, LinkedList<String>> currentAssignedTasks = new Hashtable<>();
    private Hashtable<String, Integer> currentFinishedTaskNums = new Hashtable<>();
    private int timer = 0; // wait a few seconds before reschedule
    //private LinkedList<String> assignedTasks = new LinkedList<>();

    /** build a scheduler **/
    public Scheduler(FileSystem fs){
        fileSystem = fs;
        try{
            // build sockets
            assignSocket = new DatagramSocket(schedulePort);
            receiveSocket = new DatagramSocket(SchedulerListenPort);
            communicationSocket = new DatagramSocket(communicationPort);
        }catch(IOException e){
            System.out.println(e);
        }
    }

    /** this buildTask is for maple *****************************************************/
    public void buildTask(String exe, int num_maps, String prefix, String sdfsName, int deleteData, String mode, int upload){
        // deleteData == 1 : delete data
        String log = "";
        String mes = "";
        DatagramPacket p;
        byte [] buf;
        InetAddress address;
        //if(fileSystem.selfIp.equals(schdeulerIp)) { // maybe we need to do sth with mode.....
        // everyone schedule its own task
        MapTask t = new MapTask(exe, num_maps, prefix, sdfsName, deleteData, mode, upload); // sdfsName is the data file name
        mapTaskQueue.add(t);
        System.out.println("build task " + mode);
        // write log
        log  = log + new Date().getTime() + " ";
        log = log + t.mode + " " + t.prefix + " "  + " received";
        fileSystem.write(log); // write log
        // write finish
    }

    /** this function is incase of input file is a folder **/
    public void buildTask(String exe, int num_maps, String prefix, String sdfsName, int deleteData, String mode, int upload, String isFolder){
        // deleteData == 1 : delete data
        String log = "";
        String mes = "";
        DatagramPacket p;
        byte [] buf;
        InetAddress address;
        //if(fileSystem.selfIp.equals(schdeulerIp)) { // maybe we need to do sth with mode.....
        // everyone schedule its own task
        MapTask t = new MapTask(exe, num_maps, prefix, sdfsName, deleteData, mode, upload, isFolder); // sdfsName is the data file name
        mapTaskQueue.add(t);
        System.out.println("build task " + mode);
        // write log
        log  = log + new Date().getTime() + " ";
        log = log + t.mode + " " + t.prefix + " "  + " received";
        fileSystem.write(log);
        // write finish
    }

    /** this function helps to upload the intermediate files.
     * since the number of files is large, we build a folder and
     * put everything into sdfs together**/
    private String createDir(String destDirName) {
        String folderName = destDirName;
        File dir = new File(localPath + destDirName);
        if (dir.exists()) {
            // folder already exist
            System.out.println(destDirName + "already exist");
            return destDirName;
        }
        if (!destDirName.endsWith(File.separator)) {
            destDirName = destDirName + File.separator;
        }
        //make dir
        if (dir.mkdirs()) {
            System.out.println("create" + destDirName + "success");
            return destDirName;
        } else {
            System.out.println("create" + destDirName + "failed！");
            return "";
        }
    }

    /** upload every file **/
    public void putIntermediateFiles(Hashtable<String, LinkedList<String>> table, String prefix){
        File out;
        String values = "";
        FileOutputStream os;
        LinkedList<String> fileNames = new LinkedList<>();
        // create folder
        String folderName = createDir(prefix + "Folder");
        for(String key : table.keySet()) {
            values = "";
            // output file
            out = new File(localPath + folderName + "/"  + key); // folder name doesn't contain  /
            fileNames.add(prefix + key);
            for(String value : table.get(key)){
                values = value + " ";
            }
            try {
                // creat and write to file
                out.createNewFile();
                os = new FileOutputStream(out);
                os.write(values.getBytes());
                os.close();
            } catch(java.io.IOException e){
                System.out.println(e);
            }
        }
        if(folderName.equals("")){
            System.out.println("create folder failed, abort upload");
        }
        else{
            fileSystem.putFile(folderName, folderName);
        }
        /*for(String name : fileNames){
            fileSystem.putFile(name, name);
        }*/
    }

    /** this is for juice
     * build juice task **/
    public void buildTask(String exe, int num_maps, String prefix, String sdfsName, String outputFile, int deleteData, String mode, int upload){
        // deleteData == 1 : delete data
        String log = "";
        String mes = "";
        DatagramPacket p;
        byte [] buf;
        InetAddress address;
        //if(fileSystem.selfIp.equals(schdeulerIp)) { // maybe we need to do sth with mode.....
        // everyone schedule its own task
        MapTask t = new MapTask(exe, num_maps, prefix, sdfsName, outputFile, deleteData, mode, upload); // sdfsName is the data file name
        mapTaskQueue.add(t);
        System.out.println("build task " + mode);
        // write log
        log  = log + new Date().getTime() + " ";
        System.out.println("task start at " + new Date().getTime());
        log = log + t.mode + " " + t.prefix + " "  + " finish";
        fileSystem.write(log);
        // write finish
    }

    /** add failure to a queue **/
    public void detectFailure(String ip){
        synchronized (detectedFailure){
            detectedFailure.add(ip);
            timer = 0;
        }
        System.out.println("scheduler knows failure of " + ip);
    }

    /** split the input file into sub files almost equally **/
    private LinkedList<String> splitFile(String fileName, int numOfMaps){
        //int nodeNum = fileSystem.NodeTable.size();
        long t = new Date().getTime();
        LinkedList<String> splitedFiles = new LinkedList<String>();
        ArrayList<FileOutputStream> outStreams = new ArrayList<FileOutputStream>();
        BufferedReader reader = null;
        int index = 0;
        String inputString = null;
        try{
            // load input file
            File inputFile = new File(localPath + fileName);
            System.out.println("file " + fileName +"  state " + inputFile.exists());
            reader = new BufferedReader(new FileReader(inputFile));
            // split according to num of maps
            for(int i = 0; i < numOfMaps; i++){
                splitedFiles.add(fileName + i);
                File f = new File(intermediatePath + fileName + i);
                f.createNewFile();
                FileOutputStream os = new FileOutputStream(f);
                outStreams.add(os);
            }
            // write to file
            while((inputString = reader.readLine()) != null){
                outStreams.get(index).write((inputString + '\n').getBytes());
                index++;
                index = index % numOfMaps;
            }
        }catch(FileNotFoundException e){
            System.out.println("split file failed " + e);
        }catch (java.io.IOException e1){
            System.out.println("split failed " + e1);
        }
        return splitedFiles;
    }

    /** schedule map data files to different workers
     * take alive worker number into account**/
    private Hashtable<String, LinkedList<String>> ScheduleMapData(LinkedList<String> splitedFiles, int numOfMaps){
        // assign tasks to node ips, return list of <ip, file>
        Hashtable<String, LinkedList<String>> res = new Hashtable<>(); // pair : ip, files
        LinkedList<String> files;
        ArrayList<String> ips;
        int sizeNodeNum, sizeFileNum;
        int count = 0;
        ips = new ArrayList<>(fileSystem.NodeTable.keySet());
        sizeNodeNum = ips.size();
        // check for alive worker num
        if(sizeNodeNum < numOfMaps){
            System.out.println("we don't have enough nodes, thus we choose " + sizeNodeNum + " as map num");
            numOfMaps = sizeNodeNum;
        }
        sizeFileNum = splitedFiles.size();

        // assign files
        for(int index = 0; ; index++){
            //Pair<String, String> p = new Pair<String, String>(ips.get(index), splitedFiles.get(i));
            /*if(ips.get(i).equals(fileSystem.selfIp)) {
                continue;
            }*/

            int i = index % ips.size();
            if(ips.get(i).equals(fileSystem.selfIp)){
                // do not assign task to master
                continue;
            }
            //assignedTasks.add(splitedFiles.get(count));
            // this worker already has a task
            if(res.containsKey(ips.get(i))) {
                files = res.get(ips.get(i));
                files.add(splitedFiles.get(count));
                res.put(ips.get(i), files);
                count++;
            }
            // this is this worker's first task
            else{
                files = new LinkedList<>();
                files.add(splitedFiles.get(count));
                res.put(ips.get(i), files);
                count++;
            }
            if(count == sizeFileNum) break;
        }
        return res;
    }

    /** schedule map data files to different workers
     * take alive worker number into account, use hash based strategy **/
    private Hashtable<String, LinkedList<String>> ScheduleMapDataWithHash(LinkedList<String> splitedFiles, int numOfMaps){
        // assign tasks to node ips, return list of <ip, file>
        Hashtable<String, LinkedList<String>> res = new Hashtable<>(); // pair : ip, files
        LinkedList<String> files;
        ArrayList<String> ips;
        int sizeNodeNum, sizeFileNum;
        int count = 0;
        ips = new ArrayList<>(fileSystem.NodeTable.keySet());
        sizeNodeNum = ips.size();
        // check for alive worker num
        if(sizeNodeNum < numOfMaps){
            System.out.println("we don't have enough nodes, thus we choose " + sizeNodeNum + " as map num");
            numOfMaps = sizeNodeNum;
        }
        sizeFileNum = splitedFiles.size();

        // assign files
        for(int index = 0; ; index++){
            //hash based assign
            String tmp = splitedFiles.get(count);
            int num = (int)tmp.charAt(0);
            int i = num % ips.size();
            if(ips.get(i).equals(fileSystem.selfIp)){
                // do not assign task to master
                if(ips.size() == 0){
                    System.out.println("no worker");
                    res = new Hashtable<>();
                    return res;
                }
                i = (num + 1) % ips.size();
            }
            // this worker already has a task
            if(res.containsKey(ips.get(i))) {
                files = res.get(ips.get(i));
                files.add(splitedFiles.get(count));
                res.put(ips.get(i), files);
                count++;
            }
            // this is this worker's first task
            else{
                files = new LinkedList<>();
                files.add(splitedFiles.get(count));
                res.put(ips.get(i), files);
                count++;
            }
            if(count == sizeFileNum) break;
        }
        return res;
    }

    /** tell workers the have task to do **/
    private void assignTask(String ip, String fileName, MapTask task){
        String mes = "";
        // build messages
        mes = mes + task.mode + " " + fileName + " " + task.prefix + " " + task.exeFile + " " + task.deleteData + " "; // file name is wrong
        // we don't need to tell them to add file to where
        byte [] buffer = new byte[1024];
        InetAddress address;
        try {
            // send messages
            address = InetAddress.getByName(ip);
            buffer = mes.getBytes();
            DatagramPacket p = new DatagramPacket(buffer, buffer.length, address, workerListenPort);
            assignSocket.send(p);
        }catch (java.io.IOException e) {
            System.out.println(e);
        }
        System.out.println("assign " + task.mode + " " + fileName + " to" + ip);
    }

    /** merge the output keys from workers **/
    private String mergeKeys( HashSet<String> keyFileNames, String prefix, String outputFileName, MapTask task){
        // read the key files and merge them together
        File out;
        if(outputFileName.equals("")) {
            out = new File(localPath + prefix + "MapResults"); // not intermediatePath
        }
        else{
            out = new File(localPath + outputFileName);
        }

        FileOutputStream os;
        BufferedReader reader = null;
        String inputString = "";
        Set<String> set;
        LinkedList<String> tmp;
        Hashtable<String, LinkedList<String>> allResults = new Hashtable<>(); // save all results: key, values" "valuse....
        try {
            // creat output file
            out.createNewFile();
            os = new FileOutputStream(out);
            for(String fileName : keyFileNames){
                reader = new BufferedReader(new FileReader(intermediatePath + fileName));
                // filePath is local file path, keys are saved there
                while((inputString = reader.readLine()) != null){
                    //set.add(inputString);
                    String []data = inputString.split(" ");
                    if(allResults.containsKey(data[0])){
                        tmp = allResults.get(data[0]);
                        tmp.add(data[1]);
                        allResults.put(data[0], tmp);
                    }
                    else{
                        tmp = new LinkedList<>();
                        tmp.add(data[1]);
                        allResults.put(data[0], tmp);
                    }
                }
            }
            set = allResults.keySet();
            // write to file
            for(String key : set){
                // write keys to a file
                tmp = allResults.get(key);
                os.write((key + " ").getBytes());
                for(String value : tmp){
                    os.write((value + " ").getBytes());
                }
                os.write(("\n").getBytes());
            }
        }catch(java.io.IOException e){
            System.out.println(e);
        }
        //System.out.println("merge finish, put file to SDFS");
        fileSystem.putFile(out.getName(), out.getName());
        isMaping = false; // to avoid reschedule
        if(task.upload) {
            // upload the intermediate files as required
            putIntermediateFiles(allResults, prefix);
        }
        System.out.println("we have " + allResults.keySet().size() + " keys in merge keys");
        return out.getName();
    }

    /** called by Map's run() function **/
    private void receiveMapResult(int mapNums, String prefix, String outputFileName, MapTask task){
        byte [] buffer = new byte[1024];
        DatagramPacket p = new DatagramPacket(buffer, buffer.length);
        String ack = "";
        HashSet<String> keyFileNames = new HashSet<>(); // saves the name of key files
        String totalKeyFile = "";
        String ackIp;
        currentTotalMapNums = mapNums;
        while(currentMapResults < mapNums) {
            //while(assignedTasks.size() > 0) {
            try {
                receiveSocket.receive(p);
                // this message should contain : key file name
                ack = new String(buffer);
                String[] messages = ack.split(" ");
                if(!messages[0].equals(prefix)){
                    System.out.println("wrong message " + ack);
                    continue;
                }
                keyFileNames.add(messages[1]);
                ackIp = p.getAddress().toString(); // is this correct??????
                ackIp = ackIp.substring(1);
                //System.out.println(ackIp);
                /*synchronized (assignedTasks){
                    assignedTasks.removeFirstOccurrence(messages[2]);
                }*/
                synchronized (currentFinishedTaskNums) {
                    // increase the finished Task *************************************************8
                    int tmp = currentFinishedTaskNums.get(ackIp);
                    //System.out.println(" in receiveMapResult " + ackIp);
                    System.out.println(" reveived ack : " + ack);
                    tmp++;
                    currentFinishedTaskNums.put(ackIp, tmp);
                }
                currentMapResults++;
            } catch (java.io.IOException e) {
                System.out.println(e);
            }
        }
        totalKeyFile = mergeKeys(keyFileNames, prefix, outputFileName, task);
        // we can check is we have a prefix in MapResults to judge whether a map is finished
        MapResults.put(prefix, totalKeyFile);
    }

    /** this is used for folder input data **/
    private String mergeFolder(String folder){
        String outName = folder + "InOneFile";
        File outputFile = new File(outName);
        // get the folder list
        File inputFolder = new File(localPath + folder);
        File[] files = inputFolder.listFiles(); // this might contains ../local/....
        try {
            outputFile.createNewFile();
            OutputStream os = new FileOutputStream(localPath + outName, true);
            BufferedReader reader;
            String inputString = "";
            for (File file : files) {
                System.out.println(file.getPath());
                reader = new BufferedReader(new FileReader(file.getPath()));
                while ((inputString = reader.readLine()) != null) {
                    //System.out.println(inputString);
                    os.write(inputString.getBytes());
                    os.write(("\n").getBytes());
                }
            }
        }catch(java.io.IOException e){
            System.out.println(e);
        }
        /*
        String cmd = "cat " + "./" + localPath + folder + "/* > " + "./" + localPath + outName;
        System.out.println(cmd);
        try {
            Process pro = Runtime.getRuntime().exec(cmd);
            pro.waitFor();
        } catch(java.io.IOException e){
            System.out.println(e);
        } catch(java.lang.InterruptedException e1){
            System.out.println(e1);
        }*/
        System.out.println("combine file folder complete " + outName); // outName is just a name , doesn't contain ../local/
        return outName;
    }

    /** build a map or reduce task **/
    class Map implements Runnable{
        public MapTask task;
        public Map(MapTask t){
            //assignedTasks = new LinkedList<>();
            task = t;
        }
        // clear vars for new task
        public void clear(){
            currentFinishedTaskNums.clear();
            splitedFiles.clear();
            currentAssignedTasks.clear();
        }

        public void run() {
            // the main difference of map and reduce is in assignTask function
            // split file
            File f1, f2;
            String log = "";
            clear();
            // write log to files
            log  = log + new Date().getTime() + " ";
            long time = new Date().getTime();
            System.out.println("scheduler " + task.mode + " task start at " + new Date().getTime());
            log = log + task.mode + " " + task.prefix + " "  + " start";
            fileSystem.write(log);
            // write log finish
            isMaping = true;
            currentMapResults = 0;
            String localName = task.sdfsName; // just a name, the filePath is written in splitFile
            fileSystem.request(task.sdfsName, task.sdfsName); // local
            fileSystem.request(task.exeFile + ".class", task.exeFile + ".class"); // MP4 folder
            f1 = new File(localPath + task.sdfsName);
            f2 = new File(task.exeFile + ".class");
            // check file states
            if(!(f1.exists() && f2.exists())){
                System.out.println("data file state " + f1.exists());
                System.out.println("exe file state: " + f2.exists());
                System.out.println("short of file, abort task");
                return;
            }
            // check if we have a folder as input
            if(task.isFolder){
                System.out.println("we have a folder");
                localName = mergeFolder(localName);
            }
            // split file
            splitedFiles = splitFile(localName, task.numMaps);
            Hashtable<String, LinkedList<String>> scheduledMapData = new Hashtable<>();
            int size = splitedFiles.size();
            scheduledMapData = ScheduleMapData(splitedFiles, task.numMaps);
            Set<String> set = scheduledMapData.keySet();
            for (String ip : set) {
                LinkedList<String> dataFiles = scheduledMapData.get(ip);
                // assign tasks
                currentAssignedTasks.put(ip, dataFiles);
                for (String file : dataFiles) {
                    // 3 is for data transmission mode
                    fileSystem.sendFile(ip,  file, file, 4); // intermid to intermid
                    fileSystem.sendFile(ip, task.exeFile + ".class", task.exeFile + ".class", 5);
                    // local  to MP4, send exe
                    // tell the node our task
                    currentFinishedTaskNums.put(ip, 0);
                    assignTask(ip, file, task);
                }
            }
            // wait for acks from workers
            receiveMapResult(size, task.prefix, task.OutputFile, task);
            // write log
            log  = log + new Date().getTime() + " ";
            //System.out.println("task ends at " + new Date().getTime());
            log = log + task.mode + " " + task.prefix + " "  + " finish";
            fileSystem.write(log);
            System.out.println("scheduler " + task.mode + " task takes " + (new Date().getTime() - time) + " time");
            // write log finish
            //System.out.println(task.mode + " task finish");
            //isMaping = false;
        }
    }

    /** reschedule some tasks to new workers **/
    private void reMap(String failedIp, MapTask task){
        // write log
        String log= "";
        log  = log + new Date().getTime() + " ";
        System.out.println("reMap start at " + new Date().getTime());
        //System.out.println("failed ip = " + failedIp + " " + currentAssignedTasks.containsKey(failedIp)); // false...

        log = log + task.mode + " " + task.prefix + " "  + " re-scheduled due to failure of " + failedIp;
        fileSystem.write(log);
        // write log finish
        Hashtable<String, LinkedList<String>> scheduledMapData;
        if(currentAssignedTasks.containsKey(failedIp)){
            LinkedList<String> files = currentAssignedTasks.get(failedIp);
            /*synchronized (currentAssignedTasks){
                currentAssignedTasks.remove(failedIp);
            }*/
            /*synchronized (assignedTasks){
                for(String f : files){
                    assignedTasks.removeFirstOccurrence(f);
                }
            }*/
            scheduledMapData = ScheduleMapData(files, files.size());
            LinkedList<String> list = new LinkedList<>(scheduledMapData.keySet());
            // shuffle the user list, random assign
            Collections.shuffle(list);
            // random re-assign
            for (String newIp : list) {
                LinkedList<String> dataFiles = scheduledMapData.get(newIp);
                System.out.println("we have " + dataFiles.size() + " files to reschedule");
                for (String file : dataFiles) {
                    //System.out.println("reschedule " + file + " to " + newIp);
                    // 3 is for data transmission mode
                    fileSystem.sendFile(newIp, file, file, 4);
                    fileSystem.sendFile(newIp, task.exeFile + ".class", task.exeFile + ".class", 5);
                    // tell the node our task
                    assignTask(newIp, file, task);
                    // update variables to record re-assigned tasks
                    synchronized (currentAssignedTasks){
                        if(currentAssignedTasks.containsKey(newIp)){
                            LinkedList<String> tmp = currentAssignedTasks.get(newIp);
                            tmp.add(file);
                            currentAssignedTasks.put(newIp, tmp);
                            System.out.println("re-assign" + file + " to " + newIp);
                        }
                        else{
                            LinkedList<String> tmp = new LinkedList<>();
                            tmp.add(file);
                            currentAssignedTasks.put(newIp, tmp);
                            System.out.println("re-assign" + file + " to " + newIp);
                        }
                        currentFinishedTaskNums.put(newIp, 0);
                    }
                }
            }
            System.out.println("re-assign finished");
            synchronized (currentFinishedTaskNums){
                currentFinishedTaskNums.remove(failedIp);
            }
        }
        else{
            System.out.println("this failure is not related to our current map");
        }
    }

    /** this is just used for scheduler **/
    /**
     class ListenToTasks implements Runnable{
     public void run(){
     byte [] buffer = new byte[1024];
     String tmp;
     String [] request;
     DatagramPacket p = new DatagramPacket(buffer, buffer.length);
     while (true){
     try {
     communicationSocket.receive(p);
     tmp = new String(buffer);
     request = tmp.split(" ");
     if(request[0].equals("mapRequest")){
     System.out.println("we have a map request " + tmp);
     MapTask m = new MapTask(request[1], Integer.valueOf(request[2]), request[3], request[4]);
     mapTaskQueue.add(m);
     }
     }catch(java.io.IOException e){
     System.out.println(e);
     }
     }
     }
     }
     **/

    /** the run function for the whole sechduler **/
    public void run() {
        MapTask currentTask = null;
        String failedIp = "";
        // listen on a port, deal with map request and reduce request
        /*if(fileSystem.selfIp.equals(schdeulerIp)){
            ListenToTasks t = new ListenToTasks();
            Thread tt = new Thread(t);
            tt.start();
        }*/
        while (true) {
            synchronized (detectedFailure) {
                if (timer == 10 && isMaping && detectedFailure.size() != 0) {
                    failedIp = detectedFailure.poll();
                    timer = 0;
                }
            }

            // the syn here is for safe update
            if (currentFinishedTaskNums.containsKey(failedIp)) {
                System.out.println("the failure of machine " + failedIp + " requires reschedule");
                int tmp = currentFinishedTaskNums.get(failedIp);
                // abort the work done by failed worker
                if(currentMapResults == currentTotalMapNums){
                    failedIp = "";
                    continue;
                }
                currentMapResults -= tmp;
                reMap(failedIp, currentTask);
                failedIp = "";
                //currentFinishedTaskNums.remove(failedIp);
            } else {
                //if (!failedIp.equals("")) {
                //System.out.println("the failure of machine " + failedIp + " is not related to current task");
                //}
            }

            // check if we have map/ reduce task
            synchronized (mapTaskQueue) {
                if (isMaping) {
                    // can't work for new task
                } else if (mapTaskQueue.size() != 0) {
                    currentTask = mapTaskQueue.poll();
                } else {
                    currentTask = null;
                }
            }
            // start new task
            if (!isMaping && currentTask != null) {
                Map m = new Map(currentTask);
                Thread s = new Thread(m);
                s.start();
                //map(currentTask); // assign map tasks
            }
            try {
                // check every 100ms about task and failure
                Thread.sleep(100);
                timer++;
            } catch (java.lang.InterruptedException e) {
                System.out.println(e);
            }
        }
    }
}
