package yonky.sockettest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Created by Administrator on 2018/4/19.
 */

public class TCPServerService extends Service {
    private static String TAG="TCPServer";
    private boolean mIsServiceDestroyed = false;
    private String[] mDefinedMessages=new String[]{
            "你好啊，哈哈",
            "请问你叫什么名字呀？",
            "今天天气真不错",
            "你知道吗？我可是可以和多个人同事聊天的哦"
    };

    @Override
    public void onCreate() {
        new Thread(new TcpServer()).start();
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mIsServiceDestroyed = true;
        super.onDestroy();
    }

    private class TcpServer implements Runnable{
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try{
                serverSocket = new ServerSocket(8688);
            }catch (IOException e){
                Log.e(TAG,"establish tcp server failed,port 8688");
                e.printStackTrace();
                return;
            }

            while (!mIsServiceDestroyed){
                try{
                    final Socket client = serverSocket.accept();
                    Log.i(TAG,"accept");
                    new Thread(){
                        @Override
                        public void run(){
                            try{
                                responseClient(client);
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                    };
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private  void responseClient(Socket client) throws IOException{
        //用于接收客户端信息
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        //用于向客户端发送消息
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())),true);
        out.println("欢迎来到聊天室");
        while (!mIsServiceDestroyed){
            String str= in.readLine();
            Log.i(TAG,str);
            if(str==null){
                break;
            }
            int i = new Random().nextInt(mDefinedMessages.length);
            String msg = mDefinedMessages[i];
            out.println(msg);
            Log.i(TAG,"send:"+msg);
        }
        Log.i(TAG,"client quit.");
        out.close();
        in.close();
        client.close();
    }
}
