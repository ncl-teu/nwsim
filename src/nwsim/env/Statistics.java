package nwsim.env;

public class Statistics {

    private String tranID;

    private long reqStartTime;

    private long reqFinishTime;

    private long resStartTime;

    private long resFinishTime;

    private long reqCount;

    private long resCount;

    private long totalReqReceiveBufferSize;

    private long totalResReceiveBufferSize;

    private long totalReqQueueSize;

    private long totalResQueueSize;


    private long maxReqReceiveBufferSize;

    private long maxReqQueueSize;

    private long maxResReceiveBufferSize;

    private long maxResQueueSize;

    public Statistics(String tranID) {
        this.tranID = tranID;
    }

    public String getTranID() {
        return tranID;
    }

    public void setTranID(String tranID) {
        this.tranID = tranID;
    }

    public long getReqStartTime() {
        return reqStartTime;
    }

    public void setReqStartTime(long reqStartTime) {
        this.reqStartTime = reqStartTime;
    }

    public long getReqFinishTime() {
        return reqFinishTime;
    }

    public void setReqFinishTime(long reqFinishTime) {
        this.reqFinishTime = reqFinishTime;
    }

    public long getResStartTime() {
        return resStartTime;
    }

    public void setResStartTime(long resStartTime) {
        this.resStartTime = resStartTime;
    }

    public long getResFinishTime() {
        return resFinishTime;
    }

    public void setResFinishTime(long resFinishTime) {
        this.resFinishTime = resFinishTime;
    }

    public long getReqCount() {
        return reqCount;
    }

    public void setReqCount(long reqCount) {
        this.reqCount = reqCount;
    }

    public long getResCount() {
        return resCount;
    }

    public void setResCount(long resCount) {
        this.resCount = resCount;
    }

    public long getTotalReqReceiveBufferSize() {
        return totalReqReceiveBufferSize;
    }

    public void setTotalReqReceiveBufferSize(long totalReqReceiveBufferSize) {
        this.totalReqReceiveBufferSize = totalReqReceiveBufferSize;
    }

    public long getTotalResReceiveBufferSize() {
        return totalResReceiveBufferSize;
    }

    public void setTotalResReceiveBufferSize(long totalResReceiveBufferSize) {
        this.totalResReceiveBufferSize = totalResReceiveBufferSize;
    }

    public long getTotalReqQueueSize() {
        return totalReqQueueSize;
    }

    public void setTotalReqQueueSize(long totalReqQueueSize) {
        this.totalReqQueueSize = totalReqQueueSize;
    }

    public long getTotalResQueueSize() {
        return totalResQueueSize;
    }

    public void setTotalResQueueSize(long totalResQueueSize) {
        this.totalResQueueSize = totalResQueueSize;
    }

    public long getMaxReqReceiveBufferSize() {
        return maxReqReceiveBufferSize;
    }

    public void setMaxReqReceiveBufferSize(long maxReqReceiveBufferSize) {
        this.maxReqReceiveBufferSize = maxReqReceiveBufferSize;
    }

    public long getMaxReqQueueSize() {
        return maxReqQueueSize;
    }

    public void setMaxReqQueueSize(long maxReqQueueSize) {
        this.maxReqQueueSize = maxReqQueueSize;
    }

    public long getMaxResReceiveBufferSize() {
        return maxResReceiveBufferSize;
    }

    public void setMaxResReceiveBufferSize(long maxResReceiveBufferSize) {
        this.maxResReceiveBufferSize = maxResReceiveBufferSize;
    }

    public long getMaxResQueueSize() {
        return maxResQueueSize;
    }

    public void setMaxResQueueSize(long maxResQueueSize) {
        this.maxResQueueSize = maxResQueueSize;
    }
}
