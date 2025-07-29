module com.warpshare {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires jmdns;
    requires jdk.httpserver;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires org.apache.httpcomponents.client5.httpclient5;


    opens com.warpshare to javafx.fxml;
    exports com.warpshare;
}