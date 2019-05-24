package ro.pub.cs.systems.eim.practicaltest02;

import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.protocol.HTTP;

public class PracticalTest02MainActivity extends AppCompatActivity {

    private class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch(view.getId()) {
                case R.id.connect_button:
                    serverThread.startServer();
                    break;
                case R.id.request_button:
                    String word = ((EditText)findViewById(R.id.word_text)).getText().toString();
                    String letters = ((EditText)findViewById(R.id.num_letters)).getText().toString();
                    ClientThread client = new ClientThread(word, letters);
                    client.start();

                    break;
            }
        }
    }

    private class ServerThread extends Thread {

        private boolean isRunning;

        private ServerSocket serverSocket;

        public void startServer() {
            isRunning = true;
            start();
        }

        public void stopServer() {
            isRunning = false;
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(Integer.parseInt(((EditText)findViewById(R.id.server_port_edit_text)).getText().toString()));
                Log.d("mytag", "server up");
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

                    Log.d("mytag", "get msg");
                    // get data from client
                    String msg = bufferedReader.readLine();
                    String word = msg.split(",")[0];
                    String letters = msg.split(",")[1];

                    Log.d("mytag", word);Log.d("mytag", letters);


                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost("http://services.aonaware.com/CountCheatService/CountCheatService.asmx/LetterSolutionsMin");
                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("anagram", word));
                    params.add(new BasicNameValuePair("minLetters", letters));
                    UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
                    httpPost.setEntity(urlEncodedFormEntity);
                    ResponseHandler<String> responseHandler = new BasicResponseHandler();
                    String pageSourceCode = httpClient.execute(httpPost, responseHandler);
                    Log.d("mytag", "done req");

                    String result = "";
                    Document document = Jsoup.parse(pageSourceCode);
                    Element element = document.child(0);
                    Elements elements = element.getElementsByTag("string");
                    for (Element e : elements)
                        //Log.d("mytag", e.toString().split(" ")[1].split("\n")[0]);
                        result += e.toString().split(" ")[1].split("\n")[0] + " ";
                    // send result

                    printWriter.println(result.substring(0, result.length() - 1));
                    printWriter.flush();

                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientThread extends Thread {

        String word;
        String letters;

        public ClientThread (String word, String letters) {
            this.word = word;
            this.letters = letters;
        }

        public void run() {
            try {
                Socket socket = new Socket("192.168.57.101", 5000);
                if (socket == null) {
                    Log.d("mytag", "er1");
                    return;
                }

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
                if (bufferedReader == null || printWriter == null) {
                    Log.d("mytag", "er2");
                    return;
                }
                Log.d("mytag", "send msg");
                printWriter.println(word + "," + letters);
                printWriter.flush();
                //printWriter.println(letters);
                //printWriter.flush();
                String words = bufferedReader.readLine();
//                String result;
//                while ((result = bufferedReader.readLine()) != null) {
//                    words = words + result;
//                }

                final String WORDS = words;
                ((TextView)findViewById(R.id.result_text)).post(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.result_text)).setText(WORDS);
                    }
                });

            } catch (IOException ioException) {
            } finally {
            }
        }
    }

    private ButtonClickListener buttonClickListener = new ButtonClickListener();
    private ServerThread serverThread = new ServerThread();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practical_test02_main);

        ((Button)findViewById(R.id.connect_button)).setOnClickListener(buttonClickListener);
        ((Button)findViewById(R.id.request_button)).setOnClickListener(buttonClickListener);

    }
}
