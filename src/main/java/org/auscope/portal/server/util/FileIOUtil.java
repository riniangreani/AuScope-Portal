package org.auscope.portal.server.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;

public class FileIOUtil {

    public static String CovertExceptionToString(Exception ex,String debugQuery){
        StringWriter sw = null;
        PrintWriter pw = null;
        String message="";
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            message = String.format("An exception occured.\r\n%1$s\r\nMessage=%2$s\r\n%3$s",debugQuery, ex.getMessage(), sw.toString());
        } finally {
            try {
                if (pw != null)  pw.close();
                if (sw != null)  sw.close();
            } catch (Exception ignore) {}
        }
        return message;
    }

    public static String convertStreamtoString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        is.close();
        return sb.toString();
    }
}