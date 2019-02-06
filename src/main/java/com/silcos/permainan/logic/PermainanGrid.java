package com.silcos.permainan.logic;

import com.silcos.board.Board;
import com.silcos.board.CompactLoc2D;
import com.silcos.board.Move;
import com.silcos.board.Piece;

/**
 * Board for Permainan
 */
public class PermainanGrid extends Board {

    /**
     * RoundConnector is an ordered pair of two different coordinates
     * on the Board. They are particularly used to link the two cells
     * that are connected by an arc on the Permainan board.
     */
    public final static class RoundConnector {

        public final int fRow;
        public final int fCol;
        public final int sRow;
        public final int sCol;

        RoundConnector(int fRow, int fCol, int sRow, int sCol) {
            this.fRow = fRow;
            this.fCol = fCol;
            this.sRow = sRow;
            this.sCol = sCol;
        }

        public final long fCoords() {
            return CompactLoc2D.encode(fRow, fCol);
        }

        public final long sCoords() {
            return CompactLoc2D.encode(sRow, sCol);
        }

        public long otherCoords(long coords) {
            final long fCoords = fCoords();
            return (coords == fCoords) ? sCoords() : fCoords;
        }

        public long otherCoords(int row, int column) {
            return otherCoords(CompactLoc2D.encode(row, column));
        }

        public boolean hasCoordinates(long coordinates) {
            return (coordinates == fCoords() ||
                    coordinates == sCoords());
        }
    }

    public static class Cell implements com.silcos.board.Cell {

        RoundConnector loopCache;
        Piece holder;

        @Override
        public Piece getHolder() {
            return holder;
        }

        @Override
        public void setHolder(Piece o) {
            holder = o;
        }
    }

    /**
     * RoundConnector objects for each inner-arc on the Permainan board,
     * in a clockwise order from the upper-left arc.
     */
    public static final RoundConnector[] innerArcs =
            new RoundConnector[] {
                    new RoundConnector(1, 0, 0, 1),
                    new RoundConnector(0, 4, 1, 5),
                    new RoundConnector(4, 5, 5, 4),
                    new RoundConnector(5, 1, 4, 0)
            };

    /**
     * RoundConnector objects for each outer-arc on the Permainan board,
     * in a clockwise order from the upper-left arc.
     */
    public static final RoundConnector[] outerArcs =
            new RoundConnector[] {
                    new RoundConnector(2, 0, 0, 2),
                    new RoundConnector(0, 3, 2, 5),
                    new RoundConnector(3, 5, 5, 3),
                    new RoundConnector(5, 2, 3, 0)
            };

    /**
     * Set this to true whenever a longmove is under progress. This
     * enable pieces to be killed on this grid.
     */
    boolean longMoveTrigger;

    /**
     * Links the two cells stored in connector to the connector
     * itself.
     *
     * @param connector
     */
    private void hardLinkCellsToConnector(RoundConnector connector) {
        ((Cell) cellData[connector.fRow][connector.fCol]).loopCache = connector;
        ((Cell) cellData[connector.sRow][connector.sCol]).loopCache = connector;
    }

    /**
     * Allocates the cell matrix based on the cellBasedHeight and
     * cellBasedWidth properties. Each cell is allocated in the
     * matrix.
     */
    protected void allocateCellData() {
        super.allocateCellData();

        for (int i = 0; i < getHeight(); i++) {
            for (int j = 0; j < getWidth(); j++) {
                cellData[i][j] = new Cell();
            }
        }
    }

    /**
     * Links all the cells that are connected by an internal or
     * external arc.
     */
    private void hardLinkCellsToConnectors() {
        for(RoundConnector innerArc : innerArcs)
            hardLinkCellsToConnector(innerArc);

        for(RoundConnector outerArc : outerArcs)
            hardLinkCellsToConnector(outerArc);
    }

    public PermainanGrid(int cellBasedHeight, int cellBasedWidth) {
        super(cellBasedHeight, cellBasedWidth);
        allocateCellData();
        hardLinkCellsToConnectors();
    }

    @Override
    public boolean handle(Move directMove) {
        final int srcRow = directMove.getSrcRow(), srcCol = directMove.getSrcCol();
        final Cell srcCell = (Cell) getCell(srcRow, srcCol);

        if(srcCell.getHolder() == null) {
            return false;
        }

        final int dstRow = directMove.getDstRow();
        final int dstCol = directMove.getDstCol();
        final Cell dstCell = (Cell) getCell(dstRow, dstCol);

        if((!Board.areAdjacent(srcRow, srcCol, dstRow, dstCol)
                && !((PermainanGame.Move)directMove).isLoopHinted())
                || dstCell.getHolder() != null) {
            if(!longMoveTrigger || (dstCell.getHolder() != null &&
                    (((Piece) srcCell.getHolder()).playerId() == ((Piece)
                    dstCell.getHolder()).playerId()))) {
                /* Can't kill directly */
                return false;
            }
        }

        dstCell.setHolder(srcCell.getHolder());
        srcCell.setHolder(null);

        return true;
    }

    // ----------------- Board-based Utility Methods ----------------

    /* Multiply by 90 to get angle */
    public static final int UP = 3;
    public static final int LEFT = 2;
    public static final int DOWN = 1;
    public static final int RIGHT = 0;

    public static int loopDirection(int targetRow, int targetCol) {
        if(!Board.isNotCornerButEdgeCell(targetRow, targetCol, 6, 6)) {
            return -1;
        }

        if(targetRow == 0) {
            return UP;
        } else if(targetCol == 0) {
            return LEFT;
        } else if(targetRow == 5) {
            return DOWN;
        } else {
            return RIGHT;
        }
    }

    public static int cornerToLoopableCellDirection(int targetRow, int targetCol) {
        if (targetCol == 1 || targetCol == 2) {
            return RIGHT;
        } else if (targetCol == 4 || targetCol == 3) {
            return LEFT;
        } else if (targetRow == 1 || targetRow == 2) {
            return DOWN;
        } else { // targetRow == 4
            return UP;
        }
    }

    public static int reverseDirection(int dir) {
        switch(dir) {
            case UP: return DOWN;
            case DOWN: return UP;
            case RIGHT: return LEFT;
            case LEFT: return RIGHT;
            default: throw new IllegalArgumentException("dir should be UP, RIGHT, LEFT, DOWN");
        }
    }

    public static int longMoveAtLoopableCellDirection(int row, int col) {
        if (row == 0) {
            return DOWN;
        } else if (col == 0) {
            return RIGHT;
        } else if (row == 5) {
            return UP;
        } else {
            return LEFT;
        }
    }

    public static int cornerToLoopableCellDistance(int targetRow, int targetCol) {
        final long v = CompactLoc2D.encode(targetRow, targetCol);
        for (RoundConnector ia : innerArcs) {
            if (ia.hasCoordinates(v))
                return 1;
        }
        for (RoundConnector oa : outerArcs) {
            if (oa.hasCoordinates(v))
                return 2;
        }

        throw new RuntimeException("Not loopable: " + targetRow + ", " + targetCol);
    }

}
