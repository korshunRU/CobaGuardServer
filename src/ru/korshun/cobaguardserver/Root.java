package ru.korshun.cobaguardserver;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Root {


    static boolean isError =                                     false;


    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {

        SystemTrayIcon.createTrayIcon();

        ExecutorService executorService =                               null;

        new Timer().schedule(new FakeConnect(), 0, 2000);

        while(true) {

            try (ServerSocket serverSocket = new ServerSocket(Settings.getInstance().getConnectPort());
                 ServerSocket serverFileSocket = new ServerSocket(Settings.getInstance().getConnectPortFiles())) {

                executorService =                                       Executors.newCachedThreadPool();

                System.out.println("Сервер запущен");

                while (true) {

                    if (isError) {
                        System.out.println("ОШИБКА! ПЕРЕЗАПУСК!");
                        isError =                                   false;
                        break;
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