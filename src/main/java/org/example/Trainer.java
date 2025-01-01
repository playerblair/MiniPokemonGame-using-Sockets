package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trainer {
    private final String name;
    private final List<Pokemon> pokemon;
    private Pokemon currentPokemon;
    private Move currentMove;

    public Trainer(String name) {
        this.name = name;
        this.pokemon = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Pokemon> getPokemonList() {
        return pokemon;
    }

    public Pokemon getPokemon() {
        return currentPokemon;
    }

    public Move getMove() {
        return currentMove;
    }

    public boolean setMove(Move move) {
        if (move == null) {
            return false;
        }
        this.currentMove = move;
        return true;
    }

    public boolean selectPokemon(int index) {
        if (index < 0 || index >= pokemon.size()) {
            return false;
        }
        if (pokemon.get(index).isFainted()) {
            return false;
        }
        this.currentPokemon = pokemon.get(index);
        this.currentMove = null;
        return true;
    }

    public void addPokemon(Pokemon newPkmn) {
        this.pokemon.add(newPkmn);
    }

    public Map<Integer, String> getAvailablePokemonAsHashMap() {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < pokemon.size(); i++) {
            if (!pokemon.get(i).isFainted()) {
                map.put(i + 1, pokemon.get(i).toString());
            }
        }

        return map;
    }

    public boolean hasAvailablePokemon() {
        for (Pokemon pkmn: pokemon) {
            if (!pkmn.isFainted()) {
                return true;
            }
        }
        return false;
    }

    public void pokemonHasFainted() {
        currentPokemon = null;
        currentMove = null;
    }
}
