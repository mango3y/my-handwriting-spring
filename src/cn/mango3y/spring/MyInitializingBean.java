package cn.mango3y.spring;

//让spring容器创建该bean时顺便完成别的事
public interface MyInitializingBean {
    public void afterPropertiesSet();
}
