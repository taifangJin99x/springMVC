package com.service.impl;

import com.annotation.MyAutowired;
import com.annotation.MyService;
import com.service.TestService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyService
public class TestServiceImpl implements TestService {

    @Override
    public void test1(HttpServletResponse response,String name) {
        try {
            response.getWriter().println("my name is " + name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void test2(HttpServletResponse response) {
        try {
            response.getWriter().println("doTest2 method success!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
