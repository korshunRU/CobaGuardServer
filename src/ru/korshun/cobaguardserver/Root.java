package ru.korshun.cobaguardserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Root {

    private int PORT;
    private int PORT_FILES;
    private int ACCEPT_TIMEOUT;
    public static String COBA_PATH_NAME;

    ExecutorService executorService;

    protected static final int MAX_ERROR_CONNECT =          3;

    private final String CONFIG_FILE =                      "coba.conf";
    private HashMap<String, String> params =                new HashMap<>();



    private Root() {
        readFile();
        setParams();

        SystemTrayIcon.createIcon();

        executorService =                                   Executors.newCachedThreadPool();
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



    private void serverStart() {

        ServerSocket socketConnect = null, socketSendFile = null;

        try {

            socketConnect =                                 new ServerSocket(PORT);
            socketSendFile =                                new ServerSocket(PORT_FILES);
            socketSendFile
                    .setSoTimeout(ACCEPT_TIMEOUT * 1000);

            System.out.println("Сервер запущен");

            while (true) {

                Socket socketClientConnect = socketConnect.accept();

//                new ClientConnect(socketSendFile, socketClientConnect).start();

                executorService.submit(new ClientConnect(socketSendFile, socketClientConnect));

                System.out.println("Клиент подключился: " + socketClientConnect);
                Logging.writeToFile("access", "Клиент подключился: " + socketClientConnect);

            }

        } catch (IOException e) {
            e.printStackTrace();
            Logging.writeToFile("error", e.getMessage());
        } finally {
            try {
                if (socketConnect != null) {
                    socketConnect.close();
                }
                if (socketSendFile != null) {
                    socketSendFile.close();
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
    implements Runnable {

    private ServerSocket s1;
    private Socket connectClient;
    private String deviceId;
    private int filesCount;

    public ClientConnect(ServerSocket s1, Socket ConnectClient) {
        this.s1 = s1;
        this.connectClient = ConnectClient;

        System.out.println("Сессия для " + ConnectClient + " открыта");
    }

    @Override
    public void run() {

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(connectClient.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(connectClient.getOutputStream())), true)
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

                    Logging.writeToFile("access", "Обновление завершено, клиент " + this.connectClient +
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

                    File cobaPath =                             new File(Root.COBA_PATH_NAME);
                    File[] countFiles =                         cobaPath.listFiles();

                    if (cobaPath.isDirectory() && countFiles != null) {

                        for (File file : countFiles) {

                            if (file.isFile()) {

                                if ((Long.parseLong(lastUpdateDate[1]) - file.lastModified()) < 0) {

                                    listNewFiles.add(file.getName());

                                }

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

//                    Socket sendFileClient =             null;
                    int countErrorConnection =          0;

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

                        Socket sendFileClient =                     null;

                        try {
                            sendFileClient = s1.accept();
                        } catch (IOException e) {
                            System.out.println(deviceId + ": ОШИБКА ПОДКЛЮЧЕНИЯ 6667");
//                            countErrorConnection++;

                            if(++countErrorConnection >= Root.MAX_ERROR_CONNECT) {
                                System.out.println(deviceId + ": ИСЧЕРАПАН ЛИМИТ ПОДКЛЮЧЕНИЙ, ОТКЛЮЧАЕМСЯ");
                                break;
                            }

//                        } finally {
//                            if(countErrorConnection >= Root.MAX_ERROR_CONNECT) {
//                                break;
//                            }
                        }

                        if(sendFileClient != null) {

                            try(FileInputStream fis = new FileInputStream(fileName);
                                BufferedInputStream bis = new BufferedInputStream(fis);
                                DataOutputStream dos = new DataOutputStream(sendFileClient.getOutputStream())) {

                                System.out.println(deviceId + ": Клиент для скачивания подключился, отправляем: " + newFile);
                                Logging.writeToFile(deviceId, "access", "Клиент для скачивания подключился, отправляем: " + newFile);

//                                FileInputStream fis = new FileInputStream(fileName);
//                                BufferedInputStream bis = new BufferedInputStream(fis);
//                                DataOutputStream dos = new DataOutputStream(sendFileClient.getOutputStream());

                                byte[] buffer = new byte[32 * 1024];
                                int count, total = 0;

                                while ((count = bis.read(buffer, 0, buffer.length)) != -1) {
                                    total += count;
                                    dos.write(buffer, 0, count);
                                    dos.flush();
                                }

                                System.out.println(deviceId + ": Файл " + newFile + " передан");
                                Logging.writeToFile(deviceId, "access", "Файл " + newFile + " передан");

                            } catch (IOException e) {
                                System.out.println(deviceId + ": ОШИБКА ПЕРЕДАЧИ ФАЙЛА");
                            } finally {
                                sendFileClient.close();
//                                sendFileClient =                            null;

                                if(!fileName.delete()) {
                                    System.out.println(deviceId + ": ОШИБКА УДАЛЕНИЯ ВРЕМЕННОГО ФАЙЛА");
                                }

                                System.out.println(deviceId + ": Клиент для скачивания отключился");
                                Logging.writeToFile(deviceId, "access", "Клиент для скачивания отключился");
                            }

//                            fis.close();
//                            bis.close();
//                            dos.close();

//                            sendFileClient.close();

//                            fileName.delete();
//
//                            System.out.println(deviceId + ": Клиент для скачивания отключился");
//                            Logging.writeToFile(deviceId, "access", "Клиент для скачивания отключился");

                        }

                    }

                }


            }

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            Logging.writeToFile("error", e.getMessage());
        }

        finally {
            try {
                connectClient.close();
                System.out.println(deviceId + ": Соединение закрыто. connectClient.close()");
                Logging.writeToFile(deviceId, "access", "Соединение закрыто");
            } catch (IOException e) {
                e.printStackTrace();
                Logging.writeToFile("error", e.getMessage());
            }
        }

    }

}