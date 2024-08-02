package cn.mango3y.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) //运行时生效
@Target(ElementType.TYPE) //表示只能写在类上
public @interface MyComponentScan {

    //指定扫描路径
    String value() default "";

}
