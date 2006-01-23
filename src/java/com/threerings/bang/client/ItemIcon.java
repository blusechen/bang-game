//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Dimension;

import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.data.Item;
import com.threerings.bang.util.BangContext;

/**
 * Displays an icon and descriptive text for a particular inventory item.
 */
public class ItemIcon extends PaletteIcon
{
    public ItemIcon ()
    {
    }

    /** Returns the item associated with this icon. */
    public Item getItem ()
    {
        return _item;
    }

    /** Configures this icon with its associated item. */
    public ItemIcon setItem (BangContext ctx, Item item)
    {
        _item = item;
        configureLabel(ctx);
        return this;
    }

    protected void configureLabel (BangContext ctx)
    {
        setIcon(new ImageIcon(ctx.loadImage("ui/unknown_item.png")));
        setText(_item.toString());
    }

    protected Item _item;
}
