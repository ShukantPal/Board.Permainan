package com.silcos.permainan.logic;

import com.silcos.board.Board;
import com.silcos.board.BoardMonoStateCache;
import com.silcos.board.CompactLoc2D;
import com.silcos.board.Loc2D;
import com.silcos.board.Move;
import com.silcos.board.Piece;

import java.util.ArrayList;
import java.util.List;

import static com.silcos.board.Board.isCornerCell;
import static com.silcos.permainan.logic.PermainanGrid.DOWN;
import static com.silcos.permainan.logic.PermainanGrid.LEFT;
import static com.silcos.permainan.logic.PermainanGrid.RIGHT;
import static com.silcos.permainan.logic.PermainanGrid.UP;
import static com.silcos.permainan.logic.PermainanGrid.cornerToLoopableCellDistance;
import static com.silcos.permainan.logic.PermainanGrid.innerArcs;
import static com.silcos.permainan.logic.PermainanGrid.outerArcs;

public class LongMoveAnalyzer {

    private final BoardMonoStateCache mStateCache;
    private final Loc2D mTargetLoc;
    private final int mTargetPlayerId;

    private List<BoardMonoStateCache> mAnalysisSteps[];

    private boolean isVerticalEdge() {
        final int col = mTargetLoc.column();
        return (col == 0 || col == 5);
    }

    private boolean isHorizontalEdge() {
        final int row = mTargetLoc.row();
        return (row == 0 || row == 5);
    }

    /**
     * Finds all the resulting long moves possible for this pebble
     * in the given dir.
     *
     * @param dir
     * @return
     */
    private List<GridChangeInput> findAllSteps(int dir) {
        final List<GridChangeInput> allSteps = new ArrayList<>();
        if (!isLongMovePossible(dir))
            return allSteps;

        final LongMoveTracer tracer = new LongMoveTracer(dir);
        while (tracer.doStep() != null) {
            final Loc2D result = tracer.curLoc();
            allSteps.add(new GridChangeInput(mTargetPlayerId, true, dir, tracer.killCount(),
                    mTargetLoc, result, tracer.mCurState));
        }

        return allSteps;
    }

    public LongMoveAnalyzer(BoardMonoStateCache stateCache, Loc2D targetLoc) {
        mStateCache = stateCache;
        mTargetLoc = targetLoc;
        mTargetPlayerId = stateCache.pieceAt(targetLoc.row(), targetLoc.column()).playerId();
    }

    /**
     * Checks whether a long move is allowed in the given direction. That
     * is only if there is no pebble in the same row/colum in the side of
     * the requested direction. In addition, long moves cannot happen from
     * a corner cell, or along a edge.
     *
     * If the pebble is already at the end of row/column for the given
     * direction, then to long-move the next move (along the loop) must be
     * available.
     *
     * @param dir
     * @return
     */
    public boolean isLongMovePossible(int dir) {
        int testRow = mTargetLoc.row();
        int testCol = mTargetLoc.column();

        if (Board.isCornerCell(testRow, testCol, 6, 6))
            return false;// not allowed at any condition

        int firstTransversalCount = 0;

        switch (dir) {
            case UP:
                if (isVerticalEdge())
                    return false;

                --testRow;
                while (testRow >= 0) {
                    if (mStateCache.pieceAt(testRow, testCol) != null)
                        return false;
                    ++firstTransversalCount;
                    -- testRow;
                }
                ++testRow;
                break;
            case DOWN:
                if (isVerticalEdge())
                    return false;

                ++testRow;
                while (testRow < 6) {
                    if (mStateCache.pieceAt(testRow, testCol) != null)
                        return false;
                    ++firstTransversalCount;
                    ++testRow;
                }
                --testRow;
                break;
            case RIGHT:
                if (isHorizontalEdge())
                    return false;

                ++testCol;
                while (testCol < 6) {
                    if (mStateCache.pieceAt(testRow, testCol) != null) {
                        return false;
                    }

                    ++firstTransversalCount;
                    ++testCol;
                }

                --testCol;
                break;
            case LEFT:
                if (isHorizontalEdge())
                    return false;

                --testCol;
                while (testCol >= 0) {
                    if (mStateCache.pieceAt(testRow, testCol) != null)
                        return false;
                    ++firstTransversalCount;
                    --testCol;
                }
                ++testCol;
                break;
            default:
                throw new RuntimeException("Wrong dir: " + dir);
        }

        /* Here a move can only exist if the loop is allowed, as there are no
           steps in the same row/column.*/
        if (firstTransversalCount == 0) {
            final int cornerDistance = cornerToLoopableCellDistance(testRow, testCol);
            long loopDes = -1;

            if (cornerDistance == 1) {
                for (PermainanGrid.RoundConnector rc : innerArcs) {
                    if (rc.hasCoordinates(CompactLoc2D.encode(testRow, testCol))) {
                        loopDes = rc.otherCoords(testRow, testCol);
                        break;
                    }
                }
            } else if (cornerDistance == 2) {
                for (PermainanGrid.RoundConnector rc : outerArcs) {

                    if (rc.hasCoordinates(CompactLoc2D.encode(testRow, testCol))) {
                        loopDes = rc.otherCoords(testRow, testCol);
                        break;
                    }
                }
            } else {
                throw new RuntimeException("Internal error: LongMoveAnalyer.isLongMovePossible() "
                 + testRow + "," + testCol + "r" + cornerDistance);
            }

            Piece p = mStateCache.pieceAt(CompactLoc2D.decodeRow(loopDes),
                    CompactLoc2D.decodeColumn(loopDes));
            if (p != null && p.playerId() == mTargetPlayerId) {
                return false;
            }
        }

        return true;
    }

