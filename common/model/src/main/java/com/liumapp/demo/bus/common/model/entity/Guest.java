package com.liumapp.demo.bus.common.model.entity;

import org.springframework.stereotype.Component;

/**
 * Created by liumapp on 10/9/17.
 * E-mail:liumapp.com@gmail.com
 * home-page:http://www.liumapp.com
 */
@Component
public class Guest {

    private String name;

    private String sex;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Guest(String name, String sex) {
        this.name = name;
        this.sex = sex;
    }

    public Guest() {
    }
}
