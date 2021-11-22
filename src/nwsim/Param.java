package nwsim;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import nwsim.env.*;
import nwsim.logger.NWLog;
import nwsim.network.ForwardHistory;
import nwsim.network.Packet;
import org.apache.commons.math3.random.RandomDataGenerator;

public class Param {

    /**
     * Singleton
     */
    protected static Param own;



    /**
     * 設定ファイルのオブジェクト
     */
    protected static Properties prop;

    protected static RandomDataGenerator rDataGen = new RandomDataGenerator();

    /**
     * 設定ファイルのパス
     */
    public static String configFile = "config.properties";

    /**
     * PC
     */
    public static int TYPE_COMPUTER = 1;

    /**
     * スイッチ
     */
    public static int TYPE_SWITCH = 2;

    /**
     * ルータ
     */
    public static int TYPE_ROUTER = 3;

    /**
     * サーバ
     */
    public static int TYPE_SERVER = 4;

    /**
     * PC数
     */
    public static long env_num_pc;

    /**
     * スイッチ数
     */
    // public static long env_num_switch;

    /**
     * ルータ数
     */
    public static long env_num_router;

    /**
     * PCの帯域幅配列(byte)
     */
    public static long[] bw_pc = { 10 * 1024 * 1024/8, 10 * 1024 * 1024/8, 100 * 1024 * 1024/8, 1000 * 1024 * 1024/8,
            10000 * 1024 * 1024/8, 25000 * 1024 * 1024/8, 40000 * 1024 * 1024/8 };

    /**
     * ルータの帯域幅配列(byte)
     */
    public static long[] bw_router = { 10 * 1024 * 1024/8, 10 * 1024 * 1024/8, 100 * 1024 * 1024/8, 1000 * 1024 * 1024/8,
            10000 * 1024 * 1024/8, 25000 * 1024 * 1024/8, 40000 * 1024 * 1024/8 };

    /**
     * リクエストタイプ（ここで定義してください）
     */
    public static int[] req_type = {Param.HTTP_GET_REQUEST, Param.HTTP_POST_REQUEST/*, Param.HTTP_GET_RESPONSE, Param.HTTP_GET_RESPONSE*/};
    public static final int HTTP_GET_REQUEST = 0;

    public static int http_get_request_size;

    public static final int  HTTP_POST_REQUEST = 1;
    public static long  http_post_request_limit;

    public static final int HTTP_GET_RESPONSE = 2;

    public static final int HTTP_POST_RESPONSE=3;



    /**
     * PCの帯域幅のインデックス
     */
    public static long env_num_pc_nw_min;

    /**
     * PCの帯域幅のインデックスのmu値
     */
    public static double env_num_pc_nw_mu;
    /**
     * 1ネットワークのPC数
     */
    public static long env_num_pc_nw_max;

    /**
     * PCの帯域幅のインデックス
     */
    public static long env_num_router_nw_min;

    /**
     * PCの帯域幅のインデックスのmu値
     */
    public static double env_num_router_nw_mu;
    /**
     * 1ネットワークのPC数
     */
    public static long env_num_router_nw_max;


    public static long bw_pc_min;
    public static long bw_pc_max;
    public static double bw_pc_mu;

    public static long bw_router_min;
    public static long bw_router_max;
    public static double bw_router_mu;

    public static int mtu;

    public static int delay_per_hop_min;
    public static int delay_per_hop_max;
    public static double delay_per_hop_mu;

    public static int router_nic_num_min;

    public static int router_nic_num_max;

    public static double router_nic_num_mu;

    public static long nic_buffer_size;

    //0: 10Mbps 1: 100MBps, 2: 1Gbps 3: 10Gbps 4: 25Gbps, 5: 40Gbps
    //単位はKByte
    public static long[] bw_list = {10*1024/8,100*1024/8,1024*1024/8,10*1024*1024/8,25*1024*1024/8,40*1024*1024/8 };

    public static String DEFAULT_ROUTE = "0.0.0.0";

    public static int ttl;

    public static long routeinfo_interval;

    public static long header_size;

    public static double request_exp_dist_lambda_min;

    public static double request_exp_dist_lambda_max;

    public static enum MSG_TYPE{
        IP, PING, TCP, ARP, HTTP_GET, HTTP_POST, CTRL
    }


    public static int routing_no;

    public  static int PACKET_START;

    public static int PACKET_MID;

    public static int PACKET_END;

