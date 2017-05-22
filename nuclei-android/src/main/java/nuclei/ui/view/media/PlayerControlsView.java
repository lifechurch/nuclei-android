/**
 * Copyright 2016 YouVersion
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei.ui.view.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;

import io.nuclei.R;
import nuclei.media.MediaId;
import nuclei.media.MediaInterface;
import nuclei.media.MediaProvider;
import nuclei.media.ResourceProvider;
import nuclei.media.playback.PlaybackManager;
import nuclei.ui.util.ViewUtil;
import nuclei.ui.view.PopupMenu;

public class PlayerControlsView extends FrameLayout {

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

    MediaInterface mMediaInterface;
    long mTimer;
    boolean mAutoHide;

    public PlayerControlsView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public PlayerControlsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public PlayerControlsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public PlayerControlsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setClickable(true);
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.PlayerControlsView, defStyleAttr, defStyleRes);

        mAutoHide = a.getBoolean(R.styleable.PlayerControlsView_auto_hide, false);
        boolean bottom = a.getBoolean(R.styleable.PlayerControlsView_bottom, false);

        int layout = a.getResourceId(R.styleable.PlayerControlsView_control_layout,
                bottom
                ? R.layout.cyto_view_player_controls_bottom
                : R.layout.cyto_view_player_controls);

        boolean hasPrevious = a.getBoolean(R.styleable.PlayerControlsView_has_previous, true);
        boolean hasNext = a.getBoolean(R.styleable.PlayerControlsView_has_next, true);

        boolean hasRewind = a.getBoolean(R.styleable.PlayerControlsView_has_rewind, false);
        boolean hasFastforward = a.getBoolean(R.styleable.PlayerControlsView_has_fastforward, false);

        a.recycle();

        View view = LayoutInflater.from(context).inflate(layout, this, false);
        addView(view);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaInterface != null) {
                    if (v.getId() == R.id.btn_play) {
                        if (mMediaInterface.getPlayerController() != null) {
                            v.setActivated(!v.isActivated());
                            if (v.isActivated())
                                mMediaInterface.getPlayerController().start();
                            else
                                mMediaInterface.getPlayerController().pause();
                        }
                    } else if (v.getId() == R.id.btn_speed) {
                        onSpeedSelected(v);
                    } else if (v.getId() == R.id.btn_timer) {
                        onTimerSelected(v);
                    } else if (mMediaInterface.getMediaController() != null) {
                        if (mMediaInterface.getPlayerController() != null) {
                            if (v.getId() == R.id.btn_previous)
                                mMediaInterface.getPlayerController().skipToPrevious();
                            else if (v.getId() == R.id.btn_next)
                                mMediaInterface.getPlayerController().skipToNext();
                            else if (v.getId() == R.id.btn_rewind)
                                mMediaInterface.getPlayerController().rewind();
                            else if (v.getId() == R.id.btn_fastforward)
                                mMediaInterface.getPlayerController().fastForward();
                        }
                    }
                }
            }
        };

        view.findViewById(R.id.btn_play).setOnClickListener(listener);

        ImageView previous = (ImageView) view.findViewById(R.id.btn_previous);
        ImageView next = (ImageView) view.findViewById(R.id.btn_next);
        ImageView rewind = (ImageView) view.findViewById(R.id.btn_rewind);
        ImageView fastforward = (ImageView) view.findViewById(R.id.btn_fastforward);

        if (previous != null) {
            previous.setOnClickListener(listener);
            previous.setVisibility(hasPrevious ? VISIBLE : GONE);
        }

        if (next != null) {
            next.setOnClickListener(listener);
            next.setVisibility(hasNext ? VISIBLE : GONE);
        }

        if (rewind != null) {
            rewind.setOnClickListener(listener);
            rewind.setVisibility(hasRewind ? VISIBLE : GONE);
        }

        if (fastforward != null) {
            fastforward.setOnClickListener(listener);
            fastforward.setVisibility(hasFastforward ? VISIBLE : GONE);
        }

        DefaultCallback.onHandleState(this, fastforward, rewind, next, previous, null);

        TextView speed = ((TextView) view.findViewById(R.id.btn_speed));
        if (speed != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                speed.setText(ResourceProvider.getInstance().getSelectedSpeed());
                speed.setOnClickListener(listener);
                speed.setVisibility(View.VISIBLE);
            } else {
                speed.setVisibility(View.GONE);
            }
        }

        TextView timer = (TextView) view.findViewById(R.id.btn_timer);
        if (timer != null) {
            timer.setText(ResourceProvider.getInstance().getString(ResourceProvider.TIMER));
            timer.setOnClickListener(listener);
        }
    }

    protected void onSpeedSelected(View v) {
        final String selected = ResourceProvider.getInstance().getSelectedSpeed();
        final PopupMenu menu = new PopupMenu(v.getContext(), v, ResourceProvider.getInstance().getString(ResourceProvider.SPEED));
        menu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String text = (String) parent.getAdapter().getItem(position);
                if (text != null) {
                    final float speed = ResourceProvider.getInstance().getSpeed(text);
                    MediaProvider.getInstance().setAudioSpeed(speed);
                    if (mMediaInterface != null) {
                        mMediaInterface.setSpeed(speed);
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

    void onTimerSelected(View v) {
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
                    mMediaInterface.setTimer(timer);
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

    @Override
    public boolean isShown() {
        if (getChildCount() > 0)
            return getChildAt(0).getVisibility() == VISIBLE;
        return super.isShown();
    }

    public boolean isAutoHide() {
        return mAutoHide;
    }

    public void setAutoHide(boolean autoHide) {
        mAutoHide = autoHide;
        if (mAutoHide) {
            if (mMediaInterface != null)
                mMediaInterface.autoHide();
        } else {
            if (mMediaInterface != null)
                mMediaInterface.cancelAutoHide();
        }
    }

    public void show() {
        if (getChildCount() > 0)
            getChildAt(0).setVisibility(VISIBLE);
        if (mMediaInterface != null && mAutoHide)
            mMediaInterface.autoHide();
    }

    public void hide() {
        if (getChildCount() > 0)
            getChildAt(0).setVisibility(GONE);
    }

    public long getTimer() {
        return mTimer;
    }

    public MediaInterface getMediaInterface() {
        return mMediaInterface;
    }

    public void setMediaInterface(MediaInterface mediaInterface) {
        mMediaInterface = mediaInterface;
    }

    public MediaInterface attachDefaultInterface(@NonNull FragmentActivity activity, @Nullable DefaultCallback.OnConnectedListener listener, @Nullable MediaId id) {
        mMediaInterface = new MediaInterface(activity, id, new DefaultCallback(this, listener));
        return mMediaInterface;
    }

}
