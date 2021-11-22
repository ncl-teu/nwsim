package nwsim.env;

import nwsim.Param;
import nwsim.logger.NWLog;
import nwsim.network.*;
import nwsim.network.routing.AbstractRouting;
import nwsim.network.routing.NantokaRouting;
import nwsim.network.routing.RandomRouting;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.*;

/**
 * Created by Hidehiro Kanemitsu
 */
public class Node implements Serializable, Runnable {

    /**
     * Node識別ID
     */
    protected String ID;

    /**
     * 計算機，スイッチ，ルータ
     */
    protected int Type;

    /**
     * Nicのマップ <NIC名, Nic>の形式
     */
    protected HashMap<String, Nic> nicMap;

    /**
     * 経路情報のキャッシュ
     */
    protected HashMap<String, RTCacheEntry> rtCacheMap;

    /**
     * ARPテーブル<NIC名, Arpエントリ>で， Arpエントリは<IP, Mac>の形式
     * PC＋ルータのルーティングテーブルは，デフォルト(0.0.0.0)+他のNWアドレスが入る．
     * NextHopについては，「同一NW (GWへ送る）」と「IP」とに分ける．同一かそうでないかという基準．
     * A@NW1 -> B@NW2 へパケットを送るとき，
     * . Aのルーティングテーブルを見て，
     * - もしBのNWアドレスに相当するNextHopが「同一NW」であれば，ARPテーブル@Aを見る
     * - ARPテーブル@AにBのIPがあれば，該当するインタフェースへパケットを送出
     * - ARPテーブル@AにBのIPがなければ，ブロードキャストでARPリクエストを送る．
     * そして返答があればARPテーブル@Aにキャッシュとして保存する．そしてパケット送出
     * - もしBのNWアドレスが「同一NW」で無ければ，NextHoptへパケットを送る．
     * <p>
     * * - 宛先NWアドレスから，nextHopのIPと出口NICを特定し，
     * * - ARPを見てNextHopに対応するMacアドレスを見つけてパケットへ付与して送信．
     * * - 受信したNextHopは，Macアドレスを見て，ARP@受信側のMac<->IPの対応が
     * * - 一致していればOK．そうでなければダメ．
     */
    protected HashMap<String, HashMap<String, String>> arpMap;

    /**
     * Arpエントリ<IP, Mac>
     */
    // protected HashMap<String, String> arpEntryMap;


    /**
     * ルーティングテーブル
     * <宛先NWアドレス, 経路情報のMap>という形式
     * 経路情報のMap: <NextHopのIP, RouteInfo>
     */
    protected HashMap<String, HashMap<String, RouteInfo>> routingTable;

    /**
     * ルーティングプロトコルのリスト
     */
    protected ArrayList<AbstractRouting> routingList;


    protected AbstractRouting usedRouting;

    protected HashMap<String, LinkedList<Packet>> receivedPacketMap;

    /**
     * ARPテーブル．
     *
     * @param iD
     * @param type
     * @param nicMap
     */

    public Node(String iD, int type, HashMap<String, Nic> nicMap) {
        ID = iD;
        Type = type;
        this.nicMap = nicMap;
        this.arpMap = new HashMap<String, HashMap<String, String>>();
        this.routingList = new ArrayList<AbstractRouting>();
        HashMap arpMap = new HashMap<String, String>();
        this.routingTable = new HashMap<String, HashMap<String, RouteInfo>>();
        RandomRouting routing_random = new RandomRouting(10000);
        this.routingList.add(routing_random);
      if(this.getType() == Param.TYPE_ROUTER){
            Router r = (Router)this;
            NantokaRouting routing_nantoka = new NantokaRouting(1000, r);
            this.routingList.add(routing_nantoka);
       }else{
          this.routingList.add(routing_random);
      }

        this.usedRouting = this.routingList.get(Param.routing_no);
        this.receivedPacketMap = new HashMap<String, LinkedList<Packet>>();
        this.rtCacheMap = new HashMap<String, RTCacheEntry>(100);


    }


    /**
     * @param p
     */
    public void addReceivedPacket(Packet p) {
        if (this.receivedPacketMap.containsKey(p.getTranID())) {
            LinkedList<Packet> pList = this.receivedPacketMap.get(p.getTranID());
            pList.add(p);

        } else {
            LinkedList<Packet> pList = new LinkedList<Packet>();
            pList.add(p);
            this.receivedPacketMap.put(p.getTranID(), pList);
        }
    }

