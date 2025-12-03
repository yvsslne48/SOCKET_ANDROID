package com.messaging.android.services;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.*;

/**
 * Service for file operations on Android
 * Handles reading and saving files for message attachments
 */
public class FileTransferService {

    private final Context context;
    private final String downloadDir;

    public FileTransferService(Context context) {
        this.context = context;
        // Use app-specific external storage directory
        // This doesn't require WRITE_EXTERNAL_STORAGE permission on Android 10+
        File externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (externalFilesDir != null) {
            this.downloadDir = externalFilesDir.getAbsolutePath() + "/";
        } else {
            // Fallback to internal storage
            this.downloadDir = context.getFilesDir().getAbsolutePath() + "/downloads/";
        }

        // Create directory if it doesn't exist
        File dir = new File(downloadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Read file as byte array from URI path
     */
    public byte[] readFile(String filePath) throws IOException {
        File file = new File(filePath);
        return readFile(file);
    }

    /**
     * Read file from File object
     */
    public byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int length;

        while ((length = fis.read(buffer)) != -1) {
            bos.write(buffer, 0, length);
        }

        fis.close();
        return bos.toByteArray();
    }

    /**
     * Read file from InputStream (used with ContentResolver)
     */
    public byte[] readFile(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int length;

        while ((length = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, length);
        }

        inputStream.close();
        return bos.toByteArray();
    }

    /**
     * Save byte array to file
     */
    public File saveFile(byte[] data, String fileName) throws IOException {
        File file = new File(downloadDir + fileName);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();

        return file;
    }

    /**
     * Save byte array to file and show toast
     */
    public File saveFileWithNotification(byte[] data, String fileName) throws IOException {
        File file = saveFile(data, fileName);
        showToast("File saved: " + fileName);
        return file;
    }

    /**
     * Get download directory path
     */
    public String getDownloadDirectory() {
        return downloadDir;
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String fileName) {
        File file = new File(downloadDir + fileName);
        return file.exists();
    }

    /**
     * Get file size in bytes
     */
    public long getFileSize(String fileName) {
        File file = new File(downloadDir + fileName);
        if (file.exists()) {
            return file.length();
        }
        return 0;
    }

    /**
     * Delete file
     */
    public boolean deleteFile(String fileName) {
        File file = new File(downloadDir + fileName);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * Get file object
     */
    public File getFile(String fileName) {
        return new File(downloadDir + fileName);
    }

    /**
     * Format file size for display
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Show toast message
     */
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}