package nwsim.env;

import nwsim.Param;
import nwsim.network.NAPTEntry;
import nwsim.network.Packet;
import nwsim.network.RouteInfo;
import nwsim.network.filtering.FilterRule;
import nwsim.network.routing.ExchangeTimer;

import java.util.*;

/**
 *
 * Author: Hidehiro Kanemitsu
 */
public class Router extends Node {


    /**
     * ルータが持つNICにLANのNICがあるかどうか
     */
    boolean isLANExist;

    /**
     * 未設定のNicがただ一つ残っているか
     */
    boolean isRemainOnlyOne;

    /**
     * NAPTテーブル
     * <送信元IP:port, NAPTエントリ>の形式です．
     *
     * @param iD
     * @param type
     * @param nicMap
     */
    protected HashMap<String, NAPTEntry> NAPTMap;

    /**
     * NAPTを参照中かどうか
     */
    protected boolean isNAPTInProcess;

    /**
     * キュー長
     */
    protected long currentQueueLength;

    /**
     * 最大キューサイズ
     */
    protected long maxQueueLength;

    /**
     * フィルタリングのリスト
     */
    private TreeMap<Integer, FilterRule> filterMap;

    /**
     * 自身の管理するLANのPCリスト
     */
    private LinkedList<Computer> LANPCList;

    private LinkedList<Nic> LanNicList;



    /**
     * フィルタリングテーブル
     *
     * @param iD
     * @param type
     * @param nicMap
     */

    public Router(String iD, int type, HashMap<String, Nic> nicMap) {
        super(iD, type, nicMap);
        this.isLANExist = false;
        this.isRemainOnlyOne = false;
        this.NAPTMap = new HashMap<String, NAPTEntry>();
        this.isNAPTInProcess = false;
        this.maxQueueLength = Param.router_queue_length;
        this.currentQueueLength = 0;
        this.filterMap = new TreeMap<Integer, FilterRule>();

        this.LANPCList = new LinkedList<Computer>();
        this.LanNicList = new LinkedList<Nic>();
    }

    public LinkedList<Nic> getLanNicList() {
        return LanNicList;
    }

    public void setLanNicList(LinkedList<Nic> lanNicList) {
        LanNicList = lanNicList;
    }

    /**
     * ルールの新規登録
     * @param rule
     */
    public void registerFilter(FilterRule rule){
        int currentNum = this.filterMap.size();
        int id = currentNum + 1;
        rule.setFilterID(id);
        this.filterMap.put(id, rule);
    }

    /**
     * 指定の宛先ポート番号に対応したDNATルールを探します．
     * @param port
     * @return
     */
    public FilterRule findDNATFilter(int port){
        Iterator<FilterRule> rIte = this.filterMap.values().iterator();
        FilterRule retRule = null;
        while(rIte.hasNext()){
            FilterRule r = rIte.next();
            if(r.getPacketState() == Param.PACKET_DNAT){
                int atPort = r.getDnat_atPort();
                if(atPort == port){
                    retRule = r;
                    break;
                }
            }
        }
        return retRule;

    }



    /**
     * ルータ内のキューサイズを1増やす
     * @return 増やした場合は1増やした上でtrue/キューがいっぱいで増やせない場合はfalse
     */
    public boolean incrementCurrentQueue() {
        if (this.currentQueueLength >= this.maxQueueLength) {
            return false;
        } else {
            this.currentQueueLength++;
            return true;

        }
    }

    /**
     * 当該ルータが持つLANの，PC側のNicリストを返します．
     * @return
     */
    public LinkedList<Nic> getLANNics(){
        Iterator<Nic> nIte = this.getNicMap().values().iterator();
        String nicName = null;
        LinkedList<Nic> retList = new LinkedList<Nic>();

        while(nIte.hasNext()){
            Nic nic = nIte.next();
            if(nic.getNwAddress().equals("192.168.0.0")){
                nicName = nic.getNicName();
                break;
            }
        }
        if(nicName != null){
            HashMap<String, String> aMap = this.arpMap.get(nicName);

            Iterator<String> ipIte = aMap.keySet().iterator();
            while(ipIte.hasNext()){
                String ip = ipIte.next();
                if(!ip.equals("192.168.0.1")){
                    String mac = aMap.get(ip);
                    Nic nic = Env.getIns().getNicMap().get(mac);
                    //Computer pc = (Computer)nic.getMyNode();
                    retList.add(nic);

                }
            }


        }else{

        }
        return retList;
    }




