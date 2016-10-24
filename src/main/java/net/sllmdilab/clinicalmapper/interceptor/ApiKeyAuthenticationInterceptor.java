package net.sllmdilab.clinicalmapper.interceptor;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.method.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;

@Component
public class ApiKeyAuthenticationInterceptor extends InterceptorAdapter {
	private Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationInterceptor.class);

	@Value("${T5_XREF_API_KEY}")
	private String actualApiKey;
	
	void setActualApiKey(String actualApiKey) {
		this.actualApiKey = actualApiKey;
	}

	@Override
	public boolean incomingRequestPostProcessed(RequestDetails requestDetails, HttpServletRequest request,
			HttpServletResponse response) throws AuthenticationException {

		logger.info("Verifying API key.");

		if (!StringUtils.isBlank(actualApiKey)) {
			if (!Arrays.equals(getBasicAuth(request), buildCredentials(actualApiKey))) {
				throw new AuthenticationException("Incorrect API key.");
			}

		} else {
			logger.info("API key not specified in server.");
		}

		return true;
	}

	private byte[] buildCredentials(String apiKey) {
		return ("user:" + apiKey).getBytes();
	}

	private byte[] getBasicAuth(HttpServletRequest request) {
		String auth = request.getHeader("Authorization");

		if (auth == null || !auth.startsWith("Basic")) {
			throw new AuthenticationException("Invalid auth type in request.");
		}

		return Base64.decodeBase64(auth.substring(6));
	}
}
