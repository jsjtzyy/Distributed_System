package system;

import java.io.*;
import java.net.Socket;
import java.nio.file.FileSystemException;

/**
 * Created by ghe10 on 10/14/16.
 */
public class StringSender extends tools implements Runnable{
    private Socket socket;
    private String pattern;
    private int BufferSize = 4096;

    public StringSender(Socket s, String p){
        //super(nodeTable, actionTable, deadTable);
        socket = s;
        pattern = p;
    }

    public void send(int num){
        int length = 0, l = 0;
        DataOutputStream fouts = null;
        FileInputStream fins = null;
        File file = new File(SendLogPath);
        byte[] sendByte = new byte[BufferSize];

        try {
            fins = new FileInputStream(file);
            fouts = new DataOutputStream(socket.getOutputStream());
            fouts.writeInt(num);
            while((length = fins.read(sendByte, 0, sendByte.length)) > 0){
                fouts.write(sendByte, 0, length);
                l += 1;
            }
            System.out.println("send bytes " + l);
        } catch (FileSystemException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("DataStream creation failed");
            e.printStackTrace();
        } finally{
            if(fins != null)
                try {
                    fins.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if(fouts != null)
                try {
                    fouts.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void run(){
        // int count = 0;
        System.out.println("stringSender starts");
        Runtime runtime = Runtime.getRuntime();
        String line = null;
        int num = 0;
        String [] c = pattern.split(" ");
        String [] cmd = {"grep","-c", c[c.length - 1], LogPath};
        selfSearch(pattern);
        try {
            Process p = runtime.exec(cmd);
            BufferedInputStream in = new BufferedInputStream(p.getInputStream());
            BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
            line = inBr.readLine();
            num = Integer.parseInt(line);
            System.out.println(num);
            send(num);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
