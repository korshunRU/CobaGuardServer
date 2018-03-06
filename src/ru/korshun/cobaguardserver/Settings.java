package ru.korshun.cobaguardserver;


import java.io.*;
import java.util.HashMap;


@SuppressWarnings("FieldCanBeLocal")
class Settings {

    private final String                    CONFIG_FILE =               "coba.conf";

    private String                          SIGNALS_DIR =               "signals";
    private String                          SIGNALS_FILE =              "get.txt";

    private String                          GUARD_DIR =                 "guard";
    private String                          GUARD_FILE =                "get.txt";

    private String                          LOG_DIR =                   "logs";
    private String                          LOG_FILE =                  "last_update.txt";

    private final int                       BUFFER_SIZE_DEFAULT =       8;

    private static volatile Settings        instance;

    private int                             connectPort;
    private int                             connectPortFiles;
    private int                             acceptTimeOut;
    private String                          filesPath;
    private int                             bufferSize =                BUFFER_SIZE_DEFAULT;

    private final String[]                  IMEI_LIST =                 new String[] {
                                                                                "862630038397890", //Литвиненко
                                                                                "356377065847495", //Зубов
                                                                                "356378065847493", //Зубов
                                                                                "353737067316257", //Аткин
                                                                                "353646069029281", //Манушкин
                                                                                "866130022032393", //Зарубо
                                                                                "866130022226318", //Зарубо
                                                                                "864630033627752", //Смирнов
                                                                                "864630033627745", //Смирнов
                                                                                "352879070024733", //Лобанов
                                                                                "352879070055737", //Лобанов
                                                                                "352022090650555", //Хамин
                                                                                "352023090650553", //Хамин
                                                                                "861937034901836", //Плюхин
                                                                                "861937034901828", //Плюхин
                                                                                "359004050349439", //Новиков
                                                                                "860332025891454", //Чурсинов
                                                                                "357085085092302", //Чинцов
                                                                                "357085085092310", //Чинцов
                                                                                "860332025895307", //Колесников
                                                                                "860153038107781", //Артюшенко
                                                                                "860153038107799", //Артюшенко
                                                                                "861111039852044", //Федоров
                                                                                "861111039852051", //Федоров
                                                                                "359601089867982", //Журавлев
                                                                                "359602089867980", //Журавлев
                                                                                "862678034810481", //Иванов
                                                                                "862546028375433", //Бабушкин
                                                                                "862741017056736", //Артюшенко
                                                                                "862741017056744", //Артюшенко
                                                                                "860332025887171", //Дежурный инженер
                                                                                "353164055513482", //Бормонтов
                                                                                "864706020373582", //Федотовских
                                                                                "359652051686280", //Дементьев
                                                                                "867580021835619", //Конев
                                                                                "867580021835627", //Конев
                                                                                "357963058548502", //Георгиади
                                                                                "357964058548500", //Георгиади
                                                                                "866607021979633", //Прудников
                                                                                "866607021979641" //Прудников
    };

    private final String[]                  IMEI_LIST_VIDOK =           new String[] {
                                                                                "357967051804531", //Моисеев
                                                                                "358436074737127", //Фурик
                                                                                "358437074737125", //Фурик
                                                                                "861895035353847", //Фахрисламов
                                                                                "861895035353854", //Фахрисламов
                                                                                "357351070161183", //Каширский В.
                                                                                "354588070278391", //Каширский Н.
                                                                                "358504070313043", //Савкин
                                                                                "358504070313040", //Савкин
                                                                                "354442081676963", //Варанкин
                                                                                "357189054627179", //Савкин
                                                                                "352384070833241", //Косяковский
                                                                                "357033054327762", //Богданов
                                                                                "357473050360476", //Кузьмин
                                                                                "866709038555750", //Мишарин А.
                                                                                "866709038555768", //Мишарин А.
                                                                                "355625085715261", //Мишарин C.
                                                                                "355751085715265", //Мишарин C.
                                                                                "356440083354033", //Лапин
                                                                                "356441083354031" //Лапин
                                                                        };

