package com.msp;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.SocketType;
import org.zeromq.ZMsg;

public class dealer_router_async_server {
    public static void main(String[] args) {
        int numWorkers = Integer.parseInt(args[0]);
        ServerTask server = new ServerTask(numWorkers);
        server.start();
        try {
            server.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static class ServerTask extends Thread {
        private final int numWorkers;

        ServerTask(int numWorkers) {
            this.numWorkers = numWorkers;
        }

        @Override
        public void run() {
            try (ZContext context = new ZContext()) {
                ZMQ.Socket frontend = context.createSocket(SocketType.ROUTER);
                frontend.bind("tcp://*:5570");

                ZMQ.Socket backend = context.createSocket(SocketType.DEALER);
                backend.bind("inproc://backend");

                for (int i = 0; i < numWorkers; i++) {
                    new ServerWorker(context, i).start();
                }

                ZMQ.proxy(frontend, backend, null);

                frontend.close();
                backend.close();
            }
        }
    }

    static class ServerWorker extends Thread {
        private final ZContext context;
        private final int id;

        ServerWorker(ZContext context, int id) {
            this.context = context;
            this.id = id;
        }

        @Override
        public void run() {
            ZMQ.Socket worker = context.createSocket(SocketType.DEALER);
            worker.connect("inproc://backend");
            System.out.println("Worker#" + id + " started");
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ZMsg msg = ZMsg.recvMsg(worker);
                    if (msg == null || msg.size() < 2) {
                        continue;
                    }
                    byte[] identity = msg.pop().getData();
                    byte[] message = msg.pop().getData();
                    String text = new String(message, ZMQ.CHARSET);
                    String ident = new String(identity, ZMQ.CHARSET);
                    System.out.println("Worker#" + id + " received " + text + " from " + ident);

                    ZMsg reply = new ZMsg();
                    reply.add(identity);
                    reply.add(message);
                    reply.send(worker);
                }
            } finally {
                worker.close();
            }
        }
    }
}
