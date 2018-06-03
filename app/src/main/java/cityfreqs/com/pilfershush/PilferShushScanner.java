package cityfreqs.com.pilfershush;

import android.content.Context;

import cityfreqs.com.pilfershush.assist.AudioSettings;
import cityfreqs.com.pilfershush.assist.WriteProcessor;
import cityfreqs.com.pilfershush.scanners.AudioScanner;

public class PilferShushScanner {
    private Context context;
    private AudioSettings audioSettings;
    private BackgroundChecker backgroundChecker;
    private AudioChecker audioChecker;
    private AudioScanner audioScanner;
    private WriteProcessor writeProcessor;
    private AudioJammer audioJammer;
    private int scanBufferSize;

    protected void onDestroy() {
        audioChecker.destroy();
        backgroundChecker.destroy();
    }

    /********************************************************************/
/*
*
*/
    protected boolean initScanner(Context context, boolean hasUSB, String sessionName, boolean writeFiles, boolean writeWav) {
        this.context = context;
        scanBufferSize = 0;
        audioSettings = new AudioSettings(writeFiles);
        audioChecker = new AudioChecker(context, audioSettings);
        writeProcessor = new WriteProcessor(context, sessionName, audioSettings, writeWav);

        if (audioChecker.determineInternalAudioType()) {
            entryLogger(audioChecker.getAudioSettings().toString(), false);
            audioScanner = new AudioScanner(context, audioChecker.getAudioSettings());
            // get internalAudio settings here.
            initBackgroundChecks();
            // setup Jammer
            audioJammer = new AudioJammer(context, audioSettings);
            return true;
        }

        // TODO wont yet run usb audio, no return true, background checks...
        if(hasUSB) {
            if (audioChecker.determineUsbAudioType(hasUSB)) {
                MainActivity.logger(context.getString(R.string.usb_state_7));
            }
            else {
                MainActivity.logger(context.getString(R.string.usb_state_8));
            }
        }
        return false;
    }

    protected void setFreqMinMax(int pair) {
        // at the moment stick to ranges of 3kHz as DEFAULT or SECOND pair
        // use int as may get more ranges than the 2 presently used
        if (pair == 1) {
            audioSettings.setMinFreq(AudioSettings.DEFAULT_FREQUENCY_MIN);
            audioSettings.setMaxFreq(AudioSettings.DEFAULT_FREQUENCY_MAX);
        }
        else if (pair == 2) {
            audioSettings.setMinFreq(AudioSettings.SECOND_FREQUENCY_MIN);
            audioSettings.setMaxFreq(AudioSettings.SECOND_FREQUENCY_MAX);
        }
    }

    protected String getSaveFileType() {
        return audioSettings.saveFormatToString();
    }

    protected void setWriteFiles(boolean writeFiles) {
        audioSettings.setWriteFiles(writeFiles);
    }
    protected boolean getWriteFiles() {
        return audioSettings.getWriteFiles();
    }

    protected long getLogStorageSize() {
        return writeProcessor.getStorageSize();
    }

    protected long getFreeStorageSize() {
        return writeProcessor.getFreeStorageSpace();
    }

    protected boolean cautionFreeSpace() {
        return writeProcessor.cautionFreeSpace();
    }

    protected void clearLogStorageFolder() {
        writeProcessor.deleteStorageFiles();
    }

    protected String getAudioCheckerReport() {
        return audioChecker.getAudioSettingsReport();
    }

    protected void renameSessionWrites(String sessionName) {
        writeProcessor.closeWriteFile();
        writeProcessor.setSessionName(sessionName);
        // attempt to reopen
        if (!audioSettings.getWriteFiles()) {
            entryLogger(context.getString(R.string.init_state_14), true);
        }
    }

    protected boolean checkScanner() {
        return audioChecker.checkAudioRecord();
    }

    protected void setPollingSpeed(int delayMS) {
        audioChecker.setPollingSpeed(delayMS);
        entryLogger(context.getString(R.string.option_set_1) + delayMS, false);
    }

