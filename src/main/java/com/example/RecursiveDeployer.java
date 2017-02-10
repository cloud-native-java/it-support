package com.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.CloudFoundryOperations;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

class RecursiveDeployer {

	private static Log log = LogFactory.getLog(RecursiveDeployer.class);

	private final CloudFoundryOperations cloudFoundryOperations;

	RecursiveDeployer(CloudFoundryOperations cloudFoundryOperations) {
		this.cloudFoundryOperations = cloudFoundryOperations;
	}

	public void deploy(File root) throws Exception {
		log.info("deploying everything under " + root.getAbsolutePath());
		Files.walkFileTree(root.toPath(), new DeployingFileVisitor(
				this.cloudFoundryOperations));
	}

	public static class DeployingFileVisitor extends SimpleFileVisitor<Path> {

		private Log log = LogFactory.getLog(getClass());
		private final CloudFoundryOperations cloudFoundryOperations;

		public DeployingFileVisitor(CloudFoundryOperations cloudFoundryOperations) {
			this.cloudFoundryOperations = cloudFoundryOperations;
		}

		@Override
		public FileVisitResult visitFile(Path file,
		                                 BasicFileAttributes attr) {
			if (attr.isSymbolicLink()) {
				log.debug(String.format("Symbolic link: %s ", file));
			} else if (attr.isRegularFile()) {
				log.debug(String.format("Regular file: %s ", file));
			} else {
				log.debug(String.format("Other: %s ", file));
			}
			log.debug("(" + attr.size() + "bytes)");
			return CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir,
		                                          IOException exc) {
			log.debug(String.format("Directory: %s%n", dir));
			return CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file,
		                                       IOException exception) {
			log.error("something went wrong visiting file "
					+ file.getFileName() + ".", exception);
			return CONTINUE;
		}
	}
}