    public AbstractRouting getUsedRouting() {
        return usedRouting;
    }

    public void setUsedRouting(AbstractRouting usedRouting) {
        this.usedRouting = usedRouting;
    }

    public HashMap<String, LinkedList<Packet>> getReceivedPacketMap() {
        return receivedPacketMap;
    }

    public void setReceivedPacketMap(HashMap<String, LinkedList<Packet>> receivedPacketMap) {
        this.receivedPacketMap = receivedPacketMap;
    }

    public ArrayList<AbstractRouting> getRoutingList() {
        return routingList;
    }

    public void setRoutingList(ArrayList<AbstractRouting> routingList) {
        this.routingList = routingList;
    }

    public Packet createPacekt(int flg, String tranID,
                               long headerSize, String fromIP, String toIP, int fromPort, int toPort, String data) {


        Packet p = new Packet(headerSize, fromIP, toIP, fromPort, toPort, data);
        p.setTranID(tranID);
        p.setType(Param.MSG_TYPE.TCP);
        p.setFlag(flg);

        return p;

    }

    /**
     * 受信パケットを見て，送り返す処理を行う．
     * ここでやってくるpは，必ずリクエストである必要があります．
     *
     * @param p
     * @param atNic
     */
    public void returnPacketProcess(Packet p, Nic atNic, Param.MSG_TYPE type) {
        Statistics stat = Env.getIns().findStat(p.getTranID());
        //要求の最後の到着時刻
        stat.setReqFinishTime(System.currentTimeMillis());
        stat.setResCount(p.getTotalDataSize() / p.getPacketSize() + 1);


        //ARP_REQの処理も含む．
        if (p.getType() == Param.MSG_TYPE.ARP) {
            //要求IPアドレスを取得する．
            String ip = p.getData();
            if (ip.equals(atNic.getIpAddress())) {
                //要求通りであれば，返す．
                String mac = atNic.getMacAddress();
                Packet retP = new Packet(Param.header_size, atNic.getIpAddress(), p.getFromIP(), p.getToPort(), p.getFromPort(), mac);
                retP.setFlag(Param.PACKET_START);
                retP.setTranID(p.getTranID());

                this.sendPacket(retP, atNic);


            }
        } else {
            /**
             * ポイント: atNicにおいて，pのfromIP/fromPortを，toIP/toPortへ変更．
             * そして，fromIP/Portを，受け取ったときのIPアドレスとポートへ変更．
             */
            long reqDataSize = p.getReqPacketSize();
            String toIP = p.getFromIP();
            int toPort = p.getFromPort();
            p.setFromIP(atNic.getIpAddress());
            p.setFromPort(p.getToPort());

            p.setToIP(toIP);
            p.setToPort(toPort);
            //ARPから，toIPのMacアドレスを取得
            String toMac = this.findMacAddress(toIP);
            p.setToMacAddress(toMac);
            //応答である旨をセット
            p.setRequest(false);
            //Start
            //p.setFlag(Param.PACKET_START);
            p.setMinBW((long) atNic.getBw());
            //パケットの種類によって応答の内容を設定する．
            // if(p.getType() == Param.MSG_TYPE.HTTP_GET){
            //p.setType(Param.MSG_TYPE.HTTP_GET);
            long remainedSize = 0;
            if (p.getType() == Param.MSG_TYPE.HTTP_GET) {
                remainedSize = Param.genLong(Param.http_get_response_size_min, Param.http_get_response_size_max, 1,
                        Param.http_get_response_size_mu) * 1024;
            } else if (p.getType() == Param.MSG_TYPE.HTTP_POST) {
                remainedSize = Param.http_post_response_size * 1024;
            }
            //制御パケットの場合は，1パケットのみ
            if(p.getType() == Param.MSG_TYPE.CTRL){
                remainedSize = Param.header_size + Param.mtu;
            }


            long totalDataSize = remainedSize;
            p.setTotalDataSize(remainedSize);

            remainedSize -= p.getPacketSize();
            long count = remainedSize / p.getPacketSize();
            p.setFlag(Param.PACKET_START);
            if(remainedSize <= 0){
                p.setFlag(Param.PACKET_END);
            }

            this.sendPacket(p, atNic);
            for (int i = 0; i < count; i++) {
                long size = p.getPacketSize();
                int flg = Param.PACKET_MID;
                if (i >= count - 1) {
                    size = remainedSize;
                    flg = Param.PACKET_END;
                }


                Packet p2 = this.createPacekt(flg, p.getTranID(), p.getHeaderSize(), p.getFromIP(), p.getToIP(),
                        p.getFromPort(), p.getToPort(), p.getData());
                p2.setReqPacketSize(reqDataSize);
                p2.setRequest(false);
                // p2.setType(Param.MSG_TYPE.HTTP_GET);
                p2.setType(p.getType());
                p2.setFlag(flg);
                p2.setMinBW((long) atNic.getBw());
                p2.setToMacAddress(toMac);
                p2.setTotalDataSize(totalDataSize);
                p2.setRequestHistoryList(p.getRequestHistoryList());
                remainedSize -= p2.getPacketSize();

                this.sendPacket(p2, atNic);
            }


            //   }else{
            // System.out.println();
            ///   }
            //this.sendPacket(p, atNic);

        }

    }

