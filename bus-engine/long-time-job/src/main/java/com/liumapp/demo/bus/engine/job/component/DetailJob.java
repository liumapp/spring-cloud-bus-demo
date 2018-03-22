package com.liumapp.demo.bus.engine.job.component;

import com.liumapp.demo.bus.engine.job.runnable.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author liumapp
 * @file DetailJob.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/22/18
 */
@Component
public class DetailJob {

    private Logger logger = LoggerFactory.getLogger(DetailJob.class);

    public void run () throws InterruptedException {
        Job job = new Job();
        Thread jobThread = new Thread(job);
        jobThread.start();
        Thread.sleep(10 * 1000);
        logger.info("job is done");
    }

}
