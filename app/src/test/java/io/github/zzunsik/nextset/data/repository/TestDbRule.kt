package io.github.zzunsik.nextset.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

/**
 * 테스트마다 메모리 데이터베이스를 열고 닫는 규칙. 화면 테스트에서는 Compose 규칙보다 바깥(`@get:Rule(order = 0)`)에
 * 둔다. 그래야 화면과 화면 상태가 데이터베이스를 읽는 동안 닫히지 않는다(닫힌 뒤 읽으면 IllegalStateException).
 */
internal class TestDbRule(private val seed: Boolean = true) : ExternalResource() {
    lateinit var db: TestDb
        private set

    override fun before() {
        db = TestDb()
        if (seed) runBlocking { db.seed() }
    }

    override fun after() = db.close()
}
