package com.silcos.permainan.logic;

import com.silcos.board.Board;
import com.silcos.board.BoardGame;
import com.silcos.board.Player;

public abstract class PermainanPlayer extends Player {

    protected int mId;

    private boolean mWasInited = false;

    public PermainanPlayer(BoardGame work, BoardGame.GameInputController inputController) {
        super(work, inputController);
    }

    protected PermainanGame.InputController inputController() {
        return (PermainanGame.InputController) mInputController;
    }

    @Override
    public void onTurn() {
        super.onTurn();
    }

    public boolean isNetworked;

    protected void initialPlacement() {
        if(mId == 0) {
            for(int i = 0; i <= 1; i++) {
                for(int j = 0; j < 6; j++) {
                    board().getCell(i, j).setHolder(myPieces[(i)*6+j]);
                }
            }
        } else {
            for(int i = 4; i < 6; i++) {
                for(int j = 0; j < 6; j++) {
                    board().getCell(i, j).setHolder(myPieces[(i-4)*6+j]);
                }
            }
        }
    }

    public boolean deliverMove(int srcRow, int srcCol, int dstRow, int dstCol) {
        if(!isTurn()) {
            return false;
        }

        final boolean isVisible = Board.areAdjacent(srcRow,
                srcCol, dstRow, dstCol);
        final boolean isLoopHinted = false;// use another method for loop-hinted
        final boolean moveDelv = deliverMove(new PermainanGame.Move(isVisible, srcRow, srcCol,
                dstRow, dstCol, isLoopHinted));

        return moveDelv;
    }

    public boolean deliverLongMove(int dir, int row, int col) {
        return inputController().placeLongMove(dir, row, col);
    }

    public void initAfterGame() {
        if(mWasInited) return;
        allocatePieces(12, mId);
        initialPlacement();
        mWasInited = true;
    }

}
