/**
 * Created by zhangyingyi on 16/12/2.
 */
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import java.util.*;
public class LinkGraph {

    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, Text>{

        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();
	private Text word2 = new Text();
        public void map(Object key, Text value, Context context
        ) throws IOException, InterruptedException {
            //String line = null;
            String []words = new String[2];
            int cnt = 0;
	    StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                /*insert code here*/
		words[cnt] = itr.nextToken();
		//cnt = (cnt + 1) % 2;
                //line = itr.nextToken();
               // words = line.split(" ");
               if(cnt == 1){
                word.set(words[1]);
		word2.set(words[0]);
                context.write(word, word2);
	       }
		cnt = (cnt + 1) % 2;
                //word.set(itr.nextToken());
                //context.write(word, one);
            }
        }
    }

    public static class IntSumReducer
            extends Reducer<Text,Text,Text, Text> {
        private IntWritable result = new IntWritable();
	private Text str = new Text();
        public void reduce(Text key, Iterable<Text> values,
                           Context context
        ) throws IOException, InterruptedException {
            //int sum = 0;
            String res = null;
            StringBuffer sb = new StringBuffer();
            for (Text val : values) {
                //sum += val.get();
                sb.append(val + " ");
            }
            res = sb.toString();
            res = "(" + res.trim() + ")";
	    str.set(res);
            //result.set(sum);
            context.write(key, str);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: LinkGraph <in> <out>");
            System.exit(2);
        }
        Job job = new Job(conf, "LinkGraph");
        job.setJarByClass(LinkGraph.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
