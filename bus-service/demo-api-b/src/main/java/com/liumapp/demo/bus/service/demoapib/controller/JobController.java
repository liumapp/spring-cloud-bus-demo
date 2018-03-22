package com.liumapp.demo.bus.service.demoapib.controller;

import com.alibaba.fastjson.JSON;
import com.liumapp.demo.bus.engine.job.entity.JobInfo;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author liumapp
 * @file JobController.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/22/18
 */
@RestController
@RequestMapping("job")
public class JobController {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @RequestMapping("/")
    public String beginJob () {
        JobInfo jobInfo = new JobInfo("test-app-from-demo-b" , "dfjifjisdjfiwjfeio");
        amqpTemplate.convertAndSend("long-time-job" , JSON.toJSON(jobInfo));
        return "success";
    }

}
