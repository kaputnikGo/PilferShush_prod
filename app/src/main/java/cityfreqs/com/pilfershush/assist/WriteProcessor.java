package cityfreqs.com.pilfershush.assist;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cityfreqs.com.pilfershush.MainActivity;
import cityfreqs.com.pilfershush.R;

public class WriteProcessor {
    private Context context;
    private Bundle audioBundle;
    private File extDirectory;

    private File AUDIO_OUTPUT_FILE;
    private File WAV_OUTPUT_FILE;

    public static BufferedOutputStream AUDIO_OUTPUT_STREAM;
    private static DataOutputStream AUDIO_RAW_STREAM;

    //private static final String APP_DIRECTORY_NAME = "PilferShush";
    private static final String DEFAULT_SESSION_NAME = "capture";
    private static final String AUDIO_FILE_EXTENSION_RAW = ".pcm";
    private static final String AUDIO_FILE_EXTENSION_WAV = ".wav";

    public static final long MINIMUM_STORAGE_SIZE_BYTES = 2048; // approx 2 mins pcm audio
    private static final int INT_BYTES = Integer.SIZE / Byte.SIZE;
    private static final int SHORT_BYTES = Short.SIZE / Byte.SIZE;

    // diff OS can pattern the date in the following -
    // underscore: 20180122-12_37_29-capture
    // nospace: 20180122-123729-capture
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd-HH:mm:ss", Locale.ENGLISH);