    protected boolean micChecking(boolean checking) {
        if (checking) {
            if (audioChecker.checkAudioBufferState()) {
                //TODO
                // this function is of concern as it may not work, uses logcat
                backgroundChecker.auditLogAsync();
                return true;
            }
            else {
                // audioChecker found an error, possible uninitialized error, not an exception however
                entryLogger("AUDIO ERROR FOUND, possible microphone in use.", true);
                return false;
            }
        }
        else {
            audioChecker.stopAllAudio();
            return false;
        }
    }

    protected void pollingCheck(boolean polling) {
        if (polling) {
            if (audioChecker.pollAudioCheckerInit()) {
                audioChecker.pollAudioCheckerStart();
            }
        }
        else {
            audioChecker.finishPollChecker();
        }
    }

    protected void setFrequencyStep(int freqStep) {
        audioSettings.setFreqStep(freqStep);
        entryLogger(context.getString(R.string.option_set_2) + freqStep, false);
    }

    protected void setMinMagnitude(double magnitude) {
        audioScanner.setMinMagnitude(magnitude);
        entryLogger(context.getString(R.string.option_set_3) + magnitude, false);
    }

    protected void setFFTWindowType(int userWindow) {
        audioSettings.setUserWindowType(userWindow);
    }

    protected void runAudioScanner() {
        entryLogger(context.getString(R.string.main_scanner_18), false);
        scanBufferSize = 0;
        if (audioSettings.getWriteFiles()) {
            if (!writeProcessor.prepareWriteAudioFile()) {
                entryLogger(context.getString(R.string.init_state_15), true);
            }
        }
        audioScanner.runAudioScanner();
    }

    protected void stopAudioScanner() {
        if (audioScanner != null) {
            entryLogger(context.getString(R.string.main_scanner_19), false);
            // below nulls the recordTask...
            audioScanner.stopAudioScanner();
            writeProcessor.audioFileConvert();

            if (audioScanner.canProcessBufferStorage()) {
                scanBufferSize = audioScanner.getSizeBufferStorage();
                entryLogger(context.getString(R.string.main_scanner_20) + scanBufferSize, false);
            }
            else {
                entryLogger(context.getString(R.string.main_scanner_21), false);
            }
        }
    }

    protected void resetAudioScanner() {
        audioScanner.resetAudioScanner();
    }

    /********************************************************************/

    protected void runActiveJammer() {
        // background service runnable, emits white noise within n-uhf ranges
        if (audioJammer != null) {
            audioJammer.runActiveJammer();
        }
    }
    protected void stopActiveJammer() {
        if (audioJammer != null) {
            audioJammer.stopActiveJammer();
        }
    }
    protected boolean startPassiveJammer() {
        // holds mic and if needed records to zero values to dev/null
        if (audioJammer != null) {
            return audioJammer.startPassiveJammer();
        }
        return false;
    }
    protected void runPassiveJammer() {
        if (audioJammer != null) {
            audioJammer.runPassiveJammer();
        }
    }
    protected void stopPassiveJammer() {
        if (audioJammer != null) {
            audioJammer.stopPassiveJammer();
        }
    }


    /********************************************************************/

    protected int getAudioRecordAppsNumber() {
        return backgroundChecker.getUserRecordNumApps();
    }

    protected boolean hasAudioBeaconApps() {
        return backgroundChecker.checkAudioBeaconApps();
    }

    protected int getAudioBeaconAppNumber() {
        return backgroundChecker.getAudioBeaconAppNames().length;
    }

    protected String[] getAudioBeaconAppList() {
        return backgroundChecker.getAudioBeaconAppNames();
    }

    protected String[] getScanAppList() {
        return backgroundChecker.getOverrideScanAppNames();
    }

    protected void listBeaconDetails(int appNumber) {
        listAppAudioBeaconDetails(appNumber);
    }

    protected void listScanDetails(int appNumber) {
        listAppOverrideScanDetails(appNumber);
    }

    protected boolean audioStateError() {
        audioChecker.pollAudioCheckerInit();
        return audioChecker.audioStateError();
    }

    protected boolean mainPollingCheck() {
        boolean detected = false;
        setPollingSpeed(100);
        if (audioChecker.pollAudioCheckerInit()) {
            audioChecker.pollAudioCheckerStart();
            detected = audioChecker.getDetected();
        }
        return detected;
    }

    protected void mainPollingStop() {
        audioChecker.finishPollChecker();
    }

    protected boolean hasAudioScanSequence() {
        return audioScanner.hasFrequencySequence();
    }

