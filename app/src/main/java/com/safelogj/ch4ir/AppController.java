package com.safelogj.ch4ir;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import androidx.media3.common.util.UnstableApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@UnstableApi
public class AppController extends Application {

    public static final int PLAYERS_COUNT = 4;
    public static final String LOG_TAG = "tagvlc";
    public static final String SKIP_START = "skipStart";
    public static final String HIDE_SCALE = "hideScale";
    private static final String PRIVACY_ID = "PrivacyId";
    private static final String LINKS = "links";
    private static final String LINKS_JSON = "links.txt";
    private static final String STREAM_LINKS = "streamLinks";
    private static final String CONTENT_TITLE = "title";
    private static final String CONTENT_USER_LINK = "userLink";
    private static final String CONTENT_REAL_LINK = "realLink";
    private static final String CONTENT_INFO = "info";
    private static final String CONTENT_CHANNEL = "channel";
    private static final String CONTENT_LINK_TYPE = "linkType";
    private static final String CONTENT_USER_AGENT = "userAgent";
    private static final String EMPTY_STRING = "";
    private static final String DEFAULT_PRIVACY = "https://github.com/safelogJ/ch4ir/tree/main/privacy";
    private static final String DEFAULT_INFO = "https://github.com/safelogJ/ch4ir/tree/main";
    private static final String LINE_BREAK = "\n";
    private final ExoRecorder[] exoPlayers = new ExoRecorder[PLAYERS_COUNT];
    private final Map<String, Content> mStreamLinks = new HashMap<>();
    private final ExecutorService mAppExecutorWriteSettings = Executors.newSingleThreadExecutor();
    private final Content emptyContent = new Content();
    private final boolean[] webViewErrors = {true, true, true, true};
    private boolean isSkipStart;
    private boolean isHideScale;
    private int privacyId;
    private boolean hasRedirectedOnce;

    public void writeSettingsToFile() {
        mAppExecutorWriteSettings.execute(this::writeStreamLinksAndSettings);
    }

    public ExoRecorder[] getExoPlayers() {
        return exoPlayers;
    }

    public void addContent(String key, Content content) {
        mStreamLinks.put(key, content);
        writeSettingsToFile();
    }
    public void addContents(Map<String, Content> fileLinks) {
        mStreamLinks.putAll(fileLinks);
        writeSettingsToFile();
    }

    public Map<String, Content> getStreamLinks() {
        //  if (mStreamLinks.isEmpty()) {
       //    mStreamLinks.put("Dota247", buildContentFromLink("Dota247", "https://www.twitch.tv/dotatv247"));
//
////
//          mStreamLinks.put("reefandchill", buildContentFromLink("reefandchill", "https://www.twitch.tv/reefandchill"));
//        mStreamLinks.put("NTV SERIAL", buildContentFromLink("NTV SERIAL", "https://cdn.ntv.ru/th_serial/playlist.m3u8"));
//        mStreamLinks.put("ул. Рабочая д. 5/14",  buildContentFromLink("ул. Рабочая д. 5/14", "rtsp://demo:demo@89.179.77.65:5552/RVi/1/2"));
//        mStreamLinks.put("Детское кино", buildContentFromLink("Детское кино", "https://autopilot.catcast.tv/content/38720/index.m3u8"));
//        mStreamLinks.put("paramount", buildContentFromLink("paramount", "https://v-tv.catcast.tv/content/43242/index.m3u8"));



        //     mStreamLinks.put("TVFormula", buildContentFromLink("TVFormula", "https://tv.cdn.xsg.ge/c4635/TVFormula/playlist.m3u8"));
      //       mStreamLinks.put("Музыка кино", buildContentFromLink("Музыка кино", "https://kino-1.catcast.tv/content/37739/index.m3u8"));

//        mStreamLinks.put("ул. Арматурная д. 10/16, камера №1", buildContentFromLink("ул. Арматурная д. 10/16, камера №1", "rtsp://demo:demo@89.179.77.65:5550/RVi/1/2"));
//           mStreamLinks.put("ул. Арматурная д. 10/16 камера №2", buildContentFromLink("ул. Арматурная д. 10/16 камера №2", "rtsp://demo:demo@89.179.77.65:5551/RVi/1/2"));
//           mStreamLinks.put("ул. Арматурная д. 34В", buildContentFromLink("ул. Арматурная д. 34В", "rtsp://demo:demodemo11@89.179.77.65:5554/cam/realmonitor?channel=1&;subtype=1"));

       //    }
        return mStreamLinks;
    }

    public int getPrivacyId() {
        return privacyId;
    }

    public void setPrivacyId(int privacyId) {
        this.privacyId = privacyId;
    }

    public boolean isHideScale() {
        return isHideScale;
    }

    public void setHideScale(boolean hideScale) {
        isHideScale = hideScale;
    }

    public boolean isSkipStart() {
        return isSkipStart;
    }

    public void setSkipStart(boolean skipStart) {
        isSkipStart = skipStart;
    }

    public boolean isHasRedirectedOnce() {
        return hasRedirectedOnce;
    }

