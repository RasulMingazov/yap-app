package app.yap.app.root.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val VIEWPORT = 24f
private const val STROKE = 2.2f

// The three tab glyphs as drawn in `feature_main.dc.html` (stroke icons, 22×22 on a 24 grid).
internal val HomeTabIcon: ImageVector = strokeIcon("main.tab.home") {
    path("M4 10.5 L12 4 L20 10.5 V19 A1 1 0 0 1 19 20 H14.5 V14.5 H9.5 V20 H5 A1 1 0 0 1 4 19 Z")
}

internal val ScenariosTabIcon: ImageVector = strokeIcon("main.tab.scenarios") {
    listOf(4f to 4f, 13.5f to 4f, 4f to 13.5f, 13.5f to 13.5f).forEach { (x, y) ->
        roundedSquare(x = x, y = y)
    }
}

internal val ProfileTabIcon: ImageVector = strokeIcon("main.tab.profile") {
    path("M20 21 A8 8 0 0 0 4 21")
    path("M12 3.5 A4 4 0 1 1 11.99 3.5 Z")
}

private class IconScope(val builder: ImageVector.Builder) {

    fun path(data: String) {
        builder.addPath(
            pathData = addPathNodes(data),
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        )
    }

    fun roundedSquare(x: Float, y: Float, size: Float = 6.5f, radius: Float = 1.8f) {
        val right = x + size
        val bottom = y + size
        path(
            "M${x + radius} $y H${right - radius} A$radius $radius 0 0 1 $right ${y + radius} " +
                "V${bottom - radius} A$radius $radius 0 0 1 ${right - radius} $bottom " +
                "H${x + radius} A$radius $radius 0 0 1 $x ${bottom - radius} " +
                "V${y + radius} A$radius $radius 0 0 1 ${x + radius} $y Z",
        )
    }
}

private fun strokeIcon(name: String, content: IconScope.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 22.dp,
        defaultHeight = 22.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    ).apply { IconScope(this).content() }.build()
