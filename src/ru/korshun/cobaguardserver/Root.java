package ru.korshun.cobaguardserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Root {

    private int PORT;
    private int PORT_FILES;
    private int ACCEPT_TIMEOUT;
    public static String COBA_PATH_NAME;

    ExecutorService executorService;

    protected static final int MAX_ERROR_CONNECT =          3;

    private final String CONFIG_FILE =                      "coba.conf";

    protected static String SIGNALS_DIR =                   "signals";
    protected static String SIGNALS_FILE =                  "get.txt";

    protected static String GUARD_DIR =                     "guard";
    protected static String GUARD_FILE =                    "get.txt";

    private HashMap<String, String> params =                new HashMap<>();


    protected static final String[] IMEI_LIST =             new String[] {
                                                                "356446052938789", //Литвиненко
                                                                "865749023045665", //Зубов
                                                                "353737067316257", //Аткин
                                                                "353646069029281", //Манушкин
                                                                "866130022032393", //Зарубо
                                                                "866130022226318", //Зарубо
                                                                "865749023013713", //Смирнов
                                                                "866494010384525", //Лобанов
                                                                "866494015384553", //Лобанов
                                                                "356604057607078", //Хамин
                                                                "355101064680439", //Плюхин
                                                                "865154028766672", //Новиков (Виталя)
                                                                "865154028766680", //Новиков (Виталя)
                                                                "860332025891454", //Чурсинов
                                                                "865282023989153", //Чинцов
                                                                "865282023989161", //Чинцов
                                                                "860332025895307", //Колесников
                                                                "358453052133088", //Артюшенко
                                                                "358453052133096", //Артюшенко
                                                                "862546028367000" //Федоров
                                                            };





    /**
     *  Конструктор класса
     */
    private Root() {
        readFile();
        setParams();

        SystemTrayIcon.createIcon();

        executorService =                                   Executors.newCachedThreadPool();
    }






    /**
     *  Функция считывает файл конфига и создает коллекцию с параметрами
     */
    private void readFile(){
        String line;
        try(BufferedReader br =                             new BufferedReader(new FileReader(CONFIG_FILE))) {
            while((line = br.readLine()) != null){

                if(line.length() > 0 && !line.startsWith("#") && line.contains("=")) {
                    String parts[] =                        line.split("=");
                    params.put(parts[0], parts[1]);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
            Logging.writeToFile("error", "Ошибка доступа к файлу конфига");
        }
    }






    /**
     *  Функция присваивает переменным парамерты, считанные из файла конфига
     */
    private void setParams() {
        PORT =                                              Integer.parseInt(params.get("CONNECT_PORT"));
        PORT_FILES =                                        Integer.parseInt(params.get("DOWNLOAD_PORT"));
        ACCEPT_TIMEOUT =                                    Integer.parseInt(params.get("ACCEPT_TIMEOUT"));
        COBA_PATH_NAME =                                    params.get("IMG_PATH");
    }






    /**
     * Функция создает "слушателя" для обработки подключающихся клиентов
     */
    private void serverStart() {

        ServerSocket socketConnect = null, socketSendFile = null;

        try {

            socketConnect =                                 new ServerSocket(PORT);
            socketSendFile =                                new ServerSocket(PORT_FILES);
            socketSendFile.setSoTimeout(ACCEPT_TIMEOUT * 1000);

            System.out.println("Сервер запущен");

            while (true) {

                Socket socketClientConnect = socketConnect.accept();

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























/**
 *  Вложенный класс, занимающийся непосредственно работой с подключившимся клиентом
 */
class ClientConnect
    implements Runnable {

    private ServerSocket serverFile;
    private Socket connectClient;
    private String deviceId;
    private int filesCount;

    private final String OBJECT_PART_DIVIDER =              "-";

    private final ArrayList<String> IMEI_LIST =             new ArrayList<>(Arrays.asList(Root.IMEI_LIST));







    /**
     *  Конструктор класса
     * @param serverFile                    - Ссылка на сокет, который открывается для передачи файлов
     * @param ConnectClient                 - Ссылка на сокет, который открывается для обработки запросов от клиента
     */
    public ClientConnect(ServerSocket serverFile, Socket ConnectClient) {
        this.serverFile =                                   serverFile;
        this.connectClient =                                ConnectClient;

        try {
            this.connectClient.setSoTimeout(25000);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        System.out.println("Сессия для " + ConnectClient + " открыта");
    }












    /**
     *  Функция сравнивает имя файла с номером объекта
     * @param objectNumber                  - номер объекта
     * @param fileName                      - файл
     * @return                              - в случае, если файл на нужный объект, возвращается TRUE
     */
    private boolean objectEquals(String objectNumber, String fileName) {

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
     *  Функция завершает сеанс работы с клиентом, закрывает Writer и выходит из цикла
     * @param out                           - ссылка на PrintWriter
     */
    private void sendDissconnectMessage(PrintWriter out) {
        out.println("close");
        out.flush();

        System.out.println(deviceId + ": Обновление завершено, клиент отключился");

        Logging.writeToFile("access", "Обновление завершено, клиент " + this.connectClient +
                " отключился \r\n\r\n====================================================================================== \r\n");
        Logging.writeToFile(deviceId, "access", "Обновление завершено, " +
                "клиент отключился \r\n\r\n====================================================================================== \r\n");
    }











    /**
     *  Функция выбирает файлы, которые созданы после даты обновления, переданной с устройства
     * @param query                         - ссылка на строку запроса
     * @param out                           - ссылка на PrintWriter
     * @return                              - возвращается коллекция типа ArrayList
     */
    private ArrayList<String> getNewFiles(String query, PrintWriter out) {
        ArrayList<String> listNewFiles =                    new ArrayList<>();

        String lastUpdateDate[] =                           query.split(":");
        deviceId =                                          lastUpdateDate[2];
        System.out.println(deviceId + ": запрашивается количество новых файлов");

        Logging.writeToFile(deviceId, "access", "Сессия открыта");
        Logging.writeToFile(deviceId, "access", "Запрашивается количество новых файлов");

        File cobaPath =                                     new File(Root.COBA_PATH_NAME);
        File[] countFiles =                                 cobaPath.listFiles();

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

        return listNewFiles;
    }










    /**
     *  Функция выбирает файлы на один конкретнный объект, переданный в строке запроса
     * @param query                             - ссылка на строку запроса
     * @param out                               - ссылка на PrintWriter
     * @return                                  - возвращается коллекция типа ArrayList
     */
    private ArrayList<String> getFile(String query, PrintWriter out) {
        ArrayList<String> listNewFiles =                    new ArrayList<>();

        String objectNumberStr[] =                          query.split(":");
        String objectNumber =                               objectNumberStr[1];
        deviceId =                                          objectNumberStr[2];

        System.out.println(deviceId + ": запрашивается объект " + objectNumber);

        Logging.writeToFile(deviceId, "access", "Сессия открыта");
        Logging.writeToFile(deviceId, "access", "Запрашивается объект " + objectNumber);

        File cobaPath =                                     new File(Root.COBA_PATH_NAME);
        File[] countFiles =                                 cobaPath.listFiles();

        if (cobaPath.isDirectory() && countFiles != null) {

            for (File file : countFiles) {

                if (file.isFile() && file.getName().contains(OBJECT_PART_DIVIDER)) {

                    if (objectEquals(objectNumber, file.getName())) {

                        listNewFiles.add(file.getName());

                    }

                }

            }

        }

        System.out.println(deviceId + ": Файлов " + listNewFiles.size());
        Logging.writeToFile(deviceId, "access", "Файлов " + listNewFiles.size());

        out.println(listNewFiles.size());
        out.flush();

        return listNewFiles;
    }










    /**
     *  Функция обрабатывает запрос на файл с сигналами
     * @param query                             - ссылка на строку запроса
     * @param out                               - ссылка на PrintWriter
     */
    private void executeSignalQuery(String query, PrintWriter out) {
        String objectNumberStr[] =                          query.split(":");
        String objectNumber =                               objectNumberStr[1];
        deviceId =                                          objectNumberStr[2];

        System.out.println(deviceId + ": запрос " + objectNumber + " от " + deviceId);

        // проверяем есть ли IMEI в списке
        if(IMEI_LIST.contains(deviceId)) {

            System.out.println(deviceId + ": IMEI " + deviceId + " найден");

            // проверяем наличие папки для сигналов для конкретного IMEI

            if(new File(Root.SIGNALS_DIR).isDirectory() | new File(Root.SIGNALS_DIR + File.separator + deviceId).mkdirs()) {

                File queryFile =                                    new File(Root.SIGNALS_DIR + File.separator + deviceId + File.separator + Root.SIGNALS_FILE);
                FileWriter fileWriter =                             null;

                try {

                    // если файла txt в вышесозданной папке нет - создаем
                    if(!queryFile.exists()) {
                        queryFile.createNewFile();
                    }

                    File xlsFile =                                  new File(Root.SIGNALS_DIR + File.separator + deviceId + File.separator + objectNumber + ".xls");
                    fileWriter =                                    new FileWriter(queryFile);


                    // если файл Excel c сигналами по запрошенному обхекту есть в папке - отсылаем его размер,
                    // имя и ждем подключения для скачивания
                    if(xlsFile.exists() && xlsFile.length() > 0) {

                        System.out.println(deviceId + ": файл xls найден ");

                        // отправляем размер файла
                        out.println(xlsFile.length());
                        out.flush();


                        // отправляем имя файла
                        out.println(xlsFile.getName());
                        out.flush();


                        Socket sendFileClient =                         null;

                        try {
                            sendFileClient =                            serverFile.accept();
                        } catch (IOException e) {
                            System.out.println(deviceId + ": ОШИБКА ПОДКЛЮЧЕНИЯ 6667");
                        }

                        if(sendFileClient != null) {

//                            countErrorConnection =                      0;

                            try(FileInputStream fis =                   new FileInputStream(xlsFile);
                                BufferedInputStream bis =               new BufferedInputStream(fis);
                                DataOutputStream dos =                  new DataOutputStream(sendFileClient.getOutputStream())) {

                                System.out.println(deviceId + ": Отправляем файл с сигналами: " + xlsFile.getName());
                                Logging.writeToFile(deviceId, "access", "Отправляем файл с сигналами: " + xlsFile.getName());

                                byte[] buffer =                         new byte[32 * 1024];
                                int count;

                                while ((count = bis.read(buffer, 0, buffer.length)) != -1) {
                                    dos.write(buffer, 0, count);
                                    dos.flush();
                                }

                                System.out.println(deviceId + ": Файл сигналов " + xlsFile.getName() + " передан");
                                Logging.writeToFile(deviceId, "access", "Файл сигналов " + xlsFile.getName() + " передан");

                            } catch (IOException e) {
                                System.out.println(deviceId + ": ОШИБКА ПЕРЕДАЧИ ФАЙЛА СИГНАЛОВ");
                            } finally {
                                try {
                                    sendFileClient.close();
                                } catch (IOException e) {
                                    System.out.println(deviceId + ": ОШИБКА ЗАКРЫТИЯ FILE-СОКЕТА");
                                }

                                if(!xlsFile.delete()) {
                                    System.out.println(deviceId + ": ОШИБКА УДАЛЕНИЯ ФАЙЛА СИГНАЛОВ");
                                }

                                System.out.println(deviceId + ": Клиент для скачивания файла сигналов отключился");
                                Logging.writeToFile(deviceId, "access", "Клиент для скачивания файла сигналов отключился");
                            }

                        }

                        fileWriter.write("0");

                    }


                    // если файл Excel c сигналами по запрошенному обхекту отсутствует - отсылаем 0
                    else {
                        fileWriter.write(objectNumber);
                        System.out.println(deviceId + ": файл xls отсутствует ");

                        out.println(0);
                        out.flush();
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
            System.out.println(deviceId + ": ОШИБКА АВТОРИЗАЦИИ IMEI");
            Logging.writeToFile(deviceId, "access", "ОШИБКА АВТОРИЗАЦИИ IMEI");

            out.println(-4);
            out.flush();
        }

    }








    /**
     *  Функция обрабатывает запрос на снятие или постановку объекта на охрану
     * @param query                             - ссылка на строку запроса
     * @param out                               - ссылка на PrintWriter
     */
    private void executeGuardQuery(String query, PrintWriter out) {
        String objectNumberStr[] =                              query.split(":");
        String objectNumber =                                   objectNumberStr[1];
        String objectStatus =                                   objectNumberStr[2];
        deviceId =                                              objectNumberStr[3];

        String objectStatusStr =                                (objectStatus.equals("#")) ?
                                                                    "на снятие с охраны " :
                                                                    "на постановку под охрану ";

        System.out.println(deviceId + ": запрос " + objectStatusStr + objectNumber + " от " + deviceId);

        // проверяем есть ли IMEI в списке
        if(IMEI_LIST.contains(deviceId)) {

            System.out.println(deviceId + ": IMEI " + deviceId + " найден");

            // проверяем наличие папки для сигналов для конкретного IMEI
            if(new File(Root.GUARD_DIR).isDirectory() | new File(Root.GUARD_DIR + File.separator + deviceId).mkdirs()) {

                File queryFile =                                new File(Root.GUARD_DIR + File.separator + deviceId + File.separator + Root.GUARD_FILE);



                // если файла txt в вышесозданной папке нет - создаем
                if(!queryFile.exists()) {
                    try {
                        queryFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // записываем в файл ноль
                    try (FileWriter fileWriter =        new FileWriter(queryFile)) {
                        fileWriter.write("0");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }




                String queryFileStr =                           "";

                try(BufferedReader fileReader =                 new BufferedReader(new FileReader(queryFile))) {
                    // достаем из файла первую строку
                    queryFileStr =                              fileReader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }







                // если строка из файла прочиталась и она не равна нулю - приступаем к ее анализу
                if(!queryFileStr.equals("") && !queryFileStr.equals("0")) {

                    String queryFileStrSplit[] =                queryFileStr.split(":");

//                    System.out.println("queryFileStrSplit[2] - " + queryFileStrSplit[2]);

                    // на всякий случай проверяем, совпадает ли присланный объект и его статус с теми,
                    // которые записаны в файле
                    if (queryFileStrSplit[0].equals(objectNumber) && queryFileStrSplit[1].equals(objectStatus)) {


                        // если 3й параметр в строке отсутствует - значит снятие\постановка еще не прошла, ждем
                        if (queryFileStrSplit.length == 2) {

                            // отправляем ноль - это сигнал клиенту о том, что запрос в процессе выполнения
                            out.println(0);
                            out.flush();

                            System.out.println(deviceId + ": объект " + queryFileStrSplit[0] + " в процессе обработки ...");

                        }


                        // если присутсвует 3й параметр в строке и он равен % - значит запрос успешно отработан
                        if (queryFileStrSplit.length > 2 && queryFileStrSplit[2].equals("%")) {

                            String readState =                  (queryFileStrSplit[1].equals("#")) ?
                                                                    " успешно снят с охраны" :
                                                                    " успешно поставлен на охрану";

                            int answer =                        (queryFileStrSplit[1].equals("#")) ?
                                                                    1 :
                                                                    2;

                            // отправляем единицу или двойку в зависимости от того, постановка это была или снятие
                            out.println(answer);
                            out.flush();


                            // записываем в файл ноль
                            try (FileWriter fileWriter =        new FileWriter(queryFile)) {
                                fileWriter.write("0");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            System.out.println(deviceId + ": объект " + queryFileStrSplit[0] + readState);

                        }

                    }

                    // если присланный объект и его статус не совпадает с теми, которые записаны в файле
                    // отправляем сообщение об ошибке
                    else {

                        // отправляем -1 - это сигнал клиенту о том, что произошла ошибка
                        out.println(-1);
                        out.flush();

                        System.out.println(deviceId + ": присланный объект не совпадает с записанным!");
                    }


                }



                // если строка равна нулю - записываем в файл запрос
                else if(queryFileStr.equals("0")) {

                    // записываем в файл запрос
                    try (FileWriter fileWriter =        new FileWriter(queryFile)) {
                        fileWriter.write(objectNumber + ":" + objectStatus);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // отправляем ноль - это сигнал клиенту о том, что запрос в процессе выполнения
                    out.println(0);
                    out.flush();

                    System.out.println(deviceId + ": запрос отправлен на обработку");
                }



                // если первая строка пустая - записываем ноль
                else if(queryFileStr.equals("")) {

                    // записываем в файл ноль
                    try(FileWriter fileWriter =                 new FileWriter(queryFile))  {
                        fileWriter.write("0");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }

        }



        // если IMEI не найден в списке, шлем обратно код ошибки
        else {
            System.out.println(deviceId + ": ОШИБКА АВТОРИЗАЦИИ IMEI");
            Logging.writeToFile(deviceId, "access", "ОШИБКА АВТОРИЗАЦИИ IMEI");

            out.println(-4);
            out.flush();
        }

    }







    /**
     *  Функция перебирает файлы из входной коллекции и отправляет их получателю
     * @param listNewFiles                      - ссылка на коллекцию со списком файлов для отправки
     * @param out                               - ссылка на PrintWriter
     * @return                                  - если все прошло без ошибок, возвращается TRUE
     */
    private boolean downloadFiles(ArrayList<String> listNewFiles, PrintWriter out) {
        System.out.println(deviceId + ": Получен запрос на скачивание");
        Logging.writeToFile(deviceId, "access", "Получен запрос на скачивание");

        int countErrorConnection =                          0;
        boolean downloadComplite =                          true;

        for (String newFile : listNewFiles) {

            String tmpPath =                                Root.COBA_PATH_NAME + File.separator + deviceId;
            File fileName =                                 new File(tmpPath + File.separator + newFile);

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

            Socket sendFileClient =                         null;

            try {
                sendFileClient =                            serverFile.accept();
            } catch (IOException e) {
                System.out.println(deviceId + ": ОШИБКА ПОДКЛЮЧЕНИЯ 6667");

                if(++countErrorConnection >= Root.MAX_ERROR_CONNECT) {
                    System.out.println(deviceId + ": ИСЧЕРАПАН ЛИМИТ ПОДКЛЮЧЕНИЙ, ОТКЛЮЧАЕМСЯ");
                    downloadComplite =                      false;
                    break;
                }

            }

            if(sendFileClient != null) {

                countErrorConnection =                      0;

                try(FileInputStream fis =                   new FileInputStream(fileName);
                    BufferedInputStream bis =               new BufferedInputStream(fis);
                    DataOutputStream dos =                  new DataOutputStream(sendFileClient.getOutputStream())) {

                    System.out.println(deviceId + ": Клиент для скачивания подключился, отправляем: " + newFile);
                    Logging.writeToFile(deviceId, "access", "Клиент для скачивания подключился, отправляем: " + newFile);

                    byte[] buffer =                         new byte[32 * 1024];
                    int count;

                    while ((count = bis.read(buffer, 0, buffer.length)) != -1) {
                        dos.write(buffer, 0, count);
                        dos.flush();
                    }

                    System.out.println(deviceId + ": Файл " + newFile + " передан");
                    Logging.writeToFile(deviceId, "access", "Файл " + newFile + " передан");

                } catch (IOException e) {
                    System.out.println(deviceId + ": ОШИБКА ПЕРЕДАЧИ ФАЙЛА");
                } finally {
                    try {
                        sendFileClient.close();
                    } catch (IOException e) {
                        System.out.println(deviceId + ": ОШИБКА ЗАКРЫТИЯ FILE-СОКЕТА");
                    }

                    if(!fileName.delete()) {
                        System.out.println(deviceId + ": ОШИБКА УДАЛЕНИЯ ВРЕМЕННОГО ФАЙЛА");
                    }

                    System.out.println(deviceId + ": Клиент для скачивания отключился");
                    Logging.writeToFile(deviceId, "access", "Клиент для скачивания отключился");
                }

            }

        }

        return downloadComplite;

    }










    @Override
    public void run() {

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(connectClient.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(connectClient.getOutputStream())), true)
        ) {

            ArrayList<String> listNewFiles =                new ArrayList<>();

            String query;






            while (true) {



                try {
                    query =                                 in.readLine();
                } catch (SocketTimeoutException e) {
                    System.out.println(deviceId + ": Соединение закрыто по таймауту");
                    break;
                }







                if(query == null) {
                    System.out.println("query == null   ------   DISCONNECT");
                    break;
                }










                //Клиент отсоединился
                if (query.equals("disconnect")) {
                    sendDissconnectMessage(out);
                    break;
                }










                //Клиент сделал запрос на кол-во новых файлов
                if (query.startsWith("getFilesNew")) {
                    listNewFiles =                          getNewFiles(query, out);

                    filesCount =                            listNewFiles.size();

                    continue;
                }










                //Клиент сделал запрос на скачивание конкретного файла
                if (query.startsWith("getFile")) {
                    listNewFiles =                          getFile(query, out);

                    filesCount =                            listNewFiles.size();

                    continue;
                }








                //Клиент сделал запрос по сигналам
                if (query.startsWith("getSignalFile")) {

                    executeSignalQuery(query, out);

                    continue;
                }




                //Клиент сделал запрос на постановку или снятие объекта
                if (query.startsWith("setObjectStatus")) {

                    executeGuardQuery(query, out);

                    continue;
                }





                //Клиент сделал запрос на скачивание
                if (query.equals("download") && filesCount > 0) {
                    if(!downloadFiles(listNewFiles, out)) {
                        break;
                    }
                }








            }

        } catch (IOException e) {
            e.printStackTrace();
            Logging.writeToFile("error", e.getMessage());
        } finally {

            try {
                connectClient.close();
                System.out.println(deviceId + ": Соединение закрыто. connectClient.close()");
                System.out.println("/ ========================================================================== /");
                Logging.writeToFile(deviceId, "access", "Соединение закрыто");
            } catch (IOException e) {
                e.printStackTrace();
                Logging.writeToFile("error", e.getMessage());
            }
        }




    }


}