package ik.wifiexplorer.webserver;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.Enumeration;

import static android.content.Context.WIFI_SERVICE;


public class Utility {
    public static String getLocalIpAddress( Context context ) {
        String ip = "";


        /*
        try {


            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    //if (inetAddress.isSiteLocalAddress()) {
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.isSiteLocalAddress()) {
                        ip += inetAddress.getHostAddress();
                    }

                }

            }


        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }
        */

        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        try {
            ip = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            ip += "Something Wrong! \n";
        }


        return ip;

    }

    public static String convertStreamToString(InputStream is) {
                /*
                 * To convert the InputStream to String we use the Reader.read(char[]
                 * buffer) method. We iterate until the Reader return -1 which means
                 * there's no more data to read. We use the StringWriter class to
                 * produce the string.
                 */
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return writer.toString();
        } else {
            return "";
        }
    }

    public static String openHTMLString(Context context, int id){
        InputStream is = context.getResources().openRawResource(id);

        return Utility.convertStreamToString(is);
    }


    public static  String parseSize( long filesize ){

        int digits = new String(filesize + "").length();
        DecimalFormat df = new DecimalFormat("0.00");

        if ( digits < 4 )  { return new String( filesize + " Byte");}
        if ( digits < 7 )  { return new String( df.format(filesize/1024.0) + " KB");}
        if ( digits < 10 ) { return new String( df.format(filesize/1024.0/1024.0) + " MB");}

        return new String( df.format(filesize/1024.0/1024.0/1024.0) + " GB");

    }
}
