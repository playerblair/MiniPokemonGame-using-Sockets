package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Server {

    private ServerSocket serverSocket;
    private final PokemonFactory factory;
    private final Random random;
    private final Map<String, Map<String, String[]>> typeChart;

    public Server(int port) {
        this.factory = new PokemonFactory();
        this.random = new Random();
        this.typeChart = new HashMap<>();
        try {
            serverSocket = new ServerSocket(port);
            // populateTypeChart();
        } catch (IOException e) {
            System.out.println("Server Error: " + e.getMessage());
        }
    }

    public void start() {
        System.out.println("Server started. Waiting for trainers...");
        try {
            Socket trainer1Socket = serverSocket.accept();
            System.out.println("Trainer 1 connected");
            Socket trainer2Socket = serverSocket.accept();
            System.out.println("Trainer 2 connected");

            ClientHandler trainer1Handler = new ClientHandler(trainer1Socket);
            ClientHandler trainer2Handler = new ClientHandler(trainer2Socket);

            GameSession gameSession = new GameSession(trainer1Handler, trainer2Handler, factory, random, typeChart);
            gameSession.start();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server(5000);
        server.start();
    }
}
