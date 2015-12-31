package ru.korshun.cobaguardserver;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Root {


    public static boolean isError =                                     false;


    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {

        SystemTrayIcon.createTrayIcon();

        ExecutorService executorService =                               null;

        while(true) {

            try (ServerSocket serverSocket = new ServerSocket(Settings.getInstance().getConnectPort());
                 ServerSocket serverFileSocket = new ServerSocket(Settings.getInstance().getConnectPortFiles())) {

                executorService =                                       Executors.newCachedThreadPool();

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