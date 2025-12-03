
module org.example.messagingdesktop {
        requires javafx.controls;
        requires javafx.fxml;
        requires javafx.graphics;
        requires javafx.base;
        requires java.desktop; // <-- ADD THIS
        opens com.messaging.desktop to javafx.fxml;
        opens com.messaging.desktop.controllers to javafx.fxml;

        exports org.example.messagingdesktop;
        exports com.messaging.desktop;
        exports com.messaging.desktop.controllers;
        exports com.messaging.desktop.services;
        exports com.messaging.desktop.utils;
        exports com.messaging.models;
    opens com.messaging.models to javafx.fxml;
}