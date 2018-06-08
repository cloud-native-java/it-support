package cnj;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@SpringBootTest(classes = CloudFoundryServiceTest.Config.class)
@RunWith(SpringRunner.class)
public class CloudFoundryServiceTest {


		@Autowired
		private CloudFoundryService cf;

		@Before
		public void before() throws Exception {


		}

		@Test
		public void findRoutesForApplication() {
		}

		@Test
		public void pushApplication() {
		}

		@Test
		public void createBackingService() {
		}

		@Test
		public void urlForApplication() {
		}

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
		}

		@SpringBootApplication
		public static class Config {
		}
}