package cityfreqs.com.pilfershush.assist;

import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;

import java.util.Arrays;

public class AppEntry {
    private String activityName;
    private String packageName;
    private int idNum;

    private boolean recordable;
    private boolean bootCheck;
    private boolean receivers;
    private boolean services;
    private boolean audioBeacon;

    private int servicesNum;
    private int receiversNum;

    private ServiceInfo[] serviceInfo;
    private ActivityInfo[] receiversInfo;
    private String[] beaconServices;
    private String[] beaconReceivers;

    public AppEntry(String packageName, String activityName) {
        this.packageName = packageName;
        this.activityName = activityName;
        // defaults
        recordable = false;
        bootCheck = false;
        receivers = false;
        services = false;
        audioBeacon = false;
        servicesNum = 0;
        receiversNum = 0;
    }

    /********************************************************************/
/*
 *
 */

    public String getActivityName() {
        return activityName;
    }
    public void setIdNum(int idNum) {
        this.idNum = idNum;
    }

    public int getServicesNum() {
        return servicesNum;
    }
    public int getReceiversNum() {
        return receiversNum;
    }

    public void setRecordable(boolean recordable) {
        this.recordable = recordable;
    }
    public boolean getRecordable() {
        return recordable;
    }

    public void setBootCheck(boolean bootCheck) {
        this.bootCheck = bootCheck;
    }

    public void setReceivers(boolean receivers) {
        this.receivers = receivers;
    }

    public void setServices(boolean services) {
        this.services = services;
    }
    public boolean getServices() {
        return services;
    }

    public boolean checkForCaution() {
        // set and return,
        // called by BackgroundChecker.appEntryLog(),
        // later boolean is checked for in-depth scanning of services, receivers, etc.
        return ((recordable) && (bootCheck) && (receivers) && (services));
    }

    public void setAudioBeacon(boolean audioBeacon) {
        this.audioBeacon = audioBeacon;
    }

    /*
    public boolean getAudioBeacon() {
        return audioBeacon;
    }
    */

    public boolean checkBeaconServiceNames() {
        return (beaconServices != null && beaconServices.length > 0);
    }

    public int getBeaconServiceNamesNum() {
        if (beaconReceivers != null)
            return beaconServices.length;
        else
            return 0;
    }

    public String[] getBeaconServiceNames() {
        if (beaconReceivers != null)
            return beaconServices;
        else
            return null;
    }

    public boolean checkBeaconReceiverNames() {
        return (beaconReceivers != null && beaconReceivers.length > 0);
    }

    public int getBeaconReceiverNamesNum() {
        if (beaconReceivers != null)
            return beaconReceivers.length;
        else
            return 0;
    }

    public String[] getBeaconReceiverNames() {
        if (beaconReceivers != null)
            return beaconReceivers;
        else
            return null;
    }


    /********************************************************************/
/*
 *  arrays
 */
    public void setServiceInfo(ServiceInfo[] serviceInfo) {
        // any background recording service list
        this.serviceInfo = new ServiceInfo[serviceInfo.length];
        this.serviceInfo = Arrays.copyOf(serviceInfo, serviceInfo.length);
        servicesNum = this.serviceInfo.length;
        services = true;
    }

    public void setActivityInfo(ActivityInfo[] receiversInfo) {
        // receivers list derived from activityInfo
        this.receiversInfo = new ActivityInfo[receiversInfo.length];
        this.receiversInfo = Arrays.copyOf(receiversInfo, receiversInfo.length);
        receiversNum = this.receiversInfo.length;
        receivers = true;
    }

    /********************************************************************/
/*
 * methods
 */
    public String entryPrint() {
        return idNum + " : " + activityName + "\n" + packageName + "\nRECORD: " + recordable +
                "\nBOOT: " + bootCheck + "\nSERVICES: " + services +
                "\nRECEIVERS: " + receivers + "\nNUHF/ACR SDK: " + audioBeacon +
                "\n--------------------------------------\n";
    }

    private boolean isSdkName(String nameQuery) {
        for (String name : AudioSettings.SDK_NAMES) {
            if (nameQuery.contains(name))
                return true;
        }
        return false;
    }

    // also has:
    // .packageName
    // .processName
    // .permission
    public String[] getServiceNames() {
        beaconServices = new String[serviceInfo.length];
        int i = 0;

        String[] names = new String[serviceInfo.length];
        for (int j = 0; j < serviceInfo.length; j++) {
            // get service name
            names[j] = serviceInfo[j].name;
            if (isSdkName(names[j])) {
                beaconServices[i] = names[j];
                i++;
            }
        }
        return names;
    }

    // also has (is ActivityInfo...) :
    // .packageName;
    // .applicationInfo;
    // .processName
    // .targetActivity
    public String[] getReceiverNames() {
        beaconReceivers = new String[receiversInfo.length];
        int i = 0;

        String[] names = new String[receiversInfo.length];
        for (int j = 0; j < receiversInfo.length; j++) {
            // get receiver name
            names[j] = receiversInfo[j].name;
            if (isSdkName(names[j])) {
                beaconReceivers[i] = names[j];
                i++;
            }
        }
        return names;
    }
}