    public WriteProcessor(Context context, Bundle audioBundle) {
        this.context = context;
        this.audioBundle = audioBundle;

        entryLogger(context.getString(R.string.writer_state_1), false);
        // TODO change for int/ext directory. checks for read/write state
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            createDirectory();
            entryLogger(context.getString(R.string.writer_state_2) + "\n" + extDirectory.toString() + "\n", false);
        }
        else {
            entryLogger(context.getString(R.string.writer_state_4), true);
        }
    }

    public long getStorageSize() {
        return calculateStorageSize();
    }

    public long getFreeStorageSpace() {
        return calculateFreeStorageSpace();
    }

    public void deleteStorageFiles() {
        if (calculateStorageSize() > 0) {
            deleteAllStorageFiles();
        }
    }

    public int cautionFreeSpace() {
        return (int)calculateFreeStorageSpace();
    }

    /**************************************************************/
    /*
        audio logging init
     */

    public boolean prepareWriteAudioFile() {
        // need to build the filename AND path
        if (extDirectory == null) {
            entryLogger(context.getString(R.string.writer_state_4), true);
            return false;
        }
        // add the extension and timestamp
        // eg: 20151218-10:14:32-capture.pcm(.wav)
        String timestamp = getTimestamp();
        String audioFilename = timestamp + "-" + DEFAULT_SESSION_NAME + AUDIO_FILE_EXTENSION_RAW;
        String waveFilename = timestamp + "-" + DEFAULT_SESSION_NAME + AUDIO_FILE_EXTENSION_WAV;
        // file save will overwrite unless new name is used...
        try {
            AUDIO_OUTPUT_FILE = new File(extDirectory, audioFilename);
            if (!AUDIO_OUTPUT_FILE.exists()) {
                if (!AUDIO_OUTPUT_FILE.createNewFile()) {
                    entryLogger("error creating audio out file.", true);
                }
            }

            WAV_OUTPUT_FILE = new File(extDirectory, waveFilename);
            if (!WAV_OUTPUT_FILE.exists()) {
                if (!WAV_OUTPUT_FILE.createNewFile()) {
                    entryLogger("error creating output wav file.", true);
                }
            }

            AUDIO_OUTPUT_STREAM = null;
            AUDIO_OUTPUT_STREAM = new BufferedOutputStream(new FileOutputStream(AUDIO_OUTPUT_FILE, false));
            AUDIO_RAW_STREAM = new DataOutputStream(AUDIO_OUTPUT_STREAM);
            return true;
        }
        catch (IOException ex) {
            ex.printStackTrace();
            entryLogger(context.getString(R.string.writer_state_5), true);
            entryLogger(context.getString(R.string.writer_state_9), true);
            return false;
        }
    }

    /*
        audio logging writes
     */
    public static void writeAudioFile(final short[] shortBuffer, final int bufferRead) {
        if (shortBuffer != null && AUDIO_RAW_STREAM != null) {
            try {
                for (int i = 0; i < bufferRead; i++) {
                    AUDIO_RAW_STREAM.writeShort(shortBuffer[i]);
                }
            }
            catch (IOException e) {
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
            entryLogger(context.getString(R.string.writer_state_10), true);
        }
    }

    public void audioFileConvert() {
        if (!audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[14])) {
           // write disabled
            return;
        }
        if (convertToWav()) {
            boolean isDelete = AUDIO_OUTPUT_FILE.delete();
            if (isDelete) {
                entryLogger(context.getString(R.string.writer_state_21), false);
            }
            else {
                entryLogger("error in audio output file delete.", true);
            }
        }
    }

    /**************************************************************/
    /*
        audio wav convert
    */
    private boolean convertToWav() {
        // raw file is recent pcm save
        if (!AUDIO_OUTPUT_FILE.exists()) {
            entryLogger(context.getString(R.string.writer_state_11), false);
            return false;
        }
        if (!WAV_OUTPUT_FILE.exists()) {
            entryLogger(context.getString(R.string.writer_state_12), false);
            return false;
        }
        // send to converter
        try {
            entryLogger(context.getString(R.string.writer_state_13), false);
            rawToWave(AUDIO_OUTPUT_FILE, WAV_OUTPUT_FILE);

        }
        catch (IOException ex) {
            //
            entryLogger(context.getString(R.string.writer_state_14), true);
            return false;
        }
        entryLogger(context.getString(R.string.writer_state_15), false);
        return true;
    }

    /****************************************************/
    // pcm to wav post-record functions

    private void rawToWave(final File rawFile, final File waveFile) throws IOException {
        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            int reader = input.read(rawData);
        }
        catch (FileNotFoundException ex) {
            entryLogger("rawToWave inputStream File exception catch.", true);
        }
        catch (IOException ex) {
            entryLogger("rawToWave inputStream IO exception catch.", true);
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
            writeString(fileChannel, "RIFF"); // chunk id
            writeInt(fileChannel, 36 + rawData.length); // chunk size
            writeString(fileChannel, "WAVE"); // format
            writeString(fileChannel, "fmt "); // subchunk 1 id
            writeInt(fileChannel, 16); // subchunk 1 size
            writeShort(fileChannel, (short) 1); // audio format (1 = PCM)
            writeShort(fileChannel, (short) 1); // number of channels
            writeInt(fileChannel, audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1])); // sample rate
            writeInt(fileChannel, audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1]) * 2); // byte rate
            writeShort(fileChannel, (short) 2); // block align
            writeShort(fileChannel, (short) audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[15])); // bits per sample (8 * M)
            writeString(fileChannel, "data"); // subchunk 2 id
            writeInt(fileChannel, rawData.length); // subchunk 2 size

            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }
            output.write(bytes.array());
        }
        catch (FileNotFoundException ex) {
            entryLogger("rawToWave outputStream File exception catch.", true);
        }
        catch (IOException ex) {
            entryLogger("rawToWave outputStream IO exception catch.", true);
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }
    private void writeInt(final FileChannel fc, final int value) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(INT_BYTES);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(value);
        bb.flip();
        fc.write(bb);
    }

    private void writeShort(final FileChannel fc, final short value) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(SHORT_BYTES);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(value);
        bb.flip();
        fc.write(bb);
    }

    private void writeString(final FileChannel fc, final String value) throws IOException {
        byte[] cc = value.getBytes(Charset.defaultCharset());
        ByteBuffer bb = ByteBuffer.allocate(cc.length);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(cc);
        bb.flip();
        fc.write(bb);
    }

    /**************************************************************/

    private void createDirectory() {
        // may not be writable if no permissions granted
        extDirectory = context.getExternalFilesDir(null);

        assert extDirectory != null;
        if (extDirectory.exists()) {
            entryLogger(context.getString(R.string.writer_state_23), false);
        }
        else {
            entryLogger("error creating extDirectory.", true);
        }
    }

    private void deleteAllStorageFiles() {
        // assume MainActivity has cautioned first.
        assert extDirectory != null;
        if (!extDirectory.exists()) {
            entryLogger(context.getString(R.string.writer_state_16_1), false);
            return;
        }
        entryLogger(context.getString(R.string.writer_state_18), false);
        boolean deleteSuccess = false;

        if (extDirectory.listFiles() != null) {
            for (File file : extDirectory.listFiles()) {
                deleteSuccess = file.delete();
            }
        }
        if (deleteSuccess) {
            entryLogger(context.getString(R.string.writer_state_19), false);
        }
        else {
            entryLogger("Error deleting extDirectory files.", true);
        }
    }

    private long calculateStorageSize() {
        // returns long size in bytes
        assert extDirectory != null;
        if (!extDirectory.exists()) {
            entryLogger(context.getString(R.string.writer_state_16_2), true);
            return 0;
        }
        long length = 0;
        if (extDirectory.listFiles() != null) {
            for (File file : extDirectory.listFiles()) {
                if (file.isFile()) {
                    length += file.length();
                }
            }
        }
        return length;
    }

    @SuppressLint("UsableSpace")
    private long calculateFreeStorageSpace() {
        // returns long size in bytes
        //TODO gets the extDirectory !exists
        if (!extDirectory.exists()) {
            entryLogger(context.getString(R.string.writer_state_16_3), true);
            return 0;
        }
        return extDirectory.getUsableSpace();
    }

    private String getTimestamp() {
        // for adding to default file save name
        // eg: 20151218-10:14:32-capture
        return TIMESTAMP_FORMAT.format(new Date());
    }

    private void entryLogger(String entry, boolean caution) {
        MainActivity.entryLogger(entry, caution);
    }
}

