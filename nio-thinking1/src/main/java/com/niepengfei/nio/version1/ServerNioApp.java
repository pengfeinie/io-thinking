package com.niepengfei.nio.version1;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 *
 * 无法处理多个客户端
 *
 * @author Jack
 * @version 1.0.0
 * @since 1/11/2020
 */
public class ServerNioApp {

    private static ByteBuffer buffer = ByteBuffer.allocate(1024);

    public static void main(String[] args) throws Exception{
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8080));
        serverSocketChannel.configureBlocking(false);
        while (true){
            TimeUnit.SECONDS.sleep(1);
            SocketChannel clientSocket = serverSocketChannel.accept();
            if (clientSocket == null) {
                System.out.println("null........");
            } else {
                System.out.println("client addrsss ：" + clientSocket.getRemoteAddress());
                clientSocket.configureBlocking(false);
                clientSocket.read(buffer);
                String content = new String(buffer.array(), Charset.defaultCharset());
                System.out.println(content);
            }
        }
    }
}




