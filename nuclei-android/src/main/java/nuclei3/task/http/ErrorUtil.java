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
package nuclei3.task.http;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLException;

import nuclei3.task.TimeoutException;

public final class ErrorUtil {

    public static final int TIMEOUT = 100;
    public static final int NO_CONNECTION = 200;
    public static final int AUTHENTICATION = 300;
    public static final int GENERIC_IO = 400;
    public static final int GENERIC = 500;

    private ErrorUtil() {
    }

    public static int getErrorType(Exception err) {
        Throwable t = err;
        int error = getErrorType(t);
        if (error != GENERIC && error != GENERIC_IO)
            return error;
        int depth = 10;
        do {
            if (t != null) {
                error = getErrorType(t);
                if (error != GENERIC && error != GENERIC_IO)
                    return error;
                t = t.getCause();
            }
        } while (t != null && depth-- > 0);
        if (error != GENERIC)
            return error;
        return GENERIC;
    }

    private static int getErrorType(Throwable err) {
        if (err instanceof HttpException) {
            HttpException h = (HttpException) err;
            if (h.getHttpCode() == 403 || h.getHttpCode() == 401) {
                return AUTHENTICATION;
            }
        }
        if (err instanceof TimeoutException
                || err instanceof java.util.concurrent.TimeoutException
                || err instanceof SocketTimeoutException) {
            return TIMEOUT;
        }
        if (err instanceof SocketException
                || err instanceof SSLException
                || err instanceof java.net.UnknownHostException)
            return NO_CONNECTION;
        if (err instanceof IOException)
            return GENERIC_IO;
        return GENERIC;
    }

}
