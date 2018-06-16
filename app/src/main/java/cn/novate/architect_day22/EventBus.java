package cn.novate.architect_day22;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Email: 2185134304@qq.com
 * Created by Novate 2018/6/10 11:08
 * Version 1.0
 * Params:
 * Description:
*/

public class EventBus {

    // subscriptionsByEventType 这个集合存放的是？
    // key 是 Event 参数的类
    // value 存放的是 Subscription 的集合列表
    // Subscription 包含两个属性，一个是 subscriber 订阅者（反射执行对象），一个是 SubscriberMethod 注解方法的所有属性参数值
    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
    // typesBySubscriber 这个集合存放的是？
    // key 是所有的订阅者
    // value 是所有订阅者里面方法的参数的class
    private final Map<Object, List<Class<?>>> typesBySubscriber;


    private EventBus(){
        typesBySubscriber = new HashMap<Object, List<Class<?>>>() ;
        subscriptionsByEventType = new HashMap<>() ;
    }
    static volatile EventBus defaultInstance;

    public static EventBus getDefault() {
        if (defaultInstance == null) {
            synchronized (EventBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }


    /**
     *
     * @param object：就是MAinActivity.this
     */
    public void register(Object object) {
        List<SubscriberMethod> subscriberMethods = new ArrayList<>() ;
        // 1. 解析所有方法，封装成 SubscriberMethod的集合
        // a：获取class文件对象
        Class<?> objClass = object.getClass();
        // b：获取MainActivty中所有的方法
        Method[] methods = objClass.getDeclaredMethods();
        // c：for循环
        for (Method method : methods) {
            // 通过 MAinActivity中的注解Subscribe来获取对应 Subscribe的方法，也就是test1()、test2()
            Subscribe subscribe = method.getAnnotation(Subscribe.class);
            if (subscribe != null){
                // 获取所有Subscribe属性，解析出来，这个表示test1()或者test2()方法中有几个参数，只能有1个参数，如果有多个，直接抛异常，
                // 这里就当成它只有1个参数，就直接取数组的第0个位置的元素即可
                Class<?>[] parameterTypes = method.getParameterTypes();

                SubscriberMethod subscriberMethod = new SubscriberMethod(method ,  // test1() 和 test2()方法
                        parameterTypes[0] ,    // 只是取test1()或者 test2()方法中的 第一个参数
                        subscribe.threadMode() ,   // 线程模式 主线程、子线程
                        subscribe.priority() ,  // 优先级
                        subscribe.sticky()) ;  // 是否是粘性事件

                // 有一个符合条件的，就给集合中存储一个对象
                subscriberMethods.add(subscriberMethod) ;
            }
        }
        // 2. 按照规则，存放到 subscriptionsByEventType集合 里面去，这个是map集合
        for (SubscriberMethod subscriberMethod : subscriberMethods) {
            // 注册
            subscriber(object , subscriberMethod) ;
        }
    }


    /**
     * 注册
     * @param object：MainActivity.this
     * @param subscriberMethod：MainActivity中符合含有注解Subscriber的方法，也就是test1()和test2()方法
     */
    private void subscriber(Object object, SubscriberMethod subscriberMethod) {
        Class<?> eventType = subscriberMethod.eventType;

        // 根据eventType键，获取对应的值
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions == null){
            // 线程安全的集合
            subscriptions = new CopyOnWriteArrayList<>() ;
            // 根据键值对存储数据到 map集合
            subscriptionsByEventType.put(eventType , subscriptions) ;
        }

        // 这里直接忽略判断优先级，这里直接添加
        Subscription subscription = new Subscription(object , subscriberMethod) ;
        // 把对象添加到集合中
        subscriptions.add(subscription) ;

        // typesBySubscriber要弄好，是为了方便移除
        List<Class<?>> eventTypes = typesBySubscriber.get(object);
        if (eventTypes == null){
            eventTypes = new ArrayList<>() ;
            typesBySubscriber.put(object , eventTypes) ;
        }

        if (!eventTypes.contains(eventType)){
            eventTypes.add(eventType) ;
        }
    }


    /**
     * 注销移除
     * @param object：MainActivity
     */
    public void unregister(Object object) {
        List<Class<?>> eventTypes = typesBySubscriber.get(object);
        if (eventTypes != null){
            for (Class<?> eventType : eventTypes) {
                removeObject(eventType , object) ;
            }
        }
    }


    /**
     * 移除
     */
    private void removeObject(Class<?> eventType, Object object) {
        // 一边for循环，一边移除是不行的
        // 获取事件类的所有订阅信息列表，将订阅信息从订阅信息集合中移除，同时将订阅信息中的active属性置为FALSE
        List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions != null) {
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                Subscription subscription = subscriptions.get(i);
                if (subscription.subscriber == object) {
                    // 将订阅信息从集合中移除
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }


    /**
     * TestActivity中需要发送数据的post()方法
     */
    public void post(Object event) {
        // 遍历subscriptionsByEventType的map集合，
        // 也就是遍历test1()和test()2这两个方法，
        // 找到符合的方法，然后调用方法的 method.invoke()执行
        // 要注意线程的切换

        // 获取class文件对象
        Class<?> eventType = event.getClass();
        // 找到符合的方法，然后调用方法的 method.invoke()执行
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions != null){
            for (Subscription subscription : subscriptions) {
                executeMethod(subscription , eventType) ;
            }
        }
    }


    /**
     * 执行
     */
    private void executeMethod(final Subscription subscription, final Class<?> event) {
        // 获取threadMode
        ThreadMode threadMode = subscription.subscriberMethod.threadMode;
        // 判断是否是主线程：一个线程只有一个 looper对象
        boolean isMainThread = Looper.getMainLooper() == Looper.myLooper();
        // 枚举
        switch (threadMode){
            // 如果发送的是在主线程，就在主线程，如果发送的是在子线程，就在子线程
            case POSTING:
                 invokeMethod(subscription , event) ;
                 break;
            // 主线程
            case MAIN:
                 if (isMainThread){
                     invokeMethod(subscription , event) ;
                 }else{
                     // 这里必须添加Looper.myLooper()，否则会报错，因为在主线程可以直接new Handler()，
                     // 在子线程如果new Handler()，就必须添加上 Looper.perpare()否则会报错
                     Handler handler = new Handler(Looper.myLooper()) ;
                     handler.post(new Runnable() {
                         @Override
                         public void run() {
                             invokeMethod(subscription , event) ;
                         }
                     }) ;
                 }
                 break;
            // 异步
            case ASYNC:
                AsyncPoster.enqueue(subscription , event);
                 break;
            case BACKGROUND:
                 if (!isMainThread){
                     invokeMethod(subscription , event) ;
                 }else{
                    AsyncPoster.enqueue(subscription , event);
                 }
                 break;
        }
    }



    private void invokeMethod(Subscription subscription , Class<?> event) {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber ,   // 对象
                    event) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
