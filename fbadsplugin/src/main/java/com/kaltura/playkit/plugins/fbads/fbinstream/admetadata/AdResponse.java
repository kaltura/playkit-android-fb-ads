package com.kaltura.playkit.plugins.fbads.fbinstream.admetadata;

public class AdResponse {

    private Trackers trackers;
    private Video video;
    private Capabilities capabilities;

    public AdResponse() {}

    public AdResponse(Trackers trackers, Video video, Capabilities capabilities) {
        this.trackers = trackers;
        this.video = video;
        this.capabilities = capabilities;
    }

    public Trackers getTrackers() {
        return trackers;
    }

    public Video getVideo() {
        return video;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }
}
