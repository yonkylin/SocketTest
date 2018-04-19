package yonky.sockettest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    public static String TAG="SOCKET CLIENT";
    private static final int MESSAGE_RECEIVE_NEW_MSG=1;
    private static final int MESSAGE_SOCKET_CONNECTED = 2;

    private Button mSendButton;
    private TextView mMessageTextView;
    private EditText mMessageEditText;

    private Socket mClientSocket;
    private PrintWriter mPrintWriter;
    @SuppressLint("handlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_RECEIVE_NEW_MSG:{
                    mMessageTextView.setText(mMessageTextView.getText()+(String)msg.obj);
                    break;
                }
                case MESSAGE_SOCKET_CONNECTED:{
                    mSendButton.setEnabled(true);
                    break;
                }
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSendButton = (Button) findViewById(R.id.send);
        mMessageTextView = (TextView) findViewById(R.id.msg_container);
        mMessageEditText = (EditText)findViewById(R.id.msg);
        mSendButton.setOnClickListener(this);
        Intent service = new Intent(this,TCPServerService.class);
        startService(service);
        new Thread(new TcpClient()).start();
    }

    @Override
    public void onClick(View view) {
        if(view ==mSendButton){
            final String msg = mMessageEditText.getText().toString();
            if(!TextUtils.isEmpty(msg) &&mPrintWriter!=null){
                mPrintWriter.println(msg);
                mMessageEditText.setText("");
                String time = formatDataTime(System.currentTimeMillis());
                final String showedMsg = "self "+time+":"+msg+"\n";
                mMessageTextView.setText(mMessageTextView.getText()+showedMsg);
            }
        }
    }

    private class TcpClient implements Runnable{
        @Override
        public void run() {
            Socket socket=null;
            while (socket==null){
                try{
                    socket = new Socket("localhost",8688);
                    mClientSocket = socket;
                    mPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
                    mHandler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
                    Log.i(TAG,"connect server success");


                }catch (IOException e){
                    SystemClock.sleep(1000);
                    Log.e(TAG,"connect tcp server failed,retry...");
                }
            }
            try{
                //接收服务器端的消息
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while(!MainActivity.this.isFinishing()){
                    String msg = br.readLine();
                    Log.i(TAG,"receive:"+msg);
                    if(msg !=null){
                        String time = formatDataTime(System.currentTimeMillis());
                        final String showedMsg = "server"+time +":"+msg+"\n";
                        mHandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG,showedMsg).sendToTarget();
                    }
                }
                Log.i(TAG,"quit...");

                mPrintWriter.close();
                br.close();
                socket.close();
            }catch (IOException e){
                e.printStackTrace();



            }
        }
    }
    private String formatDataTime(long time){
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }

    @Override
    protected void onDestroy() {
        if(mClientSocket !=null){
            try{
                mClientSocket.shutdownInput();
                mClientSocket.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}
