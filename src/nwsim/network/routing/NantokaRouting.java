package nwsim.network.routing;

import nwsim.env.Router;
import nwsim.network.Packet;
import nwsim.network.RouteInfo;

import java.util.HashMap;

public class NantokaRouting extends AbstractRouting{
    public NantokaRouting(long exchangeSpan, Router router) {
        super(exchangeSpan, router);
    }

    @Override
    public double calcMetric(RouteInfo info) {
        return 0;
    }

    @Override
    public HashMap<String, HashMap<String, RouteInfo>> getUpdatedRouteMap() {
        return super.getUpdatedRouteMap();
    }

    @Override
    public boolean updateRouteMap(Packet p) {
        return super.updateRouteMap(p);
    }
}
