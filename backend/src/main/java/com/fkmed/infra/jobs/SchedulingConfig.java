package com.fkmed.infra.jobs;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables {@code @Scheduled} jobs (e.g. the audit retention sweep). */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
