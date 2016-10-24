package net.sllmdilab.clinicalmapper.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import net.sllmdilab.clinicalmapper.servlet.ClinicalMapperServlet;

public class ClinicalMapperWebAppInitializer implements WebApplicationInitializer {

	private static final String SERVLET_NAME = "dispatcher";

	@Override
	public void onStartup(ServletContext container) {
		// Create the 'root' Spring application context
		AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
		rootContext.register(SecurityConfig.class);
		rootContext.register(ClinicalMapperApplicationConfig.class);
		
		// Manage the lifecycle of the root application context
		container.addListener(new ContextLoaderListener(rootContext));

		ServletRegistration.Dynamic servlet = container.addServlet(SERVLET_NAME, new ClinicalMapperServlet());
		servlet.setLoadOnStartup(1);
		servlet.addMapping("/fhir/*");
	}
}
