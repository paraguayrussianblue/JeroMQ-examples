package com.msp;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.SocketType;

import java.util.Random;

public class pub_sub_and_pull_push_client {
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            
            ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
            subscriber.connect("tcp://localhost:5557");
            subscriber.subscribe("".getBytes(ZMQ.CHARSET)); 

            
            ZMQ.Socket publisher = context.createSocket(SocketType.PUSH);
            publisher.connect("tcp://localhost:5558");

            
            ZMQ.Poller poller = context.createPoller(1);
            poller.register(subscriber, ZMQ.Poller.POLLIN);

            Random rand = new Random(System.currentTimeMillis());

            while (!Thread.currentThread().isInterrupted()) {
                // Poll with 100ms timeout
                int polled = poller.poll(100);
                if (polled > 0 && poller.pollin(0)) {
                    byte[] msgBytes = subscriber.recv(0);
                    String msg = new String(msgBytes, ZMQ.CHARSET);
                    System.out.println("I: received message " + msg);
                } else {
                    int r = rand.nextInt(100) + 1; // 1 to 100
                    if (r < 10) {
                        String out = Integer.toString(r);
                        publisher.send(out.getBytes(ZMQ.CHARSET), 0);
                        System.out.println("I: sending message " + r);
                    }
                }
            }
        }
    }
}
