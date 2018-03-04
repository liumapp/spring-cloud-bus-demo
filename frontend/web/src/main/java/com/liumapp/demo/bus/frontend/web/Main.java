package com.liumapp.demo.bus.frontend.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by liumapp on 10/9/17.
 * E-mail:liumapp.com@gmail.com
 * home-page:http://www.liumapp.com
 */
@SpringBootApplication(scanBasePackages = {"com.liumapp.demo.bus"})
public class Main {

    public static void main (String[] args) {

        SpringApplication.run(Main.class , args);

    }

}
