package com.hoc;

import com.hoc.model.Buffer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Client {
  public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
    final Socket socket = new Socket("localhost", 6000);

    if (socket.isConnected()) {
      final ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());

      System.out.println("Start read");
      final byte[] bytes = Files.readAllBytes(Paths.get("bảng-giám-thị (3).xlsx"));
      outputStream.writeObject(new Buffer(bytes));
      System.out.println("Done: " + bytes.length);


      final ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
      final Buffer buffer = (Buffer) inputStream.readObject();
      Files.write(Paths.get("output.xlsx"), buffer.bytes);
      System.out.println("Client done");
    }
  }
}