    private String genCachKey(String orgFromIP, Packet p) {
        StringBuffer buf = new StringBuffer(orgFromIP);
        buf.append("^");
        buf.append(p.getFromPort());
        buf.append("^");
        buf.append(p.getToIP());
        buf.append("^");
        buf.append(p.getToPort());

        return buf.toString();
    }

    public boolean registerRouteCache(String orgFromIP, Packet p, String toMac, Nic fromNic) {
        RTCacheEntry entry = new RTCacheEntry(orgFromIP, p.getFromPort(), p.getToIP(), p.getToPort(),
                toMac, fromNic);
        String key = this.genCachKey(orgFromIP, p);
        if (!this.rtCacheMap.containsKey(key)) {
            this.rtCacheMap.put(key, entry);
        }
        return true;

    }

    /**
     * パケット処理メソッド
     * 宛先 != 自分である場合の処理です．
     * - もし当該ノードがルータならば，
     * - ルーティングテーブルを見てNWアドレスがあるかどうか探す．
     * - もしあれば，p->宛先アドレスがARPにあるかどうかチェック
     * -
     * - 宛先IP != 当該NICのIPならば，
     * - 宛先IPがARPにあれば(同一NW），該当IFから直接渡す．
     * - 宛先IPがARPに無ければ（宛先が異なるNW)，
     */
    public void processPacket(Packet p, Nic atNic) {


        if (p.getTTL() == 0) {
            HashMap<String, Double> map = Param.getIns().calcAvgBufferSize(p);
           /* NWLog.getIns().log(",TCP,"+p.getTranID() + ","+ p.getRequestHistoryList().getFirst().getFromID() + ","+ p.getRequestHistoryList().getLast().getToID() + "," +
                    (p.getRequestHistoryList().getLast().getArrivalTime() -p.getRequestHistoryList().getFirst().getStartTime()) + "," + p.getRequestHistoryList().size() +
                    ","+ map.get("reqbuffer") + "," + map.get("reqqueue") + ","+p.getPacketSize() + "," + "-,"+p.getResponseHistoryList().size()+
                    ","+ map.get("resbuffer") + "," + map.get("resqueue") + ","+ p.getPacketSize() + ","+ "TTL=0");
*/
            long totalReqTime = p.getRequestHistoryList().getLast().getArrivalTime() - p.getRequestHistoryList().getFirst().getStartTime() + p.getTotalDelay();

             NWLog.getIns().tranLog(p, "TTL_Max");

            return;
        }
        /*
        if((!p.isRequest() )&& (this.getType() == Param.TYPE_ROUTER)){
            this.processResponse(p, atNic);
            return;

        }*/

        //ルーティングテーブルのキャッシュ
        String cacheKey = this.genCachKey(p.getFromIP(), p);
        if (this.rtCacheMap.containsKey(cacheKey)) {
            RTCacheEntry entry = this.rtCacheMap.get(cacheKey);
            p.setToMacAddress(entry.getToMac());

            p.setFromIP(entry.getFromNic().getIpAddress());
            //NextHopに対して送る．
            //そして送信する．
            this.sendPacket(p, entry.getFromNic());
            return;
        }
        /*if(p.isRequest()){
            if(this.getType() == Param.TYPE_ROUTER){
                NAPTEntry cacheEntry = ((Router) this).findNAPTEntryAsCache(p.getToPort(), p.getFromIP(), p.getFromPort());
                if(cacheEntry != null){
                    p.setFromIP(cacheEntry.getSrcGlobalIP());
                    p.setToMacAddress(cacheEntry.getMac_nextHop());
                    this.sendPacket(p, cacheEntry.getNic_exit());
                    return;

                }
            }
        }else{

        }
*/


        //パケットの宛先を見て，該当する経路リストを見る．
        LinkedList<RouteInfo> routeList = this.findRouteList(p.getToIP(), atNic, p);

        //最適な経路を選択する．
        //ここで以前と同じ経路が選ばれてしまっているのが問題．
        //よって，historyListをもたせる．
        RouteInfo info = this.usedRouting.selectRoute(routeList);


        //インタフェースを特定する．
        String eth_from = info.getNicName();
        //NextHop
        String nextHop = info.getNextHop();
        //System.out.println("@"+this.getID() + "Next Hop:" + nextHop);
        //自身の出口NIC
        Nic nic_exit = this.nicMap.get(eth_from);
        //NextHop側のMacアドレスを特定する．
        String mac_nextHop = this.findMacAddress(nextHop);
        //nextHopの情報が自身のAPRに登録されていない場合
        //普通はありえない？？？
        if (mac_nextHop == null) {
            String nw = Param.getNWAddress(p.getToIP(), nic_exit.getSubNetMask());
            //
            if (nw.equals(nic_exit.getNwAddress())) {
                //MACアドレスが無いかつfromNICとtoIPが同一ネットワークならば，
                //データ部は，解決したいIPアドレス
                Packet arp_req = new Packet(20, nic_exit.getIpAddress(), nic_exit.getGwIP(),
                        Param.genInt(1025, 65535, 0, 0.5), -1, nextHop);
                arp_req.setType(Param.MSG_TYPE.ARP);
                //GWのnicのmacアドレスを取得
                HashMap<String, String> arpMap = this.arpMap.get(atNic.getNicName());
                arp_req.setToMacAddress(arpMap.get(atNic.getGwIP()));
                //GWに対して送る．
                //this.processPacket(arp_req);
                this.sendPacket(arp_req, nic_exit);
            } else {

                //異なるネットワークならば，転送するのみ．
                //ルータの処理になるので，NAPTテーブルに情報を保存しておく．
                NAPTEntry entry = new NAPTEntry(p.getFromIP(), p.getFromPort(), nic_exit.getIpAddress(), p.getFromPort(), p.getToIP(), p.getToPort(),
                        p.getToIP(), p.getToPort());
                ((Router) this).registerNAPTEntry(p.getFromIP(), p.getFromPort(), entry);
                //デフォルトルートのNIC
                p.setFromIP(nic_exit.getIpAddress());
                //System.out.println("nic:"+nic_from.getMacAddress());

                this.processPacket(p, nic_exit);


            }

        } else {
            if (p.isRequest()) {
                //Macアドレスがあれば，パケットにセットする．宛先がfromNICと同一NWである．
                p.setToMacAddress(mac_nextHop);
                if (this.getType() == Param.TYPE_ROUTER) {
                    //異なるネットワークならば，転送するのみ．
                    //ルータの処理になるので，NAPTテーブルに情報を保存しておく．
                    NAPTEntry entry = new NAPTEntry(p.getFromIP(), p.getFromPort(),
                            nic_exit.getIpAddress(), p.getFromPort(), p.getToIP(), p.getToPort(),
                            p.getToIP(), p.getToPort());
                    //entry.setMac_nextHop(mac_nextHop);
                    //entry.setNic_exit(nic_exit);
                    ((Router) this).registerNAPTEntry(p.getFromIP(), p.getFromPort(), entry);
                }

                String orgFromIP = p.getFromIP();

                p.setFromIP(nic_exit.getIpAddress());
                //NextHopに対して送る．
                //そして送信する．
                this.sendPacket(p, nic_exit);
                String key = this.genCachKey(orgFromIP, p);
                this.registerRouteCache(orgFromIP, p, mac_nextHop, nic_exit);
            } else {
                //応答の場合
                if (this.getType() == Param.TYPE_ROUTER) {
                    this.processResponse(p, atNic);
                    //ルータならば，NAPTテーブルを見る．
                 /*   NAPTEntry entry = ((Router)this).findNAPTEntry(p.getToIP(), p.getToPort(), p.getFromIP(), p.getFromPort());

                    //local情報を取得
                    p.setToIP(entry.getSrcLocalIP());
                    p.setToPort(entry.getSrcLocalPort());
                    p.setFromIP(nic_exit.getIpAddress());
                    p.setFromPort(p.getFromPort());
                    this.sendPacket(p, nic_exit);
*/
                }
            }


        }


    }

