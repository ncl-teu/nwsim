package nwsim.logger;

import nwsim.Param;
import nwsim.env.Env;
import nwsim.env.Statistics;
import nwsim.network.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

public class NWLog implements Runnable{
    // getLoggerの引数はロガー名を指定する。
    // log4j2では、ロガー名の指定が省略可能になった。
    private static Logger logger;

    public static NWLog own;

    LinkedBlockingQueue<String> infoQueue;

    public static NWLog getIns() {
        if (NWLog.own == null) {
            NWLog.own = new NWLog();
        }
        return NWLog.own;
    }

    private NWLog() {
        NWLog.logger = LogManager.getLogger();
        this.infoQueue = new LinkedBlockingQueue<String>();
    }

    @Override
    public void run() {
        while(true){
            if(!this.infoQueue.isEmpty()){
                String info = this.infoQueue.poll();
                this.write(info);
            }
        }
    }

    public void tranLog(Packet p, String result){
        Statistics stat = Env.getIns().findStat(p.getTranID());

        long totalReqTime = stat.getReqFinishTime() - stat.getReqStartTime() + p.getTotalDelay();
        long totalResTime = stat.getResFinishTime() - stat.getResStartTime() + p.getTotalDelay();
        double req_avg_buffer_size = Param.getRoundedValue(stat.getTotalReqReceiveBufferSize()
                / (double)(p.getRequestHistoryList().size() * stat.getReqCount()+1));
        double res_avg_buffer_size = Param.getRoundedValue(stat.getTotalResReceiveBufferSize()
                / (double)(p.getResponseHistoryList().size() * stat.getResCount()+1));

        double req_avg_queue_size = Param.getRoundedValue(stat.getTotalReqQueueSize()
                / (double)(p.getRequestHistoryList().size() * stat.getReqCount()+1));
        double res_avg_queue_size = Param.getRoundedValue(stat.getTotalResQueueSize()
                / (double)(p.getResponseHistoryList().size() * stat.getResCount()+1));
        NWLog.getIns().log(","+p.getType()+","+p.getTranID() + ","+ p.getRequestHistoryList().getFirst().getFromID() + ","+ p.getRequestHistoryList().getLast().getToID() + ","
                + p.getMinBW()*8/(1024) + ","+
                totalReqTime + "," + p.getRequestHistoryList().size() +
                ","+ req_avg_buffer_size + "," +stat.getMaxReqReceiveBufferSize() + "," + req_avg_queue_size + "," +stat.getMaxReqQueueSize() + "," +p.getReqPacketSize() + "," +
                totalResTime+","+p.getResponseHistoryList().size()+
                ","+ res_avg_buffer_size + "," + stat.getMaxResReceiveBufferSize() + ", " + res_avg_queue_size + "," + stat.getMaxResQueueSize() + ", "+p.getTotalDataSize() + ","+ result);

    }

    /**
     * ログ出力する．
     * 
     * @param m
     */
    public void log(String m) {
        //logger.info(m);
        this.infoQueue.offer(m);
    }

    private void write(String m){
        logger.info(m);
    }

}
