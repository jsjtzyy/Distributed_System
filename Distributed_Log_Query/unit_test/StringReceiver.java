package system;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class StringReceiver {
	private Socket socket;
	private String path = "../log"; // needs to be sepcified
	private int index;
	private Node node;
	private int[] Results;
	private int BufSize = 4096;

	public StringReceiver(Socket s, Node n, int []res, int i){
		socket = s;
        node = n;
        Results = res;
		index = i;
	}
	
	public void receive(){
		DataInputStream dataIns;
		FileOutputStream fileOuts;
		long length = 0;
		byte[] inputBuf = new byte[BufSize];
		try{
			dataIns = new DataInputStream(socket.getInputStream());

			System.out.println("Start receiving file");
			Results[index - 1] = dataIns.readInt();
			/*fileName = dataIns.readUTF();
			System.out.println(fileName);*/
			fileOuts = new FileOutputStream(path + (index) + ".log");
			/*fileLength = dataIns.readLong();
			System.out.println(fileLength);*/

			while((length = dataIns.read(inputBuf, 0, BufSize)) > 0){
				fileOuts.write(inputBuf, 0, (int)length);
				fileOuts.flush();
			}
			System.out.println("Receive finished");
		} catch(IOException ex0){
			System.out.println(ex0);
			Results[index - 1] = -1;
			node.alive = false;
		}

	}
}
