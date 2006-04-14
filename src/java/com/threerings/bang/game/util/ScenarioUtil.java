//
// $Id$

package com.threerings.bang.game.util;

import java.util.HashMap;
import java.util.Iterator;

import com.threerings.openal.SoundGroup;
import com.threerings.util.RandomUtil;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.data.piece.Claim;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Contains scenario-related utilities.
 */
public class ScenarioUtil
{
    /**
     * Selects a random list of scenarios for the specified town.
     */
    public static String[] selectRandom (String townId, int count)
    {
        String[] avail = _scenmap.get(townId);
        String[] choices = new String[count];
        for (int ii = 0; ii < choices.length; ii++) {
            choices[ii] = (String)RandomUtil.pickRandom(avail);
        }
        return choices;
    }

    /**
     * Called on the client to preload any sounds for this scenario.
     */
    public static void preloadSounds (String scenarioId, SoundGroup sounds)
    {
        if (scenarioId.equals(ScenarioCodes.CLAIM_JUMPING) ||
            scenarioId.equals(ScenarioCodes.GOLD_RUSH) ||
            scenarioId.equals(ScenarioCodes.TUTORIAL)) {
            sounds.preloadClip("rsrc/bonuses/nugget/added.wav");
            sounds.preloadClip("rsrc/bonuses/nugget/removed.wav");
            sounds.preloadClip("rsrc/bonuses/nugget/pickedup.wav");
        }
    }

    /** Maps town ids to a list of valid gameplay scenarios. */
    protected static HashMap<String,String[]> _scenmap =
        new HashMap<String,String[]>();
    static {
        _scenmap.put(BangCodes.FRONTIER_TOWN,
                     ScenarioCodes.FRONTIER_TOWN_SCENARIOS);
        _scenmap.put(BangCodes.INDIAN_VILLAGE,
                     ScenarioCodes.INDIAN_VILLAGE_SCENARIOS);
        _scenmap.put(BangCodes.BOOM_TOWN,
                     ScenarioCodes.BOOM_TOWN_SCENARIOS);
        _scenmap.put(BangCodes.GHOST_TOWN,
                     ScenarioCodes.GHOST_TOWN_SCENARIOS);
        _scenmap.put(BangCodes.CITY_OF_GOLD,
                     ScenarioCodes.CITY_OF_GOLD_SCENARIOS);
    }
}
