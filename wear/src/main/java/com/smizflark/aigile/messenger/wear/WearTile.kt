package com.smizflark.aigile.messenger.wear

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

private const val VERSION = "1"
private const val WHITE = 0xFFFFFFFF.toInt()
private const val GREY = 0xFFBBBBBB.toInt()
private const val BLUE = 0xFF8AB4F8.toInt()

/** The "AI-gile" tile: unread count + top 3 chats from the same DataItems the app reads (AII-415). */
class WearTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val content = runCatching { runBlocking { load(applicationContext) } }.getOrDefault(TileContent.build(emptyList(), false))
        return Futures.immediateFuture(
            TileBuilders.Tile.Builder()
                .setResourcesVersion(VERSION)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(applicationContext, content)))
                .build())
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion(VERSION).build())

    companion object {
        /** Called from the app's data listener when /chats or /locked change. */
        fun refresh(ctx: Context) {
            runCatching { getUpdater(ctx).requestUpdate(WearTileService::class.java) }
        }

        private suspend fun load(ctx: Context): TileContent {
            var chats = emptyList<WearChat>()
            var locked = false
            val buf = Wearable.getDataClient(ctx).dataItems.await()
            try {
                buf.forEach {
                    when (it.uri.path) {
                        WearChats.PATH -> it.data?.let { d -> chats = WearChats.parse(String(d)) }
                        WearChats.LOCKED -> locked = WearChats.parseLocked(it.data)
                    }
                }
            } finally { buf.release() }
            return TileContent.build(chats, locked)
        }

        private fun open(ctx: Context, key: String?): ModifiersBuilders.Clickable {
            val a = ActionBuilders.AndroidActivity.Builder().setPackageName(ctx.packageName).setClassName(WearActivity::class.java.name)
            if (key != null) a.addKeyToExtraMapping(WearActivity.EXTRA_CHAT, ActionBuilders.AndroidStringExtra.Builder().setValue(key).build())
            return ModifiersBuilders.Clickable.Builder().setId(key ?: "open")
                .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(a.build()).build()).build()
        }

        private fun text(s: String, size: Float, color: Int, bold: Boolean = false): LayoutElementBuilders.Text =
            LayoutElementBuilders.Text.Builder().setText(StringProp.Builder(s).build()).setMaxLines(1)
                .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END)
                .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(sp(size)).setColor(argb(color))
                    .setWeight(if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL).build())
                .build()

        fun layout(ctx: Context, c: TileContent): LayoutElementBuilders.LayoutElement {
            if (c.locked) {
                return LayoutElementBuilders.Box.Builder().setWidth(expand()).setHeight(expand())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .setModifiers(ModifiersBuilders.Modifiers.Builder()
                        .setPadding(ModifiersBuilders.Padding.Builder().setAll(dp(24f)).build()).setClickable(open(ctx, null)).build())
                    .addContent(LayoutElementBuilders.Text.Builder().setText(StringProp.Builder(ctx.getString(R.string.locked)).build())
                        .setMaxLines(3).setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
                        .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(sp(14f)).setColor(argb(WHITE)).build()).build())
                    .build()
            }
            val col = LayoutElementBuilders.Column.Builder().setWidth(expand()).setHeight(expand())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(open(ctx, null)).build())
            col.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(22f)).build())
            col.addContent(text(ctx.getString(R.string.app_name), 12f, BLUE, true))
            col.addContent(text(c.unread.toString(), 30f, WHITE, true))
            col.addContent(text(ctx.resources.getQuantityString(R.plurals.tile_unread, c.unread), 11f, GREY))
            col.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(6f)).build())
            if (c.top.isEmpty()) col.addContent(text(ctx.getString(R.string.waiting), 12f, GREY))
            c.top.forEach { chat ->
                col.addContent(LayoutElementBuilders.Box.Builder().setWidth(dp(120f))
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(open(ctx, chat.key))
                        .setPadding(ModifiersBuilders.Padding.Builder().setTop(dp(2f)).setBottom(dp(2f)).build()).build())
                    .addContent(text((if (chat.unread) "● " else "") + chat.name, 13f, if (chat.unread) WHITE else GREY, chat.unread)).build())
            }
            return col.build()
        }
    }
}
