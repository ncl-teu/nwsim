package nwsim.main;

import nwsim.Param;
import nwsim.env.Env;
import nwsim.logger.NWLog;

public class Main {
    public static void main(String[] args) {

        // paramの初期設定
        Param.getIns();
        //環境の初期設定
        Env.getIns().initialize();

        //ログ出力用スレッドを起動
        Thread writer = new Thread(NWLog.getIns());
        writer.start();

        NWLog.getIns().log("# of PCs: " + Env.getIns().getPcMap().size() + "/# of Routers:"+Env.getIns().getRouterMap().size());
        //NWLog.getIns().log("This is a pen");
        NWLog.getIns().log("Time,Protocol,TranID, fromID, toID, minBW(Mbps), reqDuration(ms), " +
                "reqHops, req_avg_receive_buffersize, req_max_receive_buffersize, req_avg_queue_size, req_max_queue_size, reqSize(byte), resDuraiton(ms), " +
                "resHop, res_avg_receive_buffersize, res_max_receive_buffersize, res_avg_queue_size, res_max_queue_size, resSize(byte), result(Hit/TTL/Drop)");
    }

}
