package com.msp;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.SocketType;

public class dealer_router_async_client extends Thread {
    private final String clientID;

    public dealer_router_async_client(String clientID) {
        this.clientID = clientID;
    }

    @Override
    public void run() {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.DEALER);
            socket.setIdentity(clientID.getBytes(ZMQ.CHARSET));
            socket.connect("tcp://localhost:5570");

            System.out.println("Client " + clientID + " started");

            ZMQ.Poller poller = context.createPoller(1);
            poller.register(socket, ZMQ.Poller.POLLIN);

            int reqs = 0;
            while (!Thread.currentThread().isInterrupted()) {
                reqs++;
                System.out.println("Req #" + reqs + " sent..");
                String req = "request #" + reqs;
                socket.send(req.getBytes(ZMQ.CHARSET), 0);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                poller.poll(1000);
                if (poller.pollin(0)) {
                    byte[] replyBytes = socket.recv(0);
                    String reply = new String(replyBytes, ZMQ.CHARSET);
                    System.out.println(clientID + ": " + reply);
                }
            }
        }
    }

    public static void main(String[] args) {
        new dealer_router_async_client(args[0]).start();
    }
}
