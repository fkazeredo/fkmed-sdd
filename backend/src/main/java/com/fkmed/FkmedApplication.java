package com.fkmed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** FKMed — health plan beneficiary portal. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FkmedApplication {

  public static void main(String[] args) {
    SpringApplication.run(FkmedApplication.class, args);
  }
}
