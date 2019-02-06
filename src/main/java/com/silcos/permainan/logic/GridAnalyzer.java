package com.silcos.permainan.logic;

import com.silcos.board.BoardMonoStateCache;
import com.silcos.board.CompactLoc2D;
import com.silcos.board.Loc2D;
import com.silcos.board.Piece;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Provides analysis utilities for the PermainanGrid.
 */
public class GridAnalyzer {

    private static int deltaEvalCorner(int x, int y, BoardMonoStateCache stateCache) {
        final Piece piece = stateCache.pieceAt(x, y);
        if (piece == null)
            return 0;

        return (piece.playerId() == 0) ? -10 : 10;
    }

    /**
     * Finds the delta on eval() for placing pieces at the corner. Pieces at
     * the corner are unfavourable and are half-valued.
     *
     * @param stateCache
     * @return
     */
    private static int evalCorners(BoardMonoStateCache stateCache) {
        return deltaEvalCorner(0, 0, stateCache) +
                deltaEvalCorner(0, 5, stateCache) +
                deltaEvalCorner(5, 5, stateCache) +
                deltaEvalCorner(5, 0, stateCache);
    }

    /**
     * Evaluates the current position of the board. A +ve value means
     * that the red player is winning, while a -ve value means that the
     * black one is.
     *
     * Each piece is worth 10 points, unless one is placed on the corner
     * cell, where is worth only 5 points (-5 by deltaEvalCorner).
     *
     * @param stateCache
     * @return
     */
    public static int eval(BoardMonoStateCache stateCache) {
        int eval = 0;

        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                final Piece piece = stateCache.pieceAt(r, c);
                if (piece == null)
                    continue;

                int rC = r < 3 ? r : 5 - r;
                int cC = c < 3 ? c : 5 - c;
                if (rC == cC) {
                    eval += ((piece.playerId()) == 0) ? r : -r;
                }

                eval += ((piece.playerId() == 0) ? 10 : -10);
            }
        }

      //  eval += evalCorners(stateCache);
        return eval;
    }

    /**
     * Finds all the possible inputs for the given board state, that can
     * be provided by the given player (playerId).
     *
     * @param playerId player-id of the moving player
     * @param stateCache
     * @return
     */
    public static List<GridChangeInput> findAllInputs(int playerId, BoardMonoStateCache stateCache) {
        List<GridChangeInput> allInputs = new ArrayList<>();

        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                Loc2D loc = new CompactLoc2D(r, c);
                Piece p = stateCache.pieceAt(r, c);

                if (p != null && p.playerId() == playerId) {
                    allInputs.addAll(new MoveAnalyzer(stateCache, loc).findAllSimpleInputs());

                    final List<GridChangeInput>[] longMoveInputs =
                            new LongMoveAnalyzer(stateCache, loc).findAllLongMoves();

                    for (List<GridChangeInput> longMoveInputInDir : longMoveInputs) {
                        allInputs.addAll(longMoveInputInDir);
                    }
                }
            }
        }

        Collections.shuffle(allInputs);
        return allInputs;
    }

}
