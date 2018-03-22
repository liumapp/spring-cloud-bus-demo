package com.liumapp.demo.bus.service.independent;

import com.liumapp.demo.bus.engine.job.LongTimeJob;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * @author liumapp
 * @file Main.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/22/18
 */
@SpringBootApplication
@Import({LongTimeJob.class})
public class IndependentWorker {

    public static void main(String[] args) {
        SpringApplication.run(IndependentWorker.class , args);
    }

}
