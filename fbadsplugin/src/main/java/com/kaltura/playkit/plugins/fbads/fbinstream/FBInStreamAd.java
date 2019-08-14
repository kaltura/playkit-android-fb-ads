package com.kaltura.playkit.plugins.fbads.fbinstream;

public class FBInStreamAd {

    private String adPlacementId;
    private long adBreakTime;
    private boolean adPlayed;
    private int adIndexInPod;

    public FBInStreamAd(String adPlacementId, long adBreakTime, int adIndexInPod) {
        this.adPlacementId = adPlacementId;
        this.adBreakTime = adBreakTime;
        this.adIndexInPod = adIndexInPod;
    }

    public String getAdPlacementId() {
        return adPlacementId;
    }

    public void setAdPlacementId(String adPlacementId) {
        this.adPlacementId = adPlacementId;
    }

    public long getAdBreakTime() {
        return adBreakTime;
    }

    public void setAdBreakTime(long adBreakTime) {
        this.adBreakTime = adBreakTime;
    }

    public boolean isAdPlayed() {
        return adPlayed;
    }

    public void setAdPlayed(boolean adPlayed) {
        this.adPlayed = adPlayed;
    }

    public int getAdIndexInPod() {
        return adIndexInPod;
    }

    public void setAdIndexInPod(int adIndexInPod) {
        this.adIndexInPod = adIndexInPod;
    }

//AdBreak adBreak1 = new AdBreak("156903085045437_239184776817267", 0, AdBreak.AdBreakType.PREROLL, AdResource.AdProvider.FACEBOOK, AdBreak.AdFormat.FB);
}
