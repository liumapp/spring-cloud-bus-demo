package com.liumapp.demo.bus.hello.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author liumapp
 * @file Sender.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/21/18
 */
@Component
public class Sender {

    private static Logger logger = LoggerFactory.getLogger(Sender.class);

    @Autowired
    private AmqpTemplate amqpTemplate;

    public void send () {
        String context = "hello " + new Date();
        logger.info("Sender send msg : " + context);
        this.amqpTemplate.convertAndSend("hello" , context);
    }

}
