package com.silcos.permainan.logic;

import com.silcos.board.BoardMonoStateCache;
import com.silcos.board.CompactLoc2D;
import com.silcos.board.Loc2D;
import com.silcos.board.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for analyzing possible moves for a pebble that are
 * possible.
 */
public class MoveAnalyzer {

    private final BoardMonoStateCache mStateCache;
    private final Loc2D mTargetLoc;

    public MoveAnalyzer(BoardMonoStateCache stateCache, Loc2D targetLoc) {
        mStateCache = stateCache;
        mTargetLoc = targetLoc;
    }

    /**
     * Finds all simple inputs that are possible to give for the analyzed
     * pebble. These inputs are non-long-move-inputs & cannot possibly
     * kill another piece.
     *
     * @return
     */
    public List<GridChangeInput> findAllSimpleInputs() {
        final List<GridChangeInput> allSimpleInputs = new ArrayList<>();

        final int fromRow = mTargetLoc.row();
        final int fromCol = mTargetLoc.column();
        final int playerId = mStateCache.pieceAt(fromRow, fromCol).playerId();

        for (int deltaRow = -1; deltaRow <= 1; deltaRow++) {
            final int newRow = fromRow + deltaRow;
            if (newRow < 0 || newRow >= 6)
                continue;

            for (int deltaCol = -1; deltaCol <= 1; deltaCol++) {
                final int newCol = fromCol + deltaCol;
                if (newCol < 0 || newCol >= 6)
                    continue;

                if (mStateCache.pieceAt(newRow, newCol) != null)
                    continue;

                final CompactLoc2D toLoc = new CompactLoc2D(newRow, newCol);
                allSimpleInputs.add(new GridChangeInput(playerId, false, -1, 0, mTargetLoc, toLoc,
                        mStateCache.doMove(
                                new Move(false, fromRow, fromCol, newRow, newCol),
                                6, 6)));
            }
        }

        return allSimpleInputs;
    }
}
