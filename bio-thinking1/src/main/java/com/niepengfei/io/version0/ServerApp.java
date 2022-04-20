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
            System.out.println("waiting connecting....");
            Socket cs = serverSocket.accept();
            System.out.println("connected,client port : " + cs.getPort());
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
