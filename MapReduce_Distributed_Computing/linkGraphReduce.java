import java.io.*;
import java.util.*;

/**
 * Created by zhangyingyi on 16/11/25.
 */
public class linkGraphReduce {
    public static void main(String args[]){
        if(args.length == 0) return;
        if(args.length != 2){
            System.out.println("wrong number of arguments, supposed to be 2 arguments");
            return;
        }
        String inputfile = args[0];
        HashSet<String> set = new HashSet<>();
        String line = null;
        String []words = null;
        StringBuffer sb = new StringBuffer();
        String strtmp = null;
        int index = 0;
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(inputfile));
            line = in.readLine();
            while (line!=null)
            {
                index = 0;
                while(index < line.length() && line.charAt(index) != ' ') ++index;
                if(index == line.length()) continue;   // avoid out of boundary
                strtmp = line.substring(index + 1);
                set.add(line.substring(0, index) + " " + "(" + strtmp + ")");  // append the value from intermediate file
                line=in.readLine();
            }
            in.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        try{
            BufferedWriter out = new BufferedWriter(new FileWriter(args[1] ));
            for(String str : set){
                out.write(str);         // generate output file of reduce work
                out.newLine();
            }
            out.close();
        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
