package com.nilcaream.werewolves;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1")
public class ApiController {

    private final Flow flow;

    public ApiController(Flow flow) {
        this.flow = flow;
    }

    private static final class CreateGameRequest {
        String gameId;
        List<Role> roles;
    }

    @PutMapping(value = "/game", consumes = "application/json", produces = "application/json")
    public Map<String, String> createGame(@RequestBody CreateGameRequest request) {
        flow.createGame(request.gameId, request.roles);
        return Map.of("status", "OK");
    }

    private static final class CreatePlayerRequest {
        String gameId;
        String playerId;
        String playerName;
    }

    @PutMapping(value = "/player", consumes = "application/json", produces = "application/json")
    public Map<String, String> createPlayer(@RequestBody CreatePlayerRequest request) {
        flow.createPlayer(request.gameId, request.playerId, request.playerName);
        return Map.of("status", "OK");
    }

    @DeleteMapping(value = "/player/{playerId}/{gameId}", produces = "application/json")
    public Map<String, String> deletePlayer(@PathVariable String playerId, @PathVariable String gameId) {
        flow.deletePlayer(gameId, playerId);
        return Map.of("status", "OK");
    }

    @PostMapping(value = "/game/start/{gameId}", produces = "application/json")
    public Map<String, String> startGame(@PathVariable String gameId) {
        flow.startGame(gameId);
        return Map.of("status", "OK");
    }

    @GetMapping(value = "/player/{playerId}/{gameId}", produces = "application/json")
    public Flow.PlayerStatus playerStatus(@PathVariable String playerId, @PathVariable String gameId) {
        return flow.playerStatus(gameId, playerId);
    }

    private static final class PlayerActionRequest {
        String gameId;
        String playerId;
        List<String> actions;
    }

    @PostMapping(value = "/player/action", consumes = "application/json", produces = "application/json")
    public synchronized Map<String, String> executeAction(@RequestBody PlayerActionRequest request) {
        Player.State state = flow.executeAction(request.gameId, request.playerId, request.actions);
        return Map.of("status", state.name());
    }
}
