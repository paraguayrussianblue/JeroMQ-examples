package com.msp;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.SocketType;

import java.util.Random;

public class pub_sub_and_pull_push_client_v2 {
    public static void main(String[] args) {

        String clientID = args[0];

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
                poller.poll(100);
                if (poller.pollin(0)) {
                    byte[] msgBytes = subscriber.recv(0);
                    String message = new String(msgBytes, ZMQ.CHARSET);
                    System.out.println(clientID + ": receive status => " + message);
                } else {
                    int r = rand.nextInt(100) + 1; // 1 to 100
                    if (r < 10) {
                        try { Thread.sleep(1000); } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        String msg = "(" + clientID + ":ON)";
                        publisher.send(msg.getBytes(ZMQ.CHARSET), 0);
                        System.out.println(clientID + ": send status - activated");
                    } else if (r > 90) {
                        try { Thread.sleep(1000); } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        String msg = "(" + clientID + ":OFF)";
                        publisher.send(msg.getBytes(ZMQ.CHARSET), 0);
                        System.out.println(clientID + ": send status - deactivated");
                    }
                }
            }
        }
    }
}
