package cnj;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegrationTestSupportAutoConfiguration {

		@Bean
		CloudFoundryService cfs(CloudFoundryOperations cfo) {
				return new CloudFoundryService(cfo);
		}
}
