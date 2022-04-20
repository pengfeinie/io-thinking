package com.niepengfei.io.version1;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 *
 *
 * <p>
 * 服务端启动类, 一个线程处理所有的连接请求,
 * 测试步骤：
 * 1. 启动服务， 第一个客户端连接上来，看控制台输出，得知哪些方法阻塞，然后发送数据。 （如果不发送数据，后续的客户端也连接不上来）
 * 2. 第二个客户端连接上来，看控制台输出，得知哪些方法阻塞，然后发送数据。
 * 3. 此时第一个客户端再发送数据，服务端已经接收不到了，因为服务端丢失了与第一个客户端的连接。
 * </p>
 *
 * @author Jack
 * @version 1.0.0
 * @since 1/3/2020
 */
public class ServerApp {

    private static final byte[] buffer = new byte[1024];

    @SuppressWarnings("resource")
	public static void main(String[] args) throws Exception{
        ServerSocket serverSocket = new ServerSocket(8080);
        while (true) {
            System.out.println("waiting connecting....");
            Socket cs = serverSocket.accept();
            System.out.println("connected, client port : " + cs.getPort());
            System.out.println("waiting client data....");
            InputStream inputStream = cs.getInputStream();
            int read = inputStream.read(buffer);
            if (read > 0) {
                System.out.println("client data connected");
                String content = new String(buffer, Charset.defaultCharset());
                System.out.println(content);
            }
        }
    }
}
