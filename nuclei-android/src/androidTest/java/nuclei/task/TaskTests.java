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
package nuclei.task;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.test.ApplicationTestCase;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskTests extends ApplicationTestCase<Application> {

    static final AtomicInteger count = new AtomicInteger();

    public TaskTests() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception {
        createApplication();
        ContextHandle.initialize(getApplication());
        Tasks.initialize(getApplication());
        super.setUp();
    }

    public void testChain() {
        final AtomicInteger result = new AtomicInteger();
        Tasks.execute(new TestChainTaskRoot())
                .continueWith(new TestChainTask())
                .continueWith(new TestChainTask())
                .addCallback(new Result.CallbackAdapter<Integer>() {
                    @Override
                    public void onResult(Integer count) {
                        assertTrue(Looper.getMainLooper() == Looper.myLooper());
                        synchronized (result) {
                            result.set(count);
                            result.notify();
                        }
                    }
                });
        synchronized (result) {
            try {
                result.wait(30000);
            } catch (InterruptedException ignore) {}
        }
        assertEquals(3, result.get());
    }

    public void testPool() {
        TaskPool pool = TaskPool.newBuilder("TestPool")
                .withThreads(2)
                .withLooper(Looper.getMainLooper())
                .build();
        final AtomicInteger tasks = new AtomicInteger(50);
        Result.Callback<String> callback = new Result.CallbackAdapter<String>() {
            @Override
            public void onResult(String type) {
                assertTrue(Looper.getMainLooper() == Looper.myLooper());
                tasks.decrementAndGet();
                synchronized (count) {
                    count.notify();
                }
            }
        };
        for (int i = 0; i < tasks.get(); i++)
            pool.execute(new TestTask()).addCallback(callback);
        assertEquals(0, count.get());
        while (tasks.get() > 0) {
            synchronized (count) {
                try {
                    count.wait(30000);
                } catch (InterruptedException ignore) {}
            }
        }
        assertEquals(0, tasks.get());
        pool.shutdown();
    }

    class TestTask extends Task<String> {

        @Override
        public String getId() {
            return "TestTaskId";
        }

        @Override
        public void run(Context context) {
            assertTrue(Looper.getMainLooper() != Looper.myLooper());
            assertEquals(1, count.incrementAndGet());
            try {
                synchronized (this) {
                    Thread.sleep(100);
                }
            } catch (Exception err) {}
            assertEquals(0, count.decrementAndGet());
            onComplete("done!");
        }

    }

    class TestChainTaskRoot extends ChainedTask<Integer, Integer> {

        @Override
        public String getId() {
            return "ChainedRootTaskId";
        }

        @Override
        public void run(Context context, Result<Integer> previousResult) {
            assertTrue(Looper.getMainLooper() != Looper.myLooper());
            onComplete(1);
        }

    }

    class TestChainTask extends ChainedTask<Integer, Integer> {

        @Override
        public String getId() {
            return "ChainedTaskId";
        }

        @Override
        public void run(Context context, Result<Integer> previousResult) {
            assertTrue(Looper.getMainLooper() != Looper.myLooper());
            Integer count = previousResult.get();
            onComplete(count + 1);
        }

    }

}