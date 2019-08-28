package com.kaltura.playkit.plugins.fbads.fbinstream.admetadata;

public class Video {

    private String videoURL;
    private String videoHDURL;
    private Object videoHDBitrate;
    private String destinationURL;

    public Video() {}

    public Video(String videoURL, String videoHDURL, Object videoHDBitrate, String destinationURL) {
        this.videoURL = videoURL;
        this.videoHDURL = videoHDURL;
        this.videoHDBitrate = videoHDBitrate;
        this.destinationURL = destinationURL;
    }

    public String getVideoURL() {
        return videoURL;
    }

    public String getVideoHDURL() {
        return videoHDURL;
    }

    public Object getVideoHDBitrate() {
        return videoHDBitrate;
    }

    public String getDestinationURL() {
        return destinationURL;
    }
}
