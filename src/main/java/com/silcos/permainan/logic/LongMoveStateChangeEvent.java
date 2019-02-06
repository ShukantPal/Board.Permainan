package com.silcos.permainan.logic;

import com.silcos.board.BoardGame;
import com.silcos.board.Event;
import com.silcos.permainan.logic.PermainanGame.LongMover;

/**
 * LMSC events are issued whenever the state of an ongoing long move
 * is modified internally - when a long move starts & ends.
 */
public class LongMoveStateChangeEvent extends Event {

    public static int LMSC_TYPE_ID = 2425235;
    public static int LMSC_STARTED = 343;
    public static int LMSC_ENDED = 352;

    private static int sLMSCIssueCount = 0;

    protected final int mChangeId;
    protected LongMover mPreviousState;

    protected LongMoveStateChangeEvent(int changeId, LongMover previousState, BoardGame origin) {
        super(LMSC_TYPE_ID + sLMSCIssueCount++, origin);

        mChangeId = changeId;
        mPreviousState = previousState;
    }

    public int getChangeId() {
        return mChangeId;
    }

    public LongMover getPreviousState() {
        return mPreviousState;
    }

    @Override
    public PermainanGame getOrigin() {
        return (PermainanGame) mOrigin;
    }

    @Override
    public int typeId() {
        return LMSC_TYPE_ID;
    }
}
