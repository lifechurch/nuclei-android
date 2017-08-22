package nuclei3.task;

public interface TaskPoolListener {

    void onCreated(TaskPool pool);

    void onShutdown(TaskPool pool);

}
