//
// $Id$

package com.threerings.bang.game.server;

import com.threerings.bang.game.client.BangService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link BangService}.
 */
public interface BangProvider extends InvocationProvider
{
    /**
     * Handles a {@link BangService#cancelOrder} request.
     */
    void cancelOrder (ClientObject caller, int arg1);

    /**
     * Handles a {@link BangService#getBoard} request.
     */
    void getBoard (ClientObject caller, BangService.BoardListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link BangService#order} request.
     */
    void order (ClientObject caller, int arg1, short arg2, short arg3, int arg4, InvocationService.ResultListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link BangService#playCard} request.
     */
    void playCard (ClientObject caller, int arg1, Object arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link BangService#reportPerformance} request.
     */
    void reportPerformance (ClientObject caller, String arg1, String arg2, int[] arg3);

    /**
     * Handles a {@link BangService#selectTeam} request.
     */
    void selectTeam (ClientObject caller, int arg1, String[] arg2, int[] arg3);
}