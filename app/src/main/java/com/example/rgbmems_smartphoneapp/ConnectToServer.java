package com.example.rgbmems_smartphoneapp;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.os.Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.json.JSONException;
import org.json.JSONObject;

public class ConnectToServer {
    public static Socket client;
    public String serverIp = "10.0.2.2"; // Change to your server's IP address
    public int serverPort = 8000; // Port on which the server is listening
    private PendingMessage pendingMessage = null; // Store the pending message to be sent

    private Handler handler = new Handler(); // Initialize the Handler
    private TextView responseTextView; // Declare TextView

    public void setResponseTextView(TextView responseTextView) {
        this.responseTextView = responseTextView; // Assign TextView from MainActivity
    }

    public void setPendingMessage(String messageType, int messageValue) {
        this.pendingMessage = new PendingMessage(messageType, messageValue);
        Log.d("ConnectServer", "Pending message stored: " + messageType + " - " + messageValue);

    }

    public void connectToServer(Context context) {
        new Thread(() -> {
            try {
                // Connect to socket
                if (client == null || client.isClosed()) {
                    client = new Socket(serverIp, serverPort);
                }

                if (client != null && client.isConnected()) {
                    // Connection successful
                    Log.d("ConnectServer", "Connected to server");

                    // Check if there is a pending message, send it immediately upon successful connection
                    if (pendingMessage != null) {
                        Log.d("ConnectServer", "Pending message found. Sending message...");
                        sendMessageToServer(context, pendingMessage.getName(), pendingMessage.getCheckNumber());
                        pendingMessage = null; // Clear the pending message after sending
                    }

                    // Read the response from the server
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        Log.d("ConnectServer", "Response from server: " + serverResponse);

                        // Update the user interface with the response from the server
                        String finalResponse = serverResponse;
                        ((MainActivity) context).runOnUiThread(() -> {
                            // Display the response from the server on the screen using TextView
                            if (responseTextView != null) {
                                updateResponseText(finalResponse); // Call the method to update and hide the TextView
                            }
                        });
                    }
                } else {
                    // Connection failed
                    Log.d("ClientThread", "Not connected to server!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("ConnectServer", "Error connecting to server", e);
            }
        }).start();
    }

    // Send message to the server
    public void sendMessageToServer(Context context, String type, int value) {
        new Thread(() -> {
            try {
                //if (client != null && !client.isClosed()) {
                if (isConnected()) {
                    // Create a JSON object from type and value
                    JSONObject jsonMessage = new JSONObject();
                    jsonMessage.put("type", type);
                    jsonMessage.put("value", value);

                    // Send message to the server
                    //メモoutputStreamはクラス内で定義していて使う前と使った後にクリアするのでここで定義不要
                    //OutputStream outputStream = client.getOutputStream();
                    synchronized (outputStream) {
                        outputStream.flush();
                        outputStream.write((jsonMessage.toString() + "\n").getBytes());
                        outputStream.flush();
                    }

                    Log.d("ClientThread", "Message sent to server: " + jsonMessage.toString());
                } else {
                    Log.e("ClientThread", "Connection not established. Message not sent.");
                }
            } catch (IOException | JSONException e) {
                Log.e("ConnectServer", "Error sending message to server", e);
            }
        }).start();
    }

    // Check if the client is connected to the server
    public boolean isConnected() {
        return client != null && client.isConnected() && !client.isClosed();
    }

    /*メモSocketClient側のdisconnectを採用
    // Disconnect from the server
    public void disconnect(Context context) {
        if (client != null && !client.isClosed()) {
            try {
                client.close(); // Close the connection
                client = null;
                Log.d("ConnectServer", "Disconnected from server");
            } catch (IOException e) {
                Log.e("ConnectServer", "Error while disconnecting", e);
            }
        } else {
            Log.d("ConnectServer", "Already disconnected.");
        }
    }
     */

    // Update the content of the TextView and hide it after a certain period of time
    public void updateResponseText(String response) {
        if (responseTextView != null) {
            responseTextView.setText(response);
            responseTextView.setVisibility(View.VISIBLE); // Show the TextView

            // Set a timer to hide the TextView
            handler.postDelayed(() -> responseTextView.setVisibility(View.GONE), 3000); // Hide after 3 seconds
        }
    }
    //********************************************
    //以下、元SocketClient.java記載部分
    //********************************************
    private static final String TAG = "SocketClient";   //最終的には"ConnectServer"に統一する

    private OutputStream outputStream; // Output stream for sending data
    private InputStream inputStream; // Input stream for receiving data
    private boolean isReconnecting = false; // Flag to check if a reconnection attempt is in progress


    public void connect() {
        new Thread(() -> {
            try {
                // Connect to socket
                if (client == null || client.isClosed()) {
                    client = new Socket(serverIp, serverPort);
                }
                // Set timeout for the socket
                client.setSoTimeout(10000); // 10 seconds timeout for receiving data
                outputStream = client.getOutputStream();
                inputStream = client.getInputStream();
                Log.d(TAG, "Connected to server");
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to server: " + e.getMessage(), e);
            }
        }).start();
    }

    // Continuously check connection; if not connected, attempt to reconnect
    private void ensureConnected() {
        if (!isConnected() && !isReconnecting) {
            isReconnecting = true; // Set flag to indicate reconnection attempt
            connect(); // Attempt to connect
            isReconnecting = false; // Reset flag after connection attempt
        }
    }

    /*
     メモisConnectedはConnectServer.javaにもともとあったものを採用
     Method to check connection status
     public boolean isConnected() {
         return client != null && client.isConnected();
     }
    */

    // Update sendImage method to send only the image without the image sequence number
    public void sendImage(byte[] imageData) {
        new Thread(() -> {
            ensureConnected(); // Ensure connection before sending image
            try {
                if (isConnected()) {
                    // Check image data
                    if (imageData == null || imageData.length == 0) {
                        Log.e(TAG, "Image data is null or empty");
                        return; // Return early if there is no image data
                    }
                    JSONObject jsonMessage_img = new JSONObject();
                    jsonMessage_img.put("type", "sendnumber");              //HOME画面の画像番号選択と同じでいい？
                    jsonMessage_img.put("value", SecondFragment.currentNumber);   //画像番号
                    jsonMessage_img.put("ImageDataByteArray", imageData);         //画像のbyte配列
                    // Send data through the socket
                    synchronized (outputStream) {
                        outputStream.flush(); // Clear old data if any
                        outputStream.write((jsonMessage_img.toString() + "\n").getBytes());
                        //メモjsonでデータを送るので再度送る必要ない？
                        //outputStream.write(imageData); // Write the image data to output stream
                        outputStream.flush(); // Ensure all data is sent
                    }

                    Log.d(TAG, "Image sent to server");

                    // Read response from server
                    byte[] responseBuffer = new byte[4096];
                    int bytesRead = inputStream.read(responseBuffer); // Read response
                    if (bytesRead > 0) {
                        String response = new String(responseBuffer, 0, bytesRead);
                        Log.d(TAG, "Server response: " + response);
                    } else {
                        Log.e(TAG, "No response from server");
                    }
                } else {
                    Log.e(TAG, "Socket is not connected");
                }
            } catch (SocketException e) {
                Log.e(TAG, "Socket error: " + e.getMessage(), e);
                reconnect(); // Reconnect if there is a socket issue
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Socket timeout: " + e.getMessage(), e);
                reconnect(); // Reconnect if there is a timeout
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error sending image: " + e.getMessage(), e);
                reconnect(); // Try reconnecting after an error
            }
        }).start();
    }

    // Attempt to reconnect in case of issues
    private void reconnect() {
        disconnect(); // Disconnect current socket
        connect(); // Attempt to reconnect
    }

    public void disconnect() {
        try {
            if (inputStream != null) {
                inputStream.close(); // Close input stream
            }
            if (outputStream != null) {
                outputStream.close(); // Close output stream
            }
            if (client != null && !client.isClosed()) {
                client.close(); // Close socket if it's not already closed
                Log.d(TAG, "Socket closed");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }
}

