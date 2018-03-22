package com.liumapp.demo.bus.engine.job.entity;

/**
 * @author liumapp
 * @file JobInfo.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/22/18
 */
public class JobInfo {

    private String appId;

    private String appSecret;

    public JobInfo() {
    }

    public JobInfo(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    @Override
    public String toString() {
        return "JobInfo{" +
                "appId='" + appId + '\'' +
                ", appSecret='" + appSecret + '\'' +
                '}';
    }
}