    protected int getFrequencySequenceSize() {
        return audioScanner.getFrequencySequenceSize();
    }

    protected String getModFrequencyLogic() {
        return context.getString(R.string.audio_scan_1) + "\n" + audioScanner.getFrequencySequenceLogic();
    }

    // MainActivity.stopScanner() debug type outputs
    // currently rem'd out
    protected String getFrequencySequence() {
        // get original sequence as transmitted...
        String sequence = "";
        for (Integer freq : audioScanner.getFreqSequence()) {
            sequence += freq.toString();
            // add a space
            sequence += " ";
        }
        return sequence;
    }

    protected String getFreqSeqLogicEntries() {
        return audioScanner.getFreqSeqLogicEntries();
    }

    /********************************************************************/

    private void initBackgroundChecks() {
        backgroundChecker = new BackgroundChecker();
        if (backgroundChecker.initChecker(context.getPackageManager())) {
            // is good
            auditBackgroundChecks();
        }
        else {
            // is bad
            MainActivity.logger(context.getString(R.string.background_scan_1));
        }
    }

/*
* 	CHECKS
*/
    private void auditBackgroundChecks() {
        // is good
        MainActivity.logger(context.getString(R.string.background_scan_2) + "\n");
        backgroundChecker.runChecker();

        MainActivity.logger(context.getString(R.string.background_scan_3) + backgroundChecker.getUserRecordNumApps() + "\n");

        backgroundChecker.audioAppEntryLog();
    }

    private void listAppAudioBeaconDetails(int selectedIndex) {
        if (backgroundChecker.getAudioBeaconAppEntry(selectedIndex).checkBeaconServiceNames()) {
            entryLogger(context.getString(R.string.background_scan_4)
                    + backgroundChecker.getAudioBeaconAppEntry(selectedIndex).getActivityName()
                    + ": " + backgroundChecker.getAudioBeaconAppEntry(selectedIndex).getBeaconServiceNamesNum(), true);

            logAppEntryInfo(backgroundChecker.getAudioBeaconAppEntry(selectedIndex).getBeaconServiceNames());
        }
        //TODO
        // add a call for any receiver names too
        if (backgroundChecker.getAudioBeaconAppEntry(selectedIndex).checkBeaconReceiverNames()) {
            entryLogger(context.getString(R.string.background_scan_5)
                    + backgroundChecker.getAudioBeaconAppEntry(selectedIndex).getActivityName()
                    + ": " + backgroundChecker.getAudioBeaconAppEntry(selectedIndex).getBeaconReceiverNamesNum(), true);

            logAppEntryInfo(backgroundChecker.getAudioBeaconAppEntry(selectedIndex).getBeaconReceiverNames());
        }
    }

    private void listAppOverrideScanDetails(int selectedIndex) {
        // check for receivers too?
        entryLogger(context.getString(R.string.background_scan_6)
                + backgroundChecker.getOverrideScanAppEntry(selectedIndex).getActivityName()
                + ": " + backgroundChecker.getOverrideScanAppEntry(selectedIndex).getServicesNum(), true);

        if (backgroundChecker.getOverrideScanAppEntry(selectedIndex).getServicesNum() > 0) {
            logAppEntryInfo(backgroundChecker.getOverrideScanAppEntry(selectedIndex).getServiceNames());
        }

        entryLogger(context.getString(R.string.background_scan_7)
                + backgroundChecker.getOverrideScanAppEntry(selectedIndex).getActivityName()
                + ": " + backgroundChecker.getOverrideScanAppEntry(selectedIndex).getReceiversNum(), true);

        if (backgroundChecker.getOverrideScanAppEntry(selectedIndex).getReceiversNum() > 0) {
            logAppEntryInfo(backgroundChecker.getOverrideScanAppEntry(selectedIndex).getReceiverNames());
        }
    }

    private void logAppEntryInfo(String[] appEntryInfoList) {
        entryLogger("\n" + context.getString(R.string.background_scan_8) + "\n", false);
        for (int i = 0; i < appEntryInfoList.length; i++) {
            entryLogger(appEntryInfoList[i] + "\n", false);
        }
    }

    private static void entryLogger(String entry, boolean caution) {
        MainActivity.entryLogger(entry, caution);
    }
}