    /**
     * 当該ルータが持つLANのPCリストを返します．
     *
     * @return
     */
    public LinkedList<Computer> getLANPCs(){
        Iterator<Nic> nIte = this.getNicMap().values().iterator();
        String nicName = null;
        LinkedList<Computer> retList = new LinkedList<Computer>();

        while(nIte.hasNext()){
            Nic nic = nIte.next();
            if(nic.getNwAddress().equals("192.168.0.0")){
                nicName = nic.getNicName();
                break;
            }
        }
        if(nicName != null){
            HashMap<String, String> aMap = this.arpMap.get(nicName);

            Iterator<String> ipIte = aMap.keySet().iterator();
            while(ipIte.hasNext()){
                String ip = ipIte.next();
                if(!ip.equals("192.168.0.1")){
                    String mac = aMap.get(ip);
                    Nic nic = Env.getIns().getNicMap().get(mac);
                    Computer pc = (Computer)nic.getMyNode();
                    retList.add(pc);

                }
            }


        }else{

        }
        return retList;
    }

    /**
     * ルータ内のキューサイズを1減らす
     * @return
     */
    public boolean decrementCurrentQueue() {
        long tmp = this.currentQueueLength - 1;
        this.currentQueueLength = Math.max(0, tmp);
        return true;
    }

    /**
     * @param r     登録先ルータ
     * @param atNic 　登録先ルータ側のNIC (eth)
     * @param set   チェック済みNICのmacアドレス
     * @return
     */
    public LinkedList<RouteInfo> configRouteAtRouter(
            Router r, Nic atNic, TreeSet<String> set) {

        Iterator<Nic> nIte = r.getNicMap().values().iterator();
        LinkedList<RouteInfo> rList = new LinkedList<RouteInfo>();
        if (set.contains(atNic.getMacAddress())) {
            return rList;
        }
        while (nIte.hasNext()) {
            Nic nic = nIte.next();
            if ((!nic.getIpAddress().equals(nic.getGwIP())) && (!nic.getNwAddress().equals("192.168.0.0"))) {
                //GWルータのNICを取得
                String gwMac = r.findMacAddress(nic.getGwIP());
                Nic gwNic = Env.getIns().getNicMap().get(gwMac);
                Router gwRouter = (Router) gwNic.getMyNode();
                //gwルータに対して，経路をもらう．
                LinkedList<RouteInfo> routeList = this.configRouteAtRouter(gwRouter, gwNic, set);
                Iterator<RouteInfo> rIte = routeList.iterator();
                while (rIte.hasNext()) {
                    RouteInfo rinfo = rIte.next();
                    r.registerRoute(rinfo);
                    set.add(gwNic.getMacAddress());
                }
            } else {
                set.add(nic.getMacAddress());
            }
        }

        return rList;
    }

    /**
     * 初期化
     */
    public void initialize() {
        //自分がグローバルNWのGWである経路情報を，nicのGWに登録する．
        Iterator<Nic> nicIte = this.getNicMap().values().iterator();
        while (nicIte.hasNext()) {
            Nic nic = nicIte.next();
            if (nic.getIpAddress() == null) {
                continue;
            }

            if (nic.getIpAddress().equals("192.168.0.1")) {
                //LANの経路情報を登録する．
                RouteInfo lanRoute = new RouteInfo(nic.getNwAddress(), nic.getSubNetMask(),
                        nic.getGwIP(), nic.getNicName(), 100);
                this.registerRoute(lanRoute);
            }
            if ((nic.getIpAddress().equals(nic.getGwIP())) && (!nic.getIpAddress().equals("192.168.0.1"))) {
                HashMap<String, RouteInfo> info = this.getRoutingTable().get(nic.getNwAddress());
                RouteInfo rInfo = info.get(nic.getIpAddress());
                //Env.getIns().reigsterRouteToGW(this, rInfo);
                //他のnicの隣接するGWへ登録する．
                Iterator<Nic> tmpNicIte = this.getNicMap().values().iterator();
                while (tmpNicIte.hasNext()) {
                    Nic tmpNic = tmpNicIte.next();
                    if ((!tmpNic.getIpAddress().equals(nic.getIpAddress())) &&
                            (!nic.getIpAddress().equals("192.168.0.1"))) {
                        //自身が子供である場合，親に対して経路を登録する．
                        if (!tmpNic.getIpAddress().equals(tmpNic.getGwIP())) {
                            //この場合に，登録する．
                            HashMap<String, String> arpInfo = this.arpMap.get(tmpNic.getNicName());
                            String gwMac = arpInfo.get(tmpNic.getGwIP());
                            Nic gwNic = Env.getIns().getNicMap().get(gwMac);
                            //自分（子）->親(GW)へ，子供のNWアドレスを登録する．
                            RouteInfo gwRoute = new RouteInfo(tmpNic.getNwAddress(), tmpNic.getSubNetMask(),
                                    tmpNic.getIpAddress(), gwNic.getNicName(), 100);


                            RouteInfo gwRoute2 = new RouteInfo(nic.getNwAddress(), nic.getSubNetMask(),
                                    tmpNic.getIpAddress(), gwNic.getNicName(), 100);
                            gwNic.getMyNode().registerRoute(gwRoute);
                            gwNic.getMyNode().registerRoute(gwRoute2);

                            RouteInfo childRoute = new RouteInfo(tmpNic.getNwAddress(), tmpNic.getSubNetMask(),
                                    tmpNic.getGwIP(), tmpNic.getNicName(), 100);
                            // this.registerRoute(childRoute);
                            //wRoute.setTransfered(true);
                            //さらに，infoも登録する．
                            Iterator<RouteInfo> rIte = info.values().iterator();
                            while (rIte.hasNext()) {
                                RouteInfo rinfo = rIte.next();
                                RouteInfo newInfo = new RouteInfo(rinfo.getNwAddress(), rinfo.getSubNetMask(),
                                        tmpNic.getIpAddress(), gwNic.getNicName(), 100);
                                gwNic.getMyNode().registerRoute(newInfo);
                                // reigsterRouteToGW()
                                Env.getIns().reigsterRouteToGW(this, newInfo);
                            }
                        }
                    }
                }

            } else {
                continue;
            }
        }

    }

