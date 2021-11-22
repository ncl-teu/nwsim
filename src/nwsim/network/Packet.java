package nwsim.network;

import nwsim.Param;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

public class Packet implements Serializable {

    /**
     * 取引ID
     */
    protected String tranID;

    protected long reqPacketSize;

    /**
     * フラグ(START/MID/END)
     */
    protected int flag;

    protected boolean isRequest;
    /**
     * パケットサイズ(byte)
     */
    protected long packetSize;

    /**
     * ヘッダー部のサイズ(byte) つまり，packetSize - headerSizeがデータ部のサイズ
     */
    protected long headerSize;

    /**
     * 送信元IP
     */
    protected String fromIP;

    /**
     * 送信先IP
     */
    protected String toIP;

    /**
     * 送信元ポート番号
     */
    protected int fromPort;

    /**
     * 送信先ポート番号
     */
    protected int toPort;

    /**
     * Time To Live
     */
    protected int TTL;

    /**
     * シーケンス番号
     */
    protected long sequenceNo;

    /**
     * 次のホップのMacアドレス
     */
    protected String toMacAddress;

    /**
     * プロトコル番号
     */
    protected int p_number;

    /**
     * データ部（とりあえず文字列）
     */
    protected String data;

    /**
     * データサイズ（自動で計算）
     */
    protected long dataSize;

    /**
     * ホストビット数（ネットワークアドレス計算のため）
     * 本当は無いけど，処理の簡略化のため・・・
     */
    protected int hostBit;

    protected Param.MSG_TYPE type;

    protected LinkedList<ForwardHistory> requestHistoryList;

    protected LinkedList<ForwardHistory> responseHistoryList;

    /**
     * 送ろうとしているデータのトータルサイズ
     */
    protected long totalDataSize;

    /**
     * 経由ルータの最小帯域幅
     */
    protected long minBW;

    /**
     * 遅延の合計値
     */
    protected long totalDelay;

    protected long req_max_queuesize;

    protected long req_max_buffersize;

    protected long res_max_buffersize;

    protected long res_max_queuesize;

    /**
     * 自分で決めたカスタムオブジェクトフィールド
     */
    protected Object aplMap;





    public Packet(long headerSize, String fromIP, String toIP, int fromPort, int toPort, String data) {
        this.dataSize = Param.mtu;
        this.headerSize = headerSize;
        this.packetSize = headerSize + this.dataSize;
       // this.reqPacketSize =  this.getTotalDataSize();
        this.aplMap = new Object();
        this.req_max_buffersize = 0;
        this.req_max_queuesize = 0;
        this.res_max_buffersize = 0;
        this.res_max_queuesize = 0;
        this.fromIP = fromIP;
        this.toIP = toIP;
        this.fromPort = fromPort;
        this.toPort = toPort;
        this.data = data;
        this.TTL = Param.ttl;
        this.isRequest = true;
        this.flag = Param.PACKET_START;


        this.sequenceNo = 0;
        this.requestHistoryList = new LinkedList<ForwardHistory>();
        this.responseHistoryList = new LinkedList<ForwardHistory>();
        this.totalDataSize = 0;
        this.minBW = 0;
        this.totalDelay = 0;

    }
    public long addTotalDelay(long delay){
        this.totalDelay += delay;
        return this.totalDelay;
    }

    public Object getAplMap() {
        return aplMap;
    }

    public void setAplMap(Object aplMap) {
        this.aplMap = aplMap;
    }

    public long getReq_max_queuesize() {
        return req_max_queuesize;
    }

    public void setReq_max_queuesize(long req_max_queuesize) {
        this.req_max_queuesize = req_max_queuesize;
    }

    public long getReq_max_buffersize() {
        return req_max_buffersize;
    }

    public void setReq_max_buffersize(long req_max_buffersize) {
        this.req_max_buffersize = req_max_buffersize;
    }

    public long getRes_max_buffersize() {
        return res_max_buffersize;
    }

    public void setRes_max_buffersize(long res_max_buffersize) {
        this.res_max_buffersize = res_max_buffersize;
    }

    public long getRes_max_queuesize() {
        return res_max_queuesize;
    }

    public void setRes_max_queuesize(long res_max_queuesize) {
        this.res_max_queuesize = res_max_queuesize;
    }

    public long getTotalDelay() {
        return totalDelay;
    }

    public void setTotalDelay(long totalDelay) {
        this.totalDelay = totalDelay;
    }

    public long getReqPacketSize() {
        return reqPacketSize;
    }

    public void setReqPacketSize(long reqPacketSize) {
        this.reqPacketSize = reqPacketSize;
    }

    public long getMinBW() {
        return minBW;
    }

    public void setMinBW(long minBW) {
        this.minBW = minBW;
    }

    public long getTotalDataSize() {
        return totalDataSize;
    }

    public void setTotalDataSize(long totalDataSize) {
        this.totalDataSize = totalDataSize;
        //this.reqPacketSize = this.totalDataSize;
    }

    public void addRequestHistory(ForwardHistory h){
        this.requestHistoryList.addLast(h);
    }

    public void addResponseHistory(ForwardHistory h){
        this.responseHistoryList.addLast(h);
    }

    public LinkedList<ForwardHistory> getResponseHistoryList() {
        return responseHistoryList;
    }

    public void setResponseHistoryList(LinkedList<ForwardHistory> responseHistoryList) {
        this.responseHistoryList = responseHistoryList;
    }

    public boolean isRequest() {
        return isRequest;
    }

    public void setRequest(boolean request) {
        isRequest = request;
    }

    public String getToMacAddress() {
        return toMacAddress;
    }

    public void setToMacAddress(String toMacAddress) {
        this.toMacAddress = toMacAddress;
    }

    public Param.MSG_TYPE getType() {
        return type;
    }

    public void setType(Param.MSG_TYPE type) {
        this.type = type;
    }

    public int getP_number() {
        return p_number;
    }

    public void setP_number(int p_number) {
        this.p_number = p_number;
    }

    public long getPacketSize() {
        return packetSize;
    }

    public void setPacketSize(long packetSize) {
        this.packetSize = packetSize;
    }

    public long getHeaderSize() {
        return headerSize;
    }

    public void setHeaderSize(long headerSize) {
        this.headerSize = headerSize;
    }

    public String getFromIP() {
        return fromIP;
    }

    public void setFromIP(String fromIP) {
        this.fromIP = fromIP;
    }

    public String getToIP() {
        return toIP;
    }

    public void setToIP(String toIP) {
        this.toIP = toIP;
    }

    public int getFromPort() {
        return fromPort;
    }

    public void setFromPort(int fromPort) {
        this.fromPort = fromPort;
    }

    public int getToPort() {
        return toPort;
    }

    public void setToPort(int toPort) {
        this.toPort = toPort;
    }

    public int getTTL() {
        return TTL;
    }

    public void setTTL(int tTL) {
        TTL = tTL;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public int getHostBit() {
        return hostBit;
    }

    public void setHostBit(int hostBit) {
        this.hostBit = hostBit;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public LinkedList<ForwardHistory> getRequestHistoryList() {
        return requestHistoryList;
    }

    public void setRequestHistoryList(LinkedList<ForwardHistory> requestHistoryList) {
        this.requestHistoryList = requestHistoryList;
    }

    public String getTranID() {
        return tranID;
    }

    public void setTranID(String tranID) {
        this.tranID = tranID;
    }
}
