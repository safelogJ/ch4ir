package com.safelogj.ch4ir;

import android.net.Uri;

import java.util.Locale;

public class Content {

    public static final int UNSUPPORTED_LINK_TYPE = 0;
    public static final int RTSP_LINK_TYPE = 1;
    //  public static final int RTMP_LINK_TYPE = 2;
    public static final int M3U8_LINK_TYPE = 3;
    public static final int MPD_LINK_TYPE = 4;
    public static final int TWITCH_LINK_DESKTOP_TYPE = 5;
    public static final int TWITCH_LINK_MOBILE_TYPE = 6;
    public static final String TWITCH_PATTERN_PLAYER_LINK = "https://safelogj.github.io/ch4ir/?channel=";
    private static final int TWITCH_LINK_MOBILE_SIZE = 20;
    private static final int TWITCH_LINK_DESKTOP_SIZE = 22;
    private static final String NEW_LINE = "\n";
    private static final String EMPTY_STRING = "";
    private static final String SLASH = "/";
    private static final String RTSP_PATTERN = "rtsp://";
    private static final String M3U8_PATTERN = ".m3u8";
    private static final String MPD_PATTERN = ".mpd";
    private static final String TWITCH_PATTERN_DESKTOP = "https://www.twitch.tv/";
    private static final String TWITCH_PATTERN_MOBILE = "https://m.twitch.tv/";

    private static final String ERROR_LINK = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <style>
        html, body {
          background-color: black;
          color: #BBDEFB;
          margin: 0;
          padding: 0;
          font-family: sans-serif;
          height: 100vh;
          display: flex;
          justify-content: center;
          align-items: center;
          overflow: auto;
        }
    
        .container {
          max-width: 90vw;
          padding: 16px;
          box-sizing: border-box;
          word-wrap: break-word;
          overflow-wrap: break-word;
          text-align: center;
        }
    
        p {
          font-size: 16px;
          margin: 0 0 12px 0;
        }
      </style>
    </head>
    <body>
      <div class="container">
        <p>%s</p>
        <p>%s</p>
      </div>
    </body>
    </html>
    """;
//    private static final String [] GIT_LINKS = {
//            "https://safelogj.github.io/ch4ir_channel1/?channel=",
//            "https://safelogj.github.io/ch4ir_channel2/?channel=",
//            "https://safelogj.github.io/ch4ir_channel3/?channel=",
//            "https://safelogj.github.io/ch4ir_channel4/?channel="
//    };

    private String title = EMPTY_STRING;
    private String userLink = EMPTY_STRING;
    private String realLink = EMPTY_STRING;
    private String info = EMPTY_STRING;
    private int linkType;
    private String channel = EMPTY_STRING;
    private String userAgent = EMPTY_STRING;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserLink() {
        return userLink;
    }

    public void setUserLink(String userLink) {
        this.userLink = userLink;
    }

    public String getRealLink() {
        return realLink;
    }

    public void setRealLink(String realLink) {
        this.realLink = realLink;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getLinkType() {
        return linkType;
    }

    public void setLinkType(int linkType) {
        this.linkType = linkType;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public static int checkLinkType(String userLink) {
          String link = userLink.toLowerCase(Locale.ROOT);
        if (link.startsWith(RTSP_PATTERN)) {
            return RTSP_LINK_TYPE;
        } else if (link.contains(M3U8_PATTERN)) {
            return M3U8_LINK_TYPE;
        } else if (link.contains(MPD_PATTERN)) {
            return MPD_LINK_TYPE;
        } else if (link.length() > TWITCH_LINK_DESKTOP_SIZE && link.startsWith(TWITCH_PATTERN_DESKTOP)) {
            return TWITCH_LINK_DESKTOP_TYPE;
        } else if (link.length() > TWITCH_LINK_MOBILE_SIZE && link.startsWith(TWITCH_PATTERN_MOBILE)) {
            return TWITCH_LINK_MOBILE_TYPE;
        } else {
            return UNSUPPORTED_LINK_TYPE;
        }
    }

    private static String[] buildTwitchLink(String link, int size) {
        try {
            Uri uri = Uri.parse(link);
            String[] segments = uri.getPath().split(SLASH);
            String channel = (segments.length > 1) ? segments[1] : segments[0];
            return new String[]{TWITCH_PATTERN_PLAYER_LINK + channel, channel};
        } catch (Exception e) {
            String channel = link.substring(size);
            return new String[]{TWITCH_PATTERN_PLAYER_LINK + channel, channel};
        }
    }

    public static String[] buildRealLink(String userLink, int linkType) {
        if (linkType == TWITCH_LINK_DESKTOP_TYPE) {
            return buildTwitchLink(userLink, TWITCH_LINK_DESKTOP_SIZE);
        } else if (linkType == TWITCH_LINK_MOBILE_TYPE) {
            return buildTwitchLink(userLink, TWITCH_LINK_MOBILE_SIZE);
        } else {
            return new String[]{userLink, EMPTY_STRING};
        }
    }

    public static String buildInfo(String title, String link) {
        return title + NEW_LINE + link + NEW_LINE;
    }

    public static String getErrorPage(String result, String userLink) {
        return String.format(ERROR_LINK, result, userLink);
    }

//    public static String getTwitchGitLink(int playerIdx) {
//       return GIT_LINKS[playerIdx];
//    }
}
