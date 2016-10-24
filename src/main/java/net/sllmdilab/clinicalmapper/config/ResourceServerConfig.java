package net.sllmdilab.clinicalmapper.config;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.mitre.oauth2.introspectingfilter.IntrospectingTokenService;
import org.mitre.oauth2.introspectingfilter.service.IntrospectionAuthorityGranter;
import org.mitre.oauth2.introspectingfilter.service.IntrospectionConfigurationService;
import org.mitre.oauth2.introspectingfilter.service.impl.SimpleIntrospectionAuthorityGranter;
import org.mitre.oauth2.introspectingfilter.service.impl.StaticIntrospectionConfigurationService;
import org.mitre.oauth2.model.RegisteredClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	@Value("${T5_XREF_OAUTH_INTROSPECTION_URL}")
	private String introspectionUrl;
	
	@Value("${T5_XREF_OAUTH_CLIENT_ID}")
	private String clientId;
	
	@Value("${T5_XREF_OAUTH_CLIENT_SECRET}")
	private String clientSecret;
	
	@Bean
	public IntrospectionConfigurationService introspectionConfigurationService() {
		StaticIntrospectionConfigurationService service = new StaticIntrospectionConfigurationService();
		service.setIntrospectionUrl(introspectionUrl);

		RegisteredClient client = new RegisteredClient();
		client.setClientId(clientId);
		client.setClientSecret(clientSecret);

		service.setClientConfiguration(client);

		return service;
	}

	@Bean
	public IntrospectingTokenService introspectingTokenService() {
		IntrospectingTokenService service = new IntrospectingTokenService();
		service.setIntrospectionConfigurationService(introspectionConfigurationService());
		service.setIntrospectionAuthorityGranter(introspectionAuthorityGranter());
		
		return service;
	}

	@Bean
	public IntrospectionAuthorityGranter introspectionAuthorityGranter() {
		SimpleIntrospectionAuthorityGranter authGranter = new SimpleIntrospectionAuthorityGranter();

		authGranter.setAuthorities(Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

		return authGranter;
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		if(StringUtils.isBlank(clientId)) {
			http
			.anonymous()
			.and()
			.authorizeRequests()
				.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.antMatchers("/**").permitAll();
		} else {
			http
			.anonymous()
			.and()
			.authorizeRequests()
				.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.antMatchers("/fhir/metadata").permitAll()
				.antMatchers("/**").authenticated();
			
		}
		
		http.addFilterBefore(corsFilter(), HeaderWriterFilter.class);
	}
	
	@Bean
	public CorsFilter corsFilter() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowCredentials(true);
		corsConfiguration.addExposedHeader("Location");
		corsConfiguration.addAllowedMethod(CorsConfiguration.ALL);
		corsConfiguration.addAllowedOrigin(CorsConfiguration.ALL);
		corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);

		UrlBasedCorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();
		corsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
		return new CorsFilter(corsConfigurationSource);
	}

	@Override
	public void configure(ResourceServerSecurityConfigurer resourceConf) throws Exception {
		
	}
}
