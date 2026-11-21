package io.github.zzunsik.nextset.ui.navigation

import androidx.navigation3.runtime.NavKey

// 화면 이동은 모두 이 함수들로 한다(#103). 화면이 뒤로 가기 스택을 직접 add·remove하지 않는다(BackStackMovesTest가 본다).
//
// 버튼을 빠르게 두 번 누르면 두 번째 누르기가 첫 번째 누르기의 화면 이동이 끝나기 전에 온다. 화면 전환 동안에는 앞 화면이 아직 그려져
// 있어 그 버튼도 눌린다. 그래서 이동을 부른 화면([from])이 스택의 맨 위일 때만 이동한다. 첫 번째 누르기로 다른 화면이 위에 올라간 뒤에
// 온 누르기는 아무것도 하지 않는다. 시간으로 거르지 않으므로 느린 기기에서도, 빠르게 연달아 다른 화면으로 가는 경우에도 같다.

/** [from] 화면이 맨 위에 있을 때만 [to]를 연다. 열었으면 true. */
fun MutableList<NavKey>.navigate(from: NavKey, to: NavKey): Boolean {
    if (lastOrNull() != from || to == from) return false
    add(to)
    return true
}

/** [key] 화면이 맨 위에 있을 때만 닫는다. 닫았으면 true. 두 번 불려도 한 화면만 닫힌다. */
fun MutableList<NavKey>.close(key: NavKey): Boolean {
    if (lastOrNull() != key) return false
    removeAt(lastIndex)
    return true
}

/** [key] 화면이 맨 위에 있을 때만 [to]로 바꾼다. 바꿨으면 true. */
fun MutableList<NavKey>.replace(key: NavKey, to: NavKey): Boolean {
    if (!close(key)) return false
    add(to)
    return true
}
