package ru.korshun.cobaguardserver;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Root {

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {

        SystemTrayIcon.createTrayIcon();

        ExecutorService executorService =                               null;

        try(ServerSocket serverSocket = new ServerSocket(Settings.getInstance().getConnectPort());
            ServerSocket serverFileSocket = new ServerSocket(Settings.getInstance().getConnectPortFiles())) {


            try {
                serverFileSocket.setSoTimeout(Settings.getInstance().getAcceptTimeOut());
            } catch (SocketException e) {
                e.printStackTrace();
            }

            executorService =                                           Executors.newCachedThreadPool();

            System.out.println("Сервер запущен");

            while (true) {

                Socket clientSocket =                                   serverSocket.accept();

                executorService.submit(new ClientConnectThread(clientSocket, serverFileSocket));

                System.out.println("Клиент подключился: " + clientSocket);
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