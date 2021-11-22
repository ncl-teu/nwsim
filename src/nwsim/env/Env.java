package nwsim.env;

import java.util.*;

import nwsim.Param;
import nwsim.network.RouteInfo;
import nwsim.network.filtering.FilterRule;

/**
 * NWSimの環境を管理するクラスです．
 *  Created by Hidehiro Kanemitsu
 */
public class Env {
    /**
     * PCのID, PC
     */
    protected HashMap<String, Computer> pcMap;

    /**
     * RouterID, Router
     */
    protected HashMap<String, Router> routerMap;

    /**
     * IPアドレス, Router
     */
    protected HashMap<String, Router> routerIPMap;

    /**
     * NICのマップ
     * (Macアドレス，NIC)
     *
     */
    protected HashMap<String, Nic> nicMap;


    /**
     * GWのIPアドレスの集合（ネットワーク生成時に重複しないようにするための対応）
     */
    protected HashSet<String> gwIPSet;

    /**
     * 自身のシングルトンオブジェクト
     */
    private static Env own;

    /**
     * PCのIDづけに必要なPCカウント値
     */
    private long pcCount;

    /**
     * TranID, 統計情報
     */
    private HashMap<String, Statistics> statMap;

    /**
     * HTTP (80)のDNATが有効なルータIPリスト
     */
    private ArrayList<String> HTTP_DNAT_RouterList;


    public static Env getIns() {
        if (Env.own == null) {
            Env.own = new Env();
        }
        return Env.own;
    }

    private Env() {
        // システムの初期化処理を行う．
        this.pcCount = 0;
        this.statMap = new HashMap<String, Statistics>();
        this.HTTP_DNAT_RouterList = new ArrayList<String>();

    }

    /**
     * LAN構築メソッドです．当該ルータに対して，
     * 192.168.0.0/24ネットワークを構築します
     * @param r
     */
    public Router constructLAN(Router r, Nic nic){
        Iterator<Nic> nIte = r.getNicMap().values().iterator();
        //各NICに対するループ
        int pc_idx = 0;
        String ip = "192.168.0.1";
        r.setLANExist(true);
        //固定にする．この時点でサブネットマスクは登録されている．
        nic.setHostBit(8);
        //ネットワークアドレスの取得
        String NW = Param.getNWAddress(ip, nic.getSubNetMask());
        nic.setNwAddress(NW);
        nic.setGwIP(ip);
        nic.setIpAddress(ip);
        nic.setLAN(true);
        r.registerArp(nic.getNicName(), nic.getIpAddress(), nic.getMacAddress());
        //this.routerIPMap.put(ip, r);
        long num = Param.genLong(Param.env_num_pc_nw_min, Param.env_num_pc_nw_max,
                1, Param.env_num_pc_nw_mu);
        //小さい方をとる．
        long num_ip_tmp = Math.min(253, num);
        long num_ip = Math.min(num_ip_tmp, Param.env_num_pc / Env.getIns().getRouterMap().size());

        //LAN内のPC生成のためのループ
        for (int j = 0; j < num_ip; j++) {
            Computer pc = this.pcMap.get("p" + this.pcCount);
            //PCのNIC設定
            Nic pc_nic = pc.getNicMap().get("eth0");
            String pc_ip = Param.incrementIP(ip, j + 1);
            pc_nic.setIpAddress(pc_ip);
            pc_nic.setHostBit(8);
            pc_nic.setNwAddress(Param.getNWAddress(pc_ip, pc_nic.getSubNetMask()));
            pc_nic.setGwIP(nic.getIpAddress());

            //ルーティングテーブルの設定
            //LANのネットワークアドレスの転送先をルータにする．
            HashMap<String, HashMap<String, RouteInfo>> rt = pc.getRoutingTable();
            RouteInfo info = new RouteInfo("192.168.0.0", "255.255.255.0", pc_nic.getGwIP(), pc_nic.getNicName(), 100);
            pc.registerRoute(info);
            //それ以外のネットワークアドレスの転送先をルータにする．
            RouteInfo default_info = new RouteInfo(Param.DEFAULT_ROUTE, "0.0.0.0", pc_nic.getGwIP(), pc_nic.getNicName(), 100);
            pc.registerRoute(default_info);

            //ARPテーブルの設定
            //とりあえず，ルータのIPを登録する．
            //<Nic名, Arpエントリ<IP, Mac>>
            //(自身のNIC, 宛先IP, 宛先NICのMacアドレス)
            pc.registerArp(pc_nic.getNicName(), pc_nic.getGwIP(), nic.getMacAddress());
            pc.registerArp(pc_nic.getNicName(), pc_nic.getIpAddress(), pc_nic.getMacAddress());
            //ついでにルータのARPにも登録
            r.registerArp(nic.getNicName(), pc_nic.getIpAddress(), pc_nic.getMacAddress());
           // pc_idx++;
            this.pcCount++;
        }
        return r;
    }

