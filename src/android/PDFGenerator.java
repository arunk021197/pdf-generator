package com.pdf.generator;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.print.PrintManager;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import com.itextpdf.text.pdf.codec.Base64;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.*;
import org.apache.cordova.PluginResult;

import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * This class echoes a string called from JavaScript.
 */
public class PDFGenerator extends CordovaPlugin {

    private final static String APPNAME = "PDFGenerator";
    private WebView offscreenWebview = null;
    public CallbackContext callbackContext = null;

    public WebView getOffscreenWebkitInstance(Context ctx) {
        LOG.i(APPNAME, "Mounting offscreen webview");
        if (this.offscreenWebview == null){
            WebView view = new WebView(ctx);
            view.getSettings().setDatabaseEnabled(true);
            view.getSettings().setJavaScriptEnabled(true);

            return this.offscreenWebview = view;
        }else{
            return this.offscreenWebview;
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        if(action.equals("GetPermissionForNotification")) {
            String[] permissions = {"android.permission.POST_NOTIFICATIONS"};
            Boolean isNotificationEnabled = NotificationManagerCompat.from(cordova.getActivity()).areNotificationsEnabled();
            if(!isNotificationEnabled) {
                PermissionHelper.requestPermissions(this, 1, permissions);
            }
        }
        if (action.equals("htmlToPDF")) {
            if(args.getString(1).equals("Base64ToPdf")) {
                try {
                    callbackContext = callbackContext;
                    createPdfFile(args.getString(2), callbackContext);
                    /*Context context= cordova.getActivity().getApplicationContext();
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        String[] permissions = new String[] {"android.permission.WRITE_EXTERNAL_STORAGE"};
                        if((ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED) {
                            PermissionHelper.requestPermissions(this, 1, permissions);
                            createPdfFile(args.getString(2), callbackContext);
                        } else {
                            createPdfFile(args.getString(2), callbackContext);
                        }
                    } else {
                        createPdfFile(args.getString(2), callbackContext);
                    }*/

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.pdfPrinter(args, callbackContext);
            return true;
        }
        return false;
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
    }
    public void createPdfFile (String base64String, CallbackContext callbackContext) throws IOException {
        //final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/MyPDFFileee" + ".pdf");
        final File fileRootPath = new File(cordova.getActivity().getExternalFilesDir(""), "");
        final File dwldsPath = new File(fileRootPath + "/My Favourite Courses" + ".pdf");
        if (dwldsPath.exists()) {
            dwldsPath.delete();
        }
        byte[] pdfAsBytes = Base64.decode(base64String);
        FileOutputStream os;
        os = new FileOutputStream(dwldsPath, false);
        os.write(pdfAsBytes);
        os.flush();
        os.close();
        shareFile(dwldsPath);
    }
    private static String getMimeType(String path) {
        String mimeType = null;
        String extension = getExtension(path);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            mimeType = mime.getMimeTypeFromExtension(extension.toLowerCase());
        }
        return mimeType;
    }
    public static String getExtension(String fileName) {
        String encoded;
        try {
            encoded = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encoded = fileName;
        }
        return MimeTypeMap.getFileExtensionFromUrl(encoded).toLowerCase();
    }
    private void shareFile(File file) {
        try{
            Context context = this.cordova.getActivity().getApplicationContext();
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName()+".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(contentUri, getMimeType(file.getAbsolutePath()));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            PluginResult pluginResult;
            cordova.startActivityForResult(this, intent, 11);
            pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        } catch (ActivityNotFoundException e) {

        }
    }

    @ Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 11) {
            System.out.println("Successs");
        }
    }

    private void pdfPrinter(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        final Context ctx = this.cordova.getActivity().getApplicationContext();
        final CordovaInterface _cordova = this.cordova;
        final CallbackContext cordovaCallback = callbackContext;

        _cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    WebView webview = getOffscreenWebkitInstance(ctx);

                    PrintManager printManager = (PrintManager) cordova.getActivity()
                            .getSystemService(Context.PRINT_SERVICE);

                    boolean outputBase64 = args.getString(4) != null && args.getString(4).equals("base64");
                    PDFPrinterWebView printerWebView = new PDFPrinterWebView(printManager, ctx, outputBase64);

                    String fileNameArg = args.getString(5);
                    if (fileNameArg != null) {
                        printerWebView.setFileName(fileNameArg);
                    }

                    String pageType = args.getString(2);
                    printerWebView.setPageType(pageType);

                    String orientation = args.getString(3);
                    if (orientation != null) {
                        printerWebView.setOrientation(orientation);
                    }

                    printerWebView.setCordovaCallback(cordovaCallback);
                    webview.setWebViewClient(printerWebView);

                    if (args.getString(0) != null && !args.getString(0).equals("null"))
                        webview.loadUrl(args.getString(0));

                    if (args.getString(1) != null && !args.getString(1).equals("null"))
                        webview.loadDataWithBaseURL(null,args.getString(1), "text/HTML","UTF-8", null);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(APPNAME, e.getMessage());
                    cordovaCallback.error("Native pasing arguments: " + e.getMessage());
                }
            }
        });
    }

}
