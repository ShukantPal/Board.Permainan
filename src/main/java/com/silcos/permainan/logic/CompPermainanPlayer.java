package com.silcos.permainan.logic;

import com.silcos.board.BoardGame;
import com.silcos.board.BoardMonoStateCache;
import com.silcos.board.BoardPlatformProvider;

import java.util.List;

import static com.silcos.board.BoardPlatformProvider.COMPUTER_TASK;

public class CompPermainanPlayer extends PermainanPlayer {

    private static final int SDEP = 4;
    private static final int INIT_ALPHA = Integer.MIN_VALUE;
    private static final int INIT_BETA = Integer.MAX_VALUE;

    private GridChangeInput computeResult;

    public CompPermainanPlayer(int id, BoardGame work,
                               BoardGame.GameInputController inputController) {
        super(work, inputController);
        mId = id;
    }

    private int doBestInput(int forId, int thisDepth,
                            int alpha, int beta,
                            BoardMonoStateCache allInputs) {
        if (thisDepth + 1 == SDEP)
            return GridAnalyzer.eval(allInputs);

        if (forId == 0) {
            return doMaxInput(thisDepth, alpha, beta, GridAnalyzer.findAllInputs(forId, allInputs));
        } else {
            return doMinInput(thisDepth, alpha, beta, GridAnalyzer.findAllInputs(forId, allInputs));
        }
    }

    private int doMaxInput(int thisDepth, int alpha, int beta, List<GridChangeInput> allInputs) {
        GridChangeInput bestInput = null;
        int bestScore = INIT_ALPHA;

        for (GridChangeInput input : allInputs) {
            final int inputScore = doBestInput(1, thisDepth + 1, alpha, beta, input.mNewState);

            if (inputScore >= bestScore) {
                bestInput = input;
                bestScore = inputScore;
            }

            alpha = Math.max(bestScore, alpha);

//            if (alpha >= beta)
//                break;
        }

        if (thisDepth == 0)
            computeResult = bestInput;

        return bestScore;
    }

    private int doMinInput(int thisDepth, int alpha, int beta, List<GridChangeInput> allInputs) {
        GridChangeInput bestInput = null;
        int bestScore = INIT_BETA;

        for (GridChangeInput input : allInputs) {
            final int inputScore = doBestInput(0, thisDepth + 1, alpha, beta, input.mNewState);

            if (inputScore <= bestScore) {
                bestInput = input;
                bestScore = inputScore;
            }

            beta = Math.min(bestScore, beta);

    //        if (alpha >= beta)
  //              break;
        }

        if (thisDepth == 0)
            computeResult = bestInput;

        return bestScore;
    }

    @Override
    public void onTurn() {
        super.onTurn();

        mWork.getPlatformProvider().runComputeIntensiveTaskAfter(200, BoardPlatformProvider.COMPUTER_TASK, new Runnable() {
                    @Override
                    public void run() {

                        doBestInput(mId, 0, INIT_ALPHA, INIT_BETA, BoardMonoStateCache.buildCache(mWork));

                        GridChangeInput result = computeResult;
                        computeResult = null;
                    /* List<GridChangeInput> myMoves = GridAnalyzer.findAllInputs(mId, stateCache);
                    GridChangeInput result = myMoves.get(0);
                    */
                        if (!result.mIsLongMove) {
                            inputController().placeMove(new PermainanGame.Move(true, result.mFromCell.row(),
                                    result.mFromCell.column(), result.mToCell.row(),
                                    result.mToCell.column(), false));
                        } else {
                            inputController().placeLongMove(result.mLongMoveInitialDir,
                                    result.mFromCell.row(), result.mFromCell.column(),
                                    result.mToCell.row(), result.mToCell.column());
                        }
                    }
                });
    }
}
