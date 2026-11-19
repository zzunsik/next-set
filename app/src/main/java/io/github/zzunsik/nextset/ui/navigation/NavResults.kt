package io.github.zzunsik.nextset.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberUpdatedState

/**
 * 뒤 화면이 앞 화면에 값을 돌려주는 곳. 종목 고르기가 고른 종목을 부른 화면(루틴 편집, 운동 중)에 돌려줄 때 쓴다.
 *
 * Navigation 3는 화면 사이에 값을 돌려주는 API를 두지 않는다. 그래서 뒤로 가기 스택을 쥔 곳에서 이 객체를 하나 만들어
 * 화면들에 넘긴다. 뒤 화면은 [put]으로 값을 놓고 스택에서 빠지며, 앞 화면은 다시 그려질 때 [ResultEffect]로 값을 한 번 꺼낸다.
 *
 * 값은 [ResultKey]의 이름으로 구분한다. 부르는 화면이 이름을 정해 뒤 화면의 키에 넘기므로
 * 같은 화면을 여러 곳에서 불러도 결과가 섞이지 않는다.
 *
 * 값은 메모리에만 둔다. 값을 놓은 순간과 앞 화면이 꺼내는 순간 사이에 프로세스가 죽으면 값을 잃는다.
 */
@Stable
class NavResults {
    private val values = mutableStateMapOf<String, Any>()

    fun <T : Any> put(key: ResultKey<T>, value: T) {
        values[key.name] = value
    }

    /** 놓인 값을 꺼낸다. 꺼낸 값은 지운다. */
    fun <T : Any> take(key: ResultKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return values.remove(key.name) as T?
    }

    /** 값이 놓였는지. Compose 상태라서 값이 놓이면 읽은 곳이 다시 그려진다. */
    fun has(key: ResultKey<*>): Boolean = values.containsKey(key.name)
}

/** 돌려받을 값의 이름과 타입. */
data class ResultKey<T : Any>(val name: String)

/** 이 화면 앞으로 [key]의 값이 돌아오면 [onResult]를 한 번 부른다. */
@Composable
fun <T : Any> NavResults.ResultEffect(key: ResultKey<T>, onResult: (T) -> Unit) {
    val currentOnResult = rememberUpdatedState(onResult)
    val arrived = has(key)
    LaunchedEffect(key, arrived) {
        if (arrived) take(key)?.let { currentOnResult.value(it) }
    }
}
