package nwsim.env;

import nwsim.Param;
import nwsim.network.Packet;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Hidehiro Kanemitsu
 */
public class Computer extends Node {

    /**
     * 保留時間
     */
    protected double lambda;

    public Computer(String iD, int type, HashMap<String, Nic> nicMap) {
        super(iD, type, nicMap);
        //要求発生確率
        this.lambda = Param.genDouble(Param.request_exp_dist_lambda_min, Param.request_exp_dist_lambda_max, 1, 0.5);


    }

    @Override
    public void run() {
        //まずはすべてのNICを稼働させる．
        this.startUpNics();
        while (true) {
            try {
                Thread.sleep(0);
                //リクエスト送信することが決まったら，指数分布の累積分布関数の確率に従い，
                //要求パケットを送る．
                double t = 1;
                double comulative_p = 0.0d;
                while (true) {
                    //指数分布による，累積の確率密度を算出．
                    comulative_p = 1 - Math.pow(Math.E, (-1) * t * this.lambda);
                    //1秒だけ待つ．
                    Thread.sleep(1000);
                    double randomValue = Math.random();
                    //パケット送信可能状態となった
                    if (randomValue <= comulative_p) {
                        break;
                    }
                    t++;
                }

                //要求パケット送信開始
                this.requestProcess();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 要求を生成し，そして送信する処理です．
     *
     * @return
     */
    public Packet requestProcess() {
        //まずは要求の種類を決める．
        int req_type_index = Param.genInt(0, Param.req_type.length - 1, 0, 0);
        int req_type = Param.req_type[req_type_index];
        //req_type = 0;

        switch (req_type) {
            case Param.HTTP_GET_REQUEST:
                long remainedSize = Param.http_get_request_size;
                Packet p = this.genInitialPacket(80);
                p.setReqPacketSize(remainedSize);

                p.setType(Param.MSG_TYPE.HTTP_GET);
                remainedSize -= p.getPacketSize();
                long count = remainedSize / p.getPacketSize() + 1;

                Nic nic = this.nicMap.get("eth0");
                this.processPacket(p, nic);
                for (int i = 0; i < count; i++) {
                    long size = p.getPacketSize();

                    int flg = Param.PACKET_MID;
                    if (i >= count - 1) {
                        size = remainedSize;
                        flg = Param.PACKET_END;
                    }

                    Packet p2 = this.createPacekt(flg, p.getTranID(), p.getHeaderSize(), p.getFromIP(), p.getToIP(),
                            p.getFromPort(), p.getToPort(), p.getData());
                    p2.setMinBW((long) nic.getBw());
                    p2.setType(Param.MSG_TYPE.HTTP_GET);
                    p2.setReqPacketSize(p.getReqPacketSize());

                    remainedSize -= p2.getPacketSize();

                    this.processPacket(p2, nic);
                }

                break;
            case Param.HTTP_POST_REQUEST:
                remainedSize = Param.genLong(Param.http_post_request_size_min, Param.http_post_request_size_max,
                        1, Param.http_post_request_size_mu) * 1024 * 1024;
                //long byteSize = remainedSize * 1024 * 1024;
                p = this.genInitialPacket(80);
                p.setReqPacketSize(remainedSize);

                p.setType(Param.MSG_TYPE.HTTP_POST);
                remainedSize -= p.getPacketSize();
                count = remainedSize / p.getPacketSize() + 1;

                nic = this.nicMap.get("eth0");
                this.processPacket(p, nic);
                for (int i = 0; i < count; i++) {
                    long size = p.getPacketSize();

                    int flg = Param.PACKET_MID;
                    if (i >= count - 1) {
                        size = remainedSize;
                        flg = Param.PACKET_END;
                    }

                    Packet p2 = this.createPacekt(flg, p.getTranID(), p.getHeaderSize(), p.getFromIP(), p.getToIP(),
                            p.getFromPort(), p.getToPort(), p.getData());
                    p2.setMinBW((long) nic.getBw());
                    p2.setType(Param.MSG_TYPE.HTTP_POST);
                    p2.setReqPacketSize(p.getReqPacketSize());

                    remainedSize -= p2.getPacketSize();

                    this.processPacket(p2, nic);
                }

                break;

            default:
                throw new IllegalStateException("Unexpected value: " + req_type);
        }
        /*
        Packet p = this.genInitialPacket();
        Nic nic = this.nicMap.get("eth0");
        p.setFlag(Param.PACKET_MID);
        this.processPacket(p, nic);
        */
        return null;

    }


    /**
     * 初期のパケットを生成します．
     * 宛先IP/Portと送信元IP/Port，データのみを設定します．
     * @param toPort 宛先のポート番号
     * @return
     */
    public Packet genInitialPacket(int toPort) {
        // Packet p = new Packet();
        //宛先を決める．とりあえずルータにする．
        String target;
        switch(toPort){
            case 80:
                //HTTPの場合は，LANへの80ポートのDNAT登録ずみのルータIPリストから選択する．
                int len =  Env.getIns().getHTTP_DNAT_RouterList().size();
                int idx = Param.genInt(0, len - 1, 0, 0.5);

                target = Env.getIns().getHTTP_DNAT_RouterList().get(idx);
                break;
            default:
                Object[] rList = Env.getIns().getRouterIPMap().keySet().toArray();
                len = rList.length;
                idx = Param.genInt(0, len - 1, 0, 0.5);
                target = (String) rList[idx];
                break;
        }

        Iterator<Nic> nIte = this.getNicMap().values().iterator();
        Nic nic = nIte.next();

        Packet p = new Packet(Param.header_size, nic.getIpAddress(), target,
                Param.genInt(1025, 65535, 0, 0.5), toPort, "test");
        p.setTranID(this.getID() + "-" + System.currentTimeMillis());

        //p.setType(Param.MSG_TYPE.TCP);
        p.setFlag(Param.PACKET_START);

        return p;

    }


    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }
}
