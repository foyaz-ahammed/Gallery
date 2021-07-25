package com.kr.gallery.pro.models;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.kk.taurus.playerbase.entity.DataSource;
import com.kk.taurus.playerbase.event.BundlePool;
import com.kk.taurus.playerbase.event.EventKey;
import com.kk.taurus.playerbase.event.OnPlayerEventListener;
import com.kk.taurus.playerbase.log.PLog;
import com.kk.taurus.playerbase.player.IPlayer;
import com.kk.taurus.playerbase.player.OnTimerUpdateListener;
import com.kk.taurus.playerbase.receiver.BaseCover;
import com.kk.taurus.playerbase.receiver.IReceiverGroup;
import com.kk.taurus.playerbase.receiver.PlayerStateGetter;
import com.kk.taurus.playerbase.touch.OnTouchGestureListener;
import com.kk.taurus.playerbase.utils.TimeUtil;
import com.kr.gallery.pro.R;
import com.kr.gallery.pro.interfaces.DataInter;

import java.util.HashMap;

import static android.content.Context.AUDIO_SERVICE;
import static com.kk.taurus.playerbase.config.AppContextAttach.getApplicationContext;

/**
 * Created by Taurus on 2018/4/15.
 */

public class ControllerCover extends BaseCover implements OnTimerUpdateListener, OnTouchGestureListener {

    private final int MSG_CODE_DELAY_HIDDEN_CONTROLLER = 101;

    View mTopContainer;
    View mBottomContainer;
    ImageView mBackIcon;
    TextView mTopTitle;
    ImageView mStateIcon;
    ImageView mScreenShotIcon;
    ImageView mControllerLockIcon;
    TextView mCurrTime;
    TextView mTotalTime;
    ImageView mSwitchScreen;
    SeekBar mSeekBar;

    ImageView mCancelFloat;
    ImageView mSettingBtn;
    View mSettingsView;

    SeekBar mVolumeSeekbar;
    SeekBar mBrightnessSeekbar;

    private int mBufferPercentage;

    private int mSeekProgress = -1;

    private boolean mTimerUpdateProgressEnable = true;

    private Activity mActivity;
    private AudioManager mAudioManager;
    private int mMaxVolume;

    private float mPlaySpeed = 1f;
    private HashMap<Float, Integer> speedViewIDList;

    private HashMap<String, Integer> loopViewIDList;
    public static final String LOOP_DEFAULT = "loop_default";
    public static final String LOOP_ONE = "loop_one";
    private String mLoop = LOOP_DEFAULT;

    View mTimerView;
    TextView mTimerBtn;

    private int mTimerDurationMin = 0;
    private long mTimerRemainMilli = 0;
    private long mTimerCountPreTime = 0;
    private HashMap<Integer, Integer> timerViewIDList;

    Handler mVideoTimerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            long millis = now - mTimerCountPreTime;
            mTimerRemainMilli = Math.max(0, mTimerRemainMilli - millis);

            setTimerBtnText(mTimerRemainMilli);

