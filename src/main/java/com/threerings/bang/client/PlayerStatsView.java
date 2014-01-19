//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.TableLayout;

import com.threerings.stats.data.Stat;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;
import com.threerings.bang.util.BangContext;

/**
 * Displays a player's accumulated statistics.
 */
public class PlayerStatsView extends BContainer
{
    public PlayerStatsView (BangContext ctx)
    {
        super(new TableLayout(2, 5, 5));
        setStyleClass("stats_view");
        _ctx = ctx;
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // clear out and refresh our stats labels
        removeAll();
        PlayerObject user = _ctx.getUserObject();
        Stat[] stats = user.stats.toArray(new Stat[user.stats.size()]);
        // TODO: sort on translated key
        for (int ii = 0; ii < stats.length; ii++) {
            StatType type = (StatType)stats[ii].getType();
            boolean hidden = type.isHidden();
            if (hidden && !_ctx.getUserObject().tokens.isAdmin()) {
                continue;
            }
            add(new BLabel(hidden ? type.key() : _ctx.xlate(BangCodes.STATS_MSGS, type.key())));
            add(new BLabel(stats[ii].valueToString()));
        }
    }

    protected BangContext _ctx;
}