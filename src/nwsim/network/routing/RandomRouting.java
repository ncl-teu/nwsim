package nwsim.network.routing;

import nwsim.network.RouteInfo;

/**
 * Created by Hidehiro Kanemitsu on 2021/10/27
 */
public class RandomRouting extends AbstractRouting{

    public RandomRouting(long exchangeSpan) {
        super(exchangeSpan);
    }



    @Override
    public double calcMetric(RouteInfo info) {
        double val = Math.random();
        info.setMetric(val);
        return val;
    }
}
