package cn.mango3y.service;

import cn.mango3y.spring.MangoApplicationContext;

public class Test {
    public static void main(String[] args) {
        //启动Spring工厂
        MangoApplicationContext context = new MangoApplicationContext(MyAppConfig.class);

        //获取多个userService的bean看看单例/多例情况下有何不同
        //System.out.println(context.getBean("userService"));
        //System.out.println(context.getBean("userService"));

        //测试@MyComponent下的类默认的bean名是否有效
        /*System.out.println(context.getBean("orderService"));
        System.out.println(context.getBean("orderService"));*/

        //测试AOP动态代理
        //jdk动态代理不好的地方：代理类只是实现了UserService相同的接口
        //不是继承UserService本身，所以最后从spring中获得UserService对象是只能转为UserInterface
        UserInterface bean = (UserInterface) context.getBean("userService");
        bean.test();


    }
}
