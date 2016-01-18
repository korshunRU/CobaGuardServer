package ru.korshun.cobaguardserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.TimerTask;



public class FakeConnect
        extends TimerTask {

    @Override
    public void run() {

        try(Socket socket = new Socket("localhost", 6666);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("disconnect");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
