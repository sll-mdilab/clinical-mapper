package net.sllmdilab.clinicalmapper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ca.uhn.fhir.context.FhirContext;
import net.sllmdilab.clinicalmapper.database.VirtuosoDBClient;

@Configuration
@ComponentScan({ "net.sllmdilab.clinicalmapper.*" })
@EnableTransactionManagement
public class ClinicalMapperApplicationConfig {

	@Value("${T5_RDF_USER}")
	private String rdfUser;

	@Value("${T5_RDF_PASSWORD}")
	private String rdfPassword;

	@Value("${T5_RDF_HOST}")
	private String rdfHost;

	@Value("${T5_RDF_PORT}")
	private String rdfPort;
	
	@Bean
	public static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
		PropertyPlaceholderConfigurer placeholderConfigurer = new PropertyPlaceholderConfigurer();
		placeholderConfigurer.setSystemPropertiesModeName("SYSTEM_PROPERTIES_MODE_OVERRIDE");
		return placeholderConfigurer;
	}

	@Bean
	public FhirContext fhirContext() {
		return FhirContext.forDstu2();
	}
	
	
	@Bean(name = "virtuosoClient")
	public VirtuosoDBClient virtuosoClient() {
		String jdbcConn = rdfHost + ":" + rdfPort;
		String graphUri = "jdbc:virtuoso://" + jdbcConn + "/charset=UTF-8/log_enable=2";

		return new VirtuosoDBClient(graphUri, rdfUser, rdfPassword);
	}
	
}
