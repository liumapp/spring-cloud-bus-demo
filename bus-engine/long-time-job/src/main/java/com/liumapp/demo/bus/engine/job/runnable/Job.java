package com.liumapp.demo.bus.engine.job.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author liumapp
 * @file Job.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/22/18
 */
public class Job implements Runnable {

    private Logger logger = LoggerFactory.getLogger(Job.class);

    @Override
    public void run() {
        try {
            for (int i = 0 ; i < 10 ; i++) {
                int time = (int) (Math.random() * 1000);
                Thread.sleep(time);
                logger.info("run = " + Thread.currentThread().getName());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
