package com.msp;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.SocketType;

public class pub_sub_and_pull_push_server_v2 {
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket publisher = context.createSocket(SocketType.PUB);
            publisher.bind("tcp://*:5557");

            
            ZMQ.Socket collector = context.createSocket(SocketType.PULL);
            collector.bind("tcp://*:5558");

            while (!Thread.currentThread().isInterrupted()) {
                byte[] message = collector.recv(0);
                if (message == null) {
                    continue;
                }
                String text = new String(message, ZMQ.CHARSET);
                System.out.println("server: publishing update => " + text);
                publisher.send(message, 0);
            }
        }
    }
}
