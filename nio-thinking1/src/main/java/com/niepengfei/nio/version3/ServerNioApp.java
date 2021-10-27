package com.niepengfei.nio.version3;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * IO多路复用
 *
 * 多路复用器只是告诉你IO的状态，读还是需要你自己去读的
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
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true){
            TimeUnit.SECONDS.sleep(1);
            while (selector.select(0)>0){
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel)key.channel();
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        client.register(selector,SelectionKey.OP_READ,byteBuffer);
                        System.out.println("新客户端: " + client.getRemoteAddress());
                        iterator.remove();
                    } else if(key.isReadable()){
                        SocketChannel channel = (SocketChannel)key.channel();
                        ByteBuffer attachment = (ByteBuffer)key.attachment();
                        channel.read(attachment);
                        iterator.remove();
                    }
                }
            }
        }
    }
}




