package org.example;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Scanner scanner;
    private Trainer trainer;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        scanner = new Scanner(System.in);
    }

    public void sendMessage(Object object) throws IOException{
        out.writeObject(object);
        out.flush();
    }

    public Object receiveMessage() throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    public void setTrainer(Trainer trainer) {
        this.trainer = trainer;
    }

    public Trainer getTrainer() {
        return trainer;
    }

    public void createTrainer() {
        displayBanner("=");
        System.out.println("Trainer Setup");
        displayBanner("-");
        System.out.print("Enter name: ");
        String name = this.scanner.nextLine().trim();
        while (name.isEmpty()) {
            System.out.print("Enter name: ");
            name = this.scanner.nextLine().trim();
        }
        trainer = new Trainer(name);
        System.out.print("Press any key to continue...");
    }

    public void displayBanner(String str) {
        System.out.println(str.repeat(50));
    }

    public static void main(String[] args) {

    }
}
