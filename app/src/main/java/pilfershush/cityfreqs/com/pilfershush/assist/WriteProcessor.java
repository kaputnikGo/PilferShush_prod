package pilfershush.cityfreqs.com.pilfershush.assist;


import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import pilfershush.cityfreqs.com.pilfershush.MainActivity;

public class WriteProcessor {
    // pcm savefile for raw import into Audacity as 48 kHz, signed 16 bit, big-endian, mono

    // wav has a little-endian

    // laborious process to export files from Download(s) folder to PC, need Music folder or similar
    // with proper file read access for external - possibly just a samsung pita

    private AudioSettings audioSettings;

    private File extDirectory;

    private String audioFilename;
    private String waveFilename;
    private String logFilename;
    private String sessionFilename;
    private static boolean writeWav;

    public static File AUDIO_OUTPUT_FILE;
    public static File WAV_OUTPUT_FILE;

    public static BufferedOutputStream AUDIO_OUTPUT_STREAM;
    public static DataOutputStream AUDIO_RAW_STREAM;

    public static File LOG_OUTPUT_FILE;
    public static FileOutputStream LOG_OUTPUT_STREAM;
    public static OutputStreamWriter LOG_OUTPUT_WRITER;

    private static final String APP_DIRECTORY_NAME = "PilferShush";
    private static final String AUDIO_FILE_EXTENSION_RAW = ".pcm";
    private static final String AUDIO_FILE_EXTENSION_WAV = ".wav";
    private static final String LOG_FILE_EXTENSION = ".txt";

    private static final long MINIMUM_STORAGE_SIZE_BYTES = 2048; // approx 2 mins pcm audio
    private static final int INT_BYTES = Integer.SIZE / Byte.SIZE;
    private static final int SHORT_BYTES = Short.SIZE / Byte.SIZE;

    // diff OS can pattern the date in the following -
    // underscore: 20180122-12_37_29-capture
    // nospace: 20180122-123729-capture
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd-HH:mm:ss", Locale.ENGLISH);