    public List<GridChangeInput>[] findAllLongMoves() {
        List<GridChangeInput>[] allLMs = new List[4];
        allLMs[UP] = findAllSteps(UP);
        allLMs[DOWN] = findAllSteps(DOWN);
        allLMs[LEFT] = findAllSteps(LEFT);
        allLMs[RIGHT] = findAllSteps(RIGHT);
        return allLMs;
    }

    /**
     * Traces each step of a long move, and counts how may pebbles are killed
     * in each step. doStep() should be called until it returns null, after
     * which it will start spitting incorrect moves (because the longest move
     * ended already).
     */
    private class LongMoveTracer {

        private int mCurRow;
        private int mCurCol;
        private int mDir;
        private int mKillCount = 0;
        private boolean mPassedInit = false;

        private BoardMonoStateCache mCurState = mStateCache;

        private boolean tryTransverse() {
            final int oldRow = mCurRow;
            final int oldCol = mCurCol;

            switch (mDir) {
                case UP:
                    if (mCurRow == 0)
                        return false;
                    --mCurRow;
                    break;
                case DOWN:
                    if (mCurRow == 5)
                        return false;
                    ++mCurRow;
                    break;
                case LEFT:
                    if (mCurCol == 0)
                        return false;
                    --mCurCol;
                    break;
                case RIGHT:
                    if (mCurCol == 5)
                        return false;
                    ++mCurCol;
                    break;
                default:
                    throw new RuntimeException("Invalid dir: " + mDir);
            }

            mCurState = mCurState.doMove(
                    new Move(true, oldRow, oldCol, mCurRow, mCurCol), 6, 6);
            return true;
        }

        private Loc2D getResult() {
            final Piece piece = mStateCache.pieceAt(mCurRow, mCurCol);
            if (piece == null)
                return curLoc();

            /* Here we use mStateCache and not mCurState because, mCurState will be
             * updated to the current move, but the opponent pebble will be in its
             * original state in mStateCache.
             */
            if (piece.playerId() != mTargetPlayerId) {
                ++mKillCount;
                return curLoc();
            }

            return null;
        }

        LongMoveTracer(int dir) {
            mCurRow = mTargetLoc.row();
            mCurCol = mTargetLoc.column();
            mDir = dir;
        }

        public Loc2D curLoc() {
            return new CompactLoc2D(mCurRow, mCurCol);
        }

        public int killCount() {
            return mKillCount;
        }

        public Loc2D doStep() {
            if (mCurRow == mTargetLoc.row() &&
                    mCurCol == mTargetLoc.column() &&
                    mStateCache != mCurState) {
                if (mPassedInit)
                    return null;
                mPassedInit = true;
            } else if (mKillCount > 0) {
                return null;// can't kill more than once
            }

            if (tryTransverse()) {// doesn't loop, just move in mDir
                return getResult();
            }

            final CompactLoc2D curLoc = (CompactLoc2D) curLoc();
            int cornerDistance = cornerDistance = PermainanGrid.cornerToLoopableCellDistance(mCurRow, mCurCol);
            Loc2D otherLoc = null;
            if (cornerDistance == 1) {
                for (PermainanGrid.RoundConnector rc : innerArcs) {
                    if (rc.hasCoordinates(curLoc.val())) {
                        otherLoc = new CompactLoc2D(rc.otherCoords(mCurRow, mCurCol));
                        mDir = PermainanGrid.longMoveAtLoopableCellDirection(otherLoc.row(), otherLoc.column());
                        break;
                    }
                }
            } else if (cornerDistance == 2) {
                for (PermainanGrid.RoundConnector rc : outerArcs) {
                    if (rc.hasCoordinates(curLoc.val())) {
                        otherLoc = new CompactLoc2D(rc.otherCoords(mCurRow, mCurCol));
                        mDir = PermainanGrid.longMoveAtLoopableCellDirection(otherLoc.row(), otherLoc.column());
                        break;
                    }
                }
            } else {
                throw new RuntimeException("Reached corner cell, cannot proceed LongMoveTracer");
            }

            final int oldRow = mCurRow;
            final int oldCol = mCurCol;

            mCurRow = otherLoc.row();
            mCurCol = otherLoc.column();

            mCurState = mCurState.doMove(new Move(true, oldRow, oldCol, mCurRow, mCurCol), 6, 6);
            return getResult();
        }

    }

}