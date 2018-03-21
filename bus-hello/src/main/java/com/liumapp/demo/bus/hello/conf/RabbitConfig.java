package com.liumapp.demo.bus.hello.conf;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author liumapp
 * @file RabbitConfig.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/21/18
 */
@Configuration
public class RabbitConfig {

    @Bean
    public Queue helloQueue () {
        return new Queue("hello");
    }

}
