package com.xst.AnonymousChat;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.xst.AnonymousChat.EncryptionUtils;

@SuppressLint("UseSwitchCompatOrMaterialCode")
public class MainActivity extends AppCompatActivity {
    private EditText mIpText;
    private EditText mPortText;
    private EditText mNicknameText;
    // Initialize views
    private Button mConnectBtn;
    private LinearLayout mMessageLayout;
    private ScrollView mChatHistory;
    private EditText mEditText;
    private Button mSendBtn;

    private Socket mClient;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private Switch mySwitch;
    private static final String ALGORITHM = "AES";
    private static final String MODE = "AES/CBC/PKCS5Padding";

    private static final String keyString = "aaaaaaaaaaaaaaaa";
    private static final String ivString = "aaaaaaaaaaaaaaaa";

    private static final SecretKeySpec key = new SecretKeySpec(keyString.getBytes(), ALGORITHM);
    private static final IvParameterSpec iv = new IvParameterSpec(ivString.getBytes());
    private static final String TAG = "MainActivity";




    private boolean mIsConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_main);

        mNicknameText = findViewById(R.id.nickname_text);
        mIpText = findViewById(R.id.ip_text);
        mConnectBtn = findViewById(R.id.connect_btn);
        //mTextView = findViewById(R.id.text_view);
        mMessageLayout = findViewById(R.id.message_layout);
        mChatHistory = findViewById(R.id.chat_history);
        mySwitch = findViewById(R.id.cserver);
        mPortText = findViewById(R.id.port_text);

// Initialize the EditText and Button views for sending messages
        mEditText = findViewById(R.id.edit_text);
        mSendBtn = findViewById(R.id.send_btn);
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    TextView ip_label = findViewById(R.id.ip_label);
                    mIpText.setVisibility(View.VISIBLE);
                    ip_label.setVisibility(View.VISIBLE);
                    mPortText.setVisibility(View.VISIBLE);
                    TextView port_label = findViewById(R.id.port_label);
                    port_label.setVisibility(View.VISIBLE);
                } else {
                    TextView ip_label = findViewById(R.id.ip_label);
                    mIpText.setVisibility(View.GONE);
                    ip_label.setVisibility(View.GONE);
                    mPortText.setVisibility(View.GONE);
                    TextView port_label = findViewById(R.id.port_label);
                    port_label.setVisibility(View.GONE);
                }
            }
        });
        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToServer();
                //HideWidgets
                TextView ip_label = findViewById(R.id.ip_label);
                TextView nick_label = findViewById(R.id.nick_label);
                TextView port_label = findViewById(R.id.port_label);
                ip_label.setVisibility(View.GONE);
                nick_label.setVisibility(View.GONE);
                port_label.setVisibility(View.GONE);
                mPortText.setVisibility(View.GONE);
                mConnectBtn.setVisibility(View.GONE);
                mNicknameText.setVisibility(View.GONE);
                mIpText.setVisibility(View.GONE);
                mySwitch.setVisibility(View.GONE);
            }
        });
        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void connectToServer() {
        // Get the IP address and nickname entered by the user
        //final String ipAddress = mIpText.getText().toString();
        final String nickname = mNicknameText.getText().toString();

        // Create a new thread to connect to the server
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Connect to the server
                    if (mIpText.getText().toString().isBlank()) {
                        String ipAddress = "chat.hccn.xyz";
                        mClient = new Socket(ipAddress, 4854);
                    } else {
                        String ipAddress = mIpText.getText().toString();
                        String sport = mPortText.getText().toString();
                        int port = Integer.parseInt(sport);
                        mClient = new Socket(ipAddress, port);
                    }
                    mIsConnected = true;

                    // Get the output and input streams from the socket
                    mOutputStream = mClient.getOutputStream();
                    mInputStream = mClient.getInputStream();

                    // Send the nickname to the server
                    //sendMessage(nickname);

                    // Start listening for incoming messages
                    startListening();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startListening() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytesRead;

                while (mIsConnected) {
                    try {
                        // Read incoming messages from the server
                        bytesRead = mInputStream.read(buffer);
                        if (bytesRead != -1) {
                            String message = EncryptionUtils.decrypt(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                            Log.d(TAG, "Decrypted message: " + message);

                            if (message.equals("NICK")) {
                                String nick = EncryptionUtils.encrypt(mNicknameText.getText().toString());
                                mOutputStream.write(nick.getBytes());
                            } else {
                                String[] List = message.split(":");
                                String nick = mNicknameText.getText().toString();
                                if(!Objects.equals(List[0], nick)){
                                    sendNotification(message);
                                }
                                showMessage(message);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }
    private void sendMessage(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Check if the output stream is null
                    if (mOutputStream == null) {
                        // Reconnect to the server or show an error message
                        connectToServer(); // Example: reconnect to the server
                        return;
                    }
                    // Send the message to the server
                    String nick = mNicknameText.getText().toString();
                    String modified = nick + ":" + message;
                    String modified_enc = EncryptionUtils.encrypt(modified);
                    mOutputStream.write(modified_enc.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void sendMessage() {
        String message = mEditText.getText().toString().trim();
        if (!message.isEmpty()) {
            sendMessage(message);
            mEditText.setText("");
        }
    }

    private void showMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Create a new TextView to display the message
                TextView textView = new TextView(MainActivity.this);
                textView.setText(message);
                textView.setTextSize(16);

                // Add the TextView to the message layout
                mMessageLayout.addView(textView);

                String[] List = message.split(":");
                String nick = mNicknameText.getText().toString();
                if(!Objects.equals(List[0], nick)){
                    sendNotification(message);
                }

                // Scroll to the bottom of the chat history
                mChatHistory.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void sendNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New Message")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Create the notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("channel_id", name, importance);
            channel.setDescription(description);
            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    protected void onDestroy() {
        super.onDestroy();
        if (mIsConnected) {
            try {
                mClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
