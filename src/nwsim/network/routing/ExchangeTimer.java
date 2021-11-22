package nwsim.network.routing;

import nwsim.Param;
import nwsim.env.Env;
import nwsim.env.Nic;
import nwsim.env.Router;
import nwsim.network.Packet;
import nwsim.network.RouteInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;

public class ExchangeTimer extends TimerTask implements  Runnable{

    /**
     * 自身が所属するルータ
     */
    protected Router router;

    @Override
    public void run() {
        HashSet<String> nextHopSet = new HashSet<String>();

        //ここに，他ルータとの通信処理を書く．
        //ルーティングテーブルのNextHopたちへpingを投げる．
        Iterator<HashMap<String, RouteInfo>> rIte = router.getRoutingTable().values().iterator();
        while(rIte.hasNext()){
            HashMap<String, RouteInfo> info = rIte.next();

            //NextHopのイテレータ
            Iterator<String> nextIte = info.keySet().iterator();
            while(nextIte.hasNext()){
                String nextHop = nextIte.next();
                RouteInfo rInfo = info.get(nextHop);
                Nic fromNic = this.router.getNicMap().get(rInfo.getNicName());
                if(nextHopSet.contains(nextHop)){
                    continue;
                }
                HashMap<String, String> map = this.router.getArpMap().get(rInfo.getNicName());
                String nextHopMac = map.get(nextHop);
                Nic nextHopNic = Env.getIns().getNicMap().get(nextHopMac);
                //nextHop向けにパケット生成
                Packet p = this.router.genCtrlPacket(fromNic, nextHopNic, 1111, Param.MSG_TYPE.CTRL);
                this.router.sendPacket(p, fromNic);


            }
        }

    }

    public ExchangeTimer(Router router) {
        this.router = router;
    }

    public Router getRouter() {
        return router;
    }

    public void setRouter(Router router) {
        this.router = router;
    }
}
