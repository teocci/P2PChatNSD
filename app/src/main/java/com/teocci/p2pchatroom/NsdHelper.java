package com.teocci.p2pchatroom;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;

import com.teocci.p2pchatroom.interfaces.NsdHelperListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class NsdHelper
{

    private Context context;
    private NsdManager manager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.RegistrationListener registrationListener;
    private NsdManager.ResolveListener resolveListener;
    private String serviceName = "NULL";
    private AtomicInteger registrationCount = new AtomicInteger(0);

    // The same IP cannot initiate too many discovery request or it will fail with MAX_LIMIT_REACHED
    private AtomicInteger discoverCount = new AtomicInteger(0);

    public synchronized String getServiceName()
    {
        return serviceName;
    }

    public synchronized void setServiceName(String serviceName)
    {
        this.serviceName = serviceName;
    }

    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String TAG = "NsdHelper";
    private static final String mServiceName = "P2PChatRoom";

    private Handler uiHandler;
    private List<NsdServiceInfo> serviceList;
    private NsdHelperListener mainThreadListener;

    List<NsdServiceInfo> getServiceList()
    {
        // Does shallow copy but work for non-synchronized collection
        return new ArrayList<>(serviceList);
    }

    //returns whether current machine is registered or not on Nsd
    boolean isRegistered()
    {
        return (registrationCount.get() > 0);
    }

    //return whether discovery mode is currently on
    boolean isDiscovering()
    {
        return (discoverCount.get() > 0);
    }

    public NsdHelper(Context ctx, Handler hdl, NsdHelperListener lnr)
    {
        context = ctx;
        uiHandler = hdl;
        serviceList = Collections.synchronizedList(new ArrayList<NsdServiceInfo>());
        mainThreadListener = lnr;
        manager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        initNsd();
    }

    public void emptyServiceList()
    {
        serviceList.clear();
    }

    private void debug(final String msg)
    {
        mainThreadListener.outputDebugMessage(msg);
    }

    private void initNsd()
    {
        initDiscoveryListener();
        initRegistrationListener();
        initResolveListener();
    }

    private void initResolveListener()
    {

        resolveListener = new NsdManager.ResolveListener()
        {
            @Override
            public void onServiceResolved(final NsdServiceInfo serviceInfo)
            {
                // when we resolved a service need to add it to the available list
                Log.d(TAG, "Service Resolution succeed for service = [ " + serviceInfo + " ]");
                debug("Service Resolution succeed for service = [ " + serviceInfo + " ]");
                serviceList.add(serviceInfo);
                //run on UI Thread
                uiHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mainThreadListener.notifyDiscoveredOneItem(serviceInfo);
                    }
                });
            }

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                Log.e(TAG, "onResolveFailed() .. errorCode = " + errorCode);
                debug("onResolveFailed() .. errorCode = " + errorCode);
            }
        };

    }

    private void initRegistrationListener()
    {
        registrationListener = new NsdManager.RegistrationListener()
        {

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                Log.e(TAG, "onRegistrationFailed() .. errorCode = " + errorCode);
                debug("onRegistrationFailed() .. errorCode = " + errorCode);
                manager.unregisterService(this);

            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo)
            {
                Log.d(TAG, "service name: [ " + serviceInfo.getServiceName() +
                        " ] has been unregistered successfully.");
                debug("service name: [ " + serviceInfo.getServiceName() +
                        " ] has been unregistered successfully.");
                //decrement registration count
                registrationCount.decrementAndGet();
                if (serviceName.equalsIgnoreCase(serviceInfo.getServiceName())) {
                    //here need to reset our private ServiceName to signal
                    //Machine is no longer broadcasting itself
                    //serviceName = "NULL";
                }
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo)
            {
                //make a copy of this String so later when we can compare this string to determine
                //if the current machine has been registered to NSD or not
                Log.d(TAG, "entered onServiceRegistered -- \n Registered ServiceInfo = [ " + serviceInfo + " ]");
                debug("entered onServiceRegistered -- \n Registered ServiceInfo = [ " + serviceInfo + " ]");
                serviceName = String.valueOf(serviceInfo.getServiceName());
                registrationCount.incrementAndGet();
                mainThreadListener.notifyRegistrationComplete();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                Log.e(TAG, "onRegistrationFailed() .. errorCode = " + errorCode);
                debug("onRegistrationFailed() .. errorCode = " + errorCode);
            }
        };

    }

    private void initDiscoveryListener()
    {
        discoveryListener = new NsdManager.DiscoveryListener()
        {

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode)
            {
                Log.e(TAG, "onStopDiscoveryFailed() .. errorCode = " + errorCode);
                debug("onStopDiscoveryFailed() .. errorCode = " + errorCode);
                //manager.stopServiceDiscovery(this);

            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode)
            {
                Log.e(TAG, "onStartDiscoveryFailed() .. errorCode = " + errorCode);
                debug("onStartDiscoveryFailed() .. errorCode = " + errorCode);
                //manager.stopServiceDiscovery(this);

            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo)
            {
                Log.e(TAG, "Service = [ " + serviceInfo + " ] is now lost..");
                debug("Service = [ " + serviceInfo + " ] is now lost..");
                //find serviceInfo in Array
                for (NsdServiceInfo nServ : serviceList) {
                    if (nServ.equals(serviceInfo)) {
                        serviceList.remove(nServ);
                        break;
                    }
                }
            }

            @Override
            public void onServiceFound(NsdServiceInfo service)
            {
                Log.d(TAG, "new service found via discovery");
                Log.d(TAG, "Service = [ " + service + " ]");
                debug("on service found: new Service = [ " + service + " ]");
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "unknown service type found!!");
                    debug("unknown service type found!!");
//              DEBUG  temp relax to allow self loop messaging
                } else if (service.getServiceName().equals(serviceName)) {
                    //arrived here because exact same machine who started Discovery Request found
                    Log.d(TAG, "Same machine found!!");
                    debug("Same machine found!!");
                } else if (service.getServiceName().contains(mServiceName)) {
                    //arrived here when diff machine using the same app is found through Discovery
                    Log.d(TAG, "found a new P2P service -[ " + service + " ]..");
                    //debug("found a new P2P service -[ "+service+" ]..");
                    // only resolve if it is the server
                    //if (service.getServiceName().equals(mServiceName))
                    manager.resolveService(service, resolveListener);
                }

            }

            @Override
            public void onDiscoveryStopped(String serviceType)
            {
                Log.d(TAG, "onDiscoveryStopped() called..");
                debug("onDiscoveryStopped() called..");
                discoverCount.decrementAndGet();

            }

            @Override
            public void onDiscoveryStarted(String serviceType)
            {
                Log.d(TAG, "service discovery started");
                debug("service discovery started");
                discoverCount.incrementAndGet();
            }
        };

    }

    public void registerService(int port)
    {
        Log.d(TAG, "entering registrerServier() on port " + port);
        debug("entering registrerServier() on port " + port);
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    public void discoverServices()
    {
        Log.d(TAG, "entering discoverServices()");
        debug("entering discoverServices()");
        manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void stopDiscoverServices()
    {
        Log.d(TAG, "entering stopDiscoverServices()");
        debug("entering stopDiscoverServices()");
        if (discoverCount.get() > 0)
            manager.stopServiceDiscovery(discoveryListener);
    }

    public void unRegisterService()
    {
        Log.d(TAG, "entering unregisterService()");
        debug("entering unregisterService()");
        if (registrationCount.get() > 0) {//meaning this service is still registered
            debug("REALLY unregisterService ... ");
            manager.unregisterService(registrationListener);
        }
    }
}
