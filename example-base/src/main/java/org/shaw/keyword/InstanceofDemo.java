package org.shaw.keyword;

import org.shaw.keyword.impl.Animal;
import org.shaw.keyword.impl.Cat;

/**
 * @create: 2017-11-23
 * @description:
 */
public class InstanceofDemo {
    public static void main(String[] args) {
        Animal animal = new Animal();
        Cat cat = new Cat();
        System.out.println(animal instanceof Animal);
    }
}
