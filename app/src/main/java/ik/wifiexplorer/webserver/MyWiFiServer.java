package ik.wifiexplorer.webserver;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;
import android.webkit.MimeTypeMap;

import ik.wifiexplorer.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;



public class MyWiFiServer extends NanoHTTPD {

    private final static int PORT = 8080;
    private String DIR_PATTERN = "--SD-PATH--";
    //private String SIZE_PATTERN = "--SIZE--";
    //private String ELEMENTS_PATTERN = "--ELEMENTS--";
    private File filepath = Environment.getExternalStorageDirectory();
    private Context context = null;
    private StringBuilder uploadFile = new StringBuilder();

    public MyWiFiServer( Context context ) throws IOException {
        super(PORT);
        start();
        this.context = context;
        MainMenu.setContext( context, filepath );
    }

    @Override
    public Response serve(IHTTPSession session) {

        StringBuilder htmlRequest = new StringBuilder();
        Method method = session.getMethod();

        // handle ALL GET-methods here:
        if ( Method.GET.equals(method) ){

            String requestString = session.getUri();


            ////////////////////////////////////////////////////////////////  HomePage call ------
            // The call is coming from the client, using GET-method
            if (requestString.equals("/")) {
                htmlRequest.append( Utility.openHTMLString(context, R.raw.index) );
                return newFixedLengthResponse( htmlRequest.toString() );
            }
            // load top.html, this is the respont to the client's GET-Request
            if (requestString.equals("/top.html")) {
                htmlRequest.append( Utility.openHTMLString(context, R.raw.top) );
                return newFixedLengthResponse( htmlRequest.toString() );
            }
            // load left.html, this is the respont to the client's GET-Request
            if (requestString.equals("/left.html")) {
                StringBuilder menu = new StringBuilder( Utility.openHTMLString(context, R.raw.left) );
                String menuPage  = "\t<div class=\"folder_parent\">";
                       menuPage += "\t\t<a href=\"javascript:onclick=parent.left.location=('/dir" + filepath + "'); parent.main.location=('/fileList');\">";
                       menuPage += "\t\t\t<div class=\"folder_closed\">" + "SD-card" + "</div>";
                       menuPage += "\t\t</a>";
                       menuPage += "\t</div>";
                //String menuPage = "<tr><td><a href=\"javascript:onclick=parent.left.location=('/dir" + filepath + "'); parent.main.location=('/fileList');\">" + "SD-Karte</a><br></td></tr>\n";
                int idx = menu.indexOf(DIR_PATTERN);
                menu.replace(idx, idx+DIR_PATTERN.length(), menuPage);
                htmlRequest.append( menu.toString() );
                return newFixedLengthResponse( htmlRequest.toString() );
            }
            // load main.html, respont to the client's GET-Request
            if (requestString.equals("/info.html")) {
                htmlRequest.append( Utility.openHTMLString(context, R.raw.info) );
                return newFixedLengthResponse( htmlRequest.toString() );
            }

            // load CSS-stylesheets, respont to the client's GET-Request:
            if (requestString.endsWith(".css")) {
                // CSS-sheet for main.html
                if ( requestString.contains("style_main") ) {
                    InputStream mbuffer = context.getResources().openRawResource(R.raw.style_main);
                    return newFixedLengthResponse(Response.Status.OK, "text/css", mbuffer, htmlRequest.toString().length());
                }
                // CSS-sheet for left.html
                if ( requestString.contains("style_left") ){
                    InputStream mbuffer = context.getResources().openRawResource(R.raw.style_left);
                    return newFixedLengthResponse(Response.Status.OK, "text/css", mbuffer, htmlRequest.toString().length());
                }
                // CSS-sheet for device.html
                if ( requestString.contains("style_device") ){
                    InputStream mbuffer = context.getResources().openRawResource(R.raw.style_device);
                    return newFixedLengthResponse(Response.Status.OK, "text/css", mbuffer, htmlRequest.toString().length());
                }
            }

            // load JavaScripts-Files laden:
            if (requestString.endsWith(".js")) {
                if ( requestString.contains("javascript_left") ) {
                    InputStream mbuffer = context.getResources().openRawResource(R.raw.javascript_left);
                    return newFixedLengthResponse(Response.Status.OK, "text/javascript", mbuffer, htmlRequest.toString().length());
                }
            }

            /////////////////////////////////////////////////////  HomePage call <<<------END-------


            // React to the click on one of the items in the left frame (onItemClick) --->> UpdatePage
            if ( requestString.length() > 4) {
                if ( requestString.substring(0,4).equals("/dir") ) {
                    String link = requestString.substring( 4, requestString.length() );
                    MainMenu.onItemClick(link);
                    htmlRequest.append( MainMenu.updateDirectoryView() );
                    return newFixedLengthResponse( htmlRequest.toString() );
                }
            }


            // Show Device-Info
            // load device.html laden
            if (requestString.equals("/device")) {

                return newFixedLengthResponse( MainMenu.createDeviceInfoPage() );

            }

            // after onItemClick: Update FileView
            if ( requestString.length() > 8) {
                if ( requestString.substring(0,9).equals("/fileList") ) {
                    SystemClock.sleep(30);
                    htmlRequest.append(MainMenu.updateFileView());
                    return newFixedLengthResponse(htmlRequest.toString());
                }
            }


            // if starting a download:
            if ( requestString.length() > 8) {
                if ( requestString.substring(0,9).equals("/download") ) {
                    return downloadFile( requestString );
                }
            }



            // create an open a new folfer:
            if ( requestString.length() > 7) {
                if (requestString.substring(0, 8).equals("/makedir")) {
                    String path       = requestString.substring(8, requestString.length());
                    String tmp        = session.getQueryParameterString();
                    String newDirName = tmp.substring(8, tmp.length());
                    return createNewDirectory( path, newDirName );
                }
            }


            // Fileupload: Part 1...Here the filename is transmitten using the GET-method
            if (requestString.length() > 6) {
                if (requestString.substring(0, 7).equals("/upload")) {
                    uploadFile.append(requestString.substring(8, requestString.length()));
                }
            }


            // rename a file:
            if (requestString.length() > 6) {
                if (requestString.substring(0, 7).equals("/rename")) {
                    String filename = requestString.substring(7, requestString.length());
                    int idx = filename.lastIndexOf("/");
                    String path = filename.substring(0, idx + 1);
                    String fileOld = filename.substring( idx+1, filename.length() );
                    String fileNew = session.getQueryParameterString();
                    fileNew = fileNew.replaceAll("%20", " ");
                    return renameFile( path, fileOld, fileNew );
                }
            }


            // delete a file:
            if (requestString.length() > 6) {
                if (requestString.substring(0, 7).equals("/delete")) {
                    String filename = requestString.substring(7, requestString.length());
                    int idx = filename.lastIndexOf("/");
                    String path = filename.substring(0, idx + 1);
                    String file = filename.substring( idx+1, filename.length() );
                    return deleteFile( path, file );
                }
            }

        }



        if ( Method.POST.equals(method) ){

            // handle ALL PUT-methods here:

            String requestString = session.getUri();
            Map<String, String> params = session.getParms();
            params.isEmpty();


            // Fileupload Part 2.......here the content of the file is transmitted to the device:
            if (requestString.length() > 6) {
                if (requestString.substring(0, 7).equals("/upload")) {

                    Map<String, String> files = new HashMap<>();
                    try { session.parseBody(files); }
                    catch (IOException | ResponseException e1) { e1.printStackTrace(); }

                    String parent_path = requestString.substring( 7, requestString.length()) + "/";
                    File source = new File(files.get("File"));
                    File destination = new File( parent_path + uploadFile );
                    try {
                        copyFile(source, destination);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    uploadFile.setLength(0);

                    MainMenu.createNewFileListItem( parent_path );
                    htmlRequest.append(MainMenu.updateFileView());
                    return newFixedLengthResponse(htmlRequest.toString());

                }
            }

        }


        return newFixedLengthResponse( "ERROR!!!" );

    }





    private Response downloadFile( String request ){

        String path = "";

        path += request.substring( 9, request.length() );
        File file = new File( path );
        long fileLen = file.length();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream( file );
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String mime = getMimeType(path);
        Response res = newFixedLengthResponse(Response.Status.OK, mime, fis, (int) fileLen);
        res.addHeader("Accept-Ranges", "bytes");

        return res;
    }




    // Create and open a new folder:
    private Response createNewDirectory( String path, String newDirName ){

        // newDirName und path filtern / Prüfen:
        // Streiche '+' Setze ' '
        // Streiche '%' Setze ' '
        newDirName = newDirName.replaceAll("\\+", " ");
        //path = path.replaceAll("%", " ");

        createDirIfNotExists(path + "/" + newDirName);
        MainMenu.createNewDirectoryListItem(path, newDirName);

        return newFixedLengthResponse( MainMenu.updateDirectoryView() );

    }


    // delete file:
    private Response deleteFile( String parent_path, String filename ){

        File f = new File( parent_path, filename);
        final boolean delete = f.delete();

        if ( delete ) {

            MainMenu.createNewFileListItem(parent_path);

        }

        return newFixedLengthResponse(MainMenu.updateFileView());

    }


    // rename file:
    private Response renameFile( String path, String oldFilename, String newFilename){

        File old  = new File( path, oldFilename );
        File ne_w = new File( path, newFilename );

        final boolean renameTo = old.renameTo(ne_w);

        if ( renameTo ){

            MainMenu.createNewFileListItem(path);

        }

        // TODO: Ist das richtig so?
        //return newFixedLengthResponse( MainMenu.updateFileView() );
        return newFixedLengthResponse( null );

    }



    ///// some helper-methods:

    // create a new folder, if it is not exists;
    private static boolean createDirIfNotExists(String path) {
        boolean ret = true;

        File file = new File( path );
        if (!file.exists()) {
            if (!file.mkdirs()) {
                ret = false;
            }
        }
        return ret;
    }


    // determine the MIME-type of a file
    private static String getMimeType(String fileUrl) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }


    // copy file:
    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        int cnt = 0;
        while ((len = in.read(buf)) > 0) {
            if ( cnt == 0 ) {
                out.write(buf, 1, len-1);
            }
            else {
                out.write(buf, 0, len);
            }

            cnt += 1;
        }
        in.close();
        out.close();
    }

}
