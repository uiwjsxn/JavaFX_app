module server_module {
	requires tls_module;
	requires java.sql;
	requires tomcat.jdbc;
	requires org.apache.tomcat.juli;
	requires security_code_module;
}