    private final String[]                  IMEI_LIST_GBR =             new String[] {
//                                                                                "862630038397890", //Литвиненко
                                                                                "866035032183724", //Центр
                                                                                "866035032183732", //Центр
                                                                                "866588032071620", //Вега
                                                                                "866588032071638", //Вега
                                                                                "865407032345044", //Альфа
                                                                                "865407032345051", //Альфа
                                                                                "865407031557169", //Байкал
                                                                                "865407031557177", //Байкал
                                                                                "865407032329600", //Град
                                                                                "865407032329618", //Град
                                                                                "865905032824389", //Дельта
                                                                                "865905032824397", //Дельта
                                                                                "865407032418668", //Ермак
                                                                                "865407032418676", //Ермак
                                                                                "866036034347564", //Заря
                                                                                "866036034347572", //Заря
                                                                                "865407032423304", //Исеть
                                                                                "865407032423312", //Исеть
                                                                                "865407035590786", //Кедр
                                                                                "865407035590794", //Кедр
                                                                                "866588032072685", //Лидер
                                                                                "866588032072693", //Лидер
                                                                                "866588032020882", //Марс
                                                                                "866588032020890", //Марс
                                                                                "866035034782101", //Нева
                                                                                "866035034782119", //Нева
                                                                                "866588032085844", //Орбита
                                                                                "866588032085851", //Орбита
                                                                                "866588032058783", //Плутон
                                                                                "866588032058791", //Плутон
                                                                                "866588031014688", //Рубин
                                                                                "866588031014696", //Рубин
                                                                                "866035032156969", //Спутник
                                                                                "866035032156977", //Спутник
                                                                                "866588032075043", //Тобол
                                                                                "866588032075050", //Тобол
                                                                                "866588031982447", //Урал
                                                                                "866588031982454", //Урал
                                                                                "352719083767011", //Феникс
                                                                                "352720083767019", //Феникс
                                                                                "866588032057900", //Хантер
                                                                                "866588032057918", //Хантер
                                                                                "866588032080583", //Шторм
                                                                                "866588032080571", //Шторм
                                                                                "866036033046266", //Юпитер
                                                                                "866036033046274", //Юпитер
                                                                                "866588032060482", //Щит
                                                                                "866588032060490", //Щит
                                                                                "866588031644229", //Ястреб
                                                                                "866588031644237" //Ястреб
                                                                        };

