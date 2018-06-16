/*
 * Copyright (C) 2012-2016 Markus Junginger, greenrobot (http://greenrobot.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.novate.architect_day22;


import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Posts events in background.
 * 
 * @author Markus
 */
class AsyncPoster implements Runnable {
    Subscription subscription;
    Object event;

    // 线程池
    private final static ExecutorService executorService = Executors.newCachedThreadPool();

    public AsyncPoster(Subscription subscription, Object event){
        this.subscription = subscription;
        this.event = event;
    }

    public static void enqueue(Subscription subscription, Object event) {
        AsyncPoster asyncPoster = new AsyncPoster(subscription,event);
        // 用线程池
        executorService.execute(asyncPoster);
    }

    @Override
    public void run() {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber,event);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