    public static long router_queue_length;

    public static long http_get_response_size_min;
    public static long http_get_response_size_max;
    public static double http_get_response_size_mu;


    public static long http_post_request_size_min;
    public static long http_post_request_size_max;
    public static double  http_post_request_size_mu;

    public static long http_post_response_size;

    public static int PACKET_ACCEPT = 0;



    public static int PACKET_REJECT = 1;

    public static int PACKET_DROP = 2;

    public static int dnat_mode;


    /**
     * Destination NAT (ポート転送）
     */
    public static int PACKET_DNAT = 3;

    public static Param getIns() {
        if (Param.own == null) {
            Param.own = new Param();
        }
        return Param.own;
    }

    private Param() {
        Param.prop = new Properties();
        try {
            // 設定ファイルの読み込み
            Param.prop.load(new FileInputStream(Param.configFile));
            // 定数の設定
            Param.env_num_pc = Long.valueOf(Param.prop.getProperty("env_num_pc")).longValue();
            Param.env_num_router = Long.valueOf(Param.prop.getProperty("env_num_router")).longValue();
            Param.env_num_pc_nw_min = Long.valueOf(Param.prop.getProperty("env_num_pc_nw_min")).longValue();
            Param.env_num_pc_nw_max = Long.valueOf(Param.prop.getProperty("env_num_pc_nw_max")).longValue();
            Param.env_num_pc_nw_mu = Double.valueOf(Param.prop.getProperty("env_num_pc_nw_mu")).doubleValue();
            Param.bw_pc_min = Long.valueOf(Param.prop.getProperty("bw_pc_min")).longValue();
            Param.bw_pc_max = Long.valueOf(Param.prop.getProperty("bw_pc_max")).longValue();
            Param.bw_pc_mu = Double.valueOf(Param.prop.getProperty("bw_pc_mu")).doubleValue();

            Param.bw_router_min = Long.valueOf(Param.prop.getProperty("bw_router_min")).longValue();
            Param.bw_router_max = Long.valueOf(Param.prop.getProperty("bw_router_max")).longValue();
            Param.bw_router_mu = Double.valueOf(Param.prop.getProperty("bw_router_mu")).doubleValue();

            Param.mtu = Integer.valueOf(Param.prop.getProperty("mtu")).intValue();
            //02 start 
            Param.delay_per_hop_min = Integer.valueOf(Param.prop.getProperty("delay_per_hop_min")).intValue();
            Param.delay_per_hop_max = Integer.valueOf(Param.prop.getProperty("delay_per_hop_max")).intValue();
            Param.delay_per_hop_mu = Double.valueOf(Param.prop.getProperty("delay_per_hop_mu")).doubleValue();
            
            Param.router_nic_num_min = Integer.valueOf(Param.prop.getProperty("router_nic_num_min")).intValue();
            Param.router_nic_num_max = Integer.valueOf(Param.prop.getProperty("router_nic_num_max")).intValue();
            Param.router_nic_num_mu = Double.valueOf(Param.prop.getProperty("router_nic_num_mu")).doubleValue();
            Param.nic_buffer_size = Long.valueOf(Param.prop.getProperty("nic_buffer_size")).longValue();

            Param.ttl = Integer.valueOf(Param.prop.getProperty("ttl")).intValue();
            Param.env_num_router_nw_min = Long.valueOf(Param.prop.getProperty("env_num_router_nw_min")).longValue();
            Param.env_num_router_nw_max = Long.valueOf(Param.prop.getProperty("env_num_router_nw_max")).longValue();
            Param.env_num_router_nw_mu = Double.valueOf(Param.prop.getProperty("env_num_router_nw_mu")).doubleValue();
            Param.routeinfo_interval = Long.valueOf(Param.prop.getProperty("routeinfo_interval")).longValue();
            Param.header_size = Long.valueOf(Param.prop.getProperty("header_size")).longValue();
            Param.request_exp_dist_lambda_min = Double.valueOf(Param.prop.getProperty("request_exp_dist_lambda_min")).doubleValue();
            Param.request_exp_dist_lambda_max = Double.valueOf(Param.prop.getProperty("request_exp_dist_lambda_max")).doubleValue();
            Param.routing_no = Integer.valueOf(Param.prop.getProperty("routing_no")).intValue();
            Param.PACKET_START = 1;
            Param.PACKET_MID = 2;
            Param.PACKET_END = 3;
            Param.router_queue_length = Long.valueOf(Param.prop.getProperty("router_queue_length")).longValue();

            Param.http_get_request_size = Integer.valueOf(Param.prop.getProperty("http_get_request_size")).intValue();
            Param.http_get_response_size_min = Long.valueOf(Param.prop.getProperty("http_get_response_size_min")).longValue();
            Param.http_get_response_size_max = Long.valueOf(Param.prop.getProperty("http_get_response_size_max")).longValue();
            Param.http_get_response_size_mu = Double.valueOf(Param.prop.getProperty("http_get_response_size_mu")).doubleValue();


            Param.http_post_request_size_min = Long.valueOf(Param.prop.getProperty("http_post_request_size_min")).longValue();
            Param.http_post_request_size_max = Long.valueOf(Param.prop.getProperty("http_post_request_size_max")).longValue();
            Param.http_post_request_size_mu = Double.valueOf(Param.prop.getProperty("http_post_request_size_mu")).doubleValue();

            Param.http_post_response_size = Long.valueOf(Param.prop.getProperty("http_post_response_size")).longValue();
            //Param.http_post_request_limit = Long.valueOf(Param.prop.getProperty("http_post_request_limit")).longValue();
            Param.dnat_mode = Integer.valueOf(Param.prop.getProperty("dnat_mode")).intValue();
            //02 end
            
            // Param.env_num_switch =
            // Long.valueOf(Param.prop.getProperty("env_num_switch")).longValue();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 
     * @param idx
     * @return
     */
    public static long getPCBw(int idx) {
        return Param.bw_pc[idx - 1];
    }

    public static long getRouterBw(int idx) {
        return Param.bw_router[idx - 1];
    }

    public HashMap<String, Double> calcAvgBufferSize(Packet p){
        Iterator<ForwardHistory> reqIte = p.getRequestHistoryList().iterator();
        Iterator<ForwardHistory> resIte = p.getResponseHistoryList().iterator();
        HashMap<String, Double> retMap = new HashMap<String, Double>();

        long total_req_buffer = 0;
        long total_req_queue = 0;
        long total_res_buffer = 0;
        long total_res_queue = 0;
        long req_size = p.getRequestHistoryList().size();
        long res_size = p.getResponseHistoryList().size();

        while(reqIte.hasNext()){
            ForwardHistory req = reqIte.next();
            total_req_buffer += req.getReq_avg_receive_buffersize();
            total_req_queue += req.getReq_avg_queue_size();

        }
        if(res_size > 0){
            retMap.put("reqbuffer", Param.getRoundedValue(total_req_buffer/(double)req_size));
            retMap.put("reqqueue", Param.getRoundedValue(total_req_queue/(double)req_size));
        }else{
            retMap.put("reqbuffer", (double)0);
            retMap.put("reqqueue", (double)0);
        }


        while(resIte.hasNext()){
            ForwardHistory res = resIte.next();
            total_res_buffer += res.getRes_avg_receive_buffersize();
            total_res_queue += res.getRes_avg_queue_size();
        }
        if(res_size > 0){
            retMap.put("resbuffer", Param.getRoundedValue(total_res_buffer/(double)res_size));
            retMap.put("resqueue", Param.getRoundedValue(total_res_queue/(double)res_size));
        }else{
            retMap.put("resbuffer", (double)0);
            retMap.put("resqueue", (double)0);
        }


        return retMap;



    }



    /**
     * Int型の，一様／正規分布の乱数出力関数
     * 
     * @param min
     * @param max
     * @param dist
     * @param mu
     * @return
     */
    public static int genInt(int min, int max, int dist, double mu) {
        max = Math.max(min, max);

        if (min == max) {
            return min;
        }
        if (dist == 0) {
            // 一様分布
            return min + (int) (Math.random() * (max - min + 1));

        } else {
            // 正規分布
            double meanValue2 = min + (max - min) * mu;
            double sig = (double) (Math.max((meanValue2 - min), (max - meanValue2))) / 3;
            double ran2 = Param.getRoundedValue(Param.rDataGen.nextGaussian(meanValue2, sig));

            if (ran2 < min) {
                ran2 = (int) min;
            }

            if (ran2 > max) {
                ran2 = (int) max;
            }

            return (int) ran2;
        }

    }

    /**
     * Double型の，一様／正規分布の乱数出力関数
     * 
     * @param min
     * @param max
     * @param dist
     * @param mu
     * @return
     */
    public static double genDouble(double min, double max, int dist, double mu) {
        if (min == max) {
            return min;
        }
        if (dist == 0) {
            // 一様分布
            return min + (double) (Math.random() * (max - min + 1));

        } else {
            // 正規分布
            double meanValue2 = min + (max - min) * mu;
            double sig = (double) (Math.max((meanValue2 - min), (max - meanValue2))) / 3;
            double ran2 = Param.getRoundedValue(Param.rDataGen.nextGaussian(meanValue2, sig));

            if (ran2 < min) {
                ran2 = (double) min;
            }

            if (ran2 > max) {
                ran2 = (double) max;
            }

            return (double) ran2;
        }

    }

    /**
     * Long型の一様・正規分布の乱数出力関数
     * 
     * @param min
     * @param max
     * @param dist
     * @param mu
     * @return
     */
    public static long genLong(long min, long max, int dist, double mu) {
        if (min == max) {
            return min;
        }
        if (dist == 0) {
            // 一様分布
            return min + (long) (Math.random() * (max - min + 1));

        } else {
            // 正規分布
            double meanValue2 = min + (max - min) * mu;
            double sig = (double) (Math.max((meanValue2 - min), (max - meanValue2))) / 3;
            double ran2 = Param.getRoundedValue(Param.rDataGen.nextGaussian(meanValue2, sig));

            if (ran2 < min) {
                ran2 = (double) min;
            }

            if (ran2 > max) {
                ran2 = (double) max;
            }

            return (long) ran2;
        }

    }

    /**
     * 浮動小数点を，少数第4位を四捨五入して，第3位の値に変換するメソッド． オーバーフローを防ぐためのメソッドです．
     * 
     * @param value1
     * @return
     */
    public static double getRoundedValue(double value1) {
        // try{
        BigDecimal value2 = new BigDecimal(String.valueOf(value1));
        double retValue = value2.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
        return retValue;

    }




    /**
     * NWアドレス計算（updated)
     * @param ip IPアドレス
     * @param sb サブネットマスク
     * @return
     * @throws UnknownHostException
     */
    public static String getNWAddress(String ip, String sb) {
        try{
            byte[] bIP = InetAddress.getByName(ip).getAddress();
            byte[] bSB = InetAddress.getByName(sb).getAddress();
            byte[] bNT = new byte[4];

            for(int i = 0;i<bIP.length;i++) {
                bNT[i] = (byte) (bIP[i] & bSB[i]);
            }
            String tmp = InetAddress.getByAddress(bNT).toString();
            //System.out.println(tmp);
            tmp = tmp.replace("/","");
            return tmp;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 通信時間をミリ秒で返します．
     *
     * @param p
     * @param fromNic
     * @param toNic
     * @return
     */
    public double calcComTime(Packet p, Nic fromNic, Nic toNic){
        long bw = p.getMinBW();
        long size = 0;
        if(p.isRequest()){
            size = p.getReqPacketSize();
        }else{
            size = p.getTotalDataSize();
        }
        double  comTime = (1024/1000) * Param.getRoundedValue(size / (double)bw);
        return comTime;
    }



    public String calcGW(){
        StringBuffer buf = new StringBuffer();

        for(int i=0;i<4;i++){
            int elem = Param.genInt(1, 191, 0,0);

            if(i == 3){
                elem = 1;
            }
            buf.append(elem);
            if(i < 3){
                buf.append(".");
            }

        }
        return buf.toString();
    }

    public static String genGWAddress(){
        String val = Param.getIns().calcGW();
        while(Env.getIns().getGwIPSet().contains(val)){
            val = Param.getIns().calcGW();

        }
        Env.getIns().getGwIPSet().add(val);
        return val;


    }

    public void tranLog(Packet p, String result){
        Statistics stat = Env.getIns().findStat(p.getTranID());
        stat.setResFinishTime(System.currentTimeMillis());
        long totalReqTime = stat.getReqFinishTime() - stat.getReqStartTime() + p.getTotalDelay();
        long totalResTime = stat.getResFinishTime() - stat.getResStartTime() + p.getTotalDelay();
        double req_avg_buffer_size = Param.getRoundedValue(stat.getTotalReqReceiveBufferSize()
                / (double)(p.getRequestHistoryList().size() * stat.getReqCount()+1));
        double res_avg_buffer_size = Param.getRoundedValue(stat.getTotalResReceiveBufferSize()
                / (double)(p.getResponseHistoryList().size() * stat.getResCount()+1));

        double req_avg_queue_size = Param.getRoundedValue(stat.getTotalReqQueueSize()
                / (double)(p.getRequestHistoryList().size() * stat.getReqCount()+1));
        double res_avg_queue_size = Param.getRoundedValue(stat.getTotalResQueueSize()
                / (double)(p.getResponseHistoryList().size() * stat.getResCount()+1));
        String msg = ","+p.getType()+","+p.getTranID() + ","+ p.getRequestHistoryList().getFirst().getFromID() + ","+ p.getRequestHistoryList().getLast().getToID() + ","
                + p.getMinBW()*8/(1024) + ","+
                totalReqTime + "," + p.getRequestHistoryList().size() +
                ","+ req_avg_buffer_size + "," +stat.getMaxReqReceiveBufferSize() + "," + req_avg_queue_size + "," +stat.getMaxReqQueueSize() + "," +p.getReqPacketSize() + "," +
                totalResTime+","+p.getResponseHistoryList().size()+
                ","+ res_avg_buffer_size + "," + stat.getMaxResReceiveBufferSize() + ", " + res_avg_queue_size + "," + stat.getMaxResQueueSize() + ", "+p.getTotalDataSize() + ","+ result;

        NWLog.getIns().log(msg);
    }



    /**
     *
     * @param ip
     * @param val 10進数で，1~255の値を指定してください．
     * @return
     */
    public static String incrementIP(String ip, int val){
        try{
            String v = "0.0.0."+val;
            byte[] b_val = InetAddress.getByName(v).getAddress();
            byte[] b_ip = InetAddress.getByName(ip).getAddress();
            byte[] bNT = new byte[4];
            StringBuffer buf = new StringBuffer();
            int b_len = b_ip.length;
            for(int i = 0;i<b_len;i++) {
                bNT[i] = (byte) (b_ip[i] + b_val[i]);

            }

            String tmp = InetAddress.getByAddress(bNT).toString();
            //String tmp = buf.toString();
            tmp = tmp.replace("/","");
            return tmp;

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;

    }



    public static int getNWPart(int bit){
        int val = 0;
        for(int i=0;i<bit;i++){
            val +=  Math.pow(2, 7-i);
        }
        return val;
    }

    /**
     * ホスト部のビット数から，サブネットアドレスを取得します．
     * @param hostBit　ホスト部のビット数
     * @return
     */
    public static String getSubNetMask(int hostBit){
       // StringTokenizer st1 = new StringTokenizer(ip, ".");
        int loopCnt = (32 - hostBit)/8;
        StringBuffer retStr;
        int cnt = 0;
        //while(st1.hasMoreTokens()){
        //int pip1 = Integer.valueOf(st1.nextToken()).intValue();
        if(hostBit >= 25){
            int nw_part_bit = 32 - hostBit;
            int nw_part = Param.getNWPart(nw_part_bit);
            retStr = new StringBuffer(nw_part);
            retStr.append(".0.0.0");
            return retStr.toString();
        }
        //int pip2 = Integer.valueOf(st1.nextToken()).intValue();
        if(hostBit >= 17){
            retStr = new StringBuffer("255");
            retStr.append(".");
            int nw_part_bit = 24 - hostBit;
            int nw_part = Param.getNWPart(nw_part_bit);
            retStr.append(nw_part);
            retStr.append(".0.0");
            return retStr.toString();
        }
        //int pip3 = Integer.valueOf(st1.nextToken()).intValue();

        if(hostBit >= 9){
            retStr = new StringBuffer("255.255.");
            int nw_part_bit = 16 - hostBit;
            int nw_part = Param.getNWPart(nw_part_bit);
            retStr.append(nw_part);
            retStr.append(".0");
            return retStr.toString();
        }
       // int pip4 = Integer.valueOf(st1.nextToken()).intValue();

        if(hostBit >= 0){
            retStr = new StringBuffer("255.255.255.");
            int nw_part_bit = 8 - hostBit;
            int nw_part = Param.getNWPart(nw_part_bit);
            retStr.append(nw_part);
            return retStr.toString();
        }
        return null;


    }



    public static Properties getProp() {
        return prop;
    }

    public static void setProp(Properties prop) {
        Param.prop = prop;
    }

    public static String getConfigFile() {
        return configFile;
    }

    public static void setConfigFile(String configFile) {
        Param.configFile = configFile;
    }

}