    /**
     * 未使用のPCを削除する処理(初期化時に使用）
     */
    public void removeUnUsedPCs(){
        Iterator<Computer> pcIte = this.pcMap.values().iterator();
        LinkedList<String> pcList = new LinkedList<String>();
        while(pcIte.hasNext()){
            Computer pc = pcIte.next();
            Nic nic = pc.getNicMap().get("eth0");
            if(nic.getHostBit() < 0){
                pcList.add(pc.getID());

            }
        }
        Iterator<String> uIte = pcList.iterator();
        while(uIte.hasNext()){
            String id = uIte.next();
            Computer pc = this.pcMap.get(id);
            String mac = pc.getNicMap().get("eth0").getMacAddress();
            this.pcMap.remove(id);
            //Nicも削除
            this.nicMap.remove(mac);
        }
    }

    /**
     *
     * rにおいて，nic以外のWAN側NICを見て，
     * もしGWでなければ，GWルータのNICに，infoを，
     * NW/Subnet: そのまま
     * nexthop: rのIP
     * eth: GW側のnic (r <-> GWがつながっている方のeth)
     * を設定する．
     *
     *  ルータが定期的に行うものとする．
     * @param r 起点となるルータ
     * @param info 転送すべき経路情報
     */
    public void reigsterRouteToGW(Router r, RouteInfo info) {
        String nicName = info.getNicName();
        Nic nic = r.getNicMap().get(nicName);
        if(nic == null){
            return;
        }
        if(nic.isLAN() || nic.getHostBit() == -1){
            return;
        }
        if(!nic.getGwIP().equals(nic.getIpAddress())){
            return;
        }

        Iterator<Nic> nIte = r.getNicMap().values().iterator();

        while(nIte.hasNext()){
            Nic n = nIte.next();
            if(n.getNicName().equals(nic.getNicName())){
                continue;
            }
            if(n.isLAN() || n.getHostBit() == -1){
                continue;
            }
            if(n.getGwIP().equals(n.getIpAddress())){
                continue;
            }
            //GWIPを取得
            String gwIP = n.getGwIP();
            //ARPを見て，Macアドレスを調べる
            HashMap<String, String> arpMap = r.getArpMap().get(n.getNicName());
            String macAddress = arpMap.get(gwIP);
            //GW側のNICを取得
            Nic gwNic = Env.getIns().getNicMap().get(macAddress);

            RouteInfo route = new RouteInfo(info.getNwAddress(), info.getSubNetMask(), n.getIpAddress(), gwNic.getNicName(), 100);
            Router gw = (Router)gwNic.getMyNode();
            if(gw.getRoutingTable().containsKey(info.getNwAddress())){
                return;
            }else{
                //GWに，自分の内側の経路を登録する．
                gw.registerRoute(route);

            }

        }

    }


