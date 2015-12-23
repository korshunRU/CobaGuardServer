package ru.korshun.cobaguardserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;



public class ClientConnectThread
    implements Runnable {


    private Socket                          connectSocket;
    private ServerSocket                    serverFileSocket;
    private int                             filesToDownload =           0;
    private String                          deviceId =                  "000000000000000";
    private final String                    OBJECT_PART_DIVIDER =       "-";
    private final ArrayList<String>         IMEI_LIST =                 new ArrayList<>(Arrays.asList(Settings.getInstance().getIMEI_LIST()));
    private String                          timeStamp =                 new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());





    public ClientConnectThread(Socket connectSocket, ServerSocket serverFileSocket) {
        this.connectSocket =                                            connectSocket;
        this.serverFileSocket =                                         serverFileSocket;

        try {
            this.connectSocket.setSoTimeout(Settings.getInstance().getAcceptTimeOut());
        } catch (SocketException e) {
            e.printStackTrace();
        }

        System.out.println(timeStamp + ": Клиент подключился: " + this.connectSocket);
    }







    @Override
    public void run() {


        try(BufferedReader in = new BufferedReader(new InputStreamReader(connectSocket.getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(connectSocket.getOutputStream())), true)) {

            String query;
            ArrayList<String> listNewFiles =                            null;

            while (!(query = in.readLine()).equals("disconnect") || (query = in.readLine()) != null) {




                // Запрашиваются все файлы для обновления
                if(query.startsWith("getNewFiles:")) {
                    listNewFiles =                                      getFiles(query, out, false);
                    filesToDownload =                                   listNewFiles.size();
                }




                // Запрашиваются файлы на один объект
                else if(query.startsWith("getObjectFiles:")) {
                    listNewFiles =                                      getFiles(query, out, true);
                    filesToDownload =                                   listNewFiles.size();
                }




                // Запрос загрузки файлов
                else if(query.startsWith("download") & listNewFiles != null & filesToDownload > 0) {
                    if(!sendFiles(listNewFiles, out)) {
                        break;
                    }
                }




                // Запрашиваются сигналы
                else if(query.startsWith("getSignalFile:")) {
                    executeSignalQuery(query, out);
                }




                // Снятие\постановка объекта
                else if(query.startsWith("setObjectStatus:")) {
                    executeGuardQuery(query, out);
                }




                // Если прилетела какая-нибудь хуйня - выходим из цикла
                else {
                    System.out.println(timeStamp + ": " + deviceId + ": / " + query + ": Некорректный запрос /");
                    break;
                }

            }

        } catch (IOException e) {
            System.out.println(timeStamp + ": " + deviceId + ": / Истекло время ожидания, отключаемся /");
        } finally {
            try {
                this.connectSocket.close();
                System.out.println(timeStamp + ": " + deviceId + ": / Соединение закрыто /");
                System.out.println(timeStamp + ": " + deviceId + ": / ========================================================= /");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }








    /**
     *  Функция сравнивает имя файла с номером объекта
     * @param objectNumber                  - номер объекта
     * @param fileName                      - файл
     * @return                              - в случае, если файл на нужный объект, возвращается TRUE
     */
    private boolean isObjectNumberEqualsWithFileName(String objectNumber, String fileName) {

        String fileNameSplit[] = fileName.substring(0, fileName.lastIndexOf(OBJECT_PART_DIVIDER)).split(",");

        if (fileNameSplit.length > 1) {

            for (String fn : fileNameSplit) {

                if (isInteger(fn) && fn.equals(objectNumber)) {

                    return true;

                }

            }

        }

        else {

            if (isInteger(fileNameSplit[0]) && fileNameSplit[0].equals(objectNumber)) {
                return true;
            }

        }

        return false;
    }







    /**
     *  Функция проверяет, является ли строка целым числом
     * @param str                           - строка для проверки
     * @return                              - в случае, если строка является числом, возвращается TRUE
     */
    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
        } catch(NumberFormatException e) {
            return false;
        }
        return true;
    }









    /**
     *  Функция выбирает файлы, которые созданы после даты обновления, переданной с устройства
     * @param query                         - ссылка на строку запроса
     * @param out                           - ссылка на PrintWriter
     * @return                              - возвращается коллекция типа ArrayList
     */
    private ArrayList<String> getFiles(String query, PrintWriter out, boolean isObjectFiles) {
        ArrayList<String> listNewFiles =                                new ArrayList<>();

        String lastUpdateDate[] =                                       query.split(":");
        deviceId =                                                      lastUpdateDate[2];
        String version = lastUpdateDate.length >= 4 ?
                                                                        lastUpdateDate[3] :
                                                                        "OLD";

        String str =                                                    (isObjectFiles) ?
                                                                            "запрашивается объект " + lastUpdateDate[1] :
                                                                            "запрашивается количество новых файлов";

        System.out.println(timeStamp + ": " + deviceId + ": " + version + ": " + str);

        File cobaPath =                                                 new File(Settings.getInstance().getFilesPath());
        File[] countFiles =                                             cobaPath.listFiles();

        if (cobaPath.isDirectory() && countFiles != null) {

            for (File file : countFiles) {

                if(isObjectFiles) {

                    if (file.isFile() && file.getName().contains(OBJECT_PART_DIVIDER) && isObjectNumberEqualsWithFileName(lastUpdateDate[1], file.getName())) {

                        listNewFiles.add(file.getName());

                    }

                }

                else {


                    if (file.isFile() && (Long.parseLong(lastUpdateDate[1]) - file.lastModified()) < 0) {

                        listNewFiles.add(file.getName());

                    }
                }

            }

        }

        System.out.println(timeStamp + ": " + deviceId + ": " + version  + ": Новых файлов " + listNewFiles.size());

        out.println(listNewFiles.size());
        out.println(Settings.getInstance().getBufferSize());

        return listNewFiles;
    }







    /**
     *  Функция перебирает файлы из входной коллекции и отправляет их получателю
     * @param listNewFiles                      - ссылка на коллекцию со списком файлов для отправки
     * @param out                               - ссылка на PrintWriter
     * @return                                  - если все прошло без ошибок, возвращается TRUE
     */
    private boolean sendFiles(ArrayList<String> listNewFiles, PrintWriter out) {

        System.out.println(timeStamp + ": " + deviceId + ": Получен запрос на скачивание");

        Socket fileSocket;

        synchronized (serverFileSocket) {
            try {
                fileSocket =                                            serverFileSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(timeStamp + ": " + deviceId + ": ОШИБКА ПОДКЛЮЧЕНИЯ 6667");
                return false;
            }
        }

        try {
            fileSocket.setSoTimeout(Settings.getInstance().getAcceptTimeOut());
        } catch (SocketException e) {
            e.printStackTrace();
        }



        for (String newFile : listNewFiles) {

            String tmpPath =                                            Settings.getInstance().getFilesPath() + File.separator + deviceId;
            File fileName =                                             new File(tmpPath + File.separator + newFile);

            ImgEncode
                    .getInstance(newFile, Settings.getInstance().getFilesPath(), tmpPath)
                    .encodeImg();

            out.println(newFile);
            out.println(fileName.length());

            FileInputStream fis;

            try {
                fis =                                       new FileInputStream(fileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            BufferedInputStream bis =                       new BufferedInputStream(fis);
            DataOutputStream dos;
            DataInputStream disTestConnect;

            try {
                dos =                                       new DataOutputStream(fileSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            try {
                disTestConnect =                            new DataInputStream(fileSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            byte[] buffer =                                 new byte[Settings.getInstance().getBufferSize() * 1024];
            int count;


            try {

                while ((count = bis.read(buffer, 0, buffer.length)) != -1) {

                    dos.write(buffer, 0, count);
                    dos.flush();

                }

                int result =                                disTestConnect.read();

                if(result == 0) {
                    System.out.println(timeStamp + ": " + deviceId + ": " + newFile + " ОШИБКА ПЕРЕДАЧИ ФАЙЛА. result == 0");
                    try {
                        bis.close();
                        dos.close();
                        disTestConnect.close();
                        fileSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    return false;
                }

                System.out.println(timeStamp + ": " + deviceId + ": Файл " + newFile + " передан");

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(timeStamp + ": " + deviceId + ": " + newFile + " ОШИБКА ПЕРЕДАЧИ ФАЙЛА");

                try {
                    bis.close();
                    dos.close();
                    disTestConnect.close();
                    fileSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                return false;
            } finally {

                try {
                    fis.close();
                    if (!fileName.delete()) {
                        System.out.println(timeStamp + ": " + deviceId + ": ОШИБКА УДАЛЕНИЯ ВРЕМЕННОГО ФАЙЛА");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }

        try {
            fileSocket.close();
        } catch (IOException e) {
            System.out.println(timeStamp + ": " + deviceId + ": ОШИБКА ЗАКРЫТИЯ FILE-СОКЕТА");
        }

        return true;

    }








    /**
     *  Функция обрабатывает запрос на файл с сигналами
     * @param query                             - ссылка на строку запроса
     * @param out                               - ссылка на PrintWriter
     */
    private void executeSignalQuery(String query, PrintWriter out) {
        String objectNumberStr[] =                                      query.split(":");
        String objectNumber =                                           objectNumberStr[1];
        deviceId =                                                      objectNumberStr[2];
        String version = objectNumberStr.length >= 4 ?
                                                                        objectNumberStr[3] :
                                                                        "OLD";

        System.out.println(timeStamp + ": " + deviceId + ": " + version  + ": запрос " + objectNumber + " от " + deviceId);

        // проверяем есть ли IMEI в списке
        if(IMEI_LIST.contains(deviceId)) {

            System.out.println(timeStamp + ": " + deviceId + ": " + version  + ": IMEI " + deviceId + " найден");

            // проверяем наличие папки для сигналов для конкретного IMEI

            if(new File(Settings.getInstance().getSIGNALS_DIR()).isDirectory() |
                    new File(Settings.getInstance().getSIGNALS_DIR() + File.separator + deviceId).mkdirs()) {

                File queryFile =                                        new File(Settings.getInstance().getSIGNALS_DIR() +
                                                                            File.separator + deviceId + File.separator +
                                                                            Settings.getInstance().getSIGNALS_FILE());
                FileWriter fileWriter =                                 null;

                try {

                    // если файла txt в вышесозданной папке нет - создаем
                    if(!queryFile.exists()) {
                        queryFile.createNewFile();
                    }

                    File xlsFile =                                      new File(Settings.getInstance().getSIGNALS_DIR() +
                                                                            File.separator + deviceId + File.separator +
                                                                            objectNumber + ".xls");
                    fileWriter =                                        new FileWriter(queryFile);


                    // если файл Excel c сигналами по запрошенному обхекту есть в папке - отсылаем его размер,
                    // имя и ждем подключения для скачивания
                    if(xlsFile.exists() && xlsFile.length() > 0) {

                        System.out.println(timeStamp + ": " + deviceId + ": " + version  + ": файл xls найден ");

                        // отправляем размер файла
                        out.println(xlsFile.length());


                        // отправляем имя файла
                        out.println(xlsFile.getName());


                        // отправляем размер буфера
                        out.println(Settings.getInstance().getBufferSize());


                        Socket sendFileClient =                         null;

                        synchronized (serverFileSocket) {
                            try {
                                sendFileClient =                        serverFileSocket.accept();
                            } catch (IOException e) {
                                System.out.println(timeStamp + ": " + deviceId + ": " + version + ": ОШИБКА ПОДКЛЮЧЕНИЯ 6667");
                            }
                        }

                        if(sendFileClient != null) {

                            try {
                                sendFileClient.setSoTimeout(Settings.getInstance().getAcceptTimeOut());
                            } catch (SocketException e) {
                                e.printStackTrace();
                            }

                            try(FileInputStream fis =                   new FileInputStream(xlsFile);
                                BufferedInputStream bis =               new BufferedInputStream(fis);
                                DataOutputStream dos =                  new DataOutputStream(sendFileClient.getOutputStream());
                                DataInputStream disTestConnect =        new DataInputStream(sendFileClient.getInputStream())) {

                                System.out.println(timeStamp + ": " + deviceId + ": " + version + ": Отправляем файл с сигналами: " + xlsFile.getName());

                                byte[] buffer =                         new byte[Settings.getInstance().getBufferSize() * 1024];
                                int count;

                                while ((count = bis.read(buffer, 0, buffer.length)) != -1) {
                                    dos.write(buffer, 0, count);
                                    dos.flush();
                                }

                                int result =                            disTestConnect.read();

                                if(result == 0) {
                                    System.out.println(timeStamp + ": " + deviceId + ": " + version + ": ОШИБКА ПЕРЕДАЧИ ФАЙЛА СИГНАЛОВ");
                                }

                                else if(result == 1){
                                    System.out.println(timeStamp + ": " + deviceId + ": " + version + ": Файл сигналов " + xlsFile.getName() + " передан");
                                }

                            } catch (IOException e) {
                                System.out.println(timeStamp + ": " + deviceId + ": " + version + ": ОШИБКА ПЕРЕДАЧИ ФАЙЛА СИГНАЛОВ");
                            } finally {
                                try {
                                    sendFileClient.close();
                                } catch (IOException e) {
                                    System.out.println(timeStamp + ": " + deviceId + ": " + version + ": ОШИБКА ЗАКРЫТИЯ FILE-СОКЕТА");
                                }

                                if(!xlsFile.delete()) {
                                    System.out.println(timeStamp + ": " + deviceId + ": " + version + ": ОШИБКА УДАЛЕНИЯ ФАЙЛА СИГНАЛОВ");
                                }
                            }

                        }

                        fileWriter.write("0");

                    }


                    // если файл Excel c сигналами по запрошенному обхекту отсутствует - отсылаем 0
                    else {
                        fileWriter.write(objectNumber);
                        System.out.println(timeStamp + ": " + deviceId + ": " + version + ": файл xls отсутствует ");

                        out.println(0);
                    }



                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(fileWriter != null) {
                        try {
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }

        }


        // если IMEI не найден в списке, шлем обратно код ошибки
        else {
            System.out.println(timeStamp + ": " + deviceId + ": " + version + ": ОШИБКА АВТОРИЗАЦИИ IMEI");

            out.println(-4);
        }

    }









    /**
     *  Функция обрабатывает запрос на снятие или постановку объекта на охрану
     * @param query                             - ссылка на строку запроса
     * @param out                               - ссылка на PrintWriter
     */
    private void executeGuardQuery(String query, PrintWriter out) {
        String objectNumberStr[] =                                      query.split(":");
        String objectNumber =                                           objectNumberStr[1];
        String objectStatus =                                           objectNumberStr[2];
        deviceId =                                                      objectNumberStr[3];
        String version = objectNumberStr.length >= 5 ?
                                                                        objectNumberStr[4] :
                                                                        "OLD";

        String objectStatusStr =                                        "";

        switch (objectStatus) {

            case "0":
                objectStatusStr =                                       "на очистку файла запросов";
                break;

            case "#":
                objectStatusStr =                                       "на снятие с охраны " + objectNumber;
                break;

            case "*":
                objectStatusStr =                                       "на постановку под охрану " + objectNumber;
                break;

        }

        System.out.println(timeStamp + ": " + deviceId + ": " + version + ": запрос " + objectStatusStr  + " от " + deviceId);

        // проверяем есть ли IMEI в списке
        if(IMEI_LIST.contains(deviceId)) {

            System.out.println(timeStamp + ": " + deviceId + ": " + version + ": IMEI " + deviceId + " найден");

            // проверяем наличие папки для сигналов для конкретного IMEI
            if(new File(Settings.getInstance().getGUARD_DIR()).isDirectory() |
                    new File(Settings.getInstance().getGUARD_DIR() + File.separator + deviceId).mkdirs()) {

                File queryFile =                                        new File(Settings.getInstance().getGUARD_DIR() +
                                                                            File.separator + deviceId + File.separator +
                                                                            Settings.getInstance().getGUARD_FILE());



                // если файла txt в вышесозданной папке нет - создаем
                if(!queryFile.exists()) {
                    try {
                        queryFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // записываем в файл ноль
                    try (FileWriter fileWriter =                        new FileWriter(queryFile)) {
                        fileWriter.write("0");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }



                // если статус объекта равен нулю - это запрос на очистку файла запроса
                if(objectStatus.equals("0")) {

                    // записываем в файл ноль
                    try (FileWriter fileWriter =                        new FileWriter(queryFile)) {
                        fileWriter.write("0");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // отправляем -5 - это сигнал клиенту о том, что файл обнулен
                    out.println(-5);

                    System.out.println(timeStamp + ": " + deviceId + ": " + version + ": запрос на очистку файла запросов выполнен ");

                    // выходим
                    return;
                }



                String queryFileStr;

                if(queryFile.length() > 0) {
                    queryFileStr =                                      "";

                    try (BufferedReader fileReader =                    new BufferedReader(new FileReader(queryFile))) {

                        // достаем из файла первую строку
                        queryFileStr =                                  fileReader.readLine().trim();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                else {

                    // записываем в файл ноль
                    try (FileWriter fileWriter =                        new FileWriter(queryFile)) {
                        fileWriter.write("0");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    queryFileStr =                                      "0";
                }






                // если строка из файла прочиталась и она не равна нулю - приступаем к ее анализу
                if(!queryFileStr.equals("") && !queryFileStr.equals("0")) {

                    String queryFileStrSplit[] =                        queryFileStr.split(":");

                    // на всякий случай проверяем, совпадает ли присланный объект и его статус с теми,
                    // которые записаны в файле
                    if (queryFileStrSplit[0].equals(objectNumber) && queryFileStrSplit[1].equals(objectStatus)) {


                        // если 3й параметр в строке отсутствует - значит снятие\постановка еще не прошла, ждем
                        if (queryFileStrSplit.length == 2) {

                            // отправляем ноль - это сигнал клиенту о том, что запрос в процессе выполнения
                            out.println(0);

                            System.out.println(timeStamp + ": " + deviceId + ": " + version + ": объект " + queryFileStrSplit[0] + " в процессе обработки ...");

                        }


                        // если присутсвует 3й параметр в строке и он равен % - значит запрос успешно отработан
                        if (queryFileStrSplit.length > 2 && queryFileStrSplit[2].equals("%")) {

                            String readState =                          (queryFileStrSplit[1].equals("#")) ?
                                                                            " успешно снят с охраны" :
                                                                            " успешно поставлен на охрану";

                            int answer =                                (queryFileStrSplit[1].equals("#")) ?
                                                                            1 :
                                                                            2;

                            // отправляем единицу или двойку в зависимости от того, постановка это была или снятие
                            out.println(answer);


                            // записываем в файл ноль
                            try (FileWriter fileWriter =                new FileWriter(queryFile)) {
                                fileWriter.write("0");
                            } catch (IOException e) {
//                                e.printStackTrace();
                                System.out.println(timeStamp + ": " + deviceId + ": " + version + ": error acces to file! Wait 2 seconds ... ");
                                try {
                                    TimeUnit.SECONDS.sleep(2);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                                try (FileWriter fileWriter =            new FileWriter(queryFile)) {
                                    fileWriter.write("0");
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }

                            System.out.println(timeStamp + ": " + deviceId + ": " + version + ": объект " + queryFileStrSplit[0] + readState);

                        }

                    }

                    // если присланный объект и его статус не совпадает с теми, которые записаны в файле
                    // отправляем сообщение об ошибке
                    else {

                        // отправляем -6 - это сигнал клиенту о том, что файл запроса занят другим объектом
                        out.println(-6);

                        System.out.println(timeStamp + ": " + deviceId + ": " + version + ": присланный объект не совпадает с записанным!");
                    }


                }



                // если строка равна нулю - записываем в файл запрос
                else if(queryFileStr.equals("0")) {

                    // записываем в файл запрос
                    try (FileWriter fileWriter =                        new FileWriter(queryFile)) {
                        fileWriter.write(objectNumber + ":" + objectStatus);
                    } catch (IOException e) {
//                        e.printStackTrace();
                        System.out.println(timeStamp + ": " + deviceId + ": " + version + ": error acces to file! Wait 2 seconds ... ");
                        try {
                            TimeUnit.SECONDS.sleep(2);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        try (FileWriter fileWriter =                    new FileWriter(queryFile)) {
                            fileWriter.write(objectNumber + ":" + objectStatus);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }

                    // отправляем ноль - это сигнал клиенту о том, что запрос в процессе выполнения
                    out.println(0);

                    System.out.println(timeStamp + ": " + deviceId + ": " + version + ": запрос отправлен на обработку");
                }



                // если первая строка пустая - записываем ноль
                else if(queryFileStr.equals("") || queryFileStr.length() == 0) {

                    // записываем в файл ноль
                    try(FileWriter fileWriter =                         new FileWriter(queryFile))  {
                        fileWriter.write("0");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }

        }



        // если IMEI не найден в списке, шлем обратно код ошибки
        else {
            System.out.println(timeStamp + ": " + deviceId + ": " + version + ": ОШИБКА АВТОРИЗАЦИИ IMEI");

            out.println(-4);
        }

    }





}