    /**
     * 特定のIPをARPに持つNICを返す
     * @param ip
     * @return
     */
    public Nic findNicFromIP(String ip){
        Iterator<String> arpIte = this.arpMap.keySet().iterator();
        Nic nic = null;
        while(arpIte.hasNext()){
            String eth = arpIte.next();
            HashMap<String, String> map = this.arpMap.get(eth);
            if(map.containsKey(ip)){
                nic = Env.getIns().getNicMap().get(map.get(ip));
                break;
            }

        }
        return nic;

    }

    public void processResponse(Packet p, Nic atNic) {
        //応答の場合
        if (this.getType() == Param.TYPE_ROUTER) {
            //ルータならば，NAPTテーブルを見る．




            synchronized (((Router) this).NAPTMap) {
                String fromIP = null;
                if(Param.dnat_mode == 0){
                    fromIP = p.getRequestHistoryList().getLast().getToIP();

                }else{
                    if(p.getResponseHistoryList().size() == 1){
                        fromIP = p.getResponseHistoryList().getLast().getFromIP();
                    }else{
                        int len = p.getRequestHistoryList().size();
                        fromIP = p.getRequestHistoryList().get(len-2).getToIP();
                    }

                }
               // NAPTEntry entry_org = ((Router) this).getNAPTMap().get(p.getRequestHistoryList().getFirst().getFromID());
                //NAPTEntry entry = ((Router) this).findNAPTEntry(p.getToIP(), p.getToPort(), fromIP, p.getFromPort());
                NAPTEntry entry = ((Router) this).findNAPTEntry(atNic.getIpAddress(), p.getToPort(), fromIP, p.getFromPort());

                if (entry == null) {
                     System.out.println("XX: fromIP:"+p.getFromIP() + "/fromPort:"+p.getFromPort() + "/toIP:"+p.getToIP() + "/toPort:"+p.getToPort());
                    return;
                }
                //local情報を取得
                p.setToIP(entry.getSrcLocalIP());
                p.setToPort(entry.getSrcLocalPort());


                //toMacアドレスの更新
                String mac_nextHop = this.findMacAddress(entry.getSrcLocalIP());
                p.setToMacAddress(mac_nextHop);
                //ルーティングテーブルを見る．
               // LinkedList<RouteInfo> rList = this.findRouteList(entry.getSrcLocalIP(), atNic, p);
                //srcLocalIPと同じNWアドレスのものを取得する．
                //srcLocalIPをarpmapから探して，それに該当するnicを特定する．
                Nic fromexitnic = this.findNicFromIP(entry.getSrcLocalIP());


                //RouteInfo rinfo = this.usedRouting.selectRoute(rList);
                //送信元のNicの更新
                //Nic fromexitnic = this.nicMap.get(rinfo.getNicName());
               //if (p.getFromIP().startsWith("192.168.0.")) {
                    p.setFromIP(fromexitnic.getIpAddress());
               // }
                //p.setFromIP(p.getRequestHistoryList().getFirst().getToIP());

                //Nicを取得する．
                String localGlobalIP = this.findMacAddress(p.getToIP());

                if (p.getToMacAddress() == null) {
                    //System.out.println();
                } else {
                    this.sendPacket(p, fromexitnic);
                    if (p.getFlag() == Param.PACKET_END) {
                        ((Router) this).getNAPTMap().remove(entry.getSrcLocalIP() + ":" + entry.getSrcGlobalPort());
                    }
                    //ここでけしているのを，ENDパケット到着で消すようにする．
                    /// ((Router)this).getNAPTMap().remove(entry.getSrcLocalIP() + ":"+entry.getSrcGlobalPort());
                }

            }

        }
    }