    /**
     * rをGWとしたルータ間ネットワークを構築する．
     * @param r
     * @param nic
     * @return
     */
    public LinkedList<Router> constructWAN(Router r, Nic nic){
        //NICが未設定の場合かつルータ同士のネットワークを構築する場合．
        //ルータ間のネットワーク
        //未設定ならば設定する．
        //ホストビットを2 - 6に設定する．
        int hostBit = Param.genInt(2, 6, 1, 0.5);
        nic.setHostBit(hostBit);
        //このネットワークの最大のIP数
        int routerNum = Env.getIns().calcIPNum(hostBit);
        //ネットワークのIP数を決定させる．
        long num_ip = Param.genLong(Param.env_num_router_nw_min, Param.env_num_router_nw_max,
                1, Param.env_num_router_nw_mu);
        int r_num = (int)Math.min(routerNum, num_ip);
        //指定数分だけの他ルータを選ぶ
        LinkedList<Router> others = this.getRouterListByNum(r_num, r);
        LinkedList<Router> oList = new LinkedList<Router>();

        //StringBuffer buf = new StringBuffer();
        //IPアドレスを設定する．
        String ip = Param.genGWAddress();
        nic.setIpAddress(ip);
        nic.setGwIP(ip);
        String nw = Param.getNWAddress(ip, nic.getSubNetMask());
        nic.setNwAddress(nw);
        nic.setLAN(false);
        nic.setMyNode(r);

        //GWのルーティングテーブルを設定
        HashMap<String, HashMap<String, RouteInfo>> rt = r.getRoutingTable();
        RouteInfo info = new RouteInfo(nw, nic.getSubNetMask(),nic.getGwIP(), nic.getNicName(), 100);
        r.registerRoute(info);
        //自分の親ルータはまだわからないので，デフォルトルートはまだ登録できない．
        r.registerArp(nic.getNicName(), nic.getIpAddress(), nic.getMacAddress());
        //othersに対して，NIC，ルーティングテーブル，ARPの登録をする．
        //この時点で，otherには未設定なnicがあることを保証している．
        Iterator<Router> oIte = others.iterator();
        String tmpIP = ip;
        int cnt = 0;
        while(oIte.hasNext()){
            if(cnt == r_num-1){
                break;
            }
            Router or = oIte.next();
            tmpIP = Param.incrementIP(tmpIP, 1);
            Nic newNic = this.findNewNic(or);
            if(newNic == null){
                continue;
            }

            newNic.setLAN(false);
            newNic.setHostBit(hostBit);
            newNic.setGwIP(ip);
            newNic.setNwAddress(nw);
            newNic.setIpAddress(tmpIP);
            this.routerIPMap.put(tmpIP, or);

            //次に，ルーティングテーブルの設定
            HashMap<String, HashMap<String, RouteInfo>> ort = or.getRoutingTable();
            RouteInfo oinfo = new RouteInfo(nw, nic.getSubNetMask(), nic.getGwIP(), newNic.getNicName(), 100);
            or.registerRoute(oinfo);
            //それ以外のネットワークアドレスの転送先をルータにする．
            //デフォルトルートが既にセットされていれば，
            RouteInfo default_info2 = new RouteInfo(Param.DEFAULT_ROUTE, "0.0.0.0", newNic.getGwIP(), newNic.getNicName(), 100);
            or.registerRoute(default_info2);
            //ARPの設定
            or.registerArp(newNic.getNicName(), newNic.getGwIP(), nic.getMacAddress());
            or.registerArp(newNic.getNicName(), newNic.getIpAddress(), newNic.getMacAddress());
            //ついでにルータのARPにも登録
            r.registerArp(nic.getNicName(), newNic.getIpAddress(), newNic.getMacAddress());

            //自分自身を登録．


            cnt++;
            oList.add(or);

        }
        return oList;

    }

    /**
     * 当該ルータを起点として，その子ルータに対してルーティングテーブル
     * を再帰的に構築する処理です．
     * @param r
     */
    public void configRoute(Router r){
        Iterator<Nic> nIte = r.getNicMap().values().iterator();
        while(nIte.hasNext()){
            Nic nic = nIte.next();
            if(nic.getHostBit() == -1){
                //LANを構築する．
                if(r.shouldSetAsLAN()){
                    r = this.constructLAN(r, nic);
                    //LinkedList<Computer> pcList = r.getLANPCs();
                    //System.out.println();
                }else{
                    //WANを構築する．
                    LinkedList<Router> oList = this.constructWAN(r, nic);
                    //今度はoListの要素に対してconstructWANを行う．
                    Iterator<Router> orIte = oList.iterator();
                    while(orIte.hasNext()){
                        Router or = orIte.next();
                        this.configRoute(or);
                    }
                }

            }
        }
    }

