package com.liumapp.demo.bus.hello;

import com.liumapp.demo.bus.hello.producer.Sender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author liumapp
 * @file HelloTest.java
 * @email liumapp.com@gmail.com
 * @homepage http://www.liumapp.com
 * @date 3/21/18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {Hello.class})
public class HelloTest {

    @Autowired
    private Sender sender;

    /**
     * you can find msg sender send the msg .
     * and msg receiver had received the msg .
     * this will be logged in your console .
     * @throws Exception
     */
    @Test
    public void hello () throws Exception {
        sender.send();
    }

}
