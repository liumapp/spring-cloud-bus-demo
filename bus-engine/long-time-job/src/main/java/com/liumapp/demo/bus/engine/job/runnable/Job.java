package com.liumapp.demo.bus.engine.job.runnable;

import com.liumapp.demo.bus.engine.job.entity.JobInfo;
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

    private JobInfo jobInfo;

    private Logger logger = LoggerFactory.getLogger(Job.class);

    @Override
    public void run() {
        try {
            for (int i = 0 ; i < 10 ; i++) {
                int time = (int) (Math.random() * 1000);
                Thread.sleep(time);
                logger.info("run = " + Thread.currentThread().getName() + " and jobInfo is : " + jobInfo.toString());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setJobInfo(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }
}
