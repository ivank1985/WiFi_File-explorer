////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//
//
//
//
//
//
//
//
//                                                                      by ik.android (ivan knauer)
////////////////////////////////////////////////////////////////////////////////////////////////////



package ik.wifiexplorer.webserver;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import ik.wifiexplorer.R;
import ik.wifiexplorer.userinterface.StorageStatistics;


public class MainMenu {


    //----------- PRIVATE Bereich der statischen Klasse MainMenu -----------------------------------

    //  PRIVATE-Variablen:
    private static String DIR_PATTERN  = "--SD-PATH--";
    private static String FILE_PATTERN = "--LEER--";
    private static String UPLOAD_PATTERN = "--UPLOAD--";
    private static String NEWDIR_PATTERN = "--NEWDIR--";
    private static String FULL_PATH_PATTERN = "--FULL_PATH--";
    private static String SIZE_PATTERN = "--SIZE--";
    private static String ELEMENTS_PATTERN = "--ELEMENTS--";
    private static String DEVICE_PATTERN = "--DEVICE--";

    private static Context mContext = null;
    private static StorageStatistics mStorageStatistics = null;
    private static int signalStrength;

    //private StringBuilder htmlMainMenuWebpage = new StringBuilder();

    // Data structures for saving the current Directory-View (Left Frame)
    private static List<String>  directoryList   = new ArrayList<>();
    private static List<Boolean> directoryIsOpen = new ArrayList<>();
    private static List<Integer> directoryDepth  = new ArrayList<>();

    //Auxiliary structures for buffering data after an "onItemClick" event
    private static List<String>  subList   = new ArrayList<>();
    private static List<Boolean> subIsOpen = new ArrayList<>();
    private static List<Integer> subDepth  = new ArrayList<>();

    // Data structures for saving the current file-view (right frame)
    private static List<String> fileList   = new ArrayList<>();
    private static String selectedDirectory = "";

    // PRIVATE-Methods:
    private static int addItemToSubList( String full_path, int depth ){

        // Adds an item to the end of the sublist.
        subList.add(full_path);
        subIsOpen.add(false);
        subDepth.add(depth);

        return 0;

    }

    private static int createSublist( String full_path, int depth ){

        // After an "onItemClick" event: Create a list of folders to display
        subList   = new ArrayList<>();
        subIsOpen = new ArrayList<>();
        subDepth  = new ArrayList<>();

        File dir = new File( full_path );
        String[] list = dir.list();
        Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

        if ( list.length >0 ) {

            for (String file : list) {

                if (!file.startsWith(".")) {

                    File tmp = new File(dir.toString() + "/" + file);
                    if (tmp.isDirectory()) {
                        addItemToSubList(tmp.toString(), depth);
                    }

                }

            }

        }

        return 0;
    }

    private static int addSublist( int index ){
        // Insert a newly generated sublist into the main list at the "index" position

        if ( subList.size() == 0){
            return -1;
        }

        directoryList.addAll(index, subList);
        directoryIsOpen.addAll(index, subIsOpen);
        directoryDepth.addAll(index, subDepth);

        return 0;
    }

    private static int createFileList( String full_path ){

        if ( full_path.isEmpty() ){
            return -1;
        }

        fileList  = new ArrayList<>();

        File dir = new File( full_path );
        String[] list = dir.list();
        Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

        if ( list.length >0 ) {

            for (String file : list) {

                if (!file.startsWith(".")) {

                    File tmp = new File(dir.toString() + "/" + file);
                    if (tmp.isFile()) {
                        fileList.add(tmp.toString());
                    }

                }

            }

        }

        return 0;
    }

    private static int openDirectory( String path, int indexOfDirectory ) {

        directoryIsOpen.set( indexOfDirectory, true );
        int parentDepth = directoryDepth.get(indexOfDirectory);

        createSublist(path, parentDepth + 1);
        createFileList(path);
        addSublist(indexOfDirectory + 1);

        return 0;
    }

