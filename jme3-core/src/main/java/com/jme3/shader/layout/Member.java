/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shader.layout;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Alexander Kasigkeit
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Member {

    public static final int OPEN_ARRAY = 0;
    public static final String AUTOMAPPING = "automapping";

    public BlockVarType type() default BlockVarType.AutoDetect;
    
    public String maps() default AUTOMAPPING;
 
}
