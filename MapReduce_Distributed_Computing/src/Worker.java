package system;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.util.LinkedList;

/**
 * Created by Administrator on 2016/11/25.
 */
public class Worker implements Runnable{
    //private int listenPort = 60000;
    private int sendPort = 51000; // used to send ack to server
    private int SchedulerListenPort = 62000;
    private int workerListenPort = 63000;
    //private String MasterIp = "172.22.148.194";
    private String localPath = "../local/";
    private String intermediatePath = "../intermediate/";
    private String LogPath = "log.log";
    private DatagramSocket listenSocket, sendSocket;
    private byte[] buffer;
    private LinkedList<WorkerTask> tasks = new LinkedList<>(); // saves the task files, when a new task comes, put it into the file
    private boolean isMapping;
    public FileSystem fileSystem;
    private String currentTaskMasterIp;

    /** init a worker with file system **/
    public Worker(FileSystem f){
        buffer = new byte[1024];
        isMapping = false;
        fileSystem = f;
        try{
            sendSocket = new DatagramSocket(sendPort);
        }catch(java.net.SocketException e){
            System.out.println("build send socket failed " + e);
        }
        try{
            listenSocket = new DatagramSocket(workerListenPort);
        }catch(java.net.SocketException e){
            System.out.println("build listen socket failed " + e);
        }
    }

    /** this class is used to store worker task **/
    class WorkerTask{
        public String mode;
        public String exeFile;
        public String dataFile;
        public String prefix;
        public String masterIp;
        public boolean deleteData;

        WorkerTask(String data, String mid, String exe, String ip, String m){
            exeFile = exe;
            dataFile = data;
            prefix = mid;
            masterIp = ip;
            deleteData = false;
            // mode : maple or juice
            mode = m;
        }

        // delete is only for reduce task
        WorkerTask(String data, String mid, String exe, int delete, String ip, String m){
            exeFile = exe;
            dataFile = data;
            prefix = mid;
            masterIp = ip;
            /** in reduce task, may require delete intermediate files**/
            if(delete == 0){
                deleteData = false;
            }
            else{
                deleteData = true;
            }
            mode =  m;
        }
    }

    /** after work finish, send ack back to master **/
    private void sendWorkAck(String outputName, WorkerTask t){
        String mes = "";
        // ack  message
        mes = mes + t.prefix + " " + outputName + " " + t.dataFile + " ";
        //System.out.println("ack message : " + mes);
        byte [] buf = mes.getBytes();
        DatagramPacket p;
        InetAddress addr;
        try{
            addr = InetAddress.getByName(currentTaskMasterIp);
            p = new DatagramPacket(buf, buf.length, addr ,SchedulerListenPort);
            sendSocket.send(p);
        }catch(java.io.IOException e){
            System.out.println(e);
        }
    }

    /** execute a task with system command
     * the .class exe file is written in java
     * and called by this function**/
    private void execute(WorkerTask task){
        System.out.println("worker " + task.mode + " task start at " + new Date().getTime());
        String log = "";
        log  = log + new Date().getTime() + " ";
        log = log + task.mode + " " + task.prefix + " " + task.masterIp + " start";
        fileSystem.write(log); // write log
        String cmd = "", outputName = "", mode = "";
        if(task.mode == "maple"){
            mode = "Map";
        }
        else{
            mode = "Reduce";
        }
        //cmd = cmd + "java" + " " + "" + intermediatePath + task.exeFile + " "
        cmd = cmd + "java" + " " + task.exeFile + " "
                + (intermediatePath + task.dataFile) + " " + intermediatePath + task.prefix
                + task.dataFile + mode; // build cmd
        outputName = outputName + task.prefix +  task.dataFile + mode;
        //System.out.println(cmd);
        try{
            Process pro = Runtime.getRuntime().exec(cmd);
            pro.waitFor();
            InputStream in = pro.getInputStream();
            BufferedReader read = new BufferedReader(new InputStreamReader(in));
            String line = null;
            // print the message in the system command
            while((line = read.readLine())!=null){
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("execute file error");
        }
        fileSystem.sendFile(currentTaskMasterIp, outputName, outputName, 4); // intmid to intmid
        sendWorkAck(outputName, task);
        if(task.deleteData) {
            File f = new File(intermediatePath + task.dataFile);
            if (f.exists()) {
                f.delete();
            }
            System.out.println("data deleted as required");
        }
        log  = log + new Date().getTime() + " ";
        log = log + task.mode + " " + task.prefix + " " + task.masterIp + " ends";
        fileSystem.write(log);
        System.out.println("worker " + task.mode + " task ends at " + new Date().getTime());
    }

    /** this class is for map work or reduce work **/
    class MapWork implements Runnable{
        public MapWork(){

        }
        // this may acts as a thread
        public void run(){
            // modified on 2016 11 29, add sleep part to this thread
            WorkerTask t = null;
            while(true){
                synchronized (tasks){
                    if(tasks.size() != 0){
                        t = tasks.poll();
                    }
                }
                if(t != null){
                    // execute  task
                    currentTaskMasterIp = t.masterIp;
                    System.out.println("execute" + " " +  t.mode + " " +  "task " + t.dataFile + " " + t.masterIp);
                    // execute command
                    execute(t);
                    System.out.println("task " + t.dataFile + " finished");
                    t = null;
                }
                try {
                    Thread.sleep(100);
                } catch(java.lang.InterruptedException e){
                    System.out.println(e);
                }
            }
        }
        // here is the end of this class
    }

    /** this is the run function of Worker class
     * it receives command and put it into a queue
     * **/
    public void run(){
        DatagramPacket p;
        String [] command;
        String tmp;
        // start a map/reduce worker first
        MapWork work = new MapWork();
        Thread s = new Thread(work);
        s.start();
        try {
            p = new DatagramPacket(buffer, buffer.length);
            while (true) {
                listenSocket.receive(p);
                tmp = new String(buffer);
                //System.out.println("new command is " + tmp);
                command = tmp.split(" ");
                //the message is  mes = mes + "map" + " " + fileName + " " + task.interMediateName + " " + task.exeFile + " ";
                if(command[0].equals("maple")){
                    //build  map task
                    String ip = p.getAddress().toString();
                    ip = ip.substring(1);
                    System.out.println("new map request from " + ip);
                    WorkerTask t = new WorkerTask(command[1], command[2], command[3], ip, "maple");
                    synchronized (tasks) {
                        tasks.add(t);
                    }
                }
                else if(command[0].equals("juice")){
                    // build reduce task
                    String ip = p.getAddress().toString();
                    ip = ip.substring(1);
                    System.out.println("new reduce request from " + ip);
                    //the message is  mes = mes + task.mode + " " + fileName + " " + task.prefix + " " + task.exeFile + " " + task.deleteData + " ";
                    WorkerTask t = new WorkerTask(command[1], command[2], command[3], Integer.valueOf(command[4]) ,ip, "juice");
                    synchronized (tasks) {
                        tasks.add(t);
                    }
                }
            }
        }catch(java.io.IOException e){
            System.out.println(e);
        }
    }
}
