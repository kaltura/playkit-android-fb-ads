package com.kaltura.playkit.plugins.fbads.fbinstream.admetadata;

public class Trackers {
    private String impression;
    private String nativeImpression;
    private String click;
    private String video;
    private String client;
    private String key;
    private String reportUrl;

    public Trackers() {}

    public Trackers(String impression, String nativeImpression, String click, String video, String client, String key, String reportUrl) {
        this.impression = impression;
        this.nativeImpression = nativeImpression;
        this.click = click;
        this.video = video;
        this.client = client;
        this.key = key;
        this.reportUrl = reportUrl;
    }

    public String getImpression() {
        return impression;
    }

    public String getNativeImpression() {
        return nativeImpression;
    }

    public String getClick() {
        return click;
    }

    public String getVideo() {
        return video;
    }

    public String getClient() {
        return client;
    }

    public String getKey() {
        return key;
    }

    public String getReportUrl() {
        return reportUrl;
    }
}
