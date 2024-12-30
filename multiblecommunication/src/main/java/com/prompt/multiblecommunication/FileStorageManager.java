package com.prompt.multiblecommunication;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileStorageManager {
    private final Context context;

    public FileStorageManager(Context context) {
        this.context = context;
    }

    // Method to save data to a file
    public void saveToFile(String filename, String data) throws IOException {
        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
            fos.write(data.getBytes());
        }
    }

    // Method to read data from a file
    public String readFromFile(String filename) throws IOException {
        try (FileInputStream fis = context.openFileInput(filename);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString().trim();
        }
    }

    public String isMacContainsInStorage(String macAddress) throws IOException {
        File file = new File(context.getFilesDir(), Utils.INSTANCE.getFileName());
        if (file.exists()) {
            String existingData = readFromFile(Utils.INSTANCE.getFileName());
            boolean isMacExist = existingData.contains(macAddress);
            if (isMacExist) {
                int lineNumber = getLineNumber(existingData, macAddress);
                List<String> lines = new ArrayList<>();
                try (FileInputStream fis = context.openFileInput(Utils.INSTANCE.getFileName());
                     InputStreamReader isr = new InputStreamReader(fis);
                     BufferedReader reader = new BufferedReader(isr)) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                }
                return lines.get(lineNumber);
            }
        }
        return "";
    }

    // Update data in a file
    public void updateFile(String filename, String newData) throws IOException {
        File file = new File(context.getFilesDir(), filename);
        if (file.exists()) {

            String existingData = readFromFile(filename);
            boolean isMacExist = existingData.contains(newData.substring(0, 17));

            if (isMacExist) {
                int lineNumber = getLineNumber(existingData, newData.substring(0, 17));
                replaceLineInFile(Utils.INSTANCE.getFileName(), lineNumber, newData);
                return;
            }

            // File exists, append data
            String updatedData = existingData + "\n" + newData;
            saveToFile(filename, updatedData);
        } else {
            // File does not exist, create and write data
            saveToFile(filename, newData);
        }
    }

    public int getLineNumber(String text, String targetLine) {
        String[] lines = text.split("\n"); // Split text into lines
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(targetLine)) {
                return i; // Return the zero-based line number
            }
        }
        return -1; // Return -1 if the line is not found
    }

    // Replace a specific line in a file
    public void replaceLineInFile(String filename, int lineNumber, String newLine) throws IOException {
        File file = new File(context.getFilesDir(), filename);
        if (!file.exists()) {
            throw new IOException("File does not exist.");
        }

        // Read all lines from the file
        List<String> lines = new ArrayList<>();
        try (FileInputStream fis = context.openFileInput(filename);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader reader = new BufferedReader(isr)) {

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        // Replace the specific line
        if (lineNumber >= 0 && lineNumber < lines.size()) {
            lines.set(lineNumber, newLine);
        } else {
            throw new IndexOutOfBoundsException("Invalid line number.");
        }

        // Write the updated lines back to the file
        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
            for (String line : lines) {
                fos.write((line + "\n").getBytes());
            }
        }
    }

    // Method to delete a file
    public boolean deleteFile(String filename) {
        return context.deleteFile(filename);
    }
}