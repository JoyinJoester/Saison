package takagi.ru.saison.ui.screens.subscription

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import takagi.ru.saison.R
import takagi.ru.saison.data.local.database.entities.CategoryEntity
import kotlin.math.roundToInt

@Composable
fun CategoryDrawer(
    visible: Boolean,
    categories: List<CategoryEntity>,
    selectedCategory: String?,
    onDismiss: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (CategoryEntity, String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    val drawerWidth = 300.dp
    val density = LocalDensity.current
    val drawerWidthPx = with(density) { drawerWidth.toPx() }
    
    // 动画状态：控制抽屉的水平偏移量
    // 初始位置在屏幕外（drawerWidthPx），目标位置是 0
    val translationX = remember { Animatable(drawerWidthPx) }
    val scope = rememberCoroutineScope()
    
    // 监听 visible 变化，驱动进出场动画
    LaunchedEffect(visible) {
        if (visible) {
            translationX.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            translationX.animateTo(
                targetValue = drawerWidthPx,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // 只有当可见或者动画未结束（未完全移出屏幕）时才渲染
    // 使用 0.5f 作为容差，避免浮点数精度问题导致微小的残留
    if (visible || translationX.value < drawerWidthPx - 0.5f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f) // 确保在最上层
        ) {
            // 背景遮罩
            // 透明度根据抽屉拉出的比例动态计算
            val scrimAlpha = (1f - (translationX.value / drawerWidthPx)).coerceIn(0f, 1f) * 0.32f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )

            // 侧边抽屉内容
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(drawerWidth)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .fillMaxHeight()
                    .offset { IntOffset(translationX.value.roundToInt(), 0) }
                    .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            // 只能向右拖动（正值），且不能小于0（不能向左超出屏幕）
                            val newOffset = (translationX.value + delta).coerceAtLeast(0f)
                            scope.launch { translationX.snapTo(newOffset) }
                        },
                        onDragStopped = { velocity ->
                            // 如果拖动超过宽度的 30% 或者向右甩动的速度够快，则关闭
                            if (translationX.value > drawerWidthPx * 0.3f || velocity > 1000f) {
                                onDismiss()
                            } else {
                                // 否则回弹
                                scope.launch { 
                                    translationX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) 
                                }
                            }
                        }
                    )
            ) {
                CategoryDrawerContent(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onDismiss = onDismiss,
                    onCategorySelected = onCategorySelected,
                    onAddCategory = onAddCategory,
                    onRenameCategory = onRenameCategory,
                    onDeleteCategory = onDeleteCategory
                )
            }
        }
    }
}

@Composable
private fun CategoryDrawerContent(
    categories: List<CategoryEntity>,
    selectedCategory: String?,
    onDismiss: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (CategoryEntity, String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "分类筛选",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 分类列表
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 全部订阅选项
            item {
                CategoryItem(
                    name = "全部订阅",
                    isSelected = selectedCategory == null,
                    isDefault = true,
                    isEditMode = false, // 全部订阅不可编辑
                    onClick = { onCategorySelected(null) },
                    onRename = {},
                    onDelete = {}
                )
            }

            // 未分类
            item {
                CategoryItem(
                    name = "未分类",
                    isSelected = selectedCategory == "默认",
                    isDefault = true,
                    isEditMode = false,
                    onClick = { onCategorySelected("默认") },
                    onRename = {},
                    onDelete = {}
                )
            }

            // 其他分类
            items(categories.filter { !it.isDefault }) { category ->
                CategoryItem(
                    name = category.name,
                    isSelected = selectedCategory == category.name,
                    isDefault = false,
                    isEditMode = isEditMode,
                    onClick = { onCategorySelected(category.name) },
                    onRename = { newName -> onRenameCategory(category, newName) },
                    onDelete = { onDeleteCategory(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 底部按钮栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 添加分类按钮
            Button(
                onClick = { showAddCategoryDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加")
            }
            
            // 编辑按钮
            OutlinedButton(
                onClick = { isEditMode = !isEditMode },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else androidx.compose.material.icons.Icons.Default.Edit,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditMode) "完成" else "编辑")
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { categoryName ->
                onAddCategory(categoryName)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
private fun CategoryItem(
    name: String,
    isSelected: Boolean,
    isDefault: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditMode && !isDefault) {
                    // 编辑模式下的按钮
                    IconButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                            contentDescription = "重命名",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (isSelected) {
                    // 选中状态下的图标
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选中",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除分类") },
            text = { Text("确定要删除分类 \"$name\" 吗？该分类下的所有订阅将移动到“未分类”。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名分类") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank() && newName != name) {
                            onRename(newName)
                        }
                        showRenameDialog = false
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加分类") },
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("分类名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(categoryName) },
                enabled = categoryName.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
