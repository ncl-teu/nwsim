package nwsim.network.routing;

import nwsim.env.Nic;
import nwsim.env.Router;
import nwsim.network.Packet;
import nwsim.network.RouteInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * ルーティングプロトコルの
 * 基本となるクラスです．
 * 各経路に対して，メトリックを決めるのが目的．
 */
public abstract class AbstractRouting {

    /**
     * 経路情報を交換する時間間隔です．単位は秒．
     */
    protected long exchangeSpan;

    /**
     * 経路情報を交換するためのスレッドです．
     * exchangeSpan秒毎に，このrun()が呼び出されます．
     */
    protected ExchangeTimer ex;

    /**
     * このルーティングを使うルータ
     */
    protected Router router;


    /**
     * コンストラクタ
     * @param exchangeSpan
     */
    public AbstractRouting(long exchangeSpan) {
        this.exchangeSpan = exchangeSpan;
    }

    /**
     * コンストラクタ
     * @param exchangeSpan
     * @param router
     */
    public AbstractRouting(long exchangeSpan, Router router) {
        this.exchangeSpan = exchangeSpan;
        this.router = router;
    }

    public Router getRouter() {
        return router;
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    /**
     * 指定の経路に対してメトリックを計算する．
     * @param info
     * @return
     */
    public abstract double calcMetric(RouteInfo info);






    public ExchangeTimer getEx() {
        return ex;
    }

    public void setEx(ExchangeTimer ex) {
        this.ex = ex;
    }

    /**
     * 複数の経路から，最もメトリックが小さいものを選択する．
     *
     * @param routeList
     * @return
     */
    public RouteInfo selectRoute(LinkedList<RouteInfo> routeList){
        Iterator<RouteInfo> rIte = routeList.iterator();
        double minMetric = Double.MAX_VALUE;
        RouteInfo retInfo = new RouteInfo();

        while(rIte.hasNext()){
            RouteInfo info = rIte.next();
            double val = info.getMetric();
            if(val <= minMetric){
                minMetric = val;
                retInfo = info;
            }
        }
        return retInfo;
    }

    /**
     * CTRLパケットの要求が到着した場合の処理です．
     * ここで，経路情報を返すようにしてください．
     * @return
     */
    public HashMap<String, HashMap<String, RouteInfo>> getUpdatedRouteMap(){
        return null;

    }

    /**
     * CTRLパケットの応答が到着した場合の処理です．
     * ここで，受け取った経路情報をどうするかを書いてください．
     * @param p 受信した応答パケット
     * @return
     */
    public boolean updateRouteMap(Packet p){
        return false;

    }

    /**
     * メトリック値を更新します．
     * @param info
     * @return 値が前と変わればtrue, 同じであればfalseを返す．
     */
    public boolean updateMetric(RouteInfo info){
        double oldVal = info.getMetric();
        double metric = this.calcMetric(info);
        info.setMetric(metric);
        if(oldVal == metric){
            return false;
        }else{
            return true;
        }

    }

    public long getExchangeSpan() {
        return exchangeSpan;
    }

    public void setExchangeSpan(long exchangeSpan) {
        this.exchangeSpan = exchangeSpan;
    }
}