    @Override
    public void run() {

        //まずはすべてのNICを稼働させる．
        this.startUpNics();
        long start = System.currentTimeMillis();
        this.initialize();
        Timer timer = new Timer();

        ExchangeTimer ex = new ExchangeTimer(this);
        //指定時間間隔で，ExchangeTimerを実行する．
        timer.scheduleAtFixedRate(ex, 10000, this.usedRouting.getExchangeSpan());




/*
        while (true) {
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            long current = System.currentTimeMillis();
            synchronized (this.getRoutingTable()) {

            }

        }

 */
    }

    /**
     * DNAT処理です．LAN内PCへのポート転送を行います．
     * @param p
     * @param atNic
     * @return
     */
    public boolean DNATProcess(Packet p, Nic atNic){
        FilterRule rule = this.findDNATFilter(p.getToPort());
        //System.out.println();


        //DNATエントリが見つかったら，toを変える．
        p.setToIP(rule.getDnat_toIP());


        //一つのエントリだけでOK．
        NAPTEntry org_entry = ((Router) this).findNAPTEntry("192.168.0.1", p.getFromPort(), p.getToIP(), p.getToPort());
        if(org_entry == null){
            NAPTEntry entry = new NAPTEntry(p.getFromIP(), p.getFromPort(),"192.168.0.1", p.getFromPort(), p.getToIP(), p.getToPort(),
                    p.getToIP(), p.getToPort());
            ((Router)this).registerNAPTEntry(/*p.getFromIP()*/"192.168.0.1", p.getFromPort(), entry);
        }



        String mac = this.findMacAddress(rule.getDnat_toIP());
        p.setToMacAddress(mac);
        p.setFromIP("192.168.0.1");
        Iterator<Nic> nIte = this.getNicMap().values().iterator();
        Nic fromNIc = null;
        while(nIte.hasNext()){
            Nic nic = nIte.next();
            if(nic.getNwAddress().equals("192.168.0.0")){
                fromNIc = nic;
                break;
            }
        }

        this.sendPacket(p, fromNIc);


        return true;
    }

    public LinkedList<RouteInfo> getRouteInfoFromNic(Router r, Nic nic) {
        LinkedList<RouteInfo> retList = new LinkedList<RouteInfo>();

        Iterator<HashMap<String, RouteInfo>> rIte = r.getRoutingTable().values().iterator();
        while (rIte.hasNext()) {
            HashMap<String, RouteInfo> info = rIte.next();
            Iterator<RouteInfo> ite = info.values().iterator();
            while (ite.hasNext()) {
                RouteInfo info2 = ite.next();
                if (info2.getNicName().equals(nic.getNicName())) {
                    retList.add(info2);
                }

            }
        }
        return retList;
    }


    public TreeMap<Integer, FilterRule> getFilterMap() {
        return filterMap;
    }

    public void setFilterMap(TreeMap<Integer, FilterRule> filterMap) {
        this.filterMap = filterMap;
    }

    public long getCurrentQueueLength() {
        return currentQueueLength;
    }

    public void setCurrentQueueLength(long currentQueueLength) {
        this.currentQueueLength = currentQueueLength;
    }

    public long getMaxQueueLength() {
        return maxQueueLength;
    }

    public void setMaxQueueLength(long maxQueueLength) {
        this.maxQueueLength = maxQueueLength;
    }

