//
// $Id$

package com.threerings.bang.bounty.server;

import com.threerings.bang.bounty.client.OfficeService;
import com.threerings.bang.bounty.data.BountyGameConfig;
import com.threerings.bang.bounty.data.OfficeMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link OfficeProvider}.
 */
public class OfficeDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public OfficeDispatcher (OfficeProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new OfficeMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case OfficeMarshaller.TEST_BOUNTY_GAME:
            ((OfficeProvider)provider).testBountyGame(
                source,
                (BountyGameConfig)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
