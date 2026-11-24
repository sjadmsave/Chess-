package com.example.chess

internal val mateInTwoPuzzlePack: List<ChessPuzzle> = listOf(
    ChessPuzzle(
        id = "p_m2_x1",
        titleAr = "تضحية الصف الخلفي #1",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1280,
        fen = "3r2k1/5ppp/4Q3/8/8/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e6e8", "d8e8", "e1e8"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x2",
        titleAr = "تركيبة الإماتة في نقلتين #2",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1294,
        fen = "3r3k/6pp/5N2/4Q3/8/4R3/6PP/6K1 w - - 0 1",
        solutionUci = listOf("e5e8", "d8e8", "e3e8"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x3",
        titleAr = "مطاردة الملك في الزاوية #3",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1308,
        fen = "2k5/8/8/r7/4q3/8/6P1/6K1 b - - 0 1",
        solutionUci = listOf("e4e1", "g1h2", "a5h5"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x4",
        titleAr = "اقتحام القطر الحاسم #4",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1322,
        fen = "5k2/5pp1/8/8/4BQ2/8/5PP1/6K1 w - - 0 1",
        solutionUci = listOf("f4d6", "f8e8", "e4c6"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x5",
        titleAr = "تضحية الصف الخلفي #5",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1336,
        fen = "2q4k/6pp/8/8/3Q4/8/5PPP/3R2K1 w - - 0 1",
        solutionUci = listOf("d4d8", "c8d8", "d1d8"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x6",
        titleAr = "تركيبة الإماتة في نقلتين #6",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1350,
        fen = "6k1/6pp/5r2/8/5q2/4n3/6PP/4R2K b - - 0 1",
        solutionUci = listOf("f4f1", "e1f1", "f6f1"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x7",
        titleAr = "مطاردة الملك في الزاوية #7",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1364,
        fen = "7k/8/8/8/1RQ5/8/8/3K4 w - - 0 1",
        solutionUci = listOf("c4c7", "h8g8", "b4b8"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x8",
        titleAr = "اقتحام القطر الحاسم #8",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1378,
        fen = "6k1/6pp/3Q4/8/8/8/4BPP1/6K1 w - - 0 1",
        solutionUci = listOf("e2c4", "g8h8", "d6d8"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x9",
        titleAr = "تضحية الصف الخلفي #9",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1392,
        fen = "3r2k1/5ppp/8/8/8/5q2/6PP/2Q4K b - - 0 1",
        solutionUci = listOf("d8d1", "c1d1", "f3d1"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x10",
        titleAr = "تركيبة الإماتة في نقلتين #10",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1406,
        fen = "1r5k/6pp/7N/8/2Q5/8/6PP/6K1 w - - 0 1",
        solutionUci = listOf("c4g8", "b8g8", "h6f7"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x11",
        titleAr = "مطاردة الملك في الزاوية #11",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1420,
        fen = "k3r3/p7/1Q6/8/8/2R5/8/2K5 w - - 0 1",
        solutionUci = listOf("b6c6", "a8b8", "c3b3"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x12",
        titleAr = "اقتحام القطر الحاسم #12",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1434,
        fen = "6k1/5pp1/8/6q1/1b6/8/7P/7K b - - 0 1",
        solutionUci = listOf("b4c5", "h2h3", "g5g1"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x13",
        titleAr = "تضحية الصف الخلفي #13",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1448,
        fen = "2r3k1/5ppp/8/8/Q7/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("a4e8", "c8e8", "e1e8"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x14",
        titleAr = "تركيبة الإماتة في نقلتين #14",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1462,
        fen = "3r3k/6pp/4N3/5Q2/8/5R2/6PP/6K1 w - - 0 1",
        solutionUci = listOf("f5f8", "d8f8", "f3f8"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x15",
        titleAr = "مطاردة الملك في الزاوية #15",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1476,
        fen = "2k5/8/8/8/5qr1/8/8/1K6 b - - 0 1",
        solutionUci = listOf("f4f2", "b1a1", "g4g1"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x16",
        titleAr = "اقتحام القطر الحاسم #16",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1490,
        fen = "5k2/5pp1/6Q1/8/8/3B4/5PP1/6K1 w - - 0 1",
        solutionUci = listOf("g6d6", "f8e8", "d3b5"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x17",
        titleAr = "تضحية الصف الخلفي #17",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1504,
        fen = "2q4k/6pp/5Q2/8/8/8/5PPP/3R2K1 w - - 0 1",
        solutionUci = listOf("f6d8", "c8d8", "d1d8"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x18",
        titleAr = "تركيبة الإماتة في نقلتين #18",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1518,
        fen = "6k1/6pp/8/8/3q4/7n/6PP/1R5K b - - 0 1",
        solutionUci = listOf("d4g1", "b1g1", "h3f2"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x19",
        titleAr = "مطاردة الملك في الزاوية #19",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1532,
        fen = "k7/p7/8/8/1Q5B/8/8/3K4 w - - 0 1",
        solutionUci = listOf("h4g3", "a7a6", "b4b8"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x20",
        titleAr = "اقتحام القطر الحاسم #20",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1546,
        fen = "5k2/5pp1/8/4Q3/8/3B4/5PP1/6K1 w - - 0 1",
        solutionUci = listOf("e5d6", "f8e8", "d3b5"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x21",
        titleAr = "تضحية الصف الخلفي #21",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1560,
        fen = "4r1k1/5ppp/8/8/1q6/8/5PPP/2R3K1 b - - 0 1",
        solutionUci = listOf("e8e1", "c1e1", "b4e1"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x22",
        titleAr = "تركيبة الإماتة في نقلتين #22",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1574,
        fen = "2r4k/6pp/7N/3Q4/8/8/6PP/6K1 w - - 0 1",
        solutionUci = listOf("d5g8", "c8g8", "h6f7"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x23",
        titleAr = "مطاردة الملك في الزاوية #23",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1588,
        fen = "3r2k1/8/8/5Q2/1R6/8/8/6K1 w - - 0 1",
        solutionUci = listOf("b4g4", "g8h8", "f5h5"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x24",
        titleAr = "اقتحام القطر الحاسم #24",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1602,
        fen = "6k1/5pp1/8/5b2/8/3q4/6PP/6K1 b - - 0 1",
        solutionUci = listOf("d3e3", "g1f1", "f5d3"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x25",
        titleAr = "تضحية الصف الخلفي #25",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1616,
        fen = "2q2k2/4ppp1/8/3Q4/8/8/5PPP/3R2K1 w - - 0 1",
        solutionUci = listOf("d5d8", "c8d8", "d1d8"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x26",
        titleAr = "تركيبة الإماتة في نقلتين #26",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1630,
        fen = "4r2k/6pp/4N3/5Q2/8/5R2/6PP/6K1 w - - 0 1",
        solutionUci = listOf("f5f8", "e8f8", "f3f8"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x27",
        titleAr = "مطاردة الملك في الزاوية #27",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1644,
        fen = "3k4/8/8/8/3q4/2r5/7P/7K b - - 0 1",
        solutionUci = listOf("d4g4", "h2h3", "c3h3"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x28",
        titleAr = "اقتحام القطر الحاسم #28",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1658,
        fen = "7k/7p/8/3Q4/4B3/8/5PP1/6K1 w - - 0 1",
        solutionUci = listOf("d5f7", "h7h6", "f7f8"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x29",
        titleAr = "تضحية الصف الخلفي #29",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1672,
        fen = "2q2k2/4ppp1/8/Q7/8/8/5PPP/3R2K1 w - - 0 1",
        solutionUci = listOf("a5d8", "c8d8", "d1d8"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x30",
        titleAr = "تركيبة الإماتة في نقلتين #30",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1686,
        fen = "6k1/6pp/4r3/8/4q3/5n2/6PP/3R3K b - - 0 1",
        solutionUci = listOf("e4e1", "d1e1", "e6e1"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x31",
        titleAr = "مطاردة الملك في الزاوية #31",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1700,
        fen = "6k1/6p1/8/5Q2/7R/8/8/6K1 w - - 0 1",
        solutionUci = listOf("f5e6", "g8f8", "h4h8"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x32",
        titleAr = "اقتحام القطر الحاسم #32",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1714,
        fen = "5k2/5pp1/6Q1/8/8/8/2B2PP1/6K1 w - - 0 1",
        solutionUci = listOf("g6d6", "f8e8", "c2a4"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x33",
        titleAr = "تضحية الصف الخلفي #33",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1728,
        fen = "4r1k1/5ppp/8/4q3/8/8/6PP/3Q3K b - - 0 1",
        solutionUci = listOf("e5e1", "d1e1", "e8e1"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x34",
        titleAr = "تركيبة الإماتة في نقلتين #34",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1292,
        fen = "2r4k/6pp/7N/8/8/1Q6/6PP/6K1 w - - 0 1",
        solutionUci = listOf("b3g8", "c8g8", "h6f7"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x35",
        titleAr = "مطاردة الملك في الزاوية #35",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1306,
        fen = "k3r3/p7/8/2Q5/4R3/8/8/2K5 w - - 0 1",
        solutionUci = listOf("c5c6", "a8b8", "e4e8"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x36",
        titleAr = "اقتحام القطر الحاسم #36",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1320,
        fen = "6k1/5pp1/8/6q1/1b6/8/6PP/6K1 b - - 0 1",
        solutionUci = listOf("g5e3", "g1f1", "e3e1"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x37",
        titleAr = "تضحية الصف الخلفي #37",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1334,
        fen = "3r2k1/5ppp/8/8/Q7/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("a4e8", "d8e8", "e1e8"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x38",
        titleAr = "تركيبة الإماتة في نقلتين #38",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1348,
        fen = "r6k/6pp/7N/8/2Q5/8/6PP/6K1 w - - 0 1",
        solutionUci = listOf("c4g8", "a8g8", "h6f7"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x39",
        titleAr = "مطاردة الملك في الزاوية #39",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1362,
        fen = "2k5/8/b7/8/8/3q4/8/K7 b - - 0 1",
        solutionUci = listOf("d3a3", "a1b1", "a6d3"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x40",
        titleAr = "اقتحام القطر الحاسم #40",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1376,
        fen = "7k/7p/8/3Q4/8/8/2B2PP1/6K1 w - - 0 1",
        solutionUci = listOf("d5f7", "h7h6", "f7f8"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x41",
        titleAr = "تضحية الصف الخلفي #41",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1390,
        fen = "3q2k1/5ppp/8/8/Q7/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("a4e8", "d8e8", "e1e8"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x42",
        titleAr = "تركيبة الإماتة في نقلتين #42",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1404,
        fen = "6k1/6pp/1q6/8/8/7n/6PP/R6K b - - 0 1",
        solutionUci = listOf("b6g1", "a1g1", "h3f2"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x43",
        titleAr = "مطاردة الملك في الزاوية #43",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1418,
        fen = "6k1/6p1/8/5N2/5Q2/8/8/3K4 w - - 0 1",
        solutionUci = listOf("f5e7", "g8h8", "f4h4"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x44",
        titleAr = "اقتحام القطر الحاسم #44",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1432,
        fen = "6k1/6pp/8/8/5Q2/3B4/5PP1/6K1 w - - 0 1",
        solutionUci = listOf("d3c4", "g8h8", "f4f8"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x45",
        titleAr = "تضحية الصف الخلفي #45",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1446,
        fen = "4r1k1/5ppp/8/8/8/4q3/5PPP/3R2K1 b - - 0 1",
        solutionUci = listOf("e3e1", "d1e1", "e8e1"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x46",
        titleAr = "تركيبة الإماتة في نقلتين #46",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1460,
        fen = "1r5k/6pp/7N/3Q4/8/8/6PP/6K1 w - - 0 1",
        solutionUci = listOf("d5g8", "b8g8", "h6f7"),
        hintAr = "ابدأ بكش قسري يضيق خناق الملك أو يسد مربع هروبه.",
        explanationAr = "الكش الأول يجبر الخصم على رد وحيد يمهّد لنقلة الإماتة القاضية."
    ),
    ChessPuzzle(
        id = "p_m2_x47",
        titleAr = "مطاردة الملك في الزاوية #47",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1474,
        fen = "k3r3/p7/3Q4/2B5/8/8/8/6K1 w - - 0 1",
        solutionUci = listOf("d6c6", "a8b8", "c5d6"),
        hintAr = "اقطع طريق هروب الملك بكش دقيق ثم أجهز عليه في النقلة الثانية.",
        explanationAr = "تنسيق مثالي بين القطع الثقيلة يدفع الملك إلى زاوية لا مفر منها."
    ),
    ChessPuzzle(
        id = "p_m2_x48",
        titleAr = "اقتحام القطر الحاسم #48",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1488,
        fen = "6k1/5pp1/8/8/4bq2/8/6PP/6K1 b - - 0 1",
        solutionUci = listOf("f4e3", "g1f1", "e4d3"),
        hintAr = "افتح الطريق أمام القطر القاتل بكش إجباري في النقلة الأولى.",
        explanationAr = "التضحية أو التمبو الأول يجبر الملك على الانتقال لمربع الإماتة المباشرة."
    ),
    ChessPuzzle(
        id = "p_m2_x49",
        titleAr = "تضحية الصف الخلفي #49",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1502,
        fen = "2q3k1/5ppp/8/4Q3/8/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e5e8", "c8e8", "e1e8"),
        hintAr = "اسحب المدافع عن الصف الثامن بتضحية قسرية أولى.",
        explanationAr = "التضحية الأولى تجبر المدافع على التخلي عن حماية الصف الأخير ثم تأتي الإماتة."
    ),
    ChessPuzzle(
        id = "p_m2_x50",
        titleAr = "إبعاد المدافع عن الصف الأخير #50",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1420,
        fen = "1r5k/5ppp/8/8/4Q3/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e4e8", "b8e8", "e1e8"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x51",
        titleAr = "إبعاد المدافع عن الصف الأخير #51",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1434,
        fen = "2r4k/5ppp/8/4Q3/8/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e5e8", "c8e8", "e1e8"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x52",
        titleAr = "إبعاد المدافع عن الصف الأخير #52",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1448,
        fen = "4r1k1/5ppp/8/8/8/4q3/5PPP/R5K1 b - - 0 1",
        solutionUci = listOf("e3e1", "a1e1", "e8e1"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x53",
        titleAr = "إبعاد المدافع عن الصف الأخير #53",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1462,
        fen = "r6k/5ppp/4Q3/8/8/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e6e8", "a8e8", "e1e8"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x54",
        titleAr = "إبعاد المدافع عن الصف الأخير #54",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1476,
        fen = "4r1k1/5ppp/8/8/8/4q3/5PPP/1R4K1 b - - 0 1",
        solutionUci = listOf("e3e1", "b1e1", "e8e1"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x55",
        titleAr = "إبعاد المدافع عن الصف الأخير #55",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1490,
        fen = "1r5k/5ppp/8/4Q3/8/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e5e8", "b8e8", "e1e8"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x56",
        titleAr = "إبعاد المدافع عن الصف الأخير #56",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1504,
        fen = "4r1k1/5ppp/8/8/8/4q3/5PPP/2R4K b - - 0 1",
        solutionUci = listOf("e3e1", "c1e1", "e8e1"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x57",
        titleAr = "إبعاد المدافع عن الصف الأخير #57",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1518,
        fen = "3r2k1/5ppp/8/4Q3/8/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e5e8", "d8e8", "e1e8"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x58",
        titleAr = "إبعاد المدافع عن الصف الأخير #58",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1532,
        fen = "4r1k1/5ppp/8/8/4q3/8/5PPP/3R3K b - - 0 1",
        solutionUci = listOf("e4e1", "d1e1", "e8e1"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x59",
        titleAr = "إبعاد المدافع عن الصف الأخير #59",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1546,
        fen = "1r4k1/5ppp/4Q3/8/8/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e6e8", "b8e8", "e1e8"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x60",
        titleAr = "إبعاد المدافع عن الصف الأخير #60",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1560,
        fen = "4r1k1/5ppp/8/8/8/4q3/5PPP/R6K b - - 0 1",
        solutionUci = listOf("e3e1", "a1e1", "e8e1"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x61",
        titleAr = "إبعاد المدافع عن الصف الأخير #61",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1574,
        fen = "1r5k/5ppp/4Q3/8/8/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e6e8", "b8e8", "e1e8"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x62",
        titleAr = "إبعاد المدافع عن الصف الأخير #62",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1588,
        fen = "4r1k1/5ppp/8/4q3/8/8/5PPP/R5K1 b - - 0 1",
        solutionUci = listOf("e5e1", "a1e1", "e8e1"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x63",
        titleAr = "إبعاد المدافع عن الصف الأخير #63",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1602,
        fen = "4r1k1/5ppp/8/4q3/8/8/5PPP/2R4K b - - 0 1",
        solutionUci = listOf("e5e1", "c1e1", "e8e1"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x64",
        titleAr = "إبعاد المدافع عن الصف الأخير #64",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1616,
        fen = "4r1k1/5ppp/8/8/8/4q3/5PPP/1R5K b - - 0 1",
        solutionUci = listOf("e3e1", "b1e1", "e8e1"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x65",
        titleAr = "إبعاد المدافع عن الصف الأخير #65",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1630,
        fen = "2r4k/5ppp/4Q3/8/8/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e6e8", "c8e8", "e1e8"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x66",
        titleAr = "إبعاد المدافع عن الصف الأخير #66",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1644,
        fen = "4r1k1/5ppp/8/8/4q3/8/5PPP/3R2K1 b - - 0 1",
        solutionUci = listOf("e4e1", "d1e1", "e8e1"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x67",
        titleAr = "إبعاد المدافع عن الصف الأخير #67",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1658,
        fen = "3r3k/5ppp/4Q3/8/8/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e6e8", "d8e8", "e1e8"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x68",
        titleAr = "إبعاد المدافع عن الصف الأخير #68",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1672,
        fen = "4r1k1/5ppp/8/8/8/4q3/5PPP/3R3K b - - 0 1",
        solutionUci = listOf("e3e1", "d1e1", "e8e1"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    ),
    ChessPuzzle(
        id = "p_m2_x69",
        titleAr = "إبعاد المدافع عن الصف الأخير #69",
        category = PuzzleCategory.MATE_IN_2,
        rating = 1686,
        fen = "r5k1/5ppp/8/4Q3/8/8/5PPP/4R1K1 w - - 0 1",
        solutionUci = listOf("e5e8", "a8e8", "e1e8"),
        hintAr = "ضحِّ بالوزير لسحب القلعة المدافعة من الصف الأخير ثم وجّه المات بالقلعة.",
        explanationAr = "تضحية الوزير القسرية تُرغم القلعة على ترك حماية المربع الحاسم."
    )
)
