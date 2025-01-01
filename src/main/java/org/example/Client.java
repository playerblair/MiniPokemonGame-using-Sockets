package org.example;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

public class Client {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Scanner scanner;
    private String trainerName;

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        scanner = new Scanner(System.in);
    }

    public void start() throws ClassNotFoundException{
        try {
            Message serverMessage;
            while ((serverMessage = (Message) in.readObject()) != null) {
                handleServerMessage(serverMessage);
            }
        } catch (IOException e) {
            System.out.println("Connection Error: " + e.getMessage());
        }
    }

    private void handleServerMessage(Message message) throws IOException {
        switch (message.getCommand()) {
            case "CREATE_TRAINER":
                createTrainer();
                break;
            case "DRAFT_POKEMON":
                draftPokemon((String[]) message.getObject());
                break;
            case "SEND_OUT_POKEMON":
                sendOutPokemon((Map<Integer, String>) message.getObject());
                break;
            case "DISPLAY_INFO":
                displayInfo((String) message.getObject());
                break;
            case "PLAYER_TURN":
                playerTurn((Map<String, String>) message.getObject());
                break;
            case "MESSAGE":
                displayMessage((String) message.getObject());
                break;
            // Add other message handlers
        }
    }

    private void createTrainer() throws IOException{
        displayBanner("=");
        System.out.println("Trainer Setup");
        displayBanner("-");
        System.out.print("Enter name: ");
        String name = this.scanner.nextLine().trim();
        while (name.isEmpty()) {
            System.out.print("Enter name: ");
            name = this.scanner.nextLine().trim();
        }
        trainerName = name;
        out.writeObject(name);
    }

    private void draftPokemon(String[] pokemonNames) throws IOException{
        displayBanner("=");
        System.out.println("Trainer: " + trainerName);
        displayBanner("-");
        int columns = 3;
        for (int i = 0; i < pokemonNames.length; i++) {
            System.out.printf("%d. %-10s%s", i+1, pokemonNames[i], ((i + 1) % columns == 0) ? "\n" : "\t");
        }
        System.out.println();
        List<String> pokemon = new ArrayList<>();
        while (pokemon.size() < 3) {
            int index;
            CHOOSE_POKEMON: while (true) {
                displayBanner("-");
                System.out.print("Choose pokemon to add to party: ");
                String choice = this.scanner.nextLine();
                Optional<Integer> choiceAsInteger = validateInteger(choice);
                if (choiceAsInteger.isPresent()) {
                    index = choiceAsInteger.get() - 1;
                    if (index >=0 && index < pokemonNames.length) {
                        break CHOOSE_POKEMON;
                    }
                }
            }
            displayBanner("-");
            System.out.println(trainerName + " added " + pokemonNames[index] + " to their party.");
            pokemon.add(pokemonNames[index]);
        }
        out.writeObject(pokemon.toArray(new String[0]));
    }

    public void sendOutPokemon(Map<Integer, String> availablePokemon) throws IOException {
        displayBanner("=");
        System.out.println("Trainer: " + trainerName);
        displayBanner("-");
        for (int key: availablePokemon.keySet()) {
            System.out.println(key + ". " + availablePokemon.get(key));
        }
        SEND_OUT_POKEMON: while (true) {
            System.out.print("Choose pokemon to send out to battle: ");
            String choice = this.scanner.nextLine();
            Optional<Integer> choiceAsInteger = validateInteger(choice);
            if (choiceAsInteger.isPresent()) {
                int index = choiceAsInteger.get();
                if (availablePokemon.containsKey(index)) {
                    System.out.println(trainerName + " sends out " + availablePokemon.get(index));
                    out.writeObject(index);
                    break SEND_OUT_POKEMON;
                }
            }
        }
    }

    public void displayInfo(String info) {
        displayBanner("=");
        System.out.println(info);
    }

    public void playerTurn(Map<String, String> moveSet) throws IOException {
        displayBanner("-");
        System.out.println("Trainer: " + trainerName);
        displayBanner("-");
        displayOptions(moveSet);
        PROCESS_COMMANDS: while (true) {
            displayBanner("-");
            System.out.print("Select option (" + String.join(", ", moveSet.keySet()) + ", s, c) : ");
            String input = this.scanner.nextLine().trim().toLowerCase();
            if (moveSet.containsKey(input) || input.equals("s") || input.equals("c")) {
                out.writeObject(input);
                break PROCESS_COMMANDS;
            }
        }
    }

    public void displayMessage(String message) {
        displayBanner("-");
        System.out.println(message);
    }

    private void displayOptions(Map<String, String> moveSet) {
        System.out.println("Available moves:");
        for (String key: moveSet.keySet()) {
            System.out.println("\t" + key + ". " + moveSet.get(key));
        }
        System.out.println("Additional options:");
        System.out.println("    s. Show stats");
        System.out.println("    c. Change pokemon");
    }

    private void displayBanner(String str) {
        System.out.println(str.repeat(50));
    }

    private Optional<Integer> validateInteger(String input) {
        try {
            return Optional.of(Integer.parseInt(input));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static void main(String[] args) {
        System.out.println("Connecting to server at " + "localhost" + ":" + 5000);
        try {
            Client client = new Client("localhost", 5000);
            System.out.println("Connected to server successfully!");
            client.start();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
