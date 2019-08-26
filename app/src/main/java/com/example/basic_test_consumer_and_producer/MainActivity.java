package com.example.basic_test_consumer_and_producer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final int CONSUMER_SEND_RATE = 1000; // ms per interest

    private static final int PRODUCER = 0, CONSUMER = 1;
    private static int mode;

    public static final int MSG_DATA_RECEIVED = 0;

    public static final String STREAM_NAME = "/test";

    MainActivity mainActivity_;
    Handler handler_;
    NetworkThreadConsumer networkThreadConsumer_;
    NetworkThreadProducer networkThreadProducer_;

    public static int numOutstandingInterests_ = 0;
    long currentSegNum_ = 0;
    Name currentStreamName_;

    boolean start_ = false;

    Button chooseModeButton_;
    Button startButton_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainActivity_ = this;

        currentStreamName_ = new Name(STREAM_NAME);

        chooseModeButton_ = (Button) findViewById(R.id.choose_mode_button);
        chooseModeButton_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (chooseModeButton_.getText().toString().equals(getString(R.string.producer_mode))) {
                    chooseModeButton_.setText(getString(R.string.consumer_mode));
                }
                else {
                    chooseModeButton_.setText(getString(R.string.producer_mode));
                }
            }
        });

        startButton_ = (Button) findViewById(R.id.start_button);
        startButton_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                chooseModeButton_.setEnabled(false);
                startButton_.setEnabled(false);

                if (chooseModeButton_.getText().toString().equals(getString(R.string.producer_mode)))
                    mode = PRODUCER;
                else
                    mode = CONSUMER;

                switch (mode) {
                    case PRODUCER:
                        networkThreadProducer_ = new NetworkThreadProducer(currentStreamName_);
                        networkThreadProducer_.start();
                        break;
                    case CONSUMER:
                        handler_ = new Handler(mainActivity_.getMainLooper()) {
                            @Override
                            public void handleMessage(@NonNull Message msg) {
                                super.handleMessage(msg);
                                switch (msg.what) {
                                    case MSG_DATA_RECEIVED:
                                        Data data = (Data) msg.obj;
                                        Log.d(TAG, System.currentTimeMillis() + ": " +
                                                "received data (" +
                                                "name: " + data.getName().toString() +
                                                ")");
                                        break;
                                    default:
                                        throw new IllegalStateException();
                                }
                            }
                        };

                        networkThreadConsumer_ = new NetworkThreadConsumer(handler_);
                        networkThreadConsumer_.start();
                        while (networkThreadConsumer_.getHandler() == null) {}

                        while (true) {
                            Name interestName = new Name(currentStreamName_);
                            Interest interest = new Interest(interestName.appendSegment(currentSegNum_));
                            currentSegNum_++;
                            networkThreadConsumer_.getHandler()
                                    .obtainMessage(NetworkThreadConsumer.MSG_INTEREST_SEND_REQUEST, interest)
                                    .sendToTarget();
                            SystemClock.sleep(CONSUMER_SEND_RATE);
                        }
                }
            }
        });

    }

    public static void modifyNumOutstandingInterests(int modifier) {
        Log.d(TAG, System.currentTimeMillis() + ": " +
                "modifying numOutstandingInterests_ (" +
                "current value: " + numOutstandingInterests_ + ", " +
                "modifier: " + modifier +
                ")");
        numOutstandingInterests_ += modifier;
    }

}
