package com.niepengfei.nio.version2;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 有什么问题？ 每循环都要去循环clientSocketList,存在用户空间与内核空间的反复切换. 假设clientSocketList非常大，性能极差。
 *
 * @author Jack
 * @version 1.0.0
 * @since 1/11/2020
 */
public class ServerNioApp {

    public static void main(String[] args) throws Exception{


        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8080));
        serverSocketChannel.configureBlocking(false);

        List<SocketChannel> clientSocketList = new ArrayList<>(16);

        while (true){
            TimeUnit.SECONDS.sleep(1);
            SocketChannel clientSocket = serverSocketChannel.accept();
            if (clientSocket == null) {
                List<SocketChannel> clientList = new ArrayList<>(clientSocketList);
                for (SocketChannel socketChannel : clientList ) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int read = socketChannel.read(byteBuffer);
                    byteBuffer.flip();
                    if (read > 0) {
                        System.out.println(new String(byteBuffer.array()));
                    } else if (read < 0){
                        clientSocketList.remove(socketChannel);
                    }
                }
            } else {
                System.out.println("新客户端 ：" + clientSocket.getRemoteAddress());
                clientSocket.configureBlocking(false);
                clientSocketList.add(clientSocket);
                List<SocketChannel> clientList = new ArrayList<>(clientSocketList);
                for (SocketChannel socketChannel : clientList ) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int read = socketChannel.read(byteBuffer);
                    byteBuffer.flip();
                    if (read > 0) {
                        System.out.println(new String(byteBuffer.array()));
                    } else if (read < 0){
                        clientSocketList.remove(socketChannel);
                    }
                }
            }
        }
    }
}




