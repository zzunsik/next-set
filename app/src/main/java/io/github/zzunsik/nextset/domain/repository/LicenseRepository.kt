package io.github.zzunsik.nextset.domain.repository

import io.github.zzunsik.nextset.domain.model.OpenSourceNotice

/** 앱에 넣은 라이선스 전문을 읽는다(SET-03). 네트워크를 쓰지 않는다(DATA-02). */
interface LicenseRepository {
    /** 화면에 보일 순서대로. */
    suspend fun notices(): List<OpenSourceNotice>

    /** [id]의 라이선스 전문. 없는 id면 [NoSuchElementException]. */
    suspend fun licenseText(id: String): String
}
