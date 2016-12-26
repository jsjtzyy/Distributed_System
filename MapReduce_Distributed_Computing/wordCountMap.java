import java.io.*;
import java.util.Hashtable;

/**
 * Created by zhangyingyi on 16/11/25.
 */
public class wordCountMap {
    public static void main(String args[]){
        if(args.length == 0) return;
        if(args.length != 2){
            System.out.println("wrong number of arguments, supposed to be 2 arguments");
            return;
        }
        String inputfile = args[0];
        Hashtable<String, Integer> map = new Hashtable<>();
        String line = null;
        String word = null;
        StringBuffer sb = new StringBuffer();
        char ch = ' ';
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(inputfile));
            line = in.readLine();
            while (line!=null)
            {
                for(int i = 0; i < line.length(); ++i){
                    ch = line.charAt(i);
                    // check if the character is digit or letter or hyphen
                    if(Character.isLetter(ch) || Character.isDigit(ch)||(ch == '-' && i + 1 < line.length() && Character.isLetter(line.charAt(i + 1)))){
                        sb.append(String.valueOf(ch));
                    }else if(sb.length() > 0){
                        word = sb.toString();
                        sb = new StringBuffer();
                        if(map.containsKey(word)){
                            map.put(word, map.get(word) + 1);
                        }else{
                            map.put(word, 1);
                        }
                    }
                }
                if(sb.length() > 0){
                    word = sb.toString();
                    if(map.containsKey(word)){
                        map.put(word, map.get(word) + 1);
                    }else{
                        map.put(word, 1);
                    }
                    sb = new StringBuffer();
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
            for(String str : map.keySet()){
                out.write(str + " " + map.get(str));   //generate output file of map task
                out.newLine();
            }
            out.close();
        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
