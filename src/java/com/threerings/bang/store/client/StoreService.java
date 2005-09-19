//
// $Id$

package com.threerings.bang.store.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides invocation services relating to the General Store.
 */
public interface StoreService extends InvocationService
{
    /**
     * Issues a request to purchase the specified good from the store.
     */
    public void buyGood (Client client, String type, ConfirmListener cl);
}
