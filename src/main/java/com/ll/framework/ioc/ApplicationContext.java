package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Bean;
import com.ll.framework.ioc.annotations.Configuration;
import com.ll.framework.ioc.annotations.Repository;
import com.ll.framework.ioc.annotations.Service;
import com.ll.standard.util.Ut;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class ApplicationContext {
    private final String basePackage;
    private final Map<String, Object> beans = new HashMap<>();
    private final Map<String, Method> beanMethods = new HashMap<>();
    private final Map<Method, Object> configObjects = new HashMap<>();

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public void init() {
        Reflections reflections = new Reflections(basePackage);

        Set<Class<?>> classes = new HashSet<>();
        classes.addAll(reflections.getTypesAnnotatedWith(Service.class));
        classes.addAll(reflections.getTypesAnnotatedWith(Repository.class));

        for (Class<?> cls : classes) {
            createBean(cls);
        }

        Set<Class<?>> configClasses = reflections.getTypesAnnotatedWith(Configuration.class);

        for (Class<?> configClass : configClasses) {
            Object configObject = createBean(configClass);

            for (Method method : configClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Bean.class)) {
                    beanMethods.put(method.getName(), method);
                    configObjects.put(method, configObject);
                }
            }
        }

        for (String beanName : new ArrayList<>(beanMethods.keySet())) {
            genBean(beanName);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T genBean(String beanName) {
        if (beans.containsKey(beanName)) {
            return (T) beans.get(beanName);
        }

        Method method = beanMethods.get(beanName);

        if (method != null) {
            return (T) createBeanByMethod(method);
        }

        return null;
    }

    private Object createBean(Class<?> cls) {
        String beanName = Ut.str.lcfirst(cls.getSimpleName());

        if (beans.containsKey(beanName)) {
            return beans.get(beanName);
        }

        try {
            Constructor<?> constructor = Arrays.stream(cls.getDeclaredConstructors())
                    .max(Comparator.comparingInt(Constructor::getParameterCount))
                    .orElseThrow();

            constructor.setAccessible(true);

            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                Object dependency = findBeanByType(parameterTypes[i]);

                if (dependency == null) {
                    dependency = createBean(parameterTypes[i]);
                }

                args[i] = dependency;
            }

            Object bean = constructor.newInstance(args);
            beans.put(beanName, bean);

            return bean;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object createBeanByMethod(Method method) {
        String beanName = method.getName();

        if (beans.containsKey(beanName)) {
            return beans.get(beanName);
        }

        try {
            method.setAccessible(true);

            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                args[i] = findBeanByType(parameterTypes[i]);
            }

            Object bean = method.invoke(configObjects.get(method), args);
            beans.put(beanName, bean);

            return bean;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object findBeanByType(Class<?> type) {
        for (Object bean : beans.values()) {
            if (type.isAssignableFrom(bean.getClass())) {
                return bean;
            }
        }

        for (Method method : beanMethods.values()) {
            if (type.isAssignableFrom(method.getReturnType())) {
                return createBeanByMethod(method);
            }
        }

        return null;
    }
}
