package nuclei.task.http;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLException;

import nuclei.task.TimeoutException;

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
        if (error != GENERIC)
            return error;
        int depth = 10;
        do {
            if (t != null) {
                error = getErrorType(t);
                if (error != GENERIC)
                    return error;
                t = t.getCause();
            }
        } while (t != null && depth-- > 0);
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
