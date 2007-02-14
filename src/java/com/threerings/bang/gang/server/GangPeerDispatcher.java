//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.GangPeerService;
import com.threerings.bang.gang.data.GangPeerMarshaller;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link GangPeerProvider}.
 */
public class GangPeerDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public GangPeerDispatcher (GangPeerProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new GangPeerMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case GangPeerMarshaller.ADD_TO_COFFERS:
            ((GangPeerProvider)provider).addToCoffers(
                source,
                (Handle)args[0], (String)args[1], ((Integer)args[2]).intValue(), ((Integer)args[3]).intValue(), (InvocationService.ConfirmListener)args[4]
            );
            return;

        case GangPeerMarshaller.CHANGE_MEMBER_RANK:
            ((GangPeerProvider)provider).changeMemberRank(
                source,
                (Handle)args[0], (Handle)args[1], ((Byte)args[2]).byteValue(), (InvocationService.ConfirmListener)args[3]
            );
            return;

        case GangPeerMarshaller.GRANT_NOTORIETY:
            ((GangPeerProvider)provider).grantNotoriety(
                source,
                (Handle)args[0], ((Integer)args[1]).intValue()
            );
            return;

        case GangPeerMarshaller.HANDLE_INVITE_RESPONSE:
            ((GangPeerProvider)provider).handleInviteResponse(
                source,
                (Handle)args[0], ((Integer)args[1]).intValue(), (Handle)args[2], ((Boolean)args[3]).booleanValue(), (InvocationService.ConfirmListener)args[4]
            );
            return;

        case GangPeerMarshaller.INVITE_MEMBER:
            ((GangPeerProvider)provider).inviteMember(
                source,
                (Handle)args[0], (Handle)args[1], (String)args[2], (InvocationService.ConfirmListener)args[3]
            );
            return;

        case GangPeerMarshaller.PROCESS_OUTFITS:
            ((GangPeerProvider)provider).processOutfits(
                source,
                (Handle)args[0], (OutfitArticle[])args[1], ((Boolean)args[2]).booleanValue(), ((Boolean)args[3]).booleanValue(), (InvocationService.ResultListener)args[4]
            );
            return;

        case GangPeerMarshaller.REMOVE_FROM_GANG:
            ((GangPeerProvider)provider).removeFromGang(
                source,
                (Handle)args[0], (Handle)args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case GangPeerMarshaller.SET_AVATAR:
            ((GangPeerProvider)provider).setAvatar(
                source,
                (AvatarInfo)args[0]
            );
            return;

        case GangPeerMarshaller.SET_STATEMENT:
            ((GangPeerProvider)provider).setStatement(
                source,
                (Handle)args[0], (String)args[1], (String)args[2], (InvocationService.ConfirmListener)args[3]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}