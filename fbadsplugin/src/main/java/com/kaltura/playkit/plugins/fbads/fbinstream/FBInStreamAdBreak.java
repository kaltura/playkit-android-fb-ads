package com.kaltura.playkit.plugins.fbads.fbinstream;

import com.kaltura.playkit.plugins.ads.AdPositionType;

import java.util.List;

public class FBInStreamAdBreak {
    private long adBreakTime;
    private List<FBInStreamAd> fbInStreamAdList;
    private AdPositionType adBreakType;

    public FBInStreamAdBreak(AdPositionType adBreakType, long adBreakTime, List<FBInStreamAd> fbInStreamAdList) {
        this.adBreakType = adBreakType;
        this.adBreakTime = adBreakTime;
        this.fbInStreamAdList = fbInStreamAdList;
        if (fbInStreamAdList != null && fbInStreamAdList.size() > 0) {
            for (int indx = 0 ; indx < fbInStreamAdList.size() ; indx++) {
                if (fbInStreamAdList.get(indx) != null) {
                    fbInStreamAdList.get(indx).setAdBreakTime(adBreakTime);
                    fbInStreamAdList.get(indx).setAdIndexInPod(indx);
                }
            }
        }
    }

    public List<FBInStreamAd> getFbInStreamAdList() {
        return fbInStreamAdList;
    }

    public void setFbInStreamAdList(List<FBInStreamAd> fbInStreamAdList) {
        this.fbInStreamAdList = fbInStreamAdList;
    }

    public long getAdBreakTime() {
        return adBreakTime;
    }

    public AdPositionType getAdBreakType() {
        return adBreakType;
    }

    public void setAdBreakType(AdPositionType adBreakType) {
        this.adBreakType = adBreakType;
    }

    public boolean isAdBreakPlayed() {

        for (FBInStreamAd  fbInStreamAd : fbInStreamAdList) {
            if (!fbInStreamAd.isAdPlayed()) {
                return false;
            }
        }
        return true;
    }
}
