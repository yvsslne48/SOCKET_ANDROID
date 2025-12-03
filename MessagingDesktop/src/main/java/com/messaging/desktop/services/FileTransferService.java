package com.messaging.desktop.services;

import java.io.*;
        import java.nio.file.*;

/**
 * Service for file operations
 * Handles reading and saving files for message attachments
 */
public class FileTransferService {

    private static final String DOWNLOAD_DIR = System.getProperty("user.home") + "/MessagingDownloads/";

    public FileTransferService() {
        // Create downloads directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(DOWNLOAD_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create download directory: " + e.getMessage());
        }
    }

    /**
     * Read file as byte array
     */
    public byte[] readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
    }

    /**
     * Read file from File object
     */
    public byte[] readFile(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Save byte array to file
     */
    public void saveFile(byte[] data, String fileName) throws IOException {
        Path path = Paths.get(DOWNLOAD_DIR + fileName);
        Files.write(path, data);
        System.out.println("File saved to: " + path.toAbsolutePath());
    }

    /**
     * Get download directory path
     */
    public String getDownloadDirectory() {
        return DOWNLOAD_DIR;
    }

    /**
     * Open file location in system file explorer
     */
    public void openFileLocation(String fileName) {
        try {
            File file = new File(DOWNLOAD_DIR + fileName);
            if (file.exists()) {
                // Open parent directory
                java.awt.Desktop.getDesktop().open(file.getParentFile());
            }
        } catch (IOException e) {
            System.err.println("Failed to open file location: " + e.getMessage());
        }
    }
}