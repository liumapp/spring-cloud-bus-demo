package com.liumapp.demo.bus.engine.job.component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.liumapp.demo.bus.engine.job.entity.JobInfo;
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

    public void run (String info) throws InterruptedException {
        JobInfo jobInfo = JSON.parseObject(info , JobInfo.class);
        jobInfo.setAppId(jobInfo.getAppId() + "-updated");
        Job job = new Job();
        job.setJobInfo(jobInfo);
        Thread jobThread = new Thread(job);
        jobThread.start();
        Thread.sleep(10 * 1000);
        logger.info("job is done");
    }

}