    public WriteProcessor(String sessionName, AudioSettings audioSettings, boolean writeWav) {
        setSessionName(sessionName);
        this.audioSettings = audioSettings;
        this.writeWav = writeWav;

        log("Check: Download(s)/PilferShush/");
        // checks for read/write state
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            createDirectory();
            log("ext dir: " + extDirectory.toString());
        }
    }

    public void setSessionName(String sessionName) {
        if (sessionName == null || sessionName == "") {
            sessionFilename = "capture";
        }
        else {
            sessionFilename = sessionName;
        }
    }

    public long getStorageSize() {
        return calculateStorageSize();
    }

    public long getFreeStorageSpace() {
        return calculateFreeStorageSpace();
    }

    public void deleteEmptyLogFiles() {
        // storage could have only 0kb log files
        deleteZeroSizeLogFiles();
    }

    public void deleteStorageFiles() {
        if (calculateStorageSize() > 0) {
            deleteAllStorageFiles();
        }
    }

    public boolean cautionFreeSpace() {
        if (calculateFreeStorageSpace() <= MINIMUM_STORAGE_SIZE_BYTES) {
            return true;
        }
        return false;
    }

    /**************************************************************/
    /*
        text logging
     */
    public void prepareLogToFile() {
        // need to build the filename AND path
        log("prepare log file...");
        File location = extDirectory;
        if (location == null) {
            log("Error getting storage directory");
            return;
        }
        // add the extension and timestamp
        // eg: 20151218-10:14:32-capture.txt
        logFilename = getTimestamp() + "-" + sessionFilename + LOG_FILE_EXTENSION;
        try {
            LOG_OUTPUT_FILE = new File(location, logFilename);
            LOG_OUTPUT_FILE.createNewFile();
            LOG_OUTPUT_STREAM = new FileOutputStream(LOG_OUTPUT_FILE, true);
            LOG_OUTPUT_WRITER = new OutputStreamWriter(LOG_OUTPUT_STREAM);
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
            log("File not found error.");
        }
        catch (IOException e) {
            e.printStackTrace();
            log("Log file write error.");
        }
    }

    public static void writeLogFile(String textline) {
        if (LOG_OUTPUT_WRITER != null) {
            try {
                LOG_OUTPUT_WRITER.append((new StringBuilder()).append(textline).append("\n"));
            }
            catch (IOException e) {
                //
            }
        }
    }

    public void closeLogFile() {
        // final act, no more writes possible.
        log("close log file.");
        try {
            if (LOG_OUTPUT_WRITER != null) {
                LOG_OUTPUT_WRITER.flush();
                LOG_OUTPUT_WRITER.close();
            }
            if (LOG_OUTPUT_STREAM != null) {
                LOG_OUTPUT_STREAM.flush();
                LOG_OUTPUT_STREAM.close();
            }
        }
        catch (IOException e) {
            log("Error closing log output stream.");
        }
    }

    /**************************************************************/
    /*
        audio logging init
     */

    public void prepareWriteAudioFile() {
        // need to build the filename AND path
        File location = extDirectory;
        if (location == null) {
            log("Error getting storage directory");
            return;
        }
        // add the extension and timestamp
        // eg: 20151218-10:14:32-capture.pcm(.wav)
        audioFilename = getTimestamp() + "-" + sessionFilename + AUDIO_FILE_EXTENSION_RAW;
        if (writeWav) {
            waveFilename = getTimestamp() + "-" + sessionFilename + AUDIO_FILE_EXTENSION_WAV;

        }

        // file save will overwrite unless new name is used...
        try {
            AUDIO_OUTPUT_FILE = new File(location, audioFilename);
            if (!AUDIO_OUTPUT_FILE.exists()) {
                AUDIO_OUTPUT_FILE.createNewFile();
            }
            AUDIO_OUTPUT_STREAM = null;
            AUDIO_OUTPUT_STREAM = new BufferedOutputStream(new FileOutputStream(AUDIO_OUTPUT_FILE, false));
            AUDIO_RAW_STREAM = new DataOutputStream(AUDIO_OUTPUT_STREAM);

            if (writeWav) {
                WAV_OUTPUT_FILE = new File(location, waveFilename);
                if (!WAV_OUTPUT_FILE.exists()) {
                    WAV_OUTPUT_FILE.createNewFile();
                }
            }
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
            log("File not found error.");
        }
        catch (IOException e) {
            e.printStackTrace();
            log("Audio write file error.");
        }
    }


    /**************************************************************/
    /*
        audio logging writes
     */
    /********************************************************************/

    public static void writeAudioFile(final short[] shortBuffer, final int bufferRead) {

            if (shortBuffer != null) {
                try {
                    for (int i = 0; i < bufferRead; i++) {
                        AUDIO_RAW_STREAM.writeShort(shortBuffer[i]);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

    }

    public void closeWriteFile() {
        try {
            if (AUDIO_OUTPUT_STREAM != null) {
                AUDIO_OUTPUT_STREAM.flush();
                AUDIO_OUTPUT_STREAM.close();
                AUDIO_RAW_STREAM.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            log("onCancelled write stream close error.");
        }
        // then close text logging
        closeLogFile();
    }

    public void audioFileConvert() {
        if (writeWav) {
            convertToWav();
            // when finished, delete the raw pcm file
        }
    }

    /**************************************************************/
    //TODO
    /*
        audio wav convert
        glitchy... still requires import raw, not saving/reading as little-endian
    */
    private boolean convertToWav() {
        // raw file is recent pcm save
        if (!AUDIO_OUTPUT_FILE.exists()) {
            log("no raw audio file found.");
            return false;
        }
        if (!WAV_OUTPUT_FILE.exists()) {
            log("no wav output file found.");
            return false;
        }
        // send to converter
        try {
            log("Converting raw audio to wav file...");
            rawToWave(AUDIO_OUTPUT_FILE, WAV_OUTPUT_FILE);

        }
        catch (IOException ex) {
            //
            log("Error writing pcm file to wav file.");
            return false;
        }
        log("Finished saving " + waveFilename);
        return true;
    }

    /****************************************************/
    // pcm to wav post-record functions

    private void rawToWave(final File rawFile, final File waveFile) throws IOException {
        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        }
        finally {
            if (input != null) {
                input.close();
            }
        }

        FileOutputStream output = null;
        FileChannel fileChannel;

        try {
            output = new FileOutputStream(waveFile);
            fileChannel = output.getChannel();
            // WAVE header
            writeString(fileChannel, "RIFF", ByteOrder.BIG_ENDIAN); // chunk id
            writeInt(fileChannel, 36 + rawData.length, ByteOrder.LITTLE_ENDIAN); // chunk size
            writeString(fileChannel, "WAVE", ByteOrder.BIG_ENDIAN); // format
            writeString(fileChannel, "fmt ", ByteOrder.BIG_ENDIAN); // subchunk 1 id
            writeInt(fileChannel, 16, ByteOrder.LITTLE_ENDIAN); // subchunk 1 size
            writeShort(fileChannel, (short) 1, ByteOrder.LITTLE_ENDIAN); // audio format (1 = PCM)
            writeShort(fileChannel, (short) 1, ByteOrder.LITTLE_ENDIAN); // number of channels
            writeInt(fileChannel, audioSettings.getSampleRate(), ByteOrder.LITTLE_ENDIAN); // sample rate
            writeInt(fileChannel, audioSettings.getSampleRate() * 2, ByteOrder.LITTLE_ENDIAN); // byte rate
            writeShort(fileChannel, (short) 2, ByteOrder.LITTLE_ENDIAN); // block align
            writeShort(fileChannel, (short) audioSettings.getBitDepth(), ByteOrder.LITTLE_ENDIAN); // bits per sample
            writeString(fileChannel, "data", ByteOrder.BIG_ENDIAN); // subchunk 2 id
            writeInt(fileChannel, rawData.length, ByteOrder.LITTLE_ENDIAN); // subchunk 2 size

            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }
            output.write(bytes.array());
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }
    private void writeInt(final FileChannel fc, final int value, ByteOrder order) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(INT_BYTES);
        bb.order(order);
        bb.putInt(value);
        bb.flip();
        fc.write(bb);
    }

    private void writeShort(final FileChannel fc, final short value, ByteOrder order) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(SHORT_BYTES);
        bb.order(order);
        bb.putShort(value);
        bb.flip();
        fc.write(bb);
    }

    private void writeString(final FileChannel fc, final String value, ByteOrder order) throws IOException {
        byte[] cc = value.getBytes(Charset.defaultCharset());
        ByteBuffer bb = ByteBuffer.allocate(cc.length);
        bb.order(order);
        bb.put(cc);
        bb.flip();
        fc.write(bb);
    }

    /**************************************************************/

    private void createDirectory() {
        // may not be writable if no permissions granted
        extDirectory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), APP_DIRECTORY_NAME);
        if (!extDirectory.exists()) {
            extDirectory.mkdirs();
        }
    }

    private void deleteZeroSizeLogFiles() {
        // assume MainActivity has cautioned first.
        if (!extDirectory.exists()) {
            log("No storage folder found.");
            return;
        }

        log("Deleting empty files...");
        int counter = 0;
        for (File file : extDirectory.listFiles()) {
            if (file.isFile()) {
                if (file.length() == 0) {
                    file.delete();
                    counter++;
                }
            }
        }
        log("Deleted " + counter + " empty file(s).");

    }

    private void deleteAllStorageFiles() {
        // assume MainActivity has cautioned first.
        if (!extDirectory.exists()) {
            log("No storage folder found.");
            return;
        }
        String[] filesDelete = extDirectory.list();
        log("Deleting " + filesDelete.length + " files from storage...");
        for (int i = 0; i < filesDelete.length; i++) {
            new File(extDirectory, filesDelete[i]).delete();
        }
        log("Storage folder now empty.");
    }

    private long calculateStorageSize() {
        // returns long size in bytes
        if (!extDirectory.exists()) {
            log("No storage folder found.");
            return 0;
        }
        long length = 0;
        for (File file : extDirectory.listFiles()) {
            if (file.isFile()) {
                length += file.length();
            }
        }
        log("Storage size: " + (int)length);
        return length;
    }

    private long calculateFreeStorageSpace() {
        // returns long size in bytes
        if (!extDirectory.exists()) {
            log("No storage folder found.");
            return 0;
        }
        // getFreeSpace == unallocated
        return extDirectory.getUsableSpace();
    }

    private String getTimestamp() {
        // for adding to default file save name
        // eg: 20151218-10:14:32-capture
        return TIMESTAMP_FORMAT.format(new Date());
    }

    private static void log(String message) {
        MainActivity.logger(message);
    }
}

