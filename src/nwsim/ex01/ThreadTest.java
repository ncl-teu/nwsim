package nwsim.ex01;

public class ThreadTest {
    public static void main(String[] args) {
        MyThread t1 = new MyThread("tokyo");
        MyThread t2 = new MyThread("hokkaido");

        Thread th1 = new Thread(t1);
        Thread th2 = new Thread(t2);

        th1.start();
        th2.start();
        for (int i = 0; i < 100; i++) {
            System.out.println("mainでのメッセージ" + i);
        }
    }
}
