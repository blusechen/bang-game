//
// $Id$

package com.threerings.bang.server;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.Tuple;

import com.threerings.parlor.rating.util.Percentiler;

import com.threerings.bang.server.persist.RatingRepository;
import com.threerings.bang.server.persist.RatingRepository.TrackerKey;

import static com.threerings.bang.Log.*;

/**
 * Manages rating bits.
 */
public class RatingManager
{
    /**
     * Prepares the rating manager for operation.
     */
    public void init (ConnectionProvider conprov)
        throws PersistenceException
    {
        _ratingrepo = new RatingRepository(conprov);
        
        // load up our scoring percentile trackers
        _trackers = new HashMap<TrackerKey, Percentiler>();
        _ratingrepo.loadScoreTrackers(_trackers);
        
        // if we're a town server, queue up an interval to periodically grind
        // our ratings tables and one to sync our score trackers
        if (ServerConfig.isTownServer) {
            createRankRecalculateInterval();
            createTrackerSyncInterval();
        }
    }
    
    /**
     * Allows the rating manager to save its state to the database.
     */
    public void shutdown ()
    {
        log.info("Rating manager shutting down.");
        syncTrackers();
    }
    
    /**
     * Returns the percentile occupied by the specified score value in the
     * specified scenario with the given number of players.
     *
     * @param record if true, the score will be recorded to the percentiler as
     * a data point.
     */
    public int getPercentile (
        String scenario, int players, int score, boolean record)
    {
        TrackerKey tkey = new TrackerKey(scenario, players);
        Percentiler tracker = _trackers.get(tkey);
        if (tracker == null) {
            tracker = new Percentiler();
            _trackers.put(tkey, tracker);
        }

        int pct = tracker.getPercentile(score);
        if (record) {
            tracker.recordValue(score);
        }
        return pct;
    }
    
    /**
     * Creates the interval that regrinds our ratings table and produces the
     * rank distributions every six hours.
     */
    protected void createRankRecalculateInterval ()
    {
        final Invoker.Unit grinder = new Invoker.Unit("rankGrinder") {
            public boolean invoke () {
                try {
                    log.info("Recalculating rankings...");
                    _ratingrepo.calculateRanks();
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to recalculate ranks.", pe);
                }
                return false;
            }
        };

        // regrind 5 minutes after reboot and then every six hours
        new Interval(BangServer.omgr) {
            public void expired () {
                BangServer.invoker.postUnit(grinder);
            }
        }.schedule(5 * 60 * 1000L, 6 * 60 * 60 * 1000L);
    }
    
    /**
     * Creates the interval that synchronizes our score trackers with the
     * database every hour.
     */
    protected void createTrackerSyncInterval ()
    {
        // resync every hour
        new Interval(BangServer.omgr) {
            public void expired () {
                syncTrackers();
            }
        }.schedule(60 * 60 * 1000L);
    }
    
    /**
     * Stores the trackers in the database.
     */
    protected void syncTrackers ()
    {
        final int tcount = _trackers.size();
        final TrackerKey[] keys = new TrackerKey[tcount];
        final Percentiler[] tilers = new Percentiler[tcount];
        int ii = 0;
        for (Map.Entry<TrackerKey, Percentiler> entry : _trackers.entrySet()) {
            keys[ii] = entry.getKey();
            tilers[ii++] = entry.getValue();
        }
        
        // write out the performance distributions
        BangServer.invoker.postUnit(new Invoker.Unit("storeScoreTrackers") {
            public boolean invoke () {
                for (int ii = 0; ii < tcount; ii++) {
                    try {
                        _ratingrepo.storeScoreTracker(keys[ii], tilers[ii]);
                    } catch (PersistenceException pe) {
                        log.warning("Error storing perf dist [scenario=" +
                            keys[ii].scenario + ", players=" +
                            keys[ii].players + ", error=" + pe + "].");
                    }
                }
                return false;
            }
        });
    }
    
    /** Provides access to the rating database. */
    protected RatingRepository _ratingrepo;
    
    /** Score percentile trackers. */
    protected HashMap<TrackerKey, Percentiler> _trackers;
}