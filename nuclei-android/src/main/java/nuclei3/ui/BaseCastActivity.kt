package nuclei3.ui

/*
 * Copyright (C) 2016 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.NetworkImageView
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.MediaUtils
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.libraries.cast.companionlibrary.utils.Utils

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnErrorListener
import android.media.MediaPlayer.OnPreparedListener
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import io.nuclei3.R

import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.VideoView
import nuclei3.media.MediaInterface

import java.util.Timer
import java.util.TimerTask

/**
 * Activity for the local media player.
 */
abstract class BaseCastActivity : NucleiActivity() {
    private var mVideoView: VideoView? = null
    private var mTitleView: TextView? = null
    private var mDescriptionView: TextView? = null
    private var mStartText: TextView? = null
    private var mEndText: TextView? = null
    private var mSeekbar: SeekBar? = null
    private var mPlayPause: ImageView? = null
    private var mLoading: ProgressBar? = null
    private var mControllers: View? = null
    private var mContainer: View? = null
    private var mCoverArt: NetworkImageView? = null
    private var mSeekbarTimer: Timer? = null
    private var mControllersTimer: Timer? = null
    private var mLocation: PlaybackLocation? = null
    private var mPlaybackState: PlaybackState? = null
    private val mHandler = Handler()
    private val mAspectRatio = 72f / 128
    private var mSelectedMedia: MediaInfo? = null
    private var mControllersVisible: Boolean = false
    private var mDuration: Int = 0
    private var mAuthorView: TextView? = null
    private var mPlayCircle: ImageButton? = null
    private var mCastContext: CastContext? = null
    private var mCastSession: CastSession? = null
    private var mSessionManagerListener: SessionManagerListener<CastSession>? = null
    private var mQueueMenuItem: MenuItem? = null
    private var mImageLoader: ImageLoader? = null

    abstract var mediaInterface: MediaInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCastContext = CastContext.getSharedInstance(this)
        mCastSession = mCastContext!!.sessionManager.currentCastSession

    }
}