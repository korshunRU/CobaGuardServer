package ru.korshun.cobaguardserver;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


@SuppressWarnings("FieldCanBeLocal")
public class Settings {

    private final String                    CONFIG_FILE =               "coba.conf";

    private String                          SIGNALS_DIR =               "signals";
    private String                          SIGNALS_FILE =              "get.txt";

    private String                          GUARD_DIR =                 "guard";
    private String                          GUARD_FILE =                "get.txt";

    private final int                       BUFFER_SIZE_DEFAULT =       8;

    private static volatile Settings        instance;

    private int                             connectPort;
    private int                             connectPortFiles;
    private int                             acceptTimeOut;
    private String                          filesPath;
    private int                             bufferSize =                BUFFER_SIZE_DEFAULT;

    private final String[]                  IMEI_LIST =                 new String[] {
                                                                                "356446052938789", //Литвиненко
                                                                                "865749023045665", //Зубов
                                                                                "353737067316257", //Аткин
                                                                                "353646069029281", //Манушкин
                                                                                "866130022032393", //Зарубо
                                                                                "866130022226318", //Зарубо
                                                                                "865749023013713", //Смирнов
                                                                                "352879070024733", //Лобанов
                                                                                "352879070055737", //Лобанов
                                                                                "356604057607078", //Хамин
                                                                                "355101064680439", //Плюхин
                                                                                "865154028766672", //Новиков
                                                                                "865154028766680", //Новиков
                                                                                "860332025891454", //Чурсинов
                                                                                "865282023989153", //Чинцов
                                                                                "865282023989161", //Чинцов
                                                                                "860332025895307", //Колесников
                                                                                "358453052133088", //Артюшенко
                                                                                "358453052133096", //Артюшенко
                                                                                "862546028367000", //Федоров
                                                                                "860071022983563", //Журавлев
                                                                                "860071022983571", //Журавлев
                                                                                "860332025889490", //Иванов
                                                                                "862546028375433", //Бабушкин
                                                                                "862741017056736", //Артюшенко
                                                                                "862741017056744", //Артюшенко
                                                                                "860332025887171", //Дежурный инженер
                                                                                "353164055513482", //Бормонтов
                                                                                "357189054627179", //Савкин
                                                                                "352384070833241", //Косяковский
                                                                                "357033054327762", //Богданов
                                                                                "357473050360476", //Кузьмин
                                                                                "865973029675156", //Лапин
                                                                                "865973029675164", //Лапин
                                                                                "860486024636714", //Мишарин
                                                                                "864706020373582", //Федотовских
                                                                                "359652051686280", //Дементьев
                                                                                "861549003744533", //Конев
                                                                                "861549003744541" //Конев
                                                                        };

    private final String[]                  IMEI_LIST_VIDOK =           new String[] {
                                                                                "357967051804531", //Моисеев
                                                                                "355320061803105", //Фурик
                                                                                "355321061803103", //Фурик
                                                                                "352554060276029", //Фахрисламов
                                                                                "352555060276026", //Фахрисламов
                                                                                "356655061419785", //Каширский В.
                                                                                "356655061421492", //Каширский В.
                                                                                "354588070278391" //Каширский Н.
                                                                        };

    private final String[]                  IMEI_LIST_GBR =             new String[] {
                                                                                "356446052938789", //Литвиненко
                                                                                "866130020922066", //Центр
                                                                                "866130021091747", //Центр
                                                                                "865676028012814", //Вега
                                                                                "865676028255215", //Вега
                                                                                "866130020921886", //Альфа
                                                                                "866130020912539", //Байкал
                                                                                "866130020922405", //Град
                                                                                "865676028006766", //Дельта
                                                                                "866130020921704", //Ермак
                                                                                "866130020922546", //Заря
                                                                                "868518023062211", //Исеть
                                                                                "866130020921951", //Кедр
                                                                                "866130020922173", //Лидер
                                                                                "866130020922330", //Марс
                                                                                "866584024203909", //Нева
                                                                                "866130020920284", //Орбита
                                                                                "866130020921985", //Плутон
                                                                                "866130020922272", //Рубин
                                                                                "866130020922215", //Спутник
                                                                                "866130020922116", //Тобол
                                                                                "865676028319730", //Урал
                                                                                "866130020922249", //Феникс
                                                                                "866130020922314", //Хантер
                                                                                "866130020922413", //Шторм
                                                                                "866130020921944", //Юпитер
                                                                                "866130020924526" //Ястреб
                                                                        };

