package com.example.controleestoque

import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val APP_VERSION = "1.0"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ControleEstoqueApp()
        }
    }
}

private enum class Screen(val label: String) {
    Dashboard("Painel"),
    Products("Produtos"),
    Categories("Categorias"),
    Users("Usuarios"),
    Movements("Movimentacoes"),
    Reports("Relatorios")
}

private enum class MovementType(val label: String) {
    Entry("Entrada"),
    Exit("Saida")
}

private enum class ReportsTab(val label: String) {
    Dashboard("Dashboard"),
    Reports("Relatorios")
}

private enum class ReportType(val label: String) {
    Users("Usuarios"),
    Products("Produtos"),
    Movements("Movimentacao"),
    Entries("Entradas"),
    Exits("Saidas")
}

private data class Product(
    val id: Int,
    val name: String,
    val code: String,
    val category: String,
    val quantity: Int,
    val minQuantity: Int,
    val maxQuantity: Int,
    val unitPrice: Double
)

private data class ProductCategory(
    val id: Int,
    val name: String
)

private data class AppUser(
    val id: Int,
    val code: String,
    val name: String,
    val login: String,
    val password: String,
    val active: Boolean
)

private data class StockMovement(
    val id: Int,
    val productId: Int,
    val productName: String,
    val productCode: String,
    val unitPrice: Double,
    val type: MovementType,
    val quantity: Int,
    val createdAt: LocalDateTime
)

private data class ReportData(
    val title: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val totalizer: String
)

