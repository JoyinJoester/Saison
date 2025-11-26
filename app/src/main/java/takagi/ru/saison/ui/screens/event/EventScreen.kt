package takagi.ru.saison.ui.screens.event

import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import takagi.ru.saison.R
import takagi.ru.saison.domain.model.EventCategory
import takagi.ru.saison.ui.components.CreateEventSheet
import takagi.ru.saison.ui.components.EventCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventScreen(
    viewModel: EventViewModel = hiltViewModel(),
    onEventClick: (Long) -> Unit,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToRoutine: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val events by viewModel.events.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showCreateSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showItemTypeSelector by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            EventTopBar(
                selectedCategory = selectedCategory,
                onCategoryChange = { viewModel.setCategory(it) },
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                isSearchActive = isSearchActive,
                onSearchActiveChange = { isSearchActive = it },
                onItemTypeSelectorClick = { showItemTypeSelector = true }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_create_event)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is EventUiState.Loading -> {
                    LoadingIndicator()
                }
                is EventUiState.Empty -> {
                    EmptyEventState(
                        onCreateClick = { showCreateSheet = true }
                    )
                }
                is EventUiState.Success -> {
                    if (events.isEmpty()) {
                        EmptySearchResult(
                            query = searchQuery,
                            onClearSearch = { viewModel.setSearchQuery("") }
                        )
                    } else {
                        EventList(
                            events = events,
                            onEventClick = onEventClick
                        )
                    }
                }
                is EventUiState.Error -> {
                    ErrorState(
                        message = (uiState as EventUiState.Error).message
                    )
                }
            }
        }
    }
    
    if (showCreateSheet) {
        CreateEventSheet(
            onDismiss = { showCreateSheet = false },
            onEventCreate = { event ->
                viewModel.createEvent(event)
                showCreateSheet = false
            }
        )
    }
    
    // 项目类型选择器 Bottom Sheet
    if (showItemTypeSelector) {
        takagi.ru.saison.ui.components.ItemTypeSelectorBottomSheet(
            currentType = takagi.ru.saison.domain.model.ItemType.EVENT,
            onDismiss = { showItemTypeSelector = false },
            onTypeSelected = { type ->
                showItemTypeSelector = false
                when (type) {
                    takagi.ru.saison.domain.model.ItemType.TASK -> onNavigateToTasks()
                    takagi.ru.saison.domain.model.ItemType.SCHEDULE -> onNavigateToRoutine()
                    takagi.ru.saison.domain.model.ItemType.EVENT -> {} // 保持在当前页面
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTopBar(
    selectedCategory: EventCategory?,
    onCategoryChange: (EventCategory?) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onItemTypeSelectorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TopAppBar(
            title = {
                if (isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(stringResource(R.string.event_search_placeholder)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Surface(
                        onClick = onItemTypeSelectorClick,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.event_screen_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.cd_dropdown_icon),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            actions = {
                IconButton(onClick = { onSearchActiveChange(!isSearchActive) }) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = stringResource(
                            if (isSearchActive) R.string.cd_close_search else R.string.cd_search
                        )
                    )
                }
            }
        )
        
        // 类别筛选
        if (!isSearchActive) {
            CategoryFilterRow(
                selectedCategory = selectedCategory,
                onCategoryChange = onCategoryChange
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: EventCategory?,
    onCategoryChange: (EventCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 全部
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategoryChange(null) },
            label = { 
                Text(
                    stringResource(R.string.event_category_all),
                    maxLines = 1
                ) 
            }
        )
        
        // 各个类别
        EventCategory.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategoryChange(category) },
                label = { 
                    Text(
                        stringResource(category.getDisplayNameResId()),
                        maxLines = 1
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = category.getIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun EventList(
    events: List<takagi.ru.saison.domain.model.Event>,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 88.dp // 为浮动按钮留出空间
        )
    ) {
        items(
            items = events,
            key = { it.id }
        ) { event ->
            EventCard(
                event = event,
                onClick = { onEventClick(event.id) }
            )
        }
    }
}

@Composable
private fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyEventState(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.event_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.event_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onCreateClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_create_event))
            }
        }
    }
}

@Composable
private fun EmptySearchResult(
    query: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.event_search_no_results),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.event_search_no_results_message, query),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onClearSearch) {
                Text(stringResource(R.string.action_clear_search))
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.event_error_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
