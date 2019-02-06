package com.silcos.permainan.logic;

import com.silcos.board.BoardGame;
import com.silcos.board.Modulator;

public class PermainanModulator implements Modulator<String> {

    protected PermainanPlayer mNetworkedPlayer;
    protected boolean mInLongMoveSync = false;

    public int mMoveSoph;

    public PermainanModulator() {
    }

    public PermainanModulator(PermainanPlayer networkedPlayer) {
        mNetworkedPlayer = networkedPlayer;
    }

    public void setNetworkedPlayer(PermainanPlayer networkedPlayer) {
        mNetworkedPlayer = networkedPlayer;
    }

    protected void decodeLongMoveTask(String inputMessage) {
        final char dir = inputMessage.charAt(1);
        mInLongMoveSync = true;
        int dirInNum;

        switch (dir) {
            case 'U': dirInNum = PermainanGrid.UP; break;
            case 'D': dirInNum = PermainanGrid.DOWN; break;
            case 'R': dirInNum = PermainanGrid.RIGHT; break;
            case 'L': dirInNum = PermainanGrid.LEFT; break;
            default: throw new IllegalArgumentException("Invalid direction: " + dir);
        }

        final String[] locs = inputMessage.substring(3).split(" ");// LL x x

        if (locs.length != 2) {
            throw new IllegalArgumentException("Invalid long move message recieved - " +
                    "the number of locs isn't 2.");
        }

        final int srcRow = Integer.valueOf(locs[0]);
        final int srcCol = Integer.valueOf(locs[1]);

        mNetworkedPlayer.deliverLongMove(dirInNum, srcRow, srcCol);
    }

    @Override
    public void decodeMoveTask(String inputMessage) {
        if (inputMessage == null || inputMessage.length() == 0) {
            throw new IllegalArgumentException("Invalid move message recieved");
        }

        if (inputMessage.charAt(0) == 'L') {
            decodeLongMoveTask(inputMessage);
            return;
        } else if (inputMessage.charAt(0) == 'I') {
            mNetworkedPlayer.inputController().longMover().doStep();
            return;
        } else if (inputMessage.charAt(0) == 'F') {
            mInLongMoveSync = false;
            mNetworkedPlayer.inputController().longMover().stop();
            return;
        } else if (inputMessage.charAt(0) == 'R') {
            mNetworkedPlayer.inputController().acceptResignation(mNetworkedPlayer.mId == 0 ? 1 : 0);
            return;
        }

        final String[] locs = inputMessage.split(" ");

        if (locs.length != 4) {
            throw new IllegalArgumentException("Length of locs is not 4 in non-long move.");
        }

        final int srcRow = Integer.valueOf(locs[0]);
        final int srcCol = Integer.valueOf(locs[1]);
        final int dstRow = Integer.valueOf(locs[2]);
        final int dstCol = Integer.valueOf(locs[3]);

        mNetworkedPlayer.deliverMove(srcRow, srcCol, dstRow, dstCol);
    }

    public String encodeMoveTask(int srcRow, int srcCol, int dstRow, int dstCol) {
        ++mMoveSoph;
        return srcRow + " " + srcCol + " " + dstRow + " " + dstCol;
    }

    public String encodeLongMoveTask(int dir, int row, int col) {
        char dirChar;
        ++mMoveSoph;

        switch (dir) {
            case PermainanGrid.UP: dirChar = 'U'; break;
            case PermainanGrid.DOWN: dirChar = 'D'; break;
            case PermainanGrid.LEFT: dirChar = 'L'; break;
            case PermainanGrid.RIGHT: dirChar = 'R'; break;
            default: throw new IllegalArgumentException("Illegal dir : " + dir);
        }

        return "L" + dirChar + " " + row + " " + col;
    }

    public String encodeLongMoveStep(int srcRow, int srcCol, int dstRow, int dstCol) {
        return "I " + encodeMoveTask(srcRow, srcCol, dstRow, dstCol);
    }

    public String encodeLongMoveEnd() {
        ++mMoveSoph;
        return "F";
    }

    public String encodeResign() {
        ++mMoveSoph;
        return "R";
    }

}