@Composable
private fun ControleEstoqueApp() {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF2E6F57),
            secondary = Color(0xFF425D8A),
            tertiary = Color(0xFF8A5A44),
            background = Color(0xFFF6F8F7),
            surface = Color.White
        )
    ) {
        var isAuthenticated by remember { mutableStateOf(false) }
        var nextUserId by remember { mutableIntStateOf(2) }
        val users = remember {
            mutableStateListOf(
                AppUser(1, "USR-001", "Administrador", "admin", "admin", true)
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isAuthenticated) {
                InventoryHome(
                    users = users,
                    onLogout = { isAuthenticated = false },
                    onSaveUser = { editedUser ->
                        val index = users.indexOfFirst { it.id == editedUser.id }
                        if (index >= 0) {
                            users[index] = editedUser
                        } else {
                            users.add(editedUser.copy(id = nextUserId++))
                        }
                    }
                )
            } else {
                LoginScreen(
                    users = users,
                    onLoginSuccess = { isAuthenticated = true }
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(
    users: List<AppUser>,
    onLoginSuccess: () -> Unit
) {
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Controle de Estoque",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Versao $APP_VERSION",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Acesse com admin / admin",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = user,
                    onValueChange = {
                        user = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Usuario") }
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Senha") },
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        val foundUser = users.firstOrNull {
                            it.active && it.login == user.trim() && it.password == password
                        }
                        if (foundUser != null) {
                            onLoginSuccess()
                        } else {
                            error = "Usuario ou senha invalidos."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Entrar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryHome(
    users: List<AppUser>,
    onLogout: () -> Unit,
    onSaveUser: (AppUser) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedScreen by remember { mutableStateOf(Screen.Dashboard) }
    var nextProductId by remember { mutableIntStateOf(4) }
    var nextCategoryId by remember { mutableIntStateOf(4) }
    var nextMovementId by remember { mutableIntStateOf(4) }
    val categories = remember {
        mutableStateListOf(
            ProductCategory(1, "Informatica"),
            ProductCategory(2, "Material de escritorio"),
            ProductCategory(3, "Material de limpeza")
        )
    }
    val products = remember {
        mutableStateListOf(
            Product(1, "Notebook Dell", "PRD-001", "Informatica", 8, 5, 20, 3450.0),
            Product(2, "Mouse sem fio", "PRD-002", "Informatica", 35, 10, 80, 89.9),
            Product(3, "Teclado mecanico", "PRD-003", "Informatica", 16, 8, 40, 249.9)
        )
    }
    val movements = remember {
        mutableStateListOf(
            StockMovement(1, 1, "Notebook Dell", "PRD-001", 3450.0, MovementType.Entry, 10, LocalDateTime.now().minusDays(2)),
            StockMovement(2, 2, "Mouse sem fio", "PRD-002", 89.9, MovementType.Entry, 40, LocalDateTime.now().minusDays(1)),
            StockMovement(3, 1, "Notebook Dell", "PRD-001", 3450.0, MovementType.Exit, 2, LocalDateTime.now().minusHours(5))
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Controle de Estoque",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Screen.entries.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.label) },
                        selected = selectedScreen == screen,
                        onClick = {
                            selectedScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(
                    label = { Text("Sair") },
                    selected = false,
                    onClick = onLogout,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(selectedScreen.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            HamburgerIcon()
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            when (selectedScreen) {
                Screen.Dashboard -> DashboardScreen(products, movements, padding)
                Screen.Products -> ProductsScreen(
                    products = products,
                    categories = categories.map { it.name },
                    contentPadding = padding,
                    onSave = { editedProduct ->
                        val index = products.indexOfFirst { it.id == editedProduct.id }
                        if (index >= 0) {
                            products[index] = editedProduct
                        } else {
                            products.add(editedProduct.copy(id = nextProductId++))
                        }
                    },
                    onDelete = { product ->
                        products.removeAll { it.id == product.id }
                    }
                )
                Screen.Categories -> CategoriesScreen(
                    categories = categories,
                    contentPadding = padding,
                    onSave = { editedCategory ->
                        val normalizedName = editedCategory.name.trim()
                        val index = categories.indexOfFirst { it.id == editedCategory.id }
                        if (index >= 0) {
                            val oldName = categories[index].name
                            categories[index] = editedCategory.copy(name = normalizedName)
                            products.replaceAll { product ->
                                if (product.category == oldName) product.copy(category = normalizedName) else product
                            }
                        } else {
                            categories.add(editedCategory.copy(id = nextCategoryId++, name = normalizedName))
                        }
                    }
                )
                Screen.Users -> UsersScreen(
                    users = users,
                    contentPadding = padding,
                    onSave = onSaveUser
                )
                Screen.Movements -> MovementsScreen(
                    products = products,
                    movements = movements,
                    contentPadding = padding,
                    onAddMovement = { product, type, quantity ->
                        val productIndex = products.indexOfFirst { it.id == product.id }
                        if (productIndex >= 0) {
                            val current = products[productIndex]
                            val newQuantity = when (type) {
                                MovementType.Entry -> current.quantity + quantity
                                MovementType.Exit -> current.quantity - quantity
                            }
                            products[productIndex] = current.copy(quantity = newQuantity)
                            movements.add(
                                0,
                                StockMovement(
                                    id = nextMovementId++,
                                    productId = current.id,
                                    productName = current.name,
                                    productCode = current.code,
                                    unitPrice = current.unitPrice,
                                    type = type,
                                    quantity = quantity,
                                    createdAt = LocalDateTime.now()
                                )
                            )
                        }
                    }
                )
                Screen.Reports -> ReportsScreen(
                    users = users,
                    products = products,
                    movements = movements,
                    contentPadding = padding
                )
            }
        }
    }
}

@Composable
private fun HamburgerIcon() {
    Column(
        modifier = Modifier.width(24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
private fun DashboardScreen(
    products: List<Product>,
    movements: List<StockMovement>,
    contentPadding: PaddingValues
) {
    val totalProducts = products.size
    val totalItems = products.sumOf { it.quantity }
    val totalValue = products.sumOf { it.quantity * it.unitPrice }
    val lowStockProducts = products.filter { it.quantity <= it.minQuantity }
    val sortedMovements = movements.sortedByDescending { it.createdAt }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SummaryCard("Produtos cadastrados", totalProducts.toString())
        }
        item {
            SummaryCard("Itens em estoque", totalItems.toString())
        }
        item {
            SummaryCard("Valor total em estoque", money(totalValue))
        }
        if (lowStockProducts.isNotEmpty()) {
            item {
                Text(
                    text = "Alertas de estoque minimo",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(lowStockProducts) { product ->
                LowStockCard(product)
            }
        }
        item {
            Text(
                text = "Movimentacoes",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (sortedMovements.isEmpty()) {
            item {
                Text(
                    text = "Nenhuma movimentacao registrada.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(sortedMovements) { movement ->
            MovementRow(movement)
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LowStockCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A4B00)
            )
            Text(
                text = "Estoque atual: ${product.quantity} | minimo: ${product.minQuantity}",
                color = Color(0xFF8A4B00)
            )
        }
    }
}

@Composable
private fun ProductsScreen(
    products: List<Product>,
    categories: List<String>,
    contentPadding: PaddingValues,
    onSave: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var deletingProduct by remember { mutableStateOf<Product?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val filteredProducts = products.filter { product ->
        val term = search.trim()
        term.isBlank() ||
            product.name.contains(term, ignoreCase = true) ||
            product.code.contains(term, ignoreCase = true) ||
            product.category.contains(term, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                editingProduct = null
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = categories.isNotEmpty()
        ) {
            Text("Cadastrar produto")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Pesquisar por nome, codigo ou categoria") },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        if (categories.isEmpty()) {
            Text(
                text = "Cadastre uma categoria antes de criar produtos.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filteredProducts) { product ->
                ProductCard(
                    product = product,
                    onEdit = {
                        editingProduct = product
                        showDialog = true
                    },
                    onDelete = { deletingProduct = product }
                )
            }
            if (filteredProducts.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum produto encontrado.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDialog) {
        ProductDialog(
            product = editingProduct,
            products = products,
            categories = categories,
            generatedCode = nextProductCode(products),
            onDismiss = { showDialog = false },
            onSave = {
                onSave(it)
                showDialog = false
            }
        )
    }

    deletingProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            title = { Text("Excluir produto") },
            text = { Text("Deseja excluir ${product.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(product)
                        deletingProduct = null
                    }
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProduct = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit) {
                        Text("Editar")
                    }
                    OutlinedButton(onClick = onDelete) {
                        Text("Excluir")
                    }
                }
            }
            Text("Codigo: ${product.code}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Categoria: ${product.category}")
            Text("Quantidade: ${product.quantity}")
            Text("Estoque minimo: ${product.minQuantity}")
            Text("Estoque maximo: ${product.maxQuantity}")
            Text("Valor unitario: ${money(product.unitPrice)}")
        }
    }
}

@Composable
private fun ProductDialog(
    product: Product?,
    products: List<Product>,
    categories: List<String>,
    generatedCode: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember(product) { mutableStateOf(product?.name.orEmpty()) }
    var code by remember(product) { mutableStateOf(product?.code ?: generatedCode) }
    var category by remember(product, categories) { mutableStateOf(product?.category ?: categories.firstOrNull().orEmpty()) }
    var quantity by remember(product) { mutableStateOf(product?.quantity?.toString().orEmpty()) }
    var minQuantity by remember(product) { mutableStateOf(product?.minQuantity?.toString().orEmpty()) }
    var maxQuantity by remember(product) { mutableStateOf(product?.maxQuantity?.toString().orEmpty()) }
    var price by remember(product) { mutableStateOf(product?.unitPrice?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Novo produto" else "Editar produto") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase()
                        error = null
                    },
                    label = { Text("Codigo gerado") },
                    singleLine = true
                )
                Text("Categoria")
                categories.forEach { option ->
                    if (category == option) {
                        Button(
                            onClick = { category = option },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { category = option },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option)
                        }
                    }
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.onlyDigits() },
                    label = { Text("Quantidade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minQuantity,
                        onValueChange = { minQuantity = it.onlyDigits() },
                        modifier = Modifier.weight(1f),
                        label = { Text("Minimo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxQuantity,
                        onValueChange = { maxQuantity = it.onlyDigits() },
                        modifier = Modifier.weight(1f),
                        label = { Text("Maximo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.replace(',', '.') },
                    label = { Text("Valor unitario") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalizedCode = code.trim().uppercase()
                    val parsedQuantity = quantity.toIntOrNull()
                    val parsedMinQuantity = minQuantity.toIntOrNull()
                    val parsedMaxQuantity = maxQuantity.toIntOrNull()
                    val parsedPrice = price.toDoubleOrNull()
                    val duplicateCode = products.any {
                        it.id != product?.id && it.code.equals(normalizedCode, ignoreCase = true)
                    }
                    when {
                        name.isBlank() || normalizedCode.isBlank() || category.isBlank() || parsedQuantity == null ||
                            parsedMinQuantity == null || parsedMaxQuantity == null || parsedPrice == null -> {
                            error = "Preencha todos os campos corretamente."
                        }
                        duplicateCode -> {
                            error = "Ja existe produto com este codigo."
                        }
                        parsedMinQuantity > parsedMaxQuantity -> {
                            error = "O minimo nao pode ser maior que o maximo."
                        }
                        parsedQuantity > parsedMaxQuantity -> {
                            error = "A quantidade atual nao pode passar do maximo."
                        }
                        else -> {
                            onSave(
                                Product(
                                    id = product?.id ?: 0,
                                    name = name.trim(),
                                    code = normalizedCode,
                                    category = category,
                                    quantity = parsedQuantity,
                                    minQuantity = parsedMinQuantity,
                                    maxQuantity = parsedMaxQuantity,
                                    unitPrice = parsedPrice
                                )
                            )
                        }
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun CategoriesScreen(
    categories: List<ProductCategory>,
    contentPadding: PaddingValues,
    onSave: (ProductCategory) -> Unit
) {
    var editingCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                editingCategory = null
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cadastrar categoria")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(categories) { category ->
                CategoryCard(
                    category = category,
                    onEdit = {
                        editingCategory = category
                        showDialog = true
                    }
                )
            }
        }
    }

    if (showDialog) {
        CategoryDialog(
            category = editingCategory,
            categories = categories,
            onDismiss = { showDialog = false },
            onSave = {
                onSave(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun CategoryCard(category: ProductCategory, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(category.name, fontWeight = FontWeight.Bold)
                Text("Codigo: CAT-${category.id.toString().padStart(3, '0')}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onEdit) {
                Text("Editar")
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    category: ProductCategory?,
    categories: List<ProductCategory>,
    onDismiss: () -> Unit,
    onSave: (ProductCategory) -> Unit
) {
    var name by remember(category) { mutableStateOf(category?.name.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Nova categoria" else "Editar categoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Nome") },
                    singleLine = true
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duplicate = categories.any {
                        it.id != category?.id && it.name.equals(name.trim(), ignoreCase = true)
                    }
                    when {
                        name.isBlank() -> error = "Informe o nome da categoria."
                        duplicate -> error = "Ja existe categoria com este nome."
                        else -> onSave(ProductCategory(category?.id ?: 0, name.trim()))
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun UsersScreen(
    users: List<AppUser>,
    contentPadding: PaddingValues,
    onSave: (AppUser) -> Unit
) {
    var editingUser by remember { mutableStateOf<AppUser?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                editingUser = null
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cadastrar usuario")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(users) { user ->
                UserCard(
                    user = user,
                    onEdit = {
                        editingUser = user
                        showDialog = true
                    }
                )
            }
        }
    }

    if (showDialog) {
        UserDialog(
            user = editingUser,
            users = users,
            generatedCode = nextUserCode(users),
            onDismiss = { showDialog = false },
            onSave = {
                onSave(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun UserCard(user: AppUser, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.name, fontWeight = FontWeight.Bold)
                Text("Codigo: ${user.code}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Login: ${user.login}")
                Text(if (user.active) "Ativo" else "Inativo", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onEdit) {
                Text("Editar")
            }
        }
    }
}

@Composable
private fun UserDialog(
    user: AppUser?,
    users: List<AppUser>,
    generatedCode: String,
    onDismiss: () -> Unit,
    onSave: (AppUser) -> Unit
) {
    var code by remember(user) { mutableStateOf(user?.code ?: generatedCode) }
    var name by remember(user) { mutableStateOf(user?.name.orEmpty()) }
    var login by remember(user) { mutableStateOf(user?.login.orEmpty()) }
    var password by remember(user) { mutableStateOf(user?.password.orEmpty()) }
    var active by remember(user) { mutableStateOf(user?.active ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user == null) "Novo usuario" else "Editar usuario") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase()
                        error = null
                    },
                    label = { Text("Codigo") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Nome") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = login,
                    onValueChange = {
                        login = it
                        error = null
                    },
                    label = { Text("Usuario") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                    },
                    label = { Text("Senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Usuario ativo")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalizedCode = code.trim().uppercase()
                    val normalizedLogin = login.trim()
                    val duplicateCode = users.any {
                        it.id != user?.id && it.code.equals(normalizedCode, ignoreCase = true)
                    }
                    val duplicateLogin = users.any {
                        it.id != user?.id && it.login.equals(normalizedLogin, ignoreCase = true)
                    }
                    when {
                        normalizedCode.isBlank() || name.isBlank() || normalizedLogin.isBlank() || password.isBlank() -> {
                            error = "Preencha todos os campos."
                        }
                        duplicateCode -> error = "Ja existe usuario com este codigo."
                        duplicateLogin -> error = "Ja existe usuario com este login."
                        else -> {
                            onSave(
                                AppUser(
                                    id = user?.id ?: 0,
                                    code = normalizedCode,
                                    name = name.trim(),
                                    login = normalizedLogin,
                                    password = password,
                                    active = active
                                )
                            )
                        }
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ReportsScreen(
    users: List<AppUser>,
    products: List<Product>,
    movements: List<StockMovement>,
    contentPadding: PaddingValues
) {
    var selectedTab by remember { mutableStateOf(ReportsTab.Dashboard) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportsTab.entries.forEach { tab ->
                if (selectedTab == tab) {
                    Button(
                        onClick = { selectedTab = tab },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tab.label)
                    }
                } else {
                    OutlinedButton(
                        onClick = { selectedTab = tab },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tab.label)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when (selectedTab) {
            ReportsTab.Dashboard -> ReportsDashboard(products, movements)
            ReportsTab.Reports -> ReportsList(users, products, movements)
        }
    }
}

@Composable
private fun ReportsDashboard(
    products: List<Product>,
    movements: List<StockMovement>
) {
    val totalEntries = movements.filter { it.type == MovementType.Entry }.sumOf { it.quantity }
    val totalExits = movements.filter { it.type == MovementType.Exit }.sumOf { it.quantity }
    val maxMovement = maxOf(totalEntries, totalExits, 1)
    val categoryTotals = products
        .groupBy { it.category }
        .mapValues { item -> item.value.sumOf { it.quantity } }
        .toList()
        .sortedByDescending { it.second }
    val maxCategory = maxOf(categoryTotals.maxOfOrNull { it.second } ?: 0, 1)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SummaryCard("Total de entradas", totalEntries.toString())
        }
        item {
            SummaryCard("Total de saidas", totalExits.toString())
        }
        item {
            ChartCard(title = "Movimentacoes por tipo") {
                ChartBar(
                    label = "Entradas",
                    value = totalEntries,
                    maxValue = maxMovement,
                    color = MaterialTheme.colorScheme.primary
                )
                ChartBar(
                    label = "Saidas",
                    value = totalExits,
                    maxValue = maxMovement,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        item {
            ChartCard(title = "Estoque por categoria") {
                if (categoryTotals.isEmpty()) {
                    Text(
                        text = "Nenhum produto cadastrado.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    categoryTotals.forEach { (category, total) ->
                        ChartBar(
                            label = category,
                            value = total,
                            maxValue = maxCategory,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ChartBar(
    label: String,
    value: Int,
    maxValue: Int,
    color: Color
) {
    val fraction = (value.toFloat() / maxValue.toFloat()).coerceIn(0.04f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(value.toString(), fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Color(0xFFE6ECE9), RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(12.dp)
                    .background(color, RoundedCornerShape(6.dp))
            )
        }
    }
}

@Composable
private fun ReportsList(
    users: List<AppUser>,
    products: List<Product>,
    movements: List<StockMovement>
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(ReportType.Users) }
    val report = buildReport(selectedType, users, products, movements)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "Tipo de relatorio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(ReportType.entries) { type ->
            if (selectedType == type) {
                Button(
                    onClick = { selectedType = type },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(type.label)
                }
            } else {
                OutlinedButton(
                    onClick = { selectedType = type },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(type.label)
                }
            }
        }
        item {
            Button(
                onClick = { printReport(context, report) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Imprimir")
            }
        }
        item {
            ReportPreview(report)
        }
    }
}

@Composable
private fun ReportPreview(report: ReportData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(report.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(report.headers.joinToString(" | "), color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            if (report.rows.isEmpty()) {
                Text("Nenhum registro encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                report.rows.forEach { row ->
                    Text(row.joinToString(" | "))
                }
            }
            HorizontalDivider()
            Text(report.totalizer, fontWeight = FontWeight.Bold)
        }
    }
}

private fun buildReport(
    type: ReportType,
    users: List<AppUser>,
    products: List<Product>,
    movements: List<StockMovement>
): ReportData {
    return when (type) {
        ReportType.Users -> ReportData(
            title = "Relatorio de usuarios",
            headers = listOf("Codigo", "Nome", "Usuario", "Status"),
            rows = users.map {
                listOf(it.code, it.name, it.login, if (it.active) "Ativo" else "Inativo")
            },
            totalizer = "Total de usuarios: ${users.size}"
        )
        ReportType.Products -> ReportData(
            title = "Relatorio de produtos",
            headers = listOf("Codigo", "Descricao", "Categoria", "Quantidade", "Valor unitario"),
            rows = products.map {
                listOf(it.code, it.name, it.category, it.quantity.toString(), money(it.unitPrice))
            },
            totalizer = "Total de produtos: ${products.size}"
        )
        ReportType.Movements -> ReportData(
            title = "Relatorio de movimentacao",
            headers = listOf("Numero", "Data", "Tipo", "Codigo", "Material", "Quantidade"),
            rows = movements.sortedBy { it.id }.map {
                listOf(
                    it.id.toString(),
                    dateTime(it.createdAt),
                    it.type.label,
                    it.productCode,
                    it.productName,
                    it.quantity.toString()
                )
            },
            totalizer = "Total de movimentacoes: ${movements.size}"
        )
        ReportType.Entries -> movementValueReport(
            title = "Relatorio de entradas",
            movements = movements.filter { it.type == MovementType.Entry }
        )
        ReportType.Exits -> movementValueReport(
            title = "Relatorio de saidas",
            movements = movements.filter { it.type == MovementType.Exit }
        )
    }
}

private fun movementValueReport(
    title: String,
    movements: List<StockMovement>
): ReportData {
    val total = movements.sumOf { it.unitPrice * it.quantity }
    return ReportData(
        title = title,
        headers = listOf("Codigo", "Descricao", "Quantidade", "Valor unitario", "Total"),
        rows = movements.sortedBy { it.id }.map {
            listOf(
                it.productCode,
                it.productName,
                it.quantity.toString(),
                money(it.unitPrice),
                money(it.unitPrice * it.quantity)
            )
        },
        totalizer = "Total geral: ${money(total)}"
    )
}

private fun printReport(context: Context, report: ReportData) {
    val webView = WebView(context)
    val html = report.toHtml()
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    webView.post {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = webView.createPrintDocumentAdapter(report.title)
        printManager.print(
            report.title,
            adapter,
            PrintAttributes.Builder().build()
        )
    }
}

private fun ReportData.toHtml(): String {
    val headerHtml = headers.joinToString("") { "<th>${it.escapeHtml()}</th>" }
    val rowsHtml = rows.joinToString("") { row ->
        "<tr>${row.joinToString("") { "<td>${it.escapeHtml()}</td>" }}</tr>"
    }
    return """
        <html>
            <head>
                <style>
                    body { font-family: sans-serif; padding: 24px; }
                    h1 { font-size: 22px; margin-bottom: 16px; }
                    table { border-collapse: collapse; width: 100%; }
                    th, td { border: 1px solid #999; padding: 8px; text-align: left; }
                    th { background: #e6ece9; }
                    .total { margin-top: 16px; font-weight: bold; }
                </style>
            </head>
            <body>
                <h1>${title.escapeHtml()}</h1>
                <table>
                    <thead><tr>$headerHtml</tr></thead>
                    <tbody>$rowsHtml</tbody>
                </table>
                <div class="total">${totalizer.escapeHtml()}</div>
            </body>
        </html>
    """.trimIndent()
}

private fun String.escapeHtml(): String {
    return replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}

@Composable
private fun MovementsScreen(
    products: List<Product>,
    movements: List<StockMovement>,
    contentPadding: PaddingValues,
    onAddMovement: (Product, MovementType, Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = products.isNotEmpty()
        ) {
            Text("Registrar movimentacao")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(movements.sortedByDescending { it.createdAt }) { movement ->
                MovementRow(movement)
            }
        }
    }

    if (showDialog && products.isNotEmpty()) {
        MovementDialog(
            products = products,
            onDismiss = { showDialog = false },
            onSave = { product, type, quantity ->
                onAddMovement(product, type, quantity)
                showDialog = false
            }
        )
    }
}

@Composable
private fun MovementDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onSave: (Product, MovementType, Int) -> Unit
) {
    var selectedProduct by remember { mutableStateOf(products.first()) }
    var type by remember { mutableStateOf(MovementType.Entry) }
    var quantity by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova movimentacao") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Produto")
                products.forEach { product ->
                    OutlinedButton(
                        onClick = { selectedProduct = product },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedProduct.id == product.id) "${product.name} selecionado" else product.name)
                    }
                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MovementType.entries.forEach { movementType ->
                        if (type == movementType) {
                            Button(onClick = { type = movementType }) {
                                Text(movementType.label)
                            }
                        } else {
                            OutlinedButton(onClick = { type = movementType }) {
                                Text(movementType.label)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it.onlyDigits()
                        error = null
                    },
                    label = { Text("Quantidade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text("Estoque atual: ${selectedProduct.quantity}")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedQuantity = quantity.toIntOrNull()
                    when {
                        parsedQuantity == null || parsedQuantity <= 0 -> {
                            error = "Informe uma quantidade valida."
                        }
                        type == MovementType.Exit && parsedQuantity > selectedProduct.quantity -> {
                            error = "Saida maior que o estoque disponivel."
                        }
                        type == MovementType.Entry && selectedProduct.quantity + parsedQuantity > selectedProduct.maxQuantity -> {
                            error = "Entrada ultrapassa o estoque maximo."
                        }
                        else -> onSave(selectedProduct, type, parsedQuantity)
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun MovementRow(movement: StockMovement) {
    val color = if (movement.type == MovementType.Entry) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(movement.productName, fontWeight = FontWeight.Bold)
                Text(
                    "${movement.type.label} de ${movement.quantity} un. em ${dateTime(movement.createdAt)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (movement.type == MovementType.Entry) "+${movement.quantity}" else "-${movement.quantity}",
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun nextProductCode(products: List<Product>): String {
    val nextNumber = products.mapNotNull { product ->
        product.code.substringAfterLast("-", missingDelimiterValue = product.code).toIntOrNull()
    }.maxOrNull()?.plus(1) ?: 1
    return "PRD-${nextNumber.toString().padStart(3, '0')}"
}

private fun nextUserCode(users: List<AppUser>): String {
    val nextNumber = users.mapNotNull { user ->
        user.code.substringAfterLast("-", missingDelimiterValue = user.code).toIntOrNull()
    }.maxOrNull()?.plus(1) ?: 1
    return "USR-${nextNumber.toString().padStart(3, '0')}"
}

private fun String.onlyDigits(): String = filter { it.isDigit() }

private fun money(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}

private fun dateTime(value: LocalDateTime): String {
    return value.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
}
