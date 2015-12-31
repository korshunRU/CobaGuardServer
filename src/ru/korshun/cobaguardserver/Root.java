package ru.korshun.cobaguardserver;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Root {


    public static boolean isError =                                     false;


    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {

        SystemTrayIcon.createTrayIcon();

        ExecutorService executorService =                               null;

        Timer timer =                                                   new Timer();

        while(true) {

            try (ServerSocket serverSocket = new ServerSocket(Settings.getInstance().getConnectPort());
                 ServerSocket serverFileSocket = new ServerSocket(Settings.getInstance().getConnectPortFiles())) {

                executorService =                                       Executors.newCachedThreadPool();

                timer.schedule(new FakeConnect(), 0, 2000);

                System.out.println("Сервер запущен");

                while (true) {

                    synchronized (Root.class) {

                        if (isError) {
                            System.out.println("ОШИБКА! ПЕРЕЗАПУСК!");
                            isError =                                   false;
                            break;
                        }

                    }

                    executorService.submit(new ClientConnectThread(serverSocket.accept(), serverFileSocket));
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }
            }

        }

    }

}