package io.github.zzunsik.nextset.domain.model

/**
 * 오픈소스 라이선스 화면의 한 줄(SET-03). 앱에 넣은 글꼴, 아이콘, 라이브러리 하나다.
 *
 * @property id 목록 안에서 겹치지 않는 이름. 화면 이동 키에 쓴다.
 * @property name 글꼴이나 라이브러리의 이름.
 * @property author 만든 곳. 저작권 표시로 보인다.
 * @property licenseName 라이선스 이름(예: "Apache License 2.0").
 */
data class OpenSourceNotice(
    val id: String,
    val name: String,
    val author: String,
    val licenseName: String,
)
