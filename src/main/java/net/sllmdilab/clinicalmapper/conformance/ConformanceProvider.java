package net.sllmdilab.clinicalmapper.conformance;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.resource.Conformance;
import ca.uhn.fhir.model.dstu2.resource.Conformance.Rest;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.provider.dstu2.ServerConformanceProvider;

@Component
public class ConformanceProvider extends ServerConformanceProvider {
	
	@Value("${T5_XREF_OAUTH_AUTHORIZE_URI}")
	private String authorizeUri;
	
	@Value("${T5_XREF_OAUTH_TOKEN_URI}")
	private String tokenUri;
	
	public ConformanceProvider() {}

	public ConformanceProvider(RestfulServer theRestfulServer) {
		super(theRestfulServer);
	}

	@Override
	public Conformance getServerConformance(HttpServletRequest theRequest) {
		Conformance conformance = super.getServerConformance(theRequest);
		
		ExtensionDt authorizeExtension = new ExtensionDt();
		authorizeExtension.setUrl("authorize");
		authorizeExtension.setValue(new UriDt(authorizeUri));
		
		ExtensionDt tokenExtension = new ExtensionDt();
		tokenExtension.setUrl("token");
		tokenExtension.setValue(new UriDt(tokenUri));
		
		ExtensionDt oauthExtension = new ExtensionDt();
		oauthExtension.setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");
		oauthExtension.addUndeclaredExtension(authorizeExtension);
		oauthExtension.addUndeclaredExtension(tokenExtension);
		
		Rest rest = conformance.getRestFirstRep();
		rest.getSecurity().addUndeclaredExtension(oauthExtension);
		return conformance;
	}

}
