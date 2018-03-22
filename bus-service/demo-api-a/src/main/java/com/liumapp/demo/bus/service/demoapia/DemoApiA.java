package com.liumapp.demo.bus.service.demoapia;

import com.liumapp.demo.bus.engine.job.LongTimeJob;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * @author liumapp
 * @file DemoApiA.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/21/18
 */
@EnableDiscoveryClient
@SpringBootApplication
@Import({LongTimeJob.class})
public class DemoApiA {

    public static void main(String[] args) {
        SpringApplication.run(DemoApiA.class , args);
    }

}
