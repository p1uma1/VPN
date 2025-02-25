module com.example.vpn {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.net.http;


    opens com.example.vpn to javafx.fxml;

}