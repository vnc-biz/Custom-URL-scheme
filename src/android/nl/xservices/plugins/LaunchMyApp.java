package nl.xservices.plugins;

import android.content.Intent;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Locale;
import android.os.Bundle;

public class LaunchMyApp extends CordovaPlugin {

  private static final String ACTION_CHECKINTENT = "checkIntent";
  private static final String ACTION_CLEARINTENT = "clearIntent";
  private static final String ACTION_GETLASTINTENT = "getLastIntent";

  private String lastIntentString = null;

  /**
   * We don't want to interfere with other plugins requiring the intent data,
   * but in case of a multi-page app your app may receive the same intent data
   * multiple times, that's why you'll get an option to reset it (null it).
   *
   * Add this to config.xml to enable that behaviour (default false):
   *   <preference name="CustomURLSchemePluginClearsAndroidIntent" value="true"/>
   */
  private boolean resetIntent;

  @Override
  public void initialize(final CordovaInterface cordova, CordovaWebView webView){
    this.resetIntent = preferences.getBoolean("resetIntent", false) ||
        preferences.getBoolean("CustomURLSchemePluginClearsAndroidIntent", false);
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (ACTION_CLEARINTENT.equalsIgnoreCase(action)) {
      final Intent intent = this.cordova.getActivity().getIntent();
      Log.d("cordova-plugin-customurlscheme", "App was started with action ACTION_CLEARINTENT - intent:  " + intent.toString());
      if (resetIntent){
        Log.d("cordova-plugin-customurlscheme", "App was started with action ACTION_CLEARINTENT - intent:  " + intent.toString() + " RESET");
        intent.setData(null);
      }
      return true;
    } else if (ACTION_CHECKINTENT.equalsIgnoreCase(action)) {
      String intentDeeplinkString = "";
      final Intent intent = this.cordova.getActivity().getIntent();
      Log.d("cordova-plugin-customurlscheme", "App was started with action ACTION_CHECKINTENT - intent:  " + intent.toString());

      try {
        Bundle data = intent.getExtras();
        if (data != null) {
          Log.d("cordova-plugin-customurlscheme", "App was started with action ACTION_CHECKINTENT - bundle:  " + data.toString());
          if (data.containsKey("deeplink")) {
            intentDeeplinkString = data.getString("deeplink");
            Log.d("cordova-plugin-customurlscheme", "App was started with action ACTION_CHECKINTENT - bundle deeplinkString:  " + intentDeeplinkString);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, intentDeeplinkString));
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        Log.d("cordova-plugin-customurlscheme", "error parsing bundle: ", e);
      }

      final String intentString = intent.getDataString();
      if (intentString != null) {
        Log.d("cordova-plugin-customurlscheme", "App was started with action ACTION_CHECKINTENT - intentString:  " + intentString);
      }
      if (intentString != null && intent.getScheme() != null) {
        lastIntentString = intentString;
        Log.d("cordova-plugin-customurlscheme", "App was started with:  " + intentString);
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, intent.getDataString()));
      } else {
        Log.d("cordova-plugin-customurlscheme", "App was started with intent:  " + intent.toString());
        if (intentString != null) {
          Log.d("cordova-plugin-customurlscheme", "App was not started via expected scheme - string:  " + intentString);
        }
        if (intent.getScheme() != null) {
          Log.d("cordova-plugin-customurlscheme", "App was not started via expected scheme - scheme:  " + intent.getScheme());
        }
        callbackContext.error("App was not started via the launchmyapp URL scheme. Ignoring this errorcallback is the best approach.");
      }
      return true;
    } else if (ACTION_GETLASTINTENT.equalsIgnoreCase(action)) {
      if(lastIntentString != null) {
        Log.d("cordova-plugin-customurlscheme", "App was started with action ACTION_GETLASTINTENT - lastIntentString:  " + lastIntentString);
        Log.d("cordova-plugin-customurlscheme", "App was started with last intent: " + lastIntentString);
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, lastIntentString));
      } else {
        Log.d("cordova-plugin-customurlscheme", "App was started with action ACTION_GETLASTINTENT - lastIntentString is null!!");
        Log.d("cordova-plugin-customurlscheme", "No intent received so far. ");
        callbackContext.error("No intent received so far.");
      }
      return true;
    } else {
      callbackContext.error("This plugin only responds to the " + ACTION_CHECKINTENT + " action.");
      if (action != null) {
        Log.d("cordova-plugin-customurlscheme", "App was started with unsupported action :  " + action);
      } else {
        Log.d("cordova-plugin-customurlscheme", "App was started with null action");
      }
      return false;
    }
  }

  @Override
  public void onNewIntent(Intent intent) {
    Log.d("cordova-plugin-customurlscheme", "onNewIntent:  " + intent.toString());
    String intentString = intent.getDataString();
    String intentScheme = intent.getScheme();
    try {
      String intentAction = intent.getAction();
      Bundle data = intent.getExtras();
      Log.d("cordova-plugin-customurlscheme", "onNewIntent action:  " + intentAction);
      Log.d("cordova-plugin-customurlscheme", "onNewIntent data:  " + data.toString());
      String deeplink = data.getString("deeplink");
      Log.d("cordova-plugin-customurlscheme", "onNewIntent bundle deeplink:  " + deeplink);
      if (deeplink != null) {
        intentString = deeplink;
        intentScheme = "vncmail";
      }
    } catch (Exception e) {
      e.printStackTrace();
      Log.d("cordova-plugin-customurlscheme", "error parsing bundle: ", e);
    }
    if (intentString != null && intentScheme != null) {
      if (resetIntent){
        Log.d("cordova-plugin-customurlscheme", "onNewIntent:  " + intent.toString() + "  RESET" );
        intent.setData(null);
      }
      try {
        StringWriter writer = new StringWriter(intentString.length() * 2);
        escapeJavaStyleString(writer, intentString, true, false);
        Log.d("cordova-plugin-customurlscheme", "sending new Intent to webView " + URLEncoder.encode(writer.toString()));
        webView.loadUrl("javascript:handleOpenURL('" + URLEncoder.encode(writer.toString()) + "');");
      } catch (IOException ignore) {
        Log.d("cordova-plugin-customurlscheme", "error sending intent to webview: ", ignore);
      }
    }
  }

  // Taken from commons StringEscapeUtils
  private static void escapeJavaStyleString(Writer out, String str, boolean escapeSingleQuote,
                                            boolean escapeForwardSlash) throws IOException {
    if (out == null) {
      throw new IllegalArgumentException("The Writer must not be null");
    }
    if (str == null) {
      return;
    }
    int sz;
    sz = str.length();
    for (int i = 0; i < sz; i++) {
      char ch = str.charAt(i);

      // handle unicode
      if (ch > 0xfff) {
        out.write("\\u" + hex(ch));
      } else if (ch > 0xff) {
        out.write("\\u0" + hex(ch));
      } else if (ch > 0x7f) {
        out.write("\\u00" + hex(ch));
      } else if (ch < 32) {
        switch (ch) {
          case '\b':
            out.write('\\');
            out.write('b');
            break;
          case '\n':
            out.write('\\');
            out.write('n');
            break;
          case '\t':
            out.write('\\');
            out.write('t');
            break;
          case '\f':
            out.write('\\');
            out.write('f');
            break;
          case '\r':
            out.write('\\');
            out.write('r');
            break;
          default:
            if (ch > 0xf) {
              out.write("\\u00" + hex(ch));
            } else {
              out.write("\\u000" + hex(ch));
            }
            break;
        }
      } else {
        switch (ch) {
          case '\'':
            if (escapeSingleQuote) {
              out.write('\\');
            }
            out.write('\'');
            break;
          case '"':
            out.write('\\');
            out.write('"');
            break;
          case '\\':
            out.write('\\');
            out.write('\\');
            break;
          case '/':
            if (escapeForwardSlash) {
              out.write('\\');
            }
            out.write('/');
            break;
          default:
            out.write(ch);
            break;
        }
      }
    }
  }

  private static String hex(char ch) {
    return Integer.toHexString(ch).toUpperCase(Locale.ENGLISH);
  }
}
