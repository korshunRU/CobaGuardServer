package ru.korshun.cobaguardserver;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Root {

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {

        SystemTrayIcon.createTrayIcon();

        ExecutorService executorService =                               null;

        try(ServerSocket serverSocket = new ServerSocket(Settings.getInstance().getConnectPort());
            ServerSocket serverFileSocket = new ServerSocket(Settings.getInstance().getConnectPortFiles())) {

            executorService =                                           Executors.newCachedThreadPool();

            System.out.println("Сервер запущен");

            while (true) {
                executorService.submit(new ClientConnectThread(serverSocket.accept(), serverFileSocket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(executorService != null) {
                executorService.shutdown();
            }
        }

    }

}