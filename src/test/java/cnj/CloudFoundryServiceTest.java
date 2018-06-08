package cnj;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationManifestUtils;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.PushApplicationRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@SpringBootTest(classes = CloudFoundryServiceTest.Config.class)
@RunWith(SpringRunner.class)
public class CloudFoundryServiceTest {

		private static File TMP_DIR = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());

		@Autowired
		private CloudFoundryService cfs;

		@Autowired
		private CloudFoundryOperations cfo;

		@Value("classpath:/manifest.yml")
		private Resource testAppManifest;

		@Value("classpath:/hi.jar")
		private Resource testAppJar;

		private File testAppJarFile;
		private File testAppManifestFile;

		private final String name = "cnj-it-support-test-app";


		private static void copy(InputStream i, OutputStream o) throws Exception {
				try (BufferedInputStream bi = new BufferedInputStream(i);
									BufferedOutputStream bo = new BufferedOutputStream(o)) {
						StreamUtils.copy(bi, bo);
				}
		}

		static {
				TMP_DIR.mkdirs();
				TMP_DIR.deleteOnExit();
		}

		private static File stageTemporaryFileForInputStream(String fn, InputStream i) throws Exception {
				File f = new File(TMP_DIR, fn);
				copy(i, new FileOutputStream(f));
				Assert.assertTrue(f.exists());
				return f;
		}

		@Before
		public void before() throws Exception {

				this.testAppJarFile = stageTemporaryFileForInputStream("hi.jar", this.testAppJar.getInputStream());
				this.testAppManifestFile = stageTemporaryFileForInputStream("manifest.yml", this.testAppManifest.getInputStream());

				Flux<String> push =
					this.cfo
						.applications()
						.push(PushApplicationRequest.builder().path(this.testAppJarFile.toPath())
							.randomRoute(true)
							.name(name)
							.buildpack("java_buildpack")
							.build())
						.thenMany(Flux.just(name));

				Predicate<ApplicationSummary> deployedPredicate = as -> as.getName().equalsIgnoreCase(name);

				Flux<ApplicationSummary> summaryFlux = this
					.cfo
					.applications()
					.list()
					.filter(deployedPredicate)
					.switchIfEmpty(push.thenMany(cfo.applications().list().filter(deployedPredicate)));

				StepVerifier
					.create(summaryFlux)
					.expectNextMatches(am -> am.getName().equalsIgnoreCase(name))
					.verifyComplete();
		}

		@Test
		public void findRoutesForApplication() {
				StepVerifier
					.create(this.cfs.findRoutesForApplication(this.name))
					.expectNextMatches(r -> r.getApplications().contains(this.name) && r.getHost().contains(this.name))
					.verifyComplete();
		}

		@Test
		public void pushApplication() {

				List<ApplicationManifest> applicationManifests = ApplicationManifestUtils
					.read(this.testAppManifestFile.toPath());

				Flux<String> pushApplications = Flux.fromStream(applicationManifests.stream())
					.flatMap(am -> this.cfs.pushApplicationWithManifest(am));

				StepVerifier
					.create(pushApplications)
					.expectNextMatches(appName -> applicationManifests.stream().anyMatch(am -> am.getName().equalsIgnoreCase(appName)))
					.verifyComplete();
		}

		@Test
		public void createBackingService() {

				List<ApplicationManifest> applicationManifests =
					ApplicationManifestUtils.read(this.testAppManifestFile.toPath());

				Flux<Void> deleteExistingApps = Flux
					.fromStream(applicationManifests.stream().map(ApplicationManifest::getName))
					.flatMap(appName -> cfo.services().deleteInstance(DeleteServiceInstanceRequest.builder().name(appName).build()));

				Flux<String> createBackingService = Flux
					.fromStream(applicationManifests.stream())
					.flatMap(am -> this.cfs.createBackingService(am.getName()));

				StepVerifier
					.create(
						deleteExistingApps
							.thenMany(createBackingService)
					)
					.expectNextMatches(appName -> applicationManifests.stream().anyMatch(a -> a.getName().equalsIgnoreCase(appName)))
					.verifyComplete();
		}

		@Test
		public void urlForApplication() {
				List<ApplicationManifest> applicationManifests =
					ApplicationManifestUtils.read(this.testAppManifestFile.toPath());

				Flux<Tuple2<String, String>> appToUrl = Flux
					.fromIterable(applicationManifests)
					.map(ApplicationManifest::getName)
					.flatMap(appName -> cfs.urlForApplication(appName, false).map(u -> Tuples.of(appName, u)));

				StepVerifier
					.create(appToUrl)
					.expectNextMatches(tpl -> !tpl.getT2().toLowerCase().contains("https") && tpl.getT2().contains(tpl.getT1()))
					.verifyComplete();
		}


		/*


		@Test
		public void getNameForManifest() {
		}

		@Test
		public void getManifestFor() {
		}

		@Test
		public void getNameForManifest1() {
		}

		@Test
		public void getNameForManifest2() {
		}

		@Test
		public void pushApplicationAndCreateBackingService() {
		}*/

		@SpringBootApplication
		public static class Config {
		}
}