    private final String[]              PARTNERS_LIST =                 new String[] {
                                                                                "OKO",
                                                                                "VIDOK",
                                                                                "ARGO",
                                                                                "OSPAS",
                                                                                "SKIT",
                                                                                "TEST"
    };

    private Settings() {

        readConfig();

    }



    public static Settings getInstance() {

        Settings localInstance =                                        instance;

        if (localInstance == null) {

            synchronized (Settings.class) {

                localInstance =                                         instance;

                if (localInstance == null) {

                    instance =                                          localInstance = new Settings();

                }

            }

        }

        return localInstance;
    }



    /**
     *  Функция считывает файл конфига и создает коллекцию с параметрами
     *  ПАРАМЕТРЫ:
     *      - IMG_PATH: путь к папке с паспортами
     *      - CONNECT_PORT: порт сервисных сообщений сервера (запрос кол-ва новых файлов, снятие с охраны и т.п.)
     *      - DOWNLOAD_PORT: порт для передачи файлов
     *      - ACCEPT_TIMEOUT: таймаут подключения
     *      - BUFFER_SIZE: размер буфера для передачи файлов
     */
    private void readConfig(){
        String line;
        try(BufferedReader br =                                         new BufferedReader(new FileReader(CONFIG_FILE))) {

            while((line = br.readLine()) != null){

                if(line.length() > 0 && !line.startsWith("#") && line.contains("=")) {
                    String parts[] =                                    line.split("=");

                    switch (parts[0]) {

                        case "IMG_PATH":
                            setFilesPath(parts[1]);
                            break;

                        case "CONNECT_PORT":
                            setConnectPort(Integer.parseInt(parts[1]));
                            break;

                        case "DOWNLOAD_PORT":
                            setConnectPortFiles(Integer.parseInt(parts[1]));
                            break;

                        case "ACCEPT_TIMEOUT":
                            setAcceptTimeOut(Integer.parseInt(parts[1]));
                            break;

                        case "BUFFER_SIZE":
                            setBufferSize(Integer.parseInt(parts[1]));
                            break;

                    }

                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }




















    /**
     *  Геттеры и сеттеры
     */
    public int getAcceptTimeOut() {
        return acceptTimeOut * 1000;
    }

    private void setAcceptTimeOut(int acceptTimeOut) {
        this.acceptTimeOut =                                            acceptTimeOut;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    private void setBufferSize(int bufferSize) {
        this.bufferSize =                                               bufferSize;
    }

    public int getConnectPort() {
        return connectPort;
    }

    private void setConnectPort(int connectPort) {
        this.connectPort =                                              connectPort;
    }

    public int getConnectPortFiles() {
        return connectPortFiles;
    }

    private void setConnectPortFiles(int connectPortFiles) {
        this.connectPortFiles =                                         connectPortFiles;
    }

    public String getFilesPath() {
        return filesPath;
    }

    private void setFilesPath(String filesPath) {
        this.filesPath =                                                filesPath;
    }

    public String[] getIMEI_LIST() {
        return IMEI_LIST;
    }

    public String getGUARD_DIR() {
        return GUARD_DIR;
    }

    public String getGUARD_FILE() {
        return GUARD_FILE;
    }

    public String getSIGNALS_DIR() {
        return SIGNALS_DIR;
    }

    public String getSIGNALS_FILE() {
        return SIGNALS_FILE;
    }

    public String[] getIMEI_LIST_VIDOK() {
        return IMEI_LIST_VIDOK;
    }

    public String[] getIMEI_LIST_GBR() {
        return IMEI_LIST_GBR;
    }

    public String[] getPARTNERS_LIST() {
        return PARTNERS_LIST;
    }
}