    /**
     * 指定のパケットを送信する処理．
     * 必要な情報は予めpにセットされていることが前提．
     * あとは，経路表及びARPテーブルを見て送信元＋送信先NIC
     * を割り出し，パケットを送信する．
     *
     * @param p
     */
    public void sendPacket(Packet p, Nic fromNic) {
        String toIP = p.getToIP();
        int ttl = p.getTTL();
        p.setTTL(ttl - 1);
        Nic nic = Env.getIns().getNicMap().get(p.getToMacAddress());
        Node node = nic.getMyNode();
        long current = System.currentTimeMillis();
        //Historyの準備
        ForwardHistory h = new ForwardHistory(this.getID(), fromNic.getIpAddress(), fromNic.getMacAddress(),
                node.getID(), nic.getIpAddress(), p.getToMacAddress(), current);

        if (p.isRequest()) {
            if (p.getFlag() == Param.PACKET_START) {
                Statistics stat = Env.getIns().findStat(p.getTranID());
                stat.setReqStartTime(current);
                stat.setReqCount(p.getReqPacketSize() / p.getPacketSize() + 1);


            }

            if (this.getType() == Param.TYPE_ROUTER) {
                h.setReq_avg_receive_buffersize(fromNic.getReceiveBuffer().size());
                h.setReq_avg_queue_size(((Router) this).getCurrentQueueLength());
                if (p.getReq_max_buffersize() <= fromNic.getReceiveBuffer().size()) {
                    p.setReq_max_buffersize(fromNic.getReceiveBuffer().size());
                }
            }
            p.addRequestHistory(h);
        } else {
            if (p.getFlag() == Param.PACKET_START) {
                Statistics stat = Env.getIns().findStat(p.getTranID());
                stat.setResStartTime(current);
                stat.setResCount(p.getTotalDataSize() / p.getPacketSize() + 1);

            }

            if (this.getType() == Param.TYPE_ROUTER) {
                h.setRes_avg_receive_buffersize(fromNic.getReceiveBuffer().size());
                h.setRes_avg_queue_size(((Router) this).getCurrentQueueLength());
                if (p.getRes_max_buffersize() <= fromNic.getReceiveBuffer().size()) {
                    p.setRes_max_buffersize(fromNic.getReceiveBuffer().size());
                }
            }
            p.addResponseHistory(h);
        }


        if (node.getType() == Param.TYPE_ROUTER) {
            Router r = (Router) node;
            //宛先ノードの受信バッファへ入れる．
            fromNic.sendPacketProcess(p, nic);
            //nic.getSendBuffer().offer(p);
            // nic.getReceiveBuffer().offer(p);
            r.decrementCurrentQueue();
        } else {
            Computer c = (Computer) node;
            //宛先ノードの受信バッファへ入れる．
            //nic.getReceiveBuffer().offer(p);
            //nic.getSendBuffer().offer(p);
            fromNic.sendPacketProcess(p, nic);

        }


    }

