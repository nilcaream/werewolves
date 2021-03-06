package com.nilcaream.werewolves;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;

class FlowTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Flow underTest = new Flow();

    @Test
    void shouldInitializePlayers() {
        // given
        Game game = underTest.createGame("game1", of(Role.WEREWOLF, Role.WEREWOLF, Role.SEER, Role.ROBBER, Role.TROUBLEMAKER, Role.VILLAGER));
        underTest.createPlayer("game1", "p1", "Player1");
        underTest.createPlayer("game1", "p2", "Player2");
        underTest.createPlayer("game1", "p3", "Player3");

        // sanity check
        assertThat(game.getPlayers().stream().map(Player::getId)).containsExactlyInAnyOrder("p1", "p2", "p3");
        assertThat(game.getPlayers().stream().map(p -> p.getRoles().isEmpty())).doesNotContain(false);

        // when
        underTest.startGame("game1");
        assertThat(game.getPlayers().stream().map(p -> p.getRoles().get(0))).containsAnyOf(Role.WEREWOLF, Role.WEREWOLF, Role.SEER, Role.ROBBER, Role.TROUBLEMAKER, Role.VILLAGER);
    }

    // robber, troublemaker, villager. Robber takes villager role. Troublemaker reverts robber move
    @Test
    void shouldExecuteGame1a() {
        // given
        Game game = createGame(of(Role.ROBBER, Role.TROUBLEMAKER, Role.VILLAGER), of(Role.WEREWOLF, Role.WEREWOLF, Role.SEER));

        // when
        Player.State state1 = underTest.executeAction("game1", "p0", of("p2"));
        Player.State state2 = underTest.executeAction("game1", "p1", of("p0", "p2"));
        Player.State state3 = underTest.executeAction("game1", "p2", of());

        // then
        assertThat(state1).isEqualTo(Player.State.WORKING);
        assertThat(state2).isEqualTo(Player.State.WORKING);
        assertThat(state3).isEqualTo(Player.State.READY_TO_PLAY);
        assertState(game, "p0", Role.ROBBER, "p0", Role.VILLAGER, "p2", Role.ROBBER);
        assertState(game, "p1", Role.TROUBLEMAKER);
        assertState(game, "p2", Role.VILLAGER);
    }

    // robber, troublemaker, villager. Robber takes villager role. Troublemaker reverts robber move
    // other role order. nothing should change
    @Test
    void shouldExecuteGame1b() {
        // given
        Game game = createGame(of(Role.ROBBER, Role.TROUBLEMAKER, Role.VILLAGER), of(Role.WEREWOLF, Role.WEREWOLF, Role.SEER));

        // when
        Player.State state2 = underTest.executeAction("game1", "p1", of("p0", "p2"));
        Player.State state3 = underTest.executeAction("game1", "p2", of());
        Player.State state1 = underTest.executeAction("game1", "p0", of("p2"));

        // then
        assertThat(state2).isEqualTo(Player.State.WORKING);
        assertThat(state3).isEqualTo(Player.State.WORKING);
        assertThat(state1).isEqualTo(Player.State.READY_TO_PLAY);
        assertState(game, "p0", Role.ROBBER, "p0", Role.VILLAGER, "p2", Role.ROBBER);
        assertState(game, "p1", Role.TROUBLEMAKER);
        assertState(game, "p2", Role.VILLAGER);
    }

    @Test
    void shouldExecuteGame2a() {
        // given
        Game game = createGame(of(Role.ROBBER, Role.TROUBLEMAKER, Role.VILLAGER, Role.WEREWOLF), of(Role.WEREWOLF, Role.VILLAGER, Role.SEER));

        // when
        underTest.executeAction("game1", "p0", of("p3")); // robber. p0 werewolf. p3 robber
        underTest.executeAction("game1", "p1", of("p0", "p2")); // troublemaker. p0 villager. p2 werewolf
        underTest.executeAction("game1", "p2", of()); // villager
        underTest.executeAction("game1", "p3", of("Center2")); // werewolf

        // then
        assertState(game, "p0", Role.VILLAGER, "p0", Role.WEREWOLF, "p3", Role.ROBBER);
        assertState(game, "p1", Role.TROUBLEMAKER);
        assertState(game, "p2", Role.WEREWOLF);
        assertState(game, "p3", Role.ROBBER, "Center2", Role.SEER);
    }

    @Test
    void shouldExecuteGame3a() {
        // given
        Game game = createGame(of(Role.WEREWOLF, Role.SEER, Role.ROBBER, Role.TROUBLEMAKER), of(Role.WEREWOLF, Role.VILLAGER, Role.DRUNK));

        // when
        underTest.executeAction("game1", "p0", of("Center0")); // werewolf: c0 werewolf
        underTest.executeAction("game1", "p1", of("p2")); // seer: p2 robber
        underTest.executeAction("game1", "p2", of("p0")); // robber. p0 robber, p2 werewolf
        underTest.executeAction("game1", "p3", of("p0", "p1")); // troublemaker

        // then
        assertState(game, "p0", Role.SEER, "Center0", Role.WEREWOLF);
        assertState(game, "p1", Role.ROBBER, "p2", Role.ROBBER);
        assertState(game, "p2", Role.WEREWOLF, "p0", Role.ROBBER, "p2", Role.WEREWOLF);
        assertState(game, "p3", Role.TROUBLEMAKER);
    }

    @Test
    void shouldExecuteGame4a() {
        // given
        Game game = createGame(of(Role.WEREWOLF, Role.WEREWOLF, Role.VILLAGER, Role.VILLAGER), of(Role.SEER, Role.TROUBLEMAKER, Role.ROBBER));

        // when
        underTest.executeAction("game1", "p0", of());
        underTest.executeAction("game1", "p1", of());
        underTest.executeAction("game1", "p2", of());
        underTest.executeAction("game1", "p3", of());

        // then
        assertState(game, "p0", Role.WEREWOLF, "p1", Role.WEREWOLF);
        assertState(game, "p1", Role.WEREWOLF, "p0", Role.WEREWOLF);
        assertState(game, "p2", Role.VILLAGER);
        assertState(game, "p3", Role.VILLAGER);
    }

    @Test
    void shouldExecuteGame5a() {
        // given
        Game game = createGame(of(Role.WEREWOLF, Role.WEREWOLF, Role.SEER, Role.ROBBER), of(Role.VILLAGER, Role.TROUBLEMAKER, Role.VILLAGER));

        // when
        underTest.executeAction("game1", "p0", of());
        underTest.executeAction("game1", "p1", of());
        underTest.executeAction("game1", "p2", of("p1"));
        underTest.executeAction("game1", "p3", of("p1"));

        // then
        assertState(game, "p0", Role.WEREWOLF, "p1", Role.WEREWOLF);
        assertState(game, "p1", Role.ROBBER, "p0", Role.WEREWOLF);
        assertState(game, "p2", Role.SEER, "p1", Role.WEREWOLF);
        assertState(game, "p3", Role.WEREWOLF, "p1", Role.ROBBER, "p3", Role.WEREWOLF);
    }

    private Game createGame(List<Role> playerRoles, List<Role> centerRoles) {
        List<Role> roles = new ArrayList<>();
        roles.addAll(playerRoles);
        roles.addAll(centerRoles);
        Game game = underTest.createGame("game1", roles);

        for (int i = 0; i < playerRoles.size(); i++) {
            underTest.createPlayer(game.getId(), "p" + i, "Player" + i);
        }

        underTest.startGame("game1");

        for (int i = 0; i < playerRoles.size(); i++) {
            String playerId = "p" + i;
            Player player = game.getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
            player.getRoles().clear();
            player.getRoles().add(playerRoles.get(i));
            logger.info("Player {} as {}", player.getId(), playerRoles.get(i));
        }

        for (int i = 0; i < centerRoles.size(); i++) {
            String playerId = Player.CENTER_PREFIX + i;
            Player player = game.getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
            player.getRoles().clear();
            player.getRoles().add(centerRoles.get(i));
            logger.info("Player {} as {}", player.getId(), centerRoles.get(i));
        }

        return game;
    }

    private void assertState(Game game, String playerId, Role expectedRole) {
        Flow.PlayerStatus status = underTest.playerStatus(game.getId(), playerId);
        Player player = game.getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
        Role role = player.getRoles().get(player.getRoles().size() - 1);
        assertThat(role).isEqualByComparingTo(expectedRole);

        assertThat(status.getPlayers().values()).containsOnly(Role.UNKNOWN);
    }

    private void assertState(Game game, String playerId, Role expectedRole, String pId1, Role expectedRole1) {
        Flow.PlayerStatus status = underTest.playerStatus(game.getId(), playerId);
        Player player = game.getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
        Role role = player.getRoles().get(player.getRoles().size() - 1);
        assertThat(role).isEqualByComparingTo(expectedRole);

        assertThat(status.getPlayers().get(pId1)).isEqualByComparingTo(expectedRole1);

        Set<Role> otherRoles = status.getPlayers().entrySet().stream()
                .filter(e -> !e.getKey().equals(pId1))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        assertThat(otherRoles).containsOnly(Role.UNKNOWN);
    }

    private void assertState(Game game, String playerId, Role expectedRole, String pId1, Role expectedRole1, String pId2, Role expectedRole2) {
        Flow.PlayerStatus status = underTest.playerStatus(game.getId(), playerId);
        Player player = game.getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
        Role role = player.getRoles().get(player.getRoles().size() - 1);
        assertThat(role).isEqualByComparingTo(expectedRole);

        assertThat(status.getPlayers().get(pId1)).isEqualByComparingTo(expectedRole1);
        assertThat(status.getPlayers().get(pId2)).isEqualByComparingTo(expectedRole2);

        Set<Role> otherRoles = status.getPlayers().entrySet().stream()
                .filter(e -> !e.getKey().equals(pId1))
                .filter(e -> !e.getKey().equals(pId2))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        assertThat(otherRoles).containsOnly(Role.UNKNOWN);
    }
}