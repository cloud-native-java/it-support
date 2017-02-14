package cnj;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CloudFoundryServiceTest.ClientConfiguration.class)
public class CloudFoundryServiceTest {

	private String svc = "p-mysql", plan = "100mb",
			instance = "cf-it-greetings-service-mysql";

	private File manifestFile;
	private File jarFile;
	@Autowired
	private CloudFoundryService cloudFoundryService;
	@Autowired
	private CloudFoundryOperations cf;
	private Log log = LogFactory.getLog(getClass());

	public CloudFoundryServiceTest() throws IOException {
		this.manifestFile = File.createTempFile("simple-manifest", ".yml");
		this.jarFile = new File(this.manifestFile.getParentFile(), "hi.jar");
	}

	private static void sync(File og, File dst) throws Throwable {
		Assert.assertTrue("the input file should exist.", og.exists());
		Assert.assertTrue("the output file should *not* exist.",
				!dst.exists() || dst.delete());
		Files.copy(og.toPath(), dst.toPath());
	}

	@Before
	public void setup() throws Throwable {
		sync(new ClassPathResource("/sample-app/manifest.yml").getFile(), this.manifestFile);
		sync(new ClassPathResource("/sample-app/hi.jar").getFile(), jarFile);
		String txt = Files.readAllLines(this.manifestFile.toPath()).stream()
				.collect(Collectors.joining(System.lineSeparator()));
		this.log.debug("contents of manifest to read? " + txt);
		this.cloudFoundryService.createServiceIfMissing(svc, plan, instance);
	}

	@After
	public void clean() throws Throwable {

		this.cloudFoundryService.destroyApplicationUsingManifest(this.manifestFile);
	}

	@Test
	public void testPushingApplicationWithManifest() throws Exception {
		try {

			Map<File, ApplicationManifest> applicationManifestMap = this.cloudFoundryService
					.applicationManifestFrom(this.manifestFile);

			String appName = applicationManifestMap.values().iterator().next().getName();

			log.info("attempting to push the application " + appName
					+ " as a user-provided-service (also called " + appName + ")");

			Assert.assertTrue(!this.cloudFoundryService.applicationExists(appName));
			Assert.assertTrue(!this.cloudFoundryService.serviceExists(appName));

			applicationManifestMap
					.forEach(this.cloudFoundryService::pushApplicationAndCreateUserDefinedServiceUsingManifest);

			Assert.assertTrue(this.cloudFoundryService.applicationExists(appName));
			Assert.assertTrue(this.cloudFoundryService.serviceExists(appName));
		}
		catch (Exception e) {
			log.error("error when trying to push application using manifest file "
					+ manifestFile.getAbsolutePath(), e);
			throw new RuntimeException("oops! " + e);
		}
	}

	@Test
	public void applicationManifestFrom() throws Exception {

		Map<File, ApplicationManifest> manifestMap = this.cloudFoundryService
				.applicationManifestFrom(manifestFile);
		manifestMap.forEach((jarFile, manifest) -> assertTrue(
				"the .jar file to push must exist.", jarFile.exists()));

		if (manifestMap.size() == 0) {
			Assert.fail();
		}
	}

	@Test
	public void urlForApplication() throws Exception {
		Flux<ApplicationSummary> summaryFlux = this.cf.applications().list()
				.filter(ad -> ad.getUrls().size() > 0);
		ApplicationSummary applicationSummary = summaryFlux.blockFirst();
		String urlForApplication = this.cloudFoundryService.urlForApplication(
				applicationSummary.getName()).toLowerCase();
		boolean matches = applicationSummary.getUrls().stream().map(String::toLowerCase)
				.filter(urlForApplication::contains).count() >= 1;
		assertTrue("one of the returned URLs should match.", matches);
	}

	@Test
	public void ensureServiceIsAvailable() throws Exception {
		assertTrue("the " + instance + " service should exist.",
				this.cloudFoundryService.serviceExists(instance));

	}

	@SpringBootApplication
	public static class ClientConfiguration {
	}
}