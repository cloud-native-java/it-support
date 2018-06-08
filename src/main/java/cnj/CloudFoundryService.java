package cnj;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationManifestUtils;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.routes.ListRoutesRequest;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.operations.services.CreateUserProvidedServiceInstanceRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/

public class CloudFoundryService {

		private final CloudFoundryOperations cf;

		CloudFoundryService(CloudFoundryOperations cf) {
				this.cf = cf;
		}

		public Flux<Route> findRoutesForApplication(String appName) {
				return cf
					.routes()
					.list(ListRoutesRequest.builder().build())
					.filter(r -> r.getApplications().contains(appName));
		}

		public Mono<String> pushApplication(ApplicationManifest manifest) {
				String appName = getNameForManifest(manifest);
				return cf
					.applications()
					.pushManifest(PushApplicationManifestRequest.builder().manifest(manifest).build())
					.then(Mono.just(appName));
		}

		public Mono<String> createBackingService(String applicationNameMono) {
				return Mono.just(applicationNameMono).flatMap(
					appName -> cf
						.applications()
						.get(GetApplicationRequest.builder().name(appName).build())
						.flatMap(x -> urlForApplication(appName, false))
						.map(url -> Collections.singletonMap("uri", url))
						.flatMap(
							credentials -> cf.services()
								.createUserProvidedInstance(CreateUserProvidedServiceInstanceRequest.builder().name(appName).credentials(credentials).build())).then(Mono.just(appName)));
		}

		public Mono<String> urlForApplication(String appName, boolean https) {
				return cf
					.applications()
					.get(GetApplicationRequest.builder().name(appName).build())
					.map(ad -> ad.getUrls().iterator().next())
					.map(url -> "http" + (https ? "s" : "") + "://" + url);
		}

		public String getNameForManifest(File f) {
				return getNameForManifest(f.toPath());
		}

		public ApplicationManifest getManifestFor(Path p) {
				return ApplicationManifestUtils.read(p).iterator().next();
		}

		public String getNameForManifest(Path p) {
				return getNameForManifest(getManifestFor(p));
		}

		public String getNameForManifest(ApplicationManifest applicationManifest) {
				return applicationManifest.getName();
		}

		public Mono<String> pushApplicationAndCreateBackingService(ApplicationManifest manifest) {
				return pushApplication(manifest).flatMap(this::createBackingService);
		}
}

