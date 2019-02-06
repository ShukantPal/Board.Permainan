package com.silcos.permainan.logic;

public class MoveEvent extends com.silcos.board.MoveEvent {

    private PermainanGame permainanGame;
    private final boolean mIsLoopHinted;

    public MoveEvent(PermainanGame permainanGame, int srcRow, int srcCol, int dstRow, int dstCol,
                     Object targetHolder, Object killedHolder,
                     boolean isLoopHinted) {
        super(permainanGame, srcRow, srcCol, dstRow, dstCol, targetHolder, killedHolder);
        this.permainanGame = permainanGame;
        mIsLoopHinted = isLoopHinted;
    }

    public boolean isLoopHinted() {
        return mIsLoopHinted;
    }
}
