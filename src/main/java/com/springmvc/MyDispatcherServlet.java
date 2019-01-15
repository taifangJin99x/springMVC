package com.springmvc;


import com.annotation.MyAutowired;
import com.annotation.MyController;
import com.annotation.MyRequestMapping;
import com.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MyDispatcherServlet extends HttpServlet {
    private Properties properties = new Properties();
    //通过扫描得到所有的beanName
    private List<String> classNames = new ArrayList();
    //通过反色得到所有的bean
    private Map<String, Object> ioc = new HashMap();
    //把url和方法进行对应
    private Map<String, Method> handlerMapping = new HashMap();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //初始化所有相关关联的类，扫面用户设定的包下面所有的类
        doScanner(properties.getProperty("scanPackage"));
        //拿到扫描到的类，通过反射机制，实例化，并且放到IOC容器中
        doInstance();
        //根据注解注入类
        doAutowired();
        //初始化HandlerMapping（将url和method对应上）
        initHandlerMapping();
    }

    private void doAutowired() {
        if (ioc.isEmpty())
            return;
        for (Map.Entry<String,Object> entity : ioc.entrySet()) {
            Field [] fields = entity.getValue().getClass().getDeclaredFields();
            for (Field field :fields){
                if (!field.isAnnotationPresent(MyAutowired.class))continue;

                MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
                String beanName = myAutowired.value().trim();
                if ("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);

                try {
                    field.set(entity.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //处理请求
            doDispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception{
        if (handlerMapping.isEmpty()){
            return;
        }

        String url = request.getRequestURI();
        String contextPath = request.getContextPath();

        url = url.replace(contextPath, "").replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(url)){
            response.getWriter().write("404 NOT FOUND");

            return;
        }

        Method method = this.handlerMapping.get(url);
        //获取参数
        Class<?>[] parameterTypes = method.getParameterTypes();

        //获取请求的参数
        Map<String, String[]> parameterMap = request.getParameterMap();

        //保存参数值
        Object[] paramValues = new Object[parameterTypes.length];

        //方法的参数列表
        for (int i = 0; i < parameterTypes.length; i++){
            //根据参数名称
            String requestParam = parameterTypes[i].getSimpleName();
            if (requestParam.equals("HttpServletRequest")){
                paramValues[i] = request;
                continue;
            }
            if (requestParam.equals("HttpServletResponse")){
                paramValues[i]=response;
                continue;
            }

            if (requestParam.equals("String")){
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()){
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", "");
                    paramValues[i] = value;
                }
            }
        }
        try{
            String beanName = toLowerFirstWord(method.getDeclaringClass().getName());
            method.invoke(this.ioc.get(beanName), paramValues);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void doLoadConfig(String location){
        File file = new File(location);
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //把wen.xml的contextConfigLocation对应的value值的文件加载到流里面
        //InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("/properties/application.properties");

        try{
            //用Properties文件加载文件里的内容
            properties.load(inputStream);
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if (null != inputStream){
                try{
                    inputStream.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String packageName){
        //把所有的.替换成/
        URL url = this.getClass().getClassLoader().getResource(packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()){
            if (file.isDirectory()){
                doScanner(packageName+"."+file.getName());
            }else {
                String className = packageName+ "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    private void doInstance(){
        if (classNames.isEmpty()){
            return;
        }
        for (String className : classNames){
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)){
                    ioc.put(toLowerFirstWord(clazz.getName()), clazz.newInstance());
                }else if (clazz.isAnnotationPresent(MyService.class)){
                    MyService service = clazz.getAnnotation(MyService.class);
                    String beanName = service.value();
                    if (!"".equals(beanName)){
                        ioc.put(beanName,clazz.newInstance());
                    }
                    Class<?> [] interfaces = clazz.getInterfaces();
                    for (Class <?> i : interfaces){
                        ioc.put(i.getName(),clazz.newInstance());
                    }
                }else {
                    continue;
                }
            }catch (Exception e){
                e.printStackTrace();
                continue;
            }
        }
    }

    private String toLowerFirstWord(String name){
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()){
            return;
        }
        try{
            for (Map.Entry<String, Object> entry : ioc.entrySet()){
                Class<? extends Object> clazz = entry.getValue().getClass();
                if (!clazz.isAnnotationPresent(MyController.class)){
                    continue;
                }

                //拼url时，是controller头的url拼上方法上的url
                String baseUrl = "";
                if (clazz.isAnnotationPresent(MyRequestMapping.class)){
                    MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
                    baseUrl = annotation.value();
                }
                Method[] methods = clazz.getMethods();
                Object obj = clazz.newInstance();
                for (Method method : methods){
                    if (!method.isAnnotationPresent(MyRequestMapping.class)){
                        continue;
                    }
                    MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                    String url = annotation.value();

                    url = (baseUrl + "/" + url).replaceAll("/+", "/");
                    handlerMapping.put(url, method);
//                    controllerMap.put(url, obj);
                    System.out.println(url + "," + method);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