    /**
     * 環境の初期化処理
     */
    public void initialize() {
        this.gwIPSet = new HashSet<String>();

        this.pcMap = new HashMap<String, Computer>();
        this.routerMap = new HashMap<String, Router>();
        this.routerIPMap = new HashMap<String, Router>();
        this.nicMap = new HashMap<String, Nic>();


        // まずはルータの登録
        for (int i = 0; i < Param.env_num_router; i++) {
            String router_name = "r" + i;

            Router r = new Router(router_name, Param.TYPE_ROUTER, new HashMap<String, Nic>());
            // rのNICの数を決める．
            int nicNum = Param.genInt(Param.router_nic_num_min, Param.router_nic_num_max,
                    1, Param.router_nic_num_mu);
            //NICに対するループ
            for (int j = 0; j < nicNum; j++) {
                // 帯域幅を決める．
                long bw = Param.genLong(Param.bw_router_min, Param.bw_router_max, 1, Param.bw_router_mu);
                // Macアドレス(本当は48bitだが，今回は一意かどうかが重要なので拘らない)
                String macAddress = UUID.randomUUID().toString();
                //バッファサイズを決める．

                // ルータをすべて配備してから，IPアドレスとサブネットマスクを設定する
                Nic nic = new Nic(bw, macAddress, null, Param.nic_buffer_size);
                nic.setMyNode(r);
                nic.setNicName("eth"+j);
                r.getNicMap().put("eth"+j, nic);


            }
            // Routerを登録する．
            Env.getIns().routerMap.put(r.getID(), r);

        }

        //次に，PCの登録
        for (int i=0;i<Param.env_num_pc;i++){
            String pc_name = "p"+i;
            Computer pc = new Computer(pc_name, Param.TYPE_COMPUTER, new HashMap<String, Nic>());
            //PCのNicは一つのみとする．
            // 帯域幅を決める．
            long bw = Param.genLong(Param.bw_pc_min, Param.bw_pc_max, 1, Param.bw_pc_mu);
            // Macアドレス(本当は48bitだが，今回は一意かどうかが重要なので拘らない)
            String macAddress = UUID.randomUUID().toString();
            //バッファサイズを決める．

            // ルータをすべて配備してから，IPアドレスとサブネットマスクを設定する
            Nic nic = new Nic(bw, macAddress, null, Param.nic_buffer_size);
            nic.setMyNode(pc);

            pc.getNicMap().put("eth0", nic);
            nic.setNicName("eth0");
            //PCを登録する．
            Env.getIns().pcMap.put(pc.getID(), pc);
        }
        //Mapからルータのiteratorを取得する．
        Iterator<Router> rIte = Env.getIns().routerMap.values().iterator();

        HashMap<String, Router> gwMap = new HashMap<String, Router>();


        int pc_idx = 0;
        //各ルータの依存関係を，nicによって定義する．
        /**
         * 階層構造のトポロジ生成
         */
        Router root = this.routerMap.get("r0");
        this.configRoute(root);
        Iterator<Router> rIte2 = this.routerMap.values().iterator();
        while(rIte2.hasNext()){
            Router r = rIte2.next();
            r.initialize();
        }

        //再帰的に経路登録する．
        //Router root = Env.getIns().getRouterMap().get("r0");
        //rootの各NICに対して再帰呼び出し．
        Iterator<Nic> nIte = root.getNicMap().values().iterator();
        LinkedList<RouteInfo> rList = new LinkedList<RouteInfo>();
        TreeSet<String> set = new TreeSet<String>();
        while(nIte.hasNext()){
            Nic nic = nIte.next();
            if((!nic.getIpAddress().equals(nic.getGwIP())) && (!nic.getNwAddress().equals("192.168.0.0"))){
                //GWルータのNICを取得
                String gwMac = root.findMacAddress(nic.getGwIP());
                Nic gwNic = Env.getIns().getNicMap().get(gwMac);
                Router gwRouter = (Router)gwNic.getMyNode();
                //gwルータに対して，経路をもらう．
                LinkedList<RouteInfo> routeList = root.configRouteAtRouter(gwRouter, gwNic, set);
                set.add(gwNic.getMacAddress());
                Iterator<RouteInfo> rIte3 = routeList.iterator();
                while(rIte3.hasNext()){
                    RouteInfo rinfo = rIte3.next();
                    root.registerRoute(rinfo);
                }
            }else{
                set.add(nic.getMacAddress());
            }



        }
        //HTTP 80 DNATを採用するルータを探す．
        Iterator<Router> hIte = this.routerMap.values().iterator();
        while(hIte.hasNext()){
            Router r = hIte.next();
            //LANを持つルータかどうか
            if(r.isLANExist()){
                //rのLAN側でない方のNICのIPアドレスをすべて登録する．
                Iterator<Nic> gIte = r.getNicMap().values().iterator();
                while(gIte.hasNext()){
                    Nic nic = gIte.next();
                    if(nic.isLAN()){
                        continue;
                    }
                    this.HTTP_DNAT_RouterList.add(nic.getIpAddress());
                }
                //rのDNATテーブルを設定する．
                FilterRule rule = new FilterRule(Param.PACKET_DNAT);
                rule.setDnat_atPort(80);
                rule.setDnat_toPort(80);
                //LAN内のどの端末にするか決める．
                LinkedList<Nic> lanNicList = r.getLANNics();
                int idx = Param.genInt(0, lanNicList.size()-1, 0, 0.5);
                int cnt = 0;
                Iterator<Nic> lIte = lanNicList.iterator();
                Nic retNic = null;
                while(lIte.hasNext()){
                    Nic nic = lIte.next();
                    if(cnt == idx){
                        retNic = nic;
                        break;
                    }
                    cnt ++;
                }
                rule.setDnat_toIP(retNic.getIpAddress());
                r.registerFilter(rule);
                //System.out.println();



            }
        }


        //ルータ，PCを稼働させる．
        Iterator<Router> routerIte = this.routerMap.values().iterator();
        while(routerIte.hasNext()){
            Router router = routerIte.next();

            Thread routerThread  = new Thread(router);
            routerThread.start();
        }
        this.removeUnUsedPCs();
        //ARPを埋める．
        try{
            //Thread.sleep(5000);
            //System.out.println();
        }catch(Exception e){
            e.printStackTrace();
        }
        Iterator<Computer> pcIte = this.pcMap.values().iterator();
        while(pcIte.hasNext()){
            Computer pc = pcIte.next();
            Thread pcThread  = new Thread(pc);
            pcThread.start();
        }
        //System.out.println();



    }

