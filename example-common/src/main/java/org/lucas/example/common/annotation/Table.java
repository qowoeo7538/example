package org.lucas.example.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//阐述了某个被标注的类的子类继承该注解
@Inherited

//注解作用范围
@Target({ElementType.TYPE})

// 注解保留范围
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {

    //如果只有一个成员属性，必须声明为value();
    String value();

}

