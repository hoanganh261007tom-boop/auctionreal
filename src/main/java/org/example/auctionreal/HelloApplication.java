package org.example.auctionreal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 500);
        stage.setTitle("Hệ thống đấu giá - Đăng nhập");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(500);
        stage.setMinHeight(450);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
