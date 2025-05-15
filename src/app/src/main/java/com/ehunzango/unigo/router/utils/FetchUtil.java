package com.ehunzango.unigo.router.utils;

import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchUtil {

    private static final int BUFFER_SIZE = 8192;

    public static void downloadFile(String urlString, File outputFile) throws IOException {
        Log.d("DataFetchUtil", "Downloading from: " + urlString);
        Log.d("DataFetchUtil", "Saving to: " + outputFile.getAbsolutePath());

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(10000);
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Server returned HTTP " + connection.getResponseCode()
                    + " " + connection.getResponseMessage());
        }

        try (
                InputStream input = new BufferedInputStream(connection.getInputStream());
                OutputStream output = new FileOutputStream(outputFile)
        ) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.flush();
        } finally {
            connection.disconnect();
        }

        Log.d("DataFetchUtil", "Download completed.");
    }
}

