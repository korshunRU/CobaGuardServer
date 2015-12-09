package ru.korshun.cobaguardserver;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;




public final class Logging {

    private static final String ERROR_FILE =            "error.log";
    private static final String ACCESS_FILE =           "access.log";

    private static final String LOG_DIR =               "logs";

    private static final String DATE_FORMAT =           "dd.MM.yyyy HH:mm:ss";

//    private static String timeStamp =                   new SimpleDateFormat("dd.MM.yyyy hh:mm:ss").format(new Date());




    /** Функция записи строки в файл
            type:
                - access: файл доступа
                - error: файл ошибок
            str: строка для записи
     */
    public static void writeToFile(String type, String str) {

        String fileName = null;

        switch (type) {
            case "access":
                fileName = ACCESS_FILE;
                break;

            case "error":
                fileName = ERROR_FILE;
                break;
        }

        fileExists(fileName);

        File file = new File(LOG_DIR + File.separator + fileName);

        try(FileWriter out = new FileWriter(file.getAbsoluteFile(), true)) {
//            FileWriter out = new FileWriter(file.getAbsoluteFile(), true);
            out.write(new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime()) + ": " + str + "\r\n");
//            out.close();
        } catch (IOException e) {
            e.printStackTrace();
//            writeToFile("error", e.getMessage());
        }

    }



    /** Функция записи строки в файл
        imei: imei телефона
        type:
            - access: файл доступа
            - error: файл ошибок
        str: строка для записи
    */
    public static void writeToFile(String imei, String type, String str) {

        String fileName = null;

        switch (type) {
            case "access":
                fileName = ACCESS_FILE;
                break;

            case "error":
                fileName = ERROR_FILE;
                break;
        }

        fileExists(imei, fileName);

        File file = new File(LOG_DIR + File.separator + imei + File.separator + fileName);

        try(FileWriter out = new FileWriter(file.getAbsoluteFile(), true)) {
//            FileWriter out = new FileWriter(file.getAbsoluteFile(), true);
            out.write(new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime()) + ": " + imei + " - " + str + "\r\n");
//            out.close();
        } catch (IOException e) {
            e.printStackTrace();
//            writeToFile(imei, "error", e.getMessage());
        }

    }




    // Функция проверки существования директории
    private static void dirExists(String dir) {

        if(!new File(dir).exists()) {
            new File(dir).mkdir();
        }

    }


    // Функция проверки существования файла
    private static void fileExists(String file) {

        dirExists(LOG_DIR);

        if(!new File(LOG_DIR + File.separator + file).exists()) {
            try {
                new File(LOG_DIR + File.separator + file).createNewFile();
            } catch (IOException e) {
                writeToFile("error", e.getMessage());
            }
        }

    }


    // Функция проверки существования файла
    private static void fileExists(String imei, String file) {

//        dirExists(LOG_DIR);
        dirExists(LOG_DIR + File.separator + imei);

        if(!new File(LOG_DIR + File.separator + imei + File.separator + file).exists()) {
            try {
                new File(LOG_DIR+ File.separator + imei + File.separator + file).createNewFile();
            } catch (IOException e) {
                writeToFile(imei, "error", e.getMessage());
            }
        }

    }



}
