package com.liumapp.demo.bus.hello.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author liumapp
 * @file Receiver.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/21/18
 */
@Component
@RabbitListener(queues = "hello")
public class Receiver {

    private static Logger logger = LoggerFactory.getLogger(Receiver.class);

    @RabbitHandler
    public void process (String hello) {
        logger.info("Receiver get msg from queues named hello : " + hello);
    }

}
