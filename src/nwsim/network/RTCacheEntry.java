package nwsim.network;

import nwsim.env.Nic;

public class RTCacheEntry {

    protected String fromIP;

    protected int fromPort;

    protected String toIP;

    protected int toPort;

    protected String toMac;

    protected Nic fromNic;

    public RTCacheEntry(String fromIP, int fromPort, String toIP, int toPort, String toMac, Nic fromNic) {
        this.fromIP = fromIP;
        this.fromPort = fromPort;
        this.toIP = toIP;
        this.toPort = toPort;
        this.toMac = toMac;
        this.fromNic = fromNic;
    }

    public String getFromIP() {
        return fromIP;
    }

    public void setFromIP(String fromIP) {
        this.fromIP = fromIP;
    }

    public int getFromPort() {
        return fromPort;
    }

    public void setFromPort(int fromPort) {
        this.fromPort = fromPort;
    }

    public String getToIP() {
        return toIP;
    }

    public void setToIP(String toIP) {
        this.toIP = toIP;
    }

    public int getToPort() {
        return toPort;
    }

    public void setToPort(int toPort) {
        this.toPort = toPort;
    }

    public String getToMac() {
        return toMac;
    }

    public void setToMac(String toMac) {
        this.toMac = toMac;
    }

    public Nic getFromNic() {
        return fromNic;
    }

    public void setFromNic(Nic fromNic) {
        this.fromNic = fromNic;
    }
}
