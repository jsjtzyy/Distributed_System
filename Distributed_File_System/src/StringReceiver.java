package system;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by ghe10 on 10/14/16.
 */
public class StringReceiver{
    private Socket socket;
    private String path = "log"; //this specifies the oath we save received file
    private int index;
    private Node node;
    private int[] Results;
    private int BufSize = 4096;

    public StringReceiver(Socket s, Node n, int[] res, int i) {
        socket = s;
        node = n;
        Results = res;
        index = i;
    }

    /* this function receives data from another VM */
    public void receive() {
        DataInputStream dataIns;
        FileOutputStream fileOuts;
        long length = 0;
        byte[] inputBuf = new byte[BufSize];
        try {
            dataIns = new DataInputStream(socket.getInputStream());
            Results[index - 1] = dataIns.readInt();
            fileOuts = new FileOutputStream(path + (index) + ".log");
            fileOuts.write((node.Ip + " " + node.bornTime + "\n").getBytes());
            while ((length = dataIns.read(inputBuf, 0, BufSize)) > 0) {
                fileOuts.write(inputBuf, 0, (int) length);
                fileOuts.flush();
            }
        } catch (IOException ex0) {
			/* incorrect communication indicates that a machine failed */
            System.out.println(ex0);
            Results[index - 1] = -1;
            node.alive = false;
        }
    }
}