    public boolean isWebViewError() {
        for (boolean error: webViewErrors) {
            if (error) {
                return true;
            }
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
    }

    public void setWebViewError(boolean webViewError, int index) {
        webViewErrors[index] = webViewError;
    }

    public void setHasRedirectedOnce(boolean hasRedirectedOnce) {
        this.hasRedirectedOnce = hasRedirectedOnce;
    }

    public Content getEmptyContent() {
        return emptyContent;
    }

    public String getPrivacyText() {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        try (InputStream inputStream = getResources().openRawResource(R.raw.privacy_text);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append(LINE_BREAK);
            }
        } catch (IOException e) {
            return DEFAULT_PRIVACY;
        }
        return stringBuilder.toString().trim();
    }

    public String getInfoText() {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        try (InputStream inputStream = getResources().openRawResource(R.raw.info_text);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append(LINE_BREAK);
            }
        } catch (IOException e) {
            return DEFAULT_INFO;
        }
        return stringBuilder.toString().trim();
    }



    @Override
    public void onCreate() {
        super.onCreate();
        initPlayers();
        readStreamLinksAndSettings();
    }

    private void initPlayers() {
        for (int i = 0; i < PLAYERS_COUNT; i++) {
            ExoRecorder player = new ExoRecorder(this, i);
            exoPlayers[i] = player;
        }
    }

    private void writeStreamLinksAndSettings() {
        File streamLinksDir = new File(getFilesDir(), LINKS);
        if (!streamLinksDir.exists() && !streamLinksDir.mkdirs()) {
            return;
        }

        File streamLinksFile = new File(streamLinksDir, LINKS_JSON);

        try (FileWriter file = new FileWriter(streamLinksFile)) {
            JSONObject jsonObject = new JSONObject();
            JSONObject linksJson = new JSONObject();

            for (Map.Entry<String, Content> entry : mStreamLinks.entrySet()) {
                JSONObject contentJson = new JSONObject();
                Content content = entry.getValue();
                String title = content.getTitle();
                contentJson.put(CONTENT_TITLE, title != null ? title : JSONObject.NULL);
                String userLink = content.getUserLink();
                contentJson.put(CONTENT_USER_LINK, userLink != null ? userLink : JSONObject.NULL);
                String realLink = content.getRealLink();
                contentJson.put(CONTENT_REAL_LINK, realLink != null ? realLink : JSONObject.NULL);
                String info = content.getInfo();
                contentJson.put(CONTENT_INFO, info != null ? info : JSONObject.NULL);
                String channel = content.getChannel();
                contentJson.put(CONTENT_CHANNEL, channel != null ? channel : JSONObject.NULL);
                String userAgent = content.getUserAgent();
                contentJson.put(CONTENT_USER_AGENT, userAgent != null ? userAgent : JSONObject.NULL);
                contentJson.put(CONTENT_LINK_TYPE, content.getLinkType());
                linksJson.put(entry.getKey(), contentJson);
            }

            jsonObject.put(STREAM_LINKS, linksJson);
            jsonObject.put(SKIP_START, isSkipStart);
            jsonObject.put(HIDE_SCALE, isHideScale);
            jsonObject.put(PRIVACY_ID, privacyId);

            file.write(jsonObject.toString(4));

        } catch (Exception e) {
            Log.d(LOG_TAG, "error write json file: " + e.getMessage());
        }
    }

    private void readStreamLinksAndSettings() {
        File streamLinksDir = new File(getFilesDir(), LINKS);
        File streamLinksFile = new File(streamLinksDir, LINKS_JSON);
        StringBuilder jsonString = new StringBuilder();

        try (FileReader reader = new FileReader(streamLinksFile)) {
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                jsonString.append(buffer, 0, length);
            }
        } catch (IOException e) {
            Log.d(LOG_TAG, "error read settings file: " + e.getMessage());
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonString.toString());
            JSONObject linksJson = jsonObject.getJSONObject(STREAM_LINKS);

            for (Iterator<String> it = linksJson.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONObject contentJson = linksJson.getJSONObject(key);

                String title = contentJson.optString(CONTENT_TITLE, EMPTY_STRING);
                String userLink = contentJson.optString(CONTENT_USER_LINK, EMPTY_STRING);
                String realLink = contentJson.optString(CONTENT_REAL_LINK, EMPTY_STRING);
                String info = contentJson.optString(CONTENT_INFO, EMPTY_STRING);
                String channel = contentJson.optString(CONTENT_CHANNEL, EMPTY_STRING);
                String userAgent = contentJson.optString(CONTENT_USER_AGENT, EMPTY_STRING);
                int linkType = contentJson.optInt(CONTENT_LINK_TYPE, 0);

                Content content = new Content();
                content.setTitle(title);
                content.setUserLink(userLink);
                content.setRealLink(realLink);
                content.setInfo(info);
                content.setChannel(channel);
                content.setUserAgent(userAgent);
                content.setLinkType(linkType);
                mStreamLinks.put(key, content);
            }
            isSkipStart = jsonObject.optBoolean(SKIP_START, false);
            isHideScale = jsonObject.optBoolean(HIDE_SCALE, false);
            privacyId = jsonObject.optInt(PRIVACY_ID, 0);


        } catch (JSONException e) {
            Log.d(LOG_TAG, "error read json data: " + e.getMessage());
        }
    }

}
