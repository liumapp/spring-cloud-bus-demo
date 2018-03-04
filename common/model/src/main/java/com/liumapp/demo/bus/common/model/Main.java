package com.liumapp.demo.bus.common.model;

import com.liumapp.demo.bus.common.model.entity.Guest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by liumapp on 10/9/17.
 * E-mail:liumapp.com@gmail.com
 * home-page:http://www.liumapp.com
 */
@Configuration
public class Main {

    @Bean
    public Guest guest() {
        return new Guest("honorific guest" , "boy");
    }

}
