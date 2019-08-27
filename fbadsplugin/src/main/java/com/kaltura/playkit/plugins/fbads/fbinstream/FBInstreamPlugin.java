package com.kaltura.playkit.plugins.fbads.fbinstream;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AdSize;
import com.facebook.ads.InstreamVideoAdListener;
import com.facebook.ads.InstreamVideoAdView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.playkit.BuildConfig;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEngineWrapper;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.FBAdsPlayerEngineWrapper;
import com.kaltura.playkit.ads.PKAdErrorType;
import com.kaltura.playkit.ads.PKAdInfo;
import com.kaltura.playkit.ads.PKAdPluginType;
import com.kaltura.playkit.ads.PKAdProviderListener;
import com.kaltura.playkit.player.PlayerEngine;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.plugins.ads.AdsProvider;
import com.kaltura.playkit.utils.Consts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class FBInstreamPlugin extends PKPlugin implements AdsProvider {
    private static final PKLog log = PKLog.get("FBInstreamPlugin");

    private Player player;
    private Context context;
    private MessageBus messageBus;
    private AdInfo adInfo;
    private FBInstreamConfig adConfig;
    private PlayerEngineWrapper adsPlayerEngineWrapper;
    private LinearLayout adContainer;
    private InstreamVideoAdView adView;
    private TreeMap<Long,FBInStreamAdBreak> fbInStreamAdBreaksMap;
    private boolean isPlayerPrepared;
    private boolean isAdDisplayed;
    private boolean isAdError;
    private boolean isAdRequested;
    private boolean isAllAdsCompleted;
    private PKAdProviderListener pkAdProviderListener;

    private int currentAdIndexInPod = -1;


    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "FBInstream";
        }

        @Override
        public PKPlugin newInstance() {
            return new FBInstreamPlugin();
        }

        @Override
        public String getVersion() {
            return BuildConfig.VERSION_NAME;
        }

        @Override
        public void warmUp(Context context) {

        }
    };
    @Override
    protected void onLoad(Player player, Object config, MessageBus messageBus, Context context) {
        log.d("FB Instream Ad onLoad");
        this.player = player;
        this.context = context;
        if (player == null) {
            log.e("Error, player instance is null.");
            return;
        }
        this.messageBus = messageBus;

        addListeners();

        adConfig = parseConfig(config);

        if (adConfig == null) {
            log.e("Error, adConfig instance is null.");
            return;
        }
        buildAdBreaksMap();
        initAdContentFrame();
    }

    private void addListeners() {
        this.messageBus.addListener(this, PlayerEvent.playheadUpdated, event -> {
            PlayerEvent.PlayheadUpdated playheadUpdated = event;
            long position = (playheadUpdated.position/ 100) * 100;

            log.e("FB Instream Ad position = " +  position);

            if (fbInStreamAdBreaksMap != null && fbInStreamAdBreaksMap.containsKey(position) && !fbInStreamAdBreaksMap.get(position).isAdBreakPlayed()) {
                isAdRequested = true;

                requestInStreamAdFromFB(fbInStreamAdBreaksMap.get(position));
                if (adView != null) {
                    if (getPlayerEngine() != null && getPlayerEngine().isPlaying()) {
                        getPlayerEngine().pause();
                    }
                }
            }
        });

        this.messageBus.addListener(this, PlayerEvent.seeking, event -> {
            long position = event.targetPosition;
            playPreviousUnplayedCuePoint(position);
        });

        this.messageBus.addListener(this, PlayerEvent.ended, event -> {
            if (adConfig.fbInStreamAdBreaks.get(adConfig.fbInStreamAdBreaks.size() -1).getAdBreakTime() == Long.MAX_VALUE) {
                requestInStreamAdFromFB(adConfig.fbInStreamAdBreaks.get(adConfig.fbInStreamAdBreaks.size() -1));
                isAllAdsCompleted = true;
            }
        });

        this.messageBus.addListener(this, PlayerEvent.playing, event -> {

        });

        this.messageBus.addListener(this, PlayerEvent.canPlay, event -> {
            isPlayerPrepared = true;
        });
    }

    private void playPreviousUnplayedCuePoint(long position) {
        if (position >= player.getDuration() && fbInStreamAdBreaksMap.containsKey(Long.MAX_VALUE) && !fbInStreamAdBreaksMap.get(Long.MAX_VALUE).isAdBreakPlayed()) {
            return;
        }
        Long lastIndexPosition = null;
        for (Map.Entry<Long, FBInStreamAdBreak> entry : fbInStreamAdBreaksMap.entrySet()) {
            if (entry.getKey() > position) {
                break;
            } else if (entry.getValue().isAdBreakPlayed()) {
                lastIndexPosition = entry.getKey();
            } else {
                lastIndexPosition = entry.getKey();
            }
        }
        if (lastIndexPosition != null && fbInStreamAdBreaksMap != null && !fbInStreamAdBreaksMap.get(lastIndexPosition).isAdBreakPlayed()) {
            requestInStreamAdFromFB(fbInStreamAdBreaksMap.get(lastIndexPosition));
            if (adView != null) {
                if (getPlayerEngine() != null && getPlayerEngine().isPlaying()) {
                    getPlayerEngine().pause();
                }
            }
        }
    }

    private void buildAdBreaksMap() {
        if (fbInStreamAdBreaksMap == null) {
            fbInStreamAdBreaksMap = new TreeMap<>();
        } else {
            fbInStreamAdBreaksMap.clear();
        }
        if (adConfig != null && adConfig.getAdBreakList() != null) {
            for (FBInStreamAdBreak adBreak : adConfig.getAdBreakList()) {
                fbInStreamAdBreaksMap.put(adBreak.getAdBreakTime(), adBreak);
            }
        }
        if (!fbInStreamAdBreaksMap.containsKey(0L)) {
            isAdRequested = true; // incase no preroll prepare....
        }
    }

    private void initAdContentFrame() {
        adContainer = new LinearLayout(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        adContainer.setLayoutParams(params);
        player.getView().addView(adContainer);
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        log.d("FB Instream Ad onUpdateMedia");
        isPlayerPrepared = false;
        isAdError = false;
        isAdRequested = false;
        isAdDisplayed = false;

        if (mediaConfig != null) {
            log.d("FB Instream Ad mediaConfig playbackStartPosition = " + mediaConfig.getStartPosition());
        }

        if(adConfig != null && adConfig.getAdBreakList() != null && adConfig.getAdBreakList().size() > 0) {
            if (adConfig.getAdBreakList().get(0).getAdBreakTime() == 0) {
                isAdRequested = true;
                requestInStreamAdFromFB(adConfig.getAdBreakList().get(0));
            } else {
                preparePlayer(true);
            }
        }
    }

    private void requestInStreamAdFromFB(FBInStreamAdBreak adBreak) {
        log.e("FB Instream Ad requestInStreamAdFromFB time = " +  adBreak.isAdBreakPlayed());


        if (adBreak.isAdBreakPlayed()) {
            return;
        }
        FBInStreamAd currentAdInAdBreak = null;
        for (FBInStreamAd adInAdBreak : adBreak.getFbInStreamAdList()) {
            if (adInAdBreak.isAdPlayed()) {
                continue;
            }
            currentAdInAdBreak = adInAdBreak;
            break;
        }

        if (currentAdInAdBreak == null) {
            log.e("FB Instream Ad currentAdInAdBreak is null");
            return;
        }

        currentAdInAdBreak.setAdPlayed(true);

        currentAdIndexInPod = currentAdInAdBreak.getAdIndexInPod();

        messageBus.post(new AdEvent.AdBufferStart(currentAdIndexInPod));

        // Instantiate an InstreamVideoAdView object.
        // NOTE: the placement ID will eventually identify this as your App, you can ignore it for
        // now, while you are testing and replace it later when you have signed up.
        // While you are using this temporary code you will only get test ads and if you release
        // your code like this to the Google Play your users will not receive ads (you will get a no fill error).
        if (adContainer == null) {
            initAdContentFrame();
        }
        AdSettings.addTestDevice("294d7470-4781-4795-9493-36602bf29231");//("7450a453-4ba6-464b-85b6-6f319c7f7326");
        //AdSettings.setVideoAutoplayOnMobile(true);
        AdSettings.setDebugBuild(true);

        adView = new InstreamVideoAdView(
                context, currentAdInAdBreak.getAdPlacementId(), ///*"156903085045437_239184776817267"*/
                getAdSize());
        // set ad listener to handle events

        createAdInfo(adBreak, currentAdInAdBreak);

        adView.setAdListener(new InstreamVideoAdListener() {
            @Override
            public void onAdVideoComplete(Ad ad) {
                // Instream Video View Complete - the video has been played to the end.
                // You can use this event to continue your video playing
                log.d("FB Instream Ad completed!");

                if (adBreak.isAdBreakPlayed() && isAllAdsCompleted) {
                    isAllAdsCompleted = false;
                    messageBus.post(new AdEvent(AdEvent.Type.ALL_ADS_COMPLETED));
                }

                if (adBreak.isAdBreakPlayed()) {
                    messageBus.post(new AdEvent(AdEvent.Type.CONTENT_RESUME_REQUESTED));
                    isAdDisplayed = false;
                    player.getView().showVideoSurface();
                    adContainer.setVisibility(View.GONE);
                    getPlayerEngine().play();
                } else {
                    messageBus.post(new AdEvent(AdEvent.Type.CONTENT_PAUSE_REQUESTED));
                    requestInStreamAdFromFB(adBreak);
                }
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                // Instream video ad failed to load
                log.e("FB Instream Ad onError " + adError.getErrorCode() + ":" + adError.getErrorMessage());
                isAdError = true;
                isAdDisplayed = false;

                sendError(PKAdErrorType.INTERNAL_ERROR, adError.getErrorCode() + ":" + adError.getErrorMessage(), null);

                if (isPlayerPrepared) {
                    if (adContainer != null) {
                        adContainer.setVisibility(View.GONE);
                    }
                    getPlayerEngine().play();
                }
            }

            @Override
            public void onAdLoaded(Ad ad) {
                player.getView().hideVideoSurface();
                // Instream video ad is loaded and ready to be displayed
                log.d("FB Instream Ad is loaded and ready to be displayed!");
                // Race condition, load() called again before last ad was displayed

                messageBus.post(new AdEvent.AdBufferEnd(currentAdIndexInPod));

                messageBus.post(new AdEvent.AdLoadedEvent(adInfo));

                if (adView == null || !adView.isAdLoaded()) {
                    return;
                }

                // Inflate Ad into container and show it
                adContainer.removeAllViews();
                adContainer.addView(adView);
                adContainer.setVisibility(View.VISIBLE);
                getPlayerEngine().pause();


                adView.show();
                isAdDisplayed = true;
                if (!isPlayerPrepared) {
                    preparePlayer(false);
                }
            }

            @Override
            public void onAdClicked(Ad ad) {
                log.d("FB Instream Ad clicked!");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                // Instream Video ad impression - the event will fire when the
                // video starts playing
                log.d("FB Instream Ads Impression logged!" + ad.toString());
                messageBus.post(new AdEvent.AdStartedEvent(adInfo));
            }
        });
        if (getPlayerEngine().isPlaying()) {
            getPlayerEngine().pause();
        }
        adView.loadAd();
        isAdDisplayed = true;
    }

    private AdInfo createAdInfo(FBInStreamAdBreak adBreak, FBInStreamAd ad) {

        String adDescription = ad.getAdPlacementId();
        long adDuration = (long) ad.getAdBreakTime() * Consts.MILLISECONDS_MULTIPLIER;
        long adPlayHead = getCurrentPosition() * Consts.MILLISECONDS_MULTIPLIER;
        String adTitle = ad.getAdPlacementId();
        boolean isAdSkippable = true; // ad.isSkippable();
        long skipTimeOffset = 5L; //(long) ad.getSkipTimeOffset() * Consts.MILLISECONDS_MULTIPLIER;
        String contentType = "facebook"; // ad.getContentType();
        String adId = ad.getAdPlacementId();
        String adSystem = "facebook"; // ad.getAdSystem();
        int adHeight = 540; //ad.isLinear() ? ad.getVastMediaHeight() : ad.getHeight();
        int adWidth  = 760; //ad.isLinear() ? ad.getVastMediaWidth() : ad.getWidth();
        int mediaBitrate = 1024; //ad.getVastMediaBitrate() != 0 ? ad.getVastMediaBitrate() * KB_MULTIPLIER : -1;
        int totalAdsInPod = adBreak.getFbInStreamAdList().size(); //ad.getAdPodInfo().getTotalAds();
        int adIndexInPod = currentAdIndexInPod; // ad.getAdPodInfo().getAdPosition();   // index starts in 1
        int podCount = ad.getAdIndexInPod(); // (adsManager != null && adsManager.getAdCuePoints() != null) ? adsManager.getAdCuePoints().size() : 0;

        int podIndex = ad.getAdIndexInPod(); //(ad.getAdPodInfo().getPodIndex() >= 0) ? ad.getAdPodInfo().getPodIndex() + 1 : podCount; // index starts in 0
        if (podIndex == 1 && podCount == 0) { // For Vast
            podCount = 1;
        }
        boolean isBumper = false; //ad.getAdPodInfo().isBumper();
        long adPodTimeOffset = 1000L; //(long) ad.getAdPodInfo().getTimeOffset() * Consts.MILLISECONDS_MULTIPLIER;

        if (!PKMediaFormat.mp4.mimeType.equals(PKMediaFormat.mp4) && adInfo != null) {
            adHeight = adInfo.getAdHeight();
            adWidth = adInfo.getAdWidth();
            mediaBitrate = adInfo.getMediaBitrate();
        }

        AdInfo adInfo = new AdInfo(adDescription, adDuration, adPlayHead,
                adTitle, isAdSkippable, skipTimeOffset,
                contentType, adId,
                adSystem,
                adHeight,
                adWidth,
                mediaBitrate,
                totalAdsInPod,
                adIndexInPod,
                podIndex,
                podCount,
                isBumper,
                (adPodTimeOffset < 0) ? -1 : adPodTimeOffset);

        adInfo.setAdPlayHead(player.getCurrentPosition() * Consts.MILLISECONDS_MULTIPLIER);

        log.v("AdInfo: " + adInfo.toString());
        this.adInfo = adInfo;
        return adInfo;
    }

    private void preparePlayer(boolean doPlay) {
        isPlayerPrepared = true;
        if (pkAdProviderListener != null) {
            pkAdProviderListener.onAdLoadingFinished();
        }
        if (doPlay) {
            getPlayerEngine().play();
        }
    }

    private AdSize getAdSize() {
        return new AdSize(pxToDP(adContainer.getMeasuredWidth()), pxToDP(adContainer.getMeasuredHeight()));
    }

    private int pxToDP(int px) {
        return (int) (px / context.getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onUpdateConfig(Object config) {
        log.d("FB Instream Ads onUpdateConfig");
        adConfig = parseConfig(config);
        buildAdBreaksMap();
    }

    @Override
    protected void onApplicationPaused() {
        log.d("Start onApplicationPaused");
    }

    @Override
    protected void onApplicationResumed() {
        log.d("Start onApplicationResumed");
    }

    @Override
    protected void onDestroy() {
        log.d("FB Instream Ads onDestroy");
        if(adView!=null) {
            adView.destroy();
        }
        if (adContainer != null) {
            adContainer.setVisibility(View.GONE);
            adContainer.removeAllViews();
            adContainer = null;
        }
    }

    @Override
    public void start() {
        log.d("FB Instream Ads start");
        if (!isAdRequested && fbInStreamAdBreaksMap != null && fbInStreamAdBreaksMap.containsKey(0L)) {
            requestInStreamAdFromFB(fbInStreamAdBreaksMap.get(0L));
        } else {
            preparePlayer(true);
        }
    }

    @Override
    public void destroyAdsManager() {
        log.d("FB Instream Ads destroyAdsManager");
        if(adView!=null) {
            adView.destroy();
        }
        if (adContainer != null) {
            adContainer.setVisibility(View.GONE);
            adContainer.removeAllViews();
            adContainer = null;
        }
    }

    @Override
    public void resume() {
        log.d("Start resume");
    }

    @Override
    public void pause() {
        log.d("Start pause");
    }

    @Override
    public void contentCompleted() {
        log.d("Start contentCompleted");
    }

    @Override
    public PKAdInfo getAdInfo() {
        return adInfo;
    }

    @Override
    public AdCuePoints getCuePoints() {
        if (fbInStreamAdBreaksMap == null) {
            return null;
        }
        SortedSet<Long> fbInStreamAdBreaksMapKeys = new TreeSet<>(fbInStreamAdBreaksMap.keySet());
        List<Long> adBreaksList = new ArrayList<>();

        for (Long adBreakTime : fbInStreamAdBreaksMapKeys) {
            if (fbInStreamAdBreaksMapKeys.size() > 0 && getPlayerEngine() != null && getPlayerEngine().getDuration() > 0 && adBreakTime == getPlayerEngine().getDuration()) {
                adBreaksList.add(-1L);
            } else {
                adBreaksList.add(adBreakTime);
            }
        }
        return new AdCuePoints(adBreaksList);
    }

    @Override
    public boolean isAdDisplayed() {
        return isAdDisplayed;
    }

    @Override
    public boolean isAdPaused() {
        return false;
    }

    @Override
    public boolean isAdRequested() {
        return isAdRequested;
    }

    @Override
    public boolean isAllAdsCompleted() {
        return isAllAdsCompleted;
    }

    @Override
    public boolean isAdError() {
        return isAdError;
    }

    @Override
    public long getDuration() {
        return Consts.TIME_UNSET;
    }

    @Override
    public long getCurrentPosition() {
        return Consts.POSITION_UNSET;
    }

    @Override
    public void setAdRequested(boolean isAdRequested) {
        this.isAdRequested = isAdRequested;
    }

    @Override
    public void setAdProviderListener(PKAdProviderListener adProviderListener) {
        pkAdProviderListener = adProviderListener;
    }

    @Override
    public void removeAdProviderListener() {
        pkAdProviderListener = null;
    }

    @Override
    public void skipAd() {
        log.d("Start skipAd");
    }

    @Override
    public PKAdPluginType getAdPluginType() {
        return PKAdPluginType.client;
    }

    @Override
    public boolean isContentPrepared() {
        return false;
    }

    @Override
    protected PlayerEngineWrapper getPlayerEngineWrapper() {
        if (adsPlayerEngineWrapper == null) {
            adsPlayerEngineWrapper = new FBAdsPlayerEngineWrapper(context, this);
        }
        return adsPlayerEngineWrapper;
    }

    private PlayerEngine getPlayerEngine() {
        return adsPlayerEngineWrapper.getPlayerEngine();
    }

    private static FBInstreamConfig parseConfig(Object config) {
        if (config instanceof FBInstreamConfig) {
            return ((FBInstreamConfig) config);
        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), FBInstreamConfig.class);
        }
        return null;
    }

    private void sendError(Enum errorType, String message, Throwable exception) {
        log.e("Ad Error: " + errorType.name() + " with message " + message);
        AdEvent errorEvent = new AdEvent.Error(new PKError(errorType, message, exception));
        messageBus.post(errorEvent);
    }

}
