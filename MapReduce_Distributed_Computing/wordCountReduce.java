import java.io.*;
import java.util.*;

/**
 * Created by zhangyingyi on 16/11/25.
 */
public class wordCountReduce {
    public static void main(String args[]){

        HashSet<String> set = new HashSet<>();
        String res = null;
        // first argument is input file name, second argument is output file name
          if(args.length == 2){
                computeTotalInFile(args[0], set);
            try{
                BufferedWriter out = new BufferedWriter(new FileWriter(args[1]));
                for(String str : set){
                    out.write(str);
                    out.newLine();
                }
                out.close();
            }catch (IOException e)
            {
                e.printStackTrace();
            }
        }else{
            System.out.println("Number of input arguments is incorrect, supposed to be 2.");
        }
    }
    // Count all the sum for a single key
    private static void computeTotalInFile(String fileName, HashSet<String> set){
        String line = null;
        String[] words = null;
        int cnt = 0;
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            line = in.readLine();
            while (line!=null)
            {
                cnt = 0;
                words = line.split(" ");
                for(int i = words.length - 1; i >= 1; --i)
                    cnt += Integer.parseInt(words[i]);
                set.add(words[0] + " " + cnt);
                line=in.readLine();
            }
            in.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
