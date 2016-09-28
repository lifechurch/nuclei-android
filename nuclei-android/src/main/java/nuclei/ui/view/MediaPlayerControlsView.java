package nuclei.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import io.nuclei.R;
import nuclei.media.MediaId;
import nuclei.media.MediaInterface;
import nuclei.media.MediaPlayerController;
import nuclei.media.MediaProvider;
import nuclei.media.MediaService;
import nuclei.media.ResourceProvider;
import nuclei.media.playback.PlaybackManager;
import nuclei.ui.util.ViewUtil;

public class MediaPlayerControlsView extends FrameLayout {

    private static final int POS_OFF = 1;
    private static final int POS_FIVE_MIN = 2;
    private static final int POS_TEN_MIN = 3;
    private static final int POS_FIFTEEN_MIN = 4;
    private static final int POS_THIRTY_MIN = 5;
    private static final int POS_ONE_HOUR = 6;
    private static final int POS_TWO_HOUR = 7;

    private static final int ONE = 1;
    private static final int TWO = 2;

    private static final int FIVE = 5;
    private static final int TEN = 10;
    private static final int FIFTEEN = 15;
    private static final int THIRTY = 30;

    private MediaInterface mMediaInterface;
    private long mTimer;
    private boolean mAutoShow;

    public MediaPlayerControlsView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public MediaPlayerControlsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public MediaPlayerControlsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public MediaPlayerControlsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MediaPlayerControlsView, defStyleAttr, defStyleRes);

        mAutoShow = a.getBoolean(R.styleable.MediaPlayerControlsView_auto_show, false);
        boolean bottom = a.getBoolean(R.styleable.MediaPlayerControlsView_bottom, false);

        int layout = a.getResourceId(R.styleable.MediaPlayerControlsView_control_layout,
                bottom
                ? R.layout.cyto_view_player_controls_bottom
                : R.layout.cyto_view_player_controls);

        boolean hasPrevious = a.getBoolean(R.styleable.MediaPlayerControlsView_has_previous, true);
        boolean hasNext = a.getBoolean(R.styleable.MediaPlayerControlsView_has_next, true);

        a.recycle();

        View view = LayoutInflater.from(context).inflate(layout, this, false);
        addView(view);
        view.setVisibility(GONE);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaInterface != null) {
                    if (v.getId() == R.id.btn_play) {
                        v.setActivated(!v.isActivated());
                        if (v.isActivated())
                            mMediaInterface.getPlayerController().start();
                        else
                            mMediaInterface.getPlayerController().pause();
                    } else if (v.getId() == R.id.btn_previous)
                        mMediaInterface.getMediaController().getTransportControls().skipToPrevious();
                    else if (v.getId() == R.id.btn_next)
                        mMediaInterface.getMediaController().getTransportControls().skipToNext();
                    else if (v.getId() == R.id.btn_speed)
                        onSpeedSelected(v);
                    else if (v.getId() == R.id.btn_timer)
                        onTimerSelected(v);
                }
            }
        };
        ImageView previous = (ImageView) findViewById(R.id.btn_previous);
        ImageView next = (ImageView) findViewById(R.id.btn_next);

        previous.setOnClickListener(listener);
        previous.setVisibility(hasPrevious ? VISIBLE : GONE);
        next.setOnClickListener(listener);
        next.setVisibility(hasNext ? VISIBLE : GONE);

        findViewById(R.id.btn_play).setOnClickListener(listener);

        onHandleState(next, previous, mMediaInterface == null || mMediaInterface.getMediaController() == null
                                      ? null
                                      : mMediaInterface.getMediaController().getPlaybackState());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TextView speed = ((TextView) findViewById(R.id.btn_speed));
            speed.setText(ResourceProvider.getInstance().getSelectedSpeed());
            speed.setOnClickListener(listener);
            speed.setVisibility(View.VISIBLE);
            speed.setText(ResourceProvider.getInstance().getSelectedSpeed());
        }

        TextView timer = (TextView) findViewById(R.id.btn_timer);
        timer.setText(ResourceProvider.getInstance().getString(ResourceProvider.TIMER));
        timer.setOnClickListener(listener);
    }

    protected void onSpeedSelected(View v) {
        final String selected = ResourceProvider.getInstance().getSelectedSpeed();
        final PopupMenu menu = new PopupMenu(v.getContext(), v, ResourceProvider.getInstance().getString(ResourceProvider.SPEED));
        menu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String text = (String) parent.getAdapter().getItem(position);
                if (text != null) {
                    float speed = ResourceProvider.getInstance().getSpeed(text);
                    MediaProvider.getInstance().setAudioSpeed(speed);
                    if (mMediaInterface != null) {
                        Bundle args = new Bundle();
                        args.putFloat(MediaService.EXTRA_SPEED, speed);
                        mMediaInterface.getMediaController()
                                .getTransportControls()
                                .sendCustomAction(MediaService.ACTION_SET_SPEED, args);
                    }
                }
                menu.dismiss();
                menu.setOnItemClickListener(null);
            }
        });
        menu.setAdapter(new ArrayAdapter<String>(v.getContext(),
                R.layout.cyto_view_dropdown_item_checkable, android.R.id.text1,
                ResourceProvider.getInstance().getSpeeds()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView v = (TextView) view.findViewById(android.R.id.text1);
                CharSequence text = getItem(position);
                if (selected.equals(text)) {
                    v.setTextColor(ViewUtil.getThemeAttrColor(v.getContext(), R.attr.colorAccent));
                    view.findViewById(R.id.checked).setVisibility(View.VISIBLE);
                } else {
                    v.setTextColor(ViewUtil.getThemeAttrColor(v.getContext(), android.R.attr.textColorPrimary));
                    view.findViewById(R.id.checked).setVisibility(View.GONE);
                }
                return view;
            }
        });
        menu.show();
    }

    private void onTimerSelected(View v) {
        final CharSequence off = ResourceProvider.getInstance().getString(ResourceProvider.OFF);
        final PopupMenu menu = new PopupMenu(v.getContext(), v, ResourceProvider.getInstance().getString(ResourceProvider.TIMER));
        menu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    return;
                long timer = -1;
                switch (position) {
                    case POS_OFF:
                        ((TextView) findViewById(R.id.btn_timer)).setText(off);
                        break;
                    case POS_FIVE_MIN:
                        timer = PlaybackManager.FIVE_MINUTES;
                        break;
                    case POS_TEN_MIN:
                        timer = PlaybackManager.TEN_MINUTES;
                        break;
                    case POS_FIFTEEN_MIN:
                        timer = PlaybackManager.FIFTEEN_MINUTES;
                        break;
                    case POS_THIRTY_MIN:
                        timer = PlaybackManager.THIRY_MINUTES;
                        break;
                    case POS_ONE_HOUR:
                        timer = PlaybackManager.ONE_HOUR;
                        break;
                    case POS_TWO_HOUR:
                        timer = PlaybackManager.TWO_HOUR;
                        break;
                    default:
                        break;
                }
                if (mMediaInterface != null) {
                    Bundle args = new Bundle();
                    args.putLong(MediaService.EXTRA_TIMER, timer);
                    mMediaInterface.getMediaController()
                            .getTransportControls().sendCustomAction(MediaService.ACTION_SET_TIMER, args);
                }
                menu.dismiss();
                menu.setOnItemClickListener(null);
            }
        });
        menu.setAdapter(new ArrayAdapter<CharSequence>(v.getContext(),
                R.layout.cyto_view_dropdown_item_checkable, android.R.id.text1,
                Arrays.asList(
                        off,
                        ResourceProvider.getInstance().getQuantityString(ResourceProvider.MINUTES, FIVE),
                        ResourceProvider.getInstance().getQuantityString(ResourceProvider.MINUTES, TEN),
                        ResourceProvider.getInstance().getQuantityString(ResourceProvider.MINUTES, FIFTEEN),
                        ResourceProvider.getInstance().getQuantityString(ResourceProvider.MINUTES, THIRTY),
                        ResourceProvider.getInstance().getQuantityString(ResourceProvider.HOURS, ONE),
                        ResourceProvider.getInstance().getQuantityString(ResourceProvider.HOURS, TWO))) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView v = (TextView) view.findViewById(android.R.id.text1);
                CharSequence text = getItem(position);
                if (mTimer == -1 && off.equals(text)) {
                    v.setTextColor(ViewUtil.getThemeAttrColor(v.getContext(), R.attr.colorAccent));
                    view.findViewById(R.id.checked).setVisibility(View.VISIBLE);
                } else {
                    v.setTextColor(ViewUtil.getThemeAttrColor(v.getContext(), android.R.attr.textColorPrimary));
                    view.findViewById(R.id.checked).setVisibility(View.GONE);
                }
                return view;
            }
        });
        menu.show();
    }

    public boolean isShown() {
        return getChildAt(0).getVisibility() == VISIBLE;
    }

    public void show() {
        getChildAt(0).setVisibility(VISIBLE);
    }

    public void hide() {
        getChildAt(0).setVisibility(GONE);
    }

    public MediaInterface newMediaInterface(FragmentActivity appCompatActivity, MediaId id) {
        mMediaInterface = new MediaInterface(appCompatActivity, id, newMediaInterfaceCallback(null));
        return mMediaInterface;
    }

    public MediaInterface newMediaInterface(FragmentActivity appCompatActivity, OnConnectedListener listener) {
        mMediaInterface = new MediaInterface(appCompatActivity, null, newMediaInterfaceCallback(listener));
        return mMediaInterface;
    }

    public MediaInterface newMediaInterface(FragmentActivity appCompatActivity) {
        mMediaInterface = new MediaInterface(appCompatActivity, null, newMediaInterfaceCallback(null));
        return mMediaInterface;
    }

    private void onHandleState(ImageView next, ImageView previous, PlaybackStateCompat state) {
        int enabled = ViewUtil.getThemeAttrColor(getContext(), android.R.attr.textColorPrimary);
        int disabled = ViewUtil.getThemeAttrColor(getContext(), android.R.attr.textColorSecondary);
        next.setColorFilter(state != null && (state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                == PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            ? enabled
                            : disabled, PorterDuff.Mode.SRC_ATOP);
        previous.setColorFilter(state != null && (state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                == PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                ? enabled
                                : disabled, PorterDuff.Mode.SRC_ATOP);
    }

    public MediaInterface.MediaInterfaceCallback newMediaInterfaceCallback(final OnConnectedListener listener) {
        return new DefaultCallback(this, listener, mAutoShow);
    }

    public void setMediaInterface(MediaInterface mediaInterface) {
        mMediaInterface = mediaInterface;
    }

    public interface OnConnectedListener {
        void onConnected(MediaPlayerControlsView view, MediaInterface mediaInterface);
    }

    public static class DefaultCallback implements MediaInterface.MediaInterfaceCallback {

        private static final int ONE_MINUTE = 60;
        private static final int ONE_HOUR = 3600;

        private final StringBuilder mFormatBuilder = new StringBuilder();
        private final Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        private final ImageView play;
        private final View loading;
        private final TextView timePlayed;
        private final TextView timeTotal;
        private final TextView speed;
        private final TextView timer;
        private final SeekBar seekBar;
        private final ImageView next;
        private final ImageView previous;

        private MediaPlayerControlsView mView;
        private OnConnectedListener mConnectedListener;
        private boolean mAutoShow;

        public DefaultCallback(MediaPlayerControlsView view, OnConnectedListener listener, boolean autoShow) {
            mView = view;
            mAutoShow = autoShow;
            mConnectedListener = listener;
            play = (ImageView) view.findViewById(R.id.btn_play);
            loading = view.findViewById(R.id.media_loading);
            timePlayed = (TextView) view.findViewById(R.id.time_played);
            timeTotal = (TextView) view.findViewById(R.id.time_total);
            speed = (TextView) view.findViewById(R.id.btn_speed);
            timer = (TextView) view.findViewById(R.id.btn_timer);
            seekBar = (SeekBar) view.findViewById(R.id.progress);
            next = (ImageView) view.findViewById(R.id.btn_next);
            previous = (ImageView) view.findViewById(R.id.btn_previous);
            if (seekBar != null) {
                seekBar.setOnSeekBarChangeListener(mSeekListener);
                seekBar.setMax(MediaInterface.ProgressHandler.MAX_PROGRESS);
            }
        }

        private MediaInterface.ProgressHandler mHandler;
        private boolean mDragging;
        private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                mDragging = true;
                mHandler.stop();
            }

            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
                if (!fromuser) {
                    return;
                }
                if (mView.mMediaInterface == null)
                    return;
                MediaInterface mediaInterface = mView.mMediaInterface;
                MediaPlayerController controller = mediaInterface.getPlayerController();
                long duration = controller.getDuration();
                long newPosition = (duration * progress) / PlaybackManager.ONE_SECOND;
                controller.seekTo((int) newPosition);
                setPosition(mediaInterface, MediaInterface.ProgressHandler.MAX_PROGRESS, newPosition, -1);
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                mDragging = false;
                if (mHandler != null)
                    mHandler.start();
            }

        };

        @Override
        public void onConnected(nuclei.media.MediaInterface mediaInterface) {
            if (mConnectedListener != null) {
                if (mView == null || mView.mMediaInterface == null)
                    return;
                mConnectedListener.onConnected(mView, mediaInterface);
            }
            if (mHandler == null)
                mHandler = new MediaInterface.ProgressHandler(mediaInterface);
            mHandler.start();
        }

        @Override
        public void onLoading(MediaPlayerController controller) {
            loading.setVisibility(View.VISIBLE);
            if (mHandler != null)
                mHandler.start();
        }

        @Override
        public void onLoaded(MediaPlayerController controller) {
            loading.setVisibility(View.GONE);
        }

        @Override
        public void onPlaying(MediaPlayerController controller) {
            play.setActivated(true);
            if (mHandler != null)
                mHandler.start();
            if (mAutoShow && mView != null) {
                mView.show();
            }
        }

        @Override
        public void onPaused(MediaPlayerController controller) {
            play.setActivated(false);
            if (mHandler != null)
                mHandler.stop();
            if (mAutoShow && mView != null) {
                mView.hide();
            }
        }

        @Override
        public void onTimerChanged(nuclei.media.MediaInterface mediaInterface, long t) {
            if (mView == null)
                return;
            mView.mTimer = t;
            if (timer != null) {
                if (t < 1)
                    timer.setText(ResourceProvider.getInstance().getString(ResourceProvider.TIMER));
                else
                    timer.setText(stringForTime(t));
            }
        }

        @Override
        public void onSpeedChanged(nuclei.media.MediaInterface mediaInterface, float s) {
            speed.setText(ResourceProvider.getInstance().getSelectedSpeed());
        }

        @Override
        public void onStateChanged(nuclei.media.MediaInterface mediaInterface, PlaybackStateCompat state) {
            if (mView == null)
                return;
            mView.onHandleState(next, previous, state);
        }

        @Override
        public void setTimePlayed(nuclei.media.MediaInterface mediaInterface, long played) {
            timePlayed.setText(stringForTime(played));
        }

        @Override
        public void setTimeTotal(nuclei.media.MediaInterface mediaInterface, long remaining) {
            timeTotal.setText(stringForTime(remaining));
        }

        @Override
        public boolean isPositionChanging(MediaInterface mediaInterface) {
            return mDragging;
        }

        @Override
        public void setPosition(nuclei.media.MediaInterface mediaInterface, long max, long position, long secondaryPosition) {
            if (seekBar != null) {
                seekBar.setProgress((int) position);
                seekBar.setSecondaryProgress((int) secondaryPosition);
            }
        }

        @Override
        public void onMetadataChanged(MediaInterface mediaInterface, MediaMetadataCompat mediaMetadataCompat) {

        }

        @Override
        public void onDestroy(MediaInterface mediaInterface) {
            mView = null;
            if (mHandler != null)
                mHandler.stop();
            mHandler = null;
        }

        private String stringForTime(long timeMs) {
            long totalSeconds = timeMs / PlaybackManager.ONE_SECOND;

            int seconds = (int) (totalSeconds % ONE_MINUTE);
            int minutes = (int) ((totalSeconds / ONE_MINUTE) % ONE_MINUTE);
            int hours = (int) (totalSeconds / ONE_HOUR);

            mFormatBuilder.setLength(0);
            if (hours > 0) {
                return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
            } else {
                return mFormatter.format("%02d:%02d", minutes, seconds).toString();
            }
        }
    }

}
