package com.liumapp.demo.bus.service.demoapib.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author liumapp
 * @file IndexController.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/21/18
 */
@RestController
@RequestMapping("/hello")
public class IndexController {

    @Value("${custom.activeInfo}")
    private String activeInfo;

    @RequestMapping("/")
    public String index () {
        return "hello , this is demo api b and active info is : " + activeInfo;
    }

}
