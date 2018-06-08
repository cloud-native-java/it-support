package cnj;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.*;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
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
import java.util.function.Supplier;

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

		private String applicationName = "cnj-it-support-test-app-with-manifest";


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

		private void testGetNameForManifest(Supplier<String> sup) {
				List<ApplicationManifest> applicationManifests = ApplicationManifestUtils
					.read(this.testAppManifestFile.toPath());

				String appName = applicationManifests
					.stream()
					.map(ApplicationManifest::getName)
					.findFirst()
					.orElseThrow(() -> new AssertionError("couldn't find the manifest name!!"));


				Assert.assertEquals(appName, applicationName);

				String appNameFromService = sup.get();
				Assert.assertEquals(appNameFromService, appName);
		}

		@Test
		public void getNameForManifest_File() {
				testGetNameForManifest(() -> this.cfs.getNameForManifest(this.testAppManifestFile));
		}

		@Test
		public void getNameForManifest_Path() {
				testGetNameForManifest(() -> this.cfs.getNameForManifest(this.testAppManifestFile.toPath()));
		}

		@Test
		public void getNameForManifest_ApplicationManifest() {
				testGetNameForManifest(() -> this.cfs.getNameForManifest(ApplicationManifestUtils.read(this.testAppManifestFile.toPath()).iterator().next()));
		}

		@Test
		public void getManifestFor() {
				ApplicationManifest am = this.cfs.getManifestFor(this.testAppManifestFile.toPath());
				Assert.assertEquals(am.getName(), this.applicationName);
		}

		@Test
		public void pushApplicationAndCreateBackingService() {

				List<ApplicationManifest> applicationManifests = ApplicationManifestUtils
					.read(this.testAppManifestFile.toPath());

				Assert.assertEquals(applicationManifests.size(), 1);

				Flux<Void> deleteExistingApps = Flux
					.fromStream(applicationManifests.stream().map(ApplicationManifest::getName))
					.flatMap(appName -> cfo.services().deleteInstance(DeleteServiceInstanceRequest.builder().name(appName).build()));

				Flux<String> pushApplications = Flux.fromStream(applicationManifests.stream())
					.flatMap(am -> this.cfs.pushApplicationAndCreateBackingService(am));

				Flux<String> deleteAndPushAndCreateBackingService = deleteExistingApps
					.thenMany(pushApplications);

				StepVerifier
					.create(deleteAndPushAndCreateBackingService)
					.expectNextMatches(appName -> applicationManifests.stream().anyMatch(am -> am.getName().equalsIgnoreCase(appName)))
					.verifyComplete();

				String name = applicationManifests.iterator().next().getName();

				StepVerifier
					.create(this.cfo.applications().get(GetApplicationRequest.builder().name(name).build()))
					.expectNextMatches(ad -> ad.getName().equals(name))
					.verifyComplete();

				StepVerifier
					.create(this.cfo.services()
						.getInstance(GetServiceInstanceRequest.builder().name(name).build()))
					.expectNextMatches(si -> si.getName().equals(name))
					.verifyComplete();

		}

		@SpringBootApplication
		public static class Config {
		}
}