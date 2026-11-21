package io.github.zzunsik.nextset.ui.navigation

import android.os.Build
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Android 8.x(API 26·27)에서 화면이 바뀔 때 입력 칸이 저절로 포커스를 잡아 키보드가 뜨지 않게 한다(#105).
 *
 * API 27까지는 포커스를 가진 뷰가 포커스를 놓으면 창이 맨 위 뷰(DecorView)부터 첫 번째 포커스 가능한 뷰를 찾아 포커스를 다시 준다
 * (`View.clearFocus`가 `rootViewRequestFocus`를 부른다. API 28부터는 터치 모드에서 다시 주지 않는다). 포커스를 가진 입력 칸이 화면 이동으로
 * 사라지면 Compose가 뷰의 포커스를 놓고, 창이 Compose 뷰에 포커스를 다시 주면 Compose는 그 화면의 첫 입력 칸에 포커스를 넣는다.
 * 그래서 루틴 이름을 쓰다가 종목 고르기를 열면 검색 칸이, 돌아오면 이름 칸이 포커스를 잡고 키보드를 다시 띄웠다. 키보드가 "n개 추가"를
 * 가리고, 돌아온 뒤 첫 뒤로 가기는 키보드를 닫는 데 쓰였다.
 *
 * 맨 위 뷰가 자식보다 먼저 포커스를 받게 해, 창이 다시 주는 포커스를 맨 위 뷰가 받는다. 입력 칸을 누르면 그 칸이 바로 포커스를 받으므로
 * 입력은 그대로다. 화면 전체를 덮는 기본 포커스 강조는 끈다. API 28부터는 아무것도 바꾸지 않는다.
 */
@Composable
internal fun KeepWindowRefocusOffTextFields() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return
    val view = LocalView.current
    DisposableEffect(view) {
        val root = view.rootView as? ViewGroup
        if (root == null) {
            onDispose { }
        } else {
            val descendantFocusability = root.descendantFocusability
            val focusable = root.focusable
            val focusableInTouchMode = root.isFocusableInTouchMode
            val defaultFocusHighlightEnabled = root.defaultFocusHighlightEnabled
            root.defaultFocusHighlightEnabled = false
            root.isFocusableInTouchMode = true
            root.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
            onDispose {
                root.descendantFocusability = descendantFocusability
                root.focusable = focusable
                root.isFocusableInTouchMode = focusableInTouchMode
                root.defaultFocusHighlightEnabled = defaultFocusHighlightEnabled
            }
        }
    }
}
