package com.msp;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.SocketType;
import org.zeromq.ZMQ.Poller;

public class dealer_router_async_client_thread extends Thread {
    private final String clientID;
    private ZContext context;
    private ZMQ.Socket socket;
    private Poller poller;
    private Thread recvThread;

    public dealer_router_async_client_thread(String clientID) {
        this.clientID = clientID;
    }

    private void recvHandler() {
        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(1000);
            if (poller.pollin(0)) {
                byte[] replyBytes = socket.recv(0);
                String reply = new String(replyBytes, ZMQ.CHARSET);
                System.out.println(clientID + " received: " + reply);
            }
        }
    }

    @Override
    public void run() {
        context = new ZContext();
        socket = context.createSocket(SocketType.DEALER);
        socket.setIdentity(clientID.getBytes(ZMQ.CHARSET));
        socket.connect("tcp://localhost:5570");

        System.out.println("Client " + clientID + " started");

        poller = context.createPoller(1);
        poller.register(socket, Poller.POLLIN);

        recvThread = new Thread(this::recvHandler);
        recvThread.setDaemon(true);
        recvThread.start();

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
        }

        context.close();
    }

    public static void main(String[] args) {
        new dealer_router_async_client(args[0]).start();
    }
}
