package nwsim.network;


import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Hidehiro Kanemitsu
 */
public class ForwardHistory implements Serializable {
    /**
     * ルータID or ノードID
     */
    private String fromID;

    private String fromIP;

    private String fromMac;


    //宛先のID（送出時にセットされる）
    private String  toID;

    private String toIP;

    private String toMac;



    //送出時刻（送出時にInterestパケットに設定される）
    private long startTime;

    //到着時刻（相手に到着時に，セットされる）
    private long arrivalTime;

    private long maxConnectionNum;

    /**
     * 要求時の平均受信バッファサイズ
     */
    private long req_avg_receive_buffersize;



    /**
     * 要求時の平均キューサイズ
     */
    private long req_avg_queue_size;



    /**
     * 応答時の平均受信バッファサイズ
     */
    private long res_avg_receive_buffersize;



    /**
     * 応答時の平均キューサイズ
     */
    private long res_avg_queue_size;






    public ForwardHistory(String fromID, String fromIP, String fromMac, String toID, String toIP, String toMac, long startTime) {
        this.fromID = fromID;
        this.fromIP = fromIP;
        this.fromMac = fromMac;
        this.toID = toID;
        this.toIP = toIP;
        this.toMac = toMac;
        this.startTime = startTime;
        this.req_avg_receive_buffersize = 0;
        this.res_avg_queue_size = 0;
        this.req_avg_queue_size = 0;
        this.res_avg_receive_buffersize = 0;

    }



    public long getReq_avg_receive_buffersize() {
        return req_avg_receive_buffersize;
    }

    public void setReq_avg_receive_buffersize(long req_avg_receive_buffersize) {
        this.req_avg_receive_buffersize = req_avg_receive_buffersize;
    }

    public long getReq_avg_queue_size() {
        return req_avg_queue_size;
    }

    public void setReq_avg_queue_size(long req_avg_queue_size) {
        this.req_avg_queue_size = req_avg_queue_size;
    }

    public long getRes_avg_receive_buffersize() {
        return res_avg_receive_buffersize;
    }

    public void setRes_avg_receive_buffersize(long res_avg_receive_buffersize) {
        this.res_avg_receive_buffersize = res_avg_receive_buffersize;
    }

    public long getRes_avg_queue_size() {
        return res_avg_queue_size;
    }

    public void setRes_avg_queue_size(long res_avg_queue_size) {
        this.res_avg_queue_size = res_avg_queue_size;
    }

    public String getFromID() {
        return fromID;
    }

    public void setFromID(String fromID) {
        this.fromID = fromID;
    }

    public String getFromIP() {
        return fromIP;
    }

    public void setFromIP(String fromIP) {
        this.fromIP = fromIP;
    }

    public String getFromMac() {
        return fromMac;
    }

    public void setFromMac(String fromMac) {
        this.fromMac = fromMac;
    }

    public String getToID() {
        return toID;
    }

    public void setToID(String toID) {
        this.toID = toID;
    }

    public String getToIP() {
        return toIP;
    }

    public void setToIP(String toIP) {
        this.toIP = toIP;
    }

    public String getToMac() {
        return toMac;
    }

    public void setToMac(String toMac) {
        this.toMac = toMac;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public long getMaxConnectionNum() {
        return maxConnectionNum;
    }

    public void setMaxConnectionNum(long maxConnectionNum) {
        this.maxConnectionNum = maxConnectionNum;
    }
}
