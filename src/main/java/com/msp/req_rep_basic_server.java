package com.msp;
import org.zeromq.ZContext; 
import org.zeromq.ZMQ;
import org.zeromq.SocketType;

public class lec_05_prg_01_req_rep_basic_server {
    public static void main(String[] args) {
        // 1) ZContext 생성
        try (ZContext context = new ZContext()) {
            // 2) REP 타입 소켓 생성
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind("tcp://*:5555");

            while (!Thread.currentThread().isInterrupted()) {
                // 3) 클라이언트 요청 대기
                byte[] requestBytes = socket.recv(0);
                if (requestBytes != null) {
                    String request = new String(requestBytes, ZMQ.CHARSET);
                    System.out.println("Received request: " + request);
                }

                // 4) 작업 시뮬레이션 (1초 대기)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // 5) 클라이언트에 응답 전송
                socket.send("World".getBytes(ZMQ.CHARSET), 0);
            }

            // 소켓과 컨텍스트는 try-with-resources 블록 종료 시 자동으로 닫힘
        }
    }
}
