package nuclei3.task;

public interface TaskListener {

    void onStart(Task<?> task);

    void onIntercepted(Task<?> oldTask, Task<?> newTask);

    void onDiscard(Task<?> task);

    void onFinish(Task<?> task);

}
