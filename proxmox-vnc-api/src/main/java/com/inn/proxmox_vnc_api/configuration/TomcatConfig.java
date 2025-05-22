//package com.inn.proxmox_vnc_api.configuration;
//
//import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
//import org.springframework.boot.web.server.WebServerFactoryCustomizer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//
//@Configuration
//public class TomcatConfig {
//
//	@Bean
//	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> customizer() {
//	    return factory -> factory.addConnectorCustomizers(connector -> {
//	        connector.setProperty("keepAliveTimeout", "70000");
//	        connector.setProperty("connectionTimeout", "70000");
//	    });
//	}
//
//}
