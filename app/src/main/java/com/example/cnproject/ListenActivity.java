package com.example.cnproject;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.Socket;

public class ListenActivity extends AppCompatActivity {

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 4096;  // Use the same buffer size as the server

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_listen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new Thread(() -> {
            try {
                Socket socket = new Socket("192.168.137.183", 5000);  // server IP
                socket.setSoTimeout(15000);  // Optional, increase timeout

                InputStream inputStream = new BufferedInputStream(socket.getInputStream());

                AudioTrack audioTrack = createAudioTrack();
                if (audioTrack != null) {
                    audioTrack.play();

                    byte[] audioBuffer = new byte[BUFFER_SIZE];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(audioBuffer)) != -1) {
                        audioTrack.write(audioBuffer, 0, bytesRead);
                        Log.d("ListenActivity", "Received audio data: " + bytesRead + " bytes");
                    }
                }

                closeResources(socket, inputStream, audioTrack);

            } catch (Exception e) {
                Log.e("ListenActivity", "Error in network or audio playback: " + e.getMessage());
            }
        }).start();
    }

    private AudioTrack createAudioTrack() {
        try {
            return new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    BUFFER_SIZE,
                    AudioTrack.MODE_STREAM
            );
        } catch (Exception e) {
            Log.e("ListenActivity", "Error creating AudioTrack: " + e.getMessage());
            return null;
        }
    }

    private void closeResources(Socket socket, InputStream inputStream, AudioTrack audioTrack) {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) {
                Log.e("ListenActivity", "Error closing input stream: " + e.getMessage());
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                Log.e("ListenActivity", "Error closing socket: " + e.getMessage());
            }
        }
    }
}
