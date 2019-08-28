package com.kaltura.playkit.plugins.fbads.fbinstream.admetadata;

public class SkipButton {

    private String skippableSeconds;

    public SkipButton() {}

    public SkipButton(String skippableSeconds) {
        this.skippableSeconds = skippableSeconds;
    }

    public String getSkippableSeconds() {
        return skippableSeconds;
    }
}
