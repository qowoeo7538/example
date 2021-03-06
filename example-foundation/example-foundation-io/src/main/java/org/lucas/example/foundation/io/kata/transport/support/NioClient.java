package org.lucas.example.foundation.io.kata.transport.support;

import org.lucas.component.common.util.StreamHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

/**
 * @create: 2017-12-15
 * @description:
 */
public class NioClient {

    private String hostname;

    private int port;

    public NioClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public void sendFile(String srcFile) {
        try (FileInputStream fileInputStream = new FileInputStream(srcFile)) {
            final SocketChannel socketChannel = SocketChannel.open();
            FileChannel fileChannel = fileInputStream.getChannel();

            InetSocketAddress listener = new InetSocketAddress(hostname, port);
            socketChannel.connect(listener);
            while (true) {
                StreamHelper.channelRead(fileChannel, (buffer) -> {
                    try {
                        socketChannel.write(buffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                StreamHelper.channelRead(socketChannel, (buffer) -> {
                    System.out.print((char) buffer.get());
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
