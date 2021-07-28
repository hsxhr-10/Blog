package com.example.springbootstaging;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

class User {

    private String name;
    private int age;
    private String sex;
    public String nickname;

    public User(String name, int age, String sex, String nickname) {
        this.name = name;
        this.age = age;
        this.sex = sex;
        this.nickname = nickname;
    }

    private void dance(String flag) {
        System.out.println(flag + " private dance");
    }

    public void printUser() {
        StringBuilder sb = new StringBuilder()
                .append("姓名: ")
                .append(name)
                .append("\n")
                .append("年龄: ")
                .append(age)
                .append("\n")
                .append("性别: ")
                .append(sex)
                .append("\n")
                .append("别名: ")
                .append(nickname);
        System.out.println(sb);
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@interface CarInfo {
    String name() default "Benz";

    String color() default "black";

    double price() default 600000d;

    String producer() default "China";
}

class CarInfoUtil {
    public static void getCarInfo(Class<?> cls) {
        // 使用反射获取所有自定义属性
        Field[] fields = cls.getDeclaredFields();

        for (Field field : fields) {
            // 如果这个属性被 CarInfo 注解标记
            if (field.isAnnotationPresent(CarInfo.class)) {
                // 使用反射获取注解信息
                CarInfo carInfo = (CarInfo) field.getAnnotation(CarInfo.class);
                String name = carInfo.name();
                String color = carInfo.color();
                double price = carInfo.price();
                String producer = carInfo.producer();

                // 输出注解信息
                System.out.println("汽车名称: " + name);
                System.out.println("汽车颜色: " + color);
                System.out.println("汽车价格: " + price);
                System.out.println("汽车生产商: " + producer);
            }
        }
    }
}

class MonitorThread extends Thread {
    public void run() {
        ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
        long[] ids = tmx.findDeadlockedThreads();
        if (ids != null) {
            ThreadInfo[] infos = tmx.getThreadInfo(ids, true, true);
            System.out.println("The following threads are deadlocked:");
            for (ThreadInfo ti : infos) {
                System.out.println(ti);
            }
        }
    }
}

class Consumer implements Runnable {

    private List<String> list;
    private Lock lock;
    private Condition cond;

    public Consumer(List<String> list, Lock lock, Condition cond) {
        this.list = list;
        this.lock = lock;
        this.cond = cond;
    }

    @Override
    public void run() {
        lock.lock();
        try {
            if (list.size() == 0) {
                try {
                    System.out.println("Consumer: list 为空, 等待 Producer 生产");
                    cond.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (list.size() > 0) {
                System.out.println("Consumer: list 不为空, 进行消费: " + list.get(0));
                list.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}

class Producer implements Runnable {

    private List<String> list;
    private Lock lock;
    private Condition cond;

    public Producer(List<String> list, Lock lock, Condition cond) {
        this.list = list;
        this.lock = lock;
        this.cond = cond;
    }

    @Override
    public void run() {
        lock.lock();
        try {
            if (list.size() == 0) {
                System.out.println("Producer: list 为空, 生产中...");
                list.add("香烟");
                System.out.println("Producer: 生产完毕, 通知 Consumer 消费");
                cond.signalAll();
            } else {
                System.out.println("Producer: list 不为空, 无需生产");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}

class ReentrantReadWriteLockDemo {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Queue<String> queue;

    public ReentrantReadWriteLockDemo(Queue<String> queue) {
        this.queue = queue;
    }

    public void put(String item) {
        lock.writeLock().lock();
        try {
            queue.add(item);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void pop() {
        lock.readLock().lock();
        try {
            String item = queue.poll();
            System.out.println("当前头部元素: " + item);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }
}

public class MixTests {

    @CarInfo(name = "BMW", color = "red", price = 656565d, producer = "japan")
    private String myCar1;

    @CarInfo
    private String myCar2;

    @Test
    void Case1() {
        List<String> list = new ArrayList<>();
        Lock lock = new ReentrantLock();
        Condition cond = lock.newCondition();

        Consumer consumer = new Consumer(list, lock, cond);
        new Thread(consumer).start();
        Producer producer = new Producer(list, lock, cond);
        new Thread(producer).start();
    }
}
