package org.lucas.example.foundation.jvm.demo.stack.support;

/**
 * @create: 2017-11-09
 * @description:
 */
public class BStackContent extends AStackContent {
    public void BATest() {
        System.out.println("======B.A开始======");
        super.ACTest();
        System.out.println("======B.A结束======");
    }

}
