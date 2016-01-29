package com.redmart.redmartandroidassignment;

import android.os.Handler;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by archie on 27/1/16.
 */
public class TaskExecutor implements Executor {

    public int defaultTimeout = 60000;
    private Thread waitingThread = null;

    private boolean stopped = false;
    private BlockingQueue<Task> workQueue;
    private List<Thread> taskThreads;

    public interface Task extends Runnable {
        public boolean isDone();
        public void stop();
    }

    public interface CompleteResult {
        public void onThreadPoolComplete();
        public void onThreadPoolTimeout();
    }


    public TaskExecutor() {
        workQueue = new LinkedBlockingQueue<>();
        taskThreads = new LinkedList<>();
    }


    public void setResultTimeout(int minutes) {
        defaultTimeout = minutes * 60000;
    }

    @Override
    public void execute(final Runnable task) {
        if(task instanceof Task)
            execute((Task)task);
    }

    public void execute(final Task task) {
        if(waitingThread != null)
            return;

        stopped = false;
        workQueue.add(task);
        Thread new_task = new Thread(new Runnable() {
            @Override
            public void run() {
                task.run();
            }
        });
        taskThreads.add(new_task);
        new_task.start();
    }

    public void shutdown() {
        stopped = true;
        waitForFinished(null);
    }

    public void waitForFinished(final CompleteResult result) {
        final Handler handler = new Handler();
        if(workQueue.size() > 0 &&  waitingThread == null) {
            waitingThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    long start_time = System.currentTimeMillis();
                    boolean timeout = false;
                    Task task = null;
                    do {
                        task = workQueue.poll();
                        if(task != null)
                            while (!task.isDone()) {
                                long execution_time = System.currentTimeMillis() - start_time;
                                if(stopped)
                                {
                                    task.stop();
                                    break;
                                }
                                else if (execution_time > defaultTimeout) {
                                    timeout = true;
                                    task.stop();
                                    break;
                                }
                            }
                    } while(task != null);

                    if(stopped) {
                        clear();
                        return;
                    }

                    if(timeout)
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(result != null)
                                    result.onThreadPoolTimeout();
                            }
                        });
                    else
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(result != null)
                                    result.onThreadPoolComplete();
                            }
                        });
                    clear();
                }
            });
            waitingThread.start();
        }
    }


    protected void clear() {
        Iterator<Thread> currentTasks = taskThreads.iterator();
        while(currentTasks.hasNext())
            currentTasks.next().interrupt();

        taskThreads.clear();

        if(waitingThread != null) {
            waitingThread.interrupt();
            waitingThread = null;
        }
    }

}
