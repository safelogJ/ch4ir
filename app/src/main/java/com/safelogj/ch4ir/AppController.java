package com.safelogj.ch4ir;

import android.app.Application;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
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
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;


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
    private static final String KEY_ALIAS = "MikrotikRouterKeyAlias";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 16; // Длина аутентификационного тега в байтах (128 бит)
    private static final int AES_KEY_SIZE = 256;
    private static final String ENCRYPTED_DATA_KEY = "encryptedData";
    private final ExoRecorder[] exoPlayers = new ExoRecorder[PLAYERS_COUNT];
    private final Map<String, Content> mStreamLinks = new HashMap<>();
    private final ExecutorService mAppExecutorWriteSettings = Executors.newSingleThreadExecutor();
    private final Content emptyContent = new Content();
    private final boolean[] webViewErrors = {true, true, true, true};
    private boolean isSkipStart;
    private boolean isHideScale;
    private int privacyId;
    private boolean hasRedirectedOnce;
    private Cipher mCipher;

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
            JSONObject rootJson = new JSONObject();
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

            rootJson.put(STREAM_LINKS, linksJson);
            rootJson.put(SKIP_START, isSkipStart);
            rootJson.put(HIDE_SCALE, isHideScale);
            rootJson.put(PRIVACY_ID, privacyId);


            // 2. Шифрование всего JSON-контента
            String rawJsonString = rootJson.toString();
            byte[] rawJsonBytes = rawJsonString.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedCombinedBytes = encrypt(rawJsonBytes);
            String encryptedBase64 = Base64.encodeToString(encryptedCombinedBytes, Base64.NO_WRAP);

            // 3. Создание JSON-оболочки для записи в файл
            JSONObject fileWrapper = new JSONObject();
            fileWrapper.put(ENCRYPTED_DATA_KEY, encryptedBase64);

            file.write(fileWrapper.toString(4));

        } catch (Exception e) {
            Log.d(LOG_TAG, "error write json file: " + e.getMessage());
        }
    }

    private void readStreamLinksAndSettings() {
        File streamLinksDir = new File(getFilesDir(), LINKS);
        File streamLinksFile = new File(streamLinksDir, LINKS_JSON);
        StringBuilder fileContent = new StringBuilder();

        try (FileReader reader = new FileReader(streamLinksFile)) {
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                fileContent.append(buffer, 0, length);
            }
        } catch (IOException e) {
            Log.d(LOG_TAG, "error read settings file: " + e.getMessage());
            return;
        }

        try {
            JSONObject fileWrapper = new JSONObject(fileContent.toString());
            String encryptedBase64 = fileWrapper.getString(ENCRYPTED_DATA_KEY);

            // Декодирование и дешифрование
            byte[] combinedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT); // Base64.DEFAULT безопасно для декодирования
            byte[] decryptedBytes = decrypt(combinedBytes);
            String rawJsonString = new String(decryptedBytes, StandardCharsets.UTF_8);

            JSONObject rootJson = new JSONObject(rawJsonString);
            JSONObject linksJson = rootJson.getJSONObject(STREAM_LINKS);

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
            isSkipStart = rootJson.optBoolean(SKIP_START, false);
            isHideScale = rootJson.optBoolean(HIDE_SCALE, false);
            privacyId = rootJson.optInt(PRIVACY_ID, 0);


        } catch (Exception e) {
            readStreamLinksAndSettingsOld();
            writeStreamLinksAndSettings();
            Log.d(LOG_TAG, "error read json data: " + e.getMessage());
        }
    }

    private void readStreamLinksAndSettingsOld() {
        File streamLinksDir = new File(getFilesDir(), LINKS);
        File streamLinksFile = new File(streamLinksDir, LINKS_JSON);
        StringBuilder fileContent = new StringBuilder();

        try (FileReader reader = new FileReader(streamLinksFile)) {
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                fileContent.append(buffer, 0, length);
            }
        } catch (IOException e) {
            Log.d(LOG_TAG, "error read settings file: " + e.getMessage());
            return;
        }

        try {
            JSONObject rootJson = new JSONObject(fileContent.toString());
            JSONObject linksJson = rootJson.getJSONObject(STREAM_LINKS);

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
            isSkipStart = rootJson.optBoolean(SKIP_START, false);
            isHideScale = rootJson.optBoolean(HIDE_SCALE, false);
            privacyId = rootJson.optInt(PRIVACY_ID, 0);


        } catch (Exception e) {
            Log.d(LOG_TAG, "error read json data: " + e.getMessage());
        }
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        // Попытка получить существующий ключ
        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            return entry.getSecretKey();
        }

        // Если ключа нет, создаем новый (Требуется API 23+ для KeyGenParameterSpec)
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

        // Настройка параметров: AES/GCM/NoPadding
        keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_SIZE)
                .build());

        return keyGenerator.generateKey();

    }

    private byte[] encrypt(byte[] dataBytes) throws Exception {
        SecretKey secretKey = getOrCreateSecretKey();
        if (mCipher == null) {
            mCipher = Cipher.getInstance(TRANSFORMATION);
        }
        mCipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] iv = mCipher.getIV();
        byte[] encryptedData = mCipher.doFinal(dataBytes);
        byte[] combined = new byte[1 + iv.length + encryptedData.length];
        combined[0] = (byte) iv.length; // Сохраняем длину IV в первом байте
        System.arraycopy(iv, 0, combined, 1, iv.length); // Копируем IV начиная со второго байта
        System.arraycopy(encryptedData, 0, combined, 1 + iv.length, encryptedData.length); // Копируем данные
        return combined;
    }

    private byte[] decrypt(byte[] combinedBytes) throws Exception {
        // Минимальная длина: 1 байт (длина IV) + 1 байт (IV) + 16 байт (GCM Tag) = 18 байт
        if (combinedBytes.length < 1 + GCM_TAG_LENGTH) {
            throw new InvalidKeyException("Combined data too short to contain IV length and GCM Tag.");
        }

        int ivLength = combinedBytes[0] & 0xFF; // Получаем фактическую длину IV из первого байта
        // Проверяем, достаточно ли данных для IV и GCM Tag
        if (combinedBytes.length < 1 + ivLength + GCM_TAG_LENGTH) {
            throw new InvalidKeyException("IV length leads to combined data too short for GCM Tag.");
        }
        // Извлекаем IV
        byte[] iv = Arrays.copyOfRange(combinedBytes, 1, 1 + ivLength);
        // Извлекаем зашифрованные данные (начинаются после байта длины и IV)
        byte[] encryptedData = Arrays.copyOfRange(combinedBytes, 1 + ivLength, combinedBytes.length);

        SecretKey secretKey = getOrCreateSecretKey();
        mCipher = Cipher.getInstance(TRANSFORMATION);
        // GCM_TAG_LENGTH * 8, так как длина тега указывается в битах (16 байт * 8 = 128 бит)
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

        mCipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        return mCipher.doFinal(encryptedData);
    }

}
