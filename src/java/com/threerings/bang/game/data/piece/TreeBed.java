//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TreeBedSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.FetishEffect;
import com.threerings.bang.game.data.effect.TreeBedEffect;

/**
 * A tree bed that grows a tree in distinct phases, with the damage keeping
 * track of progress to the next phase.
 */
public class TreeBed extends Prop
{
    /** The final phase of the tree's growth. */
    public static final byte FULLY_GROWN = 3;

    /** The current growth phase of the tree, from 0 to FULLY_GROWN. */
    public byte growth;
    
    public TreeBed ()
    {
        damage = 50;
    }
    
    @Override // documentation inherited
    public void init ()
    {
        damage = 50;
        growth = 0;
    }
        
    @Override // documentation inherited
    public float getHeight ()
    {
        // sprite is positioned according to board height, so make sure
        // the piece itself doesn't contribute
        return 0f;
    }
    
    @Override // documentation inherited
    public boolean expireWreckage (short tick)
    {
        return false;
    }

    /**
     * "Damages" this tree bed by the specified amount, causing it to grow
     * higher if the increment is negative and lower if the increment is
     * positive.
     */
    public void damage (int dinc)
    {
        damage += dinc;
        if (damage <= 0 && growth < FULLY_GROWN) {
            growth++;
            damage = 50;
        } else {
            damage = Math.max(Math.min(damage, 100), 0);
        }
    }
    
    @Override // documentation inherited
    public ArrayList<Effect> tick (
            short tick, BangObject bangobj, Piece[] pieces)
    {
        // can't heal dead trees
        ArrayList<Effect> effects = new ArrayList<Effect>();
        if (!isAlive()) {
            if (tick - lastActed > RESURRECTION_TICKS_PER_LEVEL * growth) {
                effects.add(new TreeBedEffect(this, 50, 0));
                return effects;
            }
            return null;
        }

        // normal units cause the tree to grow; logging robots cause it to
        // shrink
        int dinc = 0, ddec = 0;
        boolean doubleGrowth = false;
        ArrayList<Piece> growers = new ArrayList<Piece>();
        for (Piece piece : pieces) {
            if (piece instanceof Unit && getDistance(piece) == 1 &&
                piece.isAlive()) {
                Unit unit = (Unit)piece;
                if (FetishEffect.FROG_FETISH.equals(unit.holding)) {
                    doubleGrowth = true;
                }
                int pdamage = unit.getTreeProximityDamage(this);
                if (pdamage > 0) {
                    if (growth > 0) { // no hurting sprouts
                        dinc += pdamage;
                    }
                } else {
                    ddec += pdamage;
                }
                growers.add(piece);
            }
        }
        int tdamage = dinc + ddec * (doubleGrowth ? 2 : 1);
        if ((dinc == 0 && ddec == 0) || (growth == FULLY_GROWN &&
            damage == 0 && tdamage < 0)) {
            return null;
        }
        effects.add(new TreeBedEffect(this,
            growers.toArray(new Piece[growers.size()]), tdamage));
        return effects;
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TreeBedSprite();
    }

    /** The number of ticks it takes per level grown for a stump to turn back
     * into a living bed. */
    protected static final int RESURRECTION_TICKS_PER_LEVEL = 2;
}
