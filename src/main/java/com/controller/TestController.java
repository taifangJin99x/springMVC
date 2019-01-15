package com.controller;

import com.annotation.*;
import com.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyController
@MyRequestMapping("/test")
public class TestController {
    @MyAutowired()
    private TestService testService;
    @MyRequestMapping("/doTest")
    public void test1(HttpServletRequest request, HttpServletResponse response,@MyRequestParam("name") String name){
        testService.test1(response,name);

    }

    @MyRequestMapping("/doTests")
    public void test2(HttpServletRequest request, HttpServletResponse response){
        testService.test2(response);
    }
}
