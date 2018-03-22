package com.liumapp.demo.bus.service.demoapib.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author liumapp
 * @file RabbitConfig.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/22/18
 */
@Configuration
public class RabbitConfig {

    @Bean
    public Queue LongTimeJobQueue () {
        return new Queue("long-time-job");
    }

}
