package book.dukc.ch03_basic;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import org.jetbrains.annotations.NotNull;

/**
 * 通过 Java 来看协程的本质，原来也是回调。
 *
 * @author Ztiany
 * Email ztiany3@gmail.com
 * Date 2020/9/29 14:18
 */
public class Java {

    public static void main(String... args) {
        Object o = ContinuationKt.notSuspend(new Continuation<Integer>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return null;
            }

            @Override
            public void resumeWith(@NotNull Object o) {

            }
        });
    }
}
