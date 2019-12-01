package com.kaltura.playkit.plugins.fbads.fbinstream.admetadata;

public class Capabilities {

    private SkipButton skipButton;

    public Capabilities() {}

    public Capabilities(SkipButton skipButton) {
        this.skipButton = skipButton;
    }

    public SkipButton getSkipButton() {
        return skipButton;
    }
}
