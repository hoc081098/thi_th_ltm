package com.hoc;

import com.hoc.model.Buffer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
  public static void main(String[] args) throws IOException {
    final ServerSocket serverSocket = new ServerSocket(6000);
    final ExecutorService executorService = Executors.newFixedThreadPool(10);

    while (true) {
      System.out.println("Running...");
      final Socket socket = serverSocket.accept();
      executorService.submit(() -> {
        try {
          handleSocket(socket);
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    }
  }

  private static void handleSocket(Socket socket) throws Exception {
    final Buffer buffer = Process.readBytes
      .andThen(Process.readExcel)
      .andThen(Process.distribution)
      .andThen(Process.writeExcelFile)
      .apply(socket);
    new ObjectOutputStream(socket.getOutputStream()).writeObject(buffer);
    System.out.println("Done");
  }
}

