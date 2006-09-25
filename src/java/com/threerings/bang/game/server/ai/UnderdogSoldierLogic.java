//
// $Id$

package com.threerings.bang.game.server.ai;

import java.awt.Point;

import java.util.Arrays;
import java.util.ArrayList;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Handles controlling the Underdog Soldier.
 */
public class UnderdogSoldierLogic extends PieceLogic
{
    @Override // documentation inherited
    protected void moveUnit (
       Piece[] pieces, Unit unit, PointSet moves, PointSet attacks)
    {
        // determine each player's rank based on their points
        int[] spoints = _bangobj.points.clone();
        int[] rank = new int[_bangobj.players.length];
        Arrays.sort(spoints);
        ArrayUtil.reverse(spoints);
        for (int ii = 0; ii < rank.length; ii++) {
            for (int jj = 0; jj < spoints.length; jj++) {
                if (_bangobj.points[ii] == spoints[jj]) {
                    rank[ii] = jj;
                    break;
                }
            }
        }
        // Find the closests unit for each player
        TargetOption to = null;
        for (Piece p : pieces) {
            if (p.isTargetable() && unit.validTarget(_bangobj, p, false) &&
                    p.owner >= 0) {
                TargetOption option = new TargetOption(p, unit.getDistance(p), 
                        attacks.contains(p.x, p.y), rank[p.owner]);
                if (to == null) {
                    to = option;
                } else {
                    if (to.compareTo(option) > 0) {
                        to = option;
                    }
                }
            }
        }
        if (to != null) {
            Piece target = to.target;
            if (attacks.contains(target.x, target.y)) {
                executeOrder(unit, Short.MAX_VALUE, 0, target);
                return;
            } else {
                Point dest = getClosestPoint(unit, moves, target.x, target.y);
                if (dest != null) {
                    executeOrder(unit, dest.x, dest.y, null);
                    return;
                }
            }
        }
        
        // otherwise just move randomly
        if (moves.size() == 0) {
            return;
        }
        int midx = RandomUtil.getInt(moves.size());
        executeOrder(unit, moves.getX(midx), moves.getY(midx), null);
    }

    /** A Helper class for finding the optimal target. */
    protected class TargetOption
        implements Comparable
    {
        public Piece target;
        public int score;

        public TargetOption (
                Piece target, int dist, boolean attackable, int rank)
        {
            this.target = target;
            score = rank * 6 + (attackable ? 0 : dist);
        }

        public int compareTo (Object o)
        {
            TargetOption to = (TargetOption)o;
            int compare = this.score - to.score;
            return (compare != 0 ? compare : target.damage - to.target.damage);
        }
    }
}
