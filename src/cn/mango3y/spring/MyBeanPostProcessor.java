package cn.mango3y.spring;

public interface MyBeanPostProcessor {
    public Object postProcessBeforeInitialization(String beanName, Object bean);
    public Object postProcessAfterInitialization(String beanName, Object bean);

}
