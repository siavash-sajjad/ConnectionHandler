package ir.sadeghlabs.connectionhandler;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by Siavash on 9/18/2015.
 */
public class ConnectionHandler extends AsyncTask<Void, Void, String> {
    private Context context;
    private String url;
    private Constant.method method;
    private ProgressDialog progressDialog;
    private String message;
    private boolean cancelable = false;
    private CommunicatorListener listener;
    private int status = 0;
    private List<NameValuePair> nameValuePairList;



    public ConnectionHandler(Context context){
        this.context = context;
    }

    public ConnectionHandler(Context context, String url, Constant.method method, String message, boolean cancelable, CommunicatorListener listener) {
        this.context = context;
        this.url = url;
        this.method = method;
        this.message = message;
        this.cancelable = cancelable;
        this.listener = listener;
    }

    public ConnectionHandler(Context context, String url, Constant.method method, String message, boolean cancelable, List<NameValuePair> nameValuePairList, CommunicatorListener listener) {
        this.context = context;
        this.url = url;
        this.method = method;
        this.message = message;
        this.cancelable = cancelable;
        this.listener = listener;
        this.nameValuePairList = nameValuePairList;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(cancelable);
        progressDialog.show();

        if (this.listener != null) {
            this.listener.onPreExecute();
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        String result = "";
        try {
            if (isAccessToInternet()) {
                switch (method) {
                    case POST:
                        result = postData();
                        break;
                    case GET:
                        result = getData();
                        break;
                }

                return result;
            } else {
                status = -1;
                return "";
            }
        } catch (Exception ex) {
            return "";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        if (this.listener != null) {
            switch (status) {
                case -1: //no internet connection
                    this.listener.onNoInternetConnection();
                    break;
                case 1: //every think is ok
                    this.listener.onDone(result);
                    break;
                case -10: //exception in code
                    this.listener.onCodeError();
                    break;
            }
        }

        super.onPostExecute(result);
    }

    public boolean isAccessToInternet() {
        boolean connected = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo[] netInfo = cm.getAllNetworkInfo();

            for (NetworkInfo ni : netInfo) {
                if ((ni.getTypeName().equalsIgnoreCase("WIFI")
                        || ni.getTypeName().equalsIgnoreCase("MOBILE"))
                        & ni.isConnected() & ni.isAvailable()) {
                    connected = true;
                }

            }
        }

        return connected;
    }

    private String postData() {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(url);

            post.setEntity(new UrlEncodedFormEntity(nameValuePairList));
            HttpResponse response = client.execute(post);
            HttpEntity httpEntity = response.getEntity();
            final String response_str = EntityUtils.toString(httpEntity);

            status = 1;

            return response_str;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            status = -10;
            return "";
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            status = -10;
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            status = -10;
            return "";
        }
    }

    private String getData() {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = client.execute(get);
            int code = response.getStatusLine().getStatusCode();
            if (code == 200) {
                String result = StreamToString(response.getEntity().getContent());

                status = 1;

                return result;
            } else {
                status = -10;
                return "";
            }
        } catch (IOException e) {
            status = -10;
            e.printStackTrace();
            return "";
        }
    }

    public String StreamToString(InputStream data) {
        StringBuilder builder = new StringBuilder();
        BufferedReader bfr = new BufferedReader(new InputStreamReader(data));
        String line;
        try {
            while ((line = bfr.readLine()) != null) {
                builder.append(line);

            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return builder.toString();
    }

    public interface CommunicatorListener {
        void onNoInternetConnection();

        void onPreExecute();

        void onDone(String var1);

        void onCancelled();

        void onCodeError();

        void onServerError(Exception var1);
    }
}
