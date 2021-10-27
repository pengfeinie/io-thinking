package com.niepengfei.io.version1;

import org.apache.commons.io.IOUtils;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

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

    private static byte[] buffer = new byte[1024];

    public static void main(String[] args) throws Exception{
        //socket() = 6fd
        //bind(6fd,8080)
        //listen(fd)
        ServerSocket serverSocket = new ServerSocket(8080);

        while (true) {
            System.out.println("等待连接....");
            //在这里会发生阻塞, 等待客户端来连
            //accept(6fd) = 7fd
            Socket clientSocket = serverSocket.accept();
            System.out.println("已经建立连接");
            System.out.println("等待客户端的数据....");
            InputStream inputStream = clientSocket.getInputStream();
            //在这里也会发生阻塞, 等待客户端发送数据
            int read = inputStream.read(buffer);
            if (read > 0) {
                System.out.println("数据已经被接收");
                String content = IOUtils.toString(buffer, "UTF-8");
                System.out.println(content);
            }
        }
    }
}
