package ru.korshun.cobaguardserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;


public class Root {

    private int PORT;
    private int PORT_FILES;
    private int ACCEPT_TIMEOUT;
    public static String COBA_PATH_NAME;
    public int test = 1;

    private final String CONFIG_FILE = "coba.conf";
    private HashMap<String, String> params = new HashMap<>();

    private Root() {
        readFile();
        setParams();
    }




    private void readFile(){
        String line;
        try(BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE))) {
            while((line = br.readLine()) != null){

                if(line.length() > 0 && !line.startsWith("#") && line.contains("=")) {
                    String parts[] = line.split("=");
                    params.put(parts[0], parts[1]);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
            Logging.writeToFile("error", "Ошибка доступа к файлу конфига");
        }
    }


    private void setParams() {
        PORT =                      Integer.parseInt(params.get("CONNECT_PORT"));
        PORT_FILES =                Integer.parseInt(params.get("DOWNLOAD_PORT"));
        ACCEPT_TIMEOUT =            Integer.parseInt(params.get("ACCEPT_TIMEOUT"));
        COBA_PATH_NAME =            params.get("IMG_PATH");
    }



//1


    private void serverStart() {

        ServerSocket s = null, s1 = null;

        try {

            s = new ServerSocket(PORT);
            s1 = new ServerSocket(PORT_FILES);
            s1.setSoTimeout(ACCEPT_TIMEOUT * 1000);

            SystemTrayIcon.createIcon();

            System.out.println("Сервер запущен");

            while (true) {

                Socket client = s.accept();

                new ClientConnect(s1, client).start();

                System.out.println("Клиент подключился: " + client);
                Logging.writeToFile("access", "Клиент подключился: " + client);

            }

        } catch (IOException e) {
            e.printStackTrace();
            Logging.writeToFile("error", e.getMessage());
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (s1 != null) {
                    s1.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Logging.writeToFile("error", e.getMessage());
            }
        }

    }


    public static void main(String[] args) {

        new Root().serverStart();

    }

}




class ClientConnect
    extends Thread {

    private ServerSocket s1;
    private Socket client;
    private String deviceId;
    private int filesCount;

    public ClientConnect(ServerSocket s1, Socket client) {
        this.s1 = s1;
        this.client = client;

        System.out.println("Сессия для " + client + " открыта");
    }

    public void run(){

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true)
        ) {

            ArrayList<String> listNewFiles = new ArrayList<>();

            String query;

            while (true) {

                TimeUnit.MILLISECONDS.sleep(1000);

                query = in.readLine();



                if(query == null) {
                    System.out.println("query == null   ------   DISCONNECT");
                    break;
                }




                //Клиент отсоединился
                if (query.equals("disconnect")) {
                    out.println("close");
                    out.flush();

                    System.out.println(deviceId + ": Обновление завершено, клиент отключился");

                    Logging.writeToFile("access", "Обновление завершено, клиент " + this.client +
                            " отключился \r\n\r\n====================================================================================== \r\n");
                    Logging.writeToFile(deviceId, "access", "Обновление завершено, " +
                            "клиент отключился \r\n\r\n====================================================================================== \r\n");

                    break;
                }




                //Клиент сделал запрос на кол-во новых файлов
                if (query.startsWith("getFilesNew")) {

                    String lastUpdateDate[] = query.split(":");
                    deviceId = lastUpdateDate[2];
                    System.out.println(deviceId + ": запрашивается количество новых файлов");

                    Logging.writeToFile(deviceId, "access", "Сессия открыта");
                    Logging.writeToFile(deviceId, "access", "Запрашивается количество новых файлов");


                    for (File file : new File(Root.COBA_PATH_NAME).listFiles()) {

                        if (file.isFile()) {

                            if ((Long.parseLong(lastUpdateDate[1]) - file.lastModified()) < 0) {

                                listNewFiles.add(file.getName());

                            }

                        }

                    }

                    System.out.println(deviceId + ": Новых файлов " + listNewFiles.size());
                    Logging.writeToFile(deviceId, "access", "Новых файлов " + listNewFiles.size());

                    out.println(listNewFiles.size());
                    out.flush();

                    filesCount = listNewFiles.size();

                    continue;

                }


                //Клиент сделал запрос на скачивание
                if (query.equals("download") & filesCount > 0) {

                    System.out.println(deviceId + ": Получен запрос на скачивание");
                    Logging.writeToFile(deviceId, "access", "Получен запрос на скачивание");

                    Socket client1;

                    for (String newFile : listNewFiles) {

                        String tmpPath = Root.COBA_PATH_NAME + File.separator + deviceId;
                        File fileName = new File(tmpPath + File.separator + newFile);

                        Logging.writeToFile(deviceId, "access", "Шифруем: " + newFile);

                        ImgEncode
                                .getInstance(newFile, Root.COBA_PATH_NAME, tmpPath)
                                .encodeImg();

                        out.println(newFile);
                        out.flush();

                        out.println(fileName.length());
                        out.flush();

                        System.out.println(deviceId + ": Ожидаем подключения для скачивания ...");
                        Logging.writeToFile(deviceId, "access", "Ожидаем подключения для скачивания ...");

                        client1 = s1.accept();

                        System.out.println(deviceId + ": Клиент для скачивания подключился, отправляем: " + newFile);
                        Logging.writeToFile(deviceId, "access", "Клиент для скачивания подключился, отправляем: " + newFile);

                        FileInputStream fis = new FileInputStream(fileName);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        DataOutputStream dos = new DataOutputStream(client1.getOutputStream());

                        byte[] buffer = new byte[32 * 1024];
                        int count, total = 0;

                        while ((count = bis.read(buffer, 0, buffer.length)) != -1) {
                            total += count;
                            dos.write(buffer, 0, count);
                            dos.flush();
                        }

                        System.out.println(deviceId + ": Файл " + newFile + " передан");
                        Logging.writeToFile(deviceId, "access", "Файл " + newFile + " передан");

                        fis.close();
                        bis.close();
                        dos.close();

                        client1.close();

                        fileName.delete();

                        System.out.println(deviceId + ": Клиент для скачивания отключился");
                        Logging.writeToFile(deviceId, "access", "Клиент для скачивания отключился");

                    }

                }


            }

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            Logging.writeToFile("error", e.getMessage());
        }

        finally {
            try {
                client.close();
                System.out.println(deviceId + ": Соединение закрыто. client.close()");
                Logging.writeToFile(deviceId, "access", "Соединение закрыто");
            } catch (IOException e) {
                e.printStackTrace();
                Logging.writeToFile("error", e.getMessage());
            }
        }

    }

}