package com.silcos.permainan.logic;

import com.silcos.board.Board;
import com.silcos.board.BoardEventListener;
import com.silcos.board.BoardGame;
import com.silcos.board.BoardMonoStateCache;
import com.silcos.board.CircularPlayerRotator;
import com.silcos.board.CompactLoc2D;
import com.silcos.board.Event;
import com.silcos.board.FinishEvent;
import com.silcos.board.Piece;
import com.silcos.board.Player;

import java.util.ArrayList;

import static com.silcos.board.BoardPlatformProvider.COMPUTER_TASK;
import static com.silcos.permainan.logic.LongMoveStateChangeEvent.LMSC_ENDED;
import static com.silcos.permainan.logic.LongMoveStateChangeEvent.LMSC_STARTED;

public class PermainanGame extends BoardGame {

    public static final String NAME = "Permainan";

    public static class Move extends com.silcos.board.Move {

        /* for capturing moves around the arc */
        private final boolean mLoopHinted;

        /* for steps of a long move */
        private final boolean mLongMoveStep;

        public Move(final boolean isVisible, final int srcRow,
                    final int srcCol, final int dstRow,
                    final int dstCol, final boolean loopHinted) {
            super(isVisible, srcRow, srcCol, dstRow, dstCol);
            mLoopHinted = loopHinted;
            mLongMoveStep = false;
        }

        public Move(final int srcRow, final int srcCol,
                    final int dstRow, final int dstCol,
                    final boolean loopHinted) {
            super(true, srcRow, srcCol, dstRow, dstCol);
            mLoopHinted = loopHinted;
            mLongMoveStep = true;
        }

        public boolean isLoopHinted() {
            return mLoopHinted;
        }

        public boolean isLongMoveStep() {
            return mLongMoveStep;
        }

        public boolean hasLoopInPath() {
            return mLoopHinted | mLongMoveStep;
        }
    }

    // ----------------- Long Move Logic ------------------

    private LongMover mLongMoveState;

    private void dispatchLongMoveStateChangeEvent(int changeId, LongMover previousState) {
        dispatchEvent(new LongMoveStateChangeEvent(changeId, previousState, this));
    }

    public PermainanGame() {
        super(new CircularPlayerRotator.Factory(), 2);
        mBoard = new PermainanGrid(6, 6);
        mController = new InputController();
    }

    @Override
    public Move getMove(int index) {
        return (Move) super.getMove(index);
    }

    @Override
    public PermainanPlayer getPlayer(int playerId) {
        return (PermainanPlayer) mPlayers[playerId];
    }

    public void setPlayer(Player player, int playerId) {
        if(player instanceof PermainanPlayer) {
            super.setPlayer(player, playerId);
        } else {
            throw new IllegalArgumentException("PermainanGame only " +
                    "accepts players that are an instance of the class " +
                    "PlayActivity.PermainanPlayer.");
        }
    }

    @Override
    public PermainanGrid board() {
        return (PermainanGrid) super.board();
    }

    public boolean stopLongMoveAt(int stopRow, int stopColumn) {
        if(mLongMoveState == null) {
            return false;
        }

        mLongMoveState.stopAt(stopRow, stopColumn);
        return true;
    }

    public LongMover longMoveState() {
        return mLongMoveState;
    }

    public boolean isLongMoveActive() {
        return mLongMoveState != null;
    }