    private final HashMap<String, String> IMEI_MAP_GBR = new HashMap<>();

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

//        IMEI_MAP_GBR.put("862630038397890", "Литвиненко");
        IMEI_MAP_GBR.put("866035032183724", "Центр");
        IMEI_MAP_GBR.put("866035032183732", "Центр");
        IMEI_MAP_GBR.put("866588032071620", "Вега");
        IMEI_MAP_GBR.put("866588032071638", "Вега");
        IMEI_MAP_GBR.put("865407032345044", "Альфа");
        IMEI_MAP_GBR.put("865407032345051", "Альфа");
        IMEI_MAP_GBR.put("865407031557169", "Байкал");
        IMEI_MAP_GBR.put("865407031557177", "Байкал");
        IMEI_MAP_GBR.put("865407032329600", "Град");
        IMEI_MAP_GBR.put("865407032329618", "Град");
        IMEI_MAP_GBR.put("865905032824389", "Дельта");
        IMEI_MAP_GBR.put("865905032824397", "Дельта");
        IMEI_MAP_GBR.put("865407032418668", "Ермак");
        IMEI_MAP_GBR.put("865407032418676", "Ермак");
        IMEI_MAP_GBR.put("866036034347564", "Заря");
        IMEI_MAP_GBR.put("866036034347572", "Заря");
        IMEI_MAP_GBR.put("865407032423304", "Исеть");
        IMEI_MAP_GBR.put("865407032423312", "Исеть");
        IMEI_MAP_GBR.put("865407035590786", "Кедр");
        IMEI_MAP_GBR.put("865407035590794", "Кедр");
        IMEI_MAP_GBR.put("866588032072685", "Лидер");
        IMEI_MAP_GBR.put("866588032072693", "Лидер");
        IMEI_MAP_GBR.put("866588032020882", "Марс");
        IMEI_MAP_GBR.put("866588032020890", "Марс");
        IMEI_MAP_GBR.put("866035034782101", "Нева");
        IMEI_MAP_GBR.put("866035034782119", "Нева");
        IMEI_MAP_GBR.put("866588032085844", "Орбита");
        IMEI_MAP_GBR.put("866588032085851", "Орбита");
        IMEI_MAP_GBR.put("866588032058783", "Плутон");
        IMEI_MAP_GBR.put("866588032058791", "Плутон");
        IMEI_MAP_GBR.put("866035032156969", "Спутник");
        IMEI_MAP_GBR.put("866035032156977", "Спутник");
        IMEI_MAP_GBR.put("866588032075043", "Тобол");
        IMEI_MAP_GBR.put("866588032075050", "Тобол");
        IMEI_MAP_GBR.put("866588031982447", "Урал");
        IMEI_MAP_GBR.put("866588031982454", "Урал");
        IMEI_MAP_GBR.put("352719083767011", "Феникс");
        IMEI_MAP_GBR.put("352720083767019", "Феникс");
        IMEI_MAP_GBR.put("866588032057900", "Хантер");
        IMEI_MAP_GBR.put("866588032057918", "Хантер");
        IMEI_MAP_GBR.put("866588032080583", "Шторм");
        IMEI_MAP_GBR.put("866588032080571", "Шторм");
        IMEI_MAP_GBR.put("866036033046266", "Юпитер");
        IMEI_MAP_GBR.put("866036033046274", "Юпитер");
        IMEI_MAP_GBR.put("866588031014688", "Рубин");
        IMEI_MAP_GBR.put("866588031014696", "Рубин");
        IMEI_MAP_GBR.put("866588031644229", "Ястреб");
        IMEI_MAP_GBR.put("866588031644237", "Ястреб");

    }



    static Settings getInstance() {

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
    int getAcceptTimeOut() {
        return acceptTimeOut * 1000;
    }

    private void setAcceptTimeOut(int acceptTimeOut) {
        this.acceptTimeOut =                                            acceptTimeOut;
    }

    int getBufferSize() {
        return bufferSize;
    }

    private void setBufferSize(int bufferSize) {
        this.bufferSize =                                               bufferSize;
    }

    int getConnectPort() {
        return connectPort;
    }

    private void setConnectPort(int connectPort) {
        this.connectPort =                                              connectPort;
    }

    int getConnectPortFiles() {
        return connectPortFiles;
    }

    private void setConnectPortFiles(int connectPortFiles) {
        this.connectPortFiles =                                         connectPortFiles;
    }

    String getFilesPath() {
        return filesPath;
    }

    private void setFilesPath(String filesPath) {
        this.filesPath =                                                filesPath;
    }

    String[] getIMEI_LIST() {
        return IMEI_LIST;
    }

    String getGUARD_DIR() {
        return GUARD_DIR;
    }

    String getGUARD_FILE() {
        return GUARD_FILE;
    }

    String getSIGNALS_DIR() {
        return SIGNALS_DIR;
    }

    String getSIGNALS_FILE() {
        return SIGNALS_FILE;
    }

    String[] getIMEI_LIST_VIDOK() {
        return IMEI_LIST_VIDOK;
    }

    String[] getIMEI_LIST_GBR() {
        return IMEI_LIST_GBR;
    }

    String[] getPARTNERS_LIST() {
        return PARTNERS_LIST;
    }

    public String getLOG_DIR() {
        return LOG_DIR;
    }

    public String getLOG_FILE() {
        return LOG_FILE;
    }

    public HashMap<String, String> getIMEI_MAP_GBR() {
        return IMEI_MAP_GBR;
    }
}
