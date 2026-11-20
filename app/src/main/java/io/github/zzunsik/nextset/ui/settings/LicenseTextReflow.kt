package io.github.zzunsik.nextset.ui.settings

/** 구분선: 같은 기호(`-`, `=`, `_`, `*`)만 세 개 이상 있는 줄. */
private val SEPARATOR = Regex("""^\s*([-=_*])\1{2,}\s*$""")

/** 목록·번호 표시: `1.`, `1)`, `(a)`, `(iv)`, `(1)`, `a)`, `-`, `*`, `•` 뒤에 공백. */
private val LIST_MARKER = Regex("""^\s*(\d+[.)]|\((?:[a-z]|[ivx]+|\d+)\)|[a-z]\)|[-*•])\s+""")

/** 문단 첫 줄의 제목: 대문자, 공백, `&`만 있는 줄(OFL의 "PREAMBLE", "PERMISSION & CONDITIONS"). */
private val HEADING = Regex("""^\s*[A-Z][A-Z &]*$""")

/**
 * 라이선스 원문을 화면에 그릴 때 문단 안의 줄바꿈을 공백으로 합친다(#57). 파일은 고치지 않고 그릴 때만 쓴다.
 * 원문은 70~80자에서 줄을 바꿔 두어 화면 폭에서 다시 접히면 한두 단어짜리 줄이 생기기 때문이다.
 *
 * 줄은 바로 앞 줄에 이어 붙는다. 다만 다음 줄바꿈은 그대로 둔다.
 * - 빈 줄(문단 경계)과 그 앞뒤
 * - 구분선(`-----`)과 그 앞뒤
 * - 문단 첫 줄이 제목(대문자, 공백, `&`만)일 때 그 뒤
 * - 목록·번호 표시(`1.`, `1)`, `(a)` 등)로 시작하는 줄의 앞
 * - 들여쓰기가 앞 줄과 다른 줄의 앞. 목록 줄 다음에는 표시 뒤 글자 위치에 맞춘 들여쓰기(내어쓰기)도 이어 붙는다
 *   (Apache-2.0의 "2. Grant of ..." 다음 줄이 6칸 들여쓰기인 경우)
 *
 * 이어 붙일 때는 앞 줄 끝 공백, 줄바꿈, 다음 줄 앞 공백을 공백 하나로 바꾼다. 그 밖의 글자는 바꾸지 않는다.
 */
fun reflowLicenseText(text: String): String {
    val lines = text.lines()
    val out = ArrayList<String>(lines.size)
    // 마지막으로 낸 줄 뒤에 이어 붙일 수 있는지와, 이어 붙일 줄의 들여쓰기.
    var open = false
    var continuationIndents = emptySet<Int>()
    lines.forEachIndexed { index, line ->
        if (line.isBlank()) {
            out += line
            open = false
            return@forEachIndexed
        }
        val indent = line.length - line.trimStart().length
        val separator = SEPARATOR.matches(line)
        val marker = LIST_MARKER.find(line)
        if (open && !separator && marker == null && indent in continuationIndents) {
            out[out.lastIndex] = out.last().trimEnd() + " " + line.trim()
            continuationIndents = setOf(indent)
            return@forEachIndexed
        }
        out += line
        val firstInParagraph = index == 0 || lines[index - 1].isBlank()
        open = !separator && !(firstInParagraph && HEADING.matches(line))
        continuationIndents = if (marker != null) setOf(indent, marker.range.last + 1) else setOf(indent)
    }
    return out.joinToString("\n")
}
