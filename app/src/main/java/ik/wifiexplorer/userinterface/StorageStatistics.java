package ik.wifiexplorer.userinterface;

import android.os.Environment;
import android.os.StatFs;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.text.DecimalFormat;


public class StorageStatistics {

    // PRIVATE--------------------------------------------------------------------------------------

    private int fileCounter         = 0;
    private int dirCounter          = 0;
    private long usedMemorySize     = 0;
    private long freeMemorySize     = 0;
    private long systemSize         = 0;
    private long totalMemorySize    = 0;
    private long fileSizeUnknown    = 0;
    private int countUnknown        = 0;
    private long fileSizeImages     = 0;
    private int countImages         = 0;
    private long fileSizeVideo      = 0;
    private int countVideo          = 0;
    private long fileSizeData       = 0;
    private int countData           = 0;
    private long fileSizeAudio      = 0;
    private int countAudio          = 0;

    private File root = null;


    private String getMimeType( File file )
    {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl( file.toString() );
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    private String parseSize( long filesize ){

        int digits = (filesize + "").length();
        DecimalFormat df = new DecimalFormat("0.00");

        if ( digits < 4 )  { return filesize + " Byte";}
        if ( digits < 7 )  { return df.format(filesize / 1024.0) + " KB";}
        if ( digits < 10 ) { return df.format(filesize / 1024.0 / 1024.0) + " MB";}

        return df.format(filesize / 1024.0 / 1024.0 / 1024.0) + " GB";

    }

    private void countFiles( File rootDir ){


        if ( !rootDir.exists() || !rootDir.isDirectory() ) {
            return;
        }

        File[] files = rootDir.listFiles();
        if ( files == null ){
            return;
        }

        for( File f : files) {

            if (f.canRead()) {

                if (f.isDirectory()) {
                    dirCounter += 1;
                    countFiles( f );
                }

                if (f.isFile()) {
                    // TODO: tue etwas falls es eine Datei ist:
                    fileCounter    += 1;
                    usedMemorySize += f.length();

                    String mimeType = getMimeType( f );

                    if ( mimeType != null ) {
                        if (mimeType.contains("audio")) {
                            fileSizeAudio += f.length();
                            countAudio += 1;

                        } else if (mimeType.contains("video")) {
                            fileSizeVideo += f.length();
                            countVideo += 1;

                        } else if (mimeType.contains("application")) {
                            fileSizeData += f.length();
                            countData += 1;

                        } else if (mimeType.contains("text")) {
                            fileSizeData += f.length();
                            countData += 1;

                        } else if (mimeType.contains("image")) {
                            fileSizeImages += f.length();
                            countImages += 1;

                        } else {
                            fileSizeUnknown += f.length();
                            countUnknown += 1;

                        }

                    }
                    else {
                        fileSizeUnknown += f.length();
                        countUnknown += 1;

                    }

                }

            }
        }


    }

    private void countAvailableExternalMemorySize() {

        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();

        freeMemorySize = availableBlocks * blockSize;
    }

    private void countTotalExternalMemorySize() {

        /*
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        totalMemorySize = (statFs.getBlockCount() * statFs.getBlockSize());
        */

        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getAbsolutePath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();

        totalMemorySize = totalBlocks * blockSize;

    }

    // PRIVATE----------------------------------------------------------------------------------ENDE





    // PUBLIC---------------------------------------------------------------------------------------

    // Constructor:
    public StorageStatistics( File rootpath ){

        countFiles( rootpath );
        countTotalExternalMemorySize();
        countAvailableExternalMemorySize();

        systemSize = totalMemorySize - freeMemorySize - usedMemorySize;
        root = rootpath;

    }

    public void refresh(){
        // Update / refresh the statistics from the client:

        fileCounter         = 0;
        dirCounter          = 0;
        usedMemorySize      = 0;
        freeMemorySize      = 0;
        systemSize          = 0;
        totalMemorySize     = 0;
        fileSizeUnknown     = 0;
        countUnknown        = 0;
        fileSizeImages      = 0;
        countImages         = 0;
        fileSizeVideo       = 0;
        countVideo          = 0;
        fileSizeData        = 0;
        countData           = 0;
        fileSizeAudio       = 0;
        countAudio          = 0;

        countFiles(root);
        countTotalExternalMemorySize();
        countAvailableExternalMemorySize();

        systemSize = totalMemorySize - freeMemorySize - usedMemorySize;
    }

    public String getNumberOfFiles(){
        return (fileCounter + "");
    }

    public String getNumberOfDirectories(){
        return (dirCounter + "");
    }

    public String getClientsideUsedMemorySize(){
        return ( parseSize( usedMemorySize ) );
    }

    public String getNumberOfImages(){
        return ( countImages + "" );
    }

    public String getSizeOfImages(){
        return parseSize( fileSizeImages );
    }

    public String getNumberOfVideos(){
        return ( countVideo + "" );
    }

    public String getSizeOfVideos(){
        return parseSize( fileSizeVideo );
    }

    public String getNumberOfAudioFiles(){
        return ( countAudio + "" );
    }

    public String getSizeOfAudioFiles(){
        return parseSize( fileSizeAudio );
    }

    public String getNumberOfDataFiles(){
        return ( countData + "" );
    }

    public String getSizeOfDataFiles(){
        return parseSize( fileSizeData );
    }

    public String getNumberOfUnknownFiles(){
        return ( countUnknown + "" );
    }

    public String getSizeOfUnknownFiles(){
        return parseSize( fileSizeUnknown );
    }

    public String getTotalMemorySize(){
        return parseSize( totalMemorySize );
    }

    public String getFreeMemorySize(){
        countTotalExternalMemorySize();
        return parseSize( freeMemorySize );
    }

    public String getSystemUsedMemorySize(){
        return parseSize( systemSize );
    }

    public String getTotalUsedMemorySize(){
        return parseSize( usedMemorySize+systemSize );
    }

    // PUBLIC-----------------------------------------------------------------------------------ENDE

}
