package com.nilcaream.werewolves;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
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
        Game game = underTest.createGame("game1", of(Role.WEREWOLF, Role.WEREWOLF, Role.SEER, Role.ROBBER, Role.TROUBLEMAKER, Role.VILLAGER));
        underTest.createPlayer("game1", "p1", "Player1");
        underTest.createPlayer("game1", "p2", "Player2");
        underTest.createPlayer("game1", "p3", "Player3");

        // roles setup - c0, c1, c2, p1, p2, p3
        // state: robber, troublemaker, villager. center: werewolf, werewolf, seer
        startGame(game, Role.WEREWOLF, Role.WEREWOLF, Role.SEER, Role.ROBBER, Role.TROUBLEMAKER, Role.VILLAGER);

        // when
        Player.State state1 = underTest.executeAction("game1", "p1", of("p3"));// state: villager, troublemaker, robber. center: werewolf, werewolf, seer
        Player.State state2 = underTest.executeAction("game1", "p2", of("p1", "p3"));// state: robber, troublemaker, villager. center: werewolf, werewolf, seer
        Player.State state3 = underTest.executeAction("game1", "p3", emptyList());// state: robber, troublemaker, villager. center: werewolf, werewolf, seer

        // then
        assertThat(state1).isEqualTo(Player.State.WORKING);
        assertThat(state2).isEqualTo(Player.State.WORKING);
        assertThat(state3).isEqualTo(Player.State.READY_TO_PLAY);
        assertState(underTest.playerStatus("game1", "p1"), Player.State.READY_TO_PLAY, "p3", Role.ROBBER, "p1", Role.VILLAGER);
        assertState(underTest.playerStatus("game1", "p2"), Player.State.READY_TO_PLAY);
        assertState(underTest.playerStatus("game1", "p3"), Player.State.READY_TO_PLAY);
    }

    // robber, troublemaker, villager. Robber takes villager role. Troublemaker reverts robber move
    // other role order. nothing should change
    @Test
    void shouldExecuteGame1b() {
        // given
        Game game = underTest.createGame("game1", of(Role.WEREWOLF, Role.WEREWOLF, Role.SEER, Role.ROBBER, Role.TROUBLEMAKER, Role.VILLAGER));
        underTest.createPlayer("game1", "p1", "Player1");
        underTest.createPlayer("game1", "p2", "Player2");
        underTest.createPlayer("game1", "p3", "Player3");

        // roles setup - c0, c1, c2, p1, p2, p3
        // state: robber, troublemaker, villager. center: werewolf, werewolf, seer
        startGame(game, Role.WEREWOLF, Role.WEREWOLF, Role.SEER, Role.ROBBER, Role.TROUBLEMAKER, Role.VILLAGER);

        // when
        Player.State state2 = underTest.executeAction("game1", "p2", of("p1", "p3"));
        Player.State state3 = underTest.executeAction("game1", "p3", emptyList());
        Player.State state1 = underTest.executeAction("game1", "p1", of("p3"));

        // then
        assertThat(state2).isEqualTo(Player.State.WORKING);
        assertThat(state3).isEqualTo(Player.State.WORKING);
        assertThat(state1).isEqualTo(Player.State.READY_TO_PLAY);
        assertState(underTest.playerStatus("game1", "p1"), Player.State.READY_TO_PLAY, "p3", Role.ROBBER, "p1", Role.VILLAGER);
        assertState(underTest.playerStatus("game1", "p2"), Player.State.READY_TO_PLAY);
        assertState(underTest.playerStatus("game1", "p3"), Player.State.READY_TO_PLAY);
    }

    private void startGame(Game game, Role... roles) {
        underTest.startGame("game1");
        List<Player> players = game.getPlayers().stream().sorted(Comparator.comparing(Player::getId)).collect(Collectors.toList());
        assertThat(roles.length).isEqualTo(players.size());
        for (int i = 0, rolesLength = roles.length; i < rolesLength; i++) {
            Player player = players.get(i);
            player.getRoles().clear();
            player.getRoles().add(roles[i]);
            logger.info("Player {} as {}", player.getId(), roles[i]);
        }
    }

    private void assertState(Flow.PlayerStatus actual, Player.State expectedState) {
        assertThat(actual.getStatus()).isEqualByComparingTo(expectedState);
        assertThat(actual.getPlayers().values()).containsOnly(Role.UNKNOWN);
    }

    private void assertState(Flow.PlayerStatus actual, Player.State expectedState, String pId1, Role expectedRole1) {
        assertThat(actual.getStatus()).isEqualByComparingTo(expectedState);
        assertThat(actual.getPlayers().get(pId1)).isEqualByComparingTo(expectedRole1);

        Set<Role> otherRoles = actual.getPlayers().entrySet().stream()
                .filter(e -> !e.getKey().equals(pId1))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        assertThat(otherRoles).containsOnly(Role.UNKNOWN);
    }

    private void assertState(Flow.PlayerStatus actual, Player.State expectedState, String pId1, Role expectedRole1, String pId2, Role expectedRole2) {
        assertThat(actual.getStatus()).isEqualByComparingTo(expectedState);
        assertThat(actual.getPlayers().get(pId1)).isEqualByComparingTo(expectedRole1);
        assertThat(actual.getPlayers().get(pId2)).isEqualByComparingTo(expectedRole2);

        Set<Role> otherRoles = actual.getPlayers().entrySet().stream()
                .filter(e -> !e.getKey().equals(pId1))
                .filter(e -> !e.getKey().equals(pId2))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        assertThat(otherRoles).containsOnly(Role.UNKNOWN);
    }
}