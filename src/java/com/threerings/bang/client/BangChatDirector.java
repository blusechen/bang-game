//
// $Id$

package com.threerings.bang.client;

import java.util.Iterator;
import java.util.StringTokenizer;

import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.Config;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.util.BangContext;

/**
 * Handles custom chat bits for Bang.
 */
public class BangChatDirector extends ChatDirector
{
    public BangChatDirector (BangContext ctx)
    {
        super(ctx, ctx.getMessageManager(), BangCodes.CHAT_MSGS);
        _ctx = ctx;

        // register our user chat command handlers
        MessageBundle msg = _msgmgr.getBundle(_bundle);
        registerCommandHandler(msg, "debug", new DebugHandler());
        registerCommandHandler(msg, "tell", new TellHandler());
    }

    /** A place for temporary debug hacks. */
    protected class DebugHandler extends CommandHandler
    {
        public String handleCommand (SpeakService speakSvc, String command,
                                     String args, String[] history)
        {
            String[] argv = StringUtil.split(args, " ");
            if (argv[0].equals("slowmo")) {
                if (Config.display.animationSpeed == 1) {
                    Config.display.animationSpeed = 0.25f;
                } else {
                    Config.display.animationSpeed = 1f;
                }

            } else if (argv[0].equals("hfloat")) {
                Config.display.floatHighlights = !Config.display.floatHighlights;

            } else if (argv[0].equals("stats")) {
                _ctx.getApp().displayStatistics(
                    !_ctx.getApp().showingStatistics());

            } else {
                return "m.unknown_debug";
            }

            displayFeedback(
                _bundle, MessageBundle.tcompose("m.debug_toggled", argv[0]));

            return ChatCodes.SUCCESS;
        }
    }

    /** Implements <code>/tell</code>. */
    protected class TellHandler extends CommandHandler
    {
        public String handleCommand (
            SpeakService speakSvc, final String command, String args,
            String[] history)
        {
            // there should be at least two arg tokens: '/tell target word'
            StringTokenizer tok = new StringTokenizer(args);
            if (tok.countTokens() < 2) {
                return "m.usage_tell";
            }

            // now strip off everything up to the handle for the message
            String handle = tok.nextToken();
            int uidx = args.indexOf(handle);
            String message = args.substring(uidx + handle.length()).trim();
            if (StringUtil.isBlank(message)) {
                return "m.usage_tell";
            }

            // make sure we're not trying to tell something to ourselves
            Handle target = new Handle(handle);
            PlayerObject self = _ctx.getUserObject();
            if (self.handle.equals(target)) {
                return "m.talk_self";
            }

            // clear out from the history any tells that are mistypes
            for (Iterator iter = _history.iterator(); iter.hasNext(); ) {
                String hist = (String) iter.next();
                if (hist.startsWith("/" + command) &&
                    (new StringTokenizer(hist).countTokens() > 2)) {
                    iter.remove();
                }
            }

            // mogrify the chat
            message = mogrifyChat(message);

            // store the full command in the history, even if it was mistyped
            final String histEntry = command + " " + target + " " + message;
            history[0] = histEntry;

            // request to send this text as a tell message
            requestTell(target, message, new ResultListener() {
                public void requestCompleted (Object result) {
                    // replace the full one in the history with just
                    // "/tell <handle>"
                    String newEntry = "/" + command + " " + result + " ";
                    _history.remove(newEntry);
                    int dex = _history.lastIndexOf("/" + histEntry);
                    if (dex >= 0) {
                        _history.set(dex, newEntry);
                    } else {
                        _history.add(newEntry);
                    }
                }
                public void requestFailed (Exception cause) {
                    // do nothing
                }
            });

            return ChatCodes.SUCCESS;
        }
    }

    protected BangContext _ctx;
}
