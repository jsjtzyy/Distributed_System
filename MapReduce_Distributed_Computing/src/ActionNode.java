package system;

import java.util.Date;

/**
 * Created by ghe10 on 10/14/16.
 */
public class ActionNode {
    public long findTime;
    public long actionTime;
    public String IP;
    public int Port;
    public int Action; // join 0, leave 1, failure 2
    public int count;

    ActionNode(String ip, int port, int action, long time){
        IP = ip;
        Port = port;
        Action = action;
        count = 0;
        findTime = new Date().getTime();
        actionTime = time;
    }
}
