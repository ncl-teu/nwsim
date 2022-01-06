package nwsim.network.routing;

import nwsim.Param;
import nwsim.env.Env;
import nwsim.env.Nic;
import nwsim.env.Router;
import nwsim.network.ForwardHistory;
import nwsim.network.Packet;
import nwsim.network.RouteInfo;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * RIP (Routing Information Protocol)のクラスです．
 */
public class RIP extends AbstractRouting{

    public RIP(long exchangeSpan) {
        super(exchangeSpan);
    }

    public RIP(long exchangeSpan, Router router) {
        super(exchangeSpan, router);
    }

    @Override
    public double calcMetric(RouteInfo info) {
        return 0;
    }

    @Override
    public HashMap<String, HashMap<String, RouteInfo>> getUpdatedRouteMap() {
        //自分の持つものすべてを返す
        return this.router.getRoutingTable();
        //return super.getUpdatedRouteMap();
    }

    public  synchronized Serializable deepCopy(Serializable obj){
        //System.gc();


        try{
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(obj);
            out.close();
            byte[] bytes = bout.toByteArray();
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object newObject = in.readObject();
            in.close();
            return (Serializable) newObject;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public synchronized boolean updateRouteMap(Packet p, Nic atNic) {
        //パケットから，先方からやってきた経路情報を取得する．

        HashMap<String, HashMap<String, RouteInfo>> routeMap = (HashMap<String, HashMap<String, RouteInfo>>)p.getAplMap();
        HashMap<String, HashMap<String, RouteInfo>> myMap = this.router.getRoutingTable();
        LinkedList<RouteInfo> rList = new LinkedList<RouteInfo>();
        synchronized (myMap){
            Iterator<String> nwIte = routeMap.keySet().iterator();
            while(nwIte.hasNext()){
                String nw = nwIte.next();
                if(myMap.containsKey(nw)){
                    //かつ，NextHopを見て一致するものを見る．
                    HashMap<String, RouteInfo> tmpMap = routeMap.get(nw);
                    Iterator<String> nMap = tmpMap.keySet().iterator();
                    while(nMap.hasNext()){
                        //パケット側のnextHop
                        String nextHop = nMap.next();
                        HashMap<String, RouteInfo> myMap2 = myMap.get(nw);
                        if(myMap2.containsKey(nextHop)){
                            RouteInfo info = tmpMap.get(nextHop);
                            RouteInfo myInfo = myMap2.get(nextHop);
                            if(info.getLastUpdatedTime() >= myInfo.getLastUpdatedTime()){
                                //新規登録する．
                               // this.addNewRouteInfo(p, info, atNic);
                                rList.add(info);
                            }

                        }else{
                            RouteInfo info = tmpMap.get(nextHop);
                            //this.addNewRouteInfo(p, info, atNic);
                            rList.add(info);

                        }

                    }


                }else{
                    //無ければ新規登録
                    HashMap<String, RouteInfo> rMap = routeMap.get(nw);
                    Iterator<RouteInfo> routeIte = rMap.values().iterator();
                    while(routeIte.hasNext()){
                        RouteInfo info = routeIte.next();
                       // this.addNewRouteInfo(p, info, atNic);
                        rList.add(info);
                    }

                }
            }
        }
        //最後に登録
        Iterator<RouteInfo> infoIte = rList.iterator();
        while(infoIte.hasNext()){
            RouteInfo info = infoIte.next();
            this.addNewRouteInfo(p, info, atNic);
        }

        return true;
        //return super.updateRouteMap(p);
    }

    public void processMetric(Packet p, Nic atNic){
        if (p.getToIP().equals(atNic.getIpAddress())) {
            //pのaplMapを見て，宛先NWアドレスのエントリがあれば，
            //NextHopを見る．p->fromIP == NextHopであれば，そのmetric
            //を，pのaplMapの値+1をセットする．

            //その前に，転送しない条件チェック
            //p->fromIPがすでに履歴にあれば終了．
            Iterator<ForwardHistory> fIte = p.getRequestHistoryList().iterator();
            boolean isForwarded = false;
            while (fIte.hasNext()) {
                ForwardHistory history = fIte.next();
                if (history.getFromID().equals(p.getFromIP())) {
                    isForwarded = true;
                    break;
                }
            }
            if (isForwarded) {
                return;
            }
            HashMap<String, String> metricMap = (HashMap<String, String>) p.getAplMap();
            String nwAddress = metricMap.get("nwaddress");


            if (this.router.getRoutingTable().containsKey(nwAddress)) {
                //経路情報のリストを返す．
                HashMap<String, RouteInfo> rMap = this.router.getRoutingTable().get(nwAddress);
                if (rMap.containsKey(p.getFromIP())) {
                    RouteInfo info = rMap.get(p.getFromIP());
                    int val = Integer.valueOf(metricMap.get("metric")).intValue();
                    val++;
                    info.setMetric(val);
                    //タイムスタンプ更新
                    info.setLastUpdatedTime(System.currentTimeMillis());
                    //メトリックも反映させる．
                    metricMap.put("metric", String.valueOf(val));
                    //あとは各NextHopに対して，パケットを送信する．
                    Iterator<HashMap<String, RouteInfo>> rIte = this.router.getRoutingTable().values().iterator();
                    while(rIte.hasNext()){
                        HashMap<String, RouteInfo> routeMap = rIte.next();
                        Iterator<String> nextIte = routeMap.keySet().iterator();
                        while(nextIte.hasNext()){
                            String nextHop = nextIte.next();
                            if(nextHop.equals(atNic.getIpAddress())){
                                continue;
                            }
                            if(nextHop.startsWith("192.168.0")){
                               continue;
                            }
                            RouteInfo fInfo = routeMap.get(nextHop);
                            Nic fromNic = this.router.getNicMap().get(fInfo.getNicName());
                            HashMap<String, String> map = this.router.getArpMap().get(fInfo.getNicName());
                            String nextHopMac = map.get(nextHop);
                            Nic nextHopNic = Env.getIns().getNicMap().get(nextHopMac);
                            //nextHop向けにパケット生成
                            Packet p_metric = this.router.genCtrlPacket(fromNic, nextHopNic, 1111, Param.MSG_TYPE.RIP_METRIC);
                            //↓の1は，ホップするたびに+1してください．
                            p.setRequest(true);
                            HashMap<String, String> ripMap = new HashMap<String, String>();
                            ripMap.put("metric", String.valueOf(val));
                            ripMap.put("nwaddress",nwAddress);
                            p_metric.setAplMap(ripMap);
                            this.router.sendPacket(p_metric, fromNic);

                        }
                    }

                }

            }
        }

    }
}