    /**
     *
     * @param port
     * @param DestGlobalIP
     * @param DestGlobalPort
     * @return
     */
    public NAPTEntry findNAPTEntryAsCache(int port, String DestGlobalIP, int DestGlobalPort) {
        NAPTEntry retEntry = null;
        while (this.isNAPTInProcess) {
            try {
                //Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.isNAPTInProcess = true;
        synchronized (this.NAPTMap) {
            Iterator<NAPTEntry> nIte = this.NAPTMap.values().iterator();
            while (nIte.hasNext()) {
                NAPTEntry entry = nIte.next();
                if ((entry.getSrcGlobalPort() == port)) {
                    if ((entry.getDestGlobalIP().equals(DestGlobalIP)) && (entry.getDestGlobalPort() == DestGlobalPort)) {
                        retEntry = entry;
                        break;
                    }
                }
            }
        }

        this.isNAPTInProcess = false;

        return retEntry;
    }

    /**
     * データ（戻り）の送り元IP/Portから，要求送信元の情報を取得する．
     *
     * @param fromIP   srcGlobalIP
     * @param fromPort srcGlobalPort
     * @param toIP     DestGlobalIP
     * @param toPort   DestGlobalPort
     * @return
     */
    public NAPTEntry findNAPTEntry(String fromIP, int fromPort, String toIP, int toPort) {

        NAPTEntry retEntry = null;

        this.isNAPTInProcess = true;
        synchronized (this.NAPTMap) {
            Iterator<NAPTEntry> nIte = this.NAPTMap.values().iterator();
            while (nIte.hasNext()) {
                NAPTEntry entry = nIte.next();
                if ((entry.getSrcGlobalIP().equals(fromIP)) && (entry.getSrcGlobalPort() == fromPort)) {
                    if ((entry.getDestGlobalIP().equals(toIP)) && (entry.getDestGlobalPort() == toPort)) {
                        retEntry = entry;
                        break;
                    }
                }
            }
        }

        this.isNAPTInProcess = false;

        return retEntry;
    }



    public void registerNAPTEntry(String srcIP, int srcPort, NAPTEntry entry) {
        /*while(this.isNAPTInProcess){
            try{
                Thread.sleep(1);
            }catch(Exception e){
                e.printStackTrace();
            }
        }*/
        this.isNAPTInProcess = true;
        synchronized (this.NAPTMap) {
            if (this.NAPTMap.containsKey(srcIP + ":" + srcPort)) {

            } else {
                this.NAPTMap.put(srcIP + ":" + srcPort, entry);

            }
        }

        this.isNAPTInProcess = false;
    }

    public boolean isLANExist() {
        Iterator<Nic> nIte = this.nicMap.values().iterator();
        boolean ret = false;
        while (nIte.hasNext()) {
            Nic nic = nIte.next();
            if (nic.isLAN()) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    /**
     * セットアップ時に，LANとしてNICを追加すべきかどうか
     * の判断．
     *
     * @return
     */
    public boolean shouldSetAsLAN() {
        boolean ret = false;
        if (!this.isLANExist()) {
            if (this.isRemainOnlyOne()) {
                ret = true;
            }
        }
        return ret;
    }

    public void setLANExist(boolean LANExist) {
        isLANExist = LANExist;
    }

    public boolean isNAPTInProcess() {
        return isNAPTInProcess;
    }

    public void setNAPTInProcess(boolean NAPTInProcess) {
        isNAPTInProcess = NAPTInProcess;
    }

    public boolean isAllNicConfigured() {
        Iterator<Nic> nIte = this.nicMap.values().iterator();
        boolean ret = true;
        int cnt = 0;
        while (nIte.hasNext()) {
            Nic nic = nIte.next();
            if (nic.getHostBit() == -1) {
                ret = false;
                break;
            }

        }
        return ret;
    }

    public boolean isRemainOnlyOne() {
        Iterator<Nic> nIte = this.nicMap.values().iterator();
        boolean ret = false;
        int cnt = 0;
        while (nIte.hasNext()) {
            Nic nic = nIte.next();
            if (nic.getHostBit() == -1) {
                cnt++;
            }

        }
        if (cnt == 1) {
            return true;
        } else {
            return false;
        }
    }

    public LinkedList<Computer> getLANPCList() {
        return LANPCList;
    }

    public void setLANPCList(LinkedList<Computer> LANPCList) {
        this.LANPCList = LANPCList;
    }

    public void setRemainOnlyOne(boolean remainOnlyOne) {
        isRemainOnlyOne = remainOnlyOne;
    }

    public HashMap<String, NAPTEntry> getNAPTMap() {
        return NAPTMap;
    }

    public void setNAPTMap(HashMap<String, NAPTEntry> NAPTMap) {
        this.NAPTMap = NAPTMap;
    }
}
