package com.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;

@SpringBootApplication
public class DeployerApplication {

	private final Log log = LogFactory.getLog(getClass());

	@Bean
	RecursiveDeployer deployer(CloudFoundryOperations cfOps) {
		return new RecursiveDeployer(cfOps);
	}


	@Bean
	ApplicationRunner run(RecursiveDeployer rd) {
		return args -> rd.deploy(new File("/Users/jlong/book/CNJ/code/integration"));
	}

	public static void main(String[] args) {
		SpringApplication.run(DeployerApplication.class, args);
	}
}

