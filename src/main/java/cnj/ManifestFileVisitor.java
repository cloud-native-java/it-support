package cnj;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.CloudFoundryOperations;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

// TODO ideally you could point this to a directory and have
// TODO it deploy everything it found with a {@literal manifest.yml} file in it.

/**
 * @author <a href="mailto:josh@joshlong.com">Josh
 * Long</a>
 */
@Deprecated
class ManifestFileVisitor extends SimpleFileVisitor<Path> {

	private final CloudFoundryOperations cloudFoundryOperations;
	private Log log = LogFactory.getLog(getClass());

	ManifestFileVisitor(CloudFoundryOperations cloudFoundryOperations) {
		this.cloudFoundryOperations = cloudFoundryOperations;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {

		if (attr.isSymbolicLink()) {
			log.debug(String.format("Symbolic link: %s ", file));
		}
		else if (attr.isRegularFile()) {
			log.debug(String.format("Regular file: %s ", file));
		}
		else {
			log.debug(String.format("Other: %s ", file));
		}
		log.debug("(" + attr.size() + "bytes)");

		return CONTINUE;

	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
		log.debug(String.format("Directory: %s%n", dir));
		return CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exception) {
		log.error("something went wrong visiting file " + file.getFileName() + ".", exception);
		return CONTINUE;
	}
}