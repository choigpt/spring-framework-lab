package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Component;
import com.ll.framework.ioc.annotations.Repository;
import com.ll.framework.ioc.annotations.Service;
import com.ll.standard.util.Ut;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ApplicationContext {
    private final String basePackage;
    private final Map<String, Object> beans = new HashMap<>();

    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;
    }

    public void init() {
        Reflections reflections = new Reflections(basePackage);

        Set<Class<?>> classes = new HashSet<>();

        classes.addAll(reflections.getTypesAnnotatedWith(Component.class));
        classes.addAll(reflections.getTypesAnnotatedWith(Service.class));
        classes.addAll(reflections.getTypesAnnotatedWith(Repository.class));

        for (Class<?> cls : classes) {
            if (isBeanClass(cls)) {
                createBean(cls);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T genBean(String beanName) {
        return (T) beans.get(beanName);
    }

    private Object createBean(Class<?> cls) {
        if (!isBeanClass(cls)) {
            throw new IllegalArgumentException(cls.getName() + " is not a bean class");
        }

        String beanName = Ut.str.lcfirst(cls.getSimpleName());

        if (beans.containsKey(beanName)) {
            return beans.get(beanName);
        }

        try {
            Constructor<?> constructor = getConstructor(cls);
            constructor.setAccessible(true);

            Object[] args = new Object[constructor.getParameterCount()];
            Class<?>[] parameterTypes = constructor.getParameterTypes();

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> depType = parameterTypes[i];

                Object dep = findBeanByType(depType);

                if (dep == null) {
                    dep = createBean(depType);
                }

                args[i] = dep;
            }

            Object bean = constructor.newInstance(args);
            beans.put(beanName, bean);

            return bean;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isBeanClass(Class<?> cls) {
        return !cls.isAnnotation()
                && (cls.isAnnotationPresent(Component.class)
                || cls.isAnnotationPresent(Service.class)
                || cls.isAnnotationPresent(Repository.class));
    }

    private Constructor<?> getConstructor(Class<?> cls) {
        Constructor<?>[] constructors = cls.getDeclaredConstructors();

        if (constructors.length == 1) {
            return constructors[0];
        }

        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() > 0) {
                return constructor;
            }
        }

        return constructors[0];
    }

    private Object findBeanByType(Class<?> type) {
        for (Object bean : beans.values()) {
            if (type.isAssignableFrom(bean.getClass())) {
                return bean;
            }
        }

        return null;
    }
}