    /**
     * NICを稼働させます．
     */
    public void startUpNics() {
        Iterator<Nic> nIte = this.nicMap.values().iterator();
        while (nIte.hasNext()) {
            Nic nic = nIte.next();
            Thread t = new Thread(nic);
            //Nicのプロセスを起動させる．
            t.start();
            //System.out.println(this.getID() + ":Nic "+nic.getNicName() + " is started.");
        }
    }

    /**
     * ARPテーブルから，指定の宛先IPに紐づく宛先のMacアドレスを取得する．
     *
     * @param ip
     * @return
     */
    public String findMacAddress(/*String eth, */String ip) {
        Iterator<HashMap<String, String>> ite = this.arpMap.values().iterator();
        String mac = null;
        while (ite.hasNext()) {
            HashMap<String, String> info = ite.next();
            if (info.containsKey(ip)) {
                mac = info.get(ip);
                break;
            }
        }
        return mac;
        /*
        HashMap<String, String> map = this.arpMap.get(eth);
        String mac = null;
        if(map == null){
            return mac;
        }
        if(map.containsKey(ip)){
            mac = map.get(ip);
        }else{
            return mac;
        }

        return mac;
        */

    }

    /**
     * データ送信処理
     *
     * @param data: 送るデータ
     */
    public void sendData(Data data) {
        //KBであるからbyteに変換する
        long remainedDataSize = data.getSize() * 1024;

        while (remainedDataSize < 0) {
            // Packet p = new Packet(Param.packet_size+Param.header_size, )
        }

    }

