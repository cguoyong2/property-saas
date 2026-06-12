package com.yongquan.propertysaas.job.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JobProperties.class)
public class JobConfiguration {
}
