package org.lucas.example.foundation.design.pattern.observer.method2;

import org.lucas.example.foundation.design.pattern.observer.method2.support.ConcreteObserver1;
import org.lucas.example.foundation.design.pattern.observer.method2.support.ConcreteSubject1;
import org.lucas.example.foundation.design.pattern.observer.method2.support.Observer1;

/**
 * @create: 2017-11-07
 * @description:
 */
public class ObserverPatternDemo {
    public static void main(String[] args) {
        ConcreteSubject1 concreteSubject1 = new ConcreteSubject1();
        Observer1 observer1 = new ConcreteObserver1();

        concreteSubject1.attach(observer1);
        concreteSubject1.change("new state");
        System.out.println("具体主题角色类:" + concreteSubject1.getState());
    }
}
