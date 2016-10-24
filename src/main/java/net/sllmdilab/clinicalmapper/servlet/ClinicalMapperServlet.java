package net.sllmdilab.clinicalmapper.servlet;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import net.sllmdilab.clinicalmapper.config.ClinicalMapperApplicationConfig;
import net.sllmdilab.clinicalmapper.conformance.ConformanceProvider;
import net.sllmdilab.clinicalmapper.interceptor.ApiKeyAuthenticationInterceptor;

@Import(ClinicalMapperApplicationConfig.class)
@Component
public class ClinicalMapperServlet extends RestfulServer {
	private Logger logger = LoggerFactory.getLogger(ClinicalMapperServlet.class);

	private static final long serialVersionUID = 1L;
	
	@Autowired
	private ApiKeyAuthenticationInterceptor authenticationInterceptor;

	@Autowired
	private IResourceProvider[] resourceProviders;
	
	@Autowired
	private ConformanceProvider conformanceProvider;
	
	@Override
	protected void initialize() throws ServletException {
		SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

		setFhirContext(FhirContext.forDstu2());

		setResourceProviders(resourceProviders);
		setInterceptors(authenticationInterceptor);
		setServerConformanceProvider(conformanceProvider);
		logger.info("Initialized servlet.");
	}
}
