package com.teocci.p2pchatroom.interfaces;

import android.net.nsd.NsdServiceInfo;

/**
 * Created by teocci on 2/17/17.
 *
 */
public interface NsdHelperListener
{
    public void notifyDiscoveredOneItem(NsdServiceInfo nsdItem);

    public void notifyRegistrationComplete();

    public void outputDebugMessage(final String msg);
}
