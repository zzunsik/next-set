package io.github.zzunsik.nextset.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import io.github.zzunsik.nextset.NextSetApplication
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerKey
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerMode
import io.github.zzunsik.nextset.ui.navigation.NavResults
import io.github.zzunsik.nextset.ui.navigation.NextSetNavHost
import io.github.zzunsik.nextset.ui.navigation.ResultEffect
import io.github.zzunsik.nextset.ui.navigation.pickedExercisesKey
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import kotlinx.serialization.Serializable

/**
 * 디버그 빌드에만 있는 시험 화면(#21). 종목 고르기를 부르는 화면(루틴 편집 #22, 운동 중 #24)이 생기기 전에
 * 기기에서 종목 고르기와 커스텀 종목 추가·수정·삭제를 써 보고, 고른 종목이 부른 화면으로 돌아오는지 본다.
 * 그 화면들이 쓸 방식(키를 스택에 넣고 [ResultEffect]로 받기)을 그대로 쓴다. 실제 앱 데이터베이스를 쓴다.
 */
class ExerciseTryOutActivity : ComponentActivity() {
    @Serializable
    private data object TryOutKey : NavKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as NextSetApplication).container.exerciseRepository
        setContent {
            NextSetTheme {
                val backStack = rememberNavBackStack(TryOutKey)
                val results = remember { NavResults() }
                NextSetNavHost(backStack = backStack, results = results, exerciseRepository = repository) {
                    entry<TryOutKey> {
                        var picked by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
                        results.ResultEffect(pickedExercisesKey(RESULT_NAME)) { ids ->
                            picked = ids
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(NextSetTheme.spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.md),
                        ) {
                            PrimaryButton(
                                text = stringResource(R.string.debug_open_picker),
                                onClick = { backStack.add(ExercisePickerKey(ExercisePickerMode.Pick, RESULT_NAME)) },
                            )
                            PrimaryButton(
                                text = stringResource(R.string.debug_open_manager),
                                onClick = { backStack.add(ExercisePickerKey(ExercisePickerMode.Manage)) },
                            )
                            Text(
                                text = if (picked.isEmpty()) {
                                    stringResource(R.string.debug_picked_none)
                                } else {
                                    stringResource(R.string.debug_picked, picked.joinToString())
                                },
                                style = NextSetTheme.typography.body,
                                color = NextSetTheme.colors.text,
                            )
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val RESULT_NAME = "debug-try-out"
    }
}
