package com.catpuppyapp.sshkeyman.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catpuppyapp.sshkeyman.constants.Cons
import com.catpuppyapp.sshkeyman.style.MyStyleKt
import com.catpuppyapp.sshkeyman.theme.Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


object UIHelper {

    fun getFontColor(inDarkTheme:Boolean= Theme.inDarkTheme): Color {
        return if(inDarkTheme) MyStyleKt.TextColor.darkThemeFontColor else MyStyleKt.TextColor.fontColor
    }

    fun getDisableBtnColor(inDarkTheme: Boolean= Theme.inDarkTheme):Color {
        return if(inDarkTheme) MyStyleKt.IconColor.disable_DarkTheme else MyStyleKt.IconColor.disable
    }

    //不太重要的字体颜色
    fun getSecondaryFontColor(inDarkTheme:Boolean= Theme.inDarkTheme): Color {
        return if(inDarkTheme) MyStyleKt.TextColor.darkThemeSecondaryFontColor else MyStyleKt.TextColor.secondaryFontColor
    }

    /**
     * enable 为true返回启用颜色，否则返回null
     */
    @Composable
    fun getIconEnableColorOrNull(enable:Boolean):Color? {
        if(!enable) return null

        //darkTheme用反转颜色；否则用主颜色
        val color = if(Theme.inDarkTheme) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.primary
//        val enableColor = Color(red=(color.red+0.3f).coerceAtMost(1f), green = (color.green+0.3f).coerceAtMost(1f), blue = (color.blue+0.3f).coerceAtMost(1f), alpha = 1f)
        //如果是darkTheme，提升整体亮度；否则只增加蓝色
        val enableColor = if(Theme.inDarkTheme) Color(red=(color.red+0.3f).coerceAtMost(1f), green = (color.green+0.3f).coerceAtMost(1f), blue = (color.blue+0.3f).coerceAtMost(1f), alpha = 1f) else Color(red=color.red, green = color.green, blue = (color.blue+0.5f).coerceAtMost(1f), alpha = 1f)
        return enableColor
    }

    //长按选中连续条目
    fun<T> doSelectSpan(itemIdxOfItemList:Int, item: T, selectedItems:List<T>, itemList:List<T>, switchItemSelected:(T)->Unit, selectIfNotInSelectedListElseNoop:(T)->Unit) {
        //如果 已选条目列表为空 或 索引无效，选中条目，然后返回
        if(selectedItems.isEmpty() || itemIdxOfItemList<0 || itemIdxOfItemList>itemList.lastIndex) {
            switchItemSelected(item)
            return
        }

        //如果不为空，执行连续选中

        //取出最后一个选择的条目
        val lastSelectedItem = selectedItems.last()

        //在源list中查找最后一个条目的位置（索引）
        val lastSelectedItemIndexOfItemList = itemList.indexOf(lastSelectedItem)

        //itemList查无选中列表的最后一个元素，发生这种情况的场景举例：完整列表，选中条目abc，过滤列表不包含abc，长按选择，过滤列表被传入此函数的itemList，这时，itemList就会查无abc，indexOf返回-1
        if(lastSelectedItemIndexOfItemList == -1) {
            switchItemSelected(item)
            return
        }

        //如果长按的条目就是之前选中的条目，什么都不做（选中一个条目，然后长按它即可触发此条件）
        if(lastSelectedItemIndexOfItemList == itemIdxOfItemList) {
            return
        }

        //min()
        val startIndex = Math.min(lastSelectedItemIndexOfItemList, itemIdxOfItemList)
        //max()
        val endIndexExclusive = Math.max(lastSelectedItemIndexOfItemList, itemIdxOfItemList) + 1

        //检查索引是否有效
        if(startIndex >= endIndexExclusive
            || startIndex<0 || startIndex>itemList.lastIndex
            || endIndexExclusive<0 || endIndexExclusive>itemList.size
        ) {
            return
        }

        //选中范围内的条目 左闭右开 [startIndex, endIndexExclusive)
        //list.forEach(selectIfNotInSelectedListElseNoop) 等于 list.forEach{selectIfNotInSelectedListElseNoop(it)}
//        itemList.subList(startIndex, endIndexExclusive).forEach {selectIfNotInSelectedListElseNoop(it)}  //需要拷贝列表，bad
        for(i in startIndex..<endIndexExclusive) {
            selectIfNotInSelectedListElseNoop(itemList[i])  //不需要拷贝列表，good
        }
    }

    //如果没在已选中列表，执行选中，否则什么都不做
    fun<T> selectIfNotInSelectedListElseNoop(
        item: T,
        selectedItems:MutableList<T>,
        contains:(srcList:List<T>, T)->Boolean = {list, i -> list.contains(i)}
    ) {
        if(!contains(selectedItems, item)) {
            selectedItems.add(item)
        }
    }

    fun<T> selectIfNotInSelectedListElseRemove(
        item:T,
        selectedItems:MutableList<T>,
        contains:(srcList:List<T>, curItem:T)->Boolean = {srcList, curItem -> srcList.contains(curItem)}
    ) {
        if(contains(selectedItems, item)) {
            selectedItems.remove(item)
        }else{
            selectedItems.add(item)
        }
    }

    fun scrollToItem(coroutineScope: CoroutineScope, listState: LazyListState, index:Int)  {
        coroutineScope.launch { listState.scrollToItem(Math.max(0, index)) }
    }

    fun scrollTo(coroutineScope: CoroutineScope, listState: ScrollState, index:Int)  {
        coroutineScope.launch { listState.scrollTo(Math.max(0, index)) }
    }

    /**
     * 获取高亮条目闪烁时间（换个角度来说，即“解除高亮倒计时”）。
     * 应用场景：在列表定位某个条目后，短暂高亮那个条目以便用户发现
     */
    fun getHighlightingTimeInMills(): Long {
//        return 2000L  //注：1000等于1秒
        return 800L
    }

    @Composable
    fun getHighlightingBackgroundColor(baseColor:Color = MaterialTheme.colorScheme.inversePrimary): Color {
        return baseColor.copy(alpha = 0.4f)
    }

    @Composable
    fun defaultCardColor():Color {
        // light blue `Color(0xFFDBE9F3)`

        return if(Theme.inDarkTheme) MaterialTheme.colorScheme.surfaceBright else MaterialTheme.colorScheme.surfaceDim
//        return MaterialTheme.colorScheme.surfaceBright
    }


}
