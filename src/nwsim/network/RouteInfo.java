package nwsim.network;

/**
 * Created by Hidehiro Kanemitsu on 2021/10/15
 * ルーティングテーブルのエントリです．
 */
public class RouteInfo {

    /**
     * 宛先ネットワークアドレス
     */
    protected String nwAddress;

    /**
     * 宛先ネットワークのサブネットマスク
     */
    protected String subNetMask;

    /**
     * Next HopのIP
     *
     */
    protected String nextHop;

    /**
     * 出口である送信元のNICの名前．eth0, eth1...
     */
    protected String nicName;

    /**
     * メトリック
     * ルーティングアルゴリズムで決める値．
     */
    protected double metric;

    protected boolean isTransfered;

    public RouteInfo(String nwAddress, String subNetMask, String nextHop, String nicName, double metric) {
        this.nwAddress = nwAddress;
        this.subNetMask = subNetMask;
        this.nextHop = nextHop;
        this.nicName = nicName;
        this.metric = metric;
        this.isTransfered = false;
    }

    public RouteInfo(){

    }

    public String getNwAddress() {
        return nwAddress;
    }

    public void setNwAddress(String nwAddress) {
        this.nwAddress = nwAddress;
    }

    public String getNextHop() {
        return nextHop;
    }

    public void setNextHop(String nextHop) {
        this.nextHop = nextHop;
    }

    public String getNicName() {
        return nicName;
    }

    public void setNicName(String nicName) {
        this.nicName = nicName;
    }

    public double getMetric() {
        return metric;
    }

    public void setMetric(double metric) {
        this.metric = metric;
    }

    public String getSubNetMask() {
        return subNetMask;
    }

    public void setSubNetMask(String subNetMask) {
        this.subNetMask = subNetMask;
    }

    public boolean isTransfered() {
        return isTransfered;
    }

    public void setTransfered(boolean transfered) {
        isTransfered = transfered;
    }
}
