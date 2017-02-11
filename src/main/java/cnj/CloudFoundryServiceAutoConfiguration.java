package cnj;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;

/**
 * supports high level Cloud Foundry operations. Intended to cover the bulk of the
 * things a typical Cloud Foundry shell script might do.
 *
 */
@Configuration
//@EnableRetry
public class CloudFoundryServiceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public RetryTemplate retryTemplate() {
		RetryTemplate rt = new RetryTemplate();
		rt.setBackOffPolicy(new ExponentialBackOffPolicy());
		rt.setRetryPolicy(new SimpleRetryPolicy(20,
				Collections.singletonMap(RuntimeException.class, true)));
		return rt;
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactorCloudFoundryClient cloudFoundryClient(
			ConnectionContext connectionContext,
			TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient
				.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactorDopplerClient dopplerClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorDopplerClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public DefaultConnectionContext connectionContext(@Value("${cf.api}") String apiHost) {
		if (apiHost.contains("://")) {
			apiHost = apiHost.split("://")[1];
		}
		return DefaultConnectionContext.builder()
				.apiHost(apiHost)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	ReactorUaaClient uaaClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorUaaClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public PasswordGrantTokenProvider tokenProvider(@Value("${cf.user}") String username,
	                                                @Value("${cf.password}") String password) {
		return PasswordGrantTokenProvider.builder()
				.password(password)
				.username(username)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public DefaultCloudFoundryOperations cloudFoundryOperations(
			CloudFoundryClient cloudFoundryClient,
			ReactorDopplerClient dopplerClient,
			ReactorUaaClient uaaClient,
			@Value("${cf.org}") String organization,
			@Value("${cf.space}") String space) {
		return DefaultCloudFoundryOperations.builder()
				.cloudFoundryClient(cloudFoundryClient)
				.dopplerClient(dopplerClient)
				.uaaClient(uaaClient)
				.organization(organization)
				.space(space)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public CloudFoundryService helper(DefaultCloudFoundryOperations cf) {
		return new CloudFoundryService(cf);
	}
}
