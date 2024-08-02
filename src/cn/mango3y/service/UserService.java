package cn.mango3y.service;

import cn.mango3y.spring.*;

@MyComponent("userService")
//@MyScope("prototype")
public class UserService implements MyBeanNameAware, MyInitializingBean, UserInterface {

    @MyAutowired
    private OrderService orderService;

    private String beanName;

    private String whatever;

    //让spring容器创建该bean时顺便完成别的事，比如给某个属性赋值
    @Override
    public void afterPropertiesSet() {
        this.whatever = "whatever";
        System.out.println("初始化方法...");
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void test(){
        System.out.println(orderService);
    }
}
