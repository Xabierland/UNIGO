package com.ehunzango.unigo.router.utils;

import java.io.*;
import java.util.zip.*;

// TODO: fetch the zip from the web (all the cool kids do it)
// TODO: Im sure this wont work because I tested it on a normal java proyect :)
// // gtfs.zip path
// File zipFile = new File(context.getFilesDir(), "data/gtfs.zip");
//
//         // Directory to unzip to
//         File unzipDir = new File(context.getFilesDir(), "data/gtfs/");
//
// // Create directories if they don't exist
// if (!unzipDir.exists() && !unzipDir.mkdirs()) {
//         throw new IOException("Failed to create directory: " + unzipDir.getAbsolutePath());
//         }
//
// // Use your unzip utility
//         ZipUtils.unzip(zipFile.getAbsolutePath(), unzipDir.getAbsolutePath());
public class ZipUtils {
    private static final int BUFFER_SIZE = 8192;

    // src: https://stackoverflow.com/questions/3612660/utility-to-unzip-an-entire-archive-to-a-directory-in-java
    public static void unzip(InputStream zipInputStream, File destDirectory) throws IOException {
        if (!destDirectory.exists() && !destDirectory.mkdirs()) {
            throw new IOException("Could not create directory " + destDirectory);
        }

        try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(zipInputStream))) {
            ZipEntry entry;

            while((entry = zipIn.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Protect against Zip Slip vulnerability
                File destFile = new File(destDirectory, entryName);
                String canonicalDestDir = destDirectory.getCanonicalPath();
                String canonicalDestFile = destFile.getCanonicalPath();
                if (!canonicalDestFile.startsWith(canonicalDestDir + File.separator)) {
                    throw new IOException("Entry is outside of the target dir: " + entryName);
                }

                if (entry.isDirectory()) {
                    if (!destFile.exists() && !destFile.mkdirs()) {
                        throw new IOException("Could not create directory: " + destFile);
                    }
                } else {
                    File parent = destFile.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Could not create directory: " + parent);
                    }
                    extractFile(zipIn, destFile);
                    removeBomFromFile(destFile);  // Strip BOM after extraction
                }
                zipIn.closeEntry();
            }
        }
    }

    private static void extractFile(ZipInputStream zipIn, File outFile) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = zipIn.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
        }
    }

    // Method to remove BOM from a file after extraction
    private static void removeBomFromFile(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            byte[] bom = new byte[3];
            raf.read(bom);

            // Check if BOM is present at the beginning of the file
            if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                // Shift the file content to remove BOM
                byte[] content = new byte[(int) raf.length() - 3];
                raf.read(content);
                raf.seek(0);
                raf.write(content);
                raf.setLength(content.length); // Truncate file to new size, I hate my life...
            }
        }
    }
}

