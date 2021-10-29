import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.map.InverseMapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.HashPartitioner;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.StringUtils;


public class WordCount {

    public static class TokenizerFileMapper
            extends Mapper<Object, Text, Text, IntWritable>{
        static enum CountersEnum { INPUT_WORDS }
        private final static IntWritable one = new IntWritable(1); // map输出的value
        private Text word = new Text(); // map输出的key
        private Set<String> wordsToSkip = new HashSet<String>();   //停词
        private Set<String> punctuations = new HashSet<String>(); // 标点符号
        private Configuration conf;
        private BufferedReader fis; // 文件输入流

        @Override
        public void setup(Context context) throws IOException,
                InterruptedException {
            conf = context.getConfiguration();

            if (conf.getBoolean("wordcount.skip.patterns", false)) {
                URI[] patternsURIs = Job.getInstance(conf).getCacheFiles();
                Path patternsPath = new Path(patternsURIs[0].getPath());
                String patternsFileName = patternsPath.getName().toString();
                parseSkipFile(patternsFileName); // 将停词放入需要跳过的范围
                Path punctuationsPath = new Path(patternsURIs[1].getPath());
                String punctuationsFileName = punctuationsPath.getName().toString();
                parseSkipPunctuations(punctuationsFileName); // 将标点符号放入需要跳过的范围
            }
        }

        private void parseSkipFile(String fileName) {//parseSkip函数处理两个停词文件，分析并加入wordstoSkip与punctuations的集合
            try {
                fis = new BufferedReader(new FileReader(fileName));
                String temp = null;
                while ((temp = fis.readLine()) != null) {
                    wordsToSkip.add(temp);
                }
            } catch (IOException ioe) {
                System.err.println("Error parsing the file " + StringUtils.stringifyException(ioe));
            }
        }
        private void parseSkipPunctuations(String fileName) {
            try {
                fis = new BufferedReader(new FileReader(fileName));
                String temp= null;
                while ((temp = fis.readLine()) != null) {
                    punctuations.add(temp);
                }
            } catch (IOException ioe) {
                System.err.println("Error parsing the file" + StringUtils.stringifyException(ioe));
            }
        }
        //以上预处理结束
        @Override
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            FileSplit fileSplit = (FileSplit)context.getInputSplit();
            String fileName = fileSplit.getPath().getName();
            String line = value.toString().toLowerCase();
            for (String pattern : punctuations) { // 将数据中所有满足patternsToSkip的pattern都过滤掉, replace by ""
                line = line.replaceAll(pattern, " ");
            }
            StringTokenizer itr = new StringTokenizer(line);
            while (itr.hasMoreTokens()) {
                String temp = itr.nextToken(); //从迭代器中读取单词
                if(temp.length()>=3&&!Pattern.compile("^[-\\+]?[\\d]*$").matcher(temp).matches()&&!wordsToSkip.contains(temp)) {
                    word.set(temp+"#"+fileName);
                    context.write(word, one);
                    Counter counter = context.getCounter(
                            CountersEnum.class.getName(),
                            CountersEnum.INPUT_WORDS.toString());
                    counter.increment(1);
                }
            }
        }
    }
    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, IntWritable>{
        static enum CountersEnum { INPUT_WORDS }
        private final static IntWritable one = new IntWritable(1); // map输出的value
        private Text word = new Text(); // map输出的key
        private Set<String> wordsToSkip = new HashSet<String>();   //停词
        private Set<String> punctuations = new HashSet<String>(); // 标点符号
        private Configuration conf;
        private BufferedReader fis; // 文件输入流

        @Override
        public void setup(Context context) throws IOException,
                InterruptedException {
            conf = context.getConfiguration();

            if (conf.getBoolean("wordcount.skip.patterns", false)) {
                URI[] patternsURIs = Job.getInstance(conf).getCacheFiles();
                Path patternsPath = new Path(patternsURIs[0].getPath());
                String patternsFileName = patternsPath.getName().toString();
                parseSkipFile(patternsFileName); // 将停词放入需要跳过的范围
                Path punctuationsPath = new Path(patternsURIs[1].getPath());
                String punctuationsFileName = punctuationsPath.getName().toString();
                parseSkipPunctuations(punctuationsFileName); // 将标点符号放入需要跳过的范围
            }
        }
        private void parseSkipFile(String fileName) {//parseSkip函数处理两个停词文件，分析并加入wordstoSkip与punctuations的集合
            try {
                fis = new BufferedReader(new FileReader(fileName));
                String temp = null;
                while ((temp = fis.readLine()) != null) {
                    wordsToSkip.add(temp);
                }
            } catch (IOException ioe) {
                System.err.println("Error parsing the file " + StringUtils.stringifyException(ioe));
            }
        }
        private void parseSkipPunctuations(String fileName) {
            try {
                fis = new BufferedReader(new FileReader(fileName));
                String temp= null;
                while ((temp = fis.readLine()) != null) {
                    punctuations.add(temp);
                }
            } catch (IOException ioe) {
                System.err.println("Error parsing the file" + StringUtils.stringifyException(ioe));
            }
        }
        //以上预处理结束
        @Override
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            FileSplit fileSplit = (FileSplit)context.getInputSplit();
            String fileName = fileSplit.getPath().getName();
            String line = value.toString().toLowerCase();
            for (String pattern : punctuations) { // 将数据中所有满足patternsToSkip的pattern都过滤掉, replace by ""
                line = line.replaceAll(pattern, " ");
            }
            StringTokenizer itr = new StringTokenizer(line);
            while (itr.hasMoreTokens()) {
                String temp = itr.nextToken(); //从迭代器中读取单词
                if(temp.length()>=3&&!Pattern.compile("^[-\\+]?[\\d]*$").matcher(temp).matches()&&!wordsToSkip.contains(temp)) {
                    word.set(temp);
                    context.write(word, one);
                    Counter counter = context.getCounter(
                            TokenizerFileMapper.CountersEnum.class.getName(),
                            TokenizerFileMapper.CountersEnum.INPUT_WORDS.toString());
                    counter.increment(1);
                }
            }
        }
    }

    public static class IntSumReducer extends Reducer<Text,IntWritable,Text,IntWritable> {
        private IntWritable result = new IntWritable();
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }
    //以上结束词频统计工作
    private static class IntWritableDecreasingComparator extends IntWritable.Comparator {
        public int compare(WritableComparable a, WritableComparable b) {
            return -compare(a, b);
        }
        public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
            return -compare(b1, s1, l1, b2, s2, l2);
        }
    }

    public static class SortFileReducer extends Reducer<IntWritable, Text, Text, NullWritable>{
        private MultipleOutputs<Text,NullWritable> mos;
        protected void setup(Context context) throws IOException,InterruptedException {
            mos = new MultipleOutputs<Text, NullWritable>(context);
        }
        protected void cleanup(Context context) throws IOException,InterruptedException {
            mos.close();
        }
        private Text result = new Text();
        private HashMap<String, Integer> map = new HashMap<>();
        @Override
        protected void reduce(IntWritable key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException{
            for(Text val: values){
                String docId = val.toString().split("-")[1];
                docId = docId.substring(0, docId.length()-4);
                docId = docId.replaceAll("-", "");
                String oneWord = val.toString().split("-")[0];
                int sum = map.values().stream().mapToInt(i->i).sum();
                if(sum>=4000) {//40*100，说明每个文件都已经统计出前100词频
                    break;
                }
                int rank = map.getOrDefault(docId, 0);
                if(rank < 100){
                    rank += 1;
                    map.put(docId, rank);
                }
                result.set(oneWord.toString());
                String str=rank+": "+result+", "+key;
                mos.write(docId, new Text(str), NullWritable.get() );
            }
        }
    }

    public static class SortAllReducer extends Reducer<IntWritable, Text, Text, NullWritable>{
        private Text result = new Text();
        int rank=1;

        @Override
        protected void reduce(IntWritable key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException{
            for(Text val: values){
                if(rank<=100) {
                    result.set(val.toString());
                    String str = rank + ": " + result + ", " + key;
                    rank++;
                    context.write(new Text(str), NullWritable.get());
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        GenericOptionsParser optionParser = new GenericOptionsParser(conf, args);
        String[] remainingArgs = optionParser.getRemainingArgs();
        if ((remainingArgs.length != 2) && (remainingArgs.length != 5)) {
            System.err.println("Check your parameters");
            System.exit(2);
        }
        Job job = Job.getInstance(conf, "word count");//单文件词频统计
        job.setJarByClass(WordCount.class);
        job.setMapperClass(TokenizerFileMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        List<String> otherArgs = new ArrayList<String>();
        for (int i = 0; i < remainingArgs.length; ++i) {
            if ("-skip".equals(remainingArgs[i])) {
                job.addCacheFile(new Path(remainingArgs[++i]).toUri());
                job.addCacheFile(new Path(remainingArgs[++i]).toUri());
                job.getConfiguration().setBoolean("wordcount.skip.patterns", true);
            } else {
                otherArgs.add(remainingArgs[i]);
            }
        }
        FileInputFormat.addInputPath(job, new Path(otherArgs.get(0)));
        Path tempDir = new Path("temp-part" );
        FileOutputFormat.setOutputPath(job, tempDir);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);
        if(job.waitForCompletion(true))
        {
            Job sortJob = Job.getInstance(conf, "sort file");//单文件词频排序
            sortJob.setJarByClass(WordCount.class);
            FileInputFormat.addInputPath(sortJob, tempDir);
            sortJob.setInputFormatClass(SequenceFileInputFormat.class);
            sortJob.setMapperClass(InverseMapper.class);
            sortJob.setReducerClass(SortFileReducer.class);
            Path tempFileDir = new Path("single-output" );
            FileOutputFormat.setOutputPath(sortJob, tempFileDir);
            List<String> fileNameList = Arrays.asList("shakespearealls11", "shakespeareantony23", "shakespeareas12",
                    "shakespearecomedy7", "shakespearecoriolanus24", "shakespearecymbeline17", "shakespearefirst51",
                    "shakespearehamlet25", "shakespearejulius26", "shakespeareking45", "shakespearelife54",
                    "shakespearelife55", "shakespearelife56", "shakespearelovers62", "shakespeareloves8",
                    "shakespearemacbeth46", "shakespearemeasure13", "shakespearemerchant5", "shakespearemerry15",
                    "shakespearemidsummer16", "shakespearemuch3", "shakespeareothello47", "shakespearepericles21",
                    "shakespearerape61", "shakespeareromeo48", "shakespearesecond52", "shakespearesonnets59",
                    "shakespearesonnets", "shakespearetaming2", "shakespearetempest4", "shakespearethird53",
                    "shakespearetimon49", "shakespearetitus50", "shakespearetragedy57", "shakespearetragedy58",
                    "shakespearetroilus22", "shakespearetwelfth20", "shakespearetwo18", "shakespearevenus60",
                    "shakespearewinters19");
            for (String fileName : fileNameList) {
                MultipleOutputs.addNamedOutput(sortJob, fileName, TextOutputFormat.class,Text.class, NullWritable.class);
            }
            sortJob.setOutputKeyClass(IntWritable.class);
            sortJob.setOutputValueClass(Text.class);
            sortJob.setSortComparatorClass(IntWritableDecreasingComparator.class);
            if(sortJob.waitForCompletion(true)){
                Job allSortJob = Job.getInstance(conf, "all word count");//总文件词频统计
                allSortJob.setJarByClass(WordCount.class);
                allSortJob.setMapperClass(TokenizerMapper.class);
                allSortJob.setCombinerClass(IntSumReducer.class);
                allSortJob.setReducerClass(IntSumReducer.class);
                allSortJob.setOutputKeyClass(Text.class);
                allSortJob.setOutputValueClass(IntWritable.class);
                for (int i = 0; i < remainingArgs.length; ++i) {
                    if ("-skip".equals(remainingArgs[i])) {
                        allSortJob.addCacheFile(new Path(remainingArgs[++i]).toUri());
                        allSortJob.addCacheFile(new Path(remainingArgs[++i]).toUri());
                        allSortJob.getConfiguration().setBoolean("wordcount.skip.patterns", true);
                    } else {
                        otherArgs.add(remainingArgs[i]);
                    }
                }
                FileInputFormat.addInputPath(allSortJob, new Path(otherArgs.get(0)));
                Path tempAllDir = new Path("temp-all" );
                FileOutputFormat.setOutputPath(allSortJob, tempAllDir);
                allSortJob.setOutputFormatClass(SequenceFileOutputFormat.class);
                if(allSortJob.waitForCompletion(true)){
                    Job sortJob1 = Job.getInstance(conf, "sort all");//总文件词频排序
                    sortJob1.setJarByClass(WordCount.class);
                    FileInputFormat.addInputPath(sortJob1, tempAllDir);
                    sortJob1.setInputFormatClass(SequenceFileInputFormat.class);
                    //map后交换key和value
                    sortJob1.setMapperClass(InverseMapper.class);
                    sortJob1.setReducerClass(SortAllReducer.class);
                    FileOutputFormat.setOutputPath(sortJob1, new Path(otherArgs.get(1)));
                    sortJob1.setOutputKeyClass(IntWritable.class);
                    sortJob1.setOutputValueClass(Text.class);
                    sortJob1.setSortComparatorClass(IntWritableDecreasingComparator.class);
                    System.exit(sortJob1.waitForCompletion(true) ? 0 : 1);
                }
            }
        }
    }
}