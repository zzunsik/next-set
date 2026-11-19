package io.github.zzunsik.nextset.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Room은 엔티티가 하나도 없는 데이터베이스를 컴파일하지 않는다.
 * #20에서 실제 테이블을 더할 때 이 엔티티를 지운다. 첫 릴리스 전이라 버전 1을 고쳐 쓴다
 * (docs/architecture.md "스키마 버전 관리" 5번).
 */
@Entity(tableName = "schema_placeholder")
internal data class SchemaPlaceholder(
    @PrimaryKey val id: Long,
)
