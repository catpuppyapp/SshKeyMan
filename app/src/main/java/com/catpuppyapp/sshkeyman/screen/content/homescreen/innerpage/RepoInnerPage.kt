package com.catpuppyapp.sshkeyman.screen.content.homescreen.innerpage

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.catpuppyapp.sshkeyman.R
import com.catpuppyapp.sshkeyman.compose.MyLazyColumn
import com.catpuppyapp.sshkeyman.compose.RepoCard
import com.catpuppyapp.sshkeyman.data.AppContainer
import com.catpuppyapp.sshkeyman.data.entity.SshKeyEntity
import com.catpuppyapp.sshkeyman.style.MyStyleKt
import com.catpuppyapp.sshkeyman.ui.theme.Theme
import com.catpuppyapp.sshkeyman.utils.ActivityUtil
import com.catpuppyapp.sshkeyman.utils.AppModel
import com.catpuppyapp.sshkeyman.utils.ComposeHelper
import com.catpuppyapp.sshkeyman.utils.doJobThenOffLoading
import com.catpuppyapp.sshkeyman.utils.state.CustomStateListSaveable
import com.catpuppyapp.sshkeyman.utils.state.CustomStateSaveable

private val TAG = "RepoInnerPage"
private val stateKeyTag = "RepoInnerPage"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoInnerPage(
    showBottomSheet: MutableState<Boolean>,
    sheetState: SheetState,
    curRepo: CustomStateSaveable<SshKeyEntity>,
    curRepoIndex: MutableIntState,
    contentPadding: PaddingValues,
    repoPageListState: LazyListState,
    openDrawer:() -> Unit,
    repoList:CustomStateListSaveable<SshKeyEntity>,
    needRefreshPage:MutableState<String>
) {
    val activityContext = AppModel.singleInstanceHolder.activityContext;
    val exitApp = AppModel.singleInstanceHolder.exitApp;
    val navController = AppModel.singleInstanceHolder.navController;
    val scope = rememberCoroutineScope()


    val clipboardManager = LocalClipboardManager.current


    val dbContainer = AppModel.singleInstanceHolder.dbContainer;
//    val repoDtoList = remember { mutableStateListOf<RepoEntity>() }

    val activity = ActivityUtil.getCurrentActivity()

    //back handler block start
    val isBackHandlerEnable = rememberSaveable { mutableStateOf(true)}
    val backHandlerOnBack = ComposeHelper.getDoubleClickBackHandler(context = activityContext, openDrawer = openDrawer, exitApp = exitApp)
    //注册BackHandler，拦截返回键，实现双击返回和返回上级目录
    BackHandler(enabled = isBackHandlerEnable.value, onBack = {
        backHandlerOnBack()
    })
    //back handler block end

    val inDarkTheme = Theme.inDarkTheme

    val requireBlinkIdx = rememberSaveable{mutableIntStateOf(-1)}

    val isLoading = rememberSaveable { mutableStateOf(true)}
    val loadingText = rememberSaveable { mutableStateOf(activityContext.getString(R.string.loading))}
    val loadingOn = {text:String->
        loadingText.value = text

        // disable this feel better, else screen will blank then restore, feel sick
//        isLoading.value=true
    }
    val loadingOff = {
        isLoading.value=false
        loadingText.value = ""
    }

    val showCreateDialog = rememberSaveable { mutableStateOf(false) }


    if (!isLoading.value && repoList.value.isEmpty()) {  //无仓库，显示添加按钮
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())

                ,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,

        ) {
            //interactionSource和indication的作用是隐藏按下时的背景半透明那个按压效果，很难看，所以隐藏
            Column(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                showCreateDialog.value=true
            },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ){
                Row{
                    Icon(modifier = Modifier.size(50.dp),
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add),
                        tint = MyStyleKt.IconColor.normal
                    )
                }
                Row {
                    Text(text = stringResource(id = R.string.add_a_repo),
                        style = MyStyleKt.ClickableText.style,
                        color = MyStyleKt.ClickableText.color,
                        fontSize = MyStyleKt.TextSize.default
                    )
                }
            }

        }
    }



    if(!isLoading.value && repoList.value.isNotEmpty()) {  //有仓库
        MyLazyColumn(
            contentPadding = contentPadding,
            list = repoList.value,
            listState = repoPageListState,
            requireForEachWithIndex = true,
            requirePaddingAtBottom = true
        ) {idx, element->

            RepoCard(
                showBottomSheet,
                curRepo,
                curRepoIndex,
                repoDto = element,
                repoDtoIndex = idx,
                requireBlinkIdx = requireBlinkIdx,

            )

        }


    }


    //没换页面，但需要刷新页面，这时LaunchedEffect不会执行，就靠这个变量控制刷新页面了
//    if(needRefreshRepoPage.value) {
//        initRepoPage()
//        needRefreshRepoPage.value=false
//    }
    //compose创建时的副作用
    LaunchedEffect(needRefreshPage.value) {
        try {
            // 仓库页面检查仓库状态，对所有状态为notReadyNeedClone的仓库执行clone，卡片把所有状态为notReadyNeedClone的仓库都设置成不可操作，显示正在克隆loading信息
            doInit(
                dbContainer = dbContainer,
                repoDtoList = repoList,
                loadingOn = loadingOn,
                loadingOff = loadingOff,
                activityContext = activityContext,
            )

        } catch (cancel: Exception) {
//            println("LaunchedEffect: job cancelled")
        }
    }
}

private fun doInit(
    dbContainer: AppContainer,
    repoDtoList: CustomStateListSaveable<SshKeyEntity>,
    loadingOn:(String)->Unit,
    loadingOff:()->Unit,
    activityContext:Context,
){
    doJobThenOffLoading(loadingOn, loadingOff, activityContext.getString(R.string.loading)) {
        //执行仓库页面的初始化操作
        val repoRepository = dbContainer.sshKeyRepository
        //貌似如果用Flow，后续我更新数据库，不需要再次手动更新State数据就会自动刷新，也就是Flow会观测数据，如果改变，重新执行sql获取最新的数据，但最好还是手动更新，避免资源浪费
        val repoListFromDb = repoRepository.getAll();

        repoDtoList.value.clear()
        repoDtoList.value.addAll(repoListFromDb)
    }
}

