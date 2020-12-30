package com.nilcaream.werewolves;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import java.util.stream.Stream;

@RestController
@RequestMapping("api/v1")
public class ApiController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Set<Game> games = new HashSet<>();
    private Random random = new SecureRandom();

    private static final class CreateGameRequest {
        String gameId;
        List<Role> roles;
    }

    @PutMapping(value = "/game", consumes = "application/json", produces = "application/json")
    public Map<String, String> createGame(@RequestBody CreateGameRequest request) {
        games.removeIf(game -> game.getId().equals(request.gameId));
        Game game = new Game();
        game.setId(request.gameId);
        game.setRoles(request.roles);
        games.add(game);
        logger.info("Game created: {} - {}", request.gameId, request.roles);
        return Map.of("status", "OK");
    }

    private static final class CreatePlayer {
        String gameId;
        String playerId;
        String playerName;
    }

    @PutMapping(value = "/player", consumes = "application/json", produces = "application/json")
    public Map<String, String> createPlayer(@RequestBody CreatePlayer request) {
        Player player = new Player(request.playerId, request.playerName);
        getGame(request.gameId).getPlayers().add(player);
        logger.info("Player created: {} - {} ({})", request.gameId, request.playerName, request.playerId);
        return Map.of("status", "OK");
    }

    // TODO delete player

    private static final class StartGameRequest {
        String gameId;
    }

    @PostMapping(value = "/start", consumes = "application/json", produces = "application/json")
    public Map<String, String> startGame(@RequestBody StartGameRequest request) {
        Game game = getGame(request.gameId);

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

            logger.info("Game started: {}", request.gameId);

            players.stream()
                    .sorted(Comparator.comparing(Player::getName))
                    .forEachOrdered(player -> logger.info("Player role selected: {} - {} ({}) as {}", request.gameId, player.getName(), player.getId(), first(player.getRoles())));
        } else {
            throw new IllegalStateException("Invalid roles or players count");
        }

        return Map.of("status", "OK");
    }

    private Player createCenter(int index) {
        Player player = new Player(Player.CENTER + index, Player.CENTER);
        player.setState(Player.State.READY_TO_PLAY);
        return player;
    }

    private static final class StatusResponse {
        Player.State status;
        Map<String, Role> players;
    }

    @GetMapping(value = "/player/status/{playerId}/{gameId}", produces = "application/json")
    public StatusResponse playerStatus(@PathVariable String playerId, @PathVariable String gameId) {
        Game game = getGame(gameId);
        Player player = getPlayer(game, playerId);

        StatusResponse response = new StatusResponse();
        if (game.getPlayers().stream().allMatch(p -> p.getState() == Player.State.READY_TO_PLAY)) {
            response.status = Player.State.READY_TO_PLAY;
            response.players = game.getPlayers().stream()
                    .collect(Collectors.toMap(Player::getName, p -> player.getKnownPlayers().getOrDefault(p.getId(), Role.UNKNOWN)));
        } else {
            response.status = Player.State.WORKING;
        }
        return response;
    }

    private static final class PlayerActionRequest {
        String gameId;
        String playerId;
        List<String> actions;
    }

    @PostMapping(value = "/player/action", consumes = "application/json", produces = "application/json")
    public synchronized Map<String, String> executeAction(@RequestBody PlayerActionRequest request) {
        Game game = getGame(request.gameId);
        Player player = getPlayer(game, request.playerId);

        logger.info("Player action: {} - {} ({}) - {}", request.gameId, player.getName(), player.getId(), request.actions);

        if (player.getRoles().get(0) == Role.DOPPELGANGER) {
            Role newRole = game.getPlayers().stream()
                    .filter(p -> p.getId().equals(request.actions.get(0)))
                    .map(p -> p.getRoles().get(0))
                    .findFirst()
                    .orElseThrow();
            player.getRoles().add(newRole);
        } else {
            player.setActions(request.actions);
            player.setState(Player.State.READY_TO_PLAY);
        }

        if (game.getPlayers().stream().allMatch(p -> p.getState() == Player.State.READY_TO_PLAY)) {
            executeActions(game);
            return Map.of("status", Player.State.READY_TO_PLAY.name());
        } else {
            return Map.of("status", Player.State.WORKING.name());
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
    }

    private void executeAction(Game game, Role initialRole) {
        game.getPlayers().stream()
                .filter(p -> !p.getName().equals(Player.CENTER))
                .filter(p -> first(p.getRoles()) == initialRole)
                .findFirst()
                .ifPresent(p -> executeAction(game, p));
    }

    private void executeAction(Game game, Player player) {
        Role role = getEffectiveInitialRole(player);
        Stream<Player> players = game.getPlayers().stream()
                .filter(p -> !p.getName().equals(Player.CENTER));

        player.getKnownPlayers().put(player.getId(), role);

        if (role == Role.MINION) {
            players.filter(p -> getEffectiveInitialRole(p) == Role.WEREWOLF)
                    .forEach(w -> player.getKnownPlayers().put(w.getId(), Role.WEREWOLF));
        } else if (role == Role.WEREWOLF) {
            players.filter(p -> getEffectiveInitialRole(p) == Role.WEREWOLF)
                    .forEach(w -> player.getKnownPlayers().put(w.getId(), Role.WEREWOLF));
        } else if (role == Role.MASON) {
            players.filter(p -> getEffectiveInitialRole(p) == Role.MASON)
                    .forEach(w -> player.getKnownPlayers().put(w.getId(), Role.MASON));
        } else if (role == Role.SEER) {
            players.filter(p -> player.getActions().contains(p.getId()))
                    .forEach(w -> player.getKnownPlayers().put(w.getId(), getCardRole(player)));
        } else if (role == Role.ROBBER && !player.getActions().isEmpty()) {
            Player robbedPlayer = players.filter(p -> p.getId().equals(player.getActions().get(0)))
                    .findFirst()
                    .orElseThrow();
            player.getRoles().add(getCardRole(robbedPlayer));
            robbedPlayer.getRoles().add(Role.ROBBER);
            player.getKnownPlayers().put(robbedPlayer.getId(), Role.ROBBER);
        } else if (role == Role.TROUBLEMAKER && !player.getActions().isEmpty()) {
            Player targetA = players.filter(p -> p.getId().equals(player.getActions().get(0)))
                    .findFirst()
                    .orElseThrow();
            Player targetB = players.filter(p -> p.getId().equals(player.getActions().get(1)))
                    .findFirst()
                    .orElseThrow();
            Role roleA = last(targetA.getRoles());
            targetA.getRoles().add(last(targetB.getRoles()));
            targetB.getRoles().add(roleA);
        } else if (role == Role.DRUNK && !player.getActions().isEmpty()) {
            Player targetA = players.filter(p -> p.getId().equals(player.getActions().get(0)))
                    .findFirst()
                    .orElseThrow();
            Role roleA = last(targetA.getRoles());
            targetA.getRoles().add(last(player.getRoles()));
            player.getRoles().add(roleA);
        } else if (role == Role.INSOMNIAC) {
            player.getKnownPlayers().put(player.getId(), last(player.getRoles()));
        }
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