    private static int closeDirectory( String link, int index ) {
        // Close a folder. Basicly, the list is emptied using the Depth values.

        // Create Current File-View:
        createFileList( link );

        //SPECIAL CASE: Last item in the list has no subfolder:
        if ( index+1 == directoryList.size() ){
            return -3;
        }

        // List has no, or only one entry
        if ( directoryList.size() <=1 ){
            return -2;
        }

        int depth         = directoryDepth.get( index );
        int nextItemDepth = directoryDepth.get(index + 1);

        if ( depth >= nextItemDepth ){
            // Do not close, folder is empty!
            return -1;
        }

        while ( (depth < nextItemDepth)  ){
            // Remove parts from the list step by step:
            // ATTENTION: The list is becoming shorter step by step!
            directoryList.remove( index+1 );
            directoryIsOpen.remove( index+1 );
            directoryDepth.remove( index+1 );


            //If the end of the list is reached - >> finished!
            if ( index+1 >= directoryList.size() ){
                break;
            }

            // Move the deferred depth value:
            nextItemDepth = directoryDepth.get( index+1 );
        }

        return 0;
    }

    // Bestimmung des MIME-Typrs einer Datei über Dateipfad
    private static String getMimeType(String fileUrl) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl);
        String mimeType  = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        if ( mimeType == null ){ mimeType = "unknown"; }

        return mimeType;
    }

    // Special Characters Filter:
    private static String specialCharactersFilter( String word ){

        //ä, Ä
        word = word.replaceAll("ä", "&auml;");
        word = word.replaceAll("Ä", "&Auml;");

        //ö, Ö
        word = word.replaceAll("ö", "&ouml;");
        word = word.replaceAll("Ö", "&Ouml;");

        //ü, Ü
        word = word.replaceAll("ü", "&uuml;");
        word = word.replaceAll("Ü", "&Uuml;");

        //ß
        word = word.replaceAll("ß", "&szlig;");

        //'
        word = word.replaceAll("'", "\\\\'");

        return word;

    }


    //------------- PRIVATE area of ​​the static class MainMenu -----------------------------------END



    

    //-----------------PUPLIC area of the static class MainMenu ------------------------------------
    public static void setContext( Context context, File roopath ) {
        // Default Create method
        mContext = context;

        // Generate an object of the class StorageStatistics to analyze the memory
        mStorageStatistics = new StorageStatistics( roopath );
        addItemToSubList(roopath.toString(), 0);
        addSublist(0);

    }


    public static String updateDirectoryView() {
        // Method for updating the directory display (folder tree)

        // load the default menu
        StringBuilder page = new StringBuilder(Utility.openHTMLString(mContext, R.raw.left));

        // Erzeuge aktuelle ansicht:
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < directoryList.size(); i++){

            String full_path = directoryList.get(i);
            int em = 2*directoryDepth.get(i);
            int idxOfLast = full_path.lastIndexOf("/");

            String filename;
            if ( i == 0){
                filename = "SD-card";
            }
            else {
                filename = full_path.substring(idxOfLast+1, full_path.length());
            }

            full_path = specialCharactersFilter( full_path );

            html.append( "\t\t<div class=\"folder_parent\" style=\"margin-left:"+em+"em\">" );
            html.append( "\t\t\t<a href=\"javascript:onclick=parent.left.location=('/dir" + full_path + "'); parent.main.location=('/fileList');\">" );

            if ( directoryIsOpen.get(i) ){
                html.append( "\t\t\t\t<div class=\"folder_opened\">" + filename + "</div>" );
            }
            else {
                html.append( "\t\t\t\t<div class=\"folder_closed\">" + filename + "</div>" );
            }

            html.append( "\t\t\t</a>" );
            html.append( "\t\t</div>" );


        }

        int idx = page.indexOf(DIR_PATTERN);
        page.replace(idx, idx + DIR_PATTERN.length(), html.toString());

        return page.toString();
    }


    public static String updateFileView(){
        // Method for updating the file display (currently contained in the selected folder).

        // load default
        StringBuilder page = new StringBuilder(Utility.openHTMLString(mContext, R.raw.main));

        // create current view
        StringBuilder html = new StringBuilder( "<div class = \"tabelle\">\n" );

        // Determine the total size and number of files contained:
        long sizeOfAllFiles      = 0;
        long numberOfAllFiles    = 0;

        for (int i = 0; i < fileList.size(); i++){

            // Filename and path, find the last '/' character and split the string at this point
            String full_path = fileList.get(i);
            int idxOfLast = full_path.lastIndexOf("/");

            String filename = full_path.substring(idxOfLast + 1, full_path.length());
            File   f = new File( full_path );

            // Filter? After transfer by http .......... perhaps not necessary ???
            full_path = full_path.replaceAll(" ", "%20");
            full_path = full_path.replaceAll("\'", "%27");

            // Update the total size and number of files:
            sizeOfAllFiles      += f.length();
            numberOfAllFiles    += 1;

            // Table entry:
            StringBuilder tableRow = new StringBuilder();

            if ( i%2 == 0 ) {
                tableRow.append( "\t<div class = \"line_light\">\n" );
            }
            else {
                tableRow.append( "\t<div class = \"line_dark\">\n" );
            }

            //Insert a new file as html-code
            tableRow.append( "\t\t<div class = \"line_big\"> <a href=\'/download" + full_path + "\' download>" + filename + "</a> </div>\n" );
            tableRow.append( "\t\t<div class = \"line_small\">" + getMimeType( f.toString() ) + " </div>\n" );
            tableRow.append( "\t\t<div class = \"line_small\">" + new Date(f.lastModified()).toString() + " </div>\n" );
            tableRow.append( "\t\t<div class = \"line_small\">" + Utility.parseSize(f.length()) + " </div>\n" );
            tableRow.append( "\t\t<div class = \"line_small\"> <a href=/fileList onclick=\"rename('" + filename + "', '" + full_path + "')\"> rename </a> </div>\n" );
            tableRow.append( "\t\t<div class = \"line_small\"> <a href=\'/delete" + full_path + "\' onclick=\"return confirm('\n" +
                    "Sure, the file " + full_path + " should be irrevocably deleted?')\"> delete </a> </div>\n" );
            tableRow.append( "\t</div>\n" );

            html.append( tableRow );
        }

        html.append( "</div>\n" );


        // assemble html-output:
        int idx;

        // '--UPLOAD--'
        idx = page.indexOf(UPLOAD_PATTERN);
        page.replace(idx, idx+UPLOAD_PATTERN.length(), "/upload"+selectedDirectory);

        // '--NEWDIR--'
        idx = page.indexOf(NEWDIR_PATTERN);
        page.replace(idx, idx+NEWDIR_PATTERN.length(), "/makedir"+selectedDirectory);

        // '--FULL_PATH--'
        idx = page.indexOf(FULL_PATH_PATTERN);
        page.replace(idx, idx+FULL_PATH_PATTERN.length(), selectedDirectory );

        // '--ELEMENTS--' und '--SIZE--'
        idx = page.indexOf(ELEMENTS_PATTERN);
        page.replace(idx, idx+ELEMENTS_PATTERN.length(), "Number of files in this directory: " + numberOfAllFiles + "." );
        idx = page.indexOf(SIZE_PATTERN);
        page.replace(idx, idx+SIZE_PATTERN.length(), "Total size of the files in this directory: " + Utility.parseSize(sizeOfAllFiles) + "." );

        // 5.
        idx = page.indexOf(FILE_PATTERN);
        page.replace( idx, idx+FILE_PATTERN.length(), html.toString() );

        return page.toString();

    }


    public static int onItemClick( String link ){
        // This method is called after an item has been clicked.

        selectedDirectory = link;
        int idx = directoryList.indexOf( link );

        if ( idx == -1){
            link += " ";
            idx = directoryList.indexOf( link );
        }

        if ( directoryIsOpen.get(idx) ){
            // If folders already opened before -> Close folder:

            closeDirectory( link, idx );
            directoryIsOpen.set(idx, false);

            return 1;
        }
        else {
            // If folders are not open before -> Open folder

            openDirectory( link, idx );
            directoryIsOpen.set( idx, true );
        }

        return 0;

    }

    public static int createNewDirectoryListItem( String parent_path, String newDirectory ){
        // Method for creating a new folder "newDirectory" in the currently selected directory "parent_path"

        //parent_path = parent_path.replaceAll("%", " ");
        int idx = directoryList.indexOf( parent_path );
        closeDirectory(parent_path, idx);
        openDirectory(parent_path, idx);

        idx = directoryList.indexOf( parent_path+"/"+newDirectory );
        openDirectory( parent_path+"/"+newDirectory, idx );
        selectedDirectory = parent_path + "/" + newDirectory;

        return 0;

    }


    public static int createNewFileListItem( String parent_path ){
        // PUBLIC interface to display the file list after a file upload After a file upload has been performed, the new file name is to be added to the file list.

        createFileList( parent_path );

        return 0;
    }

    public static String createDeviceInfoPage(){
        // Method for generating the Device Info page


        mStorageStatistics.refresh();

        TelephonyManager telephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        StringBuilder html = new StringBuilder( "<div class=\"table\">\n" );

        String deviceName       = android.os.Build.MODEL;
        String deviceMan        = android.os.Build.MANUFACTURER;
        String deviceID         = android.os.Build.ID;
        String deviceProd       = Build.PRODUCT;
        String AndroidVersion   = android.os.Build.VERSION.RELEASE;

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">" + deviceMan.toUpperCase() + "</div>\n" +
                "\t\t<div class=\"line_right\">" + deviceName + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">Android&trade;-version:</div>\n" +
                "\t\t<div class=\"line_right\">" + AndroidVersion + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">Device-ID:</div>\n" +
                "\t\t<div class=\"line_right\">" + deviceID + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">Product name:</div>\n" +
                "\t\t<div class=\"line_right\">" + deviceProd + "</div>\n" +
                "\t</div>\n" );


        // IMEI-Nummer
        String imei = telephonyManager.getDeviceId();
        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">IMEI:</div>\n" +
                "\t\t<div class=\"line_right\">" + imei + "</div>\n" +
                "\t</div>\n" );
        html.append( "</div>\n" );

        // memory status:
        html.append( "<h2>Memory status:</h2>\n\n" +
                "<div class=\"table\">\n");

        html.append("\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">Total memory size:</div>\n" +
                "\t\t<div class=\"line_right\">" + mStorageStatistics.getTotalMemorySize() + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">Used memory size:</div>\n" +
                "\t\t<div class=\"line_right\">" + mStorageStatistics.getTotalUsedMemorySize() + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\"><tab1>used by system and apps</div>\n" +
                "\t\t<div class=\"line_right\">" + mStorageStatistics.getSystemUsedMemorySize() + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\"><tab1>used by user-data:</div>\n" +
                "\t\t<div class=\"line_right\">" + mStorageStatistics.getClientsideUsedMemorySize() + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">free memory:</div>\n" +
                "\t\t<div class=\"line_right\">" + mStorageStatistics.getFreeMemorySize() + "</div>\n" +
                "\t</div>\n" );
        html.append( "</div>\n" );


        // memory statistics:
        html.append( "<h2>Statistics about memory usage:</h2>\n\n" +
                "<div class=\"table\">\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">number of folders:</div>\n" +
                "\t\t<div class=\"line_right\">" + mStorageStatistics.getNumberOfDirectories() + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">number of files:</div>\n" +
                "\t\t<div class=\"line_right\">" + mStorageStatistics.getNumberOfFiles() + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">number and size of pictures</div>\n" +
                "\t\t<div class=\"line_right\">" + mStorageStatistics.getNumberOfImages() + " elements  " + mStorageStatistics.getSizeOfImages() + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">number and size of music files:</div>\n" +
                "\t\t<div class=\"line_right\">" + mStorageStatistics.getNumberOfAudioFiles() + " elements  " + mStorageStatistics.getSizeOfAudioFiles() + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "<div class=\"line_left\">number and size of videos:</div>\n" +
                "<div class=\"line_right\">" + mStorageStatistics.getNumberOfVideos() + " elements  " + mStorageStatistics.getSizeOfVideos() + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">number and size of other files:</div>\n" +
                "\t\t<div class=\"line_right\">" + mStorageStatistics.getNumberOfDataFiles() + " elements  " + mStorageStatistics.getSizeOfDataFiles() + "</div>\n" +
                "\t</div>\n" );

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">number and size of hidden files:</div>\n" +
                "\t\t<div class=\"line_right\">" + mStorageStatistics.getNumberOfUnknownFiles() + " elements  " + mStorageStatistics.getSizeOfUnknownFiles() + "</div>\n" +
                "\t</div>\n" );
        html.append( "</div>\n" );

        html.append( "<div class=\"freeline\"></div>\n" +
                "<div class=\"freeline\"></div>\n\n" +
                "<div class=\"table\">\n" );

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, ifilter);

        int status = 0;
        int level  = 0;
        int scale  = 0;
        float batteryPct = -1;

        if (batteryStatus != null) {
            // battery-stat
            status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            batteryPct = level / (float)scale;
        }

        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        if ( isCharging ){
            html.append( "\t<div class=\"line\">\n" +
                    "\t\t<div class=\"line_left\">Battery is charging.</div>\n" +
                    "\t\t<div class=\"line_right\"></div>\n" +
                    "\t</div>\n" );
        }
        else {
            html.append( "\t<div class=\"line\">\n" +
                    "\t\t<div class=\"line_left\">Battery is charged.</div>\n" +
                    "\t\t<div class=\"line_right\"></div>\n" +
                    "\t</div>\n" );
        }


        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">Battery status:</div>\n" +
                "\t\t<div class=\"line_right\">" + batteryPct*100 + "%</div>\n" +
                "\t</div>\n" );

        // Netz
        String operator = telephonyManager.getNetworkOperatorName();

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">network operator:</div>\n" +
                "\t\t<div class=\"line_right\">" + operator + "</div>\n" +
                "\t</div>\n" );

        // Signalstärke Netz
        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">GSM signal strength:</div>\n" +
                "\t\t<div class=\"line_right\">" + signalStrength + "dBm</div>\n" +
                "\t</div>\n" );


        // Signalstärke WLAN
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        int linkSpeed = wifiManager.getConnectionInfo().getRssi();

        html.append( "\t<div class=\"line\">\n" +
                "\t\t<div class=\"line_left\">WiFi signal strength:</div>\n" +
                "\t\t<div class=\"line_right\">" + linkSpeed + "dBm</div>\n" +
                "\t</div>\n" );

        html.append( "</div>\n\n" +
                "<div class=\"freeline\"></div>\n" +
                "<div class=\"freeline\"></div>\n" +
                "<div class=\"freeline\"></div>\n" +
                "<div class=\"freeline\"></div>\n" );

        StringBuilder page = new StringBuilder(Utility.openHTMLString(mContext, R.raw.device));

        int idx = page.indexOf(DEVICE_PATTERN);
        page.replace(idx, idx+DEVICE_PATTERN.length(), html.toString() );

        return page.toString();

    }

    public static void setGsmSignalStrengthValue( int value ){


        signalStrength = value;

    }

    //---------------- PUPLIC area of the static class MainMenu ---------------------------------END


}
