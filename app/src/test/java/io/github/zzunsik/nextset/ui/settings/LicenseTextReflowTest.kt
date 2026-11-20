package io.github.zzunsik.nextset.ui.settings

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 라이선스 전문의 줄 합치기 규칙(#57). 앞의 예시는 지어낸 문장이고, 뒤에서 앱에 넣은 라이선스 파일을 그대로 읽어 본다. */
class LicenseTextReflowTest {
    @Test
    fun joinsLinesWithinAParagraph() {
        assertEquals(
            "The goals of this license are to stimulate worldwide development of projects.",
            reflowLicenseText("The goals of this license are to stimulate worldwide\ndevelopment of\nprojects."),
        )
    }

    @Test
    fun keepsBlankLinesBetweenParagraphs() {
        assertEquals(
            "First paragraph goes on here.\n\n\nSecond paragraph.\n",
            reflowLicenseText("First paragraph\ngoes on here.\n\n\nSecond\nparagraph.\n"),
        )
    }

    @Test
    fun keepsSeparatorLines() {
        assertEquals(
            "Copied below.\n-----------\nMADE-UP LICENSE Version 1.0 - 1 January 2000\n===========\nText after.",
            reflowLicenseText("Copied below.\n-----------\nMADE-UP LICENSE Version 1.0 - 1 January 2000\n===========\nText after."),
        )
    }

    @Test
    fun keepsTheBreakAfterAHeading() {
        assertEquals(
            "PERMISSION & CONDITIONS\nPermission is granted to any person.\n\nDISCLAIMER\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND.",
            reflowLicenseText(
                "PERMISSION & CONDITIONS\nPermission is granted\nto any person.\n\nDISCLAIMER\nTHE SOFTWARE IS PROVIDED \"AS IS\",\nWITHOUT WARRANTY\nOF ANY KIND.",
            ),
        )
    }

    @Test
    fun keepsListAndNumberedLinesAndJoinsTheirContinuation() {
        // OFL처럼 번호 다음 줄을 들여쓰지 않은 목록
        assertEquals(
            "1) Neither the software nor its parts may be sold by itself.\n2) Copies may be bundled.",
            reflowLicenseText("1) Neither the software nor its parts\nmay be sold by itself.\n2) Copies\nmay be bundled."),
        )
        // Apache-2.0처럼 번호 뒤 글자 위치에 맞춰 내어 쓴 목록과 (a), (b) 항목
        assertEquals(
            "   4. Redistribution. You may reproduce copies of the Work, provided that:\n" +
                "\n" +
                "      (a) You must give any other recipients a copy; and\n" +
                "      (b) You must keep the notices.\n" +
                "   - one\n" +
                "   * two\n" +
                "   • three",
            reflowLicenseText(
                "   4. Redistribution. You may reproduce copies of the\n" +
                    "      Work, provided that:\n" +
                    "\n" +
                    "      (a) You must give any other\n" +
                    "          recipients a copy; and\n" +
                    "      (b) You must keep the notices.\n" +
                    "   - one\n" +
                    "   * two\n" +
                    "   • three",
            ),
        )
    }

    @Test
    fun keepsLinesWithADifferentIndent() {
        assertEquals(
            "   You may obtain a copy of the License at\n       http://example.com/made-up\n   Unless required by law.",
            reflowLicenseText("   You may obtain a copy of the License at\n       http://example.com/made-up\n   Unless required\n   by law."),
        )
        // 가운데로 맞춘 제목 줄
        assertEquals(
            "      Made-up License\n   Version 1.0, January 2000",
            reflowLicenseText("      Made-up License\n   Version 1.0, January 2000"),
        )
    }

    @Test
    fun joinsWithASingleSpaceEvenAfterTrailingWhitespace() {
        assertEquals("can be bundled, embedded, redistributed", reflowLicenseText("can be bundled, embedded, \nredistributed"))
        assertEquals("      of the\n", reflowLicenseText("      of the\n").let { it })
    }

    /**
     * 앱에 넣은 라이선스 파일마다: 줄을 합친 자리의 공백만 바뀌고 글자는 하나도 빠지거나 바뀌지 않는다.
     * 문단 수(빈 줄), 구분선, 제목, 목록·번호 줄은 그대로 줄 머리에 남는다.
     */
    @Test
    fun bundledLicensesKeepEveryWordAndTheirStructure() {
        val files = File("src/main/assets/licenses").listFiles { file -> file.extension == "txt" }.orEmpty()
        assertTrue("라이선스 파일이 없다", files.isNotEmpty())
        files.forEach { file ->
            val original = file.readText()
            val reflowed = reflowLicenseText(original)
            val name = file.name

            assertEquals(name, collapseWhitespace(original), collapseWhitespace(reflowed))
            // 바뀐 곳은 줄바꿈(과 그 앞뒤 공백)이 공백 하나가 된 자리뿐이다.
            assertEquals(name, original.replace(Regex("""\s+"""), ""), reflowed.replace(Regex("""\s+"""), ""))
            assertTrue(name, reflowed.lines().size < original.lines().size)
            assertEquals(name, original.lines().count { it.isBlank() }, reflowed.lines().count { it.isBlank() })

            val keptLines = original.lines().filterIndexed { index, line ->
                line.isNotBlank() && (
                    index == 0 || original.lines()[index - 1].isBlank() ||
                        Regex("""^\s*([-=_*])\1{2,}\s*$""").matches(line) ||
                        Regex("""^\s*(\d+[.)]|\([a-z]+\))\s""").containsMatchIn(line)
                    )
            }
            val reflowedLines = reflowed.lines()
            keptLines.forEach { kept ->
                assertTrue("$name: 줄 머리에 남아야 한다: $kept", reflowedLines.any { it.startsWith(kept.trimEnd()) })
            }
        }
    }

    @Test
    fun bundledOflKeepsHeadingsAndConditions() {
        val reflowed = reflowLicenseText(File("src/main/assets/licenses/pretendard.txt").readText()).lines()
        listOf("PREAMBLE", "DEFINITIONS", "PERMISSION & CONDITIONS", "TERMINATION", "DISCLAIMER").forEach { heading ->
            assertTrue(heading, heading in reflowed)
        }
        assertTrue(
            reflowed.contains(
                "The goals of the Open Font License (OFL) are to stimulate worldwide development of collaborative font " +
                    "projects, to support the font creation efforts of academic and linguistic communities, and to provide " +
                    "a free and open framework in which fonts may be shared and improved in partnership with others.",
            ),
        )
        assertTrue(reflowed.contains("1) Neither the Font Software nor any of its individual components, in Original or Modified Versions, may be sold by itself."))
    }

    @Test
    fun bundledApacheKeepsSectionsAndItems() {
        val reflowed = reflowLicenseText(File("src/main/assets/licenses/apache-2.0.txt").readText()).lines()
        assertTrue(reflowed.contains("                                 Apache License"))
        assertTrue(reflowed.contains("                           Version 2.0, January 2004"))
        assertTrue(reflowed.contains("   1. Definitions."))
        assertTrue(reflowed.contains("      (a) You must give any other recipients of the Work or Derivative Works a copy of this License; and"))
        assertTrue(reflowed.contains("       http://www.apache.org/licenses/LICENSE-2.0"))
        assertTrue(reflowed.any { it.startsWith("   2. Grant of Copyright License. Subject to the terms and conditions of this License, each") })
    }

    private fun collapseWhitespace(text: String) = text.trim().replace(Regex("""\s+"""), " ")
}
