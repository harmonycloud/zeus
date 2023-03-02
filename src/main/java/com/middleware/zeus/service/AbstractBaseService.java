package com.middleware.zeus.service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.util.SpringContextUtils;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
public abstract class AbstractBaseService {

    /**
     * 内部类保证线程安全的单例
     */
    private static class Singleton {
        private static final Map<String, Object> OPERATORS = SpringContextUtils.getBeansWithAnnotation(Operator.class);
    }

    protected <T, R> T getOperator(Class<T> funClass, Class<R> baseClass, Object... types) {
        for (Map.Entry<String, Object> entry : Singleton.OPERATORS.entrySet()) {
            try {
                if (baseClass.isAssignableFrom(entry.getValue().getClass()) && types.length == 0
                    || (funClass.isAssignableFrom(entry.getValue().getClass()) && (boolean)baseClass
                        .getMethod("support",
                            entry.getValue().getClass().getAnnotation(Operator.class).paramTypes4One())
                        .invoke(entry.getValue(), types))) {
                    return (T)entry.getValue();
                }
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                // TODO process exception
                e.printStackTrace();
            }
        }
        return null;
    }

    protected <T, R> List<T> getOperators(Class<T> funClass, Class<R> baseClass, Object... types) {
        List<T> operators = new ArrayList<>();
        for (Map.Entry<String, Object> entry : Singleton.OPERATORS.entrySet()) {
            try {
                if (baseClass.isAssignableFrom(entry.getValue().getClass()) &&
                        types.length == 0 || (funClass.isAssignableFrom(entry.getValue().getClass()) &&
                        (boolean) baseClass.getMethod("support", entry.getValue().getClass().getAnnotation(Operator.class).paramTypes4Many()).invoke(entry.getValue(), types))) {
                    operators.add((T) entry.getValue());
                }
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                // TODO process exception
                e.printStackTrace();
            }
        }
        return operators;
    }

}
