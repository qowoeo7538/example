package org.lucas.example.foundation.io.demo.character;

import java.io.*;

/**
 * Created by joy on 17-2-8.
 */
public class BrAndBwOrPwDemo {
    public static void main(String[] args) throws Exception {
        brAndPw("/home/joy/桌面/a.properties", "/home/joy/桌面/dsfsdfsdf.properties");
    }

    /**
     * 文本文件拷贝
     *
     * @param srcData
     * @param copyData
     * @throws Exception
     */
    public static void brAndBw(String srcData, String copyData) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(srcData)));
             BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(copyData)))) {
            String line;
            //一次读一行，并不能识别换行
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                bufferedWriter.write(line);
                bufferedWriter.newLine(); //换行操作
                bufferedWriter.flush();
            }
        }
    }

    /**
     * 文本文件拷贝(一次读一行)
     *
     * @param srcData
     * @param copyData
     * @throws Exception
     */
    public static void brAndPw(String srcData, String copyData) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(srcData)))) {

            //PrintWriter pw1 = new PrintWriter(outputStream,boolean autoFlush);  自动刷新
            PrintWriter printWriter = new PrintWriter(copyData);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                //换行操作
                printWriter.println(line);
                printWriter.flush();
            }
            printWriter.flush();
        }
    }
}
