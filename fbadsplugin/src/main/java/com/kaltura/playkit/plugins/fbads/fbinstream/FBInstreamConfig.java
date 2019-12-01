package com.kaltura.playkit.plugins.fbads.fbinstream;

import java.util.ArrayList;
import java.util.List;

public class FBInstreamConfig {

    private List<FBInStreamAdBreak> fbInStreamAdBreaks;
    private boolean enableDebugMode;
    private String testDevice;
    private boolean alwaysStartWithPreroll;

    public FBInstreamConfig() {}

    public FBInstreamConfig(List<FBInStreamAdBreak> fbInStreamAdBreaks) {
        this.fbInStreamAdBreaks = fbInStreamAdBreaks;
    }

    public FBInstreamConfig enableDebugMode(boolean enableDebugMode) {
        this.enableDebugMode = enableDebugMode;
        return this;
    }

    // need to set test device only for testing / development
    public FBInstreamConfig setTestDevice(String testDevice) {
        this.testDevice = testDevice;
        return this;
    }

    public List<FBInStreamAdBreak> getFbInStreamAdBreaks() {
        return fbInStreamAdBreaks;
    }

    public boolean isEnableDebugMode() {
        return enableDebugMode;
    }

    public String getTestDevice() {
        return testDevice;
    }

    public FBInstreamConfig setAlwaysStartWithPreroll(boolean alwaysStartWithPreroll) {
        this.alwaysStartWithPreroll = alwaysStartWithPreroll;
        return this;
    }

    public boolean isAlwaysStartWithPreroll() {
        return alwaysStartWithPreroll;
    }

    public List<FBInStreamAdBreak> getAdBreakList() {
        return fbInStreamAdBreaks != null ? fbInStreamAdBreaks : new ArrayList<>();
    }

    public void setAdBreakList(List<FBInStreamAdBreak> fbInStreamAdBreaks) {
        this.fbInStreamAdBreaks = fbInStreamAdBreaks;
    }

    public FBInStreamAdBreak getAdBreakByTime(long adBreaktime) {
        for (FBInStreamAdBreak fbInStreamAdBreak : fbInStreamAdBreaks) {
            if (fbInStreamAdBreak.getAdBreakTime() == adBreaktime) {
                return fbInStreamAdBreak;
            }
        }
        return null;
    }
}
