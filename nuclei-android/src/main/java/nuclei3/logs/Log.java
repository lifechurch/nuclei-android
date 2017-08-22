/**
 * Copyright 2016 YouVersion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei3.logs;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Log {

    public static final int ASSERT = android.util.Log.ASSERT;
    public static final int DEBUG = android.util.Log.DEBUG;
    public static final int ERROR = android.util.Log.ERROR;
    public static final int INFO = android.util.Log.INFO;
    public static final int VERBOSE = android.util.Log.VERBOSE;
    public static final int WARN = android.util.Log.WARN;

    private static PrintWriter sExtraLog;
    private static SimpleDateFormat sDateFormat;

    private final String mTag;

    protected Log(String tag) {
        mTag = tag;
    }

    public boolean isLoggable(int priority) {
        return android.util.Log.isLoggable(mTag, priority);
    }

    public void v(String message) {
        if (android.util.Log.isLoggable(mTag, android.util.Log.VERBOSE)) {
            android.util.Log.v(mTag, message);
            if (Logs.EXTRA) {
                logExtra(android.util.Log.VERBOSE, mTag, message, null);
            }
        }
    }

    public void v(String message, Throwable err) {
        if (android.util.Log.isLoggable(mTag, android.util.Log.VERBOSE)) {
            android.util.Log.v(mTag, message, err);
            if (Logs.EXTRA) {
                logExtra(android.util.Log.VERBOSE, mTag, message, err);
            }
        }
    }

    public void v(String format, Object...args) {
        if (android.util.Log.isLoggable(mTag, android.util.Log.VERBOSE)) {
            String m = String.format(Locale.US, format, args);
            android.util.Log.v(mTag, m);
            if (Logs.EXTRA) {
                logExtra(android.util.Log.VERBOSE, mTag, m, null);
            }
        }
    }

    public void d(String format, Object...args) {
        if (android.util.Log.isLoggable(mTag, android.util.Log.DEBUG)) {
            String m = String.format(Locale.US, format, args);
            android.util.Log.d(mTag, m);
            if (Logs.EXTRA) {
                logExtra(android.util.Log.DEBUG, mTag, m, null);
            }
        }
    }

    public void d(String message, Throwable err) {
        if (android.util.Log.isLoggable(mTag, android.util.Log.DEBUG)) {
            android.util.Log.d(mTag, message, err);
            if (Logs.EXTRA) {
                logExtra(android.util.Log.DEBUG, mTag, message, err);
            }
        }
    }

    public void i(String message) {
        if (android.util.Log.isLoggable(mTag, android.util.Log.INFO)) {
            android.util.Log.i(mTag, message);
            if (Logs.EXTRA) {
                logExtra(android.util.Log.INFO, mTag, message, null);
            }
        }
    }

    public void i(String message, Throwable err) {
        if (android.util.Log.isLoggable(mTag, android.util.Log.INFO)) {
            android.util.Log.i(mTag, message, err);
            if (Logs.EXTRA) {
                logExtra(android.util.Log.INFO, mTag, message, err);
            }
        }
    }

    public void w(String message) {
        if (android.util.Log.isLoggable(mTag, android.util.Log.WARN)) {
            android.util.Log.w(mTag, message);
            if (Logs.EXTRA) {
                logExtra(android.util.Log.WARN, mTag, message, null);
            }
        }
    }

    public void w(String message, Throwable err) {
        if (android.util.Log.isLoggable(mTag, android.util.Log.WARN)) {
            android.util.Log.w(mTag, message, err);
            if (Logs.EXTRA) {
                logExtra(android.util.Log.WARN, mTag, message, err);
            }
        }
    }

    public void e(String message) {
        if (android.util.Log.isLoggable(mTag, android.util.Log.ERROR)) {
            android.util.Log.e(mTag, message);
            if (Logs.EXTRA) {
                logExtra(android.util.Log.ERROR, mTag, message, null);
            }
        }
    }

    public void e(String message, Throwable err) {
        if (android.util.Log.isLoggable(mTag, android.util.Log.ERROR)) {
            android.util.Log.e(mTag, message, err);
            if (Logs.EXTRA) {
                logExtra(android.util.Log.ERROR, mTag, message, err);
            }
        }
    }

    public void wtf(String message) {
        android.util.Log.wtf(mTag, message);
        if (android.util.Log.isLoggable(mTag, android.util.Log.ERROR) && Logs.EXTRA) {
            logExtra(android.util.Log.ERROR, mTag, message, null);
        }
    }

    public void wtf(String message, Throwable err) {
        android.util.Log.wtf(mTag, message, err);
        if (android.util.Log.isLoggable(mTag, android.util.Log.ERROR) && Logs.EXTRA) {
            logExtra(android.util.Log.ERROR, mTag, message, err);
        }
    }

    static synchronized void logExtra(int level, String tag, String message, Throwable err) {
        if (sExtraLog == null) {
            try {
                sExtraLog = new PrintWriter(new FileOutputStream(Logs.newLogFile()));
                sDateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm z", Locale.US);
            } catch (IOException e) {
                android.util.Log.wtf(tag, "Error creating extra log file", e);
            }
        }
        if (sExtraLog != null) {
            try {
                sExtraLog.write(sDateFormat.format(new Date()));
                sExtraLog.write(" - ");
                sExtraLog.write(tag);
                switch (level) {
                    case ASSERT:
                        sExtraLog.write("/A - ");
                        break;
                    case VERBOSE:
                        sExtraLog.write("/V - ");
                        break;
                    case DEBUG:
                        sExtraLog.write("/D - ");
                        break;
                    case INFO:
                        sExtraLog.write("/I - ");
                        break;
                    case WARN:
                        sExtraLog.write("/W - ");
                        break;
                    case ERROR:
                        sExtraLog.write("/E - ");
                        break;
                }
                sExtraLog.write(message);
                sExtraLog.write("\n");
                if (err != null) {
                    sExtraLog.write(android.util.Log.getStackTraceString(err));
                    sExtraLog.write("\n");
                }
                sExtraLog.flush();
            } catch (Exception e) {
                android.util.Log.wtf(tag, "Error writing extra log file", e);
            }
        }
    }

    public static void flush() throws IOException {
        if (sExtraLog != null) {
            sExtraLog.flush();
        }
    }

}
