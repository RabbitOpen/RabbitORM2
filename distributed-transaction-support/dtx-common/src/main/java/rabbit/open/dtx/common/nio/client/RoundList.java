package rabbit.open.dtx.common.nio.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 循环list
 * @author xiaoqianbin
 * @date 2020/1/9
 **/
public class RoundList<T> {

    private ReentrantLock reentrantLock = new ReentrantLock();

    private Condition emptyCondition = reentrantLock.newCondition();

    private List<T> list = new ArrayList<>();

    private int cursor = -1;

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    public void add(T t) {
        reentrantLock.lock();
        list.add(t);
        emptyCondition.signalAll();
        reentrantLock.unlock();
    }

    /**
     * 循环取出下一个节点, 没有就返回空
     * @author xiaoqianbin
     * @date 2020/1/9
     **/
    public T fetch(long timeoutMilliSeconds) {
        try {
            reentrantLock.lock();
            if (isEmpty()) {
                if (0 == timeoutMilliSeconds) {
                    return null;
                }
                long nanos = timeoutMilliSeconds * 1000_000;
                while (isEmpty()) {
                    nanos = emptyCondition.awaitNanos(nanos);
                    if (nanos <= 0) {
                        return null;
                    }
                }
            }
            cursor = getNextCursor(cursor);
            return list.get(cursor);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * 计算cursor
     * @author xiaoqianbin
     * @date 2020/1/9
     **/
    private int getNextCursor(int cursor) {
        if (cursor < list.size() - 1) {
            return cursor + 1;
        } else {
            return 0;
        }
    }


    /**
     * 循环取出下一个节点, 没有就返回空， cursor会移动
     * @author xiaoqianbin
     * @date 2020/1/9
     **/
    public T fetch() {
        return fetch(0L);
    }

    /**
     * 循环取出下一个节点, 没有就返回空， cursor不会移动
     * @author xiaoqianbin
     * @date 2020/1/9
     **/
    public T peekNext() {
        reentrantLock.lock();
        try {
            if (!list.isEmpty()) {
                return list.get(getNextCursor(cursor));
            } else {
                return null;
            }
        } finally {
            reentrantLock.unlock();
        }
    }


    /**
     * 删除元素
     * @author xiaoqianbin
     * @date 2020/1/9
     **/
    public boolean remove(T t) {
        reentrantLock.lock();
        boolean result = list.remove(t);
        reentrantLock.unlock();
        return result;
    }

}
