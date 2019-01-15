package com.service;

import com.annotation.MyService;

import javax.servlet.http.HttpServletResponse;

@MyService()
public interface TestService {

    void test1(HttpServletResponse httpServletResponse,String name);

    void test2(HttpServletResponse response);
}