    /**
     * 未使用のNICを探します．
     * @param n
     * @return
     */
    public Nic findNewNic(Node n){
        Iterator<Nic> nIte = n.getNicMap().values().iterator();
        Nic retNic = null;
        while(nIte.hasNext()){
            Nic nic  = nIte.next();
            if(nic.getHostBit() == -1){
                retNic = nic;
                break;
            }
        }
        return retNic;

    }

    public Statistics findStat(String tranID){
        if(this.statMap.containsKey(tranID)){
            return this.getStatMap().get(tranID);

        }else{
            Statistics stat = new Statistics(tranID);
            this.statMap.put(tranID, stat);
            return stat;
        }
    }

    public HashSet<String> getGwIPSet() {
        return gwIPSet;
    }

    public void setGwIPSet(HashSet<String> gwIPSet) {
        this.gwIPSet = gwIPSet;
    }

    public HashMap<String, Statistics> getStatMap() {
        return statMap;
    }

    public void setStatMap(HashMap<String, Statistics> statMap) {
        this.statMap = statMap;
    }

    /**
     * 指定した数だけのルータを取得する．なお，
     * LAN用NICがisRemainOnlyOneかつisLanExist: false以外のものだけを取得する．
     * @param num
     * @param r
     * @return
     */
    public LinkedList<Router> getRouterListByNum(int num, Router r){
        Iterator<Router> rIte = this.routerMap.values().iterator();
        int cnt = 0;
        LinkedList<Router> retList = new LinkedList<Router>();

        while(rIte.hasNext()){
            Router router = rIte.next();
            if(r.getID().equals(router.getID())){
                continue;
            }
            if((!router.isLANExist)&&(r.isRemainOnlyOne)){
                continue;
            }

            if(cnt == num){
                break;
            }
            retList.add(router);
        }
        return retList;
    }

    public HashMap<String, Router> getRouterIPMap() {
        return routerIPMap;
    }

    public void setRouterIPMap(HashMap<String, Router> routerIPMap) {
        this.routerIPMap = routerIPMap;
    }

    /**
     * ホストビットを指定して，割当て可能なIPアドレス数を 計算します．
     * 
     * @param hostbit
     * @return
     */
    public int calcIPNum(int hostbit) {
        int val = 0;
        for (int i = 0; i < hostbit; i++) {
            val += Math.pow(2, i);
        }
        return val - 1;

    }

    /**
     * IDをキーに，ノードを取得する．
     * 
     * @param id
     * @return
     */
    public Computer findPc(String id) {
        return this.pcMap.get(id);
    }

    public Router findRouter(String id) {
        return this.routerMap.get(id);
    }

    public HashMap<String, Computer> getPcMap() {
        return pcMap;
    }

    public void setPcMap(HashMap<String, Computer> pcMap) {
        this.pcMap = pcMap;
    }

    public HashMap<String, Router> getRouterMap() {
        return routerMap;
    }

    public void setRouterMap(HashMap<String, Router> routerMap) {
        this.routerMap = routerMap;
    }

    public HashMap<String, Nic> getNicMap() {
        return nicMap;
    }

    public void setNicMap(HashMap<String, Nic> nicMap) {
        this.nicMap = nicMap;
    }

    public ArrayList<String> getHTTP_DNAT_RouterList() {
        return HTTP_DNAT_RouterList;
    }

    public void setHTTP_DNAT_RouterList(ArrayList<String> HTTP_DNAT_RouterList) {
        this.HTTP_DNAT_RouterList = HTTP_DNAT_RouterList;
    }
}
