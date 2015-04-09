package ru.korshun.cobaguardserver;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Created by user on 28.12.2014.
 */
public final class SystemTrayIcon {

    private static TrayIcon trayIcon;


    public static void createIcon() {
        createTrayIcon();
    }


    private static void createTrayIcon() {
        if (SystemTray.isSupported()) {

            SystemTray tray = SystemTray.getSystemTray();

            java.awt.Image image = Toolkit.getDefaultToolkit().getImage("img/tray.png");

//            stage.setOnCloseRequest(t -> hide(stage) );

            final ActionListener closeListener = e -> {
                System.out.println("Сервер отключен");

                Platform.exit();
                System.exit(0);
            };

//            ActionListener showListener = e -> Platform.runLater(stage::show);

            // create a popup menu
            PopupMenu popup = new PopupMenu();

//            MenuItem showItem = new MenuItem("Show");
//            showItem.addActionListener(showListener);
//            popup.add(showItem);

            MenuItem closeItem = new MenuItem("Выход");
            closeItem.addActionListener(closeListener);
            popup.add(closeItem);

            trayIcon = new TrayIcon(image, "CobaGuardServer", popup);

//            trayIcon.addActionListener(showListener);

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
    }

}