            mTimerCountPreTime = now;
            if (mTimerRemainMilli > 0) mVideoTimerHandler.postDelayed(this, 500);
            else notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_BACK, null);
        }
    };

    private boolean mControllerLocked = false;

    private boolean mIsLandscape;

    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_CODE_DELAY_HIDDEN_CONTROLLER:
                    PLog.d(getTag().toString(), "msg_delay_hidden...");
                    setControllerState(false);
                    break;
            }
        }
    };

    private boolean mGestureEnable = true;

    private String mTimeFormat;

    private boolean mControllerTopEnable;
    private boolean mIsFloatWindow = false;

    private ObjectAnimator mBottomAnimator;
    private ObjectAnimator mScreenShotIconAnimator;
    private ObjectAnimator mLockIconAnimator;
    private ObjectAnimator mTopAnimator;

    public ControllerCover(Context context) {
        super(context);
    }

    @Override
    protected void onCoverAttachedToWindow() {
        super.onCoverAttachedToWindow();
        DataSource dataSource = getGroupValue().get(DataInter.Key.KEY_DATA_SOURCE);
        setTitle(dataSource);

        boolean topEnable = getGroupValue().getBoolean(DataInter.Key.KEY_CONTROLLER_TOP_ENABLE, false);
        mControllerTopEnable = topEnable;
        if(!topEnable){
            setTopContainerState(false);
        }

        boolean screenSwitchEnable = getGroupValue().getBoolean(DataInter.Key.KEY_CONTROLLER_SCREEN_SWITCH_ENABLE, true);
        setScreenSwitchEnable(screenSwitchEnable);

        // below was in bind
        mSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mVolumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mMaxVolume * progress / 100, 0);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mBrightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                WindowManager.LayoutParams lpa = mActivity.getWindow().getAttributes();
                lpa.screenBrightness = progress / 100f;
                if (lpa.screenBrightness > 1.0f){
                    lpa.screenBrightness = 1.0f;
                }else if (lpa.screenBrightness < 0.01f){
                    lpa.screenBrightness = 0.01f;
                }
                mActivity.getWindow().setAttributes(lpa);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        getGroupValue().registerOnGroupValueUpdateListener(mOnGroupValueUpdateListener);
    }

    @Override
    protected void onCoverDetachedToWindow() {
        super.onCoverDetachedToWindow();
        mTopContainer.setVisibility(View.GONE);
        mBottomContainer.setVisibility(View.GONE);
        removeDelayHiddenMessage();

        // below was in unbind
        cancelTopAnimation();
        cancelBottomAnimation();

        getGroupValue().unregisterOnGroupValueUpdateListener(mOnGroupValueUpdateListener);
        removeDelayHiddenMessage();
        mHandler.removeCallbacks(mSeekEventRunnable);
    }

    public void onViewClick(View view){
        switch (view.getId()){
            case R.id.cover_player_controller_image_view_back_icon:
                notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_BACK, null);
                break;
            case R.id.cover_player_controller_image_view_play_state:
                boolean selected = mStateIcon.isSelected();
                if(selected){
                    Bundle bundle = new Bundle();
                    bundle.putFloat(EventKey.INT_ARG1, mPlaySpeed);
                    notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_SET_PLAY_SPEED, bundle);
                    // above set speed is equivalent to resume
//                    requestResume(null);
                }else{
                    requestPause(null);
                }
                mStateIcon.setSelected(!selected);
                break;
            case R.id.cover_player_controller_image_view_switch_screen:
                notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_TOGGLE_SCREEN, null);
                break;
            case R.id.cover_player_controller_image_view_screen_shot:
                notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_SCREEN_SHOT, null);
                break;
            case R.id.video_setting_float_window:
                setScreenShotIconState(false);
                setLockIconState(false);
                mSettingsView.setVisibility(View.GONE);
                notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_CONTROLLER_LOCK, null);
                break;
            case R.id.cover_player_controller_cancel_float_window:
                notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_CANCEL_FLOAT_WINDOW, null);
                break;
            case R.id.cover_player_controller_setting_btn:
                // set seekbar volume
                int currentVolume = getVolume();
                mVolumeSeekbar.setProgress( 100 * currentVolume / mMaxVolume);
                // set seekbar brightness
                float brightness = getBrightness();
                mBrightnessSeekbar.setProgress( (int)(100 * brightness));
                // select speed textview
                selectVideoSpeed(mPlaySpeed);
                //select loop
                selectVideoLoop(mLoop);
                // select timer
                selectVideoTimer(mTimerDurationMin);

                setTopContainerState(false);
                setBottomContainerState(false);
                setScreenShotIconState(false);
                setLockIconState(false);

                mSettingsView.setVisibility(View.VISIBLE);
                mTimerView.setVisibility(View.GONE);
                break;
            case R.id.video_setting_speed_0_5:
                selectVideoSpeed(0.5f);
                break;
            case R.id.video_setting_speed_0_75:
                selectVideoSpeed(0.75f);
                break;
            case R.id.video_setting_speed_1_0:
                selectVideoSpeed(1f);
                break;
            case R.id.video_setting_speed_1_25:
                selectVideoSpeed(1.25f);
                break;
            case R.id.video_setting_speed_1_5:
                selectVideoSpeed(1.5f);
                break;
            case R.id.video_setting_speed_2_0:
                selectVideoSpeed(2.0f);
                break;
            case R.id.video_loop_default:
                selectVideoLoop(LOOP_DEFAULT);
                break;
            case R.id.video_loop_one:
                selectVideoLoop(LOOP_ONE);
                break;
            case R.id.video_setting_timer:
                mSettingsView.setVisibility(View.GONE);
                mTimerView.setVisibility(View.VISIBLE);
                break;
            case R.id.video_setting_timer_none:
                setNewTimerTime(0);
                break;
            case R.id.video_setting_timer_30_min:
                setNewTimerTime(30);
                break;
            case R.id.video_setting_timer_60_min:
                setNewTimerTime(60);
                break;
            case R.id.cover_player_controller_image_view_lock:
                mControllerLocked = !mControllerLocked;

                if (mControllerLocked) mControllerLockIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_lock_vector));
                else mControllerLockIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),R.drawable.baseline_lock_open_white_24));

                setControllerState(!mControllerLocked);

                Bundle bundle = new Bundle();
                bundle.putBoolean(EventKey.INT_ARG1, mControllerLocked);
                notifyReceiverPrivateEvent(
                        DataInter.ReceiverKey.KEY_GESTURE_COVER,
                        DataInter.PrivateEvent.EVENT_CONTROLLER_LOCK,
                        bundle);

                if (mControllerLocked) {
                    if (mIsLandscape) mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    else mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                } else mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

                break;
        }
    }

    private IReceiverGroup.OnGroupValueUpdateListener mOnGroupValueUpdateListener =
            new IReceiverGroup.OnGroupValueUpdateListener() {
        @Override
        public String[] filterKeys() {
            return new String[]{
                    DataInter.Key.KEY_COMPLETE_SHOW,
                    DataInter.Key.KEY_TIMER_UPDATE_ENABLE,
                    DataInter.Key.KEY_DATA_SOURCE,
                    DataInter.Key.KEY_IS_LANDSCAPE,
                    DataInter.Key.KEY_CONTROLLER_TOP_ENABLE,
                    DataInter.Key.KEY_IS_FLOAT_WINDOW};
        }

        @Override
        public void onValueUpdate(String key, Object value) {
            if(key.equals(DataInter.Key.KEY_COMPLETE_SHOW)){
                boolean show = (boolean) value;
                if(show){
                    setControllerState(false);
                }
                setGestureEnable(!show);
            }else if(key.equals(DataInter.Key.KEY_CONTROLLER_TOP_ENABLE)){
                mControllerTopEnable = (boolean) value;
                if(!mControllerTopEnable){
                    setTopContainerState(false);
                }
            }else if(key.equals(DataInter.Key.KEY_IS_LANDSCAPE)){
                mIsLandscape = (Boolean) value;
                setSwitchScreenIcon(mIsLandscape);
            }else if(key.equals(DataInter.Key.KEY_TIMER_UPDATE_ENABLE)){
                mTimerUpdateProgressEnable = (boolean) value;
            }else if(key.equals(DataInter.Key.KEY_DATA_SOURCE)){
                DataSource dataSource = (DataSource) value;
                setTitle(dataSource);
            }else if(key.equals(DataInter.Key.KEY_IS_FLOAT_WINDOW)) {
                mIsFloatWindow = (boolean)value;

                if (mIsFloatWindow) {
                    mCancelFloat.setVisibility(View.VISIBLE);
                    mSwitchScreen.setVisibility(View.GONE);
                } else {
                    mCancelFloat.setVisibility(View.GONE);
                    mSwitchScreen.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    private int stateBeforeTracking = 0;

    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener =
            new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser) {
                updateUI(progress, seekBar.getMax());
                sendSeekEvent(seekBar.getProgress());
            }
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            PlayerStateGetter playerStateGetter = getPlayerStateGetter();
            if (playerStateGetter != null) stateBeforeTracking = playerStateGetter.getState();

            removeDelayHiddenMessage();
            requestPause(null);
        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            sendSeekEvent(seekBar.getProgress());
            sendDelayHiddenMessage();

            if (stateBeforeTracking == IPlayer.STATE_STARTED) requestResume(null);
        }
    };

    private void sendSeekEvent(int progress){
        mTimerUpdateProgressEnable = false;
        mSeekProgress = progress;
//        mHandler.removeCallbacks(mSeekEventRunnable);
        mHandler.postDelayed(mSeekEventRunnable, 0);
    }

    private Runnable mSeekEventRunnable = new Runnable() {
        @Override
        public void run() {
            if(mSeekProgress < 0)
                return;
            Bundle bundle = BundlePool.obtain();
            bundle.putInt(EventKey.INT_DATA, mSeekProgress);
            requestSeek(bundle);
        }
    };

    private void setTitle(DataSource dataSource){
        if(dataSource!=null){
            String title = dataSource.getTitle();
            if(!TextUtils.isEmpty(title)){
                setTitle(title);
                return;
            }
            String data = dataSource.getData();
            if(!TextUtils.isEmpty(data)){
                setTitle(data);
            }
        }
    }

    private void setTitle(String text){
        mTopTitle.setText(text);
    }

    private void setSwitchScreenIcon(boolean isFullScreen){
        mSwitchScreen.setImageResource(isFullScreen?R.mipmap.icon_exit_full_screen:R.mipmap.icon_full_screen);

        // update setting view layout
        LinearLayout.LayoutParams settingsViewLP = (LinearLayout.LayoutParams) mSettingsView.getLayoutParams();
        LinearLayout.LayoutParams timerViewLP = (LinearLayout.LayoutParams) mTimerView.getLayoutParams();
        if (isFullScreen) {
            settingsViewLP.weight = 1;
            settingsViewLP.height = LinearLayout.LayoutParams.MATCH_PARENT;

            timerViewLP.weight = 1;
            timerViewLP.height = LinearLayout.LayoutParams.MATCH_PARENT;
        } else {
            settingsViewLP.weight = 2;
            settingsViewLP.height = LinearLayout.LayoutParams.WRAP_CONTENT;

            timerViewLP.weight = 2;
            timerViewLP.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        }
        mSettingsView.setLayoutParams(settingsViewLP);
        mTimerView.setLayoutParams(timerViewLP);
    }

    private void setScreenSwitchEnable(boolean screenSwitchEnable) {
        mSwitchScreen.setVisibility(screenSwitchEnable?View.VISIBLE:View.GONE);
    }

    private void setGestureEnable(boolean gestureEnable) {
        this.mGestureEnable = gestureEnable;
    }

    private void cancelTopAnimation(){
        if(mTopAnimator!=null){
            mTopAnimator.cancel();
            mTopAnimator.removeAllListeners();
            mTopAnimator.removeAllUpdateListeners();
        }
    }

    private void setTopContainerState(final boolean state){
        if(mControllerTopEnable){
            mTopContainer.clearAnimation();
            cancelTopAnimation();
            mTopAnimator = ObjectAnimator.ofFloat(mTopContainer,
                            "alpha", state ? 0 : 1, state ? 1 : 0).setDuration(300);
            mTopAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    if(state){
                        mTopContainer.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if(!state){
                        mTopContainer.setVisibility(View.GONE);
                    }
                }
            });
            mTopAnimator.start();
        }else{
            mTopContainer.setVisibility(View.GONE);
        }
    }

    private void cancelBottomAnimation(){
        if(mBottomAnimator!=null){
            mBottomAnimator.cancel();
            mBottomAnimator.removeAllListeners();
            mBottomAnimator.removeAllUpdateListeners();
        }
    }

    private void setBottomContainerState(final boolean state){
        mBottomContainer.clearAnimation();
        cancelBottomAnimation();
        mBottomAnimator = ObjectAnimator.ofFloat(mBottomContainer,
                "alpha", state ? 0 : 1, state ? 1 : 0).setDuration(300);
        mBottomAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if(state){
                    mBottomContainer.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(!state){
                    mBottomContainer.setVisibility(View.GONE);
                }
            }
        });
        mBottomAnimator.start();
    }
    private void cancelScreenShotIconAnimation(){
        if(mScreenShotIconAnimator!=null){
            mScreenShotIconAnimator.cancel();
            mScreenShotIconAnimator.removeAllListeners();
            mScreenShotIconAnimator.removeAllUpdateListeners();
        }
    }
    private void setScreenShotIconState(final boolean state){
        mScreenShotIcon.clearAnimation();
        cancelScreenShotIconAnimation();
        mScreenShotIconAnimator = ObjectAnimator.ofFloat(mScreenShotIcon,
                "alpha", state ? 0 : 1, state ? 1 : 0).setDuration(300);
        mScreenShotIconAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if(state){
                    mScreenShotIcon.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(!state){
                    mScreenShotIcon.setVisibility(View.GONE);
                }
            }
        });
        mScreenShotIconAnimator.start();
    }

    private void cancelLockIconAnimation(){
        if(mLockIconAnimator!=null){
            mLockIconAnimator.cancel();
            mLockIconAnimator.removeAllListeners();
            mLockIconAnimator.removeAllUpdateListeners();
        }
    }
    private void setLockIconState(final boolean state){
        mControllerLockIcon.clearAnimation();
        cancelLockIconAnimation();
        mLockIconAnimator = ObjectAnimator.ofFloat(mControllerLockIcon,
                "alpha", state ? 0 : 1, state ? 1 : 0).setDuration(300);
        mLockIconAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if(state){
                    mControllerLockIcon.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(!state){
                    mControllerLockIcon.setVisibility(View.GONE);
                }
            }
        });
        mLockIconAnimator.start();
    }

    private void setControllerState(boolean state){
        if(state){
            sendDelayHiddenMessage();
        }else{
            removeDelayHiddenMessage();
        }
        setTopContainerState(state);
        setBottomContainerState(state);
        setScreenShotIconState(state && !mIsFloatWindow);
        setLockIconState(state && !mIsFloatWindow);

        if(state) showSystemUI();
        else hideSystemUI();
    }

    private boolean isControllerShow(){
        if (mIsFloatWindow) return mBottomContainer.getVisibility() == View.VISIBLE;
        else return mControllerLockIcon.getVisibility()==View.VISIBLE;
    }

    private void toggleController(){
        if(isControllerShow()){
            setControllerState(false);
        } else {
            if (!mControllerLocked) {
                setControllerState(true);
                mSettingsView.setVisibility(View.GONE);
                mTimerView.setVisibility(View.GONE);
            } else setLockIconState(true);
        }
    }

    private void showSystemUI() {
        mActivity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void hideSystemUI() {
        mActivity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void sendDelayHiddenMessage(){
        removeDelayHiddenMessage();
        mHandler.sendEmptyMessageDelayed(MSG_CODE_DELAY_HIDDEN_CONTROLLER, 5000);
    }

    private void removeDelayHiddenMessage(){
        mHandler.removeMessages(MSG_CODE_DELAY_HIDDEN_CONTROLLER);
    }

    private void setCurrTime(int curr){
        mCurrTime.setText(TimeUtil.getTime(mTimeFormat, curr));
    }

    private void setTotalTime(int duration){
        mTotalTime.setText(TimeUtil.getTime(mTimeFormat, duration));
    }

    private void setSeekProgress(int curr, int duration){
        mSeekBar.setMax(duration);
        mSeekBar.setProgress(curr);
        float secondProgress = mBufferPercentage * 1.0f/100 * duration;
        setSecondProgress((int) secondProgress);
    }

    private void setSecondProgress(int secondProgress){
        mSeekBar.setSecondaryProgress(secondProgress);
    }

    @Override
    public void onTimerUpdate(int curr, int duration, int bufferPercentage) {
        if(!mTimerUpdateProgressEnable)
            return;
        if(mTimeFormat==null || duration != mSeekBar.getMax()){
            mTimeFormat = TimeUtil.getFormat(duration);
        }
        mBufferPercentage = bufferPercentage;
        updateUI(curr, duration);
    }

    private void updateUI(int curr, int duration) {
        setSeekProgress(curr, duration);
        setCurrTime(curr);
        setTotalTime(duration);
    }

    @Override
    public void onPlayerEvent(int eventCode, Bundle bundle) {
        switch (eventCode){
            case OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET:
                mBufferPercentage = 0;
                mTimeFormat = null;
                updateUI(0, 0);
                DataSource data = (DataSource) bundle.getSerializable(EventKey.SERIALIZABLE_DATA);
                getGroupValue().putObject(DataInter.Key.KEY_DATA_SOURCE, data);
                setTitle(data);
                break;
            case OnPlayerEventListener.PLAYER_EVENT_ON_STATUS_CHANGE:
                int status = bundle.getInt(EventKey.INT_DATA);
                if(status== IPlayer.STATE_PAUSED){
                    mStateIcon.setSelected(true);
                }else if(status== IPlayer.STATE_STARTED){
                    mStateIcon.setSelected(false);
                }
                break;
            case OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START:
            case OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE:
                mTimerUpdateProgressEnable = true;
                break;
            case OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE:
                requestPause(null);
                break;
            case OnPlayerEventListener.PLAYER_EVENT_ON_START:
            case OnPlayerEventListener.PLAYER_EVENT_ON_RESUME:
                startVideoTimer();
                break;
            case OnPlayerEventListener.PLAYER_EVENT_ON_PAUSE:
                stopVideoTimer();
                break;
        }
    }

    @Override
    public void onErrorEvent(int eventCode, Bundle bundle) {

    }

    @Override
    public void onReceiverEvent(int eventCode, Bundle bundle) {

    }

    @Override
    public Bundle onPrivateEvent(int eventCode, Bundle bundle) {
        switch (eventCode){
            case DataInter.PrivateEvent.EVENT_CODE_UPDATE_SEEK:
                if(bundle!=null){
                    int curr = bundle.getInt(EventKey.INT_ARG1);
                    int duration = bundle.getInt(EventKey.INT_ARG2);
                    updateUI(curr, duration);
                }
                break;
        }
        return null;
    }

    @Override
    public View onCreateCoverView(Context context) {

        mActivity = getActivity();

        View view = View.inflate(context, R.layout.layout_controller_cover, null);

        mTopContainer = view.findViewById(R.id.cover_player_controller_top_container);
        mBottomContainer = view.findViewById(R.id.cover_player_controller_bottom_container);
        mBackIcon = view.findViewById(R.id.cover_player_controller_image_view_back_icon);
        mTopTitle = view.findViewById(R.id.cover_player_controller_text_view_video_title);
        mStateIcon = view.findViewById(R.id.cover_player_controller_image_view_play_state);
        mControllerLockIcon = view.findViewById(R.id.cover_player_controller_image_view_lock);
        mScreenShotIcon = view.findViewById(R.id.cover_player_controller_image_view_screen_shot);
        mCurrTime = view.findViewById(R.id.cover_player_controller_text_view_curr_time);
        mTotalTime = view.findViewById(R.id.cover_player_controller_text_view_total_time);
        mSwitchScreen = view.findViewById(R.id.cover_player_controller_image_view_switch_screen);
        mSeekBar = view.findViewById(R.id.cover_player_controller_seek_bar);
        mCancelFloat = view.findViewById(R.id.cover_player_controller_cancel_float_window);

        mSettingsView = view.findViewById(R.id.cover_player_controller_settings_view);
        mSettingBtn = view.findViewById(R.id.cover_player_controller_setting_btn);

        mVolumeSeekbar = view.findViewById(R.id.video_setting_volume_seekbar);
        mBrightnessSeekbar = view.findViewById(R.id.video_setting_brightness_seekbar);

        speedViewIDList = new HashMap<>();
        speedViewIDList.put(0.5f, R.id.video_setting_speed_0_5);
        speedViewIDList.put(0.75f, R.id.video_setting_speed_0_75);
        speedViewIDList.put(1f, R.id.video_setting_speed_1_0);
        speedViewIDList.put(1.25f, R.id.video_setting_speed_1_25);
        speedViewIDList.put(1.5f, R.id.video_setting_speed_1_5);
        speedViewIDList.put(2f, R.id.video_setting_speed_2_0);

        view.findViewById(R.id.video_setting_speed_0_5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });
        view.findViewById(R.id.video_setting_speed_0_75).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });
        view.findViewById(R.id.video_setting_speed_1_0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });
        view.findViewById(R.id.video_setting_speed_1_25).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });
        view.findViewById(R.id.video_setting_speed_1_5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });
        view.findViewById(R.id.video_setting_speed_2_0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });

        loopViewIDList = new HashMap<>();
        loopViewIDList.put(LOOP_DEFAULT, R.id.video_loop_default);
        loopViewIDList.put(LOOP_ONE, R.id.video_loop_one);
        view.findViewById(R.id.video_loop_default).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });
        view.findViewById(R.id.video_loop_one).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });

        timerViewIDList = new HashMap<>();
        timerViewIDList.put(0, R.id.video_setting_timer_none);
        timerViewIDList.put(30, R.id.video_setting_timer_30_min);
        timerViewIDList.put(60, R.id.video_setting_timer_60_min);
        view.findViewById(R.id.video_setting_timer_none).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });
        view.findViewById(R.id.video_setting_timer_30_min).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });
        view.findViewById(R.id.video_setting_timer_60_min).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });

        mTimerBtn = view.findViewById(R.id.video_setting_timer);
        mTimerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });

        mTimerView = view.findViewById(R.id.video_setting_timer_view);

        mSettingsView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // return true to block onSingleTapConfirmed
                return true;
            }
        });

        view.findViewById(R.id.video_setting_float_window).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });

        mSettingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });

        mBackIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });

        mStateIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });

        mScreenShotIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });
        mControllerLockIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });

        mCancelFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });

        mSwitchScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { onViewClick(view); }
        });

        mAudioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (mActivity != null) {
            int orientation = mActivity.getResources().getConfiguration().orientation;
            mIsLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
        }
        setSwitchScreenIcon(mIsLandscape);

        return view;
    }

    @Override
    public int getCoverLevel() {
        return levelMedium(1);
    }

    @Override
    public void onSingleTapConfirmed(MotionEvent event) {
        if(!mGestureEnable)
            return;
        toggleController();
    }

    @Override
    public void onLongPress(MotionEvent event) {

    }

    @Override
    public void onDoubleTap(MotionEvent event) {
    }

    @Override
    public void onDown(MotionEvent event) {
    }

    @Override
    public void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if(!mGestureEnable)
            return;

        mSettingsView.setVisibility(View.GONE);
        mTimerView.setVisibility(View.GONE);
    }

    @Override
    public void onEndGesture() {
    }

    private int getVolume(){
        int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume = Math.max(0, volume);
        return volume;
    }

    private float getBrightness() {
        float brightness = mActivity.getWindow().getAttributes().screenBrightness;
        if (brightness <= 0.00f){
            brightness = 0.5f;
        }else if (brightness < 0.01f){
            brightness = 0.01f;
        }
        return brightness;
    }
    private Activity getActivity(){
        Context context = getContext();
        if(context instanceof Activity){
            return (Activity) context;
        }
        return null;
    }

    private void selectVideoSpeed(float speed) {
        for (int id : speedViewIDList.values()) {
            View speedTextView = findViewById(id);
            speedTextView.setSelected(false);
        }
        int selectedID = speedViewIDList.get(speed);
        View selectedTextView = findViewById(selectedID);
        selectedTextView.setSelected(true);

        mPlaySpeed = speed;

        PlayerStateGetter playerStateGetter = getPlayerStateGetter();
        if (playerStateGetter != null) {
            int state = playerStateGetter.getState();
            if (state == IPlayer.STATE_STARTED) {
                Bundle bundle = new Bundle();
                bundle.putFloat(EventKey.INT_ARG1, speed);
                notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_SET_PLAY_SPEED, bundle);
            }
        }
    }

    private void selectVideoLoop(String loop_selection) {
        for (int id : loopViewIDList.values()) {
            View loopTextView = findViewById(id);
            loopTextView.setSelected(false);
        }

        int selectedID = loopViewIDList.get(loop_selection);
        View loopTextView = findViewById(selectedID);
        loopTextView.setSelected(true);

        mLoop = loop_selection;

        Bundle bundle = new Bundle();
        bundle.putString(EventKey.INT_ARG1, loop_selection);
        notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_SET_PLAY_LOOP, bundle);
    }

    private void selectVideoTimer(int min_selection) {
        for (int id : timerViewIDList.values()) {
            View loopTextView = findViewById(id);
            loopTextView.setSelected(false);
        }

        int selectedID = timerViewIDList.get(min_selection);
        View loopTextView = findViewById(selectedID);
        loopTextView.setSelected(true);

        mTimerBtn.setSelected(min_selection > 0);

        mTimerView.setVisibility(View.GONE);
    }

    private void setNewTimerTime(int new_min) {
        mTimerDurationMin = new_min;
        mTimerRemainMilli = new_min * 60 * 1000;

        selectVideoTimer(new_min);

        if (mTimerRemainMilli > 0) {
            PlayerStateGetter playerStateGetter = getPlayerStateGetter();
            if (playerStateGetter != null) {
                int state = playerStateGetter.getState();
                if (state == IPlayer.STATE_STARTED) {
                    stopVideoTimer();
                    startVideoTimer();
                }
            }
            setTimerBtnText(mTimerRemainMilli);
        } else {
            stopVideoTimer();
            mTimerBtn.setText(R.string.video_setting_timer);
        }
    }

    private void startVideoTimer() {
        if (mTimerRemainMilli > 0) {
            mTimerCountPreTime = System.currentTimeMillis();
            mVideoTimerHandler.postDelayed(timerRunnable, 0);
        }
    }

    private void stopVideoTimer() {
        mVideoTimerHandler.removeCallbacks(timerRunnable);
    }

    private void setTimerBtnText(long milli) {
        int seconds = (int) (milli / 1000);
        int minutes = Math.max(0, seconds / 60);
        seconds = Math.max(0, seconds % 60);

        String minutesStr = "" + minutes;
        if (minutes < 10) minutesStr = "0" + minutesStr;

        String secondsStr = "" + seconds;
        if (seconds < 10) secondsStr = "0" + secondsStr;

        String remainTime = minutesStr + ":" + secondsStr;
        mTimerBtn.setText(remainTime);
    }
}
