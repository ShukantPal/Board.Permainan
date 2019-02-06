package com.silcos.permainan.logic;

import com.silcos.board.BoardGame;

public class HumanPermainanPlayer extends PermainanPlayer {

    public HumanPermainanPlayer(int id, BoardGame work, BoardGame.GameInputController inputController) {
        super(work, inputController);
        mId = id;
    }

}
