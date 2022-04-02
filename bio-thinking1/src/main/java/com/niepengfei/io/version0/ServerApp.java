package com.niepengfei.io.version0;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

/**

 * @author Jack
 * @version 1.0.0
 * @since 1/3/2020
 */
public class ServerApp {

    private static final byte[] buffer = new byte[1024];

    public static void main(String[] args) throws Exception{
        ServerSocket serverSocket = new ServerSocket(8080);
        while (true) {
            System.out.println("等待连接....");
            //在这里会发生阻塞,即放弃CPU
            Socket clientSocket = serverSocket.accept();
            System.out.println("已经建立连接, client port : " + clientSocket.getPort());
            System.out.println("等待客户端的数据....");
            InputStream inputStream = clientSocket.getInputStream();
            int read = inputStream.read(buffer);
            if (read > 0) {
                System.out.println("数据已经被接收");
                String content = new String(buffer, Charset.defaultCharset());
                System.out.println(content);
            }
        }
    }
}
