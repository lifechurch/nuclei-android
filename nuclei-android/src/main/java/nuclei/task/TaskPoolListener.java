package nuclei.task;

public interface TaskPoolListener {

    void onCreated(TaskPool pool);

    void onShutdown(TaskPool pool);

}
