//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Handles the respawning of units.
 */
public class RespawnDelegate extends ScenarioDelegate
{
    /** The default number of ticks before a unit respawns. */
    public static final int RESPAWN_TICKS = 12;

    public RespawnDelegate ()
    {
        this(RESPAWN_TICKS);
    }

    public RespawnDelegate (int respawnTicks)
    {
        _respawnTicks = respawnTicks;
    }

    @Override // from ScenarioDelegate
    public void roundWillStart (BangObject bangobj)
    {
        // clear our respawn queue
        _respawns.clear();
    }

    @Override // from ScenarioDelegate
    public void tick (BangObject bangobj, short tick)
    {
        // respawn new pieces
        while (_respawns.size() > 0) {
            if (_respawns.peek().getRespawnTick() > tick) {
                break;
            }

            Unit unit = _respawns.poll();
            log.fine("Respawning " + unit + ".");

            // reassign the unit to its original owner
            unit.owner = unit.originalOwner;

            // figure out where to put this guy
            Point spot = _parent.getStartSpot(unit.owner);
            Point bspot = bangobj.board.getOccupiableSpot(spot.x, spot.y, 3);
            if (bspot == null) {
                log.warning("Unable to locate spawn spot for to-be-respawned " +
                            "unit [unit=" + unit + ", spot=" + spot + "].");
                // stick him back on the queue for a few ticks later
                unit.setRespawnTick((short)(tick + _respawnTicks));
                _respawns.add(unit);
                continue;
            }

            // reset the units vital statistics
            unit.damage = 0;
            unit.influence = null;
            unit.holding = null;
            unit.setRespawnTick((short)0);

            // if the unit is still in play for some reason, remove it first
            if (bangobj.pieces.containsKey(unit.getKey())) {
                bangobj.board.clearShadow(unit);
                bangobj.removeFromPieces(unit.getKey());
            }

            // then position it and add it back at its new location
            unit.position(bspot.x, bspot.y);
            bangobj.addToPieces(unit);
            bangobj.board.shadowPiece(unit);
        }
    }

    @Override // from ScenarioDelegate
    public void pieceWasKilled (BangObject bangobj, Piece piece)
    {
        if (!(piece instanceof Unit) || ((Unit)piece).originalOwner == -1) {
            return;
        }
        Unit unit = (Unit)piece;
        unit.setRespawnTick((short)(bangobj.tick + _respawnTicks));
        _respawns.add(unit);
        log.fine("Queued for respawn " + unit + ".");
    }

    /** A list of units waiting to be respawned. */
    protected PriorityQueue<Unit> _respawns = new PriorityQueue<Unit>(10,
            new Comparator<Unit>() {
                public int compare (Unit u1, Unit u2) {
                    return u1.getRespawnTick() - u2.getRespawnTick();
                }

                public boolean equals(Object obj) {
                    return false;
                }
            });

    /** The number of ticks that must elapse before a unit is respawned. */
    protected int _respawnTicks = RESPAWN_TICKS;
}
