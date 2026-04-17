module com.trainerclienthub {
    
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    
    requires java.sql;

    
    requires java.logging;
    requires jbcrypt;


    opens com.trainerclienthub            to javafx.fxml;
    opens com.trainerclienthub.controller to javafx.fxml;
    opens com.trainerclienthub.model      to javafx.fxml, javafx.base;
    opens com.trainerclienthub.service    to javafx.fxml;
    opens com.trainerclienthub.util       to javafx.fxml;


    exports com.trainerclienthub;
    exports com.trainerclienthub.controller;
    exports com.trainerclienthub.model;
    exports com.trainerclienthub.service;
    exports com.trainerclienthub.DAO;
    exports com.trainerclienthub.db;
    exports com.trainerclienthub.util;
}