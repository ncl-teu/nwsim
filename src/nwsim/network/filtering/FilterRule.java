package nwsim.network.filtering;

import java.util.ArrayList;

/**
 * フィルタリングルールを示すクラスです．
 * Accept/Reject/Drop/DNATの条件を表現します．
 * Hidehiro Kanemitsu.
 */
public class FilterRule {

    /**
     * フィルタID
     */
    private int filterID;

    /**
     * ACCPT/REJECT/DROP/DNATのいずれか
     */
    private int packetState;

    /**
     * ACCEPT/REJECT/DROP対象となるfrom IPの範囲のリスト
     */
    private ArrayList<String> fromIPRangeList;

    /**
     * ACCEPT/REJECT/DROP対象となるfrom NWの範囲のリスト
     */
    private ArrayList<String> fromNWRangeList;


    /**
     * DNATの場合の当該ルータでの宛先ポート
     */
    private int dnat_atPort;


    /**
     * 当該ルータから，LAN内のノードへの宛先ポート
     */
    private int dnat_toPort;

    /**
     * 当該ルータから，LAN内のノードへの宛先IP
     */
    private String dnat_toIP;

    public FilterRule( int packetState) {
        this.packetState = packetState;
    }

    public ArrayList<String> getFromNWRangeList() {
        return fromNWRangeList;
    }

    public void setFromNWRangeList(ArrayList<String> fromNWRangeList) {
        this.fromNWRangeList = fromNWRangeList;
    }



    public int getFilterID() {
        return filterID;
    }

    public void setFilterID(int filterID) {
        this.filterID = filterID;
    }

    public int getPacketState() {
        return packetState;
    }

    public void setPacketState(int packetState) {
        this.packetState = packetState;
    }

    public ArrayList<String> getFromIPRangeList() {
        return fromIPRangeList;
    }

    public void setFromIPRangeList(ArrayList<String> fromIPRangeList) {
        this.fromIPRangeList = fromIPRangeList;
    }

    public int getDnat_atPort() {
        return dnat_atPort;
    }

    public void setDnat_atPort(int dnat_atPort) {
        this.dnat_atPort = dnat_atPort;
    }

    public int getDnat_toPort() {
        return dnat_toPort;
    }

    public void setDnat_toPort(int dnat_toPort) {
        this.dnat_toPort = dnat_toPort;
    }

    public String getDnat_toIP() {
        return dnat_toIP;
    }

    public void setDnat_toIP(String dnat_toIP) {
        this.dnat_toIP = dnat_toIP;
    }
}
