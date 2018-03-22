package com.liumapp.demo.bus.service.independent.customer;

import com.liumapp.demo.bus.engine.job.component.DetailJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author liumapp
 * @file LongTimeJob.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/22/18
 */
@Component
@RabbitListener(queues = "long-time-job")
public class LongTimeJob {

    @Autowired
    private DetailJob detailJob;

    private static Logger logger = LoggerFactory.getLogger(LongTimeJob.class);

    @RabbitHandler
    public void process (String hello) {
        logger.info("Receiver get msg from queues named long-time-job : " + hello);
        try {
            detailJob.run(hello);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}