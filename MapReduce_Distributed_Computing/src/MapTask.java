package system;

/**
 * Created by Administrator on 2016/11/24.
 */

/** this class helps scheduler to memorize its tasks in a queue **/
public class MapTask {
    public String exeFile; // .class java file for tasks
    public int numMaps;
    public String prefix;
    public String sdfsName;
    public String OutputFile = "";
    public boolean finished = false;
    public int deleteData = 0; // delete intermediate data
    public String mode;
    public boolean isFolder;
    boolean upload;
    //exe, num_maps, prefix, sdfsName, deleteData, mode
    public MapTask(String exe, int num_maps, String mid, String inputName, int delete, String m, int up){
        exeFile = exe;
        numMaps = num_maps;
        prefix = mid;
        sdfsName = inputName;
        finished = false;
        deleteData = delete;
        mode = m; // mode : maple, juice
        OutputFile = "";
        upload = (up == 1); // check if we upload intermediate data
        isFolder = false;
    }

    /** this function is for folder input data **/
    public MapTask(String exe, int num_maps, String mid, String inputName, int delete, String m, int up, String isfolder){
        exeFile = exe;
        numMaps = num_maps;
        prefix = mid;
        sdfsName = inputName;
        finished = false;
        deleteData = delete;
        mode = m; // mode : maple, juice
        OutputFile = "";
        upload = (up == 1);
        isFolder = (isfolder.equals("true")); // check if we have a folder
    }

    public MapTask(String exe, int num_maps, String mid, String inputFile, String outputFile, int delete, String m, int up){
        // this is for reduce task
        exeFile = exe;
        numMaps = num_maps;
        prefix = mid;
        sdfsName = inputFile;
        finished = false;
        deleteData = delete;
        mode = m; // mode : maple, juice
        OutputFile = outputFile;
        upload = (up == 1);
        isFolder = false;
    }

    public MapTask(String exe, int num_maps, String mid, String inputFile, String outputFile, int delete, String m, int up, String isfolder){
        // this is for reduce task
        exeFile = exe;
        numMaps = num_maps;
        prefix = mid;
        sdfsName = inputFile;
        finished = false;
        deleteData = delete;
        mode = m; // mode : maple, juice
        OutputFile = outputFile;
        upload = (up == 1); // check if upload
        isFolder = (isfolder.equals("true")); // check if it is a folder
    }

}
