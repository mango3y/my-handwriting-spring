package cn.mango3y.spring;

/*spring提供一种回调机制，若想知道某类的bean名称，该机制可起作用*/
public interface MyBeanNameAware {

    public void setBeanName(String beanName);


}
