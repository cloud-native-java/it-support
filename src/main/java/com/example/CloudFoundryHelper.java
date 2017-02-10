package com.example;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

import java.util.Optional;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
class CloudFoundryHelper {

	private final CloudFoundryOperations cf;
	private final RetryTemplate rt;

	public CloudFoundryHelper(CloudFoundryOperations cf,
	                          RetryTemplate rt) {
		this.cf = cf;
		this.rt = rt;
	}

	public Optional<String> urlFor(String appName) {
		try {
			RetryCallback<Optional<String>, Throwable> retryCallback =
					retryContext -> this.cf.applications()
							.get(GetApplicationRequest.builder().name(appName).build())
							.map(ad -> (ad.getUrls().stream()).findFirst())
							.block();
			return (rt.execute(retryCallback));
		} catch (Throwable throwable) {
			return Optional.empty();
		}
	}
}
