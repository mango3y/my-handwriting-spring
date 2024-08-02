package cn.mango3y.spring;

/*bean定义类*/
public class MyBeanDefinition {
    private Class type;
    private String scope;

    public MyBeanDefinition() {
    }

    public MyBeanDefinition(Class type, String scope) {
        this.type = type;
        this.scope = scope;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
