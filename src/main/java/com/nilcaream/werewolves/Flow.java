package com.nilcaream.werewolves;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Flow {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Set<Game> games = new HashSet<>();
    private Random random = new SecureRandom();

    public Game createGame(String gameId, List<Role> roles) {
        games.removeIf(game -> game.getId().equals(gameId));
        Game game = new Game();
        game.setId(gameId);
        game.getRoles().addAll(roles);
        games.add(game);
        logger.info("Game created: {} - {}", gameId, roles);
        return game;
    }

    public Player createPlayer(String gameId, String playerId, String playerName) {
        Player player = new Player(playerId, playerName);
        getGame(gameId).getPlayers().add(player);
        logger.info("Player created: {} - {} ({})", gameId, playerName, playerId);
        return player;
    }

    public Player deletePlayer(String gameId, String playerId) {
        Game game = getGame(gameId);
        Player player = getPlayer(game, playerId);
        game.getPlayers().remove(player);
        logger.info("Player deleted: {} - {} ({})", gameId, player.getName(), playerId);
        return player;
    }

    public void startGame(String gameId) {
        Game game = getGame(gameId);

        if (game.getRoles().size() - game.getPlayers().size() == 3) {
            game.getPlayers().add(createCenter(0));
            game.getPlayers().add(createCenter(1));
            game.getPlayers().add(createCenter(2));

            List<Role> roles = new ArrayList<>(game.getRoles());
            Collections.shuffle(roles, random);
            List<Player> players = new ArrayList<>(game.getPlayers());
            for (int i = 0; i < players.size(); i++) {
                players.get(i).getRoles().add(roles.get(i));
            }

            logger.info("Game started: {}", gameId);

            players.stream()
                    .sorted(Comparator.comparing(Player::getName))
                    .forEachOrdered(player -> logger.info("Player role selected: {} - {} ({}) as {}", gameId, player.getName(), player.getId(), first(player.getRoles())));
        } else {
            throw new IllegalStateException("Invalid roles or players count");
        }
    }

    private Player createCenter(int index) {
        Player player = new Player(Player.CENTER_PREFIX + index, Player.CENTER_PREFIX + index);
        player.setState(Player.State.READY_TO_PLAY);
        return player;
    }

    public static final class PlayerStatus {
        private Player.State status;
        private Map<String, Role> players;

        public Player.State getStatus() {
            return status;
        }

        public Map<String, Role> getPlayers() {
            return players;
        }
    }

    public PlayerStatus playerStatus(String gameId, String playerId) {
        Game game = getGame(gameId);
        Player player = getPlayer(game, playerId);

        PlayerStatus status = new PlayerStatus();
        if (game.getPlayers().stream().allMatch(p -> p.getState() == Player.State.READY_TO_PLAY)) {
            status.status = Player.State.READY_TO_PLAY;
            status.players = game.getPlayers().stream()
                    .collect(Collectors.toMap(Player::getId, p -> player.getKnownPlayers().getOrDefault(p.getId(), Role.UNKNOWN)));
        } else {
            status.status = Player.State.WORKING;
        }

        return status;
    }

    public synchronized Player.State executeAction(String gameId, String playerId, List<String> actions) {
        Game game = getGame(gameId);
        Player player = getPlayer(game, playerId);

        logger.info("Player action: {} - {} ({}) - {}", gameId, player.getName(), player.getId(), actions);

        if (player.getRoles().get(0) == Role.DOPPELGANGER) {
            Role newRole = game.getPlayers().stream()
                    .filter(p -> p.getId().equals(actions.get(0)))
                    .map(p -> p.getRoles().get(0))
                    .findFirst()
                    .orElseThrow();
            player.getRoles().add(newRole);
        } else {
            player.getActions().clear();
            player.getActions().addAll(actions);
            player.setState(Player.State.READY_TO_PLAY);
        }

        if (game.getPlayers().stream().allMatch(p -> p.getState() == Player.State.READY_TO_PLAY)) {
            executeActions(game);
            return Player.State.READY_TO_PLAY;
        } else {
            return Player.State.WORKING;
        }
    }

    private void executeActions(Game game) {
        executeAction(game, Role.DOPPELGANGER);
        executeAction(game, Role.WEREWOLF);
        executeAction(game, Role.MINION);
        executeAction(game, Role.MASON);
        executeAction(game, Role.SEER);
        executeAction(game, Role.ROBBER);
        executeAction(game, Role.TROUBLEMAKER);
        executeAction(game, Role.DRUNK);
        executeAction(game, Role.INSOMNIAC);

        game.getPlayers().stream()
                .sorted(Comparator.comparing(Player::getName))
                .forEach(p -> logger.info("Final: {} - {} {} ({})", game.getId(), last(p.getRoles()), p.getName(), p.getId()));
    }

    private void executeAction(Game game, Role initialRole) {
        game.getPlayers().stream()
                .filter(p -> !p.getId().startsWith(Player.CENTER_PREFIX))
                .filter(p -> getEffectiveInitialRole(p) == initialRole)
                .forEach(p -> executeAction(game, p));
    }

    private void executeAction(Game game, Player player) {
        Role role = getEffectiveInitialRole(player);
        Set<Player> players = game.getPlayers().stream()
                .filter(p -> !p.getId().startsWith(Player.CENTER_PREFIX))
                .collect(Collectors.toSet());

        if (role == Role.MINION) {
            players.stream()
                    .filter(p -> getEffectiveInitialRole(p) == Role.WEREWOLF)
                    .forEach(w -> player.getKnownPlayers().put(w.getId(), Role.WEREWOLF));
        } else if (role == Role.WEREWOLF) {
            List<Player> werewolves = players.stream()
                    .filter(p -> getEffectiveInitialRole(p) == Role.WEREWOLF)
                    .filter(p -> !p.getId().equals(player.getId()))
                    .collect(Collectors.toList());
            if (werewolves.isEmpty()) {
                Player center = getPlayer(game, player.getActions().get(0));
                player.getKnownPlayers().put(center.getId(), center.getRoles().get(0));
            } else {
                werewolves.forEach(w -> player.getKnownPlayers().put(w.getId(), Role.WEREWOLF));
            }
        } else if (role == Role.MASON) {
            players.stream()
                    .filter(p -> getEffectiveInitialRole(p) == Role.MASON)
                    .forEach(w -> player.getKnownPlayers().put(w.getId(), Role.MASON));
        } else if (role == Role.SEER) {
            players.stream()
                    .filter(p -> player.getActions().contains(p.getId()))
                    .forEach(w -> player.getKnownPlayers().put(w.getId(), getCardRole(w)));
        } else if (role == Role.ROBBER && !player.getActions().isEmpty()) {
            Player robbedPlayer = players.stream()
                    .filter(p -> p.getId().equals(player.getActions().get(0)))
                    .findFirst()
                    .orElseThrow();
            Role robbedRole = getCardRole(robbedPlayer);
            player.getRoles().add(robbedRole);
            robbedPlayer.getRoles().add(Role.ROBBER);
            player.getKnownPlayers().put(robbedPlayer.getId(), Role.ROBBER);
            player.getKnownPlayers().put(player.getId(), robbedRole);
        } else if (role == Role.TROUBLEMAKER && !player.getActions().isEmpty()) {
            Player targetA = players.stream()
                    .filter(p -> p.getId().equals(player.getActions().get(0)))
                    .findFirst()
                    .orElseThrow();
            Player targetB = players.stream()
                    .filter(p -> p.getId().equals(player.getActions().get(1)))
                    .findFirst()
                    .orElseThrow();
            Role roleA = last(targetA.getRoles());
            targetA.getRoles().add(last(targetB.getRoles()));
            targetB.getRoles().add(roleA);
        } else if (role == Role.DRUNK && !player.getActions().isEmpty()) {
            Player targetA = players.stream()
                    .filter(p -> p.getId().equals(player.getActions().get(0)))
                    .findFirst()
                    .orElseThrow();
            Role roleA = last(targetA.getRoles());
            targetA.getRoles().add(last(player.getRoles()));
            player.getRoles().add(roleA);
        } else if (role == Role.INSOMNIAC) {
            player.getKnownPlayers().put(player.getId(), last(player.getRoles()));
        }

        player.getKnownPlayers().forEach((knownId, knownRole) -> logger.info("Execution: {} - {} {} ({}) - {} ({}) {}", game.getId(), role.name(), player.getName(), player.getId(), knownRole.name(), getPlayer(game, knownId).getName(), knownId));
    }

    private Role getEffectiveInitialRole(Player player) {
        if (first(player.getRoles()) == Role.DOPPELGANGER) {
            return player.getRoles().get(1);
        } else {
            return player.getRoles().get(0);
        }
    }

    private Role getCardRole(Player player) {
        // doppelganger card on table, not changed by seer or robber
        if (first(player.getRoles()) == Role.DOPPELGANGER && player.getRoles().size() == 2) {
            return Role.DOPPELGANGER;
        } else {
            return last(player.getRoles());
        }
    }

    private <T> T first(List<T> list) {
        return list.get(0);
    }

    private <T> T last(List<T> list) {
        return list.get(list.size() - 1);
    }

    private Game getGame(String gameId) {
        return games.stream()
                .filter(g -> g.getId().equals(gameId))
                .findFirst()
                .orElseThrow();
    }

    private Player getPlayer(Game game, String playerId) {
        return game.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow();
    }
}
