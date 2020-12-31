package com.nilcaream.werewolves;

import org.junit.jupiter.api.Test;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;

class FlowTest {

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
}