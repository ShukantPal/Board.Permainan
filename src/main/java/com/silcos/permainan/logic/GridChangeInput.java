package com.silcos.permainan.logic;

import com.silcos.board.BoardMonoStateCache;
import com.silcos.board.Loc2D;

import java.util.List;

/**
 * Holds a possible input given to the grid as a move. This means that
 * it can be a long move or a regular move.
 */
public class GridChangeInput {

    public final int mPlayerId;
    public final boolean mIsLongMove;
    public final int mLongMoveInitialDir;
    public final int mOtherPebblesCleared;
    public final Loc2D mFromCell;
    public final Loc2D mToCell;
    public final BoardMonoStateCache mNewState;

    public List<GridChangeInput> playerNotes;

    public GridChangeInput(final int playerId, final boolean isLongMove, final int longMoveDir,
                           final int otherPebblesCleared,
                           final Loc2D fromCell, final Loc2D toCell,
                           final BoardMonoStateCache newState) {
        mPlayerId = playerId;
        mIsLongMove = isLongMove;
        mLongMoveInitialDir = longMoveDir;
        mOtherPebblesCleared = otherPebblesCleared;
        mFromCell = fromCell;
        mToCell = toCell;
        mNewState = newState;
    }

    public boolean isMaxPlayer() {
        return mPlayerId == 0;
    }

    public boolean isMinPlayer() {
        return mPlayerId == 1;
    }

    public String toString() {
        return "mPlayerId: " + mPlayerId +
                ", mIsLongMove: " + mIsLongMove +
                ", mLongMoveInitialDir: " + mLongMoveInitialDir +
                ", mOtherPebblesCleared: " + mOtherPebblesCleared +
                ", mFromCell: " + mFromCell.row() + ", " + mFromCell.column() +
                ", mToCell: " + mToCell.row() + ", " + mToCell.column();
    }

}
