package org.example.bot;

import org.example.model.Game;
import org.example.model.TurnAction;

/** Интерфейс выбора хода ботом. */
public interface BotStrategy {

    String name();

    TurnAction decide(Game game);
}

