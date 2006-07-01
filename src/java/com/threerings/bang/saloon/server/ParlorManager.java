//
// $Id$

package com.threerings.bang.saloon.server;

import java.util.HashSet;
import java.util.logging.Level;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Throttle;

import com.threerings.media.util.MathUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;

import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.util.ScenarioUtil;

import com.threerings.bang.saloon.client.ParlorService;
import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.ParlorMarshaller;
import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonCodes;

import static com.threerings.bang.Log.log;

/**
 * Manages a back parlor room.
 */
public class ParlorManager extends PlaceManager
    implements SaloonCodes, ParlorProvider
{
    /**
     * Called by the {@link SaloonManager} after creating this back parlor.
     */
    public void init (SaloonManager salmgr, ParlorInfo info, String password)
    {
        _salmgr = salmgr;
        _parobj.setInfo(info);
        _password = password;
        log.info("Parlor created " + info + ".");
    }

    /**
     * Ratifies the entry of the supplied player. Throws an invocation
     * exception explaining the reason for rejection if they do not meet the
     * entry requirements.
     */
    public void ratifyEntry (PlayerObject user, String password)
        throws InvocationException
    {
        // if this player is the creator, or an admin, let 'em in regardless
        if (user.handle.equals(_parobj.info.creator) ||
            user.tokens.isAdmin()) {
            return;
        }

        // make sure the password matches if we have a password
        if (_parobj.info.passwordProtected &&
            !password.equalsIgnoreCase(_password)) {
            throw new InvocationException(INCORRECT_PASSWORD);
        }

        // make sure they're a pardner of the creator if that is required
        if (_parobj.info.pardnersOnly) {
            PlayerObject creator =
                BangServer.lookupPlayer(_parobj.info.creator);
            if (creator == null) {
                throw new InvocationException(CREATOR_NOT_ONLINE);
            }
            if (!creator.pardners.containsKey(user.handle)) {
                throw new InvocationException(NOT_PARDNER);
            }
        }
    }

    // documentation inherited from interface ParlorProvider
    public void updateParlorConfig (
        ClientObject caller, ParlorInfo info, boolean onlyCreatorStart)
    {
        PlayerObject user = (PlayerObject)caller;
        if (user.handle.equals(_parobj.info.creator)) {
            _parobj.startTransaction();
            try {
                info.creator = _parobj.info.creator;
                info.occupants = _parobj.occupantInfo.size();
                if (!_parobj.info.equals(info)) {
                    _parobj.setInfo(info);
                    _salmgr.parlorUpdated(info);
                }
                _parobj.setOnlyCreatorStart(onlyCreatorStart);
            } finally {
                _parobj.commitTransaction();
            }
        }
    }

    // documentation inherited from interface ParlorProvider
    public void updateParlorPassword (ClientObject caller, String password)
    {
        PlayerObject user = (PlayerObject)caller;
        if (user.handle.equals(_parobj.info.creator)) {
            _password = password;
        }
    }

    // documentation inherited from interface ParlorProvider
    public void updateGameConfig (ClientObject caller, ParlorGameConfig game)
    {
        // if we're already matchmaking, reject any config updates
        if (_parobj.playerOids != null) {
            return;
        }

        // otherwise just make sure they have the necessary privileges
        PlayerObject user = (PlayerObject)caller;
        if (user.handle.equals(_parobj.info.creator) ||
            !_parobj.onlyCreatorStart) {
            _parobj.setGame(game);
        }
    }

    // documentation inherited from interface ParlorProvider
    public void startMatchMaking (
        ClientObject caller, ParlorGameConfig game, byte[] bdata,
        ParlorService.InvocationListener listener)
        throws InvocationException
    {
        // if we're not allowing new games, fail immediately
        if (!RuntimeConfig.server.allowNewGames) {
            throw new InvocationException(SaloonCodes.NEW_GAMES_DISABLED);
        }
        PlayerObject user = (PlayerObject)caller;

        // if we've already started, then just turn this into a join
        if (_parobj.playerOids != null) {
            joinMatch(caller);

        } else {
            // sanity check the configuration
            int minPlayers = (game.tinCans > 0) ? 1 : 2;
            game.players = MathUtil.bound(
                minPlayers, game.players, GameCodes.MAX_PLAYERS);
            game.rounds = MathUtil.bound(
                1, game.rounds, GameCodes.MAX_ROUNDS);
            game.teamSize = MathUtil.bound(
                1, game.teamSize, GameCodes.MAX_TEAM_SIZE);
            game.tinCans = MathUtil.bound(
                0, game.tinCans, GameCodes.MAX_PLAYERS - game.players);
            if (game.scenarios == null || game.scenarios.length == 0) {
                game.scenarios = ScenarioUtil.getScenarios(ServerConfig.townId);
            }

            // update the game config with the desired config
            _parobj.setGame(game);

            // if this player is an admin, allow the board data
            if (user.tokens.isAdmin()) {
                _bdata = bdata;
            }

            // create a playerOids array and stick the starter in slot zero
            int[] playerOids = new int[game.players];
            playerOids[0] = caller.getOid();
            _parobj.setPlayerOids(playerOids);

            // start a "start the game" timer if we're ready to go
            checkStart();
        }
    }

    // documentation inherited from interface ParlorProvider
    public void joinMatch (ClientObject caller)
    {
        // make sure the match wasn't cancelled
        if (_parobj.playerOids == null) {
            return;
        }

        // look for a spot, and make sure they're not already joined
        PlayerObject user = (PlayerObject)caller;
        int idx = -1;
        for (int ii = 0; ii < _parobj.playerOids.length; ii++) {
            if (idx == -1 && _parobj.playerOids[ii] == 0) {
                idx = ii;
            }
            if (_parobj.playerOids[ii] == user.getOid()) {
                // abort!
                return;
            }
        }

        if (idx != -1) {
            _parobj.playerOids[idx] = user.getOid();
            _parobj.setPlayerOids(_parobj.playerOids);
        }

        // start a "start the game" timer if we're ready to go
        checkStart();
    }

    // documentation inherited from interface ParlorProvider
    public void leaveMatch (ClientObject caller)
    {
        // make sure the match wasn't already cancelled
        if (_parobj.playerOids == null) {
            return;
        }

        // clear this player out of the list
        PlayerObject user = (PlayerObject)caller;
        int remain = 0;
        for (int ii = 0; ii < _parobj.playerOids.length; ii++) {
            if (_parobj.playerOids[ii] == user.getOid()) {
                _parobj.playerOids[ii] = 0;
            } else if (_parobj.playerOids[ii] > 0) {
                remain++;
            }
        }

        // either remove this player or cancel the match, depending
        if (remain == 0) {
            _parobj.setPlayerOids(null);
        } else {
            _parobj.setPlayerOids(_parobj.playerOids);
        }

        // cancel our game start timer
        if (_starter != null) {
            _parobj.setStarting(false);
            _starter.cancel();
            _starter = null;
        }
    }

    @Override // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return ParlorObject.class;
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        return 5 * 1000L;
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        _parobj = (ParlorObject)_plobj;
        _parobj.setService((ParlorMarshaller)
                           BangServer.invmgr.registerDispatcher(
                               new ParlorDispatcher(this), false));
    }

    @Override // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();

        // let the saloon manager know that we're audi
        _salmgr.parlorDidShutdown(this);

        log.info("Parlor shutdown " + _parobj.info + ".");

        // clear out our invocation service
        if (_parobj != null) {
            BangServer.invmgr.clearDispatcher(_parobj.service);
            _parobj = null;
        }
    }

    @Override // documentation inherited
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        // update our occupant count in the saloon
        publishOccupants();
    }

    @Override // documentation inherited
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // update our occupant count in the saloon
        publishOccupants();
    }

    protected void publishOccupants ()
    {
        if (!_throttle.throttleOp()) {
            _parobj.info.occupants = _parobj.occupantInfo.size();
            _salmgr.parlorUpdated(_parobj.info);
        }
    }

    protected void checkStart ()
    {
        if (readyToStart() && _starter == null) {
            _parobj.setStarting(true);
            _starter = new Interval(BangServer.omgr) {
                public void expired () {
                    if (_starter != this) {
                        return;
                    }
                    _starter = null;
                    if (readyToStart()) {
                        log.info("Starting " + _parobj.game + ".");
                        startMatch();
                    }
                }
            };
            _starter.schedule(SaloonManager.START_DELAY);
        }
    }

    protected boolean readyToStart ()
    {
        return (IntListUtil.indexOf(_parobj.playerOids, 0) == -1);
    }

    protected void startMatch ()
    {
        BangConfig config = new BangConfig();

        // we can use these values directly as we sanity checked them earlier
        config.seats = _parobj.game.players + _parobj.game.tinCans;
        config.players = new Handle[config.seats];
        config.teamSize = _parobj.game.teamSize;
        config.scenarios = new String[_parobj.game.rounds];

        // back parlor games are never rated
        config.rated = false;

        // select our scenarios
        for (int ii = 0; ii < config.scenarios.length; ii++) {
            config.scenarios[ii] =
                RandomUtil.pickRandom(_parobj.game.scenarios);
        }

        // fill in the human players
        for (int ii = 0; ii < _parobj.playerOids.length; ii++) {
            PlayerObject user = (PlayerObject)
                BangServer.omgr.getObject(_parobj.playerOids[ii]);
            if (user == null) {
                log.warning("Zoiks! Missing player for parlor match " +
                            "[game=" + _parobj.game +
                            ", oid=" + _parobj.playerOids[ii] + "].");
                return; // abandon ship
            }
            config.players[ii] = user.handle;
        }

        // add our ais (if any)
        config.ais = new BangAI[config.seats];
        HashSet<String> names = new HashSet<String>();
        for (int ii = _parobj.playerOids.length; ii < config.ais.length; ii++) {
            // TODO: sort out personality and skill
            BangAI ai = BangAI.createAI(1, 50, names);
            config.ais[ii] = ai;
            config.players[ii] = ai.handle;
        }

        // if we're using a custom board, get that in there
        config.bdata = _bdata;

        try {
            BangServer.plreg.createPlace(config, null);
        } catch (Exception e) {
            log.log(Level.WARNING, "Choked creating game " + config + ".", e);
        }

        // and clear out our parlor bits
        _parobj.setStarting(false);
        _parobj.setPlayerOids(null);
        _parobj.setGame(null);
        _bdata = null;
    }

    protected ParlorObject _parobj;
    protected SaloonManager _salmgr;
    protected byte[] _bdata;
    protected String _password;
    protected Interval _starter;
    protected Throttle _throttle = new Throttle(1, 10);
}