    private boolean isFinished() {
        int cnt[] = new int[2];
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                final Piece p = pieceAt(r, c);
                if (p != null)
                    cnt[p.playerId()] += 1;
                if (cnt[0] > 0 && cnt[1] > 0)
                    return false;
            }
        }

        stopGame();
        if (cnt[0] == 0) {
            dispatchEvent(new FinishEvent(this, 1));
        } else {
            dispatchEvent(new FinishEvent(this, 0));
        }
        return true;
    }

    public static InputController extractController(PermainanGame game) {
        return (InputController) game.mController;
    }

    public static void wirePlayers(PermainanGame game, Player p1, Player p2) {
        game.mPlayers = new Player[]{
                p1, p2
        };
        game.renewInputController();
        // Place pebbles
        p1.initAfterGame();
        p2.initAfterGame();
        game.mPlayerStats[0] = game.new PlayerStats(p1);
        game.mPlayerStats[1] = game.new PlayerStats(p2);
        game.dispatchPlayerWiringEvent();
    }

    public static PermainanGame newLocalTwoPlayer(PermainanGame game, BoardEventListener eventListener) {
        final GameInputController inputController = PermainanGame.extractController(game);

        final HumanPermainanPlayer playerI = new HumanPermainanPlayer(0, game, inputController);
        final HumanPermainanPlayer playerII = new HumanPermainanPlayer(1, game, inputController);
        game.addBoardEventListener(eventListener);

        wirePlayers(game, playerI, playerII);
        ((InputController) inputController).updateBoardStateHistory();
        playerI.onTurn();
        return game;
    }

    public static PermainanGame newCompBased(PermainanGame game, BoardEventListener eventListener,
                                             boolean comp1, boolean comp2) {
        final GameInputController inputController = extractController(game);

        final PermainanPlayer playerI = comp1 ? new CompPermainanPlayer(0, game, inputController)
                                            : new HumanPermainanPlayer(0, game, inputController);
        final PermainanPlayer playerII = comp2 ? new CompPermainanPlayer(1, game, inputController)
                                            : new HumanPermainanPlayer(1, game, inputController);
        game.addBoardEventListener(eventListener);

        wirePlayers(game, playerI, playerII);
        ((InputController) inputController).updateBoardStateHistory();
        playerI.onTurn();
        return game;
    }

    public static boolean xFlag = false;

    public class InputController extends GameInputController {

        private ArrayList<BoardMonoStateCache> mBoardStateHistory = new ArrayList<>();
        private boolean mAddedLongMoveToHistory;

        /**
         * Updates the board-state history to conform to the newest move. If
         * a long move is under-process, the latest board-state is instead
         * modified to maintain even-odd order of player moves.
         */
        private void updateBoardStateHistory() {
            if (!isLongMoveActive() || !mAddedLongMoveToHistory) {
                if(isLongMoveActive())
                    mAddedLongMoveToHistory = true;
                mBoardStateHistory.add(
                        BoardMonoStateCache.buildCache(PermainanGame.this));
            } else {
                mBoardStateHistory.set(
                        mBoardStateHistory.size() - 1,
                        BoardMonoStateCache.buildCache(PermainanGame.this));
            }
        }

        private void updateLatestLongMoveTo(final int newFinalRow, final int newFinalCol) {
            final int idx = history.size() - 1;
            history.get(idx).resetDst(newFinalRow, newFinalCol);
        }

        public int getHistoryCacheSize() {
            return mBoardStateHistory.size();
        }

        public BoardMonoStateCache getStateAtMove(int moveNo) {
            return mBoardStateHistory.get(moveNo);
        }

        public boolean mPlayerLostAllPieces = false;

        @Override
        protected void onPlayerLostAllPieces(int loserId) {
            mPlayerLostAllPieces = true;
            stopGame();
            final int winnerId = (loserId == 0) ? 1 : 0;
            dispatchEvent(new FinishEvent(PermainanGame.this,
                    winnerId
            ));
        }

        @Override
        public boolean placeMove(final com.silcos.board.Move yourMove_) {
            if (isDead())
                return false;
            if(!(yourMove_ instanceof Move)) {
                throw new RuntimeException("Move is not compatible with " +
                        "PermainanGame.Move.");
            }

            final Move yourMove = (Move) yourMove_;
            final int srcRow = yourMove.getSrcRow();
            final int srcCol = yourMove.getSrcCol();
            final int dstRow = yourMove.getDstRow();
            final int dstCol = yourMove.getDstCol();
            final boolean loopHinted = yourMove.isLoopHinted();
            final Piece srcHolder = (Piece) mBoard.getCell(srcRow, srcCol).getHolder();
            final Piece dstHolder = (Piece) mBoard.getCell(dstRow, dstCol).getHolder();

            if (isLongMoveActive() && pieceAt(dstRow, dstCol) != null)
                mLongMoveState.mKilled = true;

            if(!mBoard.handle(yourMove))
                return false;

            if (!yourMove.isLongMoveStep())
                history.add(yourMove);

            updateBoardStateHistory();

            if(dstHolder != null) {
                getPlayerStat(dstHolder.playerId())
                        .onKilled(dstHolder);
            }

            dispatchEvent(new MoveEvent(PermainanGame.this, srcRow, srcCol, dstRow, dstCol,
                    srcHolder, dstHolder, loopHinted));

            if(dstHolder != null &&
                    getPlayerStat(dstHolder.playerId()).livePieces() == 0) {
                onPlayerLostAllPieces(dstHolder.playerId());
                return true;
            }

            if(yourMove.isLongMoveStep()) {
                if (mLongMoveState.mRepeatOnNetworkConfirm)
                    return true;

                getPlatformProvider().runComputeIntensiveTaskAfter(yourMove.mLoopHinted ? 800 : 500,
                        COMPUTER_TASK,
                        new Runnable() {
                            @Override
                            public void run() {
                                if(!mLongMoveState.doStep()) {
                                    final LongMover state = mLongMoveState;
                                    mLongMoveState = null;
                                    board().longMoveTrigger = false;
                                    switchPlayer();
                                    mAddedLongMoveToHistory = false;
                                    dispatchLongMoveStateChangeEvent(LMSC_ENDED, state);
                                }
                            }
                        });
            } else {
                playerRotator.nextPlayer().onTurn();
                if (timer != null)
                    timer.switchTo(playerRotator.getCurrentId());
            }

            return true;
        }

        public LongMover longMover() {
            return mLongMoveState;
        }

        public boolean placeLongMove(int dir, int row, int col) {
            return placeLongMove(dir, row, col, -1, -1);
        }

        public boolean placeLongMove(int dir, int row, int col, int stopRow, int stopCol) {
            if (isDead())
                return false;

            if(Board.isEdgeCell(row, col, 6, 6)) {
                final int cornerDir = PermainanGrid.cornerToLoopableCellDirection(row, col);
                if(Board.isCornerCell(row, col, 6, 6) || cornerDir == dir
                        || cornerDir == PermainanGrid.reverseDirection(dir)) {
                    return false;
                }
            }

            BoardMonoStateCache stateCache = BoardMonoStateCache.buildCache(PermainanGame.this);
            LongMoveAnalyzer analyzer = new LongMoveAnalyzer(stateCache, new CompactLoc2D(row, col));

            if (dir == PermainanGrid.RIGHT) {
                String x = "";
                for (int c = col; c < 6; c++) {
                    x += stateCache.pieceAt(row, c) == null ? "null " : "pres ";
                }
            }

            xFlag = true;
            if (!analyzer.isLongMovePossible(dir)) {
                return false;
            }

            mLongMoveState = new LongMover(dir, row, col);
            if (stopRow != -1 && stopCol != -1)
                mLongMoveState.stopAt(stopRow, stopCol);

            board().longMoveTrigger = true;

            mLongMoveState.mRepeatOnNetworkConfirm =
                    ((PermainanPlayer) playerRotator.previousPlayer()).isNetworked;

            history.add(new Move(row, col, row, col, false));
            dispatchLongMoveStateChangeEvent(LMSC_STARTED, null);
            if (!mLongMoveState.mRepeatOnNetworkConfirm)
                mLongMoveState.doStep();
            if (timer != null)
                timer.pause();
            return true;
        }

        public void switchPlayer() {
            mAddedLongMoveToHistory = false;
            ((PermainanPlayer) playerRotator.previousPlayer()).onFinishTurn();
            playerRotator.nextPlayer().onTurn();

            if (timer != null)
                timer.switchTo(playerRotator.getCurrentId());
        }
    }

    public void dispatchEvent(Event e) {
        if (e.typeId() == ELIMINATION_EVENT)
            isDead = true;
        super.dispatchEvent(e);
    }

    /**
     * LongMover holds the current state of a in-process long-move. It
     * requires help from {@link InputController} to be called again
     * once a step move has completed.
     *
     * LongMover should be called repeatedly to complete each step move
     * via {@code doStep()}. Networked games must keep LongMover objects
     * in-sync.
     */
    public final class LongMover {

        private int mDir;
        private int mRow;
        private int mCol;
        private int mStopRow;
        private int mStopCol;
        private boolean mStopSet;
        public final int mInitialRow;
        public final int mInitialCol;
        public final int mInitialDir;
        private boolean mPassedInit;
        public boolean mKilled = false;

        public boolean mMark = false;
        public boolean mRepeatOnNetworkConfirm = true;

        private LongMover(int dir, int row, int col) {
            mDir = dir;
            mRow = row;
            mCol = col;
            mInitialRow = row;
            mInitialCol = col;
            mInitialDir = dir;
            mPassedInit = false;
            mStopSet = false;
        }

        public int row() {
            return mRow;
        }

        public int col() {
            return mCol;
        }

        public int getDir() {
            return mDir;
        }

        public void setDir(int dir) {
            mDir = dir;
        }

        public void stopAt(int stopRow, int stopCol) {
            mStopSet = true;
            mStopRow = stopRow;
            mStopCol = stopCol;
        }

        public void stop() {
            final LongMover state = mLongMoveState;
            mLongMoveState = null;
            board().longMoveTrigger = false;
            ((InputController) mController).switchPlayer();
            dispatchLongMoveStateChangeEvent(LMSC_ENDED, state);
        }

        public boolean doStep() {
            if(mStopSet && mStopRow == mRow &&
                    mStopCol == mCol) {
                return false;
            }
            if (mKilled)
                return false;

            int newRow = mRow;
            int newCol = mCol;
            switch (mDir) {
                case PermainanGrid.UP:
                    --newRow;
                    break;
                case PermainanGrid.LEFT:
                    --newCol;
                    break;
                case PermainanGrid.RIGHT:
                    ++newCol;
                    break;
                case PermainanGrid.DOWN:
                    ++newRow;
                    break;
                default:
                    assert (false);
            }

            final int oldRow = mRow;
            final int oldCol = mCol;

            boolean moveAllowed;

            if (newRow < 0 || newRow > 5 || newCol < 0 || newCol > 5) {
                // we have to loop here instead @(fromRow, fromColumn)
                final PermainanGrid.Cell cell = (PermainanGrid.Cell) mBoard.getCell(oldRow, oldCol);
                final long dst = cell.loopCache.otherCoords(oldRow, oldCol);
                mRow = CompactLoc2D.decodeRow(dst);
                mCol = CompactLoc2D.decodeColumn(dst);

                mDir = PermainanGrid.longMoveAtLoopableCellDirection(mRow, mCol);
                moveAllowed = mController.placeMove(
                        new Move(oldRow, oldCol, mRow, mCol, true));
            } else {
                mRow = newRow;
                mCol = newCol;

                moveAllowed = mController.placeMove(
                        new Move(oldRow, oldCol, newRow, newCol, false));
            }

            if (mRow == mInitialRow && mCol == mInitialCol) {
                if (mPassedInit) {
                    return false;
                }
                mPassedInit = true;
            }

            if (!moveAllowed) {
                ((PermainanGrid) mBoard).longMoveTrigger = false;
            }
            return moveAllowed;
        }
    }
}
