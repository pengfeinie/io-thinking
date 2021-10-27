package com.niepengfei.io.version1;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * <p>
 * 客户端1
 * </p>
 *
 * @author Jack
 * @version 1.0.0
 * @since 1/3/2020
 */
public class ClientApp {

    public static void main(String[] args) throws Exception{
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(8080));
        OutputStream outputStream = socket.getOutputStream();
        Scanner scan = new Scanner(System.in);
        String content = scan.nextLine();
        outputStream.write(content.getBytes());
    }
}
