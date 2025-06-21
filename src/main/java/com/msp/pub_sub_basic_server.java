package com.msp;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.SocketType;

import java.util.concurrent.ThreadLocalRandom;

public class pub_sub_basic_server {
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            System.out.println("Publishing updates at weather server...");
            ZMQ.Socket publisher = context.createSocket(SocketType.PUB);
            publisher.bind("tcp://*:5556");
            
            while (!Thread.currentThread().isInterrupted()) {
                int zipcode = ThreadLocalRandom.current().nextInt(1, 100000);
                int temperature = ThreadLocalRandom.current().nextInt(-80, 135);
                int relhumidity = ThreadLocalRandom.current().nextInt(10, 60);

                String update = zipcode + " " + temperature + " " + relhumidity;
                publisher.send(update.getBytes(ZMQ.CHARSET), 0);

            }
        }
    }
}
