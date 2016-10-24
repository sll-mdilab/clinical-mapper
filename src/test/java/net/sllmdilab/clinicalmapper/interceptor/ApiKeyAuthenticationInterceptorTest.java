package net.sllmdilab.clinicalmapper.interceptor;

import static org.mockito.Mockito.when;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import net.sllmdilab.clinicalmapper.interceptor.ApiKeyAuthenticationInterceptor;

public class ApiKeyAuthenticationInterceptorTest {

	private ApiKeyAuthenticationInterceptor interceptor;
	
	private static final String MOCK_KEY="jljgeklawgh435";
	
	@Mock
	private HttpServletRequest mockRequest;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		
		interceptor = new ApiKeyAuthenticationInterceptor();
		
		interceptor.setActualApiKey(MOCK_KEY);
	}
	
	@Test(expected=AuthenticationException.class)
	public void invalidCredentialsAreRejected() {
		when(mockRequest.getHeader("Authorization")).thenReturn("Basic " + Base64.getEncoder().encodeToString(("user:" + "blabla").getBytes()));
		
		interceptor.incomingRequestPostProcessed(null, mockRequest, null);
	}

	@Test
	public void validCredentialsAreAccepted() {
		when(mockRequest.getHeader("Authorization")).thenReturn("Basic " + Base64.getEncoder().encodeToString(("user:" + MOCK_KEY).getBytes()));
		
		interceptor.incomingRequestPostProcessed(null, mockRequest, null);
	}
	
	@Test(expected=AuthenticationException.class)
	public void wrongAuthTypeIsRejected() {
		when(mockRequest.getHeader("Authorization")).thenReturn("Digest " + Base64.getEncoder().encodeToString(("user:" + MOCK_KEY).getBytes()));
		
		interceptor.incomingRequestPostProcessed(null, mockRequest, null);
	}
}
