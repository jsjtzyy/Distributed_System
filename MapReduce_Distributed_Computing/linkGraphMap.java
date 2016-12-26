import java.io.*;
import java.util.Hashtable;

/**
 * Created by zhangyingyi on 16/11/25.
 */
public class linkGraphMap {
    public static void main(String args[]){
        if(args.length == 0) return;
        if(args.length != 2){
            System.out.println("wrong number of arguments, supposed to be 2 arguments");
            return;
        }
        String inputfile = args[0];
        Hashtable<String, String> map = new Hashtable<>();
        String line = null;
        String []words = null;
        String tmpstr = null;
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(inputfile));
            line = in.readLine();
            while (line!=null)
            {
                words = line.split("    ");
                if(words.length != 2) {
                    System.out.println("Line is splitted by tab nor 4 single spaces, pleas fix the code and recompile it.");
                    break;
                }
                if(map.containsKey(words[1])){   // use the destination node as key
                    tmpstr = map.get(words[1]);
                    map.put(words[1], tmpstr + " " + words[0]);
                }else{
                    map.put(words[1], words[0]);
                }
                line=in.readLine();
            }
            in.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        try{
            BufferedWriter out = new BufferedWriter(new FileWriter(args[1] ));
            for(String str : map.keySet()){    // generate output file of map task
                out.write(str + " " + map.get(str));
                out.newLine();
            }
            out.close();
        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
