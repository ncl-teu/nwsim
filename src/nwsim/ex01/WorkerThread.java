package nwsim.ex01;

import java.util.concurrent.LinkedBlockingQueue;

public class WorkerThread implements Runnable {
    /**
     * 名前
     */
    private String name;

    /**
     * Intergerを格納するためのキュー（バッファ）
     */
    private LinkedBlockingQueue<Integer> queue;

    public WorkerThread(String val) {
        this.name = val;
        this.queue = new LinkedBlockingQueue<Integer>();
    }

    @Override
    public void run() {
        // 無限ループ
        while (true) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (this.queue.isEmpty()) {
                continue;
            } else {
                // キューの先頭を取り出す．(先頭は削除される)
                Integer val = this.queue.poll();
                System.out.println("**取り出し@" + this.name + "/" + val);
            }

        }

    }

    /**
     * @return String return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public LinkedBlockingQueue<Integer> getQueue() {
        return queue;
    }

    public void setQueue(LinkedBlockingQueue<Integer> queue) {
        this.queue = queue;
    }

}
