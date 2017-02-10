package com.example;

import cnj.CloudFoundryService;
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
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(SpringRunner.class)
@SpringBootTest(
		classes = CloudFoundryServiceTest.ClientConfiguration.class)
public class CloudFoundryServiceTest {

	private File manifestFile;

	@Autowired
	private CloudFoundryService helper;

	@Autowired
	private CloudFoundryOperations cf;

	private Log log = LogFactory.getLog(getClass());

	public CloudFoundryServiceTest() throws IOException {
		this.manifestFile = File.createTempFile("simple-manifest", ".yml");
	}

	@Before
	public void setup() throws Throwable {
		ClassPathResource classPathResource = new ClassPathResource("/sample-app/manifest.yml");
		Assert.assertTrue("the input manifest should exist.", classPathResource.exists());
		Assert.assertTrue("the output manifest should *not* exist.",
				!this.manifestFile.exists() || this.manifestFile.delete());
		Files.copy(classPathResource.getFile().toPath(), this.manifestFile.toPath());
		String txt = Files.readAllLines(this.manifestFile.toPath())
				.stream()
				.collect(Collectors.joining(System.lineSeparator()));
		this.log.debug("contents of manifest to read? " + txt);

	}

	@After
	public void clean() throws Throwable {
		assertTrue("we should clean up after ourselves!",
				this.manifestFile.exists() || this.manifestFile.delete());
	}

	@SpringBootApplication
	public static class ClientConfiguration {

		@Bean
		public CloudFoundryService helper(CloudFoundryOperations cf, RetryTemplate rt) {
			return new CloudFoundryService(cf, rt);
		}
	}

	@Test
	public void testPushingApplicationWithManifest() throws Exception {
		try {
			this.helper.applicationManifestFrom(this.manifestFile)
					.forEach((jar, am) -> this.helper.pushApplicationUsingManifest(jar, am));

		}
		catch (IllegalArgumentException e) {
			log.error("error when trying to push application using manifest file "
					+ manifestFile.getAbsolutePath(), e);
			throw new RuntimeException("oops! " + e);
		}
	}

	@Test
	public void applicationManifestFrom() throws Exception {

		Map<File, ApplicationManifest> manifestMap = this.helper.applicationManifestFrom(manifestFile);
		manifestMap.forEach((jarFile, manifest) -> {
			assertTrue("the .jar file to push must exist.", jarFile.exists());
		});

		if (manifestMap.size() == 0) {
			Assert.fail();
		}
	}

	@Test
	public void urlForApplication() throws Exception {
		Flux<ApplicationSummary> summaryFlux = this.cf.applications()
				.list()
				.filter(ad -> ad.getUrls().size() > 0);
		ApplicationSummary applicationSummary = summaryFlux.blockFirst();
		String urlForApplication = this.helper.urlForApplication(
				applicationSummary.getName())
				.toLowerCase();
		boolean matches =
				applicationSummary.getUrls()
						.stream()
						.map(String::toLowerCase)
						.filter(urlForApplication::contains).count() >= 1;
		assertTrue("one of the returned URLs should match.", matches);
	}


	@Test
	public void ensureServiceIsAvailable() throws Exception {
		String svc = "cleardb", plan = "spark", instance = UUID.randomUUID().toString();
		try {
			this.helper.createMarketplaceServiceIfMissing(
					svc, plan, instance);

			assertTrue("the " + instance + " service should exist.",
					this.helper.marketplaceServiceExists(instance));
		} finally {
			this.helper.destroyMarketplaceService(instance);
			assertFalse("the " + instance + "service should not exist.",
					this.helper.marketplaceServiceExists(instance));
		}
	}
}