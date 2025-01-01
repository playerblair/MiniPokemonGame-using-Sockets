package org.example;

import java.io.IOException;
import java.util.*;

public class GameSession {
    private final ClientHandler client1;
    private final ClientHandler client2;
    private final PokemonFactory factory;
    private final Random random;
    private final Map<String, Map<String, String[]>> typeChart;

    public GameSession(ClientHandler client1, ClientHandler client2, PokemonFactory factory, Random random, Map<String, Map<String, String[]>> typeChart) {
        this.client1 = client1;
        this.client2 = client2;
        this.factory = factory;
        this.random = random;
        this.typeChart = new HashMap<>();
    }

    public void start() {
        try {
            populateTypeChart();
            setupTrainer(client1);
            setupTrainer(client2);

            draftPokemon(client1);
            draftPokemon(client2);

            sendOutPokemon(client1);
            sendOutPokemon(client2);

            MAIN_LOOP: while (true) {
                if (!bothTrainersHaveAvailablePokemon()) {
                    break MAIN_LOOP;
                }
                displayInfo();
                handleTurn(client1);
                handleTurn(client2);
                resolveRound();
            }
            announceWinner();
        } catch (Exception e) {
            System.out.println("Game Session Error: " + e.getMessage());
        }
    }

    public void setupTrainer(ClientHandler clientHandler) throws IOException, ClassNotFoundException {
        clientHandler.sendMessage(new Message("CREATE_TRAINER"));
        clientHandler.setTrainer(new Trainer((String) clientHandler.receiveMessage()));
    }

    public void draftPokemon(ClientHandler clientHandler) throws IOException, ClassNotFoundException {
        Message message = new Message("DRAFT_POKEMON", factory.getPokemonNames());
        clientHandler.sendMessage(message);
        String[] pokemon = (String[]) clientHandler.receiveMessage();
        addPokemon(clientHandler, pokemon);
    }

    public void sendOutPokemon(ClientHandler clientHandler) throws IOException, ClassNotFoundException {
        Trainer trainer = clientHandler.getTrainer();
        int index;
        clientHandler.sendMessage(new Message("SEND_OUT_POKEMON", trainer.getAvailablePokemonAsHashMap()));
        index = (int) clientHandler.receiveMessage();
        trainer.selectPokemon(index - 1);
    }

    public void displayInfo() throws IOException {
        String info = "Trainer: " + client1.getTrainer().getName() + " - Current Pokemon: " + client1.getTrainer().getPokemon().toString() +
                "\nvs.\n" +
                "Trainer: " + client2.getTrainer().getName() + " - Current Pokemon: " + client2.getTrainer().getPokemon().toString();
        Message message = new Message("DISPLAY_INFO", info);
        client1.sendMessage(message);
        client2.sendMessage(message);
    }

    public void handleTurn(ClientHandler clientHandler) throws IOException, ClassNotFoundException {
        Trainer trainer = clientHandler.getTrainer();
        Pokemon pokemon = trainer.getPokemon();
        TURN: while (true) {
            clientHandler.sendMessage(new Message("PLAYER_TURN", pokemon.getMoveSetAsHashMap()));
            String option = (String) clientHandler.receiveMessage();
            switch (option) {
                case "s":
                    showPokemonStats(clientHandler);
                    continue TURN;
                case "c":
                    sendOutPokemon(clientHandler);
                    break TURN;
                default:
                    trainer.setMove(pokemon.selectMove(Integer.parseInt(option) - 1));
                    break TURN;
            }
        }
    }

    public void resolveRound() throws IOException, ClassNotFoundException {
        Trainer trainer1 = client1.getTrainer();
        Pokemon trainer1Pokemon = trainer1.getPokemon();

        Trainer trainer2 = client2.getTrainer();
        Pokemon trainer2Pokemon = trainer2.getPokemon();

        if (trainer1Pokemon.getSPD().getValue() > trainer2Pokemon.getSPD().getValue()) {
            executeMove(trainer1, trainer1Pokemon, client1, trainer2Pokemon, client2);
            executeMove(trainer2, trainer2Pokemon, client2, trainer1Pokemon, client1);
        } else if (trainer2Pokemon.getSPD().getValue() > trainer1Pokemon.getSPD().getValue()) {
            executeMove(trainer2, trainer2Pokemon, client2, trainer1Pokemon, client1);
            executeMove(trainer1, trainer1Pokemon, client1, trainer2Pokemon, client2);
        } else {
            if (random.nextBoolean()) {
                executeMove(trainer1, trainer1Pokemon, client1, trainer2Pokemon, client2);
                executeMove(trainer2, trainer2Pokemon, client2, trainer1Pokemon, client1);
            } else {
                executeMove(trainer2, trainer2Pokemon, client2, trainer1Pokemon, client1);
                executeMove(trainer1, trainer1Pokemon, client1, trainer2Pokemon, client2);
            }
        }
    }

    public void showPokemonStats(ClientHandler clientHandler) throws IOException {
        clientHandler.sendMessage(new Message("MESSAGE", clientHandler.getTrainer().getPokemon().getStatsAsString()));
    }

