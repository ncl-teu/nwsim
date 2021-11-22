package nwsim.network;

import nwsim.env.Nic;

/**
 * Created by Hidehiro Kanemitsu on 2021/10/21
 * NAPTテーブルのエントリです．
 */
public class NAPTEntry {

    /**
     * 内側の送信元端末のIP
     */
    protected String srcLocalIP;

    /**
     * 内側の送信元端末のポート番号
     */
    protected int srcLocalPort;

    /**
     * 外側の送信元IP(つまりルータの外側IP)
     * ここは，ルーティングテーブルで，
     * - 宛先NWアドレスから，nextHopのIPと出口NICを特定し，
     * - ARPを見てNextHopに対応するMacアドレスを見つけてパケットへ付与して送信．
     * - 受信側は，Macアドレスを見て，ARP@受信側のMac<->IPの対応が
     * - 一致していればOK．そうでなければダメ．
     */
    protected String srcGlobalIP;

    /**
     * 外側の送信元ポート（ルータの外側のポート番号）
     */
    protected int srcGlobalPort;


    /**
     * 宛先のIP(destGlobalIPと同じ）
     */
    protected String destLocalIP;

    /**
     * 宛先のポート(destGlobalPortと同じ）
     */
    protected int destLocalPort;

    /**
     * 宛先IP
     */
    protected String destGlobalIP;

    /**
     * 宛先Port
     */
    protected int destGlobalPort;


    /**
     *
     * @param srcLocalIP
     * @param srcLocalPort
     * @param srcGlobalIP
     * @param srcGlobalPort
     * @param destLocalIP
     * @param destLocalPort
     * @param destGlobalIP
     * @param destGlobalPort
     */
    public NAPTEntry(String srcLocalIP,
                     int srcLocalPort,
                     String srcGlobalIP,
                     int srcGlobalPort,
                     String destLocalIP,
                     int destLocalPort,
                     String destGlobalIP,
                     int destGlobalPort) {
        this.srcLocalIP = srcLocalIP;
        this.srcLocalPort = srcLocalPort;
        this.srcGlobalIP = srcGlobalIP;
        this.srcGlobalPort = srcGlobalPort;
        this.destLocalIP = destLocalIP;
        this.destLocalPort = destLocalPort;
        this.destGlobalIP = destGlobalIP;
        this.destGlobalPort = destGlobalPort;

    }


    public String getSrcLocalIP() {
        return srcLocalIP;
    }

    public void setSrcLocalIP(String srcLocalIP) {
        this.srcLocalIP = srcLocalIP;
    }

    public int getSrcLocalPort() {
        return srcLocalPort;
    }

    public void setSrcLocalPort(int srcLocalPort) {
        this.srcLocalPort = srcLocalPort;
    }

    public String getSrcGlobalIP() {
        return srcGlobalIP;
    }

    public void setSrcGlobalIP(String srcGlobalIP) {
        this.srcGlobalIP = srcGlobalIP;
    }

    public int getSrcGlobalPort() {
        return srcGlobalPort;
    }

    public void setSrcGlobalPort(int srcGlobalPort) {
        this.srcGlobalPort = srcGlobalPort;
    }

    public String getDestLocalIP() {
        return destLocalIP;
    }

    public void setDestLocalIP(String destLocalIP) {
        this.destLocalIP = destLocalIP;
    }

    public int getDestLocalPort() {
        return destLocalPort;
    }

    public void setDestLocalPort(int destLocalPort) {
        this.destLocalPort = destLocalPort;
    }

    public String getDestGlobalIP() {
        return destGlobalIP;
    }

    public void setDestGlobalIP(String destGlobalIP) {
        this.destGlobalIP = destGlobalIP;
    }

    public int getDestGlobalPort() {
        return destGlobalPort;
    }

    public void setDestGlobalPort(int destGlobalPort) {
        this.destGlobalPort = destGlobalPort;
    }
}
