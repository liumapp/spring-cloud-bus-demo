package com.liumapp.demo.bus.service.demoapia.controller;

import com.liumapp.demo.bus.engine.job.component.DetailJob;
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
    private DetailJob detailJob;

    @RequestMapping("/")
    public String begin () throws InterruptedException {
        detailJob.run();
        return "success";
    }

}
