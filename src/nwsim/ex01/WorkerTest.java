package nwsim.ex01;

public class WorkerTest {
    public static void main(String[] args){
        WorkerThread thA = new WorkerThread("WorkerA");
        WorkerThread thB = new WorkerThread("WorkerB");
        Thread tA = new Thread(thA);
        Thread tB = new Thread(thB);
        tA.start();
        tB.start();

        //それぞれのスレッド内のキューへ入れる．
        for(int i=0;i<100;i++){
            System.out.println("-----Put:"+i);
            try{
                Thread.sleep(100);
            }catch(Exception e){
                e.printStackTrace();
            }
            thA.getQueue().offer(i);
            thB.getQueue().offer(i);

        }


    }
}
