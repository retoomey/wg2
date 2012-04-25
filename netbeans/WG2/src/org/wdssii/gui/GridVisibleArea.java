package org.wdssii.gui;

import java.awt.Rectangle;

/** Object representing a square section of visible table area.
 * Used to render product outlines for readout in the 3D world, also to render table
 * tracking.  Used to handle rectangle selection.
 * 
 *  @author Robert Toomey
 *  
 */
public class GridVisibleArea {

    /** Number of rows hight the grid is */
    public int numRows;

    /** Number of columns wide the grid is */
    public int numCols;

    /** Index of the first visible row on screen */
    public int startRow;

    /** Index of first visible column on screen */
    public int startCol; 

    /* Index of last fully (non-clipped row on screen */
    public int lastFullRow;

    /* Index of last clipped row on screen */
    public int lastPartialRow;

    /* Index of last fully (non-clipped column on screen */
    public int lastFullColumn;

    /* Index of last clipped column on screen */
    public int lastPartialColumn;

    /** Possible clipping region of the viewport */
    public Rectangle clipBounds;

    /** Active flag if selection set */
    public boolean active;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(numRows);
        builder.append(",");
        builder.append(numCols);
        builder.append(") Col:");
        builder.append(startCol);
        builder.append(", ");
        builder.append(lastFullColumn);
        builder.append(", ");
        builder.append(lastPartialColumn);
        builder.append(", Row:");
        builder.append(startRow);
        builder.append(", ");
        builder.append(lastFullRow);
        builder.append(", ");
        builder.append(lastPartialRow);

        return builder.toString();
    }
}