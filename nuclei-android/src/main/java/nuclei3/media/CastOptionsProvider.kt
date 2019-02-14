package nuclei3.media

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider


abstract class CastOptionsProvider : OptionsProvider {

    abstract val applicationId: String

    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
                .setReceiverApplicationId(applicationId)
                .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}