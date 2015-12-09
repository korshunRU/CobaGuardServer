package ru.korshun.cobaguardserver;

import javafx.application.Platform;

import java.awt.*;
import java.awt.event.ActionListener;




public final class SystemTrayIcon {

    public static void createTrayIcon() {

        TrayIcon trayIcon;

        if (SystemTray.isSupported()) {

            SystemTray tray =                                           SystemTray.getSystemTray();

            java.awt.Image image =                                      Toolkit.getDefaultToolkit().getImage("img/tray.png");

            final ActionListener closeListener = e -> {
                Platform.exit();
                System.exit(0);
            };

            PopupMenu popup =                                           new PopupMenu();
            MenuItem closeItem =                                        new MenuItem("Выход");

            closeItem.addActionListener(closeListener);
            popup.add(closeItem);

            trayIcon =                                                  new TrayIcon(image, "CobaGuardServer", popup);

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
    }

}