    private void populateTypeChart() {
        String[] categories = new String[]{"strong", "weak", "null"};
        for (String category: categories) {
            String fileName = "type-" + category + ".csv";
            try (Scanner reader = new Scanner(GameSession.class.getClassLoader().getResourceAsStream(fileName))) {
                Map<String, String[]> map = new HashMap<>();
                while (reader.hasNext()) {
                    String line = reader.nextLine();
                    String[] parts = line.split(",");
                    String[] array;
                    if (parts.length > 1) {
                        array = Arrays.copyOfRange(parts, 1, parts.length);
                    } else {
                        array = new String[0];
                    }
                    map.put(parts[0], array);
                }
                typeChart.put(category, map);
            } catch (Exception e) {
                System.out.println("Error (UserInterface):" + e.getMessage());
            }
        }
    }

    private void addPokemon(ClientHandler clientHandler, String[] pokemon) {
        Trainer trainer = clientHandler.getTrainer();
        for (String pkmn: pokemon) {
            trainer.addPokemon(this.factory.createPokemon(pkmn));
        }
    }

    private boolean bothTrainersHaveAvailablePokemon() {
        return client1.getTrainer().hasAvailablePokemon() && client2.getTrainer().hasAvailablePokemon();
    }

    private void executeMove(Trainer trainer, Pokemon pokemon, ClientHandler clientHandler, Pokemon oppPokemon, ClientHandler oppClientHandler) throws IOException, ClassNotFoundException {
        if (trainer.getMove() != null) {
            double modifier = battle(trainer.getMove(), pokemon, oppPokemon);
            if (oppPokemon.isFainted()) {
                pokemonHasFainted(oppClientHandler, oppPokemon.getName());
            }
            clientHandler.sendMessage(new Message("MESSAGE", pokemon.getName() + " used " + trainer.getMove().getName() + "."));
            oppClientHandler.sendMessage(new Message("MESSAGE", "ENEMY " + pokemon.getName() + " used " + trainer.getMove().getName() + "."));

            if (modifier != 1.0) {
                Message effectivenessMessage = new Message("MESSAGE", getEffectiveness(modifier));
                clientHandler.sendMessage(effectivenessMessage);
                oppClientHandler.sendMessage(effectivenessMessage);
            }

        }
    }

    private double battle(Move move, Pokemon pokemon, Pokemon oppPokemon) {
        String category = move.getCategory();
        double modifier = 1.0;
        if (category.equals("status")) {
            Stat stat;
            if (move.getTarget().equals("self")) {
                stat = pokemon.getStat(move.getTargetStat());
            } else {
                stat = oppPokemon.getStat(move.getTargetStat());
            }

            if (move.getModifier().equals("-1")) {
                stat.lower();
            } else {
                stat.raise();
            }
        } else {
            double att = pokemon.getStat(category.equals("physical") ? "att" : "spatt").getValue();
            double def = oppPokemon.getStat(category.equals("physical") ? "def" : "spdef").getValue();
            double base = calcDamage(move.getPower(), att, def);
            modifier = getTypeModifier(move.getType(), oppPokemon.getTypes());
            double randomModifier = 0.85 + (random.nextDouble() * 0.15);
            double damage = base * modifier * randomModifier;
            oppPokemon.takeDamage(damage);
        }
        move.use();
        return modifier;
    }

    private double calcDamage(int power, double att, double def) {
        double part1 = 1.0 * (2 * Pokemon.getLevel() + 10) / 250;
        double part2 = att / def;
        return part1 * part2 * power;
    }

    private double getTypeModifier(String moveType, String[] pokemonTypes) {
        double modifier = 1.0;
        for (String type: pokemonTypes) {
            if (Arrays.asList(typeChart.get("strong").get(moveType)).contains(type)) {
                modifier *= 2.0;
                continue;
            }
            if (Arrays.asList(typeChart.get("weak").get(moveType)).contains(type)) {
                modifier *= 0.5;
                continue;
            }
            if (Arrays.asList(typeChart.get("null").get(moveType)).contains(type)) {
                modifier *= 0.0;
            }
        }
        return modifier;
    }

    private String getEffectiveness(double modifier) {
        if (modifier <= 0) {
            return "It had no effect.";
        } else if (modifier < 1.0) {
            return "It wasn't very effective.";
        } else if (modifier >= 2.0) {
            return "It was super effective.";
        }
        return null;
    }

    private void pokemonHasFainted(ClientHandler clientHandler, String pokemon) throws IOException, ClassNotFoundException {
        clientHandler.sendMessage(new Message("MESSAGE", pokemon + " has fainted."));
        if (clientHandler.getTrainer().hasAvailablePokemon()) {
            sendOutPokemon(clientHandler);
        } else {
            clientHandler.getTrainer().pokemonHasFainted();
        }
    }

    private void announceWinner() throws IOException {
        if (client1.getTrainer().hasAvailablePokemon()) {
            client1.sendMessage(new Message("MESSAGE", "You have won the battle!"));
            client2.sendMessage(new Message("MESSAGE", "You have lost the battle!"));
        } else {
            client2.sendMessage(new Message("MESSAGE", "You have won the battle!"));
            client1.sendMessage(new Message("MESSAGE", "You have lost the battle!"));
        }
    }

    public static void main(String[] args) {

    }
}
