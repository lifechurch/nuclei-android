package nuclei3.task;

import android.arch.lifecycle.LiveData;

public class ResultLiveData<T> extends LiveData<T> {

    @Override
    protected void postValue(T value) {
        super.postValue(value);
    }

    @Override
    protected void setValue(T value) {
        super.setValue(value);
    }
}