    @Override
    public void run() {


    }

    /**
     * 指定IPに該当するインタフェースを取得する．
     * ルーティングテーブルからネットワークアドレスを特定し，
     * それに最長一致する経路情報を返す．
     *
     * @param ip 宛先IP
     * @return LinkedList<RouteInfo> 経路情報のリスト
     */
    public LinkedList<RouteInfo> findRouteList(String ip, Nic fromNic, Packet p) {
        LinkedList<RouteInfo> retList = new LinkedList<RouteInfo>();
        //宛先が到達可能であるようなネットワークアドレスのリスト
        LinkedList<RouteInfo> tmpList = new LinkedList<RouteInfo>();
        synchronized (this.routingTable) {
            Iterator<HashMap<String, RouteInfo>> iIte = this.routingTable.values().iterator();
            int maxCount = 0;

            while (iIte.hasNext()) {
                HashMap<String, RouteInfo> info = iIte.next();
                Iterator<RouteInfo> rIte = info.values().iterator();
                while (rIte.hasNext()) {
                    RouteInfo rinfo = rIte.next();
                    if (rinfo.getNextHop().equals(p.getFromIP())) {
                        continue;
                    }
                    //String nw = rinfo.getNwAddress();
                /*if(!fromNic.equals(nw)){
                    continue;
                }*/
                    //if(rinfo.getNwAddress()==null){
                    //ルーティングテーブル内のサブネットマスクを適用して
                    //ネットワークアドレスを計算する
                    String nw = Param.getNWAddress(ip, rinfo.getSubNetMask());

                    // }
                    //計算したネットワークアドレスと既存ルーティングテーブル
                    //の宛先ネットワークアドレスが一致すれば結果に追加．
                    if (p.isRequest()) {
                        if (nw.equals(rinfo.getNwAddress())) {
                            if (!this.isAlreadyForwarded(rinfo.getNextHop(), p)) {
                                String binary = this.getBinarySubnetMask(rinfo.getSubNetMask());
                                int cnt = this.countBit(binary, '1');

                                if (cnt > maxCount) {
                                    retList.clear();
                                    retList.add(rinfo);
                                    maxCount = cnt;
                                } else if (cnt == maxCount) {
                                    retList.add(rinfo);
                                }
                                //tmpList.add(rinfo);
                            }

                        }
                    } else {
                        if (nw.equals(rinfo.getNwAddress())) {
                            String gnw = Param.getNWAddress(rinfo.getNwAddress(), rinfo.getSubNetMask());
                            if (!nw.equals(gnw)) {
                                continue;
                            }
                            String binary = this.getBinarySubnetMask(rinfo.getSubNetMask());
                            int cnt = this.countBit(binary, '1');

                            if (cnt > maxCount) {
                                retList.clear();
                                retList.add(rinfo);
                                maxCount = cnt;
                            } else if (cnt == maxCount) {
                                retList.add(rinfo);
                            }
                            //tmpList.add(rinfo);
                        }
                    }

                }
                // System.out.println("一致した数"+retList.size());

            }
        }

        //tmpListから，subnetmaskのビット数が最大のものを選択する．

        if (retList.isEmpty()) {
            //デフォルトルートをセットする．
            if (!this.routingTable.containsKey(Param.DEFAULT_ROUTE)) {
                RouteInfo default_info = new RouteInfo(Param.DEFAULT_ROUTE, "0.0.0.0", fromNic.getGwIP(), fromNic.getNicName(), 100);

                this.registerRoute(default_info);
            }
            HashMap<String, RouteInfo> dmap = this.routingTable.get(Param.DEFAULT_ROUTE);
            Iterator<RouteInfo> rIte = dmap.values().iterator();
            while (rIte.hasNext()) {
                retList.add(rIte.next());
            }
        }
        return retList;

    }

