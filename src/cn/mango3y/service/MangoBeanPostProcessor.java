package cn.mango3y.service;

import cn.mango3y.spring.MyBeanPostProcessor;
import cn.mango3y.spring.MyComponent;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@MyComponent
public class MangoBeanPostProcessor implements MyBeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(String beanName, Object bean) {
        //自己写入想要在bean初始化前做的事
        //可以根据beanName定制针对某个bean的工作
        if("userService".equals(beanName)){
            System.out.println(beanName + "初始化前");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(String beanName, Object bean) {
        //自己写入想要在bean初始化后做的事
        if("userService".equals(beanName)){
            System.out.println(beanName + "初始化后");
        }

        //与aop相关的动态代理
        //为userService的bean创建代理对象
        if(beanName.equals("userService")){

            //由JDK动态代理生成的代理对象
            Object proxyInstance = Proxy.newProxyInstance(MyBeanPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    System.out.println("切面逻辑");
                    return method.invoke(bean, args);
                }
            });
            return proxyInstance;
        }
        return bean;
    }
}
