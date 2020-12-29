package com.nilcaream.werewolves;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1")
public class ApiController {

    private Map<String, List<String>> gameToRoles = new HashMap<>();

    @GetMapping(value = "/roles/{game}", produces = "application/json")
    public List<String> getRoles(@PathVariable String game) {
        return gameToRoles.get(game);
    }

    @PostMapping(value = "/roles/{game}", consumes = "application/json", produces = "application/json")
    public Map<String, String> setRoles(@PathVariable String game, @RequestBody List<String> roles) {
        gameToRoles.put(game, roles);
        return Map.of("status", "OK");
    }

    @GetMapping(value = "/status/{game}", produces = "application/json")
    public Map<String, Object> status(@PathVariable String game) {
        Map<String, Object> result = new HashMap<>();

        return result;
    }
}