    public String getBinarySubnetMask(String subnetmask) {
        try {
            byte[] bytes2 = InetAddress.getByName(subnetmask).getAddress();
            String subnetMask_binary = new BigInteger(1, bytes2).toString(2); // bytes ではなく bytes2
            return subnetMask_binary;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int countBit(String str, char target) {
        int count = 0;


        for (char x : str.toCharArray()) {
            if (x == target) {
                count++;
            }
        }


        return count;
    }


    /**
     * @param fromNic
     * @param nextHopNic
     * @param toPort
     * @param msg_type
     * @return
     */
    public Packet genCtrlPacket(Nic fromNic, Nic nextHopNic, int toPort, Param.MSG_TYPE msg_type) {
        // Packet p = new Packet();
        //宛先を決める．とりあえずルータにする．
        String target;
        Packet p = new Packet(Param.header_size, fromNic.getIpAddress(), nextHopNic.getIpAddress(),
                Param.genInt(1025, 65535, 1, 0.5), toPort, "test");
        p.setTotalDataSize(Param.header_size + Param.mtu);
        p.setReqPacketSize(Param.header_size + Param.mtu);
        //メッセージタイプの設定
        p.setType(msg_type);

        //1パケットしか送らないから，END扱いにする．
        p.setFlag(Param.PACKET_END);
        p.setRequest(true);
        p.setToMacAddress(nextHopNic.getMacAddress());
        p.setMinBW((long) Math.min(fromNic.getBw(), nextHopNic.getBw()));

        return p;

    }


    public boolean isAlreadyForwarded(String nextHop, Packet p) {
        Iterator<ForwardHistory> fIte = p.getRequestHistoryList().iterator();
        boolean ret = false;
        while (fIte.hasNext()) {
            ForwardHistory h = fIte.next();
            if (h.getToIP().equals(nextHop)) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    public HashMap<String, HashMap<String, RouteInfo>> getRoutingTable() {
        return routingTable;
    }

    public void setRoutingTable(HashMap<String, HashMap<String, RouteInfo>> routingTable) {
        this.routingTable = routingTable;
    }

    public HashMap<String, RTCacheEntry> getRtCacheMap() {
        return rtCacheMap;
    }

    public void setRtCacheMap(HashMap<String, RTCacheEntry> rtCacheMap) {
        this.rtCacheMap = rtCacheMap;
    }

    public boolean isDefaultRouteSet() {
        boolean ret = false;
        if (this.routingTable.containsKey(Param.DEFAULT_ROUTE)) {
            ret = true;
        } else {

        }
        return ret;
    }

    /**
     * 経路情報の登録
     *
     * @param info
     */
    public void registerRoute(RouteInfo info) {
        //もしNWアドレスが存在すれば，追加する．
        if (this.routingTable.containsKey(info.getNwAddress())) {
            HashMap<String, RouteInfo> entry = this.routingTable.get(info.getNwAddress());
            entry.put(info.getNextHop(), info);

        } else {
            HashMap<String, RouteInfo> entry = new HashMap<String, RouteInfo>();
            entry.put(info.getNextHop(), info);
            this.routingTable.put(info.getNwAddress(), entry);
        }
    }

    /**
     * ARPエントリの登録
     *
     * @param nic 当該ノードのNIC名(eth0...)
     * @param ip  相手のIPアドレス
     * @param mac 相手側のNICのMacアドレス
     */
    public void registerArp(String nic, String ip, String mac) {
        HashMap<String, HashMap<String, String>> arp = this.getArpMap();
        if (arp.containsKey(nic)) {
            HashMap<String, String> entry = arp.get(nic);
            entry.put(ip, mac);
        } else {
            HashMap<String, String> entry = new HashMap<String, String>();
            entry.put(ip, mac);
            this.arpMap.put(nic, entry);
        }

    }

    public String getID() {
        return ID;
    }

    public void setID(String iD) {
        ID = iD;
    }

    public int getType() {
        return Type;
    }

    public void setType(int type) {
        Type = type;
    }

    public HashMap<String, Nic> getNicMap() {
        return nicMap;
    }

    public void setNicMap(HashMap<String, Nic> nicMap) {
        this.nicMap = nicMap;
    }

    public HashMap<String, HashMap<String, String>> getArpMap() {
        return arpMap;
    }

    public void setArpMap(HashMap<String, HashMap<String, String>> arpMap) {
        this.arpMap = arpMap;
    }
}
