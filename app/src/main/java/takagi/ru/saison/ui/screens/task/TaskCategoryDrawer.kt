package takagi.ru.saison.ui.screens.task

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
import takagi.ru.saison.domain.model.Tag
import kotlin.math.roundToInt

@Composable
fun TaskCategoryDrawer(
    visible: Boolean,
    categories: List<Tag>,
    selectedCategory: Tag?,
    onDismiss: () -> Unit,
    onCategorySelected: (Tag?) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (Tag, String) -> Unit,
    onDeleteCategory: (Tag) -> Unit
) {
    val drawerWidth = 300.dp
    val density = LocalDensity.current
    val drawerWidthPx = with(density) { drawerWidth.toPx() }
    
    val translationX = remember { Animatable(drawerWidthPx) }
    val scope = rememberCoroutineScope()
    
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

    if (visible || translationX.value < drawerWidthPx - 0.5f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {
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
                            val newOffset = (translationX.value + delta).coerceAtLeast(0f)
                            scope.launch { translationX.snapTo(newOffset) }
                        },
                        onDragStopped = { velocity ->
                            if (translationX.value > drawerWidthPx * 0.3f || velocity > 1000f) {
                                onDismiss()
                            } else {
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
                TaskCategoryDrawerContent(
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
private fun TaskCategoryDrawerContent(
    categories: List<Tag>,
    selectedCategory: Tag?,
    onDismiss: () -> Unit,
    onCategorySelected: (Tag?) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (Tag, String) -> Unit,
    onDeleteCategory: (Tag) -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryItem(
                    name = "全部任务",
                    isSelected = selectedCategory == null,
                    isDefault = true,
                    isEditMode = false,
                    onClick = { onCategorySelected(null) },
                    onRename = {},
                    onDelete = {}
                )
            }
            
            items(categories) { category ->
                CategoryItem(
                    name = category.name,
                    isSelected = selectedCategory?.id == category.id,
                    isDefault = category.name == "Default",
                    isEditMode = isEditMode,
                    onClick = { onCategorySelected(category) },
                    onRename = { newName -> onRenameCategory(category, newName) },
                    onDelete = { onDeleteCategory(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showAddCategoryDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加")
            }
            
            OutlinedButton(
                onClick = { isEditMode = !isEditMode },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
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

            if (isEditMode && !isDefault) {
                Row {
                    IconButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "重命名",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
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
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank() && newName != name) {
                            onRename(newName)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除分类") },
            text = { Text("确定要删除分类“$name”吗？该分类下的任务将移动到默认分类。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
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
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onConfirm(categoryName)
                    }
                },
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
