//to use Jfoenix normally, add the VM argument:		--add-opens java.base/java.lang.reflect=com.jfoenix
//and when you create the installer and .exe files, you should also include this VM argument in "jpackage" command
module client_module {
	requires tls_module;
	
	requires com.jfoenix;
	requires java.desktop;
	requires javafx.base;
	requires transitive javafx.controls;
	requires javafx.fxml;
	requires transitive javafx.graphics;
	requires javafx.swing;
	exports app to javafx.graphics;
	opens fxml_controller to javafx.fxml;
	
	//do not forget it!!! or java.sql.SQLException: No suitable driver found for jdbc:derby:XXX
	requires org.apache.derby.engine;
	//containing derby driver: org.apache.derby.jdbc.EmbeddedDriver
	requires org.apache.derby.tools;
	
	requires tomcat.jdbc;
	requires org.apache.tomcat.juli;
	
	requires java.sql;
}
