/*
 * Copyright (C) Alexander Kasigkeit - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Alexander Kasigkeit <alexander.kasigkeit@web.de>, 2020
 */
package com.jme3.renderer.opengl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alexander Kasigkeit <alexander.kasigkeit@web.de>
 */
public class GLCounter implements InvocationHandler {

    protected Map<String, Integer> counter;
    protected Object obj;
    protected GL gl;
    protected List<String> sortList = new ArrayList<>();
    protected StringBuilder sb = new StringBuilder();

    protected Comparator<String> comp = new Comparator<String>() {
        @Override
        public int compare(String t, String t1) {
            return counter.get(t1) - counter.get(t);
        }
    };

    private GLCounter(Map<String, Integer> counter, GL gl, Object obj) {
        this.counter = counter;
        this.gl = gl;
        this.obj = obj;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.equals("resetStats")) {
            sortList.clear();
            sortList.addAll(counter.keySet());
            Collections.sort(sortList, comp);

            int totalCalls = 0;
            sb.setLength(0);
            sb.append("--------- GL CALLS ---------\n");
            for (int i = 0; i < sortList.size(); i++) {
                String name = sortList.get(i);
                int pad = 40 - name.length();
                sb.append(name);
                for (int j = 0; j < pad; j++) {
                    sb.append(".");
                }
                Integer c = counter.get(name);
                sb.append(c).append("\n");
                totalCalls += c;
            }
            sb.append("TOTAL:..................................").append(totalCalls);
            System.out.println(sb.toString());
            
            counter.clear();
        }
        int called = counter.getOrDefault(methodName, 0) + 1;
        counter.put(methodName, called);

        Object result = method.invoke(obj, args);

        return result;
    }

    public static Object createProxy(Map<String, Integer> counter, GL gl, Object obj, Class<?>... implementedInterfaces) {
        return Proxy.newProxyInstance(
                GLDebug.class.getClassLoader(),
                implementedInterfaces,
                new GLCounter(counter, gl, obj)
        );
    }

}
