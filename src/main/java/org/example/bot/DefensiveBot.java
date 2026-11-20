package org.example.bot;

import org.example.model.Game;
import org.example.model.HitZone;
import org.example.model.TurnAction;

public class DefensiveBot implements BotStrategy {

    @Override
    public String name() {
        return "Оборонительный";
    }

    @Override
    public TurnAction decide(Game game) {
        return BotSupport.choose(
                game, HitZone.TRACKS, 2);
    }
}
