package com.kaltura.playkit.plugins.fbads.fbinstream;

import java.util.ArrayList;
import java.util.List;

public class FBInstreamConfig {

    private List<FBInStreamAdBreak> fbInStreamAdBreaks;
    private boolean enableDebugMode;
    private String testDevice;

    public FBInstreamConfig(List<FBInStreamAdBreak> fbInStreamAdBreaks) {
        this.fbInStreamAdBreaks = fbInStreamAdBreaks;
    }

    public FBInstreamConfig enableDebugMode(boolean enableDebugMode) {
        this.enableDebugMode = enableDebugMode;
        return this;
    }

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
