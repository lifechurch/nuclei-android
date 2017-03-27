package nuclei.persistence;

import android.net.Uri;

public class PersistenceUri {

    private Uri URI;
    private Uri BASE_URI;
    private String path;
    private String authority;

    public PersistenceUri(String authority, String path) {
        this.authority = authority;
        this.path = path;
    }

    public void setAuthority(ContentProviderBase.Token token, String authority) {
        if (token == null)
            throw new NullPointerException();
        this.authority = authority;
        URI = null;
        BASE_URI = null;
    }

    public Uri withAppending(long clientId) {
        return Uri.withAppendedPath(toBaseUri(), Long.toString(clientId));
    }

    public Uri toUri() {
        if (URI == null) {
            URI = Uri.parse("content://" + authority + path);
        }
        return URI;
    }

    public Uri toBaseUri() {
        if (BASE_URI == null) {
            BASE_URI = Uri.parse("content://" + authority + path + "/");
        }
        return BASE_URI;
    }

}
