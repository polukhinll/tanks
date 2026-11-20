package org.example.bot;

import org.example.model.Game;
import org.example.model.HitZone;
import org.example.model.TurnAction;

public class AggressiveBot implements BotStrategy {

    @Override
    public String name() {
        return "Агрессивный";
    }

    @Override
    public TurnAction decide(Game game) {
        return BotSupport.choose(
                game, HitZone.TURRET, 1);
    }
}

