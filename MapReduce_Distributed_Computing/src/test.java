package system;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
/**
 * Created by Administrator on 2016/10/27.
 */
public class test {
    public String result = "";
    public int search(int m, int n) {
        //if (board == null) return 0;
        //int m = board.length;
        if (m == 0 || n == 0) return 0;
        //int n = board[0].length;
        int m1 = m < n ? m : n;
        int n1 = m < n ? n : m;
        int[][] dp = new int[m1][2];
        for (int i = 0; i < m1; i++) dp[i][0] = 1;
        dp[0][1] = 1;
        int last = 0, now = 1, tmp = -1;
        for (int i = 1; i < n1; i++) {
            for (int j = 1; j < m1; j++) {
                dp[j][now] = dp[j][last] + dp[j - 1][last] + dp[j - 1][now];
            }
            tmp = last; last = now; now = last;
        }
        return dp[m1 - 1][last];
    }


    public String op(String s){
        ArrayList<String> list = new ArrayList<String>();
        list.add("(");

        String tmp = "";
        char [] cs = s.toCharArray();
        int i = 0;
        while(i < cs.length){
            if(cs[i] == '('){
                if(!tmp.equals("")) {
                    list.add(tmp);
                }
                tmp = "(";
            }
            else if(cs[i] ==')'){
                String last = list.get(list.size() - 1);
                if(last.equals("(")){
                    list.add(tmp.substring(1));
                    tmp = "";
                }
                else{
                    tmp += cs[i];
                }
            }
            else{
                tmp += cs[i];
            }
            i++;
        }
        String res = "";
        for(int j = 1; j < list.size(); j++){
            res += list.get(j);
        }
        return res + tmp;
    }

    public int binarySearch(int [] nums, int target, int len){
        int l = 0, r = len - 1;
        int mid;
        while(l < r){
            mid = l + (r - l) / 2;
            if(target >= nums[mid]){ // since we must have a number smaller than target
                l = mid + 1;
            }
            else{
                r = mid - 1;
            }
        }
        return l;
    }

    public int LongestIncreasingSubstring(int [] data){
        int length = data.length;
        int [] lastNum = new int[length + 1];
        lastNum[0] = data[0];//Integer.MAX_VALUE;
        int max = 0;
        for(int i = 1; i < length; i++){

            if(data[i] > lastNum[max]){
                max++;
                lastNum[max] = data[i];
                //max = i;
            }
            else{
                int tmp = binarySearch(lastNum, data[i - 1], i);
                lastNum[tmp] = data[i];
            }
        }
        return max + 1;
    }

    static int maxLength(int[] a, int k) {
        int len = a.length;
        int low = 0, high = 0;
        int max = 0;
        int chars = 0;
        int Length = 0;
        while(a[low] > k && low < len){
            low++;
            high++;
        }
        if(low >= len) return 0;
        max = 1;
        chars = a[low];
        high++;
        while(high < len){
            if(chars + a[high] <= k){
                chars += a[high];
                Length = high - low + 1;
                max = Length > max ? Length : max;
                high++;
            }
            else{
                chars -= a[low];
                low++;
            }
        }
        return max;
    }

    public static void main(String args[]){
        //System.out.println(t.op("(a)b((c))"));
        //int [] data = {1};
        //int res = maxLength(data, 0);
        String res = "";
        long t = 1234567;
        res += 1 + " " + "a" +" " + t + " ";
        String [] data = res.split(" ");
        ArrayList<String> a = new ArrayList<>();
        a.add("sb");
        //System.out.print(Integer.valueOf(data[0]));
        //Hashtable<String, Long> a = new Hashtable<>();
        //System.out.println(a.get("a"));
        //System.out.println(t.search(3, 3));

        System.out.println(a.contains("sb"));
    }
}
