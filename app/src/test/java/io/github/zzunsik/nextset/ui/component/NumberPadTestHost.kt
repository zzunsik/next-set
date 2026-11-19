package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/** 테스트 호스트의 칸 id. 행 번호와 칸 순번. */
data class HostFieldId(val row: Int, val index: Int)

/**
 * 숫자판을 화면에 붙이는 방법을 보여 주는 테스트용 화면. 운동 중(#24)과 루틴 편집(#22) 화면이 같은 식으로 붙인다.
 * 세트 행 여러 줄을 목록으로 그리고, 칸을 누르면 [NumberPadSession]을 열어 [NumberPadScaffold]에 넘긴다.
 * 문구와 값은 지어낸 것이다.
 *
 * @param initial 행마다 칸 값(기록 방식의 칸 순서).
 * @param restBar true면 휴식 타이머 바 자리에 테스트용 바를 둔다.
 */
@Composable
fun NumberPadTestHost(
    recordType: RecordType,
    initial: List<List<Int?>>,
    modifier: Modifier = Modifier,
    restBar: Boolean = false,
    openAt: HostFieldId? = null,
    listState: LazyListState = rememberLazyListState(),
    onEvent: (NumberPadEvent<HostFieldId>) -> Unit = {},
) {
    val kinds = recordType.numberFieldKinds()
    val values = remember { mutableStateListOf(*initial.map { it.toTypedArray() }.toTypedArray()) }
    fun rowFields(row: Int) = kinds.mapIndexed { i, kind -> NumberPadField(HostFieldId(row, i), kind, values[row][i]) }
    var session by remember {
        mutableStateOf(openAt?.let { NumberPadSession.open(rowFields(it.row), it.index) })
    }
    val labels = mapOf(
        NumberFieldKind.WEIGHT to stringResource(R.string.number_field_weight),
        NumberFieldKind.REPS to stringResource(R.string.number_field_reps),
        NumberFieldKind.DURATION to stringResource(R.string.number_field_duration),
    )

    NumberPadScaffold(
        numberPad = session.padSpec,
        onNumberPadKey = { key ->
            val result = session?.press(key) ?: return@NumberPadScaffold
            result.events.forEach { event ->
                if (event is NumberPadEvent.ValueChanged) {
                    val id = event.id
                    values[id.row] = values[id.row].copyOf().also { it[id.index] = event.amount }
                }
                onEvent(event)
            }
            session = result.session
        },
        modifier = modifier.background(NextSetTheme.colors.bg),
        bottomBar = {
            if (restBar) {
                // 휴식 타이머 바(#26)의 자리. 진짜 바는 #26에서 만든다.
                Box(
                    modifier = Modifier
                        .testTag(REST_BAR)
                        .fillMaxWidth()
                        .heightIn(min = NextSetTheme.sizes.buttonHeight)
                        .background(NextSetTheme.colors.surface)
                        .padding(horizontal = NextSetTheme.spacing.lg),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text("1:45", style = NextSetTheme.typography.numeralMedium, color = NextSetTheme.colors.text)
                }
            }
        },
    ) { padding ->
        LazyColumn(state = listState, contentPadding = padding, modifier = Modifier.testTag(LIST)) {
            itemsIndexed(values) { row, rowValues ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NextSetTheme.spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${row + 1}",
                        style = NextSetTheme.typography.body,
                        color = NextSetTheme.colors.text,
                        // 순번 "1"이 숫자판의 "1" 키와 같게 읽히지 않도록 이름을 붙인다(ATF DuplicateSpeakableTextCheck).
                        modifier = Modifier
                            .width(32.dp)
                            .semantics { contentDescription = "${row + 1}세트" },
                    )
                    kinds.forEachIndexed { index, kind ->
                        val id = HostFieldId(row, index)
                        val current = session
                        val selected = current?.focused?.id == id
                        NumberInputField(
                            value = if (selected) current.entry.displayText else NumberEntry.displayTextOf(kind, rowValues[index]),
                            label = labels.getValue(kind),
                            selected = selected,
                            onClick = {
                                session = if (current != null && current.focused.id.row == row) {
                                    current.focus(index)
                                } else {
                                    NumberPadSession.open(rowFields(row), index)
                                }
                            },
                            modifier = Modifier
                                .testTag(fieldTag(row, index))
                                .width(if (kinds.size == 1) 88.dp else if (kind == NumberFieldKind.WEIGHT) 72.dp else 64.dp),
                        )
                    }
                }
            }
        }
    }
}

const val LIST = "list"
const val REST_BAR = "restBar"

fun fieldTag(row: Int, index: Int) = "field-$row-$index"
