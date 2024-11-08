package com.example.cnproject;

import  android.content.pm.PackageManager;
import android.media.*;
import android.os.*;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class BroadCastActivity extends AppCompatActivity {

    private static final int PORT = 5000;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 4096;  // Set both sides to the same buffer size
    private AudioRecord audioRecord;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private TextView listenUpdate;
    private ImageView listenUpdateImg;
    int imageResourse =R.drawable.img_2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_broad_cast);
        listenUpdate = findViewById(R.id.listenFlag);
        listenUpdateImg = findViewById(R.id.listenFlagImg);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Request microphone permission
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 200);
        } else {
            startAudioCapture();
        }

        // Start server socket
        new Thread(this::startServerSocket).start();
    }

    private void startAudioCapture() {
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
            audioRecord.startRecording();

            new Thread(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (true) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        Log.d("BroadCastActivity", "Captured audio data: " + read + " bytes");
                        runOnUiThread(() -> listenUpdate.setText("Captured audio data: " + read + " bytes"));
                        runOnUiThread(() -> listenUpdateImg.setImageResource(imageResourse));
                        sendAudioData(buffer, read);  // Send captured audio data
                    }
                }
            }).start();
        } catch (Exception e) {
            Log.e("BroadCastActivity", "Error starting AudioRecord: " + e.getMessage());
        }
    }
    //testing
    private void startServerSocket() {
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            serverSocket = new ServerSocket(PORT, 0, InetAddress.getByName(ipAddress));
            Log.d("BroadCastActivity", "Server listening on port " + PORT + " at IP address " + ipAddress);

            clientSocket = serverSocket.accept();
            Log.d("BroadCastActivity", "Client connected: " + clientSocket.getInetAddress());
            runOnUiThread(() -> listenUpdate.setText("Server listening on port " + PORT + " at IP address " + ipAddress));

        } catch (Exception e) {
            Log.e("BroadCastActivity", "Error with ServerSocket: " + e.getMessage());
        }
    }

    private void sendAudioData(byte[] buffer, int length) {
        if (clientSocket != null && clientSocket.isConnected()) {
            try {
                OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write(buffer, 0, length);
                outputStream.flush();  // Flush to ensure data is sent
            } catch (Exception e) {
                Log.e("BroadCastActivity", "Error sending audio data: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (Exception e) {
            Log.e("BroadCastActivity", "Error closing socket: " + e.getMessage());
        }
    }
}
