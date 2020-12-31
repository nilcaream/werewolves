package com.nilcaream.werewolves;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class Player {

    public static final String CENTER_PREFIX = "Center";

    public enum State {
        WORKING, READY_TO_PLAY, READY_TO_VOTE, DONE
    }

    private String id;
    private String name;
    private final List<Role> roles = new ArrayList<>();
    private final Map<String, Role> knownPlayers = new HashMap<>();
    private final List<String> actions = new ArrayList<>();
    private String vote;
    private State state = State.WORKING;

    public Player() {
    }

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getVote() {
        return vote;
    }

    public void setVote(String vote) {
        this.vote = vote;
    }

    public Map<String, Role> getKnownPlayers() {
        return knownPlayers;
    }

    public List<String> getActions() {
        return actions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Role> getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Player.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("name='" + name + "'")
                .add("roles=" + roles)
                .add("knownPlayers=" + knownPlayers)
                .add("actions=" + actions)
                .add("vote='" + vote + "'")
                .add("state=" + state)
                .toString();
    }
}
