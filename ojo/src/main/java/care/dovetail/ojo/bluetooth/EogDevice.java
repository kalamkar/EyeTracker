package care.dovetail.ojo.bluetooth;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by abhi on 5/23/17.
 */

public abstract class EogDevice {

    protected final Context context;
    private final Set<Observer> observers = new HashSet<>();

    public interface Observer {
        void onConnect(String name);

        void onDisconnect(String name);

        void onNewValues(int channel1, int channel2);
    }

    protected EogDevice(Context context) {
        this.context = context;
    }

    public final void add(Observer observer) {
        observers.add(observer);
    }

    public final void remove(Observer observer) {
        observers.remove(observer);
    }

    public abstract boolean isConnected();

    public abstract void close();

    public abstract void connect();

    protected final void onConnect(String name) {
        for (Observer observer : observers) {
            observer.onConnect(name);
        }
    }

    protected final void onDisconnect(String name) {
        for (Observer observer : observers) {
            observer.onDisconnect(name);
        }
    }

    protected final void onNewValues(int channel1, int channel2) {
        for (Observer observer : observers) {
            observer.onNewValues(channel1, channel2);
        }
    }
}
