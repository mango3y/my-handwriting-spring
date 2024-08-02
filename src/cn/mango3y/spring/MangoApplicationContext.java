package cn.mango3y.spring;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class MangoApplicationContext {

    private Class configClass;

    //用于保存beanName与BeanDefinition对象的映射
    private ConcurrentHashMap<String, MyBeanDefinition> myBeanDefinitionConcurrentHashMap = new ConcurrentHashMap<>();

    //用于保存beanName与Bean对象的映射
    private ConcurrentHashMap<String, Object> singletonBeanMap = new ConcurrentHashMap<>();

    private ArrayList<MyBeanPostProcessor> myBeanPostProcessorList = new ArrayList<>();

    public MangoApplicationContext(Class configClass) {
        this.configClass = configClass;

        //启动spring容器后，第一件事情--扫描
        //与配置类有关
        //判断配置类上是否@MyComponentScan注解，有则获取该注解中的扫描路径
        if(configClass.isAnnotationPresent(MyComponentScan.class)){
            MyComponentScan myComponentScanAnnotation = (MyComponentScan)configClass.getAnnotation(MyComponentScan.class);
            String scanPath = myComponentScanAnnotation.value(); //获取其中扫描路径 cn.mango3y.service
            //真正扫描的是java文件编译后的class文件，也就是编译源文件后生成的out文件夹中的对于包
            //D:\Projects\my-handwriting-spring\out\production\my-handwriting-spring\cn\mango3y\service
            //如何cn.mango3y.service找到以上文件夹呢？
            scanPath = scanPath.replace(".", "/"); //cn/mango3y/service
            ClassLoader classLoader = MangoApplicationContext.class.getClassLoader();
            URL resource = classLoader.getResource(scanPath);//获取相对路径cn/mango3y/service对应资源(文件夹)

            File file = new File(resource.getFile());
            //System.out.println(file); //D:\Projects\my-handwriting-spring\out\production\my-handwriting-spring\cn\mango3y\service
            if(file.isDirectory()){
                //拿到该文件夹中的所有文件
                File[] files = file.listFiles();
                //遍历每个文件，筛选出class文件，再利用反射判断其上有无@MyComponent注解
                for (File f : files) {
                    String absolutePath = f.getAbsolutePath();
                    if(absolutePath.endsWith(".class")){
                        //加载类要给类全限定名 包名.类名
                        String className = absolutePath.substring(absolutePath.indexOf("cn"), absolutePath.indexOf(".class"));
                        className = className.replace("\\", ".");
                        //System.out.println(className);

                        try {
                            Class<?> clazz = classLoader.loadClass(className);
                            //如果该类被@MyComponent注解则为bean
                            if (clazz.isAnnotationPresent(MyComponent.class)) {
                                //判断该类是否由MyBeanPostProcessor接口派生的
                                if(MyBeanPostProcessor.class.isAssignableFrom(clazz)){
                                    //如果该类被@MyComponent注解且实现了MyBeanPostProcessor接口
                                    //则说明它是后处理器（与AOP有关）
                                    //生成MyBeanPostProcessor加入到myBeanPostProcessorList列表中
                                    MyBeanPostProcessor instance = (MyBeanPostProcessor)clazz.newInstance();
                                    myBeanPostProcessorList.add(instance);
                                }

                                //获取该bean的名字，即@MyComponent的value值
                                MyComponent myComponent = clazz.getAnnotation(MyComponent.class);
                                String beanName = myComponent.value();
                                //如果为空字符串，指定默认值
                                if("".equals(beanName)){
                                    //利用一个工具类，获得类名首字母小写的字符串
                                    beanName = Introspector.decapitalize(clazz.getSimpleName());
                                }

                                //则该类为bean，考虑单例(默认)/多例bean情况
                                //单例bean是工厂加载时创建，多例bean是在获取时创建
                                //如何解决？在Spring中通过BeanDefinition
                                MyBeanDefinition myBeanDefinition = new MyBeanDefinition();
                                myBeanDefinition.setType(clazz);
                                if(clazz.isAnnotationPresent(MyScope.class)){
                                    //获取@Scope的value值设置为myBeanDefinition属性
                                    MyScope scopeAnnotation = clazz.getAnnotation(MyScope.class);
                                    String scopeValue = scopeAnnotation.value();
                                    myBeanDefinition.setScope(scopeValue);
                                } else {
                                    //若无则默认单例
                                    myBeanDefinition.setScope("singleton");
                                }
                                //保存myBeanDefinition到map中，以便后续调用
                                myBeanDefinitionConcurrentHashMap.put(beanName, myBeanDefinition);
                            }
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        //根据myBeanDefinitionConcurrentHashMap，根据其中信息创建单例bean对象
        for(String beanName : myBeanDefinitionConcurrentHashMap.keySet()){
            MyBeanDefinition myBeanDefinition = myBeanDefinitionConcurrentHashMap.get(beanName);
            if("singleton".equals(myBeanDefinition.getScope())){
                Object bean = createBean(beanName, myBeanDefinition);
                singletonBeanMap.put(beanName, bean);
            }
        }
    }

    //利用反射创建bean
    private Object createBean(String beanName, MyBeanDefinition myBeanDefinition) {
        Class clazz = myBeanDefinition.getType();
        try {
            Object instance = clazz.getConstructor().newInstance(); //前提是该bean有无参构造

            //依赖注入
            //给其中被@Autowired注解的属性注入值
            Field[] fields = clazz.getDeclaredFields();
            for(Field field : fields){
                if (field.isAnnotationPresent(MyAutowired.class)) {
                    field.setAccessible(true); //毕竟属性一般都是private所以要设置可访问
                    field.set(instance, getBean(field.getName()));//给当前对象instance的当前属性赋值
                }
            }

            //满足回调：若该bean类实现了BeanNameAware接口，则它进行了获取自己bean名的回调
            //还有很多回调，如ApplicationContext回调、BeanFactory回调，实现思路都类似
            //给实现了BeanNameAware接口的类注入它自己的beanName
            if(instance instanceof MyBeanNameAware){
                ((MyBeanNameAware) instance).setBeanName(beanName);
            }

            //遍历所有的BeanPostProcessor，执行其中的postProcessBeforeInitialization方法
            for(MyBeanPostProcessor myBeanPostProcessor : myBeanPostProcessorList){
                myBeanPostProcessor.postProcessBeforeInitialization(beanName, instance);
            }

            //初始化，让spring容器创建bean时完成别的事，可以在bean类中实现MyInitializingBean接口
            //从而定制自己想在工厂创建bean时要做的事
            if(instance instanceof MyInitializingBean){
                ((MyInitializingBean) instance).afterPropertiesSet();
            }

            //初始化前后的工作由BeanPostProcessor完成
            //遍历所有的BeanPostProcessor，执行其中的postProcessBeforeInitialization方法
            //考虑到AOP，spring工厂返回的应该是bean的代理对象而不是bean本身
            //要把bean对象通过动态代理替换的代理对象，要利用BeanPostProcessor
            //且是要在初始化bean后postProcessAfterInitialization中完成
            //还记得动态代理过程吗，要么基于接口要么基于类，这里选择基于接口
            //让UserService和代理类共同实现UserInterface
            //然后在BeanPostProcessor中处理
            for(MyBeanPostProcessor myBeanPostProcessor : myBeanPostProcessorList){
                instance = myBeanPostProcessor.postProcessAfterInitialization(beanName, instance);
            }

            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getBean(String beanName){
        //问题1：如何通过名字获取bean对象？
        //问题2：如何确定是单例还是多例bean？单例去缓冲池拿对象，多例新创建一个对象？
        //以上问题都由myBeanDefinitionConcurrentHashMap解决
        MyBeanDefinition myBeanDefinition = myBeanDefinitionConcurrentHashMap.get(beanName);
        if(myBeanDefinition == null){
            throw new NullPointerException();
        } else {
            String scope = myBeanDefinition.getScope();
            if("singleton".equals(scope)){
                //单例，已经在工厂创建时生成，只用去获取保存起来的即可
                Object bean = singletonBeanMap.get(beanName);
                if(bean == null){ //比如UserService中含有OrderService属性，如果UserService先被工厂生产，但此时OrderService为空，就需要用到这一步
                    bean = createBean(beanName, myBeanDefinition);
                    singletonBeanMap.put(beanName, bean);
                }
                return bean;
            } else {
                //多例，getBean一次新建一个
                return createBean(beanName, myBeanDefinition);
            }
        }
    }
}
