package io.github.zzunsik.nextset

import android.content.Context
import io.github.zzunsik.nextset.data.db.NextSetDatabase

/**
 * 앱이 쓰는 객체를 만드는 곳. 의존성 주입 라이브러리 없이 여기서 직접 만들어 넘긴다
 * (docs/architecture.md "의존성 주입").
 *
 * 저장소 구현은 #20에서 더한다. 화면 상태는 저장소 인터페이스만 받는다.
 */
class AppContainer(context: Context) {
    /** 처음 쓸 때 연다. 앱 시작 때 디스크를 건드리지 않는다. */
    val database: NextSetDatabase by lazy {
        NextSetDatabase.build(context.applicationContext)
    